// hooks/useWelfareSearch.js
//
// 복지 검색 페이지의 모든 상태(필터·페이지·결과·로딩·에러·총 페이지)를 한곳에서 관리.
// 페이지 컴포넌트는 이 훅이 반환하는 값만 사용하면 된다.
//
// 정책:
//  - 적용된 검색 조건과 페이지 번호는 **URL 쿼리** 에 보관한다.
//    → 브라우저 뒤로가기/앞으로가기/새로고침이 자연스럽게 동작하고, 상세 페이지에 다녀와도 검색 상태가 복원된다.
//  - 필터가 바뀌어도 자동 검색하지 않는다. 노년층 UX 로 의도하지 않은 조회를 피하기 위해
//    명시적으로 search() 를 호출해야 결과가 갱신된다(검색 버튼 클릭). → search() = URL 쿼리에 commit.
//  - 페이지네이션은 번호 방식. 페이지 전환 시 results 는 누적이 아니라 교체된다.
//  - 마운트/URL 변경 시 자동 fetch — useEffect 가 searchParams 를 dep 으로 감지.
//  - 카테고리/지역 마스터는 마운트 시 1회 prefetch (모듈 캐시 덕분에 실호출은 세션당 1회).
//
// 상태 분리:
//  - pendingFilters (로컬 useState): 사용자가 폼에 입력 중인 임시 값. "검색" 누르기 전까지는 URL 에 안 들어감.
//  - appliedFilters + page (URL 쿼리): 실제 fetch 기준. URL 이 단일 진실 원천(SSR-like).

