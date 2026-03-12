# Feature ID: F-010

## Nom

Historique journalier des clients

## Contexte

Le cybercafe doit pouvoir revoir les clients d'une journee donnee avec leurs usages, achats et abonnements.

## Acteurs

- Administrateur
- Employe du cybercafe

## Objectif

Permettre la consultation de l'historique d'une journee avec le detail des consommations, abonnements et durees d'utilisation.

## Ameliorations integrees

- ajout d'un service applicatif de reporting pour la consultation d'une journee passee
- exposition d'un endpoint de synthese par jour et d'un endpoint de detail client
- reutilisation d'un modele de restitution centre sur les ventes et sessions historisees
- ajout d'une page Angular de reporting avec selecteur de date, liste et detail

## Bounded context

- reporting
- session
- sales
- customer
- subscription

## Regles metier

- l'historique est consultable par date
- pour chaque client, il faut retrouver les consommations du jour
- pour chaque client, il faut retrouver les abonnements achetes
- pour chaque client, il faut retrouver les durees d'usage d'un poste
- les donnees d'une journee cloturee ne doivent pas etre alterees dans leur restitution metier

## Backend

### Cas d'usage
- GetDayHistory
- GetCustomerDayHistory

### Domain
- DayHistory
- CustomerDayHistory
- ConnectionUsageHistory
- ProductConsumptionHistory
- SubscriptionPurchaseHistory

### Application
- GetDayHistoryUseCase
- GetCustomerDayHistoryUseCase
- DayHistoryView
- CustomerDayHistoryView

### Infrastructure
- DayHistoryQueryAdapter

### API
- GET /api/history/days/{date}
- GET /api/history/days/{date}/customers
- GET /api/history/days/{date}/customers/{id}

### Donnees restituees
- `GET /api/history/days/{date}` et `GET /api/history/days/{date}/customers` retournent la date et la liste des clients avec `totalMinutes` et `salesTotal`
- `GET /api/history/days/{date}/customers/{id}` retourne le detail de ventes et de sessions pour le client demande

## Frontend

### Ecrans
- historique par journee
- detail d'un client pour une journee donnee
- navigation depuis le shell principal vers l'ecran reporting

### Composants
- day-history-table
- day-picker
- customer-history-detail
- day-summary-panel

### Navigation et comportements UI
- l'utilisateur choisit une date puis recharge la liste historique correspondante
- la liste des clients de la journee est affichee dans le meme ecran
- la selection d'un client ouvre son detail de journee sans perdre le contexte de date
- le changement de date recharge les indicateurs et les listes associees
- l'ecran doit permettre de consulter rapidement la synthese d'une journee avant d'ouvrir un detail client
- les erreurs de chargement sont visibles dans la page

## Criteres d'acceptation

- il est possible de consulter une journee passee
- chaque client de la journee est retrouve
- les achats produits, abonnements et durees d'usage sont visibles
- le detail d'un client sur la journee est consultable
- l'ecran garde une navigation coherente entre filtre par date, liste et detail
