# Feature ID: F-021

## Nom

Gestion des factures.

## Contexte

Le cybercafe doit pouvoir retrouver et consulter les factures associees aux ventes deja formalisees. Cette premiere livraison ouvre un ecran dedie de gestion des factures avec recherche ciblee et consultation detaillee.

## Acteurs

- Administrateur
- Utilisateur

## Objectif

Permettre la recherche et la consultation des factures par numero, client, statut et periode, depuis un ecran dedie accessible dans `Fonctions > Factures`.

## Bounded context

- customer
- reporting
- sales

## Regles metier

- une facture est rattachee a une vente finalisee
- une facture possede un numero unique
- une facture conserve le detail de ses lignes et son total
- le detail d'une facture est consultable sans repasser par la vente d'origine
- cette premiere livraison couvre la recherche et la consultation, pas encore la creation manuelle, la reedition ni l'annulation

## Backend

### Cas d'usage
- UC-1 Rechercher des factures par numero, client, statut ou periode
- UC-2 Consulter le detail d'une facture

### Domain
- agregat `Invoice`
- entite `InvoiceLine`
- value object `InvoiceStatus`

### Application
- `SearchInvoicesUseCase`
- `GetInvoiceDetailUseCase`
- `InvoiceSummaryView`
- `InvoiceDetailView`
- `InvoiceLineView`

### Infrastructure
- persistance JPA `cm_invoices` et `cm_invoice_lines`
- adapter repository facture
- indexes sur numero, client, statut et date d'emission

### API
- `GET /api/invoices`
- `GET /api/invoices/{invoiceId}`

## Frontend

### Ecrans
- ecran liste des factures
- detail de facture dans le panneau lateral

### Composants
- tableau des factures
- panneau detail facture
- filtres par numero, client, statut, periode, factures par page
- actions de consultation

### Appels API
- `GET /api/invoices`
- `GET /api/invoices/{invoiceId}`

### Navigation et comportements UI
- l'ecran est accessible depuis `Fonctions > Factures`
- la liste se charge automatiquement a l'ouverture
- les filtres suivent le standard frontend avec bouton `Filtres` et bloc repliable
- la pagination `Precedent` / `Suivant` est dans le cadre `Resultats`
- le clic sur une ligne ouvre le detail dans le panneau lateral
- les erreurs visibles cote utilisateur sont explicites
- la recherche privilegie un chargement cible par numero, client, statut ou periode plutot qu'une liste exhaustive non filtree

## Criteres d'acceptation

- l'utilisateur peut ouvrir l'ecran `Factures` depuis le menu `Fonctions`
- l'utilisateur peut filtrer les factures par numero, client, statut et periode
- l'utilisateur peut paginer les resultats
- l'utilisateur peut consulter le detail complet d'une facture depuis la liste
- l'ecran compile et consomme l'API facture dediee
