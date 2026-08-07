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

These must be set on the repository (Settings → Secrets and variables → Variables):

| Variable | Description |
|---|---|
| `AZURE_CLIENT_ID_SBOX` | Client ID of the OIDC app registration for sbox |
| `AZURE_SUBSCRIPTION_ID_SBOX` | Azure subscription ID for sbox (`bd2864ed-4f3e-45ed-9c6a-8d179674bab1`) |
| `TFSTATE_STORAGE_ACCOUNT_NONPROD` | Storage account name for Terraform state (non-prod) |

Authentication uses OpenID Connect — no passwords or secrets are stored.

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
