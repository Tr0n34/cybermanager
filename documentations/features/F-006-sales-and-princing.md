# Feature ID: F-006

## Nom

Gestion des ventes et des tarifs

## Contexte

Le cybercafe doit vendre des produits, des abonnements et du temps de connexion selon une tarification definie, sans melanger le referentiel des abonnes et les clients journaliers crees dans les sessions.

## Acteurs

- Employe du cybercafe
- Administrateur

## Objectif

Permettre l'enregistrement d'une vente de produit, d'abonnement ou de temps de connexion.

La tarification du temps de connexion doit reposer sur une grille configurable de plages de duree, partagee entre les ventes de temps et la facturation des sessions journalieres.

## Bounded context

- sales
- pricing
- catalog
- subscription
- customer

## Regles metier

- une vente peut contenir un ou plusieurs produits
- une vente peut contenir un abonnement
- une vente peut contenir un achat de temps de connexion
- le prix du temps de connexion est defini selon une grille tarifaire de plages de duree
- chaque plage de duree est definie en heures, minutes et prix
- la meme grille est utilisee par les ventes de temps et par les sessions journalieres
- une vente doit conserver les montants reellement appliques au moment du paiement
- une vente est rattachee a un client ou a un abonne
- une vente possede une date et une heure
- les abonnes doivent etre recherchables sans liste exhaustive
- les clients journaliers utilises dans les ventes proviennent des flux de session ou du contexte du jour

## Backend

### Cas d'usage
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
- SaleView
- ConnectionPricingView

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

### Ecrans
- ecran de vente
- historique des ventes du jour
- configuration tarifaire de connexion
- ecran dedie de configuration des tarifs d'usage dans le temps
- navigation depuis le menu principal vers l'ecran ventes

### Composants
- sale-form
- product-selector
- subscription-selector
- duration-pricing-form
- connection-pricing-settings
- sales-of-day-table

### Navigation et comportements UI
- l'utilisateur accede a l'ecran ventes depuis la navigation principale
- l'utilisateur accede a l'ecran de configuration des tarifs temps depuis le menu principal ou depuis l'ecran ventes
- les listes de produits et d'abonnements se chargent avant la creation d'une vente
- la selection d'un client ne doit pas reposer sur une liste exhaustive de tous les clients journaliers historiques
- un abonne peut etre retrouve par recherche
- chaque creation de vente met a jour l'historique du jour sans rechargement manuel de la page
- la configuration tarifaire permet d'ajouter, retirer et enregistrer des plages de duree
- l'ecran de configuration affiche la grille active avec la duree et le prix de chaque plage
- la configuration tarifaire met a jour l'affichage du formulaire de vente de temps et la facturation des sessions journalieres
- les erreurs de calcul ou d'enregistrement sont affichees dans l'ecran

## Criteres d'acceptation

- un employe peut vendre un produit
- un employe peut vendre un abonnement
- un employe peut vendre du temps de connexion
- le prix applique est correctement calcule et enregistre
- un administrateur peut configurer plusieurs plages de duree avec leur prix
- les ventes du jour sont consultables
- les boutons d'action permettent effectivement de creer les ventes et de rafraichir la liste du jour
