# Feature ID: F-004

## Nom

Gestion des abonnements

## Contexte

Le cybercafé propose des abonnements donnant droit à une durée d’utilisation du cybercafé selon une formule définie.

## Acteurs

- Administrateur
- Employé du cybercafé

## Objectif

Permettre de définir des offres d’abonnement avec un prix et une durée de connexion incluse.

## Bounded context

- subscription

## Règles métier

- une offre d’abonnement possède un nom
- une offre d’abonnement possède un prix
- une offre d’abonnement possède une durée de connexion incluse
- une offre d’abonnement peut être active ou inactive
- une offre inactive ne peut plus être vendue
- les abonnements vendus doivent conserver les informations de l’offre au moment de la vente

## Backend

### Cas d’usage
- CreateSubscriptionOffer
- UpdateSubscriptionOffer
- ActivateSubscriptionOffer
- DeactivateSubscriptionOffer
- SearchSubscriptionOffers
- GetSubscriptionOfferDetails

### Domain
- SubscriptionOffer
- SubscriptionOfferId
- SubscriptionOfferName
- Money
- IncludedDuration
- SubscriptionOfferStatus

### Application
- CreateSubscriptionOfferUseCase
- UpdateSubscriptionOfferUseCase
- SearchSubscriptionOffersUseCase
- SubscriptionOfferSummaryDto
- SubscriptionOfferDetailsDto

### Infrastructure
- SubscriptionOfferJpaEntity
- SubscriptionOfferJpaRepository

### API
- GET /api/subscription-offers
- GET /api/subscription-offers/{id}
- POST /api/subscription-offers
- PUT /api/subscription-offers/{id}
- PUT /api/subscription-offers/{id}/activate
- PUT /api/subscription-offers/{id}/deactivate

## Frontend

### Écrans
- liste des abonnements
- détail abonnement
- création abonnement
- édition abonnement

### Composants
- subscription-offers-table
- subscription-offer-form
- duration-display
- offer-status-badge

## Critères d’acceptation

- un abonnement peut être créé avec un prix et une durée
- un abonnement inactif ne peut plus être vendu
- la liste affiche le prix et la durée
- les offres sont triables et filtrables