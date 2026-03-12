# Feature ID: F-012

## Nom

Gestion des dettes clients

## Contexte

Un client journalier peut quitter ou terminer une session avec un montant de connexion non encore regle. Le cybercafe doit pouvoir suivre ces montants dus, les afficher dans les ecrans operatoires et les marquer comme regles ulterieurement.

## Acteurs

- Employe du cybercafe
- Administrateur

## Objectif

Permettre la creation, la consultation et le reglement des dettes ouvertes d'un client, avec une visibilite immediate dans les ecrans `Dettes`, `Sessions` et `Clients`.

## Bounded context

- customer
- session
- sales

## Regles metier

- une dette est rattachee a un client
- une dette possede un libelle, un montant, une date de creation et un statut
- une dette ouverte apparait comme `OPEN`
- une dette reglee apparait comme `SETTLED`
- l'arret d'une session client journalier avec un montant a facturer cree une dette ouverte si ce montant n'est pas regle dans le flux session
- toute vente de produit, d'abonnement ou de temps peut aussi creer une dette ouverte si l'utilisateur choisit le mode dette
- les dettes ouvertes doivent etre visibles en rouge dans les ecrans operatoires
- un client peut posseder plusieurs dettes ouvertes
- le reglement d'une dette ne doit pas supprimer son historique
- l'affichage operationnel d'une dette doit rester compact et presenter date / heure au format `JJ/MM/AAAA HH:mm`

## Backend

### Cas d'usage
- SearchOpenDebts
- SettleDebt
- GetCustomerDetails
- StopSession
- CreateProductSale
- CreateSubscriptionSale
- CreateConnectionTimeSale

### Domain
- DebtRecord
- DebtId
- DebtStatus

### Application
- SearchOpenDebtsUseCase
- SettleDebtUseCase
- CustomerDebtView
- CustomerDebtSummaryView

### Infrastructure
- DebtJpaEntity
- DebtJpaRepository
- DebtRepositoryAdapter

### API
- GET /api/customers/debts
- POST /api/customers/debts/{debtId}/settle
- GET /api/customers/{id}
- POST /api/sales/products
- POST /api/sales/subscriptions
- POST /api/sales/connection-time

### Parametres et comportements API
- `GET /api/customers/debts` retourne uniquement les dettes ouvertes groupees par client
- `POST /api/customers/debts/{debtId}/settle` marque la dette comme reglee et renvoie l'etat courant
- `GET /api/customers/{id}` retourne aussi les dettes du client dans le detail
- les endpoints de vente acceptent `createDebt` pour enregistrer la vente et la dette associee dans le meme flux

## Frontend

### Ecrans
- ecran `Dettes`
- detail client dans l'ecran clients
- detail developpable d'une session en cours
- detail developpable d'une session du jour

### Composants
- debts-page
- debt-summary-card
- customer-debt-lines
- session-debt-lines

### Navigation et comportements UI
- le menu principal expose une entree `Dettes`
- l'ecran `Dettes` affiche les clients ayant des dettes ouvertes et le sous-detail de chaque dette
- chaque dette peut etre marquee comme reglee depuis l'ecran `Dettes`
- dans l'ecran `Sessions`, le detail d'une session affiche les dettes du client dans la chronologie compacte des achats du jour
- dans l'ecran `Clients`, l'ouverture du detail affiche les dettes du client en rouge
- les formulaires de vente permettent de cocher la creation d'une dette au lieu d'un encaissement immediat
- dans les resumes de detail `Clients` et `Sessions`, la presence d'une dette est signalee par une icone rouge de billets non payes
- lorsqu'une dette correspond a une vente du jour, l'interface privilegie une ligne unique de type `Produit 12/03/2026 14:12 1.50 EUR` plutot qu'un libelle technique `Vente produits du ...`
- les erreurs de chargement ou de reglement restent visibles dans l'ecran

## Criteres d'acceptation

- un employe peut consulter les clients ayant des dettes ouvertes
- le detail d'une dette affiche son libelle, son montant et sa date
- une dette peut etre marquee comme reglee
- les dettes non reglees apparaissent en rouge dans les ecrans `Dettes`, `Sessions` et `Clients`
- l'arret d'une session client journalier cree une dette si un montant reste du
- une vente peut creer immediatement une dette ouverte sans quitter le flux de vente
