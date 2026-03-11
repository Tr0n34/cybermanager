# Feature ID: F-010

## Nom

Historique journalier des clients

## Contexte

Le cybercafé doit pouvoir revoir les clients d’une journée donnée avec leurs usages, achats et abonnements.

## Acteurs

- Administrateur
- Employé du cybercafé

## Objectif

Permettre la consultation de l’historique d’une journée avec le détail des consommations, abonnements et durées d’utilisation.

## Bounded context

- reporting
- session
- sales
- customer
- subscription

## Règles métier

- l’historique est consultable par date
- pour chaque client, il faut retrouver les consommations du jour
- pour chaque client, il faut retrouver les abonnements achetés
- pour chaque client, il faut retrouver les durées d’usage d’un poste
- les données d’une journée clôturée ne doivent pas être altérées dans leur restitution métier

## Backend

### Cas d’usage
- GetDayHistory
- GetCustomerDayHistory
- SearchDayCustomersHistory

### Domain
- DayHistory
- CustomerDayHistory
- ConnectionUsageHistory
- ProductConsumptionHistory
- SubscriptionPurchaseHistory

### Application
- GetDayHistoryUseCase
- GetCustomerDayHistoryUseCase
- DayHistoryDto
- CustomerDayHistoryDto

### Infrastructure
- DayHistoryQueryAdapter

### API
- GET /api/history/days/{date}
- GET /api/history/days/{date}/customers
- GET /api/history/days/{date}/customers/{id}

## Frontend

### Écrans
- historique par journée
- détail d’un client pour une journée donnée

### Composants
- day-history-table
- day-picker
- customer-history-detail
- day-summary-panel

## Critères d’acceptation

- il est possible de consulter une journée passée
- chaque client de la journée est retrouvé
- les achats produits, abonnements et durées d’usage sont visibles
- le détail d’un client sur la journée est consultable