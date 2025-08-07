package org.certis.studyplatform.member.domain.mapper;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.member.domain.Member;
import org.certis.studyplatform.member.domain.vo.*;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

/**
 * VO To Domain Mapper
 * 
 * ✅ VO → Domain Entity 변환 담당
 * ✅ Domain Layer의 VO → Domain 전용 매퍼
 * ✅ 네이밍 컨벤션: VoToDomainMapper
 */
@Component
@RequiredArgsConstructor
public class VoToDomainMapper {

    /**
     * VO → Member Domain Entity 변환 (새로 생성)
     */
    public Member toMember(String name, String studentNumber, String grade,
                          String role, String major, String description, 
                          java.util.List<String> skills) {
        return new Member(name, studentNumber, grade, skills, role, major);
    }



    /**
     * VO → Member Domain Entity 변환 (Factory Method 사용)
     */
    public Member toMember(String name, String studentNumber, String grade,
                          java.util.List<String> skills, String role, String major) {
        return Member.create(name, studentNumber, grade, skills, role, major);
    }

    /**
     * VO → Member Domain Entity 변환 (ID 포함)
     */
    public Member toMember(Long id, NameVo name, StudentNumberVo studentNumber, 
                          GradeVo grade, RoleVo role, SkillsVo skills, MajorVo major,
                          ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        return new Member(id, name, studentNumber, null, grade, role, skills, major, 
                         null, createdAt, updatedAt);
    }

    /**
     * VO → Member Domain Entity 변환 (프로필 이미지 포함)
     */
    public Member toMember(Long id, NameVo name, StudentNumberVo studentNumber, 
                          ProfileImageVo profileImage, GradeVo grade, RoleVo role, 
                          SkillsVo skills, MajorVo major, String description,
                          ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        return new Member(id, name, studentNumber, profileImage, grade, role, 
                         skills, major, description, createdAt, updatedAt);
    }
} 