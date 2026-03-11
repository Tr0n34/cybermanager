# CyberManager PostgreSQL

Configuration PostgreSQL locale pour CyberManager.

## Contenu

- `Dockerfile` : image PostgreSQL 16 personnalisee
- `docker-compose.yml` : lancement local
- `postgresql.conf` : configuration serveur
- `pg_hba.conf` : regles d'authentification
- `initdb/*.sql` : initialisation de la base
- `.env.example` : variables a reutiliser cote backend

## Demarrage

```bash
docker compose up -d --build
```

## Connexion backend

Les APIs Spring Boot sont deja compatibles avec les variables suivantes :

- `CM_DB_URL=jdbc:postgresql://localhost:5432/cybermanager`
- `CM_DB_USERNAME=cybermanager`
- `CM_DB_PASSWORD=cybermanager`

Sans variables d'environnement, les modules `cm-api` et `cm-auth-api` utilisent deja ces memes valeurs par defaut.

## Notes

- le schema applicatif par defaut est `cybermanager`
- les tables sont creees par Hibernate/JPA au demarrage des APIs
- les scripts `initdb` sont executes uniquement lors de l'initialisation d'un volume vide
