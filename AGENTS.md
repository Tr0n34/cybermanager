# AGENTS.md

## Contexte projet

CyberManager est une application fullstack de gestion d'un cyber cafe.

Stack cible :
- Backend : Java 21 / Spring Boot
- Build : Maven multi-module
- Architecture backend : DDD
- Frontend : Angular
- API : REST JSON
- Mapping : MapStruct
- Base de donnees : PostgreSQL
- Tests : JUnit / Mockito / Spring Boot Test / Angular unit tests

## Objectifs

L'application permet de :
- gerer les utilisateurs et leurs roles
- gerer les produits, abonnements, clients et sessions
- suivre les ventes, dettes, vues de monitoring et historique

## Architecture backend attendue

Le backend est organise selon les standards DDD : `domain`, `application`, `infrastructure`, `api`.

Exemple :

backend/
|- cybermanager-parent
|- auth
|  |- auth-domain
|  |- auth-application
|  |- auth-infrastructure
|  `- auth-api
`- cybermanager
   |- cybermanager-domain
   |- cybermanager-application
   |- cybermanager-infrastructure
   `- cybermanager-api

## Regles DDD backend

### Convention de packaging backend

Le packaging Java doit etre organise d'abord par couche technique, puis par bounded context a l'interieur de cette couche.

Exemples attendus :
- `com.cybermanager.domain.model.users`
- `com.cybermanager.domain.port.users`
- `com.cybermanager.application.commands.users`
- `com.cybermanager.application.queries.users`
- `com.cybermanager.application.views.users`
- `com.cybermanager.application.usecases.users`
- `com.cybermanager.application.services.users`
- `com.cybermanager.api.controllers.users`
- `com.cybermanager.api.dtos.users`
- `com.cybermanager.api.mappers.users`
- `com.cybermanager.infrastructure.adapters.persistence.users`
- `com.cybermanager.infrastructure.entities.persistence.users`
- `com.cybermanager.infrastructure.repositories.persistence.users`
- `com.cybermanager.infrastructure.security.users`

Regles :
- ne pas mettre le bounded context a la racine du package avant la couche
- le bounded context doit apparaitre a l'interieur du package de couche
- appliquer la meme logique dans `auth`, par exemple `com.cybermanager.auth.application.services`

### Domain

Contient exclusivement :
- entites metier
- value objects
- agregats
- ports entrants et sortants du domaine
- evenements de domaine
- interfaces de repository

Contraintes :
- aucune dependance Spring
- aucune annotation JPA
- aucune logique technique
- aucune exposition HTTP
- package racine attendu : `com.cybermanager.domain.*` ou `com.cybermanager.auth.domain.*`

### Application

Contient :
- use cases
- services applicatifs
- read models applicatifs
- orchestrations metier

Contraintes :
- ne contient pas de logique d'infrastructure
- depend du domain, jamais de l'API
- annotations autorisees : `@Transactional`, `@Qualifier`, `@Service`
- les use cases prennent en entree une `Query` ou une `Command` et retournent une `View`
- les use cases exposent une methode `execute`
- package racine attendu : `com.cybermanager.application.*` ou `com.cybermanager.auth.application.*`

### Infrastructure

Contient :
- entites JPA
- repositories Spring Data
- adapters
- clients externes
- persistance
- configuration technique

Contraintes :
- peut dependre de `application` et `domain`
- ne doit pas contenir la logique metier centrale
- pas d'appel direct aux `JpaRepository` hors adapter
- package racine attendu : `com.cybermanager.infrastructure.*` ou `com.cybermanager.auth.infrastructure.*`

### API

Contient :
- controllers REST
- request / response DTO
- mappers API
- gestion des erreurs HTTP

Contraintes :
- un controller ne retourne jamais d'entite domain ou JPA
- un controller appelle les use cases / services applicatifs
- pas d'acces direct aux repositories depuis les controllers
- package racine attendu : `com.cybermanager.api.*` ou `com.cybermanager.auth.api.*`

## Architecture frontend attendue

Organisation Angular par feature.

frontend/src/app/
|- core/
|- shared/
|- layout/
`- features/
   |- auth/
   |- dashboard/
   |- users/
   `- reporting/

### core

Contient les services transverses, gardes, interceptors et infrastructure Angular commune.

### shared

Contient :
- composants reutilisables
- pipes
- directives
- modeles partages

### features

Chaque feature contient :
- `pages`
- `components`
- `services`
- `models`
- `routes` si necessaire
- etat local si necessaire

## Bounded contexts

### auth
Authentification et recuperation de l'utilisateur courant.

### users
Gestion des employes, administrateurs et roles applicatifs.

### product
Catalogue des produits vendus au cybercafe.

### subscription
Offres d'abonnement et credit de temps associe.

### customer
Gestion des clients occasionnels et abonnes.

### sales
Vente de produits, d'abonnements et de temps de connexion.

### session
Gestion des sessions de connexion sur les postes.

### monitoring
Vue operationnelle de la journee, agregee et lisible.

### reporting
Historique journalier et consultation des journees passees.

## Regles frontend

- architecture par feature
- composants simples et fortement types
- services Angular dedies aux appels API
- pas de logique metier lourde dans les composants
- typage strict TypeScript
- toujours isoler les modeles d'entree/sortie
- pour les formulaires reactifs, ne pas attendre de reactivite d'un `computed` base directement sur `form.getRawValue()`
- preferer `signals`, `valueChanges` ou un state local explicite pour les filtres instantanes
- toute page de configuration doit normaliser les valeurs avant sauvegarde et rendre visible la confirmation d'enregistrement

## Workflow attendu pour Codex

Pour toute nouvelle fonctionnalite :

1. lire la feature dans `documentations/features`
2. identifier le bounded context impacte
3. implementer le domain
4. implementer l'application
5. implementer l'infrastructure
6. implementer l'API
7. implementer le frontend Angular de la feature
8. ecrire ou completer les tests
9. verifier le respect DDD
10. mettre a jour la documentation de feature si le comportement livre a evolue
11. resumer les fichiers modifies

## Regles de livraison

Une tache est terminee si :
- le code compile
- les tests pertinents passent
- les couches DDD sont respectees
- aucun controller n'expose d'entite
- le frontend consomme correctement l'API
- la documentation fonctionnelle est a jour quand le comportement utilisateur change
- un resume clair des changements est fourni

## Interdictions

- ne pas retourner d'entity JPA dans les endpoints
- ne pas injecter de repository dans un controller
- ne pas mettre de logique metier complexe dans les adapters
- ne pas melanger DTO API, DTO applicatifs et objets domain
- ne pas creer de dependance circulaire entre modules

## Convention de reponse de l'agent

Quand une implementation est proposee, toujours fournir :
- les modules concernes
- les fichiers crees / modifies
- les choix d'architecture
- les points de vigilance
