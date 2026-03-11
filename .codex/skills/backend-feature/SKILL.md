# Skill: backend-feature

## Purpose

Implémenter une fonctionnalité backend CyberManager dans une architecture DDD.

## When to use

Utiliser cette skill lorsqu'une feature impacte uniquement ou principalement le backend :
- nouveau cas d’usage métier
- nouvel endpoint REST
- nouvelle persistance
- nouveau bounded context
- évolution d’un aggregate ou d’un repository

## Required inputs

- le fichier de feature dans docs/features
- le bounded context concerné
- les règles de AGENTS.md

## Steps

1. Lire la feature et identifier :
    - objectif métier
    - acteurs
    - données manipulées
    - règles métier
    - cas d’usage
    - endpoints attendus

2. Identifier le bounded context concerné.

3. Créer ou compléter la couche domain :
    - entities
    - value objects
    - services de domaine
    - interfaces de repositories

4. Créer ou compléter la couche application :
    - use cases
    - services applicatifs
    - DTO applicatifs
    - ports

5. Créer ou compléter la couche infrastructure :
    - entities JPA
    - repositories Spring Data
    - adapters
    - implémentations des ports

6. Créer ou compléter la couche API :
    - controllers
    - request DTO
    - response DTO
    - mappers

7. Ajouter les tests :
    - unit tests domain / application
    - tests d’intégration si nécessaire

8. Vérifier :
    - aucun controller ne manipule une entité JPA
    - aucune dépendance Spring dans le domain
    - la logique métier principale est dans domain / application

## Output format

Toujours terminer par :
- résumé métier
- modules impactés
- fichiers créés / modifiés
- points de vigilance