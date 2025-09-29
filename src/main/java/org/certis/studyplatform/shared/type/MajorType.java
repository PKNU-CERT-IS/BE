package org.certis.studyplatform.shared.type;

import lombok.Getter;

@Getter
public enum MajorType {
    // 인문사회과학대학 - 학과
    KOREAN_LITERATURE("korean-literature", "국어국문학과", null, CollegeType.HUMANITIES_SOCIAL, "629-5405"),
    HISTORY("history", "사학과", null, CollegeType.HUMANITIES_SOCIAL, "629-5422"),
    ECONOMICS("economics", "경제학과", null, CollegeType.HUMANITIES_SOCIAL, "629-5312"),
    LAW("law", "법학과", null, CollegeType.HUMANITIES_SOCIAL, "629-5435"),
    CHINESE_STUDIES("chinese-studies", "중국학과", null, CollegeType.HUMANITIES_SOCIAL, "629-7050"),
    POLITICAL_DIPLOMACY("political-diplomacy", "정치외교학과", null, CollegeType.HUMANITIES_SOCIAL, "629-5465"),
    EARLY_CHILDHOOD_EDUCATION("early-childhood-education", "유아교육과", null, CollegeType.HUMANITIES_SOCIAL, "629-5490"),
    FASHION_DESIGN("fashion-design", "패션디자인학과", null, CollegeType.HUMANITIES_SOCIAL, "629-5352"),
    SOCIAL_DIVISION("social-division", "사회계열(경영학과, 중국학과, 정치외교학과)", null, CollegeType.HUMANITIES_SOCIAL, "629-5466"),

    // 영어영문학부
    ENGLISH_LITERATURE("english-literature", "영어영문학전공", DepartmentType.ENGLISH, CollegeType.HUMANITIES_SOCIAL, "629-5371"),
    ENGLISH_CULTURE_INDUSTRY("english-culture-industry", "영어문화·산업전공", DepartmentType.ENGLISH, CollegeType.HUMANITIES_SOCIAL, "629-5370"),

    // 일어일문학부
    JAPANESE_LITERATURE("japanese-literature", "일본어문학전공", DepartmentType.JAPANESE, CollegeType.HUMANITIES_SOCIAL, "629-5390"),
    JAPANESE_STUDIES("japanese-studies", "일본학전공", DepartmentType.JAPANESE, CollegeType.HUMANITIES_SOCIAL, "629-5391"),

    // 행정복지학부
    ADMINISTRATION("administration", "행정학전공", DepartmentType.ADMINISTRATION_WELFARE, CollegeType.HUMANITIES_SOCIAL, "629-5451"),
    SOCIAL_WELFARE("social-welfare", "사회복지학전공", DepartmentType.ADMINISTRATION_WELFARE, CollegeType.HUMANITIES_SOCIAL, "629-5450"),

    // 국제지역학부
    INTERNATIONAL_STUDIES("international-studies", "국제학전공", DepartmentType.INTERNATIONAL_AREA, CollegeType.HUMANITIES_SOCIAL, "629-5332"),
    INTERNATIONAL_DEVELOPMENT("international-development", "국제개발협력학전공", DepartmentType.INTERNATIONAL_AREA, CollegeType.HUMANITIES_SOCIAL, "629-5330"),

    // 자연과학대학 - 학과
    APPLIED_MATHEMATICS("applied-mathematics", "응용수학과", null, CollegeType.NATURAL_SCIENCE, "629-5515"),
    PHYSICS("physics", "물리학과", null, CollegeType.NATURAL_SCIENCE, "629-5545"),
    CHEMISTRY("chemistry", "화학과", null, CollegeType.NATURAL_SCIENCE, "629-5580"),
    MICROBIOLOGY("microbiology", "미생물학과", null, CollegeType.NATURAL_SCIENCE, "629-5610"),
    NURSING("nursing", "간호학과", null, CollegeType.NATURAL_SCIENCE, "629-5780"),
    SCIENCE_COMPUTING("science-computing", "과학컴퓨팅학과", null, CollegeType.NATURAL_SCIENCE, "629-4511"),

