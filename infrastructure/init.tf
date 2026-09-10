terraform {
  required_version = ">= 1.16.2, < 2.0.0"
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "5.4.0"
    }
  }
  backend "azurerm" {}
}

provider "azurerm" {
  features {}
}
