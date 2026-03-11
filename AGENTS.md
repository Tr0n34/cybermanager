# AGENTS.md

## Contexte projet

CyberManager est une application fullstack de gestion d'un cyber café

Stack cible :
- Backend : Java 21 / Spring Boot
- Build : Maven multi-module
- Architecture backend : DDD
- Frontend : Angular
- API : REST JSON
- Mapping : MapStruct
- Base de données : PostgreSQL
- Tests : JUnit / Mockito / Spring Boot Test / Angular unit tests

## Objectifs

L'application permet de :
- gérer les utilisateurs et leurs rôles


## Architecture backend attendue

Le backend est organisé selon les standards DDD : domain, application, infrastructure et api.

Les bounded context (c'est à dire les "objets métier" sont dans le domain)

Exemple :

backend/
├── cybermanager-parent
├── auth
│   ├── auth-domain
│   ├── auth-application
│   ├── auth-infrastructure
│   └── auth-api
├── cybermanager
│   ├── cybermanager-domain
│   ├── cybermanager-application
│   ├── cybermanager-infrastructure
│   └── cybermanager-api


## Règles DDD backend

### Convention de packaging backend

Le packaging Java doit être organisé d'abord par couche technique, puis par bounded context à l'intérieur de cette couche.

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

Règles :
- ne pas mettre le bounded context à la racine du package avant la couche
- le bounded context doit apparaître à l'intérieur du package de couche
- appliquer la même logique dans `auth`, avec le préfixe module si nécessaire, par exemple `com.cybermanager.auth.application.services`

### Domain
Contient exclusivement :
- entités métier
- value objects
- agrégats
- port entrant et sortant de domaine
- événements de domaine
- interfaces de repository (port sortant)

Contraintes :
- aucune dépendance Spring
- aucune annotation JPA
- aucune logique technique
- aucune exposition HTTP
- package racine attendu : `com.cybermanager.domain.*` ou `com.cybermanager.auth.domain.*`

### Application
Contient :
- use cases
- services applicatifs
- Read models applicatifs (VIEW)
- ports d’entrée / sortie
- orchestrations métier

Contraintes :
- ne contient pas de logique d’infrastructure
- dépend du domain, jamais de l’APIs
- ne contient que des annotations springs : @Transactional, @Qualifier, @Service
- Les usecase prennent en entrée une Query/Command et en sortie une View
- Les usecase ont une méthode execute (pattern command
- package racine attendu : `com.cybermanager.application.*` ou `com.cybermanager.auth.application.*`

### Infrastructure
Contient :
- entités JPA
- repositories Spring Data
- implémentations d’adapters
- clients externes
- persistance
- configuration technique

Contraintes :
- peut dépendre de application et domain
- ne doit pas contenir la logique métier centrale
- pas d'appel direct des JpaRepository passage par un adapter obligatoire
- package racine attendu : `com.cybermanager.infrastructure.*` ou `com.cybermanager.auth.infrastructure.*`

### API
Contient :
- controllers REST (dans in)
- request / response DTO (dans in/dto ou out/dto)
- mappers API (dans in/mappers)
- gestion des erreurs HTTP

Contraintes :
- un controller ne retourne jamais d’entité domain ou d’entité JPA
- un controller appelle les use cases / services applicatifs
- pas d’accès direct aux repositories depuis les controllers
- package racine attendu : `com.cybermanager.api.*` ou `com.cybermanager.auth.api.*`

## Architecture frontend attendue

Organisation Angular par feature.

frontend/src/app/
├── core/
├── shared/
├── layout/
└── features/
├── auth/
├── dashboard/
├── users/
└── reporting/

### core
Contient :
e

### shared
Contient :
- composants réutilisables
- pipes
- directives
- modèles partagés

### features
Chaque feature contient :
- pages
- components
- services
- models
- routes si nécessaire
- state local si nécessaire

### Bounded Context

## Bounded contexts

### auth
Authentification et récupération de l’utilisateur courant.

### users
Gestion des employés, administrateurs et rôles applicatifs.

### product
Catalogue des produits vendus au cybercafé.

### subscription
Offres d’abonnement et crédit de temps associé.

### customer
Gestion des clients occasionnels et abonnés.

### sales
Vente de produits, d’abonnements et de temps de connexion.

### session
Gestion des sessions de connexion sur les postes.

### monitoring
Vue opérationnelle de la journée, agrégée et lisible.

### reporting
Historique journalier et consultation des journées passées.

## Règles frontend

- architecture par feature
- composants simples et fortement typés
- services Angular dédiés aux appels API
- pas de logique métier lourde dans les composants
- typage strict TypeScript
- toujours isoler les modèles d’entrée/sortie

## Workflow attendu pour Codex

Pour toute nouvelle fonctionnalité :

1. lire la feature dans documentations/features
2. identifier le bounded context impacté
3. implémenter le domain
4. implémenter l’application
5. implémenter l’infrastructure
6. implémenter l’API
7. implémenter le frontend Angular de la feature
8. écrire ou compléter les tests
9. vérifier le respect DDD
10. résumer les fichiers modifiés

## Règles de livraison

Une tâche est terminée si :
- le code compile
- les tests pertinents passent
- les couches DDD sont respectées
- aucun controller n’expose d’entité
- le frontend consomme correctement l’API
- un résumé clair des changements est fourni

## Interdictions

- ne pas retourner d’Entity JPA dans les endpoints
- ne pas injecter de repository dans un controller
- ne pas mettre de logique métier complexe dans les adapters
- ne pas mélanger DTO API, DTO applicatifs et objets domain
- ne pas créer de dépendance circulaire entre modules

## Convention de réponse de l’agent

Quand une implémentation est proposée, toujours fournir :
- les modules concernés
- les fichiers créés / modifiés
- les choix d’architecture
- les points de vigilance
