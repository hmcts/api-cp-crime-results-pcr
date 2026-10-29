package uk.gov.hmcts.cp.config;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.openapi.api.PcrApi;
import uk.gov.hmcts.cp.openapi.model.CourtApplication;
import uk.gov.hmcts.cp.openapi.model.ErrorResponse;
import uk.gov.hmcts.cp.openapi.model.HearingDetails;
import uk.gov.hmcts.cp.openapi.model.NextHearing;
import uk.gov.hmcts.cp.openapi.model.Offence;
import uk.gov.hmcts.cp.openapi.model.PcrHearingResult;
import uk.gov.hmcts.cp.openapi.model.ProsecutionCase;
import uk.gov.hmcts.cp.openapi.model.ResultText;
import java.lang.reflect.Field;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
class OpenApiObjectsTest {
    @Test
    void generated_error_response_should_have_expected_fields() {
        assertThat(ErrorResponse.class).hasDeclaredMethods("error", "message", "details", "traceId");
    }

    @Test
    void generated_pcr_result_should_have_expected_fields() {
        assertThat(PcrHearingResult.class).hasDeclaredFields(
                "prosecutionCase", "defendant", "custodyLocation", "hearing", "sharedTime", "offences",
                "courtApplications");
    }

    @Test
    void generated_court_application_should_have_defendant_type_field() {
        assertThat(CourtApplication.class).hasDeclaredFields(
                "reference", "type", "defendantType", "decision", "decisionDate", "response", "responseDate",
                "results", "offences");
    }

    @Test
    void generated_prosecution_case_should_have_prosecutor_field() {
        assertThat(ProsecutionCase.class).hasDeclaredFields(
                "caseURN", "prosecutor", "caseMarkers", "results");
    }

    @Test
    void generated_prosecution_case_should_have_document_order_fields() {
        assertThat(ProsecutionCase.class).hasDeclaredFields("documentSequence", "documentCaseOrder");
    }

    @Test
    void generated_result_text_should_have_next_hearing_field() throws Exception {
        assertThat(ResultText.class.getDeclaredField("nextHearing").getType()).isEqualTo(NextHearing.class);
    }

    @Test
    void generated_offence_and_hearing_details_should_not_have_next_hearing_field() {
        assertThatThrownBy(() -> Offence.class.getDeclaredField("nextHearing")).isInstanceOf(NoSuchFieldException.class);
        assertThatThrownBy(() -> HearingDetails.class.getDeclaredField("nextHearing")).isInstanceOf(NoSuchFieldException.class);
    }

    @Test
    void generated_next_hearing_should_have_expected_fields() {
        assertThat(NextHearing.class).hasDeclaredFields("hearingId", "dateTime", "court");
    }

    @Test
    void generated_pcr_api_should_have_expected_methods() {
        assertThat(PcrApi.class)
                .hasDeclaredMethods("getPcrHearingResults");
    }

    @Test
    void generated_error_response_timestamp_should_be_instant() throws Exception {
        Field timestampField = ErrorResponse.class.getDeclaredField("timestamp");

        assertThat(timestampField.getType())
                .as("timestamp field type")
                .isEqualTo(Instant.class);
    }
}