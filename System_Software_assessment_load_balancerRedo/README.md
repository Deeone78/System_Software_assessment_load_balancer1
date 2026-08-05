# COMP20081 Load Balancer / Cloud Storage System

## How to build and run

### Step 1 — Build the JAR in NetBeans

Open the `JavaFXApplication1` project in NetBeans.  
Go to **Run → Clean and Build Project** (or press Shift+F11).  
This creates `JavaFXApplication1/target/JavaFXApplication1-1.0-SNAPSHOT.jar`.

> `Dockerfile.lb` and `Dockerfile.storage` copy this pre-built JAR into their containers.  
> You must complete this step **before** running `docker compose build`.

### Step 2 — Build and start the containers

Open a terminal in the project root (the folder containing `docker-compose.yml`) and run:

```
docker compose build
docker compose up -d
```

### Step 3 — Run the JavaFX app

Open and run `App.java` from NetBeans as normal.  
The app connects to the load balancer at `http://localhost:8080` and to MySQL at `lbc_mysql_registry:3306`.

Default admin login: **admin / admin123**

---

## What was fixed

| File | What was wrong | What was changed |
|---|---|---|
| `docker-compose.yml` | `context:` pointed to `C:/System_Software_assessment_load_balancerRedo` (only worked on one machine) | Changed all `context:` to `.` |
| `docker-compose.yml` | MySQL service was named `db-node` but `DB.java` connects to `lbc_mysql_registry` | Service renamed to `lbc_mysql_registry` |
| `docker-compose.yml` | Storage nodes 1 & 2 service names were `storage-node-1/2` but `LoadBalancer.java` resolves `lbc_storage_01/02` | Service names changed to match |
| `docker-compose.yml` | Storage nodes 3 & 4 were plain Alpine containers (no Java, no StorageServer) | Changed to build with `Dockerfile.storage` |
| `Dockerfile.partitioner` | `CMD` referenced class `Partitioner` which doesn't exist | Fixed to `FilePartitioner` |
| `FilePartitioner.java` | No `main()` method — container crashed immediately | Added `main()` that keeps the container running |
| `RegisterController.java` | Referenced `selectBtn`, `fileText`, `selectBtnHandler` which didn't exist | Removed those references |
| `dashboard.fxml` | File was completely missing — app crashed after login | Created new `dashboard.fxml` |
| `admin.fxml` | File was completely missing | Created new `admin.fxml` |
| `terminal.fxml` | File was completely missing | Created new `terminal.fxml` |
| `sql-init/init.sql` | Was named `init.sql.txt` (MySQL ignores non-`.sql` files) — also missing `users` and `file_metadata` tables | Renamed to `init.sql`, added all required tables |
| `LoadBalancer.java` | `nodes[]` array only had 2 entries | Updated to all 4 storage nodes |

---

## Container overview

| Container | Purpose | Port |
|---|---|---|
| `ntu-vm-comp20081` | NTU Lubuntu VM (RDP desktop) | 3390 (RDP), 2022 (SSH) |
| `lbc_mysql_registry` | MySQL database | 3306 |
| `lbc_load_balancer` | Load balancer (round-robin / SJN) | 8080 |
| `lbc_storage_01..04` | Storage nodes (HTTP file server on port 8081) | — |
| `lbc_partitioner` | File partitioner helper | — |
| `lbc_main_gateway` | Main app gateway (headless demo) | — |
| `lbc_host_manager` | Host manager placeholder | — |
