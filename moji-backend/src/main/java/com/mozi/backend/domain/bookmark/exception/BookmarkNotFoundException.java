package com.mozi.backend.domain.bookmark.exception;

import com.mozi.backend.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * DELETE /api/bookmarks/{welfareId} 호출 시 본인의 북마크 row가 존재하지 않을 때.
 *
 * 복지 자체는 존재하나 사용자가 그 복지를 북마크한 적이 없는 케이스. WELFARE_NOT_FOUND와
 * 명시적으로 분리해 클라이언트가 "복지가 없는 것"인지 "내가 북마크 안 한 것"인지에 따라
 * 다른 안내(예: 토스트 메시지)를 표시할 수 있게 한다.
 */
public class BookmarkNotFoundException extends BusinessException {

    private static final String CODE = "BOOKMARK_NOT_FOUND";
    private static final String MESSAGE = "북마크를 찾을 수 없어요.";

    public BookmarkNotFoundException() {
        super(CODE, MESSAGE, HttpStatus.NOT_FOUND);
    }
}
// 이 클래스의 역할: 북마크 단독 부재 케이스를 WELFARE_NOT_FOUND와 분리해 표현.
