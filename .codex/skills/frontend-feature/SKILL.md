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
- en priorite `F-019-frontend-crud-filter-and-panel-standardization.md` pour les standards UI transverses
- en priorite `F-020-session-payment-and-stopped-session-sales.md` pour toute demande sur les modales session/paiement/ventes rattachees
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
    - pour les pages CRUD de liste, standardiser les filtres dans un bloc `filters-grid collapsible` pilote par un bouton `Filtrer` ou `Filtres` avec icone
    - utiliser une ouverture / fermeture progressive du bloc de filtres, avec animation legere, plus smooth que brutale, plutot qu'un rendu abrupt
    - les filtres CRUD doivent privilegier des champs compacts, alignes a gauche et dimensionnes selon le contenu attendu plutot qu'etires inutilement
    - un bloc `filters-grid` ne doit jamais s'etirer plus que le cadre auquel il appartient: sa largeur doit suivre la largeur utile du panneau associe et rester compacte
    - le bouton `Filtres` doit rester dans le bandeau d'actions sous le titre de page, pas perdu plus bas dans l'ecran
    - pour le dashboard/monitoring, le bouton `Filtrer` se place sous `Clients du jour`, pas dans le hero principal
    - pour le dashboard/monitoring, le bloc de filtres contient a minima `Recherche client`, `Etat`, `Dette`, `Par page`
    - placer l'action de creation principale a cote du bouton `Filtres` (`Creer un nouvel ...`) plutot qu'au fond d'un panneau secondaire
    - eviter de dupliquer un bouton `Nouveau` dans la colonne de droite si l'action de creation principale existe deja en haut de page
    - pagination et taille de page configurable quand le volume peut croitre
    - si une pagination existe, utiliser explicitement `Precedent` / `Suivant` avec indication `Page X / Y`
    - quand la pagination existe, la navigation `Precedent` / `Suivant` doit partager la meme ligne d'action que le bouton `Filtres`
    - les filtres de pagination (`Clients par page`, `Dettes par page`, etc.) doivent vivre dans le bloc `filters-grid`, pas dans le bandeau d'actions
    - panneaux lateraux ou accordions si le flux comptoir le demande
    - un detail en colonne de droite doit etre encadre, distinct de la liste, et rappeler les informations synthetiques du client
    - si un detail client existe dans une colonne de droite, prevoir le bouton principal d'ouverture et, quand cela a du sens metier, un glisser-deposer en complement
    - un panneau secondaire ouvert / ferme par bouton doit etre ferme par defaut si cela reduit la charge visuelle initiale
    - une modale de simulation ou de calcul frontend doit reprendre la logique metier backend existante si elle sert a previsualiser un resultat utilisateur
    - toute modale session de vente produit, vente abonnement ou paiement doit relire a l'ouverture les produits / offres actifs si ces donnees peuvent avoir ete modifiees depuis un autre ecran
    - une modale de paiement de session doit afficher un detail de calcul lisible et suivre strictement la logique backend sur le depassement, les abonnements ajoutes et le total final
    - si un abonnement est ajoute pendant le paiement, compter son prix dans le total final, couvrir d'abord le depassement avec ses minutes, puis ne plus facturer le depassement couvert
    - apres une vente ou suppression rattachee a une session, relire le detail client et les montants visibles de session
    - pour les formulaires reactifs, ne pas baser un `computed` sur `form.getRawValue()` si l'on attend une reaction immediate
    - utiliser `signals`, `valueChanges` ou un state local explicite pour les filtres instantanes
    - les filtres de recherche operationnels doivent reagir des les premieres lettres
    - une page de configuration doit normaliser les valeurs saisies avant sauvegarde et afficher un retour utilisateur apres enregistrement
    - journaliser les traitements UI difficiles : transformation de donnees, synchronisation d'etat, normalisation, calculs derives
    - privilegier des logs centralises et coherents plutot que des `console.log` disperses
    - garder les logs utiles au diagnostic sans surcharger les parcours simples

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
    - filtres de liste coherents avec les autres CRUD frontend : bouton `Filtrer`/`Filtres`, panneau depliable, animation legere, action de creation visible
    - champs de filtre compactes et correctement calibres pour leur contenu
    - bloc de filtres calibre a la largeur utile du panneau associe, sans effet de grand bandeau vide
    - pagination coherente avec le standard frontend si le volume justifie plusieurs pages
    - boutons `Precedent` / `Suivant` bien positionnes sur la meme ligne d'action que `Filtres`
    - tout panneau secondaire ajoute a l'ecran respecte l'etat ouvert / ferme attendu par defaut
    - tout detail de monitoring ou de fiche a droite rappelle bien les informations client essentielles
    - tout glisser-deposer ajoute reste optionnel et ne remplace jamais l'action principale
    - toute modale de calcul ou de simulation frontend retourne le meme resultat que la logique metier backend equivalente
    - les modales de session relisent bien les produits / offres avant soumission si le flux depend d'un catalogue mutable
    - requetes sortantes et reponses entrantes observables quand la feature l'exige
    - anomalies et traitements difficiles correctement journalises
    - aucune donnee sensible exposee dans les logs
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
