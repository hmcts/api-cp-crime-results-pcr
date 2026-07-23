# PCR → HMPPS field mapping for `api-cp-crime-results-pcr`

**Status:** Draft. The API itself is CP-native (`openapi-spec.yml`) — this document tracks, per CP-native field, where the value actually comes from in CP, and cross-references HMPPS's own Remand and Sentencing (RaS) physical data model (Court Case / Court Appearance / Charge / Sentence / Period Length) for whichever fields HMPPS needs to map on their own side. It also audits every field CP actually surfaces on the printed Prison Court Register (`PrisonCourtRegisterPdfPayloadGenerator.java`) to check nothing relevant got left behind.

---

## 1. Field mapping — API schema → HMPPs table → CP source

### Prosecution Case (`ProsecutionCase`)

| API field | HMPPs field | CP source | Status |
|---|---|---|---|
| `caseURN` | `court_case.case_unique_identifier` / `court_appearance.court_case_reference` | CP case URN | Confirmed |
| `defendantId` (on `Defendant`) | `court_case.prisoner_id` | CP defendant UUID | **Not the same identifier.** `prisoner_id` is a NOMIS ID; CP has no deterministic mapping to it. HMPPs resolves this via Core Person Record probabilistic matching — no CP-side action, already answered. |

### Hearing Details (`HearingDetails`)

| API field | HMPPs field | CP source | Status |
|---|---|---|---|
| `courtHouseCode` | `court_appearance.court_code` | CP `courtHouse` UUID **or** Court Register code | **Open — conflicting evidence, not confirmed.** One clarification said HMPPs keys off the CP court house UUID; HMPPs's own physical data model shows `court_code`'s example as a Court Register code ("YORKCC"), not a UUID. Left as a plain string in the schema (not UUID-locked) pending service analysis — do not treat either answer as settled. |
| `hearingDate` | `court_appearance.appearance_date` | CP hearing date | Confirmed |
| `hearingOutcome` | `court_appearance.appearance_outcome_id` | — | HMPPs: ignore, no CP-side action |
| `warrantType` | `court_appearance.warrant_type` | — | HMPPs: ignore, HMPPs derives it itself |
| `overallConvictionDate` | `court_appearance.overall_conviction_date` | — | HMPPs: resolved, derives from `offences[].convictionDate` — CP need not send |

### Next Hearing (`NextHearing`)

| API field | HMPPs field | CP source | Status |
|---|---|---|---|
| `date` | `next_court_appearance.appearance_date` | CP `nextHearing` | Open — which offence's `nextHearing` wins when several diverge is still with HMPPs (Nutty/Steven query on file) |
| `courtHouseCode` | `next_court_appearance.court_code` | — | Open — not present in CP's next-appearance payload today |
| `time` | `next_court_appearance.appearance_time` | — | Open — CP source not yet confirmed |
| *(not currently in the API)* | `next_court_appearance.appearance_type_id` | — | Gap — video-link-vs-in-person not yet added to the contract, and CP source not yet confirmed either |

### Offence (`Offence`)

| API field | HMPPs field | CP source | Status |
|---|---|---|---|
| `code` | `charge.offence_code` | `offences[].offenceCode` | Confirmed — HMPPs confirmed CP codes align with OMS |
| `startDate` | `charge.offence_start_date` | `offences[].startDate` | Confirmed |
| `endDate` | `charge.offence_end_date` | `offences[]` (terminated results only) | Confirmed optional |
| `convictionDate` | `charge.conviction_date` | `offences[].convictionDate` | Confirmed |
| `listingNumber` | `sentence.count_number` | `offences[].listingNumber` | Open — not yet confirmed as the right mapping; verify against warrant examples |

Terrorism/foreign-power/domestic-violence signals aren't separate `Offence` fields — the raw judicial result prompts (`theCourtDeterminedATerroristConnectionUnderSection30OfTheCounterTerrorismAct2008AppliesToThisOffence`/`...Count`, `offenceAggByForeignPowerCondition`) are exposed as-is via `JudicialResult.judicialResultPrompts[]` instead, and the domestic-violence marker is case-level (`PcrVersion.caseMarkers[]`, sourced from `prosecutionCase.caseMarkers[]` with `markerTypeCode == 'DomesticViolence'`), not per-offence.

### Judicial Result (`JudicialResult`)

