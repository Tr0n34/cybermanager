# Feature ID: F-014

## Nom

Monitoring operationnel avec synthese financiere

## Contexte

Le personnel du cybercafe a besoin d'un ecran de pilotage de la journee qui montre a la fois les clients actifs ou passes, l'etat courant de leur session et une synthese financiere exploitable immediatement.

## Acteurs

- Employe du cybercafe
- Administrateur

## Objectif

Afficher une vue operationnelle compacte de la journee avec detail client, etats de session, ventes structurees et agregats financiers consolides.

## Bounded context

- monitoring
- session
- sales
- customer

## Regles metier

- le monitoring consolide la journee courante uniquement
- le type visible d'un client est `Client` ou `Abonne`
- l'etat visible d'une ligne correspond a l'etat courant de la derniere session connue du client
- l'argent encaisse correspond a la somme des achats du client moins ses dettes ouvertes
- la somme des dettes correspond a la somme des dettes ouvertes des clients visibles
- le detail affiche les ventes sous forme structuree : nom, quantite, prix
- le detail affiche les manipulations de session en francais avec le format `JJ/MM/AAAA HH:mm`
- le monitoring ne doit pas obliger l'utilisateur a changer de page pour consulter le detail d'un client

## Backend

### Cas d'usage

- GetDailyLiveCafeView
- GetTodayCustomerActivity

### Application

- DailyCustomerView
- DailyCustomerActivityView
- MonitoringApplicationService

### Infrastructure

- MonitoringQueryAdapter

### API

- GET /api/day-monitoring/customers
- GET /api/day-monitoring/customers/{id}

### Contrats API attendus

- la liste retourne `customerId`, `name`, `type`, `remainingMinutes`, `consumedMinutes`, `purchasesTotal`, `debtTotal`, `collectedTotal`, `state`
- le detail retourne `sales`, `sessions`, `totalCollected`, `totalDebtCreated`
- chaque ligne de vente du detail retourne aussi si elle est associee a une dette ouverte
- chaque session du detail retourne un libelle, une date de demarrage et une date d'arret
- les montants sont deja calcules cote backend avec des regles metier coherentes pour les dettes ouvertes

## Frontend

### Ecrans

- ecran `Monitoring`

### Navigation et comportements UI

- l'ecran affiche en tete un encart `Argent encaisse` et un encart `Dette creee`
- les encarts du haut sont calcules sur la liste globale affichee, pas sur le detail selectionne
- la liste affiche au minimum `Client`, `Type`, `Temps`, `Achats`, `Dette`, `Etat`
- la liste propose des filtres `client`, `etat`, `dette oui/non` et `clients par page`
- la liste utilise une pagination classique `Precedent` / `Suivant`
- la colonne de detail a droite reste compacte et ne s'etire pas a la hauteur de la liste
- un bouton `Detail` charge le detail dans la colonne de droite
- le detail affiche au maximum les `10 derniers produits` et les `10 dernieres sessions` par defaut
- un bouton `Historique complet` permet d'afficher tout l'historique du jour avec pagination independante pour les ventes et les sessions
- les etats vides et les erreurs de chargement sont visibles dans la page

## Criteres d'acceptation

- l'utilisateur voit le total encaisse du jour sur la liste globale
- l'utilisateur voit le total des dettes ouvertes du jour sur la liste globale
- il peut ouvrir le detail d'un client sans quitter l'ecran
- le detail liste les ventes avec quantite et prix
- le detail liste les evenements de session dans un francais lisible et au bon format de date
