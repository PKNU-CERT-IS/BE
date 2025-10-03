package org.certis.studyplatform.shared.config;

import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = JacksonConfiguration.class)
class JacksonConfigurationTest {

    @Autowired
    private ObjectMapper objectMapper;

    private record LargePayload(String data) {}

    @Test
    @DisplayName("Configured ObjectMapper can read >20MB JSON string field")
    void objectMapper_canDeserialize_oversizedString_over20MB() throws Exception {
        int sizeOver20Mb = 21_000_000; // characters; Jackson's limit is in characters

        String big = "A".repeat(sizeOver20Mb);
        String json = "{\"data\":\"" + big + "\"}";

        LargePayload payload = objectMapper.readValue(json, LargePayload.class);

        assertThat(payload).isNotNull();
        assertThat(payload.data()).hasSize(sizeOver20Mb);
    }

    @Test
    @DisplayName("Sanity: default ObjectMapper without custom constraints would fail for >20MB")
    void defaultObjectMapper_wouldFail_withoutRelaxedConstraints() {
        ObjectMapper defaultMapper = new ObjectMapper();

        int sizeOver20Mb = 21_000_000;
        String big = "B".repeat(sizeOver20Mb);
        String json = "{\"data\":\"" + big + "\"}";

        assertThatThrownBy(() -> defaultMapper.readValue(json, LargePayload.class))
                .hasRootCauseInstanceOf(StreamConstraintsException.class);
    }
}

