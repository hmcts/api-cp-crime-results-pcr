# PCR → HMPPS field mapping for `api-cp-crime-results-pcr`

**Status:** Draft, 15 Jul 2026. Target schema: `2026-07-15-pcr-openapi-spec-hmpps-aligned.yml`, structured 1:1 against the HMPPS Remand and Sentencing (RaS) physical data model (Court Case / Court Appearance / Charge / Sentence / Period Length). This document tracks, per RaS field, where the value comes from in CP, and separately audits every field CP actually surfaces on the printed Prison Court Register (`PrisonCourtRegisterPdfPayloadGenerator.java`) to check nothing relevant got left behind when the schema was re-targeted at RaS instead of the PDF.

---

## 1. Field mapping — API schema → RaS table → CP source

### Case (`Pcr`)

| API field | RaS field | CP source | Status |
|---|---|---|---|
| `pcr.id` | `court_case.case_unique_identifier` | CP case UUID | Confirmed |
| `pcr.reference` | `court_case.case_unique_identifier` / `court_appearance.court_case_reference` | CP case URN | Confirmed |
| `defendantId` | `court_case.prisoner_id` | CP defendant UUID | **Not the same identifier.** `prisoner_id` is a NOMIS ID; CP has no deterministic mapping to it. RaS resolves this via Core Person Record probabilistic matching — no CP-side action, already answered. |

### Court Appearance (`CourtAppearance`)

| API field | RaS field | CP source | Status |
|---|---|---|---|
| `courtCentreId` | `court_appearance.court_code` | CP `courtHouse` UUID **or** Court Register code | **Open — conflicting evidence, not confirmed.** One clarification said RaS keys off the CP court house UUID; RaS's own physical data model shows `court_code`'s example as a Court Register code ("YORKCC"), not a UUID. Left as a plain string in the schema (not UUID-locked) pending service analysis — do not treat either answer as settled. |
| `hearingDate` | `court_appearance.appearance_date` | CP hearing date | Confirmed |
| `hearingOutcome` | `court_appearance.appearance_outcome_id` | — | RaS: ignore, no CP-side action |
| `warrantType` | `court_appearance.warrant_type` | — | RaS: ignore, RaS derives it itself |
| `overallConvictionDate` | `court_appearance.overall_conviction_date` | — | RaS: resolved, derives from `charges[].convictionDate` — CP need not send |

### Next Court Appearance

| API field | RaS field | CP source | Status |
|---|---|---|---|
| `nextCourtAppearance.appearanceDate` | `next_court_appearance.appearance_date` | CP `nextHearing` | Open — which charge's `nextHearing` wins when several diverge is still with RaS (Nutty/Steven query on file) |
| `nextCourtAppearance.courtCentreId` | `next_court_appearance.court_code` | — | Open — not present in CP's next-appearance payload today |
| `nextCourtAppearance.appearanceTime` | `next_court_appearance.appearance_time` | — | Open — CP source not yet confirmed |
| `nextCourtAppearance.appearanceType` | `next_court_appearance.appearance_type_id` | — | Open — video-link-vs-in-person not yet confirmed in CP's payload |

### Charge

| API field | RaS field | CP source | Status |
|---|---|---|---|
| `offenceCode` | `charge.offence_code` | `offences[].offenceCode` | Confirmed — RaS confirmed CP codes align with OMS |
| `offenceStartDate` | `charge.offence_start_date` | `offences[].startDate` | Confirmed |
| `offenceEndDate` | `charge.offence_end_date` | `offences[]` (terminated results only) | Confirmed optional |
| `convictionDate` | `charge.conviction_date` | `offences[].convictionDate` | Confirmed |
| `terrorRelated` | `charge.terror_related` | `judicialResultPrompt` `promptReference` = `theCourtDeterminedATerroristConnectionUnderSection30OfTheCounterTerrorismAct2008AppliesToThisOffence`/`...Count`, inside the Imprisonment result's prompts | Open — value is text (`"Yes"`/`"No"`), needs coercion; RaS to confirm it keys off this exact promptReference |
| `foreignPowerRelated` | `charge.foreign_power_related` | `judicialResultPrompt` `promptReference` = `offenceAggByForeignPowerCondition`, confirmed `type: BOOLEAN` | Confirmed structurally — genuine boolean, no parsing needed |
| `domesticViolenceRelated` | `charge.domestic_violence_related` | `prosecutionCase.caseMarkers[]`, `markerTypeCode == 'DomesticViolence'` | Open — this is case-level in CP, charge-level in RaS; confirm whether every charge on a marked case should echo `true` |

### Charge Outcome

| API field | RaS field | CP source | Status |
|---|---|---|---|
| `cjsResultCode` | input to `charge_outcome.outcome_type` resolution | Judicial result CJS code | Confirmed as raw input |
| `resultText` | — (audit only) | Judicial result rendered text | Confirmed |
| `postHearingCustodyStatus` | input to `outcome_type` (NON_CUSTODIAL/REMAND/etc.) | `ResultDefinition.postHearingCustodyStatus`, Reference Data, keyed on `cjsResultCode` | Offered as a structured signal — RaS classification itself still TBD |
| `financial` / `category` / `convicted` | same | `ResultDefinition.financial`/`category`/`convicted`, Reference Data | Same — structured, offered, not a finished classification |

### Sentence

| API field | RaS field | CP source | Status |
|---|---|---|---|
| `countNumber` | `sentence.count_number` | `offences[].listingNumber` | Open — not yet confirmed as the right mapping; verify against warrant examples |
| `sentenceServeType` | `sentence.sentence_serve_type` | `concurrent` prompt; `consecutiveToSentenceImposedOn` presence | Open — `FORTHWITH` has no confirmed CP source |
| `consecutiveToId` | `sentence.consecutive_to_id` (FK) | `consecutiveToSentenceImposedOn` (date) + `whichWasImpBy` (court name) | Open — CP gives a date/court, not an ID; cross-reference mechanism undecided |
| `sentenceType` | `sentence.sentence_type_id` | CJS code (e.g. Imprisonment) | Open — RaS needs `nomis_cja_code`; CJS→CJA cross-reference TBD on RaS side |
| `fineAmount` | `sentence.fine_amount` | Imprisonment-in-default-of-fine prompts | Confirmed — explicitly excludes AOC (costs)/AOS (surcharge) |

### Period Length

| API field | RaS field | CP source | Status |
|---|---|---|---|
| `years`/`months`/`weeks`/`days` | `period_length.*` | `imprisonmentPeriod`/`totalCustodialPeriod` (free-text string) | Confirmed source, but needs CP-side parsing before it reaches the API — RaS will not accept the raw string |
| `periodOrder` | `period_length.period_order` | Constructed | Confirmed |
| `periodLengthType` | `period_length.period_length_type` | — | Open — mostly `Custodial_term`, full mapping depends on which sentence prompt produced it |

### Out of RaS scope, kept for other consumers

`courtApplicationResults[]` — RaS explicitly not interested in court admin applications. Kept in the API for whatever other consumer needs it; not part of the RaS-aligned mapping above.

---

## 2. Gap analysis — what the printed Prison Court Register surfaces that this API doesn't carry

Went back through every field `PrisonCourtRegisterPdfPayloadGenerator.java` actually renders and checked it against the schema above. Three different outcomes, not one:

### 2a. Already resolved — confirmed not needed, not a gap

- `registerDate` — generation timestamp, not a case fact.
- `officerInCase`, `parentGuardianName`, `parentGuardianAddress1` — hardcoded blank in the generator; nothing to map.
- `defendantResults[]`/`caseResults[]` (hearing-level and case-level free-text results, distinct from per-charge results) — no RaS field for these; covered by the existing `hearingOutcome` → "ignore" resolution, so this isn't a new gap, just the same one already closed.
- `offenceTitle`, `wording`, `allocationDecision`, `verdictCode`, `pleaValue`, `indicatedPleaValue` — human-readable/procedural detail with no corresponding RaS column visible in the physical data model. Treating as confirmed-not-needed, but **not yet asked of RaS directly** — worth a one-line confirmation rather than an assumption, since "no visible column" isn't the same as "RaS said no."

### 2b. Real structural gaps — fixed in the schema, not just flagged

- **Multiple results per offence, one `chargeOutcome`.** The document's `offences[].results[]` is an array — a single offence can carry more than one judicial result (e.g. a conviction plus a separate costs/ancillary result). The schema had `chargeOutcome` as a single object, matching `charge.charge_outcome_id`'s single-FK shape on RaS's side but silently forcing CP to pick one result and drop the rest. **Fixed:** renamed to `chargeOutcomes[]` (array) in the YAML — RaS decides which is authoritative rather than CP choosing for them.
- **`postHearingCustodyStatus` dropped entirely.** This was flagged as the key structured signal in the earlier draft of this document, but didn't make it into the RaS-aligned schema when the target was corrected. It's exactly the CP-side signal RaS needs to help classify `outcome_type` (custodial/remand/non-custodial). **Fixed:** added to `ChargeOutcome`, alongside `financial`/`category`/`convicted` from the same `ResultDefinition` lookup.
- **`applications[]` shape lost detail.** The document's applications carry `type`, `decision`, `decisionDate`, `response`, `responseDate`, and a `result[]` array — the schema's `CourtApplicationResult` had only `id`/`reference`/a single `result`. **Fixed:** expanded to match, even though this whole branch is out of RaS's scope — it's still meant to serve other consumers correctly.

### 2c. Real gaps — not fixed, flagged for a decision because there's no confirmed RaS target to fix them against

- **Custody location — correction, this claim was wrong in the earlier pass.** Previously described `custodyLocation` as "on every printed register." Not so: it's a key in the payload the generator builds, but checking the actual template (and the real `expected_output.pdf` from `OEELayout5IT`'s own fixture) confirms it never prints — it's in the generator's output JSON, absent from the rendered page. For the HMPPS API question specifically: this moves it from "surfaced on PCR, dropped from the API" to "not currently a case fact on the register at all" — if HMPPS/RaS genuinely need custody location, that's a new requirement to raise with them directly, not something this contract dropped.
- **Defendant identity (name, DOB, address, gender, nationality, aliases).** Entirely absent from the API — only `defendantId` (CP UUID) survives. This is deliberate given RaS's own `court_case` has no name/DOB/address columns. But RaS's Core Person Record matching explicitly runs on "name, DOB, address, PNC, CRO" as its clustering signals — worth directly confirming this event-level API isn't expected to carry any of those, rather than assuming Core Person Record is fed exclusively by some other pipeline.
- **Court venue display fields** (`ljaName`, `courtHouse` display name, `courtHouseAddress`). `courtCentreId` (UUID) covers RaS's actual join key, but if any downstream consumer needs a human-readable venue name/address rather than resolving the UUID themselves, that's currently nowhere in the contract. Likely fine to leave UUID-only, but worth a one-line confirmation rather than a silent assumption, same as 2a's procedural fields.

---

## 2d. Template double-check — what's *actually* rendered vs. what the generator produces

Went beyond the generator code and verified against the real `OEE_Layout5Template.docx` merge fields and the actual rendered output (`OEELayout5IT`'s `payload.json` → `expected_output.pdf` fixture — real printed text, not just template markup). Findings relevant to this contract:

- **`resultCode`/`cjsResultCode` is never rendered on the printed page** — the extracted PDF text only ever shows `"Result text ..."` lines, never a CJS code. This does not change the API — `cjsResultCode` stays as the correct structured input for RaS's `charge_outcome` resolution regardless of whether CP's own PDF prints it. Noted here only so the field's absence from the visible document is never mistaken for CP not having it in structured form.
- **`custodyLocation`'s absence is confirmed at the rendered-output level too** — it's a key in `payload.json` (the generator's own test input) but never appears in the extracted PDF text. Same conclusion as §2c, now confirmed against real output rather than template markup alone.
- **`ljaName`/`courtHouse`/`courtHouseAddress` render in the page header, repeated on every page** — confirmed in the extracted PDF text ("Liverpool Crown Court" / venue address / "Register generated on:" / "LJA:" all repeat identically on pages 1 and 2). Consistent with them being request-level, not per-case, fields.

---

## 3. Superseded framing (kept for the CP source-code trail)

The original version of this document treated `PrisonCourtRegisterPdfPayloadGenerator.java` as the API's source of truth, on the assumption `api-cp-crime-results-pcr` served the printed Prison Court Register. It doesn't — the real consumer is HMPPS RaS, and its physical data model is the actual target (§1 above). The CP-side findings from that earlier pass — judicial result prompts, `ResultDefinition` reference data, `caseMarkers` — carried forward correctly; only the target shape needed correcting, which is what §1 and §2 do here.
