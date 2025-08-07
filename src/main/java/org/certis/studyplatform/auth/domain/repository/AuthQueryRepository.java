package org.certis.studyplatform.auth.domain.repository;

import org.certis.studyplatform.auth.domain.model.Auth;

import java.util.Optional;

public interface AuthQueryRepository {

        // 인증 정보 ( 회원ID, 계정번호, 비밀번호, role )
        Optional<Auth> findByAccountNumber(String accountNumber);

        // 계정 번호 존재 여부
        boolean existsByAccountNumber(String accountNumber);

        // 계정번호로 회원 존재 여부 판단
        Optional<Auth> findByMemberId(Long memberId);
}
