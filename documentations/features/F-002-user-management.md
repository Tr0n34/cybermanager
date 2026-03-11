# Feature ID: F-002

## Nom

Gestion des utilisateurs

## Contexte

Les administrateurs doivent pouvoir créer, consulter, modifier, activer et désactiver les utilisateurs de CyberManager.

## Acteurs

- Administrateur

## Objectif

Permettre l’administration du référentiel des utilisateurs et de leurs rôles.

## Bounded context

- users

## Règles métier

- un email doit être unique
- un utilisateur possède au moins un rôle
- un utilisateur désactivé conserve son historique
- seuls les administrateurs peuvent modifier les rôles

## Backend

### Cas d’usage
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
- UserSummaryDto
- UserDetailsDto

### Infrastructure
- UserJpaEntity
- UserJpaRepository
- UserSearchAdapter

### API
- GET /api/users
- GET /api/users/{id}
- POST /api/users
- PUT /api/users/{id}
- PUT /api/users/{id}/enable
- PUT /api/users/{id}/disable

## Frontend

### Écrans
- liste des utilisateurs
- détail utilisateur
- création utilisateur
- édition utilisateur

### Composants
- users-table
- user-form
- role-selector
- user-status-badge

## Critères d’acceptation

- un administrateur peut créer un utilisateur
- l’unicité de l’email est contrôlée
- l’activation et la désactivation sont visibles dans l’UI
- la liste permet recherche et filtrage