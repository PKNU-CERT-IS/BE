package org.certis.studyplatform.auth.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.auth.domain.model.Auth;
import org.certis.studyplatform.auth.domain.repository.AuthQueryRepository;
import org.certis.studyplatform.member.domain.vo.RoleVo;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static org.jooq.impl.DSL.*;

@Repository
@RequiredArgsConstructor
public class AuthQueryRepositoryImpl implements AuthQueryRepository {

    private final DSLContext dsl;

    @Override
    public Optional<Auth> findByAccountNumber(String accountNumber) {

        return dsl.select(
                        field("a.member_id").as("member_id"),
                        field("a.account_number").as("account_number"),
                        field("a.password").as("password"),
                        field("m.role").as("role")
                )
                .from(table("auth").as("a"))
                .join(table("member").as("m")).on(field("a.member_id").eq(field("m.id")))
                .where(field("a.account_number").eq(accountNumber)
                        .and(field("a.deleted_at").isNull())
                        .and(field("m.deleted_at").isNull()))
                .fetchOptional()
                .map(record -> Auth.createAuthData(
                        record.getValue("member_id", Long.class),
                        record.getValue("account_number", String.class),
                        record.getValue("password", String.class),
                        record.getValue("role", RoleVo.class)
                ));
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

    public Optional<Auth> findByMemberId(Long memberId) {

        return dsl.select(
                        field("a.member_id").as("member_id"),
                        field("a.account_number").as("account_number"),
                        field("a.password").as("password"),
                        field("m.role").as("role")
                )
                .from(table("auth").as("a"))
                .join(table("member").as("m")).on(field("a.member_id").eq(field("m.id")))
                .where(field("a.member_id").eq(memberId)
                        .and(field("a.deleted_at").isNull())
                        .and(field("m.deleted_at").isNull()))
                .fetchOptional()
                .map(record -> Auth.createAuthData(
                        record.getValue("member_id", Long.class),
                        record.getValue("account_number", String.class),
                        record.getValue("password", String.class),
                        record.getValue("role", RoleVo.class)
                ));
    }

    public boolean isActiveMember(Long memberId) {

        return dsl.fetchExists(
                dsl.selectOne()
                        .from(table("member"))
                        .where(field("id").eq(memberId)
                                .and(field("deleted_at").isNull()))
        );
    }
}