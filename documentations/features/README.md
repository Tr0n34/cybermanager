# Features CyberManager

Chaque fonctionnalité métier doit être décrite dans un fichier dédié.

## Convention de nommage

Format recommandé :

F-001-authentication.md
F-002-user-management.md
F-003-asset-management.md
F-004-vulnerability-management.md
F-007-dashboard.md
F-008-reporting.md

## Cycle de vie d'une feature

1. Rédaction de la feature
2. Validation métier
3. Implémentation backend
4. Implémentation frontend
5. Vérification DDD
6. Tests
7. Livraison

## Règles

- une feature = un objectif métier clair
- une feature doit pouvoir être lue sans connaître le code
- les critères d’acceptation doivent être testables
- la feature doit identifier le bounded context concerné