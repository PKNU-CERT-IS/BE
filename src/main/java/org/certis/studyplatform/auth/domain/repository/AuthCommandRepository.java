package org.certis.studyplatform.auth.domain.repository;

import org.certis.studyplatform.auth.domain.model.vo.AuthCreationVo;
import org.certis.studyplatform.auth.domain.model.vo.AuthInfoVo;

public interface AuthCommandRepository {


    // 인증 정보 저장 ( 회원 가입 )
    void save(AuthCreationVo authCreationVo);


    // 비밀번호 변경
    void updatePassword(Long memberId, String newEncodedPassword);

    // 회원 탈퇴 (soft delete)
    void deleteByMemberId(Long memberId);

}