| API field | HMPPs field | CP source | Status |
|---|---|---|---|
| `resultCode` | input to `charge_outcome.outcome_type` resolution | Judicial result CJS code | Confirmed as raw input |
| `resultText` | — (audit only) | Judicial result rendered text | Confirmed |
| `postHearingCustodyStatus` | input to `outcome_type` (NON_CUSTODIAL/REMAND/etc.) | `ResultDefinition.postHearingCustodyStatus`, Reference Data, keyed on `cjsResultCode` — confirmed directly against the reference-data JSON fixture, alongside `financial`/`category`/`convicted`/`publishedForNows` | Offered as a structured signal — HMPPs classification itself still TBD |
| `financial` / `category` / `convicted` | same | `ResultDefinition.financial`/`category`/`convicted`, Reference Data | Same — structured, offered, not a finished classification |
| `concurrent` | `sentence.sentence_serve_type` | `concurrent` prompt | Open — only a plain boolean; the CONSECUTIVE/FORTHWITH distinction isn't carried, worth checking if that's a genuine gap |
| `consecutiveToDate` / `consecutiveToCourtName` | `sentence.consecutive_to_id` (FK) | `consecutiveToSentenceImposedOn` (date) + `whichWasImpBy` (court name) | Open — CP gives a date/court, not an ID; no cross-reference mechanism exists in CP |
| `fineAmount` | `sentence.fine_amount` | Imprisonment-in-default-of-fine prompts | Confirmed — explicitly excludes AOC (costs)/AOS (surcharge) |
| `imprisonmentPeriod` / `totalCustodialPeriod` | `period_length.*` | `imprisonmentPeriod`/`totalCustodialPeriod` (free-text string, e.g. "6 Months 1 week") | CP's native shape — free text, not pre-parsed into years/months/weeks/days components |

### Out of HMPPs scope, kept for other consumers

`courtApplications[]` — HMPPs explicitly not interested in court admin applications. Kept in the API for whatever other consumer needs it; not part of the HMPPs-aligned mapping above.

---

## 2. Gap analysis — what the printed Prison Court Register surfaces that this API doesn't carry

Went back through every field `PrisonCourtRegisterPdfPayloadGenerator.java` actually renders and checked it against the schema above. Three different outcomes, not one:

### 2a. Already resolved — confirmed not needed, not a gap

- `registerDate` — generation timestamp, not a case fact.
- `officerInCase`, `parentGuardianName`, `parentGuardianAddress1` — hardcoded blank in the generator; nothing to map.
- `defendantResults[]`/`caseResults[]` (hearing-level and case-level free-text results, distinct from per-charge results) — no HMPPs field for these; covered by the existing `hearingOutcome` → "ignore" resolution, so this isn't a new gap, just the same one already closed.
- `offenceTitle`, `wording`, `allocationDecision`, `verdictCode`, `pleaValue`, `indicatedPleaValue` — human-readable/procedural detail with no corresponding HMPPs column visible in the physical data model. Treating as confirmed-not-needed, but **not yet asked of HMPPs directly** — worth a one-line confirmation rather than an assumption, since "no visible column" isn't the same as "HMPPs said no."

### 2b. Real structural gaps — fixed in the schema, not just flagged

- **Multiple results per offence, one `chargeOutcome`.** The document's `offences[].results[]` is an array — a single offence can carry more than one judicial result (e.g. a conviction plus a separate costs/ancillary result). The schema had `chargeOutcome` as a single object, matching `charge.charge_outcome_id`'s single-FK shape on HMPPs's side but silently forcing CP to pick one result and drop the rest. **Fixed:** renamed to `chargeOutcomes[]` (array) in the YAML — HMPPs decides which is authoritative rather than CP choosing for them.
- **`postHearingCustodyStatus` dropped entirely.** This was flagged as the key structured signal in the earlier draft of this document, but didn't make it into the HMPPs-aligned schema when the target was corrected. It's exactly the CP-side signal HMPPs needs to help classify `outcome_type` (custodial/remand/non-custodial). **Fixed:** added to `ChargeOutcome`, alongside `financial`/`category`/`convicted` from the same `ResultDefinition` lookup.
- **`applications[]` shape lost detail.** The document's applications carry `type`, `decision`, `decisionDate`, `response`, `responseDate`, and a `result[]` array — the schema's `CourtApplicationResult` had only `id`/`reference`/a single `result`. **Fixed:** expanded to match, even though this whole branch is out of HMPPs's scope — it's still meant to serve other consumers correctly.

### 2c. Real gaps — not fixed, flagged for a decision because there's no confirmed HMPPs target to fix them against

- **Custody location — correction to the earlier pass, and a further correction to that correction.** Previously described `custodyLocation` as "on every printed register," then walked back to "confirmed via the real `expected_output.pdf` from `OEELayout5IT`'s own fixture" that it never prints. **That second claim could not be re-verified** — a full search of every repo in this workspace (`cpp-context-progression`, the legacy Function App, etc.) found no `OEELayout5IT` class, no `expected_output.pdf`, and no `.docx` template file anywhere. The actual `OEE_Layout5` template and its rendering are owned by an external `systemdocgenerator` service/repo not present here (confirmed via `SystemDocGeneratorEventProcessor.java` and `PrisonCourtRegisterEventProcessor.java`'s `PRISON_COURT_REGISTER_TEMPLATE = "OEE_Layout5"` constant — the template is referenced by name only, never checked in). Treat whether `custodyLocation` is actually printed as **unconfirmed**, not settled either way, until the external template repo is inspected directly.
- **Defendant identity (name, DOB, address, gender, nationality, aliases) — confirmed not needed, resolved.** HMPPS confirmed they resolve the defendant entirely via `defendantId`/`masterDefendantId` against their own systems (NOMIS) — no name/DOB/address/gender/nationality/alias field is required from this API. `title`/`firstName`/`middleName`/`lastName`/`dateOfBirth`/`address` were removed from the `Defendant` schema accordingly (previously flagged "PII — API-suppression decision open"; decision is now made — suppressed, not carried at all).
- **Court venue display fields** (`ljaName`, `courtHouse` display name, `courtHouseAddress`). `courtCentreId` (UUID) covers HMPPs's actual join key, but if any downstream consumer needs a human-readable venue name/address rather than resolving the UUID themselves, that's currently nowhere in the contract. Likely fine to leave UUID-only, but worth a one-line confirmation rather than a silent assumption, same as 2a's procedural fields.
- **Parent/guardian details — confirmed not needed, resolved.** The template reserves the full shape — `parentGuardianName`, `parentGuardianAddress1`–`5`/`parentGuardianPostCode` — but the generator only ever sends it blank, and HMPPS confirmed no parent/guardian concept is needed from this API: HMPPS resolves everything via `defendantId`, same as the defendant-identity decision above. Not added to the contract.

---

## 2d. Template double-check — superseded, could not be re-verified

This section previously claimed to have checked the real `OEE_Layout5Template.docx` merge fields and a rendered-output fixture (`OEELayout5IT`'s `payload.json` → `expected_output.pdf`). **A full re-search of this workspace found none of these artefacts** — no such class, no such PDF fixture, no `.docx` file anywhere in `cpp-context-progression` or elsewhere. The template is owned and rendered by an external `systemdocgenerator` service not present in this workspace (see §2c's custody-location correction above). Every claim in the original version of this section — that `resultCode` never renders, that `custodyLocation` is confirmed absent from the rendered page, that `ljaName`/`courtHouse`/`courtHouseAddress` render in the page header — is **unverified** from anything actually inspectable here. Treat as unconfirmed until someone with access to the external template repo checks directly.

---

## 4. Resolved — endpoint shape now follows the design doc's per-`(hearingId, defendantId)` unit of work

The earlier case-level "all defendants at a hearing" endpoint has been removed. The design doc's unit of work is
one PCR resource per `(hearingId, defendantId)` pair, so there is no whole-case view to choose between — the API
now has two operations under `/pcrs/cases/{caseURN}/hearings/{hearingId}/defendants/{defendantId}`: the base path
(a resource's full version history), and `.../versions?version={value}` (a single version, selected by a required
`version` query parameter — `version=latest` for the most recently recorded version, or any other value as a
specific version's source correlation id, minted once at the source event and propagated through, not generated by
this API). The previous open question about whether HMPPS calls a defendant-scoped endpoint or filters a case-level
response no longer applies.

---

## 5. Superseded framing (kept for the CP source-code trail)

The original version of this document treated `PrisonCourtRegisterPdfPayloadGenerator.java` as the API's source of truth, on the assumption `api-cp-crime-results-pcr` served the printed Prison Court Register. It doesn't — the real consumer is HMPPS HMPPs, and its physical data model is the actual target (§1 above). The CP-side findings from that earlier pass — judicial result prompts, `ResultDefinition` reference data, `caseMarkers` — carried forward correctly; only the target shape needed correcting, which is what §1 and §2 do here.

---

## 6. Further findings — architecture deep-dive, 19 Jul 2026

The API was subsequently redesigned to a CP-native shape rather than the HMPPs-aligned one this document originally targeted (see the service repo's design doc). These findings came out of validating two proposed CP data-model diagrams (defendant-centric and case-centric) against the real source code across `cpp-context-hearing`, `cpp-context-results`, the legacy Function App (`cpp-context-azure-legalaidagency`), and `cpp-context-progression`.

- **No `Charge`/`ChargeListing` entity exists anywhere in CP.** `Offence` (in `cpp-context-hearing`'s `Offence.java`) is the sole charge-like concept, confirmed by grep across the Function App and Progression's PDF payload generator too. A case-centric diagram proposing separate `Charge`/`ChargeListing` entities is not grounded in any CP code found — it reads as RaS's own `charge`/`charge_outcome` table split leaking back into a supposedly CP-native shape. Do not introduce this split.
- **`consecutiveTo` (cross-case consecutive sentencing) — only a date and a free-text court name exist, never a case reference.** Confirmed via `consecutiveToSentenceImposedOn` (DATE) and `whichWasImpBy` (FIXL, court name) judicial result prompts, and via the Function App's outbound mapper, which reduces every result to just `resultCode`/`resultText`. There is no case URN, case UUID, or sentence ID anywhere in CP linking a consecutive sentence to the case it was imposed in. Any API field representing this must stay date + court-name only — do not invent a case-reference field with nothing to source it from.
- **Progression has no PCR-to-judicial-result correlation mechanism at all.** `PrisonCourtRegisterEntity`'s `id` is a fresh random UUID per row (not derived from any triggering event), `recorded_date` is an insert timestamp, and no field anywhere (`resultId`, `sourceEventId`, `sharedTime`, `eventPosition`) links a generated PCR row back to the judicial result that caused it. Progression also inserts a new row per regeneration rather than overwriting, so amendments produce multiple undistinguishable-by-cause rows. This confirms the PCR/PDF versioning-correlation problem (§7 of the design doc) has nothing existing to build on — any correlation mechanism must be built new, end-to-end.
- **PCR eligibility is gated by a single, generic flag: `judicialResult.publishedForNows == false`.** Confirmed in the Function App's `RegisterFragmentService.filterJudicialResultsApplicableForRegisters`. No dedicated `ResultDefinition` field exists for "applicable to PCR" — it reuses the same flag NOWS publishing uses. Eligibility is independent of `convicted`/`isConvictedResult`: bail/remand results appear on the PCR alongside convictions.
- **The `OEE_Layout5` template and its rendering live in an external `systemdocgenerator` service, not in this workspace.** `SystemDocGeneratorEventProcessor` only reacts to `document-available`/`generation-failed` events; the template name is a hardcoded string constant handed off with a payload file reference. No `.docx`, merge-field map, or rendered-output fixture exists in any repo here — see the corrections to §2c/§2d above.
- **Decision recorded — candidate fields evaluated and confirmed NOT needed by HMPPS, not added to the CP-native contract:** prosecution/defence counsel, attending solicitor name, `defendantResults[]`/`caseResults[]` (already closed per §2a), `gender`/`nationality`, `aliases`, `ljaName`/`courtHouseAddress`, `allocationDecision`/`indicatedPleaValue`. All are confirmed to exist in CP's real PCR payload (cross-validated between the Function App's outbound mapper and Progression's `PrisonCourtRegisterPdfPayloadGeneratorTest.java` fixtures), but HMPPS confirmed they work entirely from `defendantId` against NOMIS and need none of these — including name/DOB/address themselves (§2c above), which supersedes the earlier Core Person Record justification for carrying identity fields at all. Do not add speculatively.
- **Correction — `CourtApplication`'s linked-offence references turned out to be load-bearing, not a speculative addition.** Previously recorded as "exists in CP, need unconfirmed" on the reasoning that `PrisonCourtRegisterPdfPayloadGenerator.buildApplication()` only reads an application's own top-level `results` field. That's true for the *Progression PDF generator*, but the *Function App's* `DefendantContextBaseService.getDefendantContextBaseList()` separately walks `courtApplication.courtApplicationCases[].offences[].judicialResults` **and** `courtApplication.courtOrder.courtOrderOffences[].offence.judicialResults` as two more sources, merging all four (case offences, application-level results, application-linked-offence results, court-order-offence results) into one flat list before the `!publishedForNows` eligibility filter runs — confirmed no source-specific branching exists. **Added** `CourtApplication.offences[]` to the API contract (reusing the existing `Offence` schema), folding the court-order-offence source into the same array per product decision — consumers don't need to distinguish how an offence came to be linked to the application.
