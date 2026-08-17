api_mgmt_rg   = "rg-sps-platform-sbox"
api_mgmt_name = "sps-api-mgmt-sbox"

apim_product = {
  name                          = "cp-crime-results-pcr"
  subscription_required         = true
  subscriptions_limit           = 20
  approval_required             = false
  published                     = true
  product_access_control_groups = ["developers", "administrators", "guests"]
}

entra_tenant_id = "e2995d11-9947-4e78-9de6-d44e0603518e"
entra_client_id = "d03af961-b3a6-4b59-ba5e-ad16a6329b6b"

apis = {
  pcrresults = {
    openapi_spec_path = "../src/main/resources/openapi/openapi-spec.yml"
    service_host      = "devamp01-appgw.dev.nl.cjscp"
    service_path      = ""
    revision          = "1"
  }
}
