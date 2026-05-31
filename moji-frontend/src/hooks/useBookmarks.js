// hooks/useBookmarks.js
//
// 북마크 목록 페이지(/bookmarks) 의 상태 관리. useWelfareSearch 의 미니 버전.
//  - 필터는 없고 page 만 URL 쿼리에 보관
//  - URL 변경 시 useEffect 가 자동 fetch (단일 진입점)
//  - removeBookmark: 낙관적 제거 + 실패 시 복원
//  - 페이지 마지막 항목 해제 시 자동으로 이전 페이지로 (노년층 UX)

import { useCallback, useEffect, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { fetchBookmarks, deleteBookmark } from "../api/bookmark";
import { useToast } from "./useToast";

const PAGE_SIZE = 10;

/** URLSearchParams 에서 0-based page 추출. 누락/음수/NaN 은 0. */
function pageFromUrl(searchParams) {
  const raw = Number(searchParams.get("page"));
  return Number.isFinite(raw) && raw > 0 ? Math.floor(raw) : 0;
}

/**
 * useBookmarks
 *
 * @returns {{
 *   items: Array<Object>,
 *   page: number,
 *   totalCount: number,
 *   totalPages: number,
 *   loading: boolean,
 *   error: Error | null,
 *   goToPage: (n: number) => void,
 *   removeBookmark: (welfareId: string) => Promise<void>,
 * }}
 */
export function useBookmarks() {
  const [searchParams, setSearchParams] = useSearchParams();
  const searchParamsString = searchParams.toString();
  const page = pageFromUrl(searchParams);

  const [items, setItems] = useState([]);
  const [totalCount, setTotalCount] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const { showToast } = useToast();
  // 검색 요청이 중첩되거나 언마운트 후 setState 하지 않도록 가드
  const requestIdRef = useRef(0);

  // URL 변경 시 fetch — 마운트 포함 단일 진입점
  useEffect(() => {
    const myId = ++requestIdRef.current;
    setLoading(true);
    setError(null);
    fetchBookmarks({ page, size: PAGE_SIZE })
      .then((res) => {
        if (myId !== requestIdRef.current) return;
        setItems(res.items);
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParamsString]);

  /**
   * 페이지 번호 이동(0-based). URL 의 page 만 갱신, 페이지=0 일 땐 URL 에서 제거해 주소 깨끗하게.
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

  /**
   * 북마크 해제 — 낙관적 제거.
   * BOOKMARK_NOT_FOUND 는 이미 해제된 상태라 무음 처리. 그 외 실패 시 카드 복원 + 에러 토스트.
   * 제거 후 현재 페이지가 빈데 page>0 이면 이전 페이지로 자동 이동(빈 페이지 회피).
   */
  const removeBookmark = useCallback(
    async (welfareId) => {
      const removed = items.find((it) => it.id === welfareId);
      if (!removed) return;
      const remaining = items.filter((it) => it.id !== welfareId);
      setItems(remaining);
      try {
        await deleteBookmark(welfareId);
        showToast({ kind: "success", message: "저장이 해제되었어요." });
        // 페이지가 비고 이전 페이지가 있으면 자동 이동 (다음 fetch 가 정확한 count 로 보정)
        if (remaining.length === 0 && page > 0) {
          goToPage(page - 1);
        }
      } catch (err) {
        if (err.errorCode === "BOOKMARK_NOT_FOUND") {
          // 이미 해제된 상태 — 사용자에게 알릴 필요 없음, UI 는 이미 제거됨으로 일치
          if (remaining.length === 0 && page > 0) {
            goToPage(page - 1);
          }
          return;
        }
        // 그 외 실패 → 카드 복원 + 안내
        setItems(items);
        showToast({
          kind: "error",
          message: err.message || "잠시 후 다시 시도해주세요.",
        });
      }
    },
    [items, page, goToPage, showToast],
  );

  return {
    items,
    page,
    totalCount,
    totalPages,
    loading,
    error,
    goToPage,
    removeBookmark,
  };
}
