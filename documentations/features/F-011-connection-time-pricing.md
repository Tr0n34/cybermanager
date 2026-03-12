# Feature ID: F-011

## Nom

Configuration des tarifs d'usage dans le temps

## Contexte

Le cybercafe doit pouvoir faire evoluer le prix du temps de connexion sans modifier le code. Le tarif unique actuel n'est pas suffisant car l'exploitant veut definir plusieurs plages de duree avec un prix associe.

## Acteurs

- Administrateur

## Objectif

Permettre la gestion d'une grille de tarifs de connexion basee sur des plages de duree exprimees en heures, minutes et prix.

## Ameliorations integrees

- ajout d'une API complete de lecture et de mise a jour de la grille active
- factorisation de la grille dans le domaine `sales` via `ConnectionPricingRule`
- reutilisation de la meme grille par la vente de temps et les vues de suivi et d'historique
- ajout d'une page Angular dediee a la saisie et a l'edition des paliers

## Bounded context

- sales
- session

## Regles metier

- une plage de tarif contient une duree exprimee en heures et minutes
- une plage de tarif contient un prix
- une duree doit etre strictement positive
- un prix doit etre strictement positif
- une duree ne peut etre definie qu'une seule fois dans la grille
- la grille tarifaire est ordonnee par duree croissante
- la grille tarifaire est la source de verite pour la vente de temps
- la meme grille tarifaire est la source de verite pour la facturation d'une session journaliere
- la facturation doit reutiliser les plages configurees au moment du calcul
- lorsqu'une session est arretee, le montant affiche dans `Sessions du jour` doit additionner le cout du temps de connexion et les achats produits du jour du client

## Backend

### Cas d'usage

- ConfigureConnectionPricing
- GetCurrentConnectionPricing
- CreateConnectionTimeSale
- StopSession

### Domain

- ConnectionPricingRule
- Money

### Application

- ConfigureConnectionPricingCommand
- GetCurrentConnectionPricingQuery
- ConnectionPricingView
- SalesApplicationService
- SessionApplicationService
- SessionView

### Infrastructure

- ConnectionPricingJpaRepository
- ConnectionPricingRepositoryAdapter

### API

- GET /api/pricing/connection-time
- PUT /api/pricing/connection-time

### Contrats API attendus
- `GET /api/pricing/connection-time` retourne une collection de paliers ordonnee avec `hours`, `minutes`, `durationMinutes` et `price`
- `PUT /api/pricing/connection-time` remplace la grille active complete a partir d'une collection de paliers
- les vues session exposees par `/api/sessions/current` et `/api/sessions/day` retournent aussi `calculatedPrice` pour le temps et `totalAmountDue` pour la somme `temps + achats produits du jour`

## Frontend

### Ecrans

- ecran ventes avec resume de la grille active
- ecran dedie de configuration des tarifs temps

### Composants

- connection-pricing-page
- pricing-tier-form
- pricing-tier-table

### Navigation et comportements UI

- l'utilisateur accede a l'ecran dedie depuis le menu principal ou depuis l'ecran ventes
- l'ecran de configuration permet de saisir une plage avec un champ Heures, un champ Minutes et un champ Prix
- chaque champ de saisie possede un label explicite
- apres ajout d'une plage, les champs de saisie sont remis a vide ou a leur valeur par defaut
- la grille active s'affiche immediatement apres chargement
- l'utilisateur peut retirer une plage avant enregistrement
- l'utilisateur peut enregistrer l'ensemble de la grille en une action
- la page affiche aussi le total en minutes de chaque palier pour faciliter le controle
- les messages de succes et d'erreur sont affiches dans l'ecran
- dans l'ecran `Sessions`, la colonne `A payer` apparait entre `Credit` et `Debut`
- lorsqu'une session en cours est arretee, elle passe dans `Sessions du jour` avec le total calcule a partir du temps de connexion et des achats produits

## Criteres d'acceptation

- un administrateur peut ajouter une plage de 30 minutes avec un prix
- un administrateur peut ajouter une plage de 1 heure avec un prix
- un administrateur peut enregistrer plusieurs plages dans la meme grille
- la vente de temps utilise la grille configuree
- l'arret d'une session journaliere utilise la meme grille configuree
- l'ecran ventes affiche la grille active et renvoie vers l'ecran dedie de configuration
- la lecture de la grille active est possible sans modification
- l'ecran sessions affiche le total `temps + achats produits` apres arret d'une session