    // 경영학부
    BUSINESS_MANAGEMENT("business-management", "경영학전공", DepartmentType.BUSINESS_ADMINISTRATION, CollegeType.BUSINESS, "629-5717"),
    ACCOUNTING_FINANCE("accounting-finance", "회계·재무학전공", DepartmentType.BUSINESS_ADMINISTRATION, CollegeType.BUSINESS, "629-5717"),
    TOURISM_MANAGEMENT("tourism-management", "관광경영학전공", DepartmentType.BUSINESS_ADMINISTRATION, CollegeType.BUSINESS, "629-5717"),
    // 국제통상학부
    INTERNATIONAL_COMMERCE("international-commerce", "국제통상학전공", DepartmentType.BUSINESS_ADMINISTRATION, CollegeType.BUSINESS, "629-5750"),
    INTERNATIONAL_LOGISTICS("international-logistics", "국제무역물류학전공", DepartmentType.INTERNATIONAL_TRADE, CollegeType.BUSINESS, "629-5750"),
    INTERNATIONAL_MANAGEMENT("international-management", "국제경영학전공", DepartmentType.INTERNATIONAL_TRADE, CollegeType.BUSINESS, "629-5750"),

    ARCHITECTURE_ENGINEERING("architecture-engineering", "건축공학과", null, CollegeType.ENGINEERING, "629-6081"),

    // 지속가능공학부
    CIVIL_ENGINEERING("civil-engineering", "토목공학전공", DepartmentType.SUSTAINABLE_ENGINEERING, CollegeType.ENGINEERING, "629-6060"),
    ECOLOGICAL_ENGINEERING("ecological-engineering", "생태공학전공", DepartmentType.SUSTAINABLE_ENGINEERING, CollegeType.ENGINEERING, "629-6540"),

    // 전기공학부
    ELECTRICAL_MAJOR("electrical-major", "전기공학전공", DepartmentType.ELECTRICAL_ENGINEERING, CollegeType.ENGINEERING, "629-6306"),
    CONTROL_MEASUREMENT("control-measurement", "제어계측공학전공", DepartmentType.ELECTRICAL_ENGINEERING, CollegeType.ENGINEERING, "629-6307"),
    DISPLAY_SEMICONDUCTOR("display-semiconductor", "디스플레이반도체공학전공", DepartmentType.ELECTRICAL_ENGINEERING, CollegeType.ENGINEERING, "629-6405"),

    // 공과대학 - 학과
    CHEMICAL_ENGINEERING("chemical-engineering", "화학공학과", null, CollegeType.ENGINEERING, "629-6420"),
    FIRE_PROTECTION("fire-protection", "소방공학과", null, CollegeType.ENGINEERING, "629-6462"),

    // 기계공학부
    MECHANICAL_MAJOR("mechanical-major", "기계공학전공", DepartmentType.MECHANICAL_ENGINEERING, CollegeType.ENGINEERING, "629-6125"),
    MECHANICAL_DESIGN("mechanical-design", "기계설계공학전공", DepartmentType.MECHANICAL_ENGINEERING, CollegeType.ENGINEERING, "629-6121"),

    // 에너지수송시스템공학부
    MECHANICAL_SYSTEM("mechanical-system", "기계시스템공학전공", DepartmentType.ENERGY_MARITIME_SYSTEM, CollegeType.ENGINEERING, "629-6186"),
    HVAC_ENGINEERING("hvac-engineering", "냉동공조공학전공", DepartmentType.ENERGY_MARITIME_SYSTEM, CollegeType.ENGINEERING, "629-6172"),
    NAVAL_ARCHITECTURE("naval-architecture", "조선해양시스템공학전공", DepartmentType.ENERGY_MARITIME_SYSTEM, CollegeType.ENGINEERING, "629-6609"),

