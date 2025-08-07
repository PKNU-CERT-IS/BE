package org.certis.studyplatform.auth.infrastructure.mapper;

import org.certis.studyplatform.auth.domain.model.Auth;
import org.certis.studyplatform.auth.domain.model.vo.AccountNumberVo;
import org.certis.studyplatform.auth.domain.model.vo.EncodedPasswordVo;
import org.certis.studyplatform.auth.infrastructure.persistence.AuthEntity;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.vo.RoleVo;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public AuthEntity toEntity(Auth auth){
        return AuthEntity.builder()
                .memberId(auth.getMemberId())
                .accountNumber(auth.getAccountNumberVo().accountNumber())
                .password(auth.getEncodedPasswordVo().encodedPassword())
                .build();
    }

    public Auth toDomain(AuthEntity authEntity, MemberRole role){
        return Auth.builder()
                .memberId(authEntity.getMemberId())
                .accountNumberVo(AccountNumberVo.of(authEntity.getAccountNumber()))
                .encodedPasswordVo(EncodedPasswordVo.of(authEntity.getPassword()))
                .roleVo(RoleVo.of(role))
                .createdAt(authEntity.getCreatedAt().toLocalDateTime())
                .updatedAt(authEntity.getUpdatedAt().toLocalDateTime())
                .deletedAt(authEntity.getDeletedAt() != null ? authEntity.getDeletedAt().toLocalDateTime() : null)
                .build();
    }
}
