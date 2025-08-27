package org.certis.studyplatform.member.domain.vo;

public record SearchKeywordVo(String value) {
    public static SearchKeywordVo of(String keyword) {
        return new SearchKeywordVo(keyword);
    }
}
