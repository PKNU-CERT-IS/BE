package org.certis.studyplatform.auth.domain.repository;

import org.certis.studyplatform.auth.domain.model.vo.AccountNumberVo;
import org.certis.studyplatform.auth.domain.model.vo.AuthInfoVo;

import java.util.Optional;

public interface AuthQueryRepository {

        // 인증 정보 ( 회원ID, 계정번호, 비밀번호, role )
        Optional<AuthInfoVo> findByAccountNumber(AccountNumberVo accountNumberVo);

        // 계정 번호 존재 여부
        boolean existsByAccountNumber(AccountNumberVo accountNumberVo);

}
