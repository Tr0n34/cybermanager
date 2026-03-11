# Feature ID: F-009

## Nom

Suivi en temps réel des clients du cybercafé sur la journée

## Contexte

Le personnel doit voir à tout moment quels clients sont présents ou passés dans le cybercafé sur la journée, leur temps de connexion, leurs achats et leur statut.

## Acteurs

- Employé du cybercafé
- Administrateur

## Objectif

Afficher une vue opérationnelle de la journée avec les clients en cours et les clients déjà passés, tout en les gardant visibles jusqu’à la fin de journée.

## Bounded context

- monitoring
- session
- sales
- customer
- subscription

## Règles métier

- tout client ayant une activité sur la journée doit être visible jusqu’à la fin de journée
- un client visible peut être en cours, terminé ou parti
- la vue doit afficher le temps de connexion du jour
- la vue doit afficher les consommations produits du jour
- la vue doit afficher les abonnements achetés du jour
- la vue doit distinguer client occasionnel et abonné

## Backend

### Cas d’usage
- GetDailyLiveCafeView
- SearchTodayCustomers
- GetTodayCustomerActivity

### Domain
- DailyCustomerPresence
- DailyCustomerActivity
- DailyConsumptionSummary
- DailyConnectionSummary

### Application
- GetDailyLiveCafeViewUseCase
- DailyCustomerViewDto
- DailyCustomerActivityDto

### Infrastructure
- DailyMonitoringQueryAdapter

### API
- GET /api/day-monitoring/customers
- GET /api/day-monitoring/customers/{id}

## Frontend

### Écrans
- tableau de bord opérationnel du jour
- détail de l’activité du client sur la journée

### Composants
- day-customers-table
- active-status-badge
- connection-time-column
- day-consumption-panel
- customer-day-detail-drawer

## Critères d’acceptation

- les clients du jour restent visibles jusqu’à la clôture de journée
- la vue montre le temps de connexion et les consommations
- le personnel peut distinguer rapidement les abonnés et non abonnés
- l’accès au détail du jour d’un client est possible