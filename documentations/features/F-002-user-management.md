# Feature ID: F-002

## Nom

Gestion des utilisateurs

## Contexte

Les administrateurs doivent pouvoir creer, consulter, modifier, activer et desactiver les utilisateurs de CyberManager.

## Acteurs

- Administrateur

## Objectif

Permettre l'administration du referentiel des utilisateurs et de leurs roles.

## Bounded context

- users

## Regles metier

- un email doit etre unique
- un utilisateur possede au moins un role
- un utilisateur desactive conserve son historique
- seuls les administrateurs peuvent modifier les roles

## Backend

### Cas d'usage
- CreateUser
- UpdateUser
- DisableUser
- EnableUser
- SearchUsers
- GetUserDetails

### Domain
- User
- UserId
- Email
- Role
- UserStatus

### Application
- CreateUserUseCase
- UpdateUserUseCase
- SearchUsersUseCase
- UserSummaryView
- UserDetailsView

### Infrastructure
- UserJpaEntity
- UserJpaRepository
- UserRepositoryAdapter

### API
- GET /api/users
- GET /api/users/{id}
- POST /api/users
- PUT /api/users/{id}
- PUT /api/users/{id}/enable
- PUT /api/users/{id}/disable

## Frontend

### Ecrans
- liste des utilisateurs
- detail utilisateur
- creation utilisateur
- edition utilisateur
- navigation depuis le menu principal vers l'ecran utilisateurs
- retour immediat en mode creation apres annulation ou sauvegarde

### Composants
- users-table
- user-form
- role-selector
- user-status-badge

### Navigation et comportements UI
- la liste se charge a l'ouverture de l'ecran
- la recherche et le filtrage rechargent la liste visible
- la selection d'un utilisateur ouvre le formulaire en mode edition
- l'action "nouvel utilisateur" reinitialise le formulaire et les roles
- une creation ou une modification recharge la liste et met a jour l'affichage
- les erreurs API sont affichees dans l'ecran sans casser la navigation

## Criteres d'acceptation

- un administrateur peut creer un utilisateur
- l'unicite de l'email est controlee
- l'activation et la desactivation sont visibles dans l'UI
- la liste permet recherche et filtrage
- le bouton de creation ouvre un formulaire exploitable sans soumission parasite
