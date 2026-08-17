# Infrastructure — APIM Registration

This directory contains Terraform to register the Prosecution Case Results (PCR) API with the
HMCTS Shared Platform Services APIM instance.

---

## What it does

| Resource | Description |
|---|---|
| APIM Product | `cp-crime-results-pcr` — subscription tier grouping APIs |
| APIM API | `pcrresults` — registered from the OpenAPI spec at `src/main/resources/openapi/openapi-spec.yml` |

The OpenAPI spec is the single source of truth — display name, path, and operations
are all derived from it automatically.

---

## CI/CD

The `.github/workflows/terraform-infra.yaml` workflow runs automatically:

| Trigger | Action |
|---|---|
| Pull request to `main` | `terraform plan` — output posted as PR comment |
| Push to `main` | `terraform apply` |
| Manual dispatch | Choose `plan` or `apply` from the Actions tab |

Environments are discovered automatically from `*.tfvars` files — adding `dev.tfvars`
will add a dev deployment job with no further pipeline changes needed.

### Required GitHub Actions variables

These are set at **repository level** (Settings → Secrets and variables → Variables) — they are
not inherited from the org. Copy from `api-cp-crime-schedulingandlisting-courtschedule` when
adding a new environment.

| Variable | Value | Description |
|---|---|---|
| `AZURE_CLIENT_ID_SBOX` | `72a7651d-9d2a-4c04-9085-957efeab0e53` | Client ID of the OIDC app registration for sbox |
| `AZURE_SUBSCRIPTION_ID_SBOX` | `bd2864ed-4f3e-45ed-9c6a-8d179674bab1` | Azure subscription ID for sbox |
| `AZURE_SUBSCRIPTION_SBOX` | `DTS-SPS-SBOX` | Subscription display name for sbox |

Note: `TFSTATE_STORAGE_ACCOUNT_NONPROD` is **not** required — the Terraform state storage account
is hardcoded in the workflow as `spsapimapi<env>state` (e.g. `spsapimapisboxstate`).

Authentication uses OpenID Connect — no passwords or secrets are stored.

### Federated identity configuration

The OIDC app registration (`72a7651d-9d2a-4c04-9085-957efeab0e53`) is managed centrally in
[`hmcts/azure-github-federation-config`](https://github.com/hmcts/azure-github-federation-config).
Each repo that uses it must be explicitly added as a federated credential subject — this repo was
added in commit `cb35887`. When adding a new repo or environment, raise a PR there to add entries
for both `pull_request` and `ref:refs/heads/main`:

```yaml
- 'repo:hmcts/your-repo-name:pull_request'
- 'repo:hmcts/your-repo-name:ref:refs/heads/main'
```

Without this the workflow fails at **Azure Login (OIDC)** with
`Not all values are present. Ensure 'client-id' and 'tenant-id' are supplied.`

---

## Running locally

```bash
az login
az account set --subscription bd2864ed-4f3e-45ed-9c6a-8d179674bab1

cd infrastructure
terraform init
terraform plan -var-file=sbox.tfvars
terraform apply -var-file=sbox.tfvars
```

---

## Adding a new environment

1. Copy `sbox.tfvars` to `<env>.tfvars`
2. Update `api_mgmt_rg`, `api_mgmt_name`, and `service_url` for the target environment
3. Ensure the GitHub Actions variables for that environment are set on the repo
4. Raise a PR — the pipeline will pick up the new environment automatically
