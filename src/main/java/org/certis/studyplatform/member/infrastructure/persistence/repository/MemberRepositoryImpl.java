package org.certis.studyplatform.member.infrastructure.persistence.repository;

import org.certis.studyplatform.member.domain.model.Member;
import org.certis.studyplatform.member.domain.repository.MemberRepository;
import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberEntity;
import org.certis.studyplatform.member.infrastructure.jpa.MemberJpaRepository;
import org.certis.studyplatform.member.domain.model.vo.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MemberRepositoryImpl implements MemberRepository {

    private final MemberJpaRepository jpaRepository;

    public MemberRepositoryImpl(MemberJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Member> findById(MemberIdVo id) {
        return jpaRepository.findById(id.value())
                .map(MemberEntity::toDomain);
    }

    @Override
    public Optional<Member> findByStudentNumber(StudentNumberVo studentNumberVo) {
        return jpaRepository.findByStudentNumber(studentNumberVo.value())
                .map(MemberEntity::toDomain);
    }

    @Override
    public List<Member> findByRole(String role) {
        return jpaRepository.findByRole(role).stream()
                .map(MemberEntity::toDomain)
                .toList();
    }

    @Override
    public List<Member> findBySkillsContaining(String skill) {
        return jpaRepository.findBySkillsContaining(skill).stream()
                .map(MemberEntity::toDomain)
                .toList();
    }

    @Override
    public Member save(Member member) {
        MemberEntity entity = MemberEntity.fromDomain(member);
        MemberEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void deleteById(MemberId id) {
        jpaRepository.deleteById(id.value());
    }
}