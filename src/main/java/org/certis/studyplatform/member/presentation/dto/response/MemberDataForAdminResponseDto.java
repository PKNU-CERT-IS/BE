package org.certis.studyplatform.member.presentation.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 관리자용 회원 정보 DTO
 */

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDataForAdminResponseDto {
    private Long memberId;
    private String name;
    private String role;                    // ADMIN, STUDENT, UPSOLVER 등
    private String major;                   // 전공
    private String studentNumber;           // 학번
    private List<String> activeStudies;     // 진행 중인 스터디 이름들
    private List<String> activeProjects;    // 진행 중인 프로젝트 이름들
    private Long penaltyPoints;             // 벌점
    private OffsetDateTime gracePeriod;             // 유예기간
    private String grade;                   // 학년

    @JsonFormat(pattern = "yyyy-MM-dd")
    private String birthday;                // 생년월일

    private String phoneNumber;             // 전화번호
    private String email;                   // 이메일
    private String gender;                  // 성별

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime createdAt;               // 가입일
}
