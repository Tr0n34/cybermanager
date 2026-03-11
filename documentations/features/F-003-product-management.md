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

## Bounded context

- catalog

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
- SearchProducts
- GetProductDetails

### Domain
- Product
- ProductId
- ProductName
- ProductPrice
- ProductCategory
- ProductStatus

### Application
- CreateProductUseCase
- UpdateProductUseCase
- SearchProductsUseCase
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
- PUT /api/products/{id}/activate
- PUT /api/products/{id}/deactivate

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
- les filtres nom, categorie et statut rechargent la liste
- le bouton "nouveau produit" ouvre un formulaire vide
- le clic sur une ligne ou une action d'edition ouvre le formulaire en mode modification
- apres creation, modification, activation ou desactivation, la liste est rechargee
- les erreurs de chargement ou de sauvegarde sont visibles dans l'ecran

## Criteres d'acceptation

- un employe peut consulter le catalogue
- un administrateur peut creer et modifier un produit
- un produit inactif n'apparait plus dans les ventes
- la liste permet de filtrer les produits actifs et inactifs
- la creation produit est accessible depuis l'ecran liste sans navigation cassee
