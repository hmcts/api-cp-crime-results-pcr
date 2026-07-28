package uk.gov.hmcts.cp.config;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.openapi.api.PcrApi;
import uk.gov.hmcts.cp.openapi.model.ErrorResponse;
import uk.gov.hmcts.cp.openapi.model.PcrHearingResult;
import uk.gov.hmcts.cp.openapi.model.PcrVersionMetadataList;
import uk.gov.hmcts.cp.openapi.model.PcrVersionMetadata;
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
    void generated_pcr_result_should_have_expected_fields() {
        assertThat(PcrHearingResult.class).hasDeclaredFields(
                "caseURN", "caseMarkers", "defendant", "custodyLocation", "hearing", "nextHearing", "offences", "courtApplications");
    }

    @Test
    void generated_pcr_version_metadata_list_should_have_expected_fields() {
        assertThat(PcrVersionMetadataList.class).hasDeclaredFields("versions");
    }

    @Test
    void generated_pcr_version_metadata_should_have_expected_fields() {
        assertThat(PcrVersionMetadata.class).hasDeclaredFields("id", "hearingId", "defendantId", "recordedAt");
    }

    @Test
    void generated_pcr_api_should_have_expected_methods() {
        assertThat(PcrApi.class)
                .hasDeclaredMethods("getPcrHearingResults", "getPcrHearingResultsMetadata");
    }

    @Test
    void generated_error_response_timestamp_should_be_instant() throws Exception {
        Field timestampField = ErrorResponse.class.getDeclaredField("timestamp");

        assertThat(timestampField.getType())
                .as("timestamp field type")
                .isEqualTo(Instant.class);
    }
}