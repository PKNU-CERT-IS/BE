package org.certis.studyplatform.shared.type;

import lombok.Getter;

@Getter
public enum DepartmentType {
    // 인문사회과학대학
    FREE_MAJOR_HS("free-major-hs", "인문사회과학대학 자유전공학부", CollegeType.HUMANITIES_SOCIAL, "629-5433"),
    ENGLISH("english", "영어영문학부", CollegeType.HUMANITIES_SOCIAL, "629-5371"),
    JAPANESE("japanese", "일어일문학부", CollegeType.HUMANITIES_SOCIAL, "629-5390"),
    ADMINISTRATION_WELFARE("administration-welfare", "행정복지학부", CollegeType.HUMANITIES_SOCIAL, "629-5606"),
    INTERNATIONAL_AREA("international-area", "국제지역학부", CollegeType.HUMANITIES_SOCIAL, "629-5330"),

    // 자연과학대학
    FREE_MAJOR_NS("free-major-ns", "자연과학대학 자유전공학부", CollegeType.NATURAL_SCIENCE, "629-5507"),

    // 경영대학
    FREE_MAJOR_BUS("free-major-bus", "경영대학 자유전공학부", CollegeType.BUSINESS, "629-5709"),
    BUSINESS_ADMINISTRATION("business-administration", "경영학부", CollegeType.BUSINESS, "629-5717"),
    INTERNATIONAL_TRADE("international-trade", "국제통상학부", CollegeType.BUSINESS, "629-5750"),

    // 공과대학
    FREE_MAJOR_ENG("free-major-eng", "공과대학 자유전공학부", CollegeType.ENGINEERING, "629-6013"),
    SUSTAINABLE_ENGINEERING("sustainable-engineering", "지속가능공학부", CollegeType.ENGINEERING, null),
    ELECTRICAL_ENGINEERING("electrical-engineering", "전기공학부", CollegeType.ENGINEERING, null),
    MECHANICAL_ENGINEERING("mechanical-engineering", "기계공학부", CollegeType.ENGINEERING, null),
    ENERGY_MARITIME_SYSTEM("energy-maritime-system", "에너지 수송시스템공학부", CollegeType.ENGINEERING, null),
    ENGINEERING_CHEMICAL_MATERIALS("engineering-chemical-materials", "고분자·화학소재공학부", CollegeType.ENGINEERING, null),
    NANO_CONVERGENCE_SEMICONDUCTOR_DEPT("nano-convergence-dept", "나노융합반도체공학부", CollegeType.ENGINEERING, null),
    SYSTEM_MANAGEMENT_SAFETY("system-management-safety", "시스템경영·안전공학부", CollegeType.ENGINEERING, null),
    CONVERGENCE_MATERIALS("convergence-materials", "융합소재공학부", CollegeType.ENGINEERING, null),

    // 수산과학대학
    FOOD_SCIENCE("food-science", "식품과학부", CollegeType.OCEAN_SCIENCE, null),
    MARINE_PRODUCTION_SYSTEM("marine-production-system", "해양생산시스템관리학부", CollegeType.OCEAN_SCIENCE, "629-5886"),
    FISHERIES_LIFE_SCIENCE("fisheries-life-science", "수산생명과학부", CollegeType.OCEAN_SCIENCE, null),
    MARINE_INDUSTRY_EDUCATION("marine-industry-education", "수해양산업교육과", CollegeType.OCEAN_SCIENCE, null),
    MARINE_ECONOMICS("marine-economics", "해양수산경영경제학부", CollegeType.OCEAN_SCIENCE, null),

    // 환경대학
    FREE_MAJOR_ENV("free-major-env", "환경·해양대학 자유전공학부", CollegeType.ENVIRONMENT_OCEAN, null),
    EARTH_ENVIRONMENTAL_SYSTEM("earth-environmental-system", "지구환경시스템과학부", CollegeType.ENVIRONMENT_OCEAN, null),

    // 정보융합대학
    FREE_MAJOR_IC("free-major-ic", "정보융합대학 자유전공학부", CollegeType.INFO_CONVERGENCE, null),
    DATA_INFORMATION_SCIENCE("data-information-science", "데이터정보과학부", CollegeType.INFO_CONVERGENCE, null),
    MEDIA_COMMUNICATION("media-communication", "미디어커뮤니케이션학부", CollegeType.INFO_CONVERGENCE, null),
    SMART_HEALTHCARE("smart-healthcare", "스마트헬스케어학부", CollegeType.INFO_CONVERGENCE, null),
    ELECTRONICS_COMMUNICATION("electronics-communication", "전자정보통신공학부", CollegeType.INFO_CONVERGENCE, null),
    DESIGN("design", "조형학부", CollegeType.INFO_CONVERGENCE, null),
    COMPUTER_AI("computer-ai", "컴퓨터·인공지능공학부", CollegeType.INFO_CONVERGENCE, null),

    // 미래융합학부
    FUTURE_CONVERGENCE_DEPT("future-convergence-dept", "미래융합학부", CollegeType.FUTURE_CONVERGENCE, null);;
    private final String id;
    private final String name;
    private final CollegeType college;
    private final String phone;

    DepartmentType(String id, String name, CollegeType college, String phone) {
        this.id = id;
        this.name = name;
        this.college = college;
        this.phone = phone;
    }

}