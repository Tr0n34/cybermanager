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

## Ameliorations integrees

- separation explicite du module `auth` par rapport au referentiel `users`
- exposition de deux endpoints REST dedies : connexion et recuperation de l'utilisateur courant
- prise en charge d'un jeton JWT pour transporter l'identite et les roles
- gestion d'erreurs API homogenes cote authentification
- support d'une configuration runtime des URLs frontend avec fallback `localhost`

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
- EmailAddress
- AppRole
- PasswordHash

### Application
- AuthenticateUserUseCase
- LoadCurrentUserUseCase
- AuthenticationResultView
- CurrentUserView

### Infrastructure
- AuthenticatedUserRepositoryAdapter
- PasswordVerifierAdapter
- JwtTokenProvider
- JwtAccessTokenReader

### API
- POST /api/auth/login
- GET /api/auth/me

### Contrats API attendus
- `POST /api/auth/login` recoit `email` et `password`, puis retourne un access token, l'utilisateur courant et ses roles
- `GET /api/auth/me` attend un header `Authorization: Bearer <token>` et retourne le profil courant resolu depuis le jeton
- en cas d'echec metier, l'API retourne une erreur structuree avec `code`, `message`, `status` et `timestamp`

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
- le profil courant est recharge au demarrage de l'application si un jeton est deja stocke
- toute reponse `401` invalide la session locale et renvoie vers l'ecran de connexion
- le frontend peut lire une configuration runtime des URLs d'API, avec fallback local `localhost` pour l'auth et le backend metier

## Criteres d'acceptation

- un utilisateur valide obtient un jeton et son profil
- un utilisateur invalide recoit une erreur d'authentification
- un utilisateur inactif ne peut pas se connecter
- le frontend redirige vers le tableau de bord apres connexion
- l'appel `GET /api/auth/me` restitue le meme utilisateur que celui authentifie par `POST /api/auth/login`
