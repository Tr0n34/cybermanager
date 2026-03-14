# Skill: database-feature

## Purpose

Implementer ou faire evoluer la couche base de donnees de CyberManager en respectant PostgreSQL, la separation DDD et les conventions projet.

## When to use

Utiliser cette skill lorsqu'une demande concerne :
- une nouvelle configuration PostgreSQL
- un Dockerfile ou un compose de base de donnees
- des scripts d'initialisation ou de migration
- un schema, des index, contraintes ou roles SQL
- des besoins de seed techniques ou de reference data
- une optimisation de performance sur requetes de recherche, filtrage, archivage ou purge

## Required inputs

- AGENTS.md
- le bounded context concerne
- la feature ou le besoin technique
- les variables d'environnement attendues par le backend

## Rules

- la base doit rester une implementation technique, sans logique metier centrale deplacee depuis le domain
- les objets SQL doivent etre nommes de facon lisible et stable
- privilegier PostgreSQL
- toute configuration locale doit etre reproductible par Docker
- documenter les variables d'environnement et le mode de demarrage
- ne pas casser la compatibilite avec les modules backend existants
- si JPA cree les tables, les scripts SQL doivent se limiter aux extensions, schemas, roles, seed techniques et pre-requis serveur
- toute nouvelle recherche metier potentiellement volumique doit etre accompagnee d'une verification d'index
- privilegier les filtrages en base plutot qu'un `findAll()` suivi d'un filtrage en memoire
- pour les ecrans d'archivage, de reporting ou de purge :
  - identifier les colonnes de filtre exactes
  - indexer les colonnes de filtre et de tri les plus frequentes
  - verifier les suppressions batch et l'ordre de suppression des dependances
  - preferer des operations atomiques et transactionnelles

## Steps

1. Identifier le besoin :
   - configuration serveur
   - initialisation
   - structure SQL
   - droits
   - performance
   - volumetrie et pattern de lecture/ecriture

2. Definir l'arborescence de configuration :
   - `configuration/<projet>/Dockerfile`
   - `configuration/<projet>/docker-compose.yml` si utile
   - `configuration/<projet>/initdb/*.sql`
   - `configuration/<projet>/*.conf`

3. Aligner les variables avec le backend :
   - URL JDBC
   - utilisateur
   - mot de passe
   - nom de base

4. Ecrire les scripts SQL :
   - extensions
   - schema
   - roles si necessaire
   - donnees techniques minimales
   - index et contraintes relies aux nouveaux filtres si necessaire

5. Documenter :
   - lancement
   - prerequis
   - points de vigilance
   - impact performance attendu
   - hypotheses de volumetrie si elles pilotent le choix d'index

6. Verifier :
   - coherence avec AGENTS.md
   - pas de logique metier lourde en SQL
   - demarrage reproductible
   - securite minimale sur l'authentification
   - coherence des index avec les requetes du code
   - absence de suppression partielle sur les workflows d'archivage

## Output format

Toujours fournir :
- objectif de la configuration BDD
- fichiers crees / modifies
- variables d'environnement
- commandes de lancement
- index ou optimisations ajoutes
- points de vigilance
