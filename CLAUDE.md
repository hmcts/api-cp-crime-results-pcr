# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repo: api-cp-crime-results-pcr

OpenAPI contract for Prosecution Case Results (PCR) — case/hearing/defendant results
(charges, court applications, sentences). CP-native shape, not scoped to any single
consumer's internal data model — see `docs/PCR-FIELD-MAPPING.md` for how individual fields
relate to one confirmed consumer's (HMPPS) Remand and Sentencing (RaS) physical data model,
kept as reference material for that integration, not as the source of the contract's shape.

**Pattern**: Pure spec-only
**OpenAPI spec version**: 0.1.0 (OpenAPI 3.0.0)
**OpenAPI Generator version**: 7.23.0
**Spring Boot version**: 4.1.0

## API Endpoint(s)

- `GET /pcrs/cases/{caseURN}/hearings/{hearingId}/defendants/{defendantId}` → bare `PcrResult[]`
  (full data, no `id`, no per-item `defendantId` — already known from the path)
  (200/400/404/500)
- `GET /pcrs/cases/{caseURN}/hearings/{hearingId}/defendants/{defendantId}/versions?versionId={value}`
  → `PcrVersionMetadataList` (`{ versions: PcrVersionMetadata[] }`, metadata only — `id`,
  `hearingId`, `defendantId`, `recordedAt`) (200/400/404/500)

No `401`/`403` on either endpoint — subscription-key/auth gating happens at APIM before the
request reaches this service, matching `api-cp-crime-prosecution-case-details`'s pattern (only
backend-computable responses are declared). `404` on both means the case, hearing, or defendant
itself doesn't exist — neither endpoint ever 404s on a filter match miss (both return an empty
list/array instead).

The second endpoint's `versionId` query parameter is optional: omitted, it lists metadata for
every recorded version; supplied, it filters the list down to the single matching entry (still a
list, just narrowed) — there is no `latest` special value and no single-full-data-by-id lookup.
Security: `bearerAuth` + `subscriptionKey` (no OAuth2 scope), matching HRDS's pattern —
see ADR-006/AMP-890 in `service-cp-crime-results-pcr`.

Note: the rest of this file (Generated Interfaces & Schema, Domain Models) still describes an
earlier, pre-redesign contract (`ProsecutionCaseResultsApi`,
`ProsecutionCaseResultView`/`DefendantResultView`) and needs a full refresh against the current
CP-native `PcrApi`/`PcrResult` contract — out of scope for this change, flagged here so it isn't
mistaken for accurate.

## Generated Interfaces & Schema

- Spec file: `src/main/resources/openapi/openapi-spec.yml` (no separate JSON Schema files — `openapi/schema/` is empty, kept only via `.gitkeep`)
- Generated API interface: `uk.gov.hmcts.cp.openapi.api.ProsecutionCaseResultsApi`
- Generated models: `ProsecutionCaseResultView`, `DefendantResultView`, `Pcr`, `CourtAppearance`,
  `NextCourtAppearance`, `Charge`, `ChargeOutcome`, `Sentence`, `PeriodLength`,
  `CourtApplicationResult`, `Result`, `ErrorResponse`

## Domain Models

| Model | Purpose |
|---|---|
| `Pcr` | Prosecution case reference — maps to RaS `court_case`/`court_appearance` case identifiers |
| `CourtAppearance` / `NextCourtAppearance` | Hearing-level detail — maps to RaS `court_appearance` / `next_court_appearance` |
| `Charge` / `ChargeOutcome` | Per-offence result — maps to RaS `charge` / `charge_outcome`; `chargeOutcomes` is deliberately an array since one CP offence can carry multiple judicial results |
| `Sentence` / `PeriodLength` | Sentence detail — maps to RaS `sentence` / `period_length`; `PeriodLength` is CP's free-text period phrase pre-parsed into components, not passed through as text |
| `CourtApplicationResult` / `Result` | Bail applications, variations etc. — explicitly out of RaS's model; kept for other consumers of this API |

Every non-guaranteed field is `nullable: true` with an inline description recording *why*
(RaS confirmed no CP-side action needed, vs. genuinely open/unconfirmed) — see
`docs/PCR-FIELD-MAPPING.md` for the authoritative per-field status table. When editing
the spec, keep both in sync.

## Test Structure

| Class | What it validates |
|---|---|
| `OpenApiObjectsTest` | Generated `ErrorResponse`/`PcrResult`/`PcrVersionMetadataList`/`PcrVersionMetadata` field shapes, generated `PcrApi` method names (`getPcrVersionHistory`, `getPcrVersionMetadata`), and that `ErrorResponse.timestamp` generates as `Instant` (not `OffsetDateTime`) |

## Generator Config Notes

- Missing `@JsonInclude(NON_NULL)` in `additionalModelTypeAnnotations` (`gradle/openapi.gradle`) —
  null fields will appear in JSON responses. Given how many fields in this spec are `nullable`
  pending open RaS questions, this is worth fixing before the service repo starts returning
  real payloads.
- `gradle/java.gradle` relaxes `compileGeneratedJava` to warning-only (drops `-Werror`) because
  OpenAPI-generated nullable fields pair a `= null` initializer with `@lombok.Builder`, which
  Lombok flags as a benign no-op-initializer warning on regenerated code. `-Werror` still applies
  in full to hand-written `compileJava`. Don't try to fix this by editing generated code — it
  regenerates every build.

## CI/CD Deviations

Standard workflow set, plus one addition: `publish-api-docs.yml` (release-triggered) calls
`hmcts/amp-catalog/.github/workflows/publish-swagger-ui.yml@v1` to publish this spec's Swagger UI
to `https://hmcts.github.io/api-cp-crime-results-pcr/` and register it in the AMP catalog. This
is in addition to, not instead of, the standard SwaggerHub/APIHub publish already wired through
`ci-draft.yml`/`ci-released.yml`.

## Repo-Specific Notes

- `docs/PCR-FIELD-MAPPING.md` is the field-by-field mapping ledger — its §1 cross-references
  HMPPS's RaS physical data model (Court Case / Court Appearance / Charge / Sentence / Period
  Length) as reference material for that one confirmed consumer, not as the reason a field is
  shaped the way it is — check it before changing any field's shape or nullability; several
  fields carry open questions still being tracked with the RaS team (e.g. `courtCentreId`
  format, `consecutiveToId` cross-reference, `domesticViolenceRelated` case-vs-charge level).
- `docs/DATA-PRODUCTS.md` and `docs/CHAIN_OF_CUSTODY.md` cover structured data-output and
  supply-chain/audit-trail concerns specific to this spec.
- Consumer: `service-cp-crime-results-pcr` (primary, not yet implemented against this contract).