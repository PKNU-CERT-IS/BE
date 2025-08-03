package org.certis.studyplatform.member.domain.repository;

import org.certis.studyplatform.member.domain.model.Member;
import org.certis.studyplatform.member.domain.model.vo.*;
import java.util.Optional;
import java.util.List;

public interface MemberRepository {
    Optional<Member> findById(MemberIdVo id);
    Optional<Member> findByStudentNumber(StudentNumberVo studentNumber);
    List<Member> findByRole(String role);
    List<Member> findBySkillsContaining(String skill);
    Member save(Member member);
    void deleteById(MemberIdVo id);
}
