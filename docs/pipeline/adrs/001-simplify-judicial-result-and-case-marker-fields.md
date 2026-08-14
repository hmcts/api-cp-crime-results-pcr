# 001: Simplify JudicialResult, JudicialResultPrompt and CaseMarker fields (breaking, v2.0.0)

## Status

Accepted

## Context

`JudicialResult`, `JudicialResultPrompt`, and `CaseMarker` were originally shaped around a
speculative HMPPs-aligned classification (`financial`/`category`/`convicted`/`concurrent`/
`consecutiveToDate`/`consecutiveToCourtName`/`imprisonmentPeriod` on `JudicialResult`,
`reference`/`type` on `JudicialResultPrompt`) — see `docs/PCR-FIELD-MAPPING.md` §1. None of these
has a confirmed consumer today; they were offered as structured Reference Data signals, not a
finished classification.

Separately, `CaseMarker.code` was the only field ever populated end-to-end (from CP's
`caseMarkers[].markerTypeCode`) — `description` was speculative and had no wiring anywhere in
this service or the legacy Function App's own marker handling
(`cpp-context-azure-legalaidagency`'s `CaseMapper.caseMarkers()` also only ever reads
`markerTypeCode`). Investigation found CP's real hearing payload also carries
`caseMarkers[].markerTypeDescription` alongside `markerTypeCode` (confirmed against this
service's own drift-detection fixtures and multiple Function App test fixtures, e.g.
`markerTypeCode: "DV"` / `markerTypeDescription: "Domestic Violence"`) — a real source exists for
`description`, so `code` can be dropped in favour of the human-readable field without losing
information.

## Decision

- `Offence.judicialResults` and `CourtApplication.judicialResults` are renamed to `resultTexts`.
- `JudicialResult.prompts` is renamed to `texts`.
- `JudicialResult` drops `financial`, `category`, `convicted`, `concurrent`, `consecutiveToDate`,
  `consecutiveToCourtName`, `imprisonmentPeriod`. `resultCode`, `resultText`, `fineAmount`, and
  `totalCustodialPeriod` are unaffected.
- `JudicialResultPrompt` drops `reference` and `type`; only `label` and `value` remain.
- `CaseMarker` drops `code`; `description` is retained and now sourced from CP's
  `caseMarkers[].markerTypeDescription`.
- This is a breaking contract change — published as `v2.0.0` (major bump per Conventional
  Commits `BREAKING CHANGE:` footer), not additive.

## Consequences

- `service-cp-crime-results-pcr`'s `PcrResultsMapper` (read side only) updates to stop populating
  the removed fields and to use the renamed builder methods.
- `service-cp-crime-results-pcr`'s write side (`CPCaseMarkerEntity`, `CPJudicialResultEntity`,
  `CPJudicialResultPromptEntity`, and `CPHearingResultEntityMapper`) is unaffected except for
  adding `markerTypeDescription` capture — these fields are still captured from CP at ingestion
  time (in case a future consumer need re-surfaces them), they are simply no longer exposed on
  `GET /pcr`.
- Every drift-detection expected-output fixture in the service repo is regenerated to the new
  response shape.
- No ticket reference — raised directly from a product/architecture review of the field mapping
  doc; no known external consumer is currently integrated against v1.x, so no deprecation window
  is provided.
