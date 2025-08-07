package org.certis.studyplatform.auth.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.auth.domain.model.Auth;
import org.certis.studyplatform.auth.domain.repository.AuthCommandRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.ZonedDateTime;

@Repository
@RequiredArgsConstructor
@Transactional
public class AuthCommandRepositoryImpl implements AuthCommandRepository {

    private final EntityManager entityManager;

    @Override
    public void saveAuth(Auth authAccount) {

        String sql = """
            INSERT INTO auth (member_id, account_number, password, created_at, updated_at) 
            VALUES (:memberId, :accountNumber, :password, :createdAt, :updatedAt)
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("memberId", authAccount.getMemberId());
        query.setParameter("accountNumber", authAccount.getAccountNumberVo().accountNumber());
        query.setParameter("password", authAccount.getEncodedPasswordVo().encodedPassword());
        query.setParameter("createdAt", ZonedDateTime.now());
        query.setParameter("updatedAt", ZonedDateTime.now());

        int result = query.executeUpdate();

        if (result == 0) {
            // 예외 처리
        }

    }

    @Override
    public void updatePassword(Long memberId, String newEncodedPassword) {

        String sql = """
            UPDATE auth
            SET password = :newPassword, updated_at = :updatedAt
            WHERE member_id = :memberId AND deleted_at IS NULL
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("newPassword", newEncodedPassword);
        query.setParameter("updatedAt", ZonedDateTime.now());
        query.setParameter("memberId", memberId);

        int result = query.executeUpdate();

        if (result == 0) {
            throw new RuntimeException("해당 회원의 인증정보를 찾을 수 없습니다: memberId=" + memberId);
        }

    }

    @Override
    public void deleteByMemberId(Long memberId) {

        String sql = """
            UPDATE auth
            SET deleted_at = :deletedAt
            WHERE member_id = :memberId AND deleted_at IS NULL
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("deletedAt", ZonedDateTime.now());
        query.setParameter("memberId", memberId);

        int result = query.executeUpdate();

        if (result == 0) {
            throw new RuntimeException("해당 회원의 인증정보를 찾을 수 없습니다: memberId=" + memberId);
        }
    }
}