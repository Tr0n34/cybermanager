# F-020 - Archivage clients

## Objectif

Permettre d'archiver des clients au format CSV ou XLSX a partir d'une periode de derniere activite et d'un type de client, puis de les supprimer de la base avec leurs donnees associees.

## Perimetre

- ecran `Archivage`
- filtre par date de debut, date de fin, type de client et clients par page
- previsualisation des clients archivables
- export CSV
- export XLSX
- rapport PDF des dettes
- suppression des clients archives et de leurs sessions, ventes et dettes

## Regles fonctionnelles

- la periode filtre la `derniere activite` du client
- la derniere activite d'un client est le maximum entre :
  - la date de vente la plus recente
  - la date de dette la plus recente
  - la date de fin de session la plus recente, ou la date de debut si la session n'est pas terminee
- un client n'est archivable que si sa derniere activite est dans la periode selectionnee
- un client avec session encore active et non payee n'est pas archivable
- un client avec dette ouverte n'est pas archivable
- un client avec credit temps restant n'est pas archivable
- un abonne n'est jamais archivable
- l'archivage genere un fichier CSV ou XLSX avant suppression
- la suppression retire :
  - les dettes du client
  - les ventes du client
  - les sessions du client
  - le client lui-meme
- le rapport PDF des dettes liste les dettes ouvertes de la periode avec le detail par client

## Donnees exportees

Chaque ligne CSV/XLSX contient :
- l'identite du client
- le type et le statut
- le credit restant
- la periode d'archivage
- la derniere activite
- le nombre de sessions, ventes et dettes
- les montants cumules de ventes et dettes
- un resume texte des sessions, ventes et dettes

## API

- `GET /api/customers/archive/candidates`
  - params : `startDate`, `endDate`, `type`
  - retourne la previsualisation des clients archivables
- `POST /api/customers/archive/export`
  - body : `startDate`, `endDate`, `type`, `format`
  - retourne un fichier CSV ou XLSX et supprime les clients archives dans la meme transaction
- `GET /api/customers/archive/debts-report`
  - params : `startDate`, `endDate`, `type`
  - retourne un PDF detaille des dettes ouvertes sur la periode

## Frontend

- nouvelle feature Angular `archiving`
- page dediee accessible depuis le menu principal
- affichage d'un tableau de previsualisation avant l'archivage
- filtres dans un bloc repliable pilote par le bouton `Filtres` avec icone
- pagination `Precedent` / `Suivant` dans le cadre `Resultats`
- telechargement CSV, XLSX et PDF via le navigateur
- affichage d'un cadre listant les fichiers generes avec date et utilisateur

## Performance et base de donnees

Des index JPA sont poses pour les recherches utilisees par l'archivage :
- clients par `type` et `name`
- ventes par `customerId` et `soldAt`
- sessions par `customerId`, `startedAt`, `endedAt`
- dettes par `customerId`, `createdAt`, `status`

## Points de vigilance

- l'archivage est destructif pour les donnees en base
- la periode doit etre choisie avec prudence
- le cas principal vise les clients non abonnes sans dette ni credit temps a sortir du systeme apres export
