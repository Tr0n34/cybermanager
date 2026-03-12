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

## Ameliorations integrees

- ajout d'un service applicatif dedie aux offres d'abonnement
- exposition d'une recherche REST avec filtre par libelle et statut
- standardisation de la duree incluse en `includedMinutes`
- ajout d'une page Angular de gestion des offres avec modele et service API dedies
- ajout de la suppression des offres
- refonte de l'ecran en mode liste compacte + panneau lateral refermable
- filtres instantanes et pagination avec taille de page configurable

## Bounded context

- subscription

## Regles metier

- une offre d'abonnement possede un nom
- une offre d'abonnement possede un prix
- une offre d'abonnement possede une duree de connexion incluse
- une offre d'abonnement peut etre active ou inactive
- une offre inactive ne peut plus etre vendue
- les abonnements vendus doivent conserver les informations de l'offre au moment de la vente
- les abonnements achetes par un meme client sont cumulables

## Backend

### Cas d'usage
- CreateSubscriptionOffer
- UpdateSubscriptionOffer
- ActivateSubscriptionOffer
- DeactivateSubscriptionOffer
- DeleteSubscriptionOffer
- SearchSubscriptionOffers
- GetSubscriptionOfferDetails

### Domain
- SubscriptionOffer
- SubscriptionOfferId
- Money
- SubscriptionOfferStatus

### Application
- CreateSubscriptionOfferUseCase
- UpdateSubscriptionOfferUseCase
- ActivateSubscriptionOfferUseCase
- DeactivateSubscriptionOfferUseCase
- SearchSubscriptionOffersUseCase
- GetSubscriptionOfferDetailsUseCase
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
- DELETE /api/subscription-offers/{id}
- PUT /api/subscription-offers/{id}/activate
- PUT /api/subscription-offers/{id}/deactivate

### Parametres et comportements API
- `GET /api/subscription-offers` accepte `term` et `status`
- les DTO d'entree et de sortie exposent `name`, `price`, `includedMinutes` et `status`
- les routes d'ecriture exigent un bearer token et restituent l'offre courante apres traitement

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
- le filtrage par libelle et statut est immediat et client side
- la taille de page est configurable et la pagination garde le meme flux utilisateur
- le bouton de creation ouvre un panneau de droite vide avec animation puis disparait tant que le panneau est ouvert
- l'edition d'une offre recharge le formulaire avec les donnees courantes dans ce meme panneau
- apres sauvegarde, changement de statut ou suppression, la liste est rechargee
- l'utilisateur reste sur le meme ecran apres action avec retour immediat sur la liste mise a jour
- les messages d'erreur restent visibles dans la page
- la duree incluse est saisie et affichee en minutes pour rester coherente avec l'API
- les ecrans qui vendent ou resumment un abonnement doivent rappeler que les abonnements se cumulent

## Criteres d'acceptation

- un abonnement peut etre cree avec un prix et une duree
- un abonnement inactif ne peut plus etre vendu
- la liste affiche le prix et la duree
- les offres sont triables et filtrables
- le bouton de creation et les actions de liste sont operationnels dans l'UI
- une offre peut etre supprimee depuis l'ecran d'edition
