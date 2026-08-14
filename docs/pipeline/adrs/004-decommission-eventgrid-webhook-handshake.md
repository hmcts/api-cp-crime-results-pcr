# 004: Decommission the Event Grid webhook handshake — pcr-eventgrid-relay-function is the live path

## Status

Accepted

## Context

ADR-007/AMP-892 originally had Azure Event Grid delivering `Hearing_Resulted` events straight to
this service's own webhook (`/internal/hearing-results`), with this service answering Event
Grid's subscription-validation handshake itself. That design was superseded by
`pcr-eventgrid-relay-function` — a standalone Function App that owns the real Event Grid
subscription and answers the handshake itself via its own `@EventGridTrigger` binding, then
relays each event verbatim to this same endpoint as a plain internal HTTP call.

An earlier attempt to remove the now-dead handshake code (PR #40, this repo and
`service-cp-crime-results-pcr`) was reverted (PR #43) to defer the decommission, not because the
reasoning was wrong. A fresh impact analysis (14 Aug 2026) confirms the relay function is the
real, live path and the direct-webhook design was never actually provisioned:

- `pcr-eventgrid-relay-function` is an active, deployed repo (22 commits, most recent the day of
  this ADR) with a real Event Grid subscription (`egs-pcr-relay`) on `eg-ste-ccp0121-hearingres`
  already verified delivering real events in STE, including working TLS trust to this service.
- The subscription ADR-007 describes as "already provisioned" does not exist — of the 13 real
  subscriptions on that topic, none targets this service's webhook directly. Nothing has ever
  delivered to it.
- The relay function has no dependency on this repo's generated models — it parses events as
  raw JSON and posts plain HTTP, so this change has no effect on it.

## Decision

- `HearingResultedWebhookEvent` is renamed to `HearingResultedEvent`; `receiveHearingResultedWebhook`
  operationId renamed to `receiveHearingResultedEvent`.
- `HearingResultedEvent.data.validationCode`/`.validationUrl` are removed — this operation will
  never receive a `Microsoft.EventGrid.SubscriptionValidationEvent` again.
- `WebhookAck` schema is removed entirely; the `200` response now returns no body.

## Consequences

- `service-cp-crime-results-pcr`'s `HearingResultedWebhookController`/`HearingResultedWebhookService`
  are renamed to `HearingResultedEventController`/`HearingResultedEventService`, and the
  handshake-echo branch is deleted along with its tests.
- ADR-007 in `service-cp-crime-results-pcr` needs a superseded note recorded directly in that
  file this time (the previous attempt's note was lost when PR #40 was reverted wholesale).
- `pcr-eventgrid-relay-function` needs no changes — it already sends the plain relayed shape this
  contract now reflects.
