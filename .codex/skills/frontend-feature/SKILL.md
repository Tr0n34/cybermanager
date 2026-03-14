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
- la strategie de journalisation attendue si la feature ajoute ou modifie des flux critiques

## Steps

1. Lire la feature et identifier :
    - objectif utilisateur
    - donnees affichees
    - actions utilisateur
    - erreurs a rendre visibles
    - comportement attendu de la liste, du detail et des formulaires
    - points de journalisation utiles pour les appels API, anomalies et traitements complexes

2. Identifier la feature Angular concernee :
    - `pages`
    - `components`
    - `services`
    - `models`
    - emplacement du logging partage (`core`, interceptor, service dedie)

3. Implementer ou completer les modeles TypeScript :
    - typage strict
    - contrat aligne sur l'API
    - pas de modeles implicites dans les composants

4. Implementer ou completer les services Angular :
    - un service dedie par domaine d'appels API
    - aucune logique metier lourde dans le composant
    - messages d'erreur HTTP lisibles pour l'utilisateur
    - si la feature est sensible ou difficile a diagnostiquer, ajouter une journalisation `debug` / `info` exploitable
    - journaliser les requetes sortantes utiles au diagnostic avec methode, URL, contexte fonctionnel et charge utile nettoyee si necessaire
    - journaliser les reponses entrantes utiles avec statut, duree et resume de resultat
    - journaliser les anomalies HTTP et de mapping avec un message actionnable
    - ne jamais logger mot de passe, token, cookie, secret ou donnees personnelles non necessaires

5. Implementer ou completer la page / les composants :
    - listes compactes
    - filtres immediats si le flux l'exige
    - lors d'une refonte d'ecran existant, verifier explicitement que les interactions deja presentes sont preservees : drag and drop, modales, toggles, panneaux de detail, raccourcis de navigation
    - pour les pages CRUD de liste, standardiser les filtres dans un bloc `filters-grid collapsible` pilote par un bouton `Filtres` avec icone
    - ecrire et appliquer explicitement cette regle sur tous les nouveaux ecrans de liste : les filtres sont masques par defaut, visibles par clic sur `Filtres` avec icone, et jamais affiches brutalement en permanence sans justification fonctionnelle
    - utiliser une ouverture / fermeture progressive du bloc de filtres, avec animation legere, plus smooth que brutale, plutot qu'un rendu abrupt
    - les filtres CRUD doivent privilegier des champs compacts, alignes a gauche et dimensionnes selon le contenu attendu plutot qu'etires inutilement
    - le bouton `Filtres` doit rester dans le bandeau d'actions sous le titre de page, pas perdu plus bas dans l'ecran
    - placer l'action de creation principale a cote du bouton `Filtres` (`Creer un nouvel ...`) plutot qu'au fond d'un panneau secondaire
    - eviter de dupliquer un bouton `Nouveau` dans la colonne de droite si l'action de creation principale existe deja en haut de page
    - pagination et taille de page configurable quand le volume peut croitre
    - si une pagination existe, utiliser explicitement `Precedent` / `Suivant` avec indication `Page X / Y`
    - quand la pagination existe sur une liste de resultats, les boutons `Precedent` / `Suivant` doivent etre places dans le cadre de cette liste, a cote du compteur de resultats, pas dans le bandeau d'actions global sous le titre
    - ecrire et appliquer explicitement cette regle : `Precedent` / `Suivant` pilotent le tableau `Resultats`, donc ils vivent dans le cadre `Resultats`, sur la meme ligne que le compteur, jamais en haut de page
    - les filtres de pagination (`Clients par page`, `Dettes par page`, etc.) doivent vivre dans le bloc `filters-grid`, pas dans le bandeau d'actions
    - panneaux lateraux ou accordions si le flux comptoir le demande
    - un panneau secondaire ouvert / ferme par bouton doit etre ferme par defaut si cela reduit la charge visuelle initiale
    - une modale de simulation ou de calcul frontend doit reprendre la logique metier backend existante si elle sert a previsualiser un resultat utilisateur
    - pour les formulaires reactifs, ne pas baser un `computed` sur `form.getRawValue()` si l'on attend une reaction immediate
    - utiliser `signals`, `valueChanges` ou un state local explicite pour les filtres instantanes
    - les filtres de recherche operationnels doivent reagir des les premieres lettres
    - une page de configuration doit normaliser les valeurs saisies avant sauvegarde et afficher un retour utilisateur apres enregistrement
    - journaliser les traitements UI difficiles : transformation de donnees, synchronisation d'etat, normalisation, calculs derives
    - privilegier des logs centralises et coherents plutot que des `console.log` disperses
    - garder les logs utiles au diagnostic sans surcharger les parcours simples
    - pour les ecrans denses en hauteur, preferer des tailles derivees du conteneur disponible ou de variables globales `dvh/clamp` plutot que des hauteurs fixes ou des `100vh` imbriques
    - si l'application est deja rendue dans un shell contraint en hauteur, confiner le scroll dans les panneaux internes et eviter tout debordement vertical du document global

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
    - filtres de liste coherents avec les autres CRUD frontend : bouton `Filtres`, panneau depliable, animation legere, action de creation visible
    - champs de filtre compactes et correctement calibres pour leur contenu
    - pagination coherente avec le standard frontend si le volume justifie plusieurs pages
    - boutons `Precedent` / `Suivant` bien positionnes dans le cadre de la liste de resultats quand ils pilotent cette liste
    - bouton `Filtres` avec icone present et comportement depliable conforme au standard quand l'ecran expose des filtres
    - tout panneau secondaire ajoute a l'ecran respecte l'etat ouvert / ferme attendu par defaut
    - toute modale de calcul ou de simulation frontend retourne le meme resultat que la logique metier backend equivalente
    - les refontes n'ont pas supprime d'interaction existante critique
    - requetes sortantes et reponses entrantes observables quand la feature l'exige
    - anomalies et traitements difficiles correctement journalises
    - aucune donnee sensible exposee dans les logs
    - CSS decoupe de facon maintenable
    - aucun warning de budget Angular sur les styles de composant
    - aucune regression evidente sur les interactions
    - aucun debordement vertical parasite du document global sur un viewport desktop standard
    - filtres reactifs effectivement mis a jour sans clic supplementaire
    - si une configuration locale est modifiee, la valeur est bien relue apres sauvegarde

## Output format

Toujours terminer par :
- modules impactes
- ecrans impactes
- fichiers crees / modifies
- choix d'architecture frontend
- points de vigilance
