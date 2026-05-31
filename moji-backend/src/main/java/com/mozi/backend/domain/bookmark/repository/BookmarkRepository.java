package com.mozi.backend.domain.bookmark.repository;

import com.mozi.backend.domain.bookmark.entity.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Bookmark 영속성 접근 인터페이스.
 *
 * Phase 4 북마크 API(POST/DELETE/GET)의 모든 경로가 본 인터페이스의 메서드를
 * 호출한다. user_id 인덱스를 활용해 사용자별 페이지 조회는 효율적이며,
 * 복합 UNIQUE 제약 덕에 idempotent 처리도 단순.
 */
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    /**
     * 사용자가 북마크한 복지 페이지 조회.
     *
     * Phase 4-3 GET /api/bookmarks 응답 구성용. 페이지네이션 파라미터로
     * 한번에 너무 많은 row를 반환하지 않도록 한다. @EntityGraph로 welfareCommon을
     * 함께 fetch해 LAZY 추가 쿼리(N+1)를 회피.
     *
     * @param userId 조회 대상 사용자 PK
     * @param pageable 페이지/사이즈
     * @return 해당 사용자의 Bookmark 페이지 (welfareCommon eager fetch)
     */
    @EntityGraph(attributePaths = {"welfareCommon"})
    Page<Bookmark> findByUser_Id(Long userId, Pageable pageable);

    /**
     * 동일 (user, welfare) 조합의 북마크 존재 여부.
     *
     * Phase 4-3 POST /api/bookmarks/{welfareId} 호출 시 idempotent 처리에 사용 +
     * DELETE 시 BOOKMARK_NOT_FOUND 분기에 사용. 이미 존재하면 새 row 저장 없이 200 응답으로 끝낼 수 있다.
     *
     * @param userId 사용자 PK
     * @param welfareId 복지 자연키
     * @return 이미 북마크된 상태면 true
     */
    boolean existsByUser_IdAndWelfareCommon_Id(Long userId, String welfareId);

    /**
     * 동일 (user, welfare) 조합의 북마크 row 조회.
     *
     * Phase 4-3 POST 호출 시 이미 존재하는 북마크의 ID를 응답에 포함시키기 위해 사용.
     * idempotent 흐름에서 신규/기존 모두 동일한 BookmarkCreateResponse 형식을 반환하도록 한다.
     *
     * @param userId 사용자 PK
     * @param welfareId 복지 자연키
     * @return 일치하는 Bookmark (없으면 Optional.empty)
     */
    Optional<Bookmark> findByUser_IdAndWelfareCommon_Id(Long userId, String welfareId);

    /**
     * 단일 (user, welfare) 북마크 삭제.
     *
     * Phase 4-3 DELETE /api/bookmarks/{welfareId} 호출 시 사용. @Modifying은
     * SELECT 외의 변경 쿼리임을 Spring Data JPA에 알린다.
     *
     * @param userId 사용자 PK
     * @param welfareId 복지 자연키
     */
    @Modifying
    void deleteByUser_IdAndWelfareCommon_Id(Long userId, String welfareId);

    /**
     * 사용자가 북마크한 welfare ID 중 주어진 ID 집합에 속하는 것들만 한 번에 조회.
     *
     * Phase 4-2 검색 응답의 isBookmarked 필드를 N+1 없이 채우기 위해 사용한다.
     * 페이지 크기(최대 50) 단위로 한 번의 IN 쿼리로 처리하며, 결과 ID Set과 contains
     * 체크로 각 row의 isBookmarked를 결정.
     *
     * @param userId 인증 사용자 PK
     * @param welfareIds 검사 대상 복지 ID 목록
     * @return 사용자가 북마크한 welfare ID 목록 (subset)
     */
    @Query("select b.welfareCommon.id from Bookmark b where b.user.id = :userId and b.welfareCommon.id in :welfareIds")
    List<String> findWelfareIdsByUser_IdAndWelfareCommon_IdIn(Long userId, Collection<String> welfareIds);
}
// 이 인터페이스의 역할: 북마크 도메인의 단일 영속성 진입점.
// Spring Data JPA 메서드명 규칙으로 쿼리 자동 생성 (별도 @Query 불필요).
