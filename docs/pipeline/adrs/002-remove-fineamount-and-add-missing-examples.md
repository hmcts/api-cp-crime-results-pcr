# 002: Remove JudicialResult.fineAmount; add missing OpenAPI examples

## Status

Accepted

## Context

Following ADR-001's simplification pass, `JudicialResult.fineAmount` was the last remaining field
in that family carried over from the original speculative HMPPs-aligned classification
(`docs/PCR-FIELD-MAPPING.md` §1) with no confirmed consumer.

Separately, several fields across `JudicialResult`, `JudicialResultPrompt`, `Offence`, and
`CourtApplication` had no `example:` set, so Swagger UI rendered generic type-based placeholders
(`"string"`, `0`) instead of realistic sample data.

## Decision

- `JudicialResult.fineAmount` is removed — no confirmed consumer, same rationale as ADR-001's
  removals.
- Added realistic `example:` values to `totalCustodialPeriod`, `JudicialResultPrompt.label`/
  `.value`, and the remaining placeholder-prone fields on `Offence`
  (`wording`/`endDate`/`listingNumber`/`convictionDate`/`pleaValue`/`pleaDate`) and
  `CourtApplication` (`type`/`decision`/`decisionDate`/`response`/`responseDate`).

## Consequences

- `service-cp-crime-results-pcr`'s `PcrResultsMapper` stops populating `.fineAmount(...)` on the
  read side. The write side (`CPJudicialResultEntity.fineAmount`) is unaffected — still captured
  at ingestion, simply no longer exposed on `GET /pcr`.
- Adding examples is additive/non-breaking on its own; bundled into this same breaking release
  since both changes touch the same schemas.