import { useCallback, useEffect, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { fetchWelfares } from "../api/welfare";
import { fetchCategories } from "../api/category";
import { getRegions } from "../api/region";

/**
 * 초기 필터 값(빈 폼). 빈 문자열은 baseFetch.buildUrl + URL builder 양쪽에서 자동 제거된다.
 *
 * 지역은 시도(sido) + 시군구(sigungu) 짝으로 관리한다. 백엔드 GET /api/welfares 의 region 쿼리는
 * 자유 텍스트 LIKE 검색이라, 호출 직전에 region = sigungu || sido 로 단일 문자열로 파생한다.
 */
const EMPTY_FILTERS = {
  keyword: "",
  category: "",
  sido: "",
  sigungu: "",
  welfareType: "",
};

const PAGE_SIZE = 10;

/** URLSearchParams 에서 필터 객체 추출 */
function filtersFromUrl(searchParams) {
  return {
    keyword: searchParams.get("keyword") ?? "",
    category: searchParams.get("category") ?? "",
    sido: searchParams.get("sido") ?? "",
    sigungu: searchParams.get("sigungu") ?? "",
    welfareType: searchParams.get("welfareType") ?? "",
  };
}

/** URLSearchParams 에서 0-based page 추출. 누락/음수/NaN 은 0. */
function pageFromUrl(searchParams) {
  const raw = Number(searchParams.get("page"));
  return Number.isFinite(raw) && raw > 0 ? Math.floor(raw) : 0;
}

/**
 * 필터 객체 + page 를 URL 쿼리로 직렬화.
 * 빈 값과 page=0 은 URL 에 적지 않아 깔끔한 주소를 유지한다.
 */
function buildUrlParams(filters, page) {
  const obj = {};
  for (const [k, v] of Object.entries(filters)) {
    if (v) obj[k] = v;
  }
  if (page > 0) obj.page = String(page);
  return obj;
}

/**
 * useWelfareSearch
 *
 * @returns {{
 *   filters: { keyword: string, category: string, sido: string, sigungu: string, welfareType: string },
 *   setFilter: (key: string, value: any) => void,
 *   setRegion: (pair: { sido: string, sigungu: string }) => void,
 *   categories: Array<{ code: string, name: string, type: string }>,
 *   regions: Array<{ sidoCode: string, sidoName: string, sigungus: Array<{ code: string|null, name: string|null }> }>,
 *   results: Array<Object>,
 *   page: number,
 *   totalCount: number,
 *   totalPages: number,
 *   loading: boolean,
 *   error: Error | null,
 *   search: () => void,
 *   goToPage: (n: number) => void,
 *   reset: () => void,
 * }}
 */
export function useWelfareSearch() {
  const [searchParams, setSearchParams] = useSearchParams();
  // searchParams 객체는 매 렌더마다 새 참조라 useEffect dep 으로 직접 쓰면 무한 루프. 문자열로 비교.
  const searchParamsString = searchParams.toString();

  // 임시 폼 값(타이핑 중) — URL 쿼리와는 별도로 로컬에서만 관리
  const [pendingFilters, setPendingFilters] = useState(() =>
    filtersFromUrl(searchParams),
  );

  const [categories, setCategories] = useState([]);
  const [regions, setRegions] = useState([]);
  const [results, setResults] = useState([]);
  const [totalCount, setTotalCount] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // 검색 요청이 중첩되거나 언마운트된 컴포넌트에 setState 하지 않도록 락 + cancel 플래그
  const requestIdRef = useRef(0);

  // 현재 URL 에 적용된 필터/페이지 (fetch 기준)
  const appliedFilters = filtersFromUrl(searchParams);
  const page = pageFromUrl(searchParams);

  /** 백엔드에 보낼 쿼리로 정리. region 은 sigungu 우선, 없으면 sido. */
  const buildSearchQuery = (f, p) => {
    const { sido, sigungu, ...rest } = f;
    const region = sigungu || sido || undefined;
    return { ...rest, region, page: p, size: PAGE_SIZE };
  };

  // 마운트 시 1회: 카테고리 + 지역 prefetch
  useEffect(() => {
    fetchCategories("THEME")
      .then((list) => setCategories(list))
      .catch(() => {
        // 카테고리 실패는 치명적이지 않음 (필터 옵션이 빠질 뿐) — 검색 자체는 계속 진행.
      });
    getRegions()
      .then((tree) => setRegions(tree))
      .catch(() => {
        // 지역 데이터 실패도 치명적이지 않음 — chip 영역이 비어보일 뿐.
      });
  }, []);

  // URL 변경 시 폼 입력칸을 URL 값으로 재동기화 (브라우저 뒤로/앞으로/링크 복귀 등)
  useEffect(() => {
    setPendingFilters(filtersFromUrl(searchParams));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParamsString]);

  // URL 변경 시 fetch 트리거. 이 effect 가 단일 fetch 진입점.
  useEffect(() => {
    const myId = ++requestIdRef.current;
    setLoading(true);
    setError(null);
    fetchWelfares(buildSearchQuery(appliedFilters, page))
      .then((res) => {
        if (myId !== requestIdRef.current) return;
        setResults(res.items);
        setTotalCount(res.totalCount);
        const effectiveSize = res.size || PAGE_SIZE;
        setTotalPages(
          res.totalCount > 0 ? Math.ceil(res.totalCount / effectiveSize) : 0,
        );
      })
      .catch((err) => {
        if (myId !== requestIdRef.current) return;
        setError(err);
      })
      .finally(() => {
        if (myId === requestIdRef.current) setLoading(false);
      });
    // appliedFilters/page 는 searchParamsString 에서 파생이라 dep 으로 그것만 쓰면 충분.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParamsString]);

  const setFilter = useCallback((key, value) => {
    setPendingFilters((prev) => ({ ...prev, [key]: value }));
  }, []);

  /**
   * 시도/시군구 한 번에 갱신. RegionSelector 가 cascading 동작(시도 변경 시 시군구 리셋)을
   * 짝으로 흘려보내므로 호출처가 두 키를 따로 set 할 필요가 없다.
   */
  const setRegion = useCallback(({ sido, sigungu }) => {
    setPendingFilters((prev) => ({ ...prev, sido, sigungu }));
  }, []);

  /** "검색" 클릭 — pendingFilters 를 URL 에 commit, page=0 으로 리셋. */
  const search = useCallback(() => {
    setSearchParams(buildUrlParams(pendingFilters, 0));
  }, [pendingFilters, setSearchParams]);

  /**
   * 페이지 번호 이동(0-based). 로딩 중이거나 범위를 벗어나면 무시.
   * URL 의 page 만 갱신하고 필터는 그대로 둠 (set 함수의 prev 콜백으로 안전하게 머지).
   */
  const goToPage = useCallback(
    (nextPage) => {
      if (loading) return;
      if (nextPage < 0) return;
      if (totalPages > 0 && nextPage >= totalPages) return;
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev);
        if (nextPage > 0) next.set("page", String(nextPage));
        else next.delete("page");
        return next;
      });
    },
    [loading, totalPages, setSearchParams],
  );

  /** "필터 초기화" — URL 을 비워 빈 검색 상태로 복귀. pendingFilters 는 useEffect 가 자동 재동기화. */
  const reset = useCallback(() => {
    setSearchParams({});
    setPendingFilters(EMPTY_FILTERS);
  }, [setSearchParams]);

  return {
    filters: pendingFilters,
    setFilter,
    setRegion,
    categories,
    regions,
    results,
    page,
    totalCount,
    totalPages,
    loading,
    error,
    search,
    goToPage,
    reset,
  };
}
