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
- ajout recent d'un panneau lateral d'ajout de plage, ouvert via un bouton sous le titre et referme par defaut avec animation legere
- ajout recent d'une modale de calcul de tarif de connexion a partir du temps saisi et de la grille courante

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
- l'ecran de configuration permet de saisir une plage avec un champ Heures, un champ Minutes et un champ Prix dans un panneau d'ajout distinct a droite
- chaque champ de saisie possede un label explicite
- apres ajout d'une plage, les champs de saisie sont remis a vide ou a leur valeur par defaut
- la grille active s'affiche immediatement apres chargement
- l'utilisateur peut retirer une plage avant enregistrement
- l'utilisateur peut enregistrer l'ensemble de la grille en une action
- la page affiche aussi le total en minutes de chaque palier pour faciliter le controle
- les messages de succes et d'erreur sont affiches dans l'ecran
- le panneau `Ajouter une plage` est ferme par defaut et son ouverture / fermeture reste fluide
- un bouton `Calculer un tarif de connexion` ouvre une modale de simulation sans modifier la grille
- la simulation frontend doit reprendre la meme logique de couverture minimale que le backend pour une duree non exactement couverte par un palier
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
- un administrateur peut simuler un cout de connexion pour une duree arbitraire sans enregistrer de changement
- l'ecran sessions affiche le total `temps + achats produits` apres arret d'une session
*** Add File: D:\DATA\cybermanager\documentations\features\F-019-frontend-crud-filter-and-panel-standardization.md
# Feature ID: F-019

## Nom

Standardisation frontend des filtres CRUD et panneaux lateraux

## Contexte

Plusieurs ecrans CRUD Angular utilisaient des variations locales pour les filtres, les boutons de creation et les panneaux lateraux. Cela rendait l'ergonomie inegale et compliquait la maintenance CSS.

## Acteurs

- Administrateur
- Employe du cybercafe
- Developpeur frontend

## Objectif

Uniformiser les ecrans CRUD frontend autour d'un meme schema d'interaction : bouton `Filtres`, panneau `filters-grid collapsible` anime, action principale `Creer un nouvel ...` visible en haut de page, et panneau lateral reserve a l'edition de l'element courant.

## Bounded context

- users
- product
- subscription
- sales

## Regles metier

- un ecran CRUD de liste doit exposer un bouton `Filtres`
- les filtres doivent s'ouvrir et se refermer avec une animation legere et rapide
- l'action principale de creation doit etre visible en haut de page a cote du bouton `Filtres`
- un panneau de droite ne doit pas dupliquer un bouton `Nouveau` si l'action de creation existe deja dans l'entete
- les champs de filtre doivent etre compacts, lisibles et alignes avec le contenu attendu
- les ecrans doivent rester coherents entre desktop et mobile

## Backend

### Cas d'usage
- sans impact backend direct

### Domain
- sans impact

### Application
- sans impact

### Infrastructure
- sans impact

### API
- reutilisation des endpoints CRUD existants

## Frontend

### Ecrans
- gestion des utilisateurs
- catalogue produits
- offres d'abonnement
- historique des ventes du jour

### Composants
- pages de liste CRUD avec panneau lateral
- bloc partage de filtres `filters-grid collapsible`
- barre d'actions haute avec bouton `Filtres` et creation principale

### Navigation et comportements UI
- le bouton `Filtres` ouvre un bloc anime, compact et refermable
- le bouton `Creer un nouvel ...` ou `Creer une nouvelle ...` ouvre le panneau lateral en mode creation
- le panneau de droite est reserve a la creation / edition / actions sur l'element courant
- les filtres ne doivent pas provoquer un changement de page ou de navigation
- l'animation doit rester discrete, plus smooth que brutale, sans ralentir la saisie

## Criteres d'acceptation

- les ecrans CRUD principaux suivent le meme pattern visuel pour les filtres
- la creation d'un nouvel element se fait depuis un bouton haut de page clairement visible
- aucun bouton `Nouveau` redondant n'apparait dans la colonne de droite
- l'ouverture / fermeture des filtres est fluide et rapide
- la maintenance CSS est simplifiee par une structure frontend plus homogene
