package org.certis.studyplatform.auth.infrastructure.mapper;

import org.certis.studyplatform.auth.domain.model.vo.AuthCreationVo;
import org.certis.studyplatform.auth.infrastructure.persistence.entity.AuthEntity;
import org.springframework.stereotype.Component;

@Component
public class AuthCreationMapper {
    public AuthEntity toAuthEntity(AuthCreationVo authCreationVo) {
        return AuthEntity.builder()
                .memberId(authCreationVo.memberId())
                .accountNumber(authCreationVo.accountNumber().accountNumber())
                .password(authCreationVo.encodedPassword().encodedPassword())
                .build();
    }
}
