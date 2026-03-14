# Feature ID: F-023

## Nom

Gestion de l'entreprise.

## Contexte

Les factures PDF doivent afficher les informations legales et commerciales de l'entreprise. L'application doit donc permettre de creer l'entreprise si elle n'existe pas encore, puis de la mettre a jour depuis un ecran dedie.

## Acteurs

- Administrateur
- Utilisateur

## Objectif

Permettre la creation et l'edition des informations d'entreprise depuis `Fonctions > Entreprise`.

## Bounded context

- company

## Regles metier

- l'application gere un profil entreprise actif unique
- si aucun profil entreprise n'existe, l'ecran doit permettre sa creation
- si le profil existe, l'ecran permet son edition
- les informations d'entreprise sont reutilisables par les autres features, notamment les factures PDF
- les informations saisies doivent etre normalisees avant sauvegarde

## Backend

### Cas d'usage
- UC-1 Consulter l'entreprise courante
- UC-2 Creer l'entreprise si elle n'existe pas
- UC-3 Mettre a jour l'entreprise existante

### Domain
- agregat `CompanyProfile`

### Application
- `GetCompanyProfileUseCase`
- `UpsertCompanyProfileUseCase`

### Infrastructure
- persistance JPA du profil entreprise

### API
- `GET /api/company`
- `PUT /api/company`

## Frontend

### Ecrans
- ecran `Entreprise`

### Composants
- formulaire entreprise
- retour de sauvegarde

### Appels API
- `GET /api/company`
- `PUT /api/company`

### Navigation et comportements UI
- l'ecran est accessible depuis `Fonctions > Entreprise`
- l'ecran charge l'entreprise a l'ouverture
- si aucune entreprise n'existe, le formulaire s'ouvre en mode creation
- si l'entreprise existe, le formulaire s'ouvre en mode edition
- les valeurs sont relues apres sauvegarde

## Criteres d'acceptation

- l'utilisateur peut ouvrir `Entreprise` depuis le menu `Fonctions`
- l'utilisateur peut creer l'entreprise si elle n'existe pas
- l'utilisateur peut modifier l'entreprise si elle existe
- les informations d'entreprise sont disponibles pour les factures PDF
