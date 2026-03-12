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

## Ameliorations integrees

- ajout d'un service applicatif unique pour les ventes du jour, le detail de vente et la configuration tarifaire
- prise en charge effective des trois flux de vente : produits, abonnements, temps de connexion
- ajout d'une API de consultation des ventes du jour parametree par date
- ajout d'une page Angular de vente et d'une page dediee a la configuration de la grille tarifaire
- refonte de l'ecran ventes en mode liste + panneau lateral comme les autres referentiels
- remplacement de la liste exhaustive des clients par une recherche ciblee avec autocompletion
- ajout de la vente produit depuis le flux session pour un client deja selectionne
- ajout d'une case a cocher pour enregistrer une vente en dette, quel que soit le type de vente

## Bounded context

- sales
- product
- subscription
- customer
- session

## Regles metier

- une vente peut contenir un ou plusieurs produits
- une vente peut contenir un abonnement
- une vente peut contenir un achat de temps de connexion
- l'achat d'un abonnement ajoute son credit au credit deja disponible
- le prix du temps de connexion est defini selon une grille tarifaire de plages de duree
- chaque plage de duree est definie en heures, minutes et prix
- la meme grille est utilisee par les ventes de temps et par les sessions journalieres
- une vente doit conserver les montants reellement appliques au moment du paiement
- une vente est rattachee a un client ou a un abonne
- une vente possede une date et une heure
- les abonnes doivent etre recherchables sans liste exhaustive
- les clients journaliers utilises dans les ventes proviennent des flux de session ou du contexte du jour
- toute vente peut etre transformee en dette ouverte si l'utilisateur choisit de ne pas encaisser immediatement
- les dates et heures affichees dans les historiques de vente suivent le format `JJ/MM/AAAA HH:mm`

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

### Application
- CreateProductSaleUseCase
- CreateSubscriptionSaleUseCase
- CreateConnectionTimeSaleUseCase
- ConfigureConnectionPricingUseCase
- SearchSalesOfDayUseCase
- GetSaleDetailsUseCase
- SaleView
- ConnectionPricingView

### Infrastructure
- SaleJpaEntity
- SaleLineJpaEntity
- SaleJpaRepository
- ConnectionPricingJpaRepository
- ConnectionPricingRepositoryAdapter

### API
- GET /api/sales/day
- GET /api/sales/{id}
- POST /api/sales/products
- POST /api/sales/subscriptions
- POST /api/sales/connection-time
- GET /api/pricing/connection-time
- PUT /api/pricing/connection-time

### Parametres et comportements API
- `GET /api/sales/day` accepte `date` au format ISO `yyyy-MM-dd`, sinon utilise la date du jour
- `POST /api/sales/products` attend `customerId`, une collection de lignes `{ productId, quantity }` et `createDebt`
- `POST /api/sales/subscriptions` attend `customerId`, `subscriptionOfferId` et `createDebt`
- `POST /api/sales/connection-time` attend `customerId`, `minutes` et `createDebt`
- `GET /api/pricing/connection-time` retourne la grille active avec `hours`, `minutes`, `durationMinutes` et `price`
- les erreurs metier retournent un payload stable avec `code`, `message`, `status` et `timestamp`

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
- pricing-tier-table

### Navigation et comportements UI
- l'utilisateur accede a l'ecran ventes depuis la navigation principale
- l'utilisateur accede a l'ecran de configuration des tarifs temps depuis le menu principal ou depuis l'ecran ventes
- les listes de produits et d'abonnements se chargent avant la creation d'une vente
- la selection d'un client ne doit pas reposer sur une liste exhaustive de tous les clients journaliers historiques
- un client ou un abonne est retrouve par autocompletion sur saisie du nom
- la vente produit impose une selection effective du produit dans le formulaire
- chaque formulaire de vente propose une case a cocher `Creer une dette au lieu d'encaisser`
- le formulaire de vente d'abonnement rappelle que les abonnements se cumulent
- le bouton d'ouverture du panneau de vente masque le bouton de creation tant que le panneau est ouvert
- chaque creation de vente met a jour l'historique du jour sans rechargement manuel de la page
- l'historique et les tableaux de vente restent compacts avec espaces reduits pour un usage comptoir
- la configuration tarifaire permet d'ajouter, retirer et enregistrer des plages de duree
- l'ecran de configuration affiche la grille active avec la duree et le prix de chaque plage
- la configuration tarifaire met a jour l'affichage du formulaire de vente de temps et la facturation des sessions journalieres
- l'historique du jour peut etre recharge pour une date precise afin de verifier un encaissement
- les erreurs de calcul ou d'enregistrement sont affichees dans l'ecran
- depuis l'ecran sessions, une vente produit peut etre lancee pour le client de la session en cours sans ressaisir le client

## Criteres d'acceptation

- un employe peut vendre un produit
- un employe peut vendre un abonnement
- un employe peut vendre du temps de connexion
- le prix applique est correctement calcule et enregistre
- un administrateur peut configurer plusieurs plages de duree avec leur prix
- les ventes du jour sont consultables
- les boutons d'action permettent effectivement de creer les ventes et de rafraichir la liste du jour
- la grille active est consultable sans ouvrir l'ecran d'edition
- l'utilisateur ne parcourt jamais une liste exhaustive de clients pour vendre
- une vente peut etre enregistree directement en dette ouverte depuis le formulaire
