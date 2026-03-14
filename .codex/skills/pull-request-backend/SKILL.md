# Skill: pull-request-backend

## Purpose

Preparer ou relire une pull request backend Spring Boot / Java pour CyberManager avec un niveau d'exigence compatible DDD, Maven multi-module et API REST.

## When to use

Utiliser cette skill lorsqu'une demande concerne :
- la preparation d'une PR backend
- la revue d'une PR backend
- la verification avant commit / avant push backend
- la redaction d'un resume de PR backend

## Required inputs

- `AGENTS.md`
- la ou les features `documentations/features/F-XXX` impactees
- les bugs `documentations/bugs/B-XXX` impactes si applicable
- le diff Git ou les fichiers modifies

## Steps

1. Verifier le perimetre :
    - bounded contexts touches
    - modules Maven touches
    - impacts API / application / domain / infrastructure

2. Controler la structure DDD :
    - pas de logique metier centrale dans les controllers ou adapters
    - pas de dependance Spring dans le domain
    - packages conformes `domain / application / infrastructure / api`

3. Controler le contrat API :
    - pas d'entite JPA exposee
    - DTO REST explicites
    - erreurs HTTP stables et lisibles
    - compatibilite frontend si l'API change

4. Controler la logique metier :
    - coherence des regles de calcul
    - absence de regression evidente
    - cas limites traites
    - dette / paiement / cloture / historiques verifies si concernes

5. Controler la persistance :
    - repositories limites aux adapters
    - pas de requetes dupliquees inutiles
    - migrations / schema / bootstrap coherents si concernes

6. Controler les tests :
    - tests application/domain ajoutes ou adaptes
    - scenarii de regression couverts
    - commandes de verification executees si possible

7. Controler l'etat Git avant PR :
    - aucun fichier fonctionnel oublie hors versioning
    - pas de modifications parasites dans la PR
    - docs `F-XXX` / `B-XXX` mises a jour si le comportement change

8. Produire la sortie de PR :
    - objectif metier
    - modules touches
    - endpoints touches
    - risques / points de vigilance
    - verification effectuee

## Output format

Toujours terminer par :
- modules backend impactes
- endpoints / contrats impactes
- fichiers crees / modifies
- risques et regressions possibles
- verification faite
