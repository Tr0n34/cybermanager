# Bug ID: B-002

## Nom

Recuperation d'un commit incomplet et regressions UI associees

## Contexte

Apres recuperation du dernier commit sur un autre poste, plusieurs comportements frontend etaient devenus non operationnels, notamment sur le dashboard et certains flux sessions.

## Symptomes

- glisser-deposer du dashboard non fonctionnel
- ecarts entre le poste source et le poste de recuperation
- references a des fichiers existants localement mais absents du commit
- comportements frontend differents selon le poste de travail

## Cause

Le depot contenait un etat de travail ou plusieurs fichiers effectivement utilises par l'application n'etaient pas versionnes.

La recuperation du commit sur un autre poste produisait donc un arbre incomplet, auquel s'ajoutait une regression frontend sur la page dashboard/monitoring ayant perdu sa logique de glisser-deposer.

## Correctif applique

- reintroduction de la logique de glisser-deposer dans [monitoring-page.component.ts](/D:/DATA/cybermanager/frontend/src/app/features/monitoring/pages/monitoring-page.component.ts)
- remise a l'index Git des fichiers frontend et backend oublies mais deja references par le code
- verification compile / tests apres reconstitution de l'arbre attendu

## Prevention

- ne jamais considerer un commit comme livrable tant que `git status` ne laisse pas de fichiers fonctionnels non suivis
- verifier qu'un clone propre ou un checkout sur un autre poste compile sans dependre d'un etat local cache
- pour les ecrans critiques, valider les interactions UI principales apres une refonte importante

## Verification

- `mvn -pl cm-application,cm-api -am test`
- `cmd /c npm run build`
- verification manuelle du glisser-deposer dashboard apres recuperation sur un poste propre
