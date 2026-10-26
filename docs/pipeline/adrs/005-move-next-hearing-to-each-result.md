# 005: Move nextHearing to each judicial result

## Status

Accepted

## Context

CP records a next hearing against the judicial result that sets it, e.g. "RI - Remanded in
custody" or "NHMC - Next hearing in magistrates' court". One offence can carry several such
results with different dates, and a defendant's offences can be adjourned to different dates.

The contract carried a single `hearing.nextHearing`, which forces one date to be chosen from
several. HMPPS reported the
hearing-level date not matching the date on the remand warrant (AMP-1292). The date that
applies is a business rule (in the magistrates' court, the date on the remand warrant), not
something the API can derive reliably — "earliest" picks a different date from the warrant in
real cases.

## Decision

- Add `nextHearing` to `ResultText`, carried on every result CP records one against: offence
  results, court application results, and defendant/case-level results.
- Remove `HearingDetails.nextHearing`.
- `NextHearing.hearingId` stays in the schema and remains unpopulated — CP's results carry only a
  provisional booking reference for the next hearing.

HMPPS, the consumer of `hearing.nextHearing`, agreed to this change.

## Consequences

- Breaking change: `HearingDetails.nextHearing` no longer exists.
  Consumers pick the next hearing they need from `results[].nextHearing`, e.g. the one on the
  remand result.
- Every next hearing reaches the consumer as structured data, tied to the result that set it,
  instead of only in that result's free-text prompts.
- A result with no listed date (e.g. "date to be fixed") carries no `nextHearing`.
