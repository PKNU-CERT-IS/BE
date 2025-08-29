package org.certis.studyplatform.member.domain.service;

import org.certis.studyplatform.member.domain.vo.ProfileStudyVo;
import org.certis.studyplatform.member.domain.vo.ProfileProjectVo;
import org.certis.studyplatform.shared.util.GracePeriodCalculator;
import org.certis.studyplatform.shared.util.GracePeriodCalculator.ActivityInfo;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 유예기간 계산 서비스
 *
 * VO를 ActivityInfo로 변환하여 shared 유틸리티를 사용
 */
@Service
public class GracePeriodCalculationService {

    /**
     * 스터디와 프로젝트 목록을 기반으로 유예기간을 계산
     *
     * @param studies 사용자의 스터디 목록
     * @param projects 사용자의 프로젝트 목록
     * @return 계산된 유예기간 종료 날짜 (null이면 유예기간 없음)
     */
    public OffsetDateTime calculateGracePeriod(List<ProfileStudyVo> studies, List<ProfileProjectVo> projects) {
        List<ActivityInfo> activities = new ArrayList<>();

        // 스터디를 ActivityInfo로 변환
        for (ProfileStudyVo study : studies) {
            if (study.studyStartDate() != null && study.studyEndDate() != null) {
                boolean isOngoing = study.isInProgress() || study.isReady();
                activities.add(new ActivityInfo(
                    study.studyStartDate(),
                    study.studyEndDate(),
                    isOngoing
                ));
            }
        }

        // 프로젝트를 ActivityInfo로 변환
        for (ProfileProjectVo project : projects) {
            if (project.projectStartDate() != null && project.projectEndDate() != null) {
                boolean isOngoing = project.isInProgress() || project.isReady();
                activities.add(new ActivityInfo(
                    project.projectStartDate(),
                    project.projectEndDate(),
                    isOngoing
                ));
            }
        }

        // 유틸리티를 사용하여 유예기간 계산
        return GracePeriodCalculator.calculateGracePeriod(activities);
    }
}