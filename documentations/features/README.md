# Features CyberManager

Chaque fonctionnalite metier doit etre decrite dans un fichier dedie.

## Convention de nommage

Format recommande :

- F-001-authentication.md
- F-002-user-management.md
- F-003-product-management.md
- F-004-subscription-management.md
- F-005-walk-in-customer-management.md
- F-006-sales-and-princing.md
- F-007-session-time-tracking.md
- F-008-customer-to-subscriber-conversion.md
- F-009-daily-live-coffee-monitoring.md
- F-010-daily-history.md
- F-011-connection-time-pricing.md
- F-012-customer-debt-management.md
- F-013-session-pause.md
- F-014-operational-monitoring-financial-summary.md
- F-015-historical-reporting-by-period.md
- F-016-session-counter-operations.md
- F-017-frontend-observability-and-debug-logging.md
- F-018-backend-observability-and-debug-logging.md
- F-019-frontend-crud-filter-and-panel-standardization.md
- F-020-session-payment-and-stopped-session-sales.md

## Cycle de vie d'une feature

1. Redaction de la feature
2. Validation metier
3. Implementation backend
4. Implementation frontend
5. Verification DDD
6. Tests
7. Livraison

## Regles

- une feature = un objectif metier clair
- une feature doit pouvoir etre lue sans connaitre le code
- les criteres d'acceptation doivent etre testables
- la feature doit identifier le bounded context concerne
- la section Frontend doit expliciter les ecrans, la navigation, les listes, les creations et la gestion des erreurs
- une feature CRUD doit preciser le comportement attendu apres creation, edition, activation ou desactivation
- les clients journaliers et les abonnes doivent etre documentes comme deux flux distincts quand ils ne partagent pas la meme UI
- si une liste exhaustive de clients est contre-productive, la feature doit le dire explicitement et decrire la recherche ciblee attendue
- toute zone de recherche doit decrire son libelle et son comportement attendu, y compris l'autocompletion si elle existe
- les dates et heures affichees dans le frontend doivent suivre le format `JJ/MM/AAAA HH:mm`
- les ecrans de liste doivent privilegier des tableaux compacts avec espaces reduits, pagination et taille de page configurable quand le volume peut croitre
- les erreurs metier backend doivent etre retournees avec un contrat stable exploitable par le frontend (`code`, `message`, `status`, `timestamp`)
- les references aux abonnements doivent rappeler explicitement qu'ils sont cumulables quand le contexte le justifie
- toute evolution importante d'un ecran existant peut soit completer une feature existante, soit donner lieu a une nouvelle feature `F-XXX` si le comportement ajoute un vrai sous-domaine fonctionnel
