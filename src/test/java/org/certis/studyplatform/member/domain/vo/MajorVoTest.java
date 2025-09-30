package org.certis.studyplatform.member.domain.vo;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MajorVoTest {

    @Test
    @DisplayName("느슨한 허용: 포맷 {<College>,<Department>,<Major>} 허용")
    void relaxedFormat_ShouldPass() {
        String value = "{<Engineering>,<Electrical Engineering>,<Control & Measurement>}";
        Assertions.assertThatCode(() -> MajorVo.of(value))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("느슨한 허용: <College>,<Department>,<Major> 형식 허용")
    void angleBracketCommaSeparated_ShouldPass() {
        String value = "<Engineering>,<Electrical Engineering>,<Control & Measurement>";
        Assertions.assertThatCode(() -> MajorVo.of(value))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("느슨한 허용: 매우 긴 문자열도 허용")
    void veryLongString_ShouldPass() {
        String repeated = "Electrical Engineering, Control & Measurement / Advanced (VLSI)";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            if (i > 0) sb.append(" | ");
            sb.append(repeated);
        }
        Assertions.assertThatCode(() -> MajorVo.of(sb.toString()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("금지된 제어문자 포함 시 예외")
    void controlCharacters_ShouldFail() {
        String withNewline = "Electrical\nEngineering";
        Assertions.assertThatThrownBy(() -> MajorVo.of(withNewline))
                .isInstanceOf(org.certis.studyplatform.exception.DomainException.class);

        String withTab = "Electrical\tEngineering";
        Assertions.assertThatThrownBy(() -> MajorVo.of(withTab))
                .isInstanceOf(org.certis.studyplatform.exception.DomainException.class);
    }
}


