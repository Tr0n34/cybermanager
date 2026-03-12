# Feature ID: F-003

## Nom

Gestion des produits vendus par le cybercafe

## Contexte

Le cybercafe vend des produits aux clients, par exemple des boissons, snacks, accessoires ou services complementaires.
L'application doit permettre de gerer ce catalogue afin de faciliter les ventes.

## Acteurs

- Administrateur
- Employe du cybercafe

## Objectif

Permettre la creation, la consultation, la modification, l'activation et la desactivation des produits vendus.

## Ameliorations integrees

- ajout du service applicatif catalogue couvrant recherche, detail, creation, modification et changements de statut
- exposition d'un endpoint de recherche avec filtres `term`, `status` et `category`
- ajout d'une page Angular de gestion du catalogue avec service API dedie
- conservation d'un modele de produit simple oriente vente : nom, prix, categorie, statut
- ajout de la suppression produit
- refonte de l'ecran en mode liste compacte + panneau lateral refermable
- filtres instantanes par nom, categorie et statut avec pagination et taille de page configurable

## Bounded context

- product

## Regles metier

- un produit possede un nom
- un produit possede un prix de vente
- un produit peut etre actif ou inactif
- un produit peut appartenir a une categorie
- un produit inactif ne peut plus etre vendu
- l'historique des ventes doit rester coherent meme si le produit est modifie plus tard

## Backend

### Cas d'usage
- CreateProduct
- UpdateProduct
- ActivateProduct
- DeactivateProduct
- DeleteProduct
- SearchProducts
- GetProductDetails

### Domain
- Product
- ProductId
- ProductStatus
- Money

### Application
- CreateProductUseCase
- UpdateProductUseCase
- ActivateProductUseCase
- DeactivateProductUseCase
- SearchProductsUseCase
- GetProductDetailsUseCase
- ProductView

### Infrastructure
- ProductJpaEntity
- ProductJpaRepository
- ProductRepositoryAdapter

### API
- GET /api/products
- GET /api/products/{id}
- POST /api/products
- PUT /api/products/{id}
- DELETE /api/products/{id}
- PUT /api/products/{id}/activate
- PUT /api/products/{id}/deactivate

### Parametres et comportements API
- `GET /api/products` accepte `term`, `status` et `category`
- les lectures sont accessibles sans header d'authentification dans l'etat actuel
- les operations d'ecriture exigent un bearer token et retournent le produit mis a jour
- les erreurs metier exposees par les routes d'ecriture suivent un contrat stable exploitable par le frontend

## Frontend

### Ecrans
- liste des produits
- detail produit
- creation produit
- edition produit
- navigation depuis le shell principal vers l'ecran catalogue

### Composants
- products-table
- product-form
- product-status-badge
- product-category-filter

### Navigation et comportements UI
- la liste se charge a l'ouverture de l'ecran
- les filtres nom, categorie et statut filtrent la liste immediatement a chaque saisie
- la taille de page est configurable et la pagination reste dans l'ecran
- le tableau catalogue reste compact avec badges de statut visuellement plus lisibles
- le bouton "nouveau produit" ouvre un panneau de droite vide avec animation puis disparait tant que le panneau reste ouvert
- le clic sur une ligne ou une action d'edition ouvre le panneau en mode modification
- apres creation, modification, activation, desactivation ou suppression, la liste est rechargee et le panneau peut se refermer
- les erreurs de chargement ou de sauvegarde sont visibles dans l'ecran
- la page combine le tableau catalogue et le formulaire produit dans le meme flux utilisateur
- le badge de statut actif / inactif doit rester visuellement distinct et lisible
- les dates et heures visibles dans l'ecran suivent le format `JJ/MM/AAAA HH:mm`

## Criteres d'acceptation

- un employe peut consulter le catalogue
- un administrateur peut creer et modifier un produit
- un produit inactif n'apparait plus dans les ventes
- la liste permet de filtrer les produits actifs et inactifs
- la creation produit est accessible depuis l'ecran liste sans navigation cassee
- un produit peut etre supprime depuis l'ecran d'edition
