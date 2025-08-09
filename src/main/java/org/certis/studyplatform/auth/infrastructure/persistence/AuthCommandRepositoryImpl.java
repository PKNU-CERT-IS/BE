package org.certis.studyplatform.auth.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.auth.domain.model.vo.AuthInfoVo;
import org.certis.studyplatform.auth.domain.repository.AuthCommandRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional
public class AuthCommandRepositoryImpl implements AuthCommandRepository {

    private final AuthJpaRepository authJpaRepository;


    @Override
    public void updatePassword(Long memberId, String newEncodedPassword) {
        AuthEntity entity = authJpaRepository.findByMemberId(memberId)
                .orElseThrow(() -> new RuntimeException("해당 회원의 인증정보를 찾을 수 없습니다: memberId=" + memberId));

        entity.setPassword(newEncodedPassword);
    }

    @Override
    public void deleteByMemberId(Long memberId) {
        authJpaRepository.findByMemberId(memberId)
                .ifPresentOrElse(
                        authJpaRepository::delete, // soft delete는 @SQLDelete로 처리됨
                        () -> {
                            throw new RuntimeException("해당 회원의 인증정보를 찾을 수 없습니다: memberId=" + memberId);
                        }
                );
    }
}