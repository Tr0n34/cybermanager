# Feature ID: F-001

## Nom

Authentification des utilisateurs

## Contexte

CyberManager doit permettre a un utilisateur de se connecter de maniere securisee afin d'acceder aux fonctionnalites correspondant a son role.

## Acteurs

- Administrateur
- Utilisateur

## Objectif

Permettre l'authentification d'un utilisateur via login / mot de passe et la recuperation de son profil applicatif.

## Bounded context

- auth
- users

## Regles metier

- seul un utilisateur actif peut s'authentifier
- un utilisateur desactive ne peut pas ouvrir de session
- les roles applicatifs sont charges au moment de l'authentification
- une tentative d'authentification invalide retourne une erreur metier explicite

## Backend

### Cas d'usage
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
- AuthenticationResultView
- CurrentUserView

### Infrastructure
- UserJpaEntity
- UserJpaRepository
- PasswordVerifierAdapter
- JwtTokenProvider

### API
- POST /api/auth/login
- GET /api/auth/me

## Frontend

### Ecrans
- page de connexion
- recuperation du profil courant apres login
- redirection automatique vers le tableau de bord apres authentification
- redirection automatique vers la page de connexion en cas de deconnexion ou d'absence de session

### Composants
- login-form
- authentication-error-banner

### Appels API
- POST /api/auth/login
- GET /api/auth/me

### Navigation et comportements UI
- l'utilisateur non authentifie ne peut pas acceder aux ecrans proteges
- l'utilisateur authentifie ne doit plus rester sur l'ecran de login
- un message d'erreur lisible est affiche si l'authentification echoue
- la session stocke le jeton et le profil courant pour les ecrans suivants

## Criteres d'acceptation

- un utilisateur valide obtient un jeton et son profil
- un utilisateur invalide recoit une erreur d'authentification
- un utilisateur inactif ne peut pas se connecter
- le frontend redirige vers le tableau de bord apres connexion
