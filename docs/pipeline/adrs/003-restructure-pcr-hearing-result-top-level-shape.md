# 003: Restructure PcrHearingResult top-level shape; remove ResultText.resultCode/resultText

## Status

Accepted

## Context

`PcrHearingResult` had grown a flat top-level shape — `caseURN`, `caseMarkers`, `defendant`,
`custodyLocation`, `hearing`, `nextHearing`, `offences`, `courtApplications`, `defendantResults`,
`caseResults` — where several facts that conceptually belong to one owner (the case, the
defendant, the hearing) were siblings at the root instead. Each result-bearing collection
(`Offence`, `CourtApplication`) also carried a differently-named array for the same concept
(`results`, then `judicialResults`, then `resultTexts`/`offenceResults`/`applicationResults`
across successive passes), leaving three different names for what is the same relationship in
three places.

Separately, `ResultText.resultCode` and `.resultText` had no confirmed consumer, continuing the
pattern of removals in ADR-001/002.

## Decision

- New `ProsecutionCase` schema groups `caseURN`, `caseMarkers`, and `results` (was
  `PcrHearingResult.caseResults`) — the case-level facts, nested under
  `PcrHearingResult.prosecutionCase` instead of sitting at the root.
- `Defendant` gains a `results` field (was `PcrHearingResult.defendantResults`) — judicial results
  recorded directly against the defendant now live on the defendant object itself.
- `HearingDetails` gains a `nextHearing` field (was `PcrHearingResult.nextHearing`) and a new
  `sharedTime` field (CP's own root-level `sharedTime`, a sibling of `hearing` in the source
  payload — one of the still-undecided version-correlation candidates from design §7, exposed as
  a raw fact only).
- `Offence.offenceResults` and `CourtApplication.applicationResults` are both renamed to `results`
  — one consistent name for "the judicial results recorded against this thing", used identically
  on `ProsecutionCase`, `Defendant`, `Offence`, and `CourtApplication`.
- `ResultText.resultCode` and `.resultText` are removed — no confirmed consumer.
  `ResultText.resultTexts` (the array of `Text` prompts) is now the sole field.

## Consequences

- `PcrHearingResult`'s top level is now: `prosecutionCase`, `defendant`, `custodyLocation`,
  `hearing`, `offences`, `courtApplications`.
- `service-cp-crime-results-pcr`'s `PcrResultsMapper` needs a substantial rewrite to build the
  nested `prosecutionCase`/`defendant.results`/`hearing.nextHearing` shape instead of flat
  top-level fields, and to populate `hearing.sharedTime` from a new write-side source.
- `sharedTime` needs a new write path: `HearingDetailsResponse` (root, sibling of `hearing`) gains
  a `sharedTime` field, persisted on `cp_version` via a new Flyway migration, since it is scoped to
  one ingested version rather than the shared `cp_case_hearing` row.
- Every drift-detection expected-output fixture is regenerated for the new nested shape.
