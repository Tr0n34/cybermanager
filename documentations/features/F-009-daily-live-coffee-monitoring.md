# Feature ID: F-009

## Nom

Suivi en temps reel des clients du cybercafe sur la journee

## Contexte

Le personnel doit voir a tout moment quels clients sont presents ou passes dans le cybercafe sur la journee, leur temps de connexion, leurs achats et leur statut.

## Acteurs

- Employe du cybercafe
- Administrateur

## Objectif

Afficher une vue operationnelle de la journee avec les clients en cours et les clients deja passes, tout en les gardant visibles jusqu'a la fin de journee.

## Ameliorations integrees

- ajout d'un service applicatif de monitoring dedie a la journee courante
- exposition d'une liste synthese et d'un detail d'activite par client
- agregats de suivi centres sur le temps consomme, le temps restant, le total des achats et l'etat de session
- ajout d'une page Angular de monitoring avec service API et modeles dedies

## Bounded context

- monitoring
- session
- sales
- customer
- subscription

## Regles metier

- tout client ayant une activite sur la journee doit etre visible jusqu'a la fin de journee
- un client visible peut etre en cours, termine ou parti
- la vue doit afficher le temps de connexion du jour
- la vue doit afficher les consommations produits du jour
- la vue doit afficher les abonnements achetes du jour
- la vue doit distinguer client occasionnel et abonne

## Backend

### Cas d'usage
- GetDailyLiveCafeView
- GetTodayCustomerActivity

### Domain
- DailyCustomerPresence
- DailyCustomerActivity
- DailyConsumptionSummary
- DailyConnectionSummary

### Application
- GetDailyLiveCafeViewUseCase
- DailyCustomerView
- DailyCustomerActivityView

### Infrastructure
- DailyMonitoringQueryAdapter

### API
- GET /api/day-monitoring/customers
- GET /api/day-monitoring/customers/{id}

### Donnees restituees
- la liste retourne `customerId`, `name`, `type`, `remainingMinutes`, `consumedMinutes`, `purchasesTotal` et `activeSession`
- le detail retourne l'historique des ventes et des sessions de la journee pour un client

## Frontend

### Ecrans
- tableau de bord operationnel du jour
- detail de l'activite du client sur la journee
- navigation depuis le shell principal vers l'ecran monitoring

### Composants
- day-customers-table
- active-status-badge
- connection-time-column
- day-consumption-panel
- customer-day-detail-drawer

### Navigation et comportements UI
- la liste des clients du jour se charge a l'ouverture de l'ecran
- la selection d'un client ouvre son detail dans la meme navigation
- l'ecran permet un rafraichissement explicite ou automatique des donnees du jour
- les informations de liste et de detail restent coherentes apres mise a jour
- la vue doit mettre en avant les sessions actives et les clients encore credites en temps restant
- les erreurs de consultation sont affichees dans l'ecran

## Criteres d'acceptation

- les clients du jour restent visibles jusqu'a la cloture de journee
- la vue montre le temps de connexion et les consommations
- le personnel peut distinguer rapidement les abonnes et non abonnes
- l'acces au detail du jour d'un client est possible
- la navigation liste vers detail est utilisable sans changer de page applicative