    // 고분자·화학소재공학부
    ENERGY_CHEMICAL_MATERIALS("energy-chemical-materials", "에너지화학소재공학전공", DepartmentType.ENGINEERING_CHEMICAL_MATERIALS, CollegeType.ENGINEERING, "629-6422"),
    POLYMER_ENGINEERING("polymer-engineering", "고분자공학전공", DepartmentType.ENGINEERING_CHEMICAL_MATERIALS, CollegeType.ENGINEERING, "629-6424"),

    // 반도체공학부
    NANO_CONVERGENCE("nano-convergence", "나노융합공학전공", DepartmentType.NANO_CONVERGENCE_SEMICONDUCTOR_DEPT, CollegeType.ENGINEERING, "629-6385"),
    NEXT_GEN_SEMICONDUCTOR("next-gen-semiconductor", "차세대반도체공학전공", DepartmentType.NANO_CONVERGENCE_SEMICONDUCTOR_DEPT, CollegeType.ENGINEERING, "629-7890"),

    // 시스템경영·안전공학부
    INDUSTRY_BUSINESS_ENGINEERING("industry-business_engineering", "산업경영공학전공", DepartmentType.SYSTEM_MANAGEMENT_SAFETY, CollegeType.ENGINEERING, "629-6475"),
    TECH_DATA_ENGINEERING("tech-data-engineering", "기술·데이터공학전공", DepartmentType.SYSTEM_MANAGEMENT_SAFETY, CollegeType.ENGINEERING, "629-6475"),
    HUMAN_ENGINEERING("human-engineering", "인전공학전공", DepartmentType.SYSTEM_MANAGEMENT_SAFETY, CollegeType.ENGINEERING, "629-6460"),

    // 융합소재공학부
    METALLURGICAL("metallurgical", "금속공학전공", DepartmentType.CONVERGENCE_MATERIALS, CollegeType.ENGINEERING, "629-6336"),
    MATERIALS_ENGINEERING("materials-engineering", "재료공학전공", DepartmentType.CONVERGENCE_MATERIALS, CollegeType.ENGINEERING, "629-6350"),
    NEW_MATERIALS_SYSTEM("new-materials-system", "신소재시스템공학전공", DepartmentType.CONVERGENCE_MATERIALS, CollegeType.ENGINEERING, "629-6370"),

    // 수산과학대학 - 학과
    BIOTECHNOLOGY("biotechnology", "생물공학과", null, CollegeType.OCEAN_SCIENCE, "629-5861"),
    FISHERIES_LIFE_MEDICINE("fisheries-life-medicine", "수산생명의학과", null, CollegeType.OCEAN_SCIENCE, "629-5935"),

    // 식품과학부
    FOOD_ENGINEERING("food-engineering", "식품공학전공", DepartmentType.FOOD_SCIENCE, CollegeType.OCEAN_SCIENCE, "629-5821"),
    FOOD_NUTRITION("food-nutrition", "식품영양학전공", DepartmentType.FOOD_SCIENCE, CollegeType.OCEAN_SCIENCE, "629-5841"),

    // 해양생산시스템관리학부
    MARINE_PRODUCTION("marine-production", "해양생산학전공", DepartmentType.MARINE_PRODUCTION_SYSTEM, CollegeType.OCEAN_SCIENCE, "629-5882"),
    MARINE_POLICE("marine-police", "해양경찰학전공", DepartmentType.MARINE_PRODUCTION_SYSTEM, CollegeType.OCEAN_SCIENCE, "629-5880"),

    // 수산생명과학부
    AQUACULTURE_APPLIED("aquaculture-applied", "양식응용생명과학전공", DepartmentType.FISHERIES_LIFE_SCIENCE, CollegeType.OCEAN_SCIENCE, "629-5909"),
    AQUATIC_BIOLOGY("aquatic-biology", "자원생물학전공", DepartmentType.FISHERIES_LIFE_SCIENCE, CollegeType.OCEAN_SCIENCE, "629-5930"),

