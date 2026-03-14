# Skill: pull-request-frontend

## Purpose

Preparer ou relire une pull request frontend Angular pour CyberManager avec un niveau d'exigence compatible feature architecture, typage strict, UX comptoir et contraintes de layout du projet.

## When to use

Utiliser cette skill lorsqu'une demande concerne :
- la preparation d'une PR frontend
- la revue d'une PR frontend
- la verification avant commit / avant push frontend
- la redaction d'un resume de PR frontend

## Required inputs

- `AGENTS.md`
- la ou les features `documentations/features/F-XXX` impactees
- les bugs `documentations/bugs/B-XXX` impactes si applicable
- le diff Git ou les fichiers modifies

## Steps

1. Verifier le perimetre :
    - features Angular touchees
    - pages / services / models / components touches
    - impact API ou contrats JSON

2. Controler l'architecture frontend :
    - logique HTTP dans les services
    - modeles TypeScript explicites
    - pas de logique metier lourde dans les templates
    - composants/pages ranges dans la bonne feature

3. Controler les interactions :
    - filtres, pagination, modales, drawers, drag and drop, toggles
    - aucune interaction existante critique perdue lors d'une refonte
    - calculs frontend alignes sur la logique backend si la modale simule un resultat

4. Controler le responsive :
    - pas de debordement vertical global
    - scroll confine aux bons panneaux
    - eviter les `100vh` imbriques dans un shell deja borne
    - preferer `dvh`, `clamp`, ou la hauteur disponible du parent

5. Controler le CSS :
    - styles dedies quand le composant devient dense
    - pas de duplication inutile
    - verifier le budget Angular `anyComponentStyle`

6. Controler l'etat Git avant PR :
    - aucun fichier frontend utile oublie hors versioning
    - assets / templates / css references bien commit
    - docs `F-XXX` / `B-XXX` mises a jour si le comportement change

7. Verifier si possible :
    - `npm run build`
    - warnings de build a signaler
    - risques UX residuels

8. Produire la sortie de PR :
    - ecrans touches
    - interactions ajoutees / modifiees
    - contrats API consommes
    - risques de regression visuelle ou ergonomique

## Output format

Toujours terminer par :
- ecrans impactes
- fichiers crees / modifies
- choix d'architecture frontend
- risques UX / responsive
- verification faite
