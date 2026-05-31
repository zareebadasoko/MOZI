package com.mozi.backend.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * JPA Auditing으로 createdAt/updatedAt을 자동 채우는 추상 엔티티.
 *
 * 모든 도메인 엔티티(User, WelfareCommon, Bookmark 등)는 이 클래스를 상속해
 * 생성/수정 시각을 수동으로 관리하지 않아도 되게 한다. @MappedSuperclass라
 * 별도 테이블이 생기지 않고, 자식 엔티티 테이블에 created_at/updated_at 컬럼이
 * 함께 생성된다.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
// 이 클래스의 역할: 모든 엔티티의 시간 기록을 자동화.
// JpaConfig의 @EnableJpaAuditing이 활성화돼야 실제로 동작한다.
// 자식 엔티티는 setter 없이 영속(persist) 시점에 Spring Data JPA가 자동 주입.
