package org.certis.studyplatform.auth.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.auth.domain.model.vo.AccountNumberVo;
import org.certis.studyplatform.auth.domain.model.vo.AuthInfoVo;
import org.certis.studyplatform.auth.domain.model.vo.EncodedPasswordVo;
import org.certis.studyplatform.auth.domain.repository.AuthQueryRepository;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.vo.RoleVo;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthQueryRepositoryImpl implements AuthQueryRepository {

    private final DSLContext dsl;

    @Override
    public Optional<AuthInfoVo> findByAccountNumber(AccountNumberVo accountNumberVo) {
        return dsl.select(
                        field("a.member_id").as("member_id"),
                        field("a.account_number").as("account_number"),
                        field("a.password").as("password"),
                        field("m.role").as("role")
                )
                .from(table("auth").as("a"))
                .join(table("member").as("m")).on(field("a.member_id").eq(field("m.id")))
                .where(field("a.account_number").eq(accountNumberVo.accountNumber())
                        .and(field("a.deleted_at").isNull())
                        .and(field("m.deleted_at").isNull()))
                .fetchOptional()
                .map(record -> {
                    String roleStr = record.getValue("role", String.class);
                    MemberRole role = MemberRole.valueOf(roleStr);
                    return AuthInfoVo.of(
                            record.getValue("member_id", Long.class),
                            record.getValue("account_number", String.class),
                            record.getValue("password", String.class),
                           role
                    );});
    }


    @Override
    public boolean existsByAccountNumber(AccountNumberVo accountNumberVo) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(table("auth"))
                        .where(field("account_number").eq(accountNumberVo.accountNumber())
                                .and(field("deleted_at").isNull()))
        );
    }

    private LocalDateTime convertToLocalDateTime(Object timestampObj) {
        if (timestampObj == null) {
            return null;
        }

        if (timestampObj instanceof Timestamp) {
            return ((Timestamp) timestampObj).toLocalDateTime();
        }

        if (timestampObj instanceof LocalDateTime) {
            return (LocalDateTime) timestampObj;
        }

        // 다른 타입인 경우 예외 발생
        throw new IllegalArgumentException("Cannot convert " + timestampObj.getClass() + " to LocalDateTime");
    }
}