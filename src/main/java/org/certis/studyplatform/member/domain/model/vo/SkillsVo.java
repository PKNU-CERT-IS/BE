package org.certis.studyplatform.member.domain.model.vo;

import java.util.List;

public record SkillsVo(List<String> values) {
    public SkillsVo {
        if (values == null) {
            throw new IllegalArgumentException("스킬 목록은 null일 수 없습니다");
        }
    }

    public String[] toArray() {
        return values.toArray(new String[0]);
    }

    public static SkillsVo of(String... skills) {
        return new SkillsVo(List.of(skills));
    }

    public static SkillsVo of(List<String> skills) {
        return new SkillsVo(List.copyOf(skills));
    }

    public static SkillsVo empty() {
        return new SkillsVo(List.of());
    }
}