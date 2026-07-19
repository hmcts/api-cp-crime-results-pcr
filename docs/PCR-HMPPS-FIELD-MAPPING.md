# PCR → HMPPS field mapping for `api-cp-crime-results-pcr`

**Status:** Draft, 15 Jul 2026. Target schema: `2026-07-15-pcr-openapi-spec-hmpps-aligned.yml`, structured 1:1 against the HMPPS Remand and Sentencing (HMPPs) physical data model (Court Case / Court Appearance / Charge / Sentence / Period Length). This document tracks, per HMPPs field, where the value comes from in CP, and separately audits every field CP actually surfaces on the printed Prison Court Register (`PrisonCourtRegisterPdfPayloadGenerator.java`) to check nothing relevant got left behind when the schema was re-targeted at HMPPs instead of the PDF.

---

## 1. Field mapping — API schema → HMPPs table → CP source

### Case (`Pcr`)

| API field | HMPPs field | CP source | Status |
|---|---|---|---|
| `pcr.id` | `court_case.case_unique_identifier` | CP case UUID | Confirmed |
| `pcr.reference` | `court_case.case_unique_identifier` / `court_appearance.court_case_reference` | CP case URN | Confirmed |
| `defendantId` | `court_case.prisoner_id` | CP defendant UUID | **Not the same identifier.** `prisoner_id` is a NOMIS ID; CP has no deterministic mapping to it. HMPPs resolves this via Core Person Record probabilistic matching — no CP-side action, already answered. |

### Court Appearance (`CourtAppearance`)

| API field | HMPPs field | CP source | Status |
|---|---|---|---|
| `courtCentreId` | `court_appearance.court_code` | CP `courtHouse` UUID **or** Court Register code | **Open — conflicting evidence, not confirmed.** One clarification said HMPPs keys off the CP court house UUID; HMPPs's own physical data model shows `court_code`'s example as a Court Register code ("YORKCC"), not a UUID. Left as a plain string in the schema (not UUID-locked) pending service analysis — do not treat either answer as settled. |
| `hearingDate` | `court_appearance.appearance_date` | CP hearing date | Confirmed |
| `hearingOutcome` | `court_appearance.appearance_outcome_id` | — | HMPPs: ignore, no CP-side action |
| `warrantType` | `court_appearance.warrant_type` | — | HMPPs: ignore, HMPPs derives it itself |
| `overallConvictionDate` | `court_appearance.overall_conviction_date` | — | HMPPs: resolved, derives from `charges[].convictionDate` — CP need not send |

### Next Court Appearance

| API field | HMPPs field | CP source | Status |
|---|---|---|---|
| `nextCourtAppearance.appearanceDate` | `next_court_appearance.appearance_date` | CP `nextHearing` | Open — which charge's `nextHearing` wins when several diverge is still with HMPPs (Nutty/Steven query on file) |
| `nextCourtAppearance.courtCentreId` | `next_court_appearance.court_code` | — | Open — not present in CP's next-appearance payload today |
| `nextCourtAppearance.appearanceTime` | `next_court_appearance.appearance_time` | — | Open — CP source not yet confirmed |
| `nextCourtAppearance.appearanceType` | `next_court_appearance.appearance_type_id` | — | Open — video-link-vs-in-person not yet confirmed in CP's payload |

### Charge

| API field | HMPPs field | CP source | Status |
|---|---|---|---|
| `offenceCode` | `charge.offence_code` | `offences[].offenceCode` | Confirmed — HMPPs confirmed CP codes align with OMS |
| `offenceStartDate` | `charge.offence_start_date` | `offences[].startDate` | Confirmed |
| `offenceEndDate` | `charge.offence_end_date` | `offences[]` (terminated results only) | Confirmed optional |
| `convictionDate` | `charge.conviction_date` | `offences[].convictionDate` | Confirmed |
| `terrorRelated` | `charge.terror_related` | `judicialResultPrompt` `promptReference` = `theCourtDeterminedATerroristConnectionUnderSection30OfTheCounterTerrorismAct2008AppliesToThisOffence`/`...Count`, inside the Imprisonment result's prompts | Open — value is text (`"Yes"`/`"No"`), needs coercion; HMPPs to confirm it keys off this exact promptReference |
| `foreignPowerRelated` | `charge.foreign_power_related` | `judicialResultPrompt` `promptReference` = `offenceAggByForeignPowerCondition`, confirmed `type: BOOLEAN` | Confirmed structurally — genuine boolean, no parsing needed |
| `domesticViolenceRelated` | `charge.domestic_violence_related` | `prosecutionCase.caseMarkers[]`, `markerTypeCode == 'DomesticViolence'` | Open — this is case-level in CP, charge-level in HMPPs; confirm whether every charge on a marked case should echo `true` |

### Charge Outcome

| API field | HMPPs field | CP source | Status |
|---|---|---|---|
| `cjsResultCode` | input to `charge_outcome.outcome_type` resolution | Judicial result CJS code | Confirmed as raw input |
| `resultText` | — (audit only) | Judicial result rendered text | Confirmed |
| `postHearingCustodyStatus` | input to `outcome_type` (NON_CUSTODIAL/REMAND/etc.) | `ResultDefinition.postHearingCustodyStatus`, Reference Data, keyed on `cjsResultCode` — confirmed directly against the reference-data JSON fixture, alongside `financial`/`category`/`convicted`/`publishedForNows` | Offered as a structured signal — HMPPs classification itself still TBD |
| `financial` / `category` / `convicted` | same | `ResultDefinition.financial`/`category`/`convicted`, Reference Data | Same — structured, offered, not a finished classification |

### Sentence

| API field | HMPPs field | CP source | Status |
|---|---|---|---|
| `countNumber` | `sentence.count_number` | `offences[].listingNumber` | Open — not yet confirmed as the right mapping; verify against warrant examples |
| `sentenceServeType` | `sentence.sentence_serve_type` | `concurrent` prompt; `consecutiveToSentenceImposedOn` presence | Open — `FORTHWITH` has no confirmed CP source |
| `consecutiveToId` | `sentence.consecutive_to_id` (FK) | `consecutiveToSentenceImposedOn` (date) + `whichWasImpBy` (court name) | Open — CP gives a date/court, not an ID; cross-reference mechanism undecided |
| `sentenceType` | `sentence.sentence_type_id` | CJS code (e.g. Imprisonment) | Open — HMPPs needs `nomis_cja_code`; CJS→CJA cross-reference TBD on HMPPs side |
| `fineAmount` | `sentence.fine_amount` | Imprisonment-in-default-of-fine prompts | Confirmed — explicitly excludes AOC (costs)/AOS (surcharge) |

### Period Length

| API field | HMPPs field | CP source | Status |
|---|---|---|---|
| `years`/`months`/`weeks`/`days` | `period_length.*` | `imprisonmentPeriod`/`totalCustodialPeriod` (free-text string) | Confirmed source, but needs CP-side parsing before it reaches the API — HMPPs will not accept the raw string |
| `periodOrder` | `period_length.period_order` | Constructed | Confirmed |
| `periodLengthType` | `period_length.period_length_type` | — | Open — mostly `Custodial_term`, full mapping depends on which sentence prompt produced it |

### Out of HMPPs scope, kept for other consumers

