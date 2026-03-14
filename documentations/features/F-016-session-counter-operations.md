# Feature ID: F-016

## Nom

Operations comptoir avancees sur les sessions

## Contexte

L'ecran `Sessions` est devenu le coeur operationnel du comptoir. Il doit gerer la creation de session, la pause, les ventes additionnelles, les dettes, l'encaissement a l'arret et les preferences d'affichage sans perdre en lisibilite.

## Acteurs

- Employe du cybercafe

## Objectif

Fournir un ecran `Sessions` compacte, orientee comptoir, qui centralise les actions rapides et le detail financier de chaque client.

## Bounded context

- session
- customer
- sales
- subscription

## Regles metier

- les entrees `Client`, `Abonne existant` et `Nouvel abonne` sont presentees sous forme d'accordeon
- une pause est une vraie pause : aucun decompte de credit ni de temps pendant la pause
- apres reprise, le temps de pause n'est pas ajoute au temps consomme
- pour un client standard, le decompte visuel se fait a la seconde et l'arrondi a la minute superieure n'intervient qu'a l'arret
- pour un abonne, le credit temps se decompte a la seconde
- toute minute entamee est consommee au moment du calcul final de connexion
- la modale d'arret distingue ce que le client paie maintenant de ses dettes deja ouvertes
- une session ne peut pas etre dans l'etat `Paye` tant qu'il reste des dettes ouvertes
- les dettes ouvertes d'un client doivent reapparaitre meme lors d'une nouvelle session plus tard
- une vente de produit ne doit jamais effacer les dettes deja existantes
- les dettes issues d'achats similaires peuvent etre regroupees sur une ligne compacte dans le detail des achats
- les abonnements d'un meme client sont cumulables et augmentent le credit disponible
- dans la modale de paiement, un abonnement ajoute pendant le reglement augmente d'abord le credit, reduit le cout du depassement eventuel, puis ajoute son propre prix au montant final a regler
- dans la modale de paiement, le temps de depassement couvert par un abonnement ajoute n'est plus compte une seconde fois dans le montant connexion
- si le depassement est entierement absorbe par les abonnements ajoutes, la connexion restante a payer tombe a zero
- la colonne `A payer` d'une session doit suivre la meme regle que la modale de paiement pour les abonnements deja rattaches a la session
- une session arretee mais non payee peut encore recevoir une vente produit ou une vente d'abonnement rattachee a la session
- les catalogues de reference critiques utilises par les modales de session doivent etre relus a l'ouverture si le personnel peut les avoir modifies dans un autre ecran

## Backend

### Cas d'usage

- StartSession
- PauseSession
- ResumeSession
- StopSession
- CreateProductSale
- CreateSubscriptionSale
- SettleDebt
- GetCustomerDetails

### Application

- SessionApplicationService
- SalesApplicationService
- CustomerApplicationService
- SessionView

### Infrastructure

- CafeSessionRepositoryAdapter
- SessionSchemaBootstrap

### API

- GET /api/sessions/current
- GET /api/sessions/day
- POST /api/sessions/start
- POST /api/sessions/{id}/pause
- POST /api/sessions/{id}/resume
- POST /api/sessions/{id}/stop
- POST /api/sales/products
- POST /api/sales/subscriptions
- POST /api/customers/debts/{debtId}/settle

## Frontend

### Ecrans

- ecran `Sessions`
- ecran `Reglages sessions`

### Navigation et comportements UI

- la colonne de gauche expose un accordeon compact pour `Client`, `Abonne existant`, `Nouvel abonne`
- la colonne de droite separe `Sessions en cours` et `Sessions du jour`
- chaque bloc de sessions possede ses filtres masques derriere une icone `Filtres`
- la configuration d'affichage permet de regler le nombre de lignes par page pour `Sessions en cours` et `Sessions du jour`
- l'enregistrement des reglages doit persister localement et etre relu sans redemarrage
- chaque ligne de session en cours affiche `Client`, `Type`, `Etat`, `Credit`, `A payer`, `Consomme`, `Debut`
- chaque ligne de session du jour affiche `Client`, `Type`, `Duree`, `Credit`, `A payer`, `Debut`, `Fin`
- `En cours`, `Pause` et `Paye` sont distingues visuellement
- `Detail` ouvre un panneau sous la ligne avec achats du jour, dettes, resume abonnement et actions de vente
- les lignes d'achats du jour sont compactes, alignees verticalement sur la date et le prix
- la vente d'un produit depuis une session se fait dans une modale
- la vente d'un abonnement depuis une session se fait dans une modale
- l'arret d'une session ouvre une modale de validation du paiement avec detail `Connexion`, `Achats`, `Total du jour`, `Dettes ouvertes`
- la modale de paiement peut ajouter un ou plusieurs abonnements et recalculer immediatement le montant `A payer`
- la modale de paiement affiche explicitement le detail du calcul final et le montant conseille
- la modale de paiement recharge les offres actives a son ouverture
- la modale de vente produit recharge le catalogue produit actif a son ouverture
- la modale de vente d'abonnement recharge les offres actives a son ouverture
- apres une vente rattachee a la session, le detail du client et le montant visible de la session doivent etre relus

## Criteres d'acceptation

- l'utilisateur peut piloter une session complete sans quitter l'ecran
- les reglages de pagination de session sont conserves apres sauvegarde
- l'etat d'une session passe visuellement de `En cours` a `Pause` puis `Paye` ou `Terminee` selon le cas
- la modale d'arret affiche distinctement le paiement du jour et les dettes deja ouvertes
- les dettes et achats du jour restent lisibles, compacts et coherents apres ventes, pauses et arrets
- une vente produit ou abonnement sur session arretee peut etre enregistree sans quitter l'ecran
- une vente enregistree sur session arretee apparait ensuite dans le detail de la session
