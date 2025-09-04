package org.certis.studyplatform.member.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.member.domain.repository.command.MemberContactCommandRepository;
import org.certis.studyplatform.member.domain.vo.MemberContactVo;
import org.certis.studyplatform.member.infrastructure.mapper.MemberInfrastructureEntityMapper;
import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberContactEntity;
import org.certis.studyplatform.member.infrastructure.persistence.jpa.MemberContactJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberContactRepositoryImpl implements MemberContactCommandRepository {
    private final MemberContactJpaRepository memberContactJpaRepository;
    private final MemberInfrastructureEntityMapper memberInfrastructureEntityMapper;

    @Override
    public void createContact(MemberContactVo vo) {
        MemberContactEntity entity = memberInfrastructureEntityMapper.toEntity(vo);

        memberContactJpaRepository.save(entity);
    }
}