    // 수해양산업교육과
    AQUACULTURE_ENGINEERING("aquaculture-engineering", "양식공학전공", DepartmentType.MARINE_INDUSTRY_EDUCATION, CollegeType.OCEAN_SCIENCE, "629-5965"),
    FISHERIES_ENGINEERING("fisheries-engineering", "어업공학전공", DepartmentType.MARINE_INDUSTRY_EDUCATION, CollegeType.OCEAN_SCIENCE, "629-5965"),
    NAVIGATION_ENGINEERING("navigation-engineering", "항해공학전공", DepartmentType.MARINE_INDUSTRY_EDUCATION, CollegeType.OCEAN_SCIENCE, "629-5965"),
    MARINE_ENGINEERING("marine-engineering", "기관공학전공", DepartmentType.MARINE_INDUSTRY_EDUCATION, CollegeType.OCEAN_SCIENCE, "629-5965"),
    REFRIGERATION_ENGINEERING("refrigeration-engineering", "냉동공학전공", DepartmentType.MARINE_INDUSTRY_EDUCATION, CollegeType.OCEAN_SCIENCE, "629-5965"),
    MARINE_FOOD_ENGINEERING("marine-food-engineering", "식품공학전공", DepartmentType.MARINE_INDUSTRY_EDUCATION, CollegeType.OCEAN_SCIENCE, "629-5965"),

    // 해양수산경영경제학부
    MARINE_MANAGEMENT("marine-management", "해양수산경영학전공", DepartmentType.MARINE_ECONOMICS, CollegeType.OCEAN_SCIENCE, "629-5950"),
    RESOURCE_ECONOMICS("resource-economics", "자원환경경제학전공", DepartmentType.MARINE_ECONOMICS, CollegeType.OCEAN_SCIENCE, "629-5310"),

    // 환경대학 - 학과
    OCEAN_ENGINEERING("ocean-engineering", "해양공학과", null, CollegeType.ENVIRONMENT_OCEAN, "629-6592"),
    ENERGY_RESOURCES("energy-resources", "에너지자원공학과", null, CollegeType.ENVIRONMENT_OCEAN, "629-6551"),

    // 지구환경시스템과학부
    ENVIRONMENTAL_ENGINEERING("environmental-engineering", "환경공학전공", DepartmentType.EARTH_ENVIRONMENTAL_SYSTEM, CollegeType.ENVIRONMENT_OCEAN, "629-6520"),
    OCEANOGRAPHY("oceanography", "해양학전공", DepartmentType.EARTH_ENVIRONMENTAL_SYSTEM, CollegeType.ENVIRONMENT_OCEAN, "629-6565"),
    ENVIRONMENTAL_GEOLOGY("environmental-geology", "환경지질과학전공", DepartmentType.EARTH_ENVIRONMENTAL_SYSTEM, CollegeType.ENVIRONMENT_OCEAN, "629-6621"),
    ENVIRONMENTAL_ATMOSPHERE("environmental-atmosphere", "환경대기과학전공", DepartmentType.EARTH_ENVIRONMENTAL_SYSTEM, CollegeType.ENVIRONMENT_OCEAN, "629-6636"),
    SATELLITE_INFO("satellite-info", "위성정보융합공학전공", DepartmentType.EARTH_ENVIRONMENTAL_SYSTEM, CollegeType.ENVIRONMENT_OCEAN, "629-6650"),

    // 정보융합대학 - 학과
    ARCHITECTURE("architecture", "건축학전공", null, CollegeType.INFO_CONVERGENCE, "629-6080"),
    DIGITAL_FINANCE("digital-finance", "디지털금융학과", null, CollegeType.INFO_CONVERGENCE, "629-7884"),
    SMART_MOBILITY("smart-mobility", "스마트모빌리티디자인학과", null, CollegeType.INFO_CONVERGENCE, "629-7989"),

    // 데이터정보과학부
    BIG_DATA_CONVERGENCE("big-data-convergence", "빅데이터융합전공", DepartmentType.DATA_INFORMATION_SCIENCE, CollegeType.INFO_CONVERGENCE, "629-4610"),
    STATISTICS_DATA_SCIENCE("statistics-data-science", "통계·데이터사이언스전공", DepartmentType.DATA_INFORMATION_SCIENCE, CollegeType.INFO_CONVERGENCE, "629-5534"),

