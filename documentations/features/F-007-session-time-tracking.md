# Feature ID: F-007

## Nom

Gestion du temps de connexion

## Contexte

Le cybercafe doit suivre le temps de connexion utilise par les clients journaliers et par les abonnes avec une interface fluide, orientee comptoir, sans liste exhaustive inefficace.

## Acteurs

- Employe du cybercafe

## Objectif

Permettre de demarrer, suivre, convertir et terminer une session de connexion, avec creation d'un client journalier, creation d'un client abonne et recherche par autocompletion.

## Bounded context

- session
- customer
- subscription
- sales
- debt

## Regles metier

- une session possede une heure de debut
- une session peut posseder une heure de fin
- une session est liee a un client
- un client journalier peut etre cree directement dans l'ecran sessions
- un client abonne peut etre cree directement dans l'ecran sessions
- un client abonne existant doit etre retrouvable par autocompletion
- un abonne actif avec du credit doit pouvoir demarrer et terminer une session
- un abonne sans credit disponible ne peut pas demarrer une nouvelle session
- un client journalier en session peut etre converti en abonne sans quitter l'ecran sessions
- l'affichage des sessions du jour doit etre centre sur le nom du client
- la gestion des postes n'est pas exposee dans l'interface utilisateur
- la vue de detail d'une session doit afficher les achats du jour, le resume abonnement et les dettes ouvertes du client
- l'arret d'une session client journalier facturee et non reglee cree une dette ouverte
- les abonnements d'un client abonne sont cumulables
- l'arret d'une session doit restituer immediatement le total a payer pour cette session
- une session en cours peut etre mise en pause puis reprise sans quitter la liste des sessions en cours
- le detail d'une session peut declencher une vente produit ou une vente d'abonnement en modale
- le detail `A payer` inclut la connexion et les dettes ouvertes du client
- la modale d'arret distingue ce qui est encaisse maintenant des dettes deja ouvertes
- le parametre d'affichage des listes de session est configurable et persiste localement

## Backend

### Cas d'usage
- StartSession
- StopSession
- GetCurrentSession
- SearchSessionsOfDay
- DeductSubscriberTime
- CreateCustomer
- ConvertCustomerToSubscriber

### Domain
- Session
- SessionId
- SessionStartTime
- SessionEndTime
- ConsumedDuration
- RemainingSubscriptionTime

### Application
- StartSessionUseCase
- StopSessionUseCase
- SearchSessionsOfDayUseCase
- SessionView
- CurrentSessionsView

### Infrastructure
- SessionJpaEntity
- SessionJpaRepository
- SessionRepositoryAdapter

### API
- GET /api/sessions/day
- GET /api/sessions/current
- POST /api/sessions/start
- POST /api/sessions/{id}/pause
- POST /api/sessions/{id}/resume
- POST /api/sessions/{id}/stop
- POST /api/customers
- POST /api/customers/{id}/convert-to-subscriber

## Frontend

### Ecrans
- creation d'un client journalier et demarrage de session
- creation d'un client abonne et demarrage de session
- recherche autocompletion d'un abonne existant
- vue des sessions en cours
- vue des sessions du jour
- conversion d'un client journalier actif en abonne
- detail developpable par session en cours
- detail developpable par session du jour

### Composants
- walk-in-session-form
- subscriber-autocomplete
- new-subscriber-form
- active-sessions-table
- daily-sessions-table
- convert-to-subscriber-form

### Navigation et comportements UI
- l'ecran permet de saisir directement le nom d'un client journalier
- l'ecran permet de creer un client abonne avec son abonnement initial
- la recherche d'abonne se fait par autocompletion et non par liste exhaustive
- toutes les zones de recherche possedent un label explicite
- les sessions en cours et du jour sont separees visuellement, les sessions du jour etant affichees dans un encart dedie en bas
- les sessions en cours et du jour affichent le nom du client et son type
- la zone `Sessions en cours` garde une hauteur stable, meme quand peu de lignes sont affichees
- la conversion d'un journalier en abonne est declenchable depuis une session en cours
- un bouton `Detail` ouvre sous la session le detail des achats du jour, l'abonnement et les dettes
- la vente produit pour une session en cours se declenche depuis la colonne de gauche sans quitter l'ecran
- la vente produit depuis session permet aussi de cocher la creation d'une dette
- le tableau des achats du detail session doit rester tres compact pour supporter de nombreuses lignes
- les achats du jour et les dettes creees le jour meme sont affiches dans une chronologie unique
- une dette issue d'une vente apparait sur la meme ligne que la vente correspondante, avec une icone rouge de non-paiement
- le resume du detail signale seulement la presence de dettes par une icone a droite
- les dates et heures visibles dans la session suivent le format `JJ/MM/AAAA HH:mm`
- l'arret d'une session affiche immediatement le total a payer calcule pour cette session
- chaque ligne de session en cours propose une action `Pause` ou `Reprendre` sans sortir la session de la liste
- le resume abonnement rappelle que les abonnements se cumulent
- l'interface privilegie des formulaires courts, compacts et des actions immediates pour un usage comptoir
- les colonnes et tableaux sont compactes et scrollables pour supporter un grand nombre de sessions

## Criteres d'acceptation

- un client journalier peut etre cree et demarre depuis l'ecran sessions
- un client abonne peut etre cree et demarre depuis l'ecran sessions
- un abonne existant peut etre retrouve par autocompletion et demarrer une session
- les sessions du jour affichent les noms des clients
- un client journalier actif peut etre converti en abonne depuis l'ecran sessions
- le detail d'une session affiche les dettes ouvertes du client en rouge
- le detail des achats reste lisible sans occuper excessivement de hauteur
- l'utilisateur voit le montant total a payer apres l'arret d'une session
- le detail est disponible aussi pour les sessions du jour
- les operations comptoir avancees sont detaillees dans `F-016-session-counter-operations.md`
