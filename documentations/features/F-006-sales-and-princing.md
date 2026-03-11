# Feature ID: F-006

## Nom

Gestion des ventes et des tarifs

## Contexte

Le cybercafé doit vendre des produits, des abonnements et du temps de connexion selon une tarification définie.

## Acteurs

- Employé du cybercafé
- Administrateur

## Objectif

Permettre l’enregistrement d’une vente de produit, d’abonnement ou de temps de connexion.

## Bounded context

- sales
- pricing
- catalog
- subscription
- customer

## Règles métier

- une vente peut contenir un ou plusieurs produits
- une vente peut contenir un abonnement
- une vente peut contenir un achat de temps de connexion
- le prix du temps de connexion est défini selon une durée ou une grille tarifaire
- une vente doit conserver les montants réellement appliqués au moment du paiement
- une vente est rattachée à un client ou à un abonné
- une vente possède une date et une heure

## Backend

### Cas d’usage
- CreateProductSale
- CreateSubscriptionSale
- CreateConnectionTimeSale
- GetSaleDetails
- SearchSalesOfDay
- ConfigureConnectionPricing

### Domain
- Sale
- SaleId
- SaleLine
- SaleType
- Money
- ConnectionPricingRule
- PurchasedDuration

### Application
- CreateProductSaleUseCase
- CreateSubscriptionSaleUseCase
- CreateConnectionTimeSaleUseCase
- ConfigureConnectionPricingUseCase
- SaleDetailsDto
- DaySaleSummaryDto

### Infrastructure
- SaleJpaEntity
- SaleLineJpaEntity
- SaleJpaRepository
- PricingRuleJpaEntity
- PricingRuleJpaRepository

### API
- GET /api/sales/day
- GET /api/sales/{id}
- POST /api/sales/products
- POST /api/sales/subscriptions
- POST /api/sales/connection-time
- PUT /api/pricing/connection-time

## Frontend

### Écrans
- écran de vente
- historique des ventes du jour
- configuration tarifaire de connexion

### Composants
- sale-form
- product-selector
- subscription-selector
- duration-pricing-form
- sales-of-day-table

## Critères d’acceptation

- un employé peut vendre un produit
- un employé peut vendre un abonnement
- un employé peut vendre du temps de connexion
- le prix appliqué est correctement calculé et enregistré
- les ventes du jour sont consultables