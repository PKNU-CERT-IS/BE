package org.certis.studyplatform.auth.domain.repository;

import org.certis.studyplatform.auth.domain.model.Auth;

public interface AuthCommandRepository {

    // 회원 가입
    void saveAuth(Auth auth);

    // 비밀번호 변경
    void updatePassword(Long memberId, String newEncodedPassword);

    // 회원 탈퇴 (soft delete)
    void deleteByMemberId(Long memberId);

}
