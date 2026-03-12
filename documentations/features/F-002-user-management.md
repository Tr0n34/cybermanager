# Feature ID: F-002

## Nom

Gestion des utilisateurs

## Contexte

Les administrateurs doivent pouvoir creer, consulter, modifier, activer et desactiver les utilisateurs de CyberManager.

## Acteurs

- Administrateur

## Objectif

Permettre l'administration du referentiel des utilisateurs et de leurs roles.

## Ameliorations integrees

- mise en place des commandes et queries dediees `Create`, `Update`, `Enable`, `Disable`, `Search`, `GetDetails`
- ajout d'un mapping API explicite entre commandes applicatives et `UserResponse`
- securisation des actions via lecture du JWT et controle des roles administrateur
- ecran Angular unifie liste + formulaire avec composants reutilisables
- ajout de la suppression utilisateur
- refonte de l'ecran en mode liste compacte + panneau latere droite
- filtres instantanes par nom, role et statut avec pagination et taille de page configurable

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
- DeleteUser
- SearchUsers
- GetUserDetails

### Domain
- User
- UserId
- EmailAddress
- AppRole
- UserStatus

### Application
- CreateUserUseCase
- UpdateUserUseCase
- EnableUserUseCase
- DisableUserUseCase
- SearchUsersUseCase
- GetUserDetailsUseCase
- UserSummaryView
- UserDetailsView

### Infrastructure
- UserJpaEntity
- UserJpaRepository
- UserRepositoryAdapter
- PasswordHasherAdapter
- JwtAccessTokenReader

### API
- GET /api/users
- GET /api/users/{id}
- POST /api/users
- PUT /api/users/{id}
- DELETE /api/users/{id}
- PUT /api/users/{id}/enable
- PUT /api/users/{id}/disable

### Parametres et comportements API
- `GET /api/users` accepte `term` et `status`
- toutes les routes exigent un bearer token valide
- les routes d'ecriture retournent l'etat courant complet de l'utilisateur

## Frontend

### Ecrans
- liste des utilisateurs
- detail utilisateur
- creation utilisateur
- edition utilisateur
- navigation depuis le menu principal vers l'ecran utilisateurs
- retour immediat en mode creation apres annulation ou sauvegarde
- liste compacte avec pagination
- panneau de creation / edition refermable

### Composants
- users-table
- user-form
- role-selector
- user-status-badge

### Navigation et comportements UI
- la liste se charge a l'ouverture de l'ecran
- la recherche par nom, role et statut filtre la liste immediatement a chaque saisie
- la taille de page est configurable depuis l'ecran et la pagination reste sur la meme page
- le tableau reste dense et compact pour afficher davantage d'utilisateurs sans allonger inutilement la page
- la selection d'un utilisateur ouvre le panneau de droite en mode edition
- l'action "nouvel utilisateur" ouvre le panneau de droite avec transition, masque le bouton de creation et reinitialise le formulaire
- une creation, une modification ou une suppression recharge la liste et referme le panneau
- les erreurs API sont affichees dans l'ecran sans casser la navigation
- l'activation et la desactivation sont accessibles directement depuis la liste
- la suppression est declenchable depuis l'edition avec confirmation
- le formulaire gere la creation et l'edition sans changer de page

## Criteres d'acceptation

- un administrateur peut creer un utilisateur
- l'unicite de l'email est controlee
- l'activation et la desactivation sont visibles dans l'UI
- la liste permet recherche et filtrage
- le bouton de creation ouvre un formulaire exploitable sans soumission parasite
- un utilisateur peut etre supprime depuis l'ecran d'edition
