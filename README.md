# CP Crime Prosecution Case Results (PCR) API

`api-cp-crime-results-pcr` — the OpenAPI specification for the **Prosecution
Case Results (PCR)** API on the Common Platform (CP).

This is a **specification-only** repository (`api-cp-*`). It contains the
OpenAPI contract, validation tooling, and publishing workflow. The runtime
implementation lives in the matching service repo,
[`service-cp-crime-results-pcr`](https://github.com/hmcts/service-cp-crime-results-pcr),
which consumes the artefact published from here.

> 🔗 The contract follows the [HMCTS RESTful API Standards](https://hmcts.github.io/restful-api-standards/).

## What this API exposes

Prosecution case results (PCR) recorded for a criminal case at a hearing — the
result for each offence and each court application, including any sentence
handed down. Every request reads a stable snapshot identified by a results
`eventId` rather than a mutable "latest" view.

| Method & path | Purpose |
|---|---|
| `GET /pcr/cases/{caseURN}/hearings/{hearingId}?eventId={uuid}` | Results for a whole prosecution case at a hearing, broken down per defendant |
| `GET /pcr/cases/{caseURN}/hearings/{hearingId}/defendants/{defendantId}?eventId={uuid}` | Results narrowed to a single defendant at that hearing |

The spec lives at
[`src/main/resources/openapi/openapi-spec.yml`](./src/main/resources/openapi/openapi-spec.yml).

## Ownership

- **GitHub team:** [`api-marketplace`](https://github.com/orgs/hmcts/teams/api-marketplace) (admin)
- **Product team:** _TODO — human product-team name_
- **Support / on-call:** _TODO — support model and on-call rota_
- **Escalation:** _TODO — escalation path_

## Consumers

- [`service-cp-crime-results-pcr`](https://github.com/hmcts/service-cp-crime-results-pcr) (primary)

## Versioning

Media-type + SemVer versioning per
[`docs/API-VERSIONING-STRATEGY.md`](./docs/API-VERSIONING-STRATEGY.md) and
[`docs/OPENAPI-SPEC-VERSIONING.md`](./docs/OPENAPI-SPEC-VERSIONING.md). The
published artefact coordinate consumers depend on is:

```
uk.gov.hmcts.cp:api-cp-crime-results-pcr:<version>
```

Breaking changes require a new major version and an ADR in each consuming
service.

## Supporting documents

See the [`docs`](./docs) directory:

- [`API-VERSIONING-STRATEGY.md`](./docs/API-VERSIONING-STRATEGY.md) – media-type + SemVer versioning.
- [`OPENAPI-FILE-CONVENTIONS.md`](./docs/OPENAPI-FILE-CONVENTIONS.md) – OpenAPI file and content conventions.
- [`OPENAPI-SPEC-VERSIONING.md`](./docs/OPENAPI-SPEC-VERSIONING.md) – rules for evolving the spec.
- [`GITHUB-ACTIONS.md`](./docs/GITHUB-ACTIONS.md) – workflows, required secrets and variables.
- [`CHAIN_OF_CUSTODY.md`](./docs/CHAIN_OF_CUSTODY.md) – software supply-chain and audit trail.
- [`DATA-PRODUCTS.md`](./docs/DATA-PRODUCTS.md) – structured data outputs.

> **Note:** the build requires secrets and variables in project settings — see
> [GitHub Actions: Required Secrets and Variables](./docs/GITHUB-ACTIONS.md).

## New team member setup

Anyone newly added to the owning team should do this once to confirm they can
actually push (catches the gap between "team has repo access" and "this person
can push"):

```bash
gh auth login                                       # if not already authenticated
git clone git@github.com:hmcts/api-cp-crime-results-pcr.git
cd api-cp-crime-results-pcr
git checkout -b smoke/access-check
git commit --allow-empty -m "chore: verify push access"
git push -u origin smoke/access-check
git push origin --delete smoke/access-check          # clean up the throwaway branch
```

If the push is rejected with a permissions error, check the `api-marketplace`
team grant and your membership of the team before assuming a tooling problem.

## Contributing

Contributions are welcome — see [CONTRIBUTING.md](.github/CONTRIBUTING.md).

## License

Licensed under the [MIT License](LICENSE).
