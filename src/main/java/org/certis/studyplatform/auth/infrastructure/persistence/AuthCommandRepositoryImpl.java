package org.certis.studyplatform.auth.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.auth.domain.model.vo.AuthCreationVo;
import org.certis.studyplatform.auth.domain.repository.AuthCommandRepository;
import org.certis.studyplatform.auth.infrastructure.mapper.AuthCreationMapper;
import org.certis.studyplatform.auth.infrastructure.persistence.entity.AuthEntity;
import org.certis.studyplatform.auth.infrastructure.persistence.jpa.AuthJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional
public class AuthCommandRepositoryImpl implements AuthCommandRepository {

    private final AuthJpaRepository authJpaRepository;
    private final AuthCreationMapper authCreationMapper;

    @Override
    public void save(AuthCreationVo authCreationVo) {
        log.info("Infrastructure: Starting auth creation for memberId: {}", authCreationVo.memberId());

        AuthEntity entityForSave =  authCreationMapper.toAuthEntity(authCreationVo);

        authJpaRepository.save(entityForSave);

    }
}