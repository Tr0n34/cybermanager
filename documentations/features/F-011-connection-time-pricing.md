# Feature ID: F-011

## Nom

Configuration des tarifs d'usage dans le temps

## Contexte

Le cybercafe doit pouvoir faire evoluer le prix du temps de connexion sans modifier le code. Le tarif unique actuel n'est pas suffisant car l'exploitant veut definir plusieurs plages de duree avec un prix associe.

## Acteurs

- Administrateur

## Objectif

Permettre la gestion d'une grille de tarifs de connexion basee sur des plages de duree exprimees en heures, minutes et prix.

## Bounded context

- sales
- pricing
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

## Backend

### Cas d'usage

- ConfigureConnectionPricing
- GetCurrentConnectionPricing
- CreateConnectionTimeSale
- StopSession

### Domain

- ConnectionPricingRule
- ConnectionPricingTier
- Money

### Application

- ConfigureConnectionPricingCommand
- ConnectionPricingView
- SalesApplicationService
- SessionApplicationService

### Infrastructure

- ConnectionPricingTierJpaEntity
- ConnectionPricingJpaRepository
- ConnectionPricingRepositoryAdapter

### API

- GET /api/pricing/connection-time
- PUT /api/pricing/connection-time

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
- les messages de succes et d'erreur sont affiches dans l'ecran

## Criteres d'acceptation

- un administrateur peut ajouter une plage de 30 minutes avec un prix
- un administrateur peut ajouter une plage de 1 heure avec un prix
- un administrateur peut enregistrer plusieurs plages dans la meme grille
- la vente de temps utilise la grille configuree
- l'arret d'une session journaliere utilise la meme grille configuree
- l'ecran ventes affiche la grille active et renvoie vers l'ecran dedie de configuration
