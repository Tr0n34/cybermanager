# Feature ID: F-019

## Nom

Standardisation frontend des filtres CRUD, paginations et panneaux lateraux

## Contexte

Plusieurs ecrans CRUD Angular utilisaient des variations locales pour les filtres, les boutons de creation et les panneaux lateraux. Cela rendait l'ergonomie inegale et compliquait la maintenance CSS.

## Acteurs

- Administrateur
- Employe du cybercafe
- Developpeur frontend

## Objectif

Uniformiser les ecrans CRUD frontend autour d'un meme schema d'interaction : bouton `Filtres`, panneau `filters-grid collapsible` anime, action principale `Creer un nouvel ...` visible en haut de page, pagination compacte avec `Precedent / Suivant`, et panneau lateral reserve a l'edition de l'element courant.

## Bounded context

- users
- product
- subscription
- sales
- reporting
- customer

## Regles metier

- un ecran CRUD de liste doit exposer un bouton `Filtres`
- les filtres doivent s'ouvrir et se refermer avec une animation legere et rapide
- l'action principale de creation doit etre visible en haut de page a cote du bouton `Filtres`
- un panneau de droite ne doit pas dupliquer un bouton `Nouveau` si l'action de creation existe deja dans l'entete
- les champs de filtre doivent etre compacts, lisibles et alignes avec le contenu attendu
- quand le volume peut croitre, la pagination doit rester visible pres du bouton `Filtres`
- la navigation de pagination doit utiliser le couple `Precedent` / `Suivant` avec indication de page courante
- le choix de taille de page doit apparaitre dans le bloc de filtres plutot qu'en dehors
- les ecrans doivent rester coherents entre desktop et mobile
- les ecrans operationnels a forte densite doivent faire dependre leurs hauteurs utiles de la hauteur disponible du conteneur et non de valeurs fixes en `vh`
- lorsqu'un ecran est deja rendu dans un shell borne a la hauteur visible, eviter les sous-conteneurs en `100vh` qui recreent un debordement vertical global

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
- dettes clients
- historique journalier

### Composants
- pages de liste CRUD avec panneau lateral
- bloc partage de filtres `filters-grid collapsible`
- barre d'actions haute avec bouton `Filtres` et creation principale
- pager compact `Precedent / Suivant`

### Navigation et comportements UI
- le bouton `Filtres` ouvre un bloc anime, compact et refermable
- le bouton `Creer un nouvel ...` ou `Creer une nouvelle ...` ouvre le panneau lateral en mode creation
- le panneau de droite est reserve a la creation / edition / actions sur l'element courant
- les filtres ne doivent pas provoquer un changement de page ou de navigation
- l'animation doit rester discrete, plus smooth que brutale, sans ralentir la saisie
- lorsque la pagination existe, `Precedent / Suivant` reste sur la meme ligne d'action que `Filtres`
- les filtres de taille de page doivent utiliser des champs courts et des libelles explicites (`Clients par page`, `Dettes par page`)
- les zones scrollables doivent rester confinees a leur panneau pour eviter le debordement vertical du document global

## Criteres d'acceptation

- les ecrans CRUD principaux suivent le meme pattern visuel pour les filtres
- la creation d'un nouvel element se fait depuis un bouton haut de page clairement visible
- aucun bouton `Nouveau` redondant n'apparait dans la colonne de droite
- l'ouverture / fermeture des filtres est fluide et rapide
- les ecrans pagines utilisent `Precedent / Suivant` avec un positionnement coherent et une taille de page configurable
- la maintenance CSS est simplifiee par une structure frontend plus homogene
- les ecrans volumineux ne provoquent pas de scroll vertical parasite du navigateur sur desktop standard
