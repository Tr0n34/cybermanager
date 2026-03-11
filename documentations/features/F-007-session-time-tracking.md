# Feature ID: F-007

## Nom

Gestion du temps de connexion

## Contexte

Le cybercafé doit suivre le temps de connexion utilisé par les clients occasionnels et par les abonnés.

## Acteurs

- Employé du cybercafé

## Objectif

Permettre de démarrer, suivre et terminer une session de connexion sur un poste, en déduisant le temps si le client est abonné ou en calculant le coût si le client est occasionnel.

## Bounded context

- session
- customer
- subscription

## Règles métier

- une session possède une heure de début
- une session peut posséder une heure de fin
- une session est liée à un client
- une session peut être liée à un poste informatique
- pour un abonné, le temps consommé doit être déduit de son crédit disponible
- pour un client occasionnel, la durée consommée doit permettre de calculer le prix dû
- une session en cours doit être visible en temps réel
- une session terminée reste visible jusqu’à la fin de la journée

## Backend

### Cas d’usage
- StartSession
- StopSession
- GetCurrentSession
- SearchSessionsOfDay
- CalculateWalkInSessionPrice
- DeductSubscriberTime

### Domain
- Session
- SessionId
- SessionStartTime
- SessionEndTime
- ConsumedDuration
- WorkstationId
- RemainingSubscriptionTime

### Application
- StartSessionUseCase
- StopSessionUseCase
- SearchSessionsOfDayUseCase
- SessionDetailsDto
- ActiveSessionDto
- DaySessionSummaryDto

### Infrastructure
- SessionJpaEntity
- SessionJpaRepository

### API
- GET /api/sessions/day
- GET /api/sessions/current
- POST /api/sessions/start
- POST /api/sessions/{id}/stop

## Frontend

### Écrans
- vue des sessions en cours
- vue des sessions du jour

### Composants
- active-sessions-table
- session-timer
- session-stop-action
- workstation-badge

## Critères d’acceptation

- une session peut être démarrée pour un client ou un abonné
- une session en cours affiche sa durée en temps réel
- un arrêt de session calcule correctement le temps consommé
- le temps d’un abonné est déduit correctement
- les sessions du jour restent visibles jusqu’à la fin de journée