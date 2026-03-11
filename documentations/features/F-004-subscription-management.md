# Feature ID: F-004

## Nom

Gestion des abonnements

## Contexte

Le cybercafe propose des abonnements donnant droit a une duree d'utilisation du cybercafe selon une formule definie.

## Acteurs

- Administrateur
- Employe du cybercafe

## Objectif

Permettre de definir des offres d'abonnement avec un prix et une duree de connexion incluse.

## Bounded context

- subscription

## Regles metier

- une offre d'abonnement possede un nom
- une offre d'abonnement possede un prix
- une offre d'abonnement possede une duree de connexion incluse
- une offre d'abonnement peut etre active ou inactive
- une offre inactive ne peut plus etre vendue
- les abonnements vendus doivent conserver les informations de l'offre au moment de la vente

## Backend

### Cas d'usage
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
- SubscriptionOfferView

### Infrastructure
- SubscriptionOfferJpaEntity
- SubscriptionOfferJpaRepository
- SubscriptionOfferRepositoryAdapter

### API
- GET /api/subscription-offers
- GET /api/subscription-offers/{id}
- POST /api/subscription-offers
- PUT /api/subscription-offers/{id}
- PUT /api/subscription-offers/{id}/activate
- PUT /api/subscription-offers/{id}/deactivate

## Frontend

### Ecrans
- liste des abonnements
- detail abonnement
- creation abonnement
- edition abonnement
- navigation depuis le shell principal vers l'ecran abonnements

### Composants
- subscription-offers-table
- subscription-offer-form
- duration-display
- offer-status-badge

### Navigation et comportements UI
- la liste des offres se charge a l'ouverture
- le bouton de creation ouvre un formulaire vide
- l'edition d'une offre recharge le formulaire avec les donnees courantes
- apres sauvegarde ou changement de statut, la liste est rechargee
- l'utilisateur reste sur le meme ecran apres action avec retour immediat sur la liste mise a jour
- les messages d'erreur restent visibles dans la page

## Criteres d'acceptation

- un abonnement peut etre cree avec un prix et une duree
- un abonnement inactif ne peut plus etre vendu
- la liste affiche le prix et la duree
- les offres sont triables et filtrables
- le bouton de creation et les actions de liste sont operationnels dans l'UI
