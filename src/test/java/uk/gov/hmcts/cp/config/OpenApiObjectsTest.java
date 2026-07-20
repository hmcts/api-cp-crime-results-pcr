package uk.gov.hmcts.cp.config;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.openapi.api.ProsecutionCaseResultsApi;
import uk.gov.hmcts.cp.openapi.model.ErrorResponse;
import uk.gov.hmcts.cp.openapi.model.ProsecutionCaseResultView;
import uk.gov.hmcts.cp.openapi.model.DefendantResultView;
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
    void generated_prosecution_case_result_view_should_have_expected_fields() {
        assertThat(ProsecutionCaseResultView.class).hasDeclaredFields("prosecutionCase", "hearingId", "eventId", "caseMarkers", "defendants");
    }

    @Test
    void generated_defendant_result_view_should_have_expected_fields() {
        assertThat(DefendantResultView.class).hasDeclaredFields("defendant", "custodyLocation", "hearing", "offences", "courtApplications");
    }

    @Test
    void generated_prosecution_case_results_api_should_have_expected_methods() {
        assertThat(ProsecutionCaseResultsApi.class)
                .hasDeclaredMethods("getProsecutionCaseResults", "getDefendantProsecutionCaseResults");
    }

    @Test
    void generated_error_response_timestamp_should_be_instant() throws Exception {
        Field timestampField = ErrorResponse.class.getDeclaredField("timestamp");

        assertThat(timestampField.getType())
                .as("timestamp field type")
                .isEqualTo(Instant.class);
    }
}