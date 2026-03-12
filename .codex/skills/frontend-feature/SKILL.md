# Skill: frontend-feature

## Purpose

Implementer une fonctionnalite frontend Angular CyberManager de maniere maintenable et coherente avec l'application existante.

## When to use

Utiliser cette skill lorsqu'une demande impacte principalement le frontend :
- nouvel ecran Angular
- refonte UI d'une feature existante
- ajout de formulaire, filtre, pagination ou panneau lateral
- branchement d'un service API sur une page ou un composant
- harmonisation visuelle ou ergonomique

## Required inputs

- le fichier de feature dans `documentations/features` s'il existe
- `AGENTS.md`
- les bounded contexts frontend concernes

## Steps

1. Lire la feature et identifier :
    - objectif utilisateur
    - donnees affichees
    - actions utilisateur
    - erreurs a rendre visibles
    - comportement attendu de la liste, du detail et des formulaires

2. Identifier la feature Angular concernee :
    - `pages`
    - `components`
    - `services`
    - `models`

3. Implementer ou completer les modeles TypeScript :
    - typage strict
    - contrat aligne sur l'API
    - pas de modeles implicites dans les composants

4. Implementer ou completer les services Angular :
    - un service dedie par domaine d'appels API
    - aucune logique metier lourde dans le composant
    - messages d'erreur HTTP lisibles pour l'utilisateur

5. Implementer ou completer la page / les composants :
    - listes compactes
    - filtres immediats si le flux l'exige
    - pagination et taille de page configurable quand le volume peut croitre
    - panneaux lateraux ou accordions si le flux comptoir le demande
    - pour les formulaires reactifs, ne pas baser un `computed` sur `form.getRawValue()` si l'on attend une reaction immediate
    - utiliser `signals`, `valueChanges` ou un state local explicite pour les filtres instantanes
    - les filtres de recherche operationnels doivent reagir des les premieres lettres
    - une page de configuration doit normaliser les valeurs saisies avant sauvegarde et afficher un retour utilisateur apres enregistrement

6. Decouper proprement le CSS :
    - ne pas laisser grossir indefiniment les styles inline dans `styles:`
    - preferer un fichier CSS dedie par composant/page quand le style devient substantiel
    - sortir en priorite les gros blocs CSS des pages complexes vers `*.component.css`
    - garder dans le composant uniquement la logique UI, pas une feuille de style monolithique
    - mutualiser dans `styles.css` seulement ce qui est vraiment global
    - respecter le budget Angular configure pour `anyComponentStyle`
    - ne pas livrer un composant dont le fichier CSS depasse le budget de style du projet
    - si un composant approche la limite, deplacer les styles generiques vers `styles.css` ou decouper le composant

7. Verifier :
    - ecran lisible desktop et mobile
    - typage compile
    - appels API coherents
    - CSS decoupe de facon maintenable
    - aucun warning de budget Angular sur les styles de composant
    - aucune regression evidente sur les interactions
    - filtres reactifs effectivement mis a jour sans clic supplementaire
    - si une configuration locale est modifiee, la valeur est bien relue apres sauvegarde

## Output format

Toujours terminer par :
- modules impactes
- ecrans impactes
- fichiers crees / modifies
- choix d'architecture frontend
- points de vigilance
