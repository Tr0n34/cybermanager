# Feature ID: F-003

## Nom

Gestion des produits vendus par le cybercafé

## Contexte

Le cybercafé vend des produits aux clients, par exemple des boissons, snacks, accessoires ou services complémentaires.
L’application doit permettre de gérer ce catalogue afin de faciliter les ventes.

## Acteurs

- Administrateur
- Employé du cybercafé

## Objectif

Permettre la création, la consultation, la modification, l’activation et la désactivation des produits vendus.

## Bounded context

- catalog

## Règles métier

- un produit possède un nom
- un produit possède un prix de vente
- un produit peut être actif ou inactif
- un produit peut appartenir à une catégorie
- un produit inactif ne peut plus être vendu
- l’historique des ventes doit rester cohérent même si le produit est modifié plus tard

## Backend

### Cas d’usage
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
- ProductSummaryDto
- ProductDetailsDto

### Infrastructure
- ProductJpaEntity
- ProductJpaRepository

### API
- GET /api/products
- GET /api/products/{id}
- POST /api/products
- PUT /api/products/{id}
- PUT /api/products/{id}/activate
- PUT /api/products/{id}/deactivate

## Frontend

### Écrans
- liste des produits
- détail produit
- création produit
- édition produit

### Composants
- products-table
- product-form
- product-status-badge
- product-category-filter

## Critères d’acceptation

- un employé peut consulter le catalogue
- un administrateur peut créer et modifier un produit
- un produit inactif n’apparaît plus dans les ventes
- la liste permet de filtrer les produits actifs et inactifs