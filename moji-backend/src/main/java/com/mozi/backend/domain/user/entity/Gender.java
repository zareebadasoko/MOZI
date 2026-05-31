package com.mozi.backend.domain.user.entity;

/**
 * 사용자 성별 enum.
 *
 * UserProfile의 선택 입력값으로, 노년층 사용자가 자신의 성별을 밝히지 않을
 * 권리를 보장하기 위해 NONE(밝히지 않음)을 명시적으로 둔다. nullable이지만
 * 입력하지 않은 상태와 NONE 선택 상태를 구분하기 위함.
 */
public enum Gender {
    M,
    F,
    NONE
}
// 이 enum의 역할: 성별 표현 + "밝히지 않음" 선택지.
// null = 입력 자체를 안 한 상태 / NONE = 사용자가 명시적으로 비공개 선택.
