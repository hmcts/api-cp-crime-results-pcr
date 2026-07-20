package uk.gov.hmcts.cp.config;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.openapi.api.PcrApi;
import uk.gov.hmcts.cp.openapi.model.ErrorResponse;
import uk.gov.hmcts.cp.openapi.model.PcrVersionHistory;
import uk.gov.hmcts.cp.openapi.model.PcrVersion;
import java.lang.reflect.Field;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class OpenApiObjectsTest {
    @Test
    void generated_error_response_should_have_expected_fields() {
        assertThat(ErrorResponse.class).hasDeclaredMethods("error", "message", "details", "traceId");
    }

    @Test
    void generated_pcr_version_history_should_have_expected_fields() {
        assertThat(PcrVersionHistory.class).hasDeclaredFields("hearingId", "defendantId", "versions");
    }

    @Test
    void generated_pcr_version_should_have_expected_fields() {
        assertThat(PcrVersion.class).hasDeclaredFields("id", "hearingId", "defendantId",
                "prosecutionCase", "caseMarkers", "defendant", "custodyLocation", "hearing", "offences", "courtApplications");
    }

    @Test
    void generated_pcr_api_should_have_expected_methods() {
        assertThat(PcrApi.class)
                .hasDeclaredMethods("getPcrVersionHistory", "getPcrVersion", "getLatestPcrVersion");
    }

    @Test
    void generated_error_response_timestamp_should_be_instant() throws Exception {
        Field timestampField = ErrorResponse.class.getDeclaredField("timestamp");

        assertThat(timestampField.getType())
                .as("timestamp field type")
                .isEqualTo(Instant.class);
    }
}