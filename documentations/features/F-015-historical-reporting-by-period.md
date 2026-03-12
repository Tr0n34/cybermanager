# Feature ID: F-015

## Nom

Historique par jour et par periode

## Contexte

Le cybercafe doit pouvoir relire une journee ou une plage de jours avec la meme lisibilite que le monitoring temps reel, tout en filtrant rapidement un client par son nom.

## Acteurs

- Employe du cybercafe
- Administrateur

## Objectif

Permettre la consultation d'un historique structure par jour ou plage de dates, avec detail client, recherche immediate par nom et synthese financiere.

## Bounded context

- reporting
- session
- sales
- customer

## Regles metier

- l'historique peut etre demande pour un jour unique ou une plage de dates
- le filtre par nom doit reagir des les premieres lettres
- la synthese affiche l'argent encaisse et la dette creee sur la periode selectionnee
- le detail client reprend la meme logique que le monitoring : ventes structurees et evenements de session lisibles
- les evenements de session et les dates visibles respectent le format `JJ/MM/AAAA HH:mm`
- les listes historisees doivent rester compactes et lisibles

## Backend

### Cas d'usage

- GetDayHistory
- GetCustomerDayHistory

### Application

- DayHistoryView
- DayCustomerHistoryView
- CustomerDayHistoryView
- ReportingApplicationService

### Infrastructure

- ReportingQueryAdapter

### API

- GET /api/history/days?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD
- GET /api/history/days/customers/{id}?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD

### Contrats API attendus

- la liste retourne `customers`, `totalCollected`, `totalDebtCreated`, `startDate`, `endDate`
- chaque client retourne `customerId`, `name`, `type`, `totalMinutes`, `salesTotal`, `debtTotal`, `collectedTotal`, `state`
- le detail retourne `sales`, `sessions`, `totalCollected`, `totalDebtCreated`

## Frontend

### Ecrans

- ecran `Historique`

### Navigation et comportements UI

- l'ecran affiche des champs `date de debut`, `date de fin` et `recherche par nom`
- la modification d'une date recharge la liste
- la recherche par nom filtre la liste sans clic supplementaire
- le tableau affiche au minimum `Client`, `Type`, `Temps`, `Montant`, `Dette`, `Etat`
- un bouton `Detail` ouvre le detail d'un client dans la colonne de droite
- la colonne de detail a droite reste compacte et n'est pas etiree par la liste
- les erreurs et etats vides sont rendus visiblement

## Criteres d'acceptation

- l'utilisateur peut charger une journee ou une plage de jours
- la recherche par nom reagit des les premieres lettres
- la synthese du haut affiche les montants consolides de la periode
- le detail d'un client reprend les ventes et evenements de session de la periode
