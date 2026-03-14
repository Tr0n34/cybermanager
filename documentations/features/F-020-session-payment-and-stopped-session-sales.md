# Feature ID: F-020

## Nom

Paiement de session arretee et ventes rattachees a la session

## Contexte

Une session arretee reste un moment critique du flux comptoir : le personnel doit pouvoir vendre un produit ou un abonnement a la session arretee, puis regler la session avec un calcul final coherent. Les erreurs sur cette zone ont un impact direct sur l'encaissement et la lisibilite des achats du client.

## Acteurs

- Employe du cybercafe

## Objectif

Permettre, apres arret d'une session, d'ajouter des ventes rattachees a cette session et de regler la session dans une modale qui recalcule immediatement le montant final.

## Bounded context

- session
- sales
- subscription
- customer
- debt

## Regles metier

- une session arretee mais non payee peut encore recevoir une vente produit rattachee a la session
- une session arretee mais non payee peut encore recevoir une vente d'abonnement rattachee a la session
- une vente rattachee a la session doit apparaitre dans le detail des achats de la session
- une vente rattachee a la session doit recalculer les montants visibles dans `A payer`
- la modale de paiement d'une session arretee peut ajouter un ou plusieurs abonnements
- lorsqu'un abonnement est ajoute pendant le paiement :
  - le prix de l'abonnement s'ajoute au montant final a payer
  - les minutes incluses couvrent d'abord le depassement eventuel
  - le temps de depassement couvert n'est plus facture comme connexion
  - le credit final du client abonne doit retirer le temps deja consomme au-dela de l'ancien credit
- une vente ou un paiement ne doit pas utiliser une liste locale obsolete d'offres ou de produits si le catalogue a ete modifie

## Backend

### Cas d'usage

- CreateProductSale
- CreateSubscriptionSale
- DeleteSubscriptionSale
- PaySession
- GetCustomerDetails

### API

- POST /api/sales/products
- POST /api/sales/subscriptions
- DELETE /api/sales/{id}/subscription-session
- POST /api/sessions/{id}/pay

### Contraintes API attendues

- les ventes rattachees a une session portent `customerId` et `sessionId`
- le paiement de session accepte `amountPaid`, `subscriptionOfferIds`, `createSubscriptionDebt`
- le backend reste la source de verite du calcul final et du credit restant

## Frontend

### Ecrans

- ecran `Sessions`
- modale `Vendre un produit`
- modale `Vendre un abonnement`
- modale `Regler la session`

### Comportements UI

- l'ouverture de la modale produit recharge le catalogue produit actif
- l'ouverture de la modale abonnement recharge les offres actives
- l'ouverture de la modale de paiement recharge les offres actives
- la modale de paiement affiche un recapitulatif de calcul :
  - credit client disponible
  - temps depasse avant ajout
  - minutes ajoutees par les abonnements selectionnes
  - temps depasse apres ajout
  - montant connexion
  - montant achats session
  - montant des abonnements a encaisser
  - total final
- le montant regle conseille est synchronise avec le total final recalcule
- si un abonnement a ete ajoute a la session avant paiement, il doit etre visible dans les achats de la session
- si un abonnement est retire avant paiement, le detail et le total doivent etre recalcules

### Vigilances d'implementation

- ne pas masquer la possibilite d'ajouter un abonnement dans la modale de paiement a cause d'une condition frontend trop restrictive
- ne pas supposer qu'un client est deja `SUBSCRIBER` pour afficher ou recalculer un ajout d'abonnement au paiement
- recharger le detail client apres vente ou suppression pour refleter immediatement les lignes rattachees a la session

## Criteres d'acceptation

- apres arret d'une session, une vente produit peut etre enregistree depuis le detail de session
- apres arret d'une session, une vente d'abonnement peut etre enregistree depuis le detail de session
- toute vente enregistree apparait ensuite dans les achats de la session
- la modale de paiement permet d'ajouter un abonnement
- le total final de la modale de paiement est recalcule immediatement apres ajout ou retrait d'abonnement
- le temps de depassement couvert par l'abonnement n'est plus facture comme connexion
- le prix de l'abonnement reste bien ajoute au montant final a regler
