# Feature ID: F-008

## Nom

Transformation d'un client journalier en abonne

## Contexte

Un client journalier peut decider de prendre un abonnement pendant ou apres sa session. Le systeme doit permettre cette conversion depuis le flux session, puis rendre le client visible et consultable dans l'ecran client.

## Acteurs

- Employe du cybercafe

## Objectif

Transformer un client journalier en abonne depuis sa session active et pouvoir deduire la session en cours du credit achete si demande.

## Bounded context

- customer
- subscription
- session
- sales

## Regles metier

- un client journalier peut devenir abonne
- l'historique du client doit etre conserve
- la vente de l'abonnement doit etre enregistree
- la conversion peut etre declenchee depuis une session en cours
- si la conversion a lieu pendant la session, le temps consomme peut etre deduit du nouveau credit d'abonnement
- le client converti devient visible dans l'ecran client avec son abonnement en cours et ses achats
- les futurs abonnements achetes par ce client s'ajoutent au credit existant

## Backend

### Cas d'usage
- ConvertCustomerToSubscriber
- SellSubscriptionToExistingCustomer
- DeductCurrentSessionFromNewSubscription

### Domain
- Customer
- Subscriber
- SubscriptionPurchase
- RemainingTimeCredit
- ConversionResult

### Application
- ConvertCustomerToSubscriberUseCase
- ConversionView
- CustomerDetailsView

### Infrastructure
- CustomerJpaEntity
- SubscriberJpaEntity
- SubscriptionPurchaseJpaEntity
- SaleRepositoryAdapter

### API
- POST /api/customers/{id}/convert-to-subscriber
- GET /api/customers/{id}

## Frontend

### Ecrans
- conversion depuis une session journaliere active
- retour sur la session convertie
- consultation ulterieure depuis la fiche client

### Composants
- convert-to-subscriber-form
- subscription-offer-selector
- conversion-summary-panel
- customer-details-panel

### Navigation et comportements UI
- le formulaire de conversion est accessible depuis la colonne de gauche de l'ecran sessions
- les offres d'abonnement sont chargees avant validation
- apres conversion, la session recharge les informations client avec le nouveau type
- le client converti apparait ensuite dans l'ecran client avec son abonnement en cours
- le client converti apparait ensuite dans l'ecran client avec son abonnement en cours, ses achats et ses dettes ouvertes eventuelles
- les ecrans de conversion et de consultation rappellent que les abonnements se cumulent
- les erreurs de conversion restent visibles dans l'ecran

## Criteres d'acceptation

- un client journalier actif peut etre converti en abonne
- l'abonnement choisi est vendu au moment de la conversion
- le temps de session peut etre deduit du nouveau forfait si demande
- l'historique du client est conserve
- le client converti devient consultable dans l'ecran client
- la conversion reste faisable sans quitter le flux comptoir
