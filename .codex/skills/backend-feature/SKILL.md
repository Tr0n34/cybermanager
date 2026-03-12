# Skill: backend-feature

## Purpose

Implementer une fonctionnalite backend CyberManager dans une architecture DDD.

## When to use

Utiliser cette skill lorsqu'une feature impacte uniquement ou principalement le backend :
- nouveau cas d'usage metier
- nouvel endpoint REST
- nouvelle persistance
- nouveau bounded context
- evolution d'un aggregate ou d'un repository

## Required inputs

- le fichier de feature dans documentations/features
- le bounded context concerne
- les regles de AGENTS.md

## Steps

1. Lire la feature et identifier :
    - objectif metier
    - acteurs
    - donnees manipulees
    - regles metier
    - cas d'usage
    - endpoints attendus

2. Identifier le bounded context concerne.

3. Creer ou completer la couche domain :
    - entities
    - value objects
    - services de domaine
    - interfaces de repositories
    - packages attendus : `com.cybermanager.domain.model.<bounded-context>`, `com.cybermanager.domain.port.<bounded-context>`

4. Creer ou completer la couche application :
    - use cases
    - services applicatifs
    - DTO applicatifs
    - ports
    - packages attendus : `com.cybermanager.application.commands.<bounded-context>`, `queries`, `views`, `usecases`, `services`, `ports`

5. Creer ou completer la couche infrastructure :
    - entities JPA
    - repositories Spring Data
    - adapters
    - implementations des ports
    - packages attendus : `com.cybermanager.infrastructure.adapters.*.<bounded-context>`, `entities.*.<bounded-context>`, `repositories.*.<bounded-context>`

6. Creer ou completer la couche API :
    - controllers
    - request DTO
    - response DTO
    - mappers
    - packages attendus : `com.cybermanager.api.controllers.<bounded-context>`, `com.cybermanager.api.dtos.<bounded-context>`, `com.cybermanager.api.mappers.<bounded-context>`

7. Standardiser les erreurs metier backend :
    - ne pas renvoyer les erreurs metier attendues via `IllegalArgumentException` si elles doivent etre comprises par le frontend
    - preferer une exception metier explicite avec un `code` stable, un `message` lisible et un type parmi `VALIDATION`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`
    - centraliser la traduction HTTP dans `ApiExceptionHandler`
    - retourner un payload d'erreur coherent : `code`, `message`, `status`, `timestamp`
    - reserver les erreurs techniques non prevues au fallback `500 INTERNAL_ERROR`

8. Ajouter les tests :
    - unit tests domain / application
    - tests d'integration si necessaire

9. Verifier :
    - aucun controller ne manipule une entite JPA
    - aucune dependance Spring dans le domain
    - la logique metier principale est dans domain / application
    - le bounded context est imbrique dans le package de couche et non l'inverse
    - les erreurs metier attendues sont converties en reponses HTTP stables et testables

## Output format

Toujours terminer par :
- resume metier
- modules impactes
- fichiers crees / modifies
- points de vigilance
