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
- les sessions en cours et du jour affichent le nom du client et son type
- la conversion d'un journalier en abonne est declenchable depuis une session en cours
- l'interface privilegie des formulaires courts et des actions immediates pour un usage comptoir

## Criteres d'acceptation

- un client journalier peut etre cree et demarre depuis l'ecran sessions
- un client abonne peut etre cree et demarre depuis l'ecran sessions
- un abonne existant peut etre retrouve par autocompletion et demarrer une session
- les sessions du jour affichent les noms des clients
- un client journalier actif peut etre converti en abonne depuis l'ecran sessions
