package org.certis.studyplatform.shared.type;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum CollegeType {
    HUMANITIES_SOCIAL("humanities-social", "인문사회과학대학", "629-5303"),
    NATURAL_SCIENCE("natural-science", "자연과학대학", "629-5506"),
    BUSINESS("business", "경영대학", "629-5703"),
    ENGINEERING("engineering", "공과대학", "629-6009"),
    OCEAN_SCIENCE("ocean-science", "수산과학대학", "629-5803"),
    ENVIRONMENT_OCEAN("environment-ocean", "환경·해양대학", "629-6506"),
    INFO_CONVERGENCE("info-convergence", "정보융합대학", "629-4607"),
    FUTURE_CONVERGENCE("future-convergence", "미래융합학부", "629-6601"),
    GLOBAL_LIBERAL("global-liberal", "글로벌자율전공학부", "629-6650"),
    STUDENT_AFFAIRS("student-affairs", "학부대학 자유전공학부", "629-7542");

    private final String id;
    private final String name;
    private final String phone;

    CollegeType(String id, String name, String phone) {
        this.id = id;
        this.name = name;
        this.phone = phone;
    }

}