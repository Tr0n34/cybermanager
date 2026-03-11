# Feature ID: F-005

## Nom

Gestion des clients specifiques

## Contexte

Le cybercafe doit disposer d'un ecran client permettant de retrouver les clients connus du systeme, de creer des clients specifiques et des clients abonnes, et de consulter leur situation commerciale.

## Acteurs

- Employe du cybercafe

## Objectif

Permettre la consultation, la recherche, la creation et la mise a jour des clients specifiques et abonnes, sans obliger l'utilisateur a passer par le flux session.

## Bounded context

- customer
- sales
- subscription

## Regles metier

- un client journalier est cree naturellement depuis le flux session
- un client specifique peut etre cree directement depuis l'ecran client
- un client abonne peut etre cree directement depuis l'ecran client
- la creation d'un client abonne doit etre associee a une offre d'abonnement initiale
- la fiche client doit afficher l'abonnement en cours si applicable
- la fiche client doit afficher l'historique des achats du client

## Backend

### Cas d'usage
- CreateCustomer
- SearchCustomers
- GetCustomerDetails
- UpdateCustomer

### Domain
- Customer
- CustomerId
- CustomerType
- CustomerName
- CustomerStatus

### Application
- CreateCustomerUseCase
- SearchCustomersUseCase
- GetCustomerDetailsUseCase
- UpdateCustomerUseCase
- CustomerView
- CustomerDetailsView

### Infrastructure
- CustomerJpaEntity
- CustomerJpaRepository
- CustomerRepositoryAdapter
- SaleRepositoryAdapter

### API
- GET /api/customers
- GET /api/customers/{id}
- POST /api/customers
- PUT /api/customers/{id}

## Frontend

### Ecrans
- liste des clients
- creation d'un client specifique
- creation d'un client abonne
- fiche client detaillee
- navigation depuis le shell principal vers l'ecran clients

### Composants
- customers-table
- customer-search-box
- customer-create-form
- customer-details-panel
- customer-purchases-table

### Navigation et comportements UI
- l'ecran charge les clients connus du systeme
- toutes les zones de recherche possedent un label explicite
- la recherche client permet de retrouver un client specifique ou un abonne
- la creation permet de choisir entre client specifique et client abonne
- si le type choisi est abonne, une offre d'abonnement doit etre selectionnee
- l'ouverture d'une fiche client affiche l'abonnement en cours et l'historique des achats

## Criteres d'acceptation

- un employe peut creer un client specifique depuis l'ecran client
- un employe peut creer un client abonne depuis l'ecran client
- la fiche client affiche l'abonnement en cours
- la fiche client affiche les achats de produits, d'abonnements et autres ventes
