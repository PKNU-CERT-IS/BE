package org.certis.studyplatform.auth.domain.model.vo;

import java.time.LocalDateTime;
public record AccessTokenVo(String value, LocalDateTime expiredAt) {

}