`courtApplicationResults[]` — HMPPs explicitly not interested in court admin applications. Kept in the API for whatever other consumer needs it; not part of the HMPPs-aligned mapping above.

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
- **Defendant identity (name, DOB, address, gender, nationality, aliases).** Entirely absent from the API — only `defendantId` (CP UUID) survives. This is deliberate given HMPPs's own `court_case` has no name/DOB/address columns. But HMPPs's Core Person Record matching explicitly runs on "name, DOB, address, PNC, CRO" as its clustering signals — worth directly confirming this event-level API isn't expected to carry any of those, rather than assuming Core Person Record is fed exclusively by some other pipeline.
- **Court venue display fields** (`ljaName`, `courtHouse` display name, `courtHouseAddress`). `courtCentreId` (UUID) covers HMPPs's actual join key, but if any downstream consumer needs a human-readable venue name/address rather than resolving the UUID themselves, that's currently nowhere in the contract. Likely fine to leave UUID-only, but worth a one-line confirmation rather than a silent assumption, same as 2a's procedural fields.
- **Parent/guardian address (multi-line).** The template reserves the full shape — `parentGuardianAddress1`–`5` plus `parentGuardianPostCode` — mirroring the defendant's own `address1`–`5`/`postCode` fields exactly. The generator today only ever sends `parentGuardianName`/`parentGuardianAddress1`, both hardcoded blank — but that's an unimplemented case, not evidence the concept isn't wanted. Genuinely absent from the API and from HMPPs's physical data model (no youth/appropriate-adult entity visible in it). Worth asking HMPPs directly whether parent/guardian details — presumably relevant for youth defendants — are needed at all, rather than assuming no because HMPPs's current model has no obvious home for them.

---

## 2d. Template double-check — superseded, could not be re-verified

This section previously claimed to have checked the real `OEE_Layout5Template.docx` merge fields and a rendered-output fixture (`OEELayout5IT`'s `payload.json` → `expected_output.pdf`). **A full re-search of this workspace found none of these artefacts** — no such class, no such PDF fixture, no `.docx` file anywhere in `cpp-context-progression` or elsewhere. The template is owned and rendered by an external `systemdocgenerator` service not present in this workspace (see §2c's custody-location correction above). Every claim in the original version of this section — that `resultCode` never renders, that `custodyLocation` is confirmed absent from the rendered page, that `ljaName`/`courtHouse`/`courtHouseAddress` render in the page header — is **unverified** from anything actually inspectable here. Treat as unconfirmed until someone with access to the external template repo checks directly.

---

## 4. Open question — is the defendant-scoped endpoint actually used?

PCR API has two endpoints: one returns everything for a case at a hearing (all defendants), the other scopes to a single defendant. The
defendant-scoped one is a subset of the case-level one.Does HMPPs calls it per-defendant, or whole case and filter to
the person they need? 

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
- **Decision recorded — candidate fields evaluated and NOT added to the CP-native contract, for lack of any confirmed consumer need:** prosecution/defence counsel, attending solicitor name, `defendantResults[]`/`caseResults[]` (already closed per §2a), `gender`/`nationality`, `ljaName`/`courtHouseAddress`, `allocationDecision`/`indicatedPleaValue`. All are confirmed to exist in CP's real PCR payload (cross-validated between the Function App's outbound mapper and Progression's `PrisonCourtRegisterPdfPayloadGeneratorTest.java` fixtures), but none has an identified requirement from any consumer — unlike `name`/`dateOfBirth`/`address`, which are justified by HMPPS's own Core Person Record matching signals. Revisit only if a real requirement surfaces; do not add speculatively.
- **`CourtApplication`'s linked-offence references — exists in CP, need unconfirmed.** `CourtApplication.courtApplicationCases[].offences[]` (confirmed in `cpp-context-hearing`/`cpp-context-results`) is a list of the *same* `Offence` objects/UUIDs as the linked prosecution case — i.e. which offence(s) a court application relates to, distinct from what results the application itself produced (`CourtApplication.judicialResults`, already correctly modelled). `PrisonCourtRegisterPdfPayloadGenerator.buildApplication()` doesn't use this linkage — it reads an application's results from its own top-level `results` field, never from a linked offence — so nothing about PCR's actual generation requires exposing it. Not added to `CourtApplication` in the API/data model; revisit only if a consumer need surfaces.
