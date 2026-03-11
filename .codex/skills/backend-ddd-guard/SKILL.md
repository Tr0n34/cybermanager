# Skill: backend-ddd-guard

## Purpose

Contrôler le respect de l’architecture DDD dans le backend CyberManager.

## When to use

Utiliser cette skill après toute modification backend significative.

## Checks

### API
- aucun controller n’injecte directement un repository
- aucun controller ne retourne d’entité domain ou JPA
- les DTO API sont séparés des objets métier

### Application
- les use cases orchestrent sans logique technique parasite
- la couche application dépend du domain, pas de l’API

### Domain
- aucune dépendance Spring
- aucune annotation JPA
- logique métier centrale présente ici

### Infrastructure
- les implémentations techniques restent dans infrastructure
- les adapters implémentent les ports définis côté application ou domain

## Output format

Fournir :
- conformité globale : OK / KO
- violations détectées
- fichiers concernés
- correctifs recommandés