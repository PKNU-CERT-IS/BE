package org.certis.studyplatform.board.domain.model.vo;

import org.certis.studyplatform.member.domain.MemberRole;

public record BoardAuthorInfoVo(String name, MemberRole role, String profileImageUrl) {}