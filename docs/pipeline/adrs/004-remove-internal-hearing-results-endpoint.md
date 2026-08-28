# 004: Remove POST /internal/hearing-results

## Status

Accepted

## Context

`POST /internal/hearing-results` was the synchronous ingestion path: Event Grid →
`pcr-eventgrid-relay-function` → this endpoint → `service-cp-crime-results-pcr`. AMP-1030 replaced
it with Event Grid delivering directly to a Service Bus queue (`pcr.hearing-resulted`) owned by
the service, consumed via `HearingResultedServiceBusConsumer`. `service-cp-crime-results-pcr` has
since removed the endpoint's implementation entirely (`HearingResultedEventController`,
`HearingResultedEventService`) — nothing in that repo implements this operation any more.

`pcr-eventgrid-relay-function` — the endpoint's only caller — has never been deployed beyond a
single test environment (its own `docs/TODO-production-readiness.md`: "exactly one `*pcrrelay*`
app exists, in STE-CCP0121. There is no DEV, SIT, NFT or production instance, and no promotion
path"). `service-cp-crime-results-pcr` is the only consumer of this spec.

## Decision

Remove the `/internal/hearing-results` path, its `Internal` tag, and the `receiveHearingResultedEvent`
operation from the spec.

`HearingResultedEvent`/`HearingResultedEventData` stay — they're still real, generated models,
now used by `HearingResultedServiceBusConsumer` to deserialize Service Bus queue messages, not
just by the removed HTTP operation.

## Consequences

- Breaking change: the `InternalApi` generated interface no longer exists (no path carries the
  `Internal` tag any more) — nothing currently implements it, so no code is orphaned by this.
- `pcr-eventgrid-relay-function` itself is not retired by this change — it's a separate,
  cross-repo decision. Its own docs already flag this as open (§5.1, "decide the fate of
  `HearingResultedWebhookController`").
