# Feature ID: F-001

## Nom

Authentification des utilisateurs

## Contexte

CyberManager doit permettre à un utilisateur de se connecter de manière sécurisée afin d’accéder aux fonctionnalités correspondant à son rôle.

## Acteurs

- Administrateur
- Utilisateur

## Objectif

Permettre l’authentification d’un utilisateur via login / mot de passe et la récupération de son profil applicatif.

## Bounded context

- auth
- users

## Règles métier

- seul un utilisateur actif peut s’authentifier
- un utilisateur désactivé ne peut pas ouvrir de session
- les rôles applicatifs sont chargés au moment de l’authentification
- une tentative d’authentification invalide retourne une erreur métier explicite

## Backend

### Cas d’usage
- AuthenticateUser
- LoadAuthenticatedUserProfile

### Domain
- User
- UserId
- Email
- Role
- PasswordHash

### Application
- AuthenticateUserUseCase
- LoadCurrentUserUseCase
- AuthenticationResultDto
- CurrentUserDto

### Infrastructure
- UserJpaEntity
- UserJpaRepository
- PasswordEncoderAdapter
- JwtTokenProvider

### API
- POST /api/auth/login
- GET /api/auth/me

## Frontend

### Écrans
- page de connexion
- récupération du profil courant après login

### Composants
- login-form
- authentication-error-banner

### Appels API
- POST /api/auth/login
- GET /api/auth/me

## Critères d’acceptation

- un utilisateur valide obtient un jeton et son profil
- un utilisateur invalide reçoit une erreur d’authentification
- un utilisateur inactif ne peut pas se connecter
- le frontend redirige vers le tableau de bord après connexion