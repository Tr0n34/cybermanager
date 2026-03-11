# Feature ID: F-008

## Nom

Transformation d’un client en abonné

## Contexte

Un client occasionnel peut décider de prendre un abonnement à la fin de sa session ou après une consommation. Le système doit permettre cette conversion sans perdre l’historique ni interrompre la logique métier de la journée.

## Acteurs

- Employé du cybercafé

## Objectif

Transformer un client occasionnel en abonné et, si l’abonnement est pris en fin de session, pouvoir immédiatement déduire le temps consommé du crédit inclus dans l’abonnement.

## Bounded context

- customer
- subscription
- session
- sales

## Règles métier

- un client occasionnel peut devenir abonné
- l’historique du client doit être conservé
- la vente de l’abonnement doit être enregistrée
- si la conversion a lieu à la fin d’une session, le temps consommé peut être déduit du nouveau crédit d’abonnement
- le client ne doit pas être dupliqué inutilement dans le système
- le statut du client doit refléter sa nouvelle situation d’abonné

## Backend

### Cas d’usage
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
- ConversionResultDto
- SubscriberDetailsDto

### Infrastructure
- CustomerJpaEntity
- SubscriberJpaEntity
- SubscriptionPurchaseJpaEntity

### API
- POST /api/customers/{id}/convert-to-subscriber

## Frontend

### Écrans
- action de conversion depuis la fiche client
- action de conversion depuis la clôture de session

### Composants
- convert-to-subscriber-form
- subscription-offer-selector
- conversion-summary-panel

## Critères d’acceptation

- un client occasionnel peut être converti en abonné
- l’abonnement choisi est vendu au moment de la conversion
- le temps de session peut être déduit du nouveau forfait si demandé
- l’historique du client est conservé