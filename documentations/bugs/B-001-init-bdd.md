# Bug ID: B-001

## Nom

Initialisation BDD sensible aux retours a la ligne Windows

## Contexte

Les scripts d'initialisation PostgreSQL peuvent etre modifies ou decompactes depuis Windows. Si un fichier `*.sh` ou `*.sql` est en `CRLF`, l'execution dans un conteneur Linux peut echouer, notamment sur le shebang `#!/bin/sh` ou sur certaines variables injectees.

## Symptomes

- erreur de type `bad interpreter: /bin/sh^M`
- script `initdb` non execute au demarrage du conteneur PostgreSQL
- erreurs SQL ou shell liees a des caracteres `\r`

## Cause

Le depot ne forcait pas les fins de ligne Unix pour les scripts d'initialisation. Sous Windows, un checkout ou une edition pouvait reintroduire des retours `CRLF`.

## Correctif applique

- ajout d'un fichier [.gitattributes](/D:/DATA/cybermanager/.gitattributes) pour forcer `LF` sur `*.sh` et `*.sql`
- durcissement de [00-init-database.sh](/D:/DATA/cybermanager/configuration/cybermanager/initdb/00-init-database.sh) pour nettoyer d'eventuels `\r` presents dans `POSTGRES_USER` et `POSTGRES_DB`

## Prevention

- conserver les scripts shell et SQL en `LF`
- eviter les conversions automatiques de fin de ligne sur les dossiers de configuration docker et init DB
- si le depot a deja ete checkout avec de mauvais EOL, refaire un checkout propre ou reconvertir les fichiers concernes en `LF`

## Verification

- ouvrir le fichier `00-init-database.sh` et verifier qu'il est en `LF`
- reconstruire / relancer le service PostgreSQL
- verifier que le schema `cybermanager` et les extensions sont bien crees
