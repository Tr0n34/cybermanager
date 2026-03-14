# Feature ID: F-019

## Nom

Standardisation frontend des filtres, panneaux lateraux et interactions de detail

## Contexte

Plusieurs ecrans frontend du projet suivent maintenant un meme langage d'interface : tableaux compacts, panneau de filtres repliable, pagination simple, panneau de detail a droite, modales operationnelles. Cette base doit etre documentee pour eviter les regressions visuelles et comportementales.

## Acteurs

- Employe du cybercafe
- Administrateur

## Objectif

Definir un standard frontend reutilisable pour les ecrans CRUD et les ecrans operationnels complexes, en particulier le dashboard/monitoring et les pages liste + panneau lateral.

## Bounded context

- frontend shared
- monitoring
- users
- subscription
- customer
- sales
- session

## Regles fonctionnelles

- un bouton `Filtrer` avec icone doit piloter l'ouverture et la fermeture du bloc de filtres
- le bloc de filtres doit contenir les champs de recherche, d'etat, de pagination et autres criteres de liste
- les champs de filtre doivent etre compacts, alignes a gauche et dimensionnes selon leur contenu attendu
- les boutons de pagination utilisent explicitement `Precedent` et `Suivant`
- un panneau secondaire ou panneau de detail doit etre clairement encadre et visuellement distinct de la liste principale
- un detail de fiche ou de monitoring doit rappeler les informations essentielles du client, pas uniquement l'historique
- lorsqu'un detail client existe dans une colonne de droite, l'ouverture doit etre possible par bouton explicite et, si pertinent, par glisser-deposer
- les modales operationnelles doivent recharger les donnees de reference critiques a l'ouverture si elles peuvent avoir change pendant la journee

## Frontend

### Standards UI

- pour les CRUD liste + edition, le bouton `Filtrer` reste dans la zone d'action principale
- pour le dashboard/monitoring, le bouton `Filtrer` est place sous `Clients du jour`
- le bloc de filtres du dashboard contient `Recherche client`, `Etat`, `Dette`, `Par page`
- le detail du dashboard doit etre rendu dans un cadre dedie a droite
- le detail du dashboard rappelle au minimum `Client`, `Etat`, `Type`, `Credit`, `Consomme`, `Achats`, `Encaisse`, `Dette`
- le dashboard permet d'ouvrir le detail soit avec `Detail`, soit par glisser-deposer d'un client dans le cadre de droite
- les etats vides doivent indiquer clairement l'action attendue, par exemple `Clique sur Detail ou glisse-depose un client ici`

### Comportements techniques

- le bloc de filtres utilise le pattern `filters-grid collapsible`
- la fermeture du bloc de filtres ne doit pas casser la mise en page ni laisser des espaces morts incoherents
- les details a droite utilisent un cadre dedie avec fond et bordure differencies
- les interactions de drag and drop restent un complement et non un remplacement du bouton d'action principal
- les listes operationnelles gardent un tableau compact et scrollable

## Criteres d'acceptation

- un utilisateur peut afficher ou masquer les filtres sans perdre l'acces aux autres actions
- les filtres visibles sont bien ceux attendus pour la liste concernee
- le detail a droite est clairement encadre et lisible
- les informations synthetiques du client sont visibles dans le detail
- le dashboard accepte l'ouverture d'un detail par bouton `Detail`
- le dashboard accepte aussi l'ouverture d'un detail par glisser-deposer dans le cadre de droite

