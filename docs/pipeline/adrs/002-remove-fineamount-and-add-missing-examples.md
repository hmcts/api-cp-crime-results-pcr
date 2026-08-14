# 002: Remove JudicialResult.fineAmount/totalCustodialPeriod; rename schemas; add missing OpenAPI examples

## Status

Accepted

## Context

Following ADR-001's simplification pass, `JudicialResult.fineAmount` and `.totalCustodialPeriod`
were the last remaining fields in that family carried over from the original speculative
HMPPs-aligned classification (`docs/PCR-FIELD-MAPPING.md` §1) with no confirmed consumer.

Separately, several fields across `JudicialResult`, `JudicialResultPrompt`, `Offence`, and
`CourtApplication` had no `example:` set, so Swagger UI rendered generic type-based placeholders
(`"string"`, `0`) instead of realistic sample data — fixed for the whole spec, not just this
family, via a scripted audit confirming every scalar property now has one.

Once `JudicialResult`/`JudicialResultPrompt` no longer carried the HMPPs-aligned name's baggage,
the schema type names themselves were the last thing still calling back to that framing —
`resultTexts`/`texts` are the field names consumers see, but the item schema names in the
generated model/Swagger UI still read `JudicialResult`/`JudicialResultPrompt`.

## Decision

- `JudicialResult.fineAmount` and `.totalCustodialPeriod` are removed — no confirmed consumer,
  same rationale as ADR-001's removals.
- The `JudicialResult` schema is renamed to `ResultText`, and `JudicialResultPrompt` to `Text`
  — matching this spec's own singular-item-schema convention (e.g. `offences[]` → `Offence`,
  `caseMarkers[]` → `CaseMarker`), rather than a name still tied to the abandoned HMPPs framing.
- Added realistic `example:` values to `JudicialResultPrompt.label`/`.value`, and the remaining
  placeholder-prone fields on `Offence` (`wording`/`endDate`/`listingNumber`/`convictionDate`/
  `pleaValue`/`pleaDate`) and `CourtApplication` (`type`/`decision`/`decisionDate`/`response`/
  `responseDate`), plus every other scalar property across the spec still missing one
  (`HearingResultedWebhookEvent`, `WebhookAck`, `CustodyLocation`, `HearingDetails.hearingOutcome`,
  `NextHearing.hearingId`, `Court.courtHouseId`, `Defendant.middleName`/`.nationality`, `Address`).

## Consequences

- `service-cp-crime-results-pcr`'s `PcrResultsMapper` stops populating `.fineAmount(...)`/
  `.totalCustodialPeriod(...)` on the read side. The write side (`CPJudicialResultEntity`) is
  unaffected — both are still captured at ingestion, simply no longer exposed on `GET /pcr`.
- The service repo's generated-model imports/usages (`JudicialResult` → `ResultText`,
  `JudicialResultPrompt` → `Text`) need updating in `PcrResultsMapper` and its tests.
- Adding examples is additive/non-breaking on its own; bundled into this same breaking release
  since both changes touch the same schemas.