    // 미디어커뮤니케이션학부
    JOURNALISM("journalism", "언론정보전공", DepartmentType.MEDIA_COMMUNICATION, CollegeType.INFO_CONVERGENCE, "629-5475"),
    HUMAN_ICT("human-ict", "휴먼ICT융합전공", DepartmentType.MEDIA_COMMUNICATION, CollegeType.INFO_CONVERGENCE, "629-4620"),

    // 스마트헬스케어학부
    BIOMEDICAL_ENGINEERING("biomedical-engineering", "의공학전공", null, CollegeType.INFO_CONVERGENCE, "629-5872"),
    HUMAN_BIO_CONVERGENCE("human-bio-convergence", "휴먼바이오융합전공", DepartmentType.SMART_HEALTHCARE, CollegeType.INFO_CONVERGENCE, "629-4630"),
    MARINE_SPORTS("marine-sports", "해양스포츠전공", DepartmentType.SMART_HEALTHCARE, CollegeType.INFO_CONVERGENCE, "629-5630"),

    // 전자정보통신공학부
    ELECTRONICS("electronics", "전자공학전공", DepartmentType.ELECTRONICS_COMMUNICATION, CollegeType.INFO_CONVERGENCE, "629-6206"),
    INFORMATION_COMMUNICATION("information-communication", "정보통신공학전공", DepartmentType.ELECTRONICS_COMMUNICATION, CollegeType.INFO_CONVERGENCE, "629-6207"),

    // 조형학부
    VISUAL_DESIGN("visual-design", "시각디자인전공", DepartmentType.DESIGN, CollegeType.INFO_CONVERGENCE, "629-5350"),
    INDUSTRIAL_DESIGN("industrial-design", "공업디자인전공", DepartmentType.DESIGN, CollegeType.INFO_CONVERGENCE, "629-5351"),

    // 컴퓨터·인공지능공학부
    COMPUTER_ENGINEERING("computer-engineering", "컴퓨터공학전공", DepartmentType.COMPUTER_AI, CollegeType.INFO_CONVERGENCE, "629-6261"),
    ARTIFICIAL_INTELLIGENCE("artificial-intelligence", "인공지능전공", DepartmentType.COMPUTER_AI, CollegeType.INFO_CONVERGENCE, "629-6262"),

    // 미래융합학부
    LIFELONG_COUNSELING("lifelong-counseling", "평생교육상담학전공", DepartmentType.FUTURE_CONVERGENCE_DEPT, CollegeType.FUTURE_CONVERGENCE, "629-6595"),
    POLICE_CRIME_PSYCHOLOGY("police-crime-psychology", "경찰범죄심리학전공", DepartmentType.FUTURE_CONVERGENCE_DEPT, CollegeType.FUTURE_CONVERGENCE, "629-6595"),
    SOCIAL_WELFARE_SERVICE("social-welfare-service", "사회복지서비스학전공", DepartmentType.FUTURE_CONVERGENCE_DEPT, CollegeType.FUTURE_CONVERGENCE, "629-6595"),
    MECHANICAL_SHIPBUILDING("mechanical-shipbuilding", "기계조선공학전공", DepartmentType.FUTURE_CONVERGENCE_DEPT, CollegeType.FUTURE_CONVERGENCE, "629-6596"),
    ELECTRICAL_SOFTWARE("electrical-software", "전기전자SW공학전공", DepartmentType.FUTURE_CONVERGENCE_DEPT, CollegeType.FUTURE_CONVERGENCE, "629-6596"),

    GLOBAL_LIBERAL("global-liberal", "글로벌자율전공학부", null,CollegeType.GLOBAL_LIBERAL,  "629-6650"),
    STUDENT_AFFAIRS("student-affairs", "학부대학 자유전공학부", null,  CollegeType.GLOBAL_LIBERAL,"629-7542");

    private final String id;
    private final String name;
    private final DepartmentType department;
    private final CollegeType college;
    private final String phone;

    MajorType(String id, String name, DepartmentType department, CollegeType college, String phone) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.college = college;
        this.phone = phone;
    }
}