package org.certis.studyplatform.auth.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.auth.domain.model.Auth;
import org.certis.studyplatform.auth.domain.model.vo.AccountNumberVo;
import org.certis.studyplatform.auth.domain.model.vo.EncodedPasswordVo;
import org.certis.studyplatform.auth.domain.repository.AuthQueryRepository;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.vo.RoleVo;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthQueryRepositoryImpl implements AuthQueryRepository {

    private final DSLContext dsl;

    @Override
    public Optional<Auth> findByAccountNumber(String accountNumber) {
        return dsl.select(
                        field("a.member_id").as("member_id"),
                        field("a.account_number").as("account_number"),
                        field("a.password").as("password"),
                        field("m.role").as("role"),
                        field("a.created_at").as("created_at"),
                        field("a.updated_at").as("updated_at"),
                        field("a.deleted_at").as("deleted_at")
                )
                .from(table("auth").as("a"))
                .join(table("member").as("m")).on(field("a.member_id").eq(field("m.id")))
                .where(field("a.account_number").eq(accountNumber)
                        .and(field("a.deleted_at").isNull())
                        .and(field("m.deleted_at").isNull()))
                .fetchOptional()
                .map(record -> Auth.builder()
                        .memberId(record.getValue("member_id", Long.class))
                        .accountNumberVo(AccountNumberVo.of(record.getValue("account_number", String.class)))
                        .encodedPasswordVo(EncodedPasswordVo.of(record.getValue("password", String.class)))
                        .roleVo(RoleVo.of(record.getValue("role", MemberRole.class)))
                        .createdAt(record.getValue("created_at", LocalDateTime.class))
                        .updatedAt(record.getValue("updated_at", LocalDateTime.class))
                        .deletedAt(record.getValue("deleted_at", LocalDateTime.class))
                        .build());
    }

    @Override
    public Optional<Auth> findByMemberId(Long memberId) {
        return dsl.select(
                        field("a.member_id").as("member_id"),
                        field("a.account_number").as("account_number"),
                        field("a.password").as("password"),
                        field("m.role").as("role"),
                        field("a.created_at").as("created_at"),
                        field("a.updated_at").as("updated_at"),
                        field("a.deleted_at").as("deleted_at")
                )
                .from(table("auth").as("a"))
                .join(table("member").as("m")).on(field("a.member_id").eq(field("m.id")))
                .where(field("a.member_id").eq(memberId)
                        .and(field("a.deleted_at").isNull())
                        .and(field("m.deleted_at").isNull()))
                .fetchOptional()
                .map(record -> Auth.builder()
                        .memberId(record.getValue("member_id", Long.class))
                        .accountNumberVo(AccountNumberVo.of(record.getValue("account_number", String.class)))
                        .encodedPasswordVo(EncodedPasswordVo.of(record.getValue("password", String.class)))
                        .roleVo(RoleVo.of(record.getValue("role", MemberRole.class)))
                        .createdAt(record.getValue("created_at", LocalDateTime.class))
                        .updatedAt(record.getValue("updated_at", LocalDateTime.class))
                        .deletedAt(record.getValue("deleted_at", LocalDateTime.class))
                        .build());
    }

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(table("auth"))
                        .where(field("account_number").eq(accountNumber)
                                .and(field("deleted_at").isNull()))
        );
    }
}