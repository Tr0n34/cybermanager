# Feature ID: F-XXX

## Nom

Nom court de la fonctionnalite.

## Contexte

Decrire le besoin metier.

## Acteurs

- Administrateur
- Client
- Utilisateur

## Objectif

Decrire le resultat attendu.

## Bounded context

- auth
- users
- customer
- subscription
- product
- monitoring
- reporting
- sales
- session

## Regles metier

- regle 1
- regle 2
- regle 3

## Backend

### Cas d'usage
- UC-1
- UC-2
- UC-3

### Domain
- agregats
- entites
- value objects
- services de domaine

### Application
- use cases
- views
- ports

### Infrastructure
- persistance
- adapters
- clients externes

### API
- GET /...
- POST /...
- PUT /...
- DELETE /...

## Frontend

### Ecrans
- ecran liste
- ecran detail
- ecran creation / edition

### Composants
- tableau
- formulaire
- filtres
- actions

### Appels API
- GET /...
- POST /...

### Navigation et comportements UI
- preciser comment l'ecran est atteint depuis la navigation principale ou un autre ecran
- preciser si une liste doit se charger automatiquement a l'ouverture
- preciser comment la creation ou l'edition est declenchee depuis l'UI
- preciser quel rafraichissement de liste ou de detail est attendu apres action
- preciser la gestion des erreurs visibles cote utilisateur
- preciser si la feature utilise une recherche ciblee plutot qu'une liste exhaustive
- preciser si des donnees sont creees implicitement par une autre feature plutot que manuellement ici
- preciser le libelle associe aux zones de recherche et si une autocompletion est attendue

## Criteres d'acceptation

- critere 1
- critere 2
- critere 3
