# Feature ID: F-005

## Nom

Gestion des clients sans abonnement

## Contexte

Le cybercafé accueille des clients occasionnels qui viennent sans abonnement et paient à la durée ou à la consommation.

## Acteurs

- Employé du cybercafé

## Objectif

Permettre l’enregistrement, la consultation et le suivi des clients occasionnels.

## Bounded context

- customer

## Règles métier

- un client occasionnel peut être créé rapidement
- un client occasionnel peut être identifié par son nom ou un identifiant interne
- un client occasionnel peut acheter des produits
- un client occasionnel peut démarrer une session de connexion
- un client occasionnel peut ensuite être transformé en abonné

## Backend

### Cas d’usage
- CreateWalkInCustomer
- UpdateWalkInCustomer
- SearchWalkInCustomers
- GetWalkInCustomerDetails

### Domain
- Customer
- CustomerId
- CustomerType
- CustomerName
- CustomerStatus

### Application
- CreateWalkInCustomerUseCase
- UpdateWalkInCustomerUseCase
- SearchCustomersUseCase
- CustomerSummaryDto
- CustomerDetailsDto

### Infrastructure
- CustomerJpaEntity
- CustomerJpaRepository

### API
- GET /api/customers
- GET /api/customers/{id}
- POST /api/customers
- PUT /api/customers/{id}

## Frontend

### Écrans
- liste des clients
- création rapide client
- détail client

### Composants
- customers-table
- quick-customer-form
- customer-type-badge
- customer-search-box

## Critères d’acceptation

- un employé peut créer rapidement un client sans abonnement
- un client est retrouvable dans la journée
- un client peut être utilisé dans une vente
- un client occasionnel peut être converti plus tard en abonné