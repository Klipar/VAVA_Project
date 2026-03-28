# Rabbit
![rabbit](https://i.redd.it/tddknk8yc98g1.jpeg)

Create a `.env` file in this folder. Copy all data from `.env.example`.
If needed, you can change it, but it should already work.

## commands to start project:
### Run `Database`
for running database you need `docket` and `docker-compose`. Install them and using docker-compose run containers.

### Migrations
This project use its own migrations system.
you can add migration in next format:
``` text
V{{migration_number}}__{{comment_for_migration}}.sql
```
All migrations check automatically before starting server.
But you must run server via `mvn clean compile exec:java` to actualize `jar`.

#### Configure PGAdmin:
go to [page](http://127.0.0.1:8080/).
##### Login:
user:
`admin@example.com`

password:
`admin`

##### tables can be found at:
``` text
Servers
└── (your server)
    └── Databases
        └── vava_project
            └── Schemas
                └── public
                    └── Tables
```

##### add server:
- Hostname: rabbit_db
- Port: 5432
- Username: user
- Password: password

### Run `server`
``` bash
mvn clean compile exec:java -pl server
```

you cen test it using `curl`:
``` bash
curl localhost:6969/hello
```
_in future this endpoint may not exist_
### Run `client`
``` bash
mvn clean compile javafx:run -pl client
```
