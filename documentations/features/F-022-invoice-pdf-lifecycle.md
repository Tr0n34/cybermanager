# Feature ID: F-022

## Nom

Cycle de vie des factures PDF.

## Contexte

La gestion initiale des factures permet deja de rechercher et consulter les factures existantes. Le cybercafe doit maintenant pouvoir produire une facture PDF au moment du reglement d'une session, creer une facture depuis l'ecran `Factures`, annuler une facture et la reediter a tout moment.

## Acteurs

- Administrateur
- Utilisateur

## Objectif

Permettre la production et la gestion complete des factures PDF depuis `Sessions` et `Factures`.

## Bounded context

- sales
- session
- company

## Regles metier

- une facture est emise au format PDF
- une facture peut etre creee depuis la modale de paiement d'une session
- la creation de facture depuis la modale de paiement doit etre atomique avec le reglement de la session
- une facture peut aussi etre creee manuellement depuis l'ecran `Factures`
- une facture annulee reste conservee en base mais passe au statut `CANCELLED`
- une facture annulee peut toujours etre reeditee en PDF, avec son statut visible
- la reedition d'une facture ne modifie pas son contenu
- le PDF doit reprendre les informations de l'entreprise si elles existent

## Backend

### Cas d'usage
- UC-1 Payer une session et generer une facture PDF
- UC-2 Creer une facture manuelle PDF
- UC-3 Annuler une facture
- UC-4 Reediter une facture PDF
- UC-5 Rechercher et consulter les factures

### Domain
- agregat `Invoice`
- entite `InvoiceLine`
- value object `InvoiceStatus`

### Application
- `CreateManualInvoiceUseCase`
- `CancelInvoiceUseCase`
- `ReissueInvoicePdfUseCase`
- `PaySessionAndCreateInvoiceUseCase`
- service de rendu PDF facture

### Infrastructure
- persistance JPA des factures et lignes
- indexes sur numero, statut, client et date d'emission

### API
- `POST /api/invoices`
- `POST /api/invoices/{invoiceId}/cancel`
- `GET /api/invoices/{invoiceId}/pdf`
- `POST /api/sessions/{sessionId}/pay-with-invoice`
- `GET /api/invoices`
- `GET /api/invoices/{invoiceId}`

## Frontend

### Ecrans
- modale de paiement `Sessions`
- ecran `Factures`

### Composants
- action `Valider et generer la facture PDF` dans la modale de paiement
- panneau de creation manuelle de facture
- detail facture avec actions `Reediter PDF` et `Annuler`

### Appels API
- `POST /api/sessions/{sessionId}/pay-with-invoice`
- `POST /api/invoices`
- `POST /api/invoices/{invoiceId}/cancel`
- `GET /api/invoices/{invoiceId}/pdf`
- `GET /api/invoices`
- `GET /api/invoices/{invoiceId}`

### Navigation et comportements UI
- depuis `Sessions`, l'utilisateur peut regler et telecharger la facture PDF dans la meme action
- depuis `Factures`, l'utilisateur peut ouvrir le panneau de creation manuelle avec le bouton d'action principal
- l'ecran `Factures` conserve le standard frontend : bouton `Filtres`, bloc repliable, pagination dans le cadre `Resultats`
- le detail d'une facture affiche son statut et ses lignes
- l'annulation met a jour le detail et la liste sans supprimer la facture
- la reedition telecharge de nouveau le PDF de la facture selectionnee

## Criteres d'acceptation

- l'utilisateur peut regler une session et obtenir la facture PDF depuis la modale de paiement
- l'utilisateur peut creer une facture manuelle depuis `Factures`
- l'utilisateur peut annuler une facture depuis `Factures`
- l'utilisateur peut reediter une facture PDF depuis `Factures`
- les factures apparaissent dans la liste avec leur statut a jour
