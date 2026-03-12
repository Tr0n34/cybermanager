# Feature ID: F-013

## Nom

Pause et reprise d'une session en cours

## Contexte

Au comptoir, un client peut interrompre temporairement son usage sans terminer sa session. Le systeme doit permettre de suspendre le decompte du temps tout en gardant la session visible dans les sessions en cours.

## Acteurs

- Employe du cybercafe

## Objectif

Permettre de mettre en pause puis de reprendre une session active, sans la cloturer et sans perdre sa visibilite operationnelle.

## Bounded context

- session

## Regles metier

- une session active peut etre mise en pause
- une session en pause reste visible dans les sessions en cours
- une session en pause peut etre reprise
- le temps de pause ne doit pas etre facture ni deduit du credit d'abonnement
- une session deja arretee ne peut plus etre mise en pause ni reprise
- une session deja en pause ne peut pas etre remise en pause une seconde fois sans reprise intermediaire

## Backend

### Cas d'usage

- PauseSession
- ResumeSession
- StopSession
- GetCurrentSessions

### Domain

- CafeSession
- SessionId

### Application

- PauseSessionCommand
- ResumeSessionCommand
- PauseSessionUseCase
- ResumeSessionUseCase
- SessionView

### Infrastructure

- CafeSessionJpaEntity
- CafeSessionRepositoryAdapter

### API

- POST /api/sessions/{id}/pause
- POST /api/sessions/{id}/resume
- GET /api/sessions/current

### Contrats API attendus

- `POST /api/sessions/{id}/pause` retourne la session mise a jour avec `paused = true`
- `POST /api/sessions/{id}/resume` retourne la session mise a jour avec `paused = false`
- `GET /api/sessions/current` retourne aussi l'etat `paused` pour chaque session

## Frontend

### Ecrans

- ecran sessions

### Composants

- active-sessions-table

### Navigation et comportements UI

- chaque ligne de session en cours expose un bouton `Pause` ou `Reprendre`
- une session en pause reste dans la liste `Sessions en cours`
- une session en pause est visuellement identifiable
- la liste des sessions en cours conserve une hauteur stable meme quand peu de lignes sont affichees
- la reprise d'une session remet a jour la ligne sans changer d'ecran
- les erreurs de pause ou de reprise restent visibles dans l'ecran

## Criteres d'acceptation

- un employe peut mettre en pause une session en cours
- un employe peut reprendre une session en pause
- une session en pause reste visible dans les sessions en cours
- le temps de pause n'est pas compte dans le temps facture ou deduit
