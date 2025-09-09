package org.certis.studyplatform.member.domain.repository.command;

import org.certis.studyplatform.member.domain.vo.MemberContactVo;

public interface MemberContactCommandRepository {
     void createContact(MemberContactVo vo);
}
