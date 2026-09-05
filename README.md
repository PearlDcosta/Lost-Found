# Cloud-Based Lost & Found Management System for Educational Institutions

Progress snapshot as of **Phase 13 of 21** (see the full roadmap in
`Phase1_Project_Overview.md`, provided separately in chat).

## What's included in this zip

```
LostAndFoundSystem/
├── sql/
│   └── database.sql              Full MySQL schema (USERS, ITEMS, CLAIMS,
│                                  STATUS_HISTORY) + starter admin login
│
├── resources/
│   └── config.properties         DB / RMI / RPC / storage config — EDIT THIS
│
└── src/com/lostfound/
    ├── Main.java                 CLIENT entry point (launches the Swing GUI)
    ├── model/                    Enums + User, Item, Claim
    ├── exception/                Custom checked exceptions
    ├── util/                     Config loading, DB connection, password
    │                             hashing, validation, RMI client helper
    ├── dao/                      UserDAO, ItemDAO, ClaimDAO (JDBC, CRUD + search)
    ├── remote/                   RMI interfaces/impls + LostFoundServer
    │                             (SERVER entry point)
    ├── rpc/                      UDP RPC server-time service
    ├── service/                  StorageService (Local + Cloud) for images
    ├── distributed/              Token Ring mutual exclusion (claim approval)
    └── gui/                      All Swing screens (Welcome, Login, Register,
                                   Student Dashboard, Report/Search/Claim
                                   screens, Item Details)
```

## What's NOT included yet (later phases)

Admin Dashboard screens (Manage Users/Items/Claims, Verify Claim,
statistics), Ring Election algorithm, final cloud VM deployment
instructions, and the full documentation/viva-prep pack. These come in
Phases 14–21.

---

## How to run this on your own laptop

### 1. Install a JDK (17 or newer recommended, 11+ works)

- Download from https://adoptium.net (Temurin) or https://www.oracle.com/java/technologies/downloads/
- Verify: open a terminal and run `java -version` and `javac -version`

### 2. Get a cloud MySQL database

Pick any one free-tier provider (see Phase 1 notes for details):
Aiven, Railway, Clever Cloud, or AWS RDS Free Tier. You'll end up
with a **host, port, database name, username, password**.

(For a first local test before bothering with the cloud, you can also
just install MySQL locally and use `localhost` — everything below
works identically either way.)

### 3. Run the schema script

Using MySQL Workbench, DBeaver, or the `mysql` CLI, connect to your
database and run the entire `sql/database.sql` file. This creates the
4 tables and one starter admin account:
- **Email:** `admin@college.edu`
- **Password:** `Admin@123`

### 4. Download the MySQL JDBC driver

Download `mysql-connector-j-<version>.jar` from
https://dev.mysql.com/downloads/connector/j/ (choose "Platform
Independent", the `.zip` or `.tar.gz`, and pull the `.jar` out of it).

Create a `lib/` folder inside `LostAndFoundSystem/` and put the jar
there:
```
LostAndFoundSystem/
└── lib/
    └── mysql-connector-j-9.x.x.jar
```

### 5. Edit `resources/config.properties`

If you cloned this repository, first copy `resources/config.properties.example`
to `resources/config.properties`. The real `config.properties` file is ignored
by Git so database credentials are not committed.

Open it in any text editor and fill in your real values:
```properties
db.url=jdbc:mysql://YOUR-HOST:3306/lostfound_db?useSSL=true&requireSSL=true&serverTimezone=UTC
db.username=YOUR_DB_USERNAME
db.password=YOUR_DB_PASSWORD
```
Leave `rmi.host=localhost`, `rmi.port=1099`, `rpc.host=localhost`,
`rpc.port=9877` as they are for running everything on one laptop.
Leave `storage.provider=LOCAL` for now (no cloud storage credentials
needed yet — images save to a local `images/` folder automatically).

### 6. Compile everything

Open a terminal **inside the `LostAndFoundSystem` folder** and run:

**macOS / Linux:**
```bash
javac -d bin -cp "lib/*" $(find src -name "*.java")
```

**Windows (PowerShell):**
```powershell
Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName } > sources.txt
javac -d bin -cp "lib\*" "@sources.txt"
```

This compiles all `.java` files into a `bin/` folder.

### 7. Start the SERVER (first terminal window)

```bash
java -cp "bin;resources;lib/*" com.lostfound.remote.LostFoundServer
```
(On macOS/Linux use `:` instead of `;` as the separator:
`java -cp "bin:resources:lib/*" com.lostfound.remote.LostFoundServer`)

You should see:
```
=================================================
 LostFoundServer started successfully
 RMI registry listening on port 1099
 Bound services: AuthService, ItemService, ClaimService
 RPC server-time service listening on UDP port 9877
 Press Ctrl+C to stop the server.
=================================================
```
**Leave this window open** — this is your running server.

### 8. Start the CLIENT (a second, separate terminal window)

```bash
java -cp "bin;resources;lib/*" com.lostfound.Main
```
(macOS/Linux: `java -cp "bin:resources:lib/*" com.lostfound.Main`)

The Welcome screen should appear. Log in with `admin@college.edu` /
`Admin@123`, or click Register to create a student account.

You can run **Main** again in additional terminal windows to simulate
multiple students/admins using the system at once, all talking to the
same server.

### Quick troubleshooting

| Problem | Likely cause |
|---|---|
| `ExceptionInInitializerError: MySQL JDBC driver not found` | The connector jar isn't on your classpath — check step 4/6 |
| `Could not find 'config.properties' on the classpath` | You forgot `resources` in the `-cp` when running |
| Login says "Could not reach the server" | `LostFoundServer` isn't running, or you closed its terminal |
| `Access denied for user` (MySQL) | Wrong username/password in `config.properties` |
| Login says "Invalid email or password" for the seeded admin | Make sure you ran the **current** `sql/database.sql` from this zip — an earlier version had an incorrect password hash |

Everything in this zip has been integration-tested against a real
running MySQL database and a real running RMI server (including
concurrent-access and image-upload tests) — see the phase-by-phase
explanations in chat for the full test output.

