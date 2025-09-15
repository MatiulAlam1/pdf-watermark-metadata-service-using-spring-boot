# Watermark Service Deployment & Monitoring

This document outlines the steps to build the Watermark Service JAR file locally, deploy it to the server `v4465a.unx.vstage.co` for both Test and Production environments, and manage the service instances using `sudo`. The current version being deployed is **1.0.1**, requiring **Java 17**. The JAR file name on the server is expected to be `valmet-watermark-service-1.0.1.jar` for production, and a similar naming convention for test.

**Key Environment Details:**
- **Test Environment:** Runs on port `8089` using the Spring Boot `test` profile.
- **Production Environment:** Runs on ports `8081` & `8082` (and potentially others if scaled) using the Spring Boot `prod` profile.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Building the Application (Locally)](#building-the-application-locally)
3. [Deployment to Server v4465a.unx.vstage.co](#deployment-to-server-v4465aunxvstageco)
   - [Test Environment](#test-environment)
   - [Production Environment](#production-environment)
4. [Restarting the Service (Using Sudo)](#restarting-the-service-using-sudo)
5. [Verification](#verification)
6. [Configuration](#configuration)
7. [Monitoring (Prometheus & Grafana)](#monitoring-prometheus--grafana)
8. [Scheduled Tasks / Cron Jobs](#scheduled-tasks--cron-jobs)
9. [Server-Level System Reliability Monitoring (`server_monitoring.sh`)](#server-level-system-reliability-monitoring-server_monitoringsh)
10. [Load Balancer (Nginx)](#load-balancer-nginx)
11. [Rollback Strategy (Basic)](#rollback-strategy-basic)
12. [Scaling Production Instances (Adding a New Instance)](#scaling-production-instances-adding-a-new-instance)

## Prerequisites

**For Building (Local Machine):**

- **Java Development Kit (JDK):** **Version 17** or higher.
- **Apache Maven:** **Version 3.4.1 or 3.4.5**.
- **CVS Client:** To check out the source code.
- **CVS Access:** Credentials and access to the project's CVS repository.

**For Deployment (Server: `v4465a.unx.vstage.co`):**

- **SSH Access:** SSH access to `v4465a.unx.vstage.co` with a user account (`<your_username>`).
- **Sudo Privileges:** The user `<your_username>` must have `sudo` privileges to execute the `start_watermark_service.sh` script and potentially the `server_monitoring.sh` script.
- **Java Runtime Environment (JRE):** **Version 17** or higher installed on `v4465a.unx.vstage.co`.
- **Permissions:**
  - Write permissions for `<your_username>` to the deployment directories:
    - `/data/watermark_service_test/`
    - `/data/watermark_service/`
  - The user the service runs as (likely `root` due to `sudo` execution of the scripts, unless scripts switch user) needs permissions to write application logs to:
    - `/var/log/watermark-service-test/`
    - `/var/log/watermark-service/`
  - Permissions to write to `/var/log/start_watermark_service.log` (used by `start_watermark_service.sh`) and `/var/log/system_monitor.log`, `/var/log/system_monitor_alerts.log` (used by `server_monitoring.sh`).
- **Restart Scripts:**
  - **`start_watermark_service.sh`**: This script (one for test, one for production, based on the example provided for prod) must exist in the respective deployment directories and be executable.
    - **Crucially, this script has a hardcoded `JAR_PATH` variable (e.g., `JAR_PATH="/data/watermark_service/valmet-watermark-service-1.0.1.jar"`). To deploy a new JAR version, this `JAR_PATH` variable within the script itself MUST be updated.**
  - **`server_monitoring.sh`**: An "Enhanced System Reliability Monitor Script" (e.g., `/opt/scripts/server_monitoring.sh`) is used for broader server health checks.
- **Deployed JAR File:** The actual JAR file (e.g., `valmet-watermark-service-1.0.1.jar`) must be present in the location specified by `JAR_PATH` in `start_watermark_service.sh`.
- **Tools:** `scp` or `rsync` available on your local machine for transferring the JAR file.

## Building the Application (Locally)

1. **Ensure Java 17 is active:**
   ```bash
   java -version # Verify it shows Java 17
   # Set JAVA_HOME or use SDK management tools if needed
   ```
2. **Ensure correct Maven version is active:**
   ```bash
   mvn -version # Verify it shows Maven 3.4.1 or 3.4.5
   # Use Maven wrapper (mvnw) if available or adjust PATH if needed
   ```
3. **Check out the source code from CVS:**
   Use your standard CVS client commands to check out the latest version of the Watermark Service source code to your local machine.
4. **Navigate to the project's root directory:**
   ```bash
   cd /path/to/your/local/watermark-service-project # This is where you checked out the code
   ```
5. **Clean and build the project using Maven:**
   *(Ensure your project's `pom.xml` is configured to produce the desired version, e.g., `1.0.1`, and uses Java 17. The final artifact name should match what will be configured in the `JAR_PATH` of the `start_watermark_service.sh` script, e.g., `valmet-watermark-service-1.0.1.jar`)*
   ```bash
   mvn clean package
   ```
6. **Locate the JAR file:** The build process should generate the specific JAR file, for example:
   - `target/valmet-watermark-service-1.0.1.jar`
   *Verify this file exists and its name is consistent with deployment plans.*

## Deployment to Server `v4465a.unx.vstage.co`

This process involves transferring the newly built JAR file to the server, **editing the `start_watermark_service.sh` script to update its `JAR_PATH` variable to the new JAR name/version**, and then running the updated restart script using `sudo`.

### Test Environment

- **Server:** `v4465a.unx.vstage.co`
- **Deployment Directory:** `/data/watermark_service_test/`
- **Port:** `8089`
- **Spring Boot Profile:** `test`
- **Restart Script:** `/data/watermark_service_test/start_watermark_service.sh` (This script would need similar logic to the production one: hardcoded `JAR_PATH`, accept port, set `test` profile).
- **Deployed JAR Name:** e.g., `valmet-watermark-service-1.0.1.jar` (matching `JAR_PATH` in the test script).
- **Application Log File:** `/var/log/watermark-service-test/instance_8089_logger.log`

**Example Deployment Steps (Test - New Version 1.0.1, assuming current is 1.0.0):**

1. **Build `valmet-watermark-service-1.0.1.jar` locally.**
2. **SSH into the server:**
   ```bash
   ssh <your_username>@v4465a.unx.vstage.co
   ```
3. **Edit the Test Restart Script:**
   Update `JAR_PATH` in `/data/watermark_service_test/start_watermark_service.sh` to point to the new JAR version.
   ```bash
   # On the server:
   sudo nano /data/watermark_service_test/start_watermark_service.sh
   # Change (example):
   # JAR_PATH="/data/watermark_service_test/valmet-watermark-service-1.0.0.jar"
   # TO:
   # JAR_PATH="/data/watermark_service_test/valmet-watermark-service-1.0.1.jar"
   # Ensure it's configured for the test profile (e.g., JAVA_ARGS="--spring.profiles.active=test")
   # and handles port 8089 correctly (e.g., DEFAULT_PORTS=(8089) or by argument).
   # Save and exit.
   ```
4. **Transfer the JAR (from your local machine):**
   Replace `<your_username>` with your server username. The destination path for the JAR should be `/data/watermark_service_test/` to match the `JAR_PATH` in the script.
   ```bash
   scp target/valmet-watermark-service-1.0.1.jar <your_username>@v4465a.unx.vstage.co:/data/watermark_service_test/
   ```
5. **Restart the service using sudo:**
   ```bash
   cd /data/watermark_service_test/
   # The script should be configured to handle the test port (e.g., 8089)
   # either by default or by passing it as an argument.
   sudo ./start_watermark_service.sh 8089 # Or just `sudo ./start_watermark_service.sh` if 8089 is default
   ```
6. **Verify:** Check logs for `test` profile activation and service status for port 8089 (see [Verification](#verification)).
7. **Exit SSH:** `exit`

### Production Environment

- **Server:** `v4465a.unx.vstage.co`
- **Deployment Directory:** `/data/watermark_service/`
- **Ports:** `8081`, `8082` (and any scaled instances)
- **Spring Boot Profile:** `prod`
- **Restart Script:** `/data/watermark_service/start_watermark_service.sh` (as provided in the prompt). **`JAR_PATH` in this script must be updated for new versions.**
- **Application Log Files:**
  - Instance 8081: `/var/log/watermark-service/instance_8081_logger.log`
  - Instance 8082: `/var/log/watermark-service/instance_8082_logger.log`
- **Deployed JAR Name:** Matches `JAR_PATH` in script (e.g., `valmet-watermark-service-1.0.1.jar`).

**Example Deployment Steps (Production - New Version 1.0.1, assuming current was 1.0.0):**

1. **Build `valmet-watermark-service-1.0.1.jar` locally.**
2. **SSH into the server:**
   ```bash
   ssh <your_username>@v4465a.unx.vstage.co
   ```
3. **Backup current production script (optional but recommended):**
   ```bash
   sudo cp /data/watermark_service/start_watermark_service.sh /data/watermark_service/start_watermark_service.sh.bak_$(date +%Y%m%d%H%M)
   ```
4. **Edit the Production Restart Script to point to the new JAR version:**
   ```bash
   # On the server:
   sudo nano /data/watermark_service/start_watermark_service.sh
   # Change (example):
   # JAR_PATH="/data/watermark_service/valmet-watermark-service-1.0.0.jar"
   # TO:
   # JAR_PATH="/data/watermark_service/valmet-watermark-service-1.0.1.jar"
   # Save and exit.
   ```
5. **Transfer the new JAR (from your local machine):**
   Replace `<your_username>` with your server username. The destination path for the JAR should be `/data/watermark_service/` to match the script's `JAR_PATH`.
   ```bash
   scp target/valmet-watermark-service-1.0.1.jar <your_username>@v4465a.unx.vstage.co:/data/watermark_service/
   ```
   *(Ensure any older version JAR, like `valmet-watermark-service-1.0.0.jar`, is kept in `/data/watermark_service/` if you want the ability to quickly roll back by editing the script's `JAR_PATH` back to it. Otherwise, manage old JARs per your policy.)*
6. **Restart the service instance(s) using sudo:**
   - **For a rolling update (recommended for zero downtime):**
     ```bash
     cd /data/watermark_service/
     # 1. Remove 8081 from Nginx load balancer & reload Nginx (see Nginx section)
     # 2. Restart instance 8081 (the script now uses the new JAR specified in JAR_PATH):
     sudo ./start_watermark_service.sh 8081
     # 3. Verify instance 8081 is healthy (logs, health check)
     # 4. Add 8081 back to Nginx, remove 8082 from Nginx & reload Nginx
     # 5. Restart instance 8082:
     sudo ./start_watermark_service.sh 8082
     # 6. Verify instance 8082 is healthy
     # 7. Add 8082 back to Nginx & reload Nginx
     # (Repeat for any other production instances if they exist)
     ```
   - **To restart all default production instances (8081 & 8082) at once (causes downtime):**
     ```bash
     cd /data/watermark_service/
     sudo ./start_watermark_service.sh # This will restart 8081 and 8082 by default
     ```
7. **Verify:** Check logs for `prod` profile activation and service status on ports 8081 and 8082 (see [Verification](#verification)). The `nohup` output of the `start_watermark_service.sh` script itself will be in `/var/log/start_watermark_service.log`.
8. **Exit SSH:** `exit`

## Restarting the Service (Using Sudo)

The `start_watermark_service.sh` script (in `/data/watermark_service/` for production, and a similar one in `/data/watermark_service_test/` for test) handles stopping any running instance on the specified port(s) and starting new ones. It uses the **JAR file defined in its `JAR_PATH` variable** and activates the appropriate Spring Boot profile (`prod` for production, `test` for test). **It must be executed with `sudo`.**

- **Test (on `v4465a.unx.vstage.co`):**
  *(Assuming `/data/watermark_service_test/start_watermark_service.sh` is configured for test port 8089 and `test` profile)*
  ```bash
  ssh <your_username>@v4465a.unx.vstage.co
  cd /data/watermark_service_test/
  # To restart the instance on port 8089:
  sudo ./start_watermark_service.sh 8089
  exit
  ```

- **Production (on `v4465a.unx.vstage.co`):**
  ```bash
  ssh <your_username>@v4465a.unx.vstage.co
  cd /data/watermark_service/
  ```
  - To restart **all default** production instances (currently 8081 & 8082 as per `start_watermark_service.sh` `DEFAULT_PORTS`) using the JAR in `JAR_PATH`:
    ```bash
    sudo ./start_watermark_service.sh
    ```
  - To restart **only** the instance on port **8081** using the JAR in `JAR_PATH`:
    ```bash
    sudo ./start_watermark_service.sh 8081
    ```
  - To restart **only** the instance on port **8082** using the JAR in `JAR_PATH`:
    ```bash
    sudo ./start_watermark_service.sh 8082
    ```
  - To restart a specific set of instances (e.g., 8081 and a scaled instance):
    ```bash
    sudo ./start_watermark_service.sh 8081 <other_port>
    ```
  ```bash
  exit
  ```

## Verification

After restarting the service, verify it's running correctly on `v4465a.unx.vstage.co`:

1. **Check `start_watermark_service.sh` Script's Startup Log:** This log contains the `nohup` output from the `java -jar` command when `start_watermark_service.sh` is run.
   ```bash
   # SSH into the server first
   tail -f /var/log/start_watermark_service.log
   ```

2. **Check Application Instance Logs:** Tail the specific application logs. Look for messages indicating which Spring Boot profile is active.
   ```bash
   # SSH into the server first: ssh <your_username>@v4465a.unx.vstage.co

   # --- Check Active Profile & Errors (Example for prod instances) ---
   # Test
   grep -Ei "profiles are active: test|ERROR" /var/log/watermark-service-test/instance_8089_logger.log | tail -n 20
   tail -f /var/log/watermark-service-test/instance_8089_logger.log

   # Production
   grep -Ei "profiles are active: prod|ERROR" /var/log/watermark-service/instance_8081_logger.log | tail -n 20
   tail -f /var/log/watermark-service/instance_8081_logger.log

   grep -Ei "profiles are active: prod|ERROR" /var/log/watermark-service/instance_8082_logger.log | tail -n 20
   tail -f /var/log/watermark-service/instance_8082_logger.log
   # (Add checks for other instances if scaled)

   # --- Check Cron Monitoring & Rotation Logs ---
   # For /data/watermark_service/monitor_watermark_service.sh (older script)
   # tail -f /var/log/monitor_watermark_service.log
   # For server_monitoring.sh (newer script)
   tail -f /var/log/system_monitor.log
   tail -f /var/log/system_monitor_alerts.log

   tail -f /var/log/log_rotation.log
   ```

3. **Check Process Status & JAR Version:**
   The JAR name should match the one specified in the `JAR_PATH` of the executed `start_watermark_service.sh` script.
   ```bash
   # SSH into the server first
   # Look for the java process running the correct JAR file (e.g., valmet-watermark-service-1.0.1.jar)
   ps aux | grep valmet-watermark-service.*.jar | grep java
   # Or check Java processes by listing them
   jps -l | grep valmet-watermark-service
   ```
   *Verify the process is running with the JAR version defined in the `start_watermark_service.sh` script's `JAR_PATH`.*

4. **Check Health Endpoint (if available):**
   - **Test:** `curl https://watermark-service-test.valmet.com/actuator/health` (via Nginx) or `curl https://localhost:8089/actuator/health` (directly on server, if app serves HTTPS)
   - **Production Instance 1 (direct):** `curl https://localhost:8081/actuator/health` (directly on server, if app serves HTTPS)
   - **Production Instance 2 (direct):** `curl https://localhost:8082/actuator/health` (directly on server, if app serves HTTPS)
   - **Load Balanced Endpoint (Production):** `curl https://<your_production_domain_here>/actuator/health` (e.g., `https://watermark-service.valmet.com/actuator/health`)
   
   Look for status `"status":"UP"`. Check `/actuator/info` (if available) for the correct version.

5. **Check Monitoring Dashboards:** Review Grafana dashboards for performance metrics and errors.

## Configuration

- Environment-specific configurations are managed *outside* the JAR, primarily using **Spring Boot profiles**.
- The **`start_watermark_service.sh`** script is responsible for:
  - Executing the Java application using the JAR specified in its **hardcoded `JAR_PATH` variable.**
  - Activating the correct Spring Boot profile (`test` or `prod`) via the `--spring.profiles.active` argument.
  - Setting the server port for each instance using the `--server.port` argument.
  - Passing other necessary configurations if added to `JAVA_ARGS` in the script (e.g., memory settings `-Xmx`, `-Xms`).
- Profile-specific properties are typically defined in files like `application-test.properties` or `application-prod.properties` located alongside the JAR (e.g., in `/data/watermark_service/config/` or `/data/watermark_service/`) or packaged within the JAR.
- **Application Log Configuration:** The application itself (e.g., via `logback.spring.xml` or `log4j2.spring.xml`) is configured to use the `server.port` to create distinct log files (e.g., `instance_<port>_logger.log` in `/var/log/watermark-service/` or `/var/log/watermark-service-test/`).
- External dependencies like **HashiCorp Vault** may also provide profile-specific configurations.
- Review the `start_watermark_service.sh` scripts and any `application-*.properties`/`.yml` files in the deployment directories to understand the exact configuration mechanism.

## Monitoring (Prometheus & Grafana)

Comprehensive monitoring dashboards are available in Grafana to observe the health and performance of the Watermark Service.

- **Access URL:** [`https://watermark-service.valmet.com/api/monitoring/login`](https://watermark-service.valmet.com/api/monitoring/login)
- **Authentication:** Login using your standard Valmet Active Directory (AD) credentials.

**Key Dashboards:**

1. **"Watermark Web Service Statistics & Endpoint Metrics"**
   - **Purpose:** Provides a detailed overview of the service's operational health and resource utilization.
   - **Metrics:** CPU, Memory, JVM stats, Request latency/throughput, Error rates, Endpoint-specific metrics.
   - **Usage:** Monitor post-deployment for stability and performance. Diagnose issues. Filter by instance.

2. **"Client System Statistics"**
   - **Purpose:** Tracks API usage patterns by client systems.
   - **Metrics:** Client call frequency, endpoint usage per client.
   - **Usage:** Understand consumers, identify traffic patterns.

Regularly check these dashboards, especially after deployments or during suspected incidents.

## Scheduled Tasks / Cron Jobs

Several automated tasks are configured via cron on `v4465a.unx.vstage.co`.

1. **Watermark Service Monitoring (Production - Legacy)**
   - **Schedule:** Every 3 minutes (`*/3 * * * *`)
   - **Command:** `/data/watermark_service/monitor_watermark_service.sh`
   - **Purpose:** Original watchdog for production Watermark Service instances.
   - **Mechanism:** Checks process status and likely performs basic health checks. Attempts automatic restart (potentially using `sudo ./start_watermark_service.sh <port>`) if an instance is down. This script's restart capability depends on its own logic for identifying ports and the `JAR_PATH` in `start_watermark_service.sh`.
   - **Note:** This might be superseded or complemented by the `server_monitoring.sh` script described in the next section. Clarify which script is the primary automated monitor for Java apps.
   - **Log File:** `/var/log/monitor_watermark_service.log`.

2. **HashiCorp Vault Monitoring**
   - **Schedule:** Every 1 minute (`*/1 * * * *`)
   - **Command:** `/opt/vault_scripts/monitor_hashicorp_vault_service.sh`
   - **Log File:** `/var/log/monitor_hashicorp_vault_service.log`.

3. **Vault Key Rotation**
   - **Schedule:** Monthly, 1st day, 2:00 AM (`0 2 1 * *`)
   - **Command:** `/opt/vault_scripts/rotate_rsa_key_pair_password_in_hashicorp_vault_service.sh`
   - **Log File:** `/var/log/monitor_hashicorp_vault_service.log`.

4. **Log Rotation/Cleanup (`log_rotate_and_cleanup.sh`)**
   - **Schedule:** Quarterly, 1st day of month, Midnight (`0 0 1 */3 *`)
   - **Command:** `/opt/scripts/log_rotate_and_cleanup.sh`
   - **Target Logs:** Includes `/var/log/watermark-service/*.log`, `/var/log/start_watermark_service.log`, `/var/log/system_monitor.log`, etc. Ensure this pattern covers logs from new instances.
   - **Log File:** `/var/log/log_rotation.log`.

5. **Log Permissions for Monitoring (`log_permission.sh`)**
   - **Schedule:** Hourly (`0 * * * *`)
   - **Command:** `/usr/local/bin/log_permission.sh`
   - **Purpose:** Ensures Promtail can read application logs, including those from new instances.

6. **Grafana Monitoring**
   - **Schedule:** Every 1 minute (`* * * * *`)
   - **Command:** `pgrep grafana || systemctl start grafana-server`.

7. **System Reliability Monitor (`server_monitoring.sh`) via systemctl:**
   If the "Enhanced System Reliability Monitor Script" is used, it would be run by systemctl:
   - **Schedule:** Every 1 minute
   - **Command:** `sudo /opt/scripts/server_monitoring.sh >> /var/log/system_monitor_cron.log 2>&1`
   - *(See next section for details on this script).*

## Server-Level System Reliability Monitoring (`server_monitoring.sh`)

Beyond the specific `start_watermark_service.sh` script for deploying and restarting the(Watermark service, an "Enhanced System Reliability Monitor Script" (e.g., located at `/opt/scripts/server_monitoring.sh` and run via `systemctl`) is in place for broader server health checks. This script monitors various services, including the Java applications (like Watermark Service instances), systemd services (Grafana, Prometheus, etc.), CPU load, and storage.

**Script Purpose:**

- **Service Monitoring:** Regularly checks if critical services are running.
  - **Java Applications:** Monitors applications listening on specified ports (defined in `JAVA_PORTS` array within the script). It also checks their CPU and memory/heap usage against thresholds.
  - **Systemd Services:** Monitors `grafana-server`, `prometheus`, `loki`, and `promtail` (as defined by variables in the script).
- **Resource Monitoring:**
  - **CPU:** Tracks overall system load and per-core CPU utilization.
  - **Storage:** Monitors disk space and inode usage for critical partitions.
  - **System Memory:** Checks overall system memory usage.
- **Automatic Restart:** Attempts to restart failed services (including Java applications via their respective start scripts defined in `JAVA_APP_PATHS` array) up to a configured number of `MAX_RESTART_ATTEMPTS`.
- **Alerting (via Logging):** Logs normal operations to `/var/log/system_monitor.log` and critical issues (like repeated restart failures, high CPU/storage) to `/var/log/system_monitor_alerts.log`. It includes a cooldown mechanism to prevent alert spam.

**Key Configuration Points in `server_monitoring.sh`:**

- **`JAVA_PORTS=(8089 8082 8081)`:**
  - This array defines which ports the monitor script will check for running Java applications.
  - **Crucially, when scaling the Watermark Service (or other Java apps) to new ports (e.g., adding an instance on a new port like `8083`), this `JAVA_PORTS` array within the `server_monitoring.sh` script MUST be updated to include the new port number.** After adding new port(8083) execute the command **sudo systemctl restart server-monitor.service**. Otherwise, the new instance will not be monitored or auto-restarted by this script.
- **`declare -A JAVA_APP_PATHS=( [8089]="/data/watermark_service_test/start_watermark_service.sh" [8082]="/data/watermark_service/start_watermark_service.sh" [8081]="/data/watermark_service/start_watermark_service.sh" )`:**
  - This associative array maps a Java application's port to its specific start script.
  - When a Java application on a monitored port is found to be down or problematic, this monitor script will attempt to restart it using `sudo sh <app_path> <port>`. For example, if the app on port 8081 fails, it will run `sudo sh /data/watermark_service/start_watermark_service.sh 8081`.
  - **When adding a new Java instance on a new port that needs monitoring (e.g. port `8083`), an entry must be added to `JAVA_APP_PATHS` for that port, pointing to its corresponding start script.**
    ```bash
    # Example: If adding instance on port 8083 using the prod start script
    # In server_monitoring.sh, update JAVA_APP_PATHS:
    # JAVA_APP_PATHS=(
    #    [8089]="/data/watermark_service_test/start_watermark_service.sh"
    #    [8082]="/data/watermark_service/start_watermark_service.sh"
    #    [8081]="/data/watermark_service/start_watermark_service.sh"
    #    [8083]="/data/watermark_service/start_watermark_service.sh" # Add this line
    # )
    # And also add 8083 to the JAVA_PORTS array.
    ```
- **`JAVA_HEAP_LIMIT_PERCENT=80`:** Threshold for Java application heap/memory usage before a restart is attempted.
- **`MAX_RESTART_ATTEMPTS=3`:** Maximum times the script will try to restart a failed service before logging a critical alert and stopping attempts for that service.
- **Log Files:** `/var/log/system_monitor.log` (general logs) and `/var/log/system_monitor_alerts.log` (critical alerts).
- **Systemd Service Names:** `GRAFANA_SERVICE`, `PROMETHEUS_SERVICE`, etc., define the exact names of systemd services to monitor.
- **CPU and Storage Thresholds:** Various thresholds (`CPU_LOAD_THRESHOLD`, `DISK_USAGE_THRESHOLD`, etc.) trigger alerts if breached.

**How it interacts with Watermark Service Deployment:**

- **Independent Monitoring:** This `server_monitoring.sh` acts as an additional layer of monitoring. The Watermark Service's own `start_watermark_service.sh` is used for deployment and manual restarts. This monitor script provides automated health checks and restart attempts.
- **Restart Mechanism:** If the `server_monitoring.sh` detects a Watermark Service instance (e.g., on port 8081) is down or unhealthy, it will use the path defined in `JAVA_APP_PATHS` (e.g., `/data/watermark_service/start_watermark_service.sh`) and the port number to attempt a restart. This means the `start_watermark_service.sh` script (with its hardcoded `JAR_PATH`) will be invoked by the monitor.
- **Configuration for New Instances:** When a new Watermark Service instance is added (e.g., on port 808X):
  1. Add port `808X` to the `JAVA_PORTS` array in `server_monitoring.sh`.
  2. Add an entry for port `808X` to the `JAVA_APP_PATHS` array in `server_monitoring.sh`, pointing to the correct start script.
  3. Execute the command `sudo systemctl restart server-monitor.service`.
  4. This ensures the new instance is included in the monitoring loop for health checks and auto-restarts by `server_monitoring.sh`.

By keeping the `JAVA_PORTS` and `JAVA_APP_PATHS` in `server_monitoring.sh` synchronized with the deployed Java application instances, this script provides robust, automated monitoring and recovery attempts for the Watermark Service and other critical components on the server.

## Load Balancer (Nginx)

An Nginx instance on `v4465a.unx.vstage.co` serves as a reverse proxy and load balancer for the Watermark Service. It handles SSL termination for client connections and routes traffic to the appropriate backend instances for both Production and Test environments, as well as providing access to monitoring tools like Grafana.

**Nginx Configuration Overview:**

The Nginx configuration defines how traffic is routed. The relevant parts for the Watermark Service are typically found within the main Nginx configuration file (e.g., `/etc/nginx/nginx.conf`) or included files (e.g., from `/etc/nginx/conf.d/` or `/etc/nginx/sites-enabled/`). The key components based on the provided configuration are:

*   **SSL Termination:** Nginx listens on port `443` for HTTPS traffic for `*.valmet.com` and `watermark-service-test.valmet.com`, using SSL certificates located at `/etc/httpd/ssl/ServerCertificate.crt` and `/etc/httpd/ssl/ServerCertificate.key`.
*   **Production Environment (`*.valmet.com`):**
    *   Traffic to hostnames matching `*.valmet.com` (e.g., `watermark-service.valmet.com`) is handled by a `server` block.
    *   This block uses an `upstream app_servers` definition to load balance requests across the production instances:
        ```nginx
        upstream app_servers {
          server localhost:8081; # Production instance 1
          server localhost:8082; # Production instance 2
        }
        ```
    *   Requests are proxied to these backend servers using `proxy_pass https://app_servers;`. This implies that the Spring Boot applications on ports `8081` and `8082` must be configured to serve HTTPS.
*   **Test Environment (`watermark-service-test.valmet.com`):**
    *   Traffic to `watermark-service-test.valmet.com` is handled by a separate `server` block.
    *   Requests are proxied directly to the test instance using `proxy_pass https://localhost:8089;`. This implies the Spring Boot application on port `8089` must be configured to serve HTTPS.
*   **Monitoring Access (`/api/monitoring`):**
    *   Both the `*.valmet.com` and `watermark-service-test.valmet.com` server blocks include a `location /api/monitoring` block.
    *   This proxies requests to Grafana running on `http://localhost:3000`, stripping the `/api/monitoring` prefix. This aligns with the Grafana access URL `https://watermark-service.valmet.com/api/monitoring/login`.
*   **Prometheus Access (`/prometheus`):**
    *   The `*.valmet.com` server block also includes a `location /prometheus` block, which proxies requests to Prometheus on `http://localhost:9090` with basic authentication.

**Testing and Reloading Nginx Configuration:**

After making any changes to the Nginx configuration files, always test the configuration and then reload Nginx gracefully:

1.  **Test Configuration:**
    ```bash
    sudo nginx -t
    ```
    *(Ensure this command outputs that the syntax is ok and the test is successful.)*

2.  **Reload Nginx:**
    ```bash
    sudo systemctl reload nginx
    ```
    *(Alternatively, `sudo nginx -s reload` might be used.)*

**Rolling Updates (Production Environment - Zero Downtime):**
This involves taking one instance out of the Nginx load balancer pool, updating it, then repeating for other instances.
1. Modify Nginx config to mark one backend server down in the `upstream app_servers` block (e.g., `server localhost:8081 down;`).
2. Test (`sudo nginx -t`) and reload Nginx (`sudo systemctl reload nginx`).
3. Deploy to/restart that instance (e.g., `sudo /data/watermark_service/start_watermark_service.sh <port>`). Remember to update `JAR_PATH` in `start_watermark_service.sh` first if it's a new JAR version.
4. Verify instance.
5. Re-enable instance in Nginx, mark next instance down, test, reload Nginx.
6. Repeat for other instances.
7. Re-enable all instances in Nginx, test, reload.

## Rollback Strategy (Basic)

If deployment of a new version (e.g., `valmet-watermark-service-1.0.1.jar`) fails and you need to revert to a previous version (e.g., `valmet-watermark-service-1.0.0.jar`), assuming the previous version's JAR is still present in the deployment directory (e.g., `/data/watermark_service/`):

1. **Identify failure:** Check application instance logs, health endpoints, Grafana, and `server_monitoring.sh` logs.
2. **Stop failed instance(s) (Optional):** The `start_watermark_service.sh` script will kill processes on the specified ports before restarting. If the `server_monitoring.sh` or other cron jobs are aggressively restarting the faulty version, you might want to temporarily disable them (`sudo crontab -e` and comment out the relevant lines).
3. **Edit the `start_watermark_service.sh` Script to Point to the Previous JAR:**
   On `v4465a.unx.vstage.co`, edit the script for the affected environment:
   ```bash
   # Example for Production rollback:
   sudo nano /data/watermark_service/start_watermark_service.sh
   # Change (example from 1.0.1 back to 1.0.0):
   # JAR_PATH="/data/watermark_service/valmet-watermark-service-1.0.1.jar" # Faulty version
   # TO:
   # JAR_PATH="/data/watermark_service/valmet-watermark-service-1.0.0.jar" # Previous good version
   # Save and exit.
   ```
4. **Restart the service with the previous JAR version using the `start_watermark_service.sh` script:**
   This will stop the instances running the faulty version (if any on those ports) and start them with the older JAR now specified in `JAR_PATH`.
   ```bash
   # For Production, restarting default instances 8081 and 8082:
   cd /data/watermark_service/
   sudo ./start_watermark_service.sh # Or specify ports: sudo ./start_watermark_service.sh 8081 8082
   ```
5. **Verify the service is functional on the previous version:**
   - Check application logs for correct profile and absence of startup errors.
   - Check process status: `ps aux | grep valmet-watermark-service-1.0.0.jar | grep java`
   - Check health endpoints and Grafana.
6. **Re-enable any cron jobs** (`server_monitoring.sh`, `/data/watermark_service/monitor_watermark_service.sh`) if they were disabled.

## Scaling Production Instances (Adding a New Instance)

This section describes how to add a new production Watermark Service instance, for example, running on port 8083, to the existing setup on `v4465a.unx.vstage.co`. This assumes the server has sufficient resources. The new instance will run the JAR version currently specified in the `JAR_PATH` of `/data/watermark_service/start_watermark_service.sh`.

**Prerequisites & Considerations:**

- **Server Resources:** Ensure `v4465a.unx.vstage.co` has adequate CPU, memory, and disk space.
- **`start_watermark_service.sh` Script Usage:**
  - The existing `/data/watermark_service/start_watermark_service.sh` script (from the prompt) can start an instance on a new port by passing the port number as an argument (e.g., `sudo ./start_watermark_service.sh 8083`).
  - If you want the new port (e.g., 8083) to become part of the default set of ports managed when `start_watermark_service.sh` is run without any arguments, you would need to edit that script and add the new port to its `DEFAULT_PORTS` array:
    ```bash
    # Inside /data/watermark_service/start_watermark_service.sh
    # DEFAULT_PORTS=(8081 8082)
    # Change to:
    # DEFAULT_PORTS=(8081 8082 8083)
    ```
- **Application Logging:** The Spring Boot application is configured (e.g., via `logback.spring.xml`) to create a distinct log file for the new instance, typically based on the port (e.g., `/var/log/watermark-service/instance_8083_logger.log`).
- **Log Directory and Permissions:** Ensure `/var/log/watermark-service/` exists. The `log_permission.sh` cron job should correctly set permissions for the new log file.
  <!-- - **Prometheus/Grafana:** Scrape configurations and dashboards need updating for the new instance.
    - Add the new target `v4465a.unx.vstage.co:8083` in the **/etc/prometheus/prometheus.yml** file.
    ```
    scrape_configs:
      - job_name: 'watermark-service'
         scheme: https
         metrics_path: '/actuator/prometheus'
         tls_config:
            insecure_skip_verify: true
         static_configs:
            - targets:
            - 'v4465a.unx.vstage.co:8081'
            - 'v4465a.unx.vstage.co:8082'
            - 'v4465a.unx.vstage.co:8089'
            - 'v4465a.unx.vstage.co:8083' # New instance
     ```
    - Execute the command `sudo systemctl restart prometheus` to reload prometheus service. -->

**Steps to Add a New Production Instance (e.g., on Port 8083):**

1. **Update Nginx Configuration:**
   - SSH into `v4465a.unx.vstage.co`.
   - Edit the Nginx configuration file (e.g., `/etc/nginx/nginx.conf`) with the `upstream app_servers` block.
   - Add the new server instance:
     ```nginx
     upstream app_servers {
       server localhost:8081;
       server localhost:8082;
       server localhost:8083; # New instance
     }
     ```
   - Test: `sudo nginx -t`. If successful, reload: `sudo systemctl reload nginx`.

2. **Ensure Application JAR is Present:**
   - Verify that the JAR file specified in `JAR_PATH` within `/data/watermark_service/start_watermark_service.sh` (e.g., `valmet-watermark-service-1.0.1.jar`) exists in `/data/watermark_service/`. This JAR will be used.

<!-- 3. **Update `server_monitoring.sh` (if used for automated monitoring):**
   - Edit `/opt/scripts/server_monitoring.sh` (or its actual path).
   - Add the new port to `JAVA_PORTS`:
     ```bash
     JAVA_PORTS=(8089 8082 8081 8083) # Added 8083
     ```
   - Add the new port and its start script path to `JAVA_APP_PATHS`:
     ```bash
     declare -A JAVA_APP_PATHS=(
         [8089]="/data/watermark_service_test/start_watermark_service.sh"
         [8082]="/data/watermark_service/start_watermark_service.sh"
         [8081]="/data/watermark_service/start_watermark_service.sh"
         [8083]="/data/watermark_service/start_watermark_service.sh" # Added for new instance
     )
     ```
   - Save the `server_monitoring.sh` script.
   - Execute `sudo systemctl restart server-monitor.service`. -->

3. **Start the New Application Instance:**
   - Navigate to the production deployment directory:
     ```bash
     cd /data/watermark_service/
     ```
   - Execute the `start_watermark_service.sh` script with `sudo`, providing the new port:
     ```bash
     sudo ./start_watermark_service.sh 8083
     ```
     *(This uses the JAR from `JAR_PATH` in the script, for port 8083, with prod profile. Script output to `/var/log/start_watermark_service.log`)*

4. **Verify the New Instance:**
   - `start_watermark_service.sh` Log: `tail -f /var/log/start_watermark_service.log`
   - Application Log: `tail -f /var/log/watermark-service/instance_8083_logger.log` (Check for prod profile, no errors).
   - Process Status: `ps aux | grep valmet-watermark-service.*.jar | grep "port=8083"` and `jps -l | grep valmet-watermark-service`.
   - Health Endpoint (Directly): `curl https://localhost:8083/actuator/health` (Look for `"status":"UP"`).
   - Health Endpoint (Via Nginx): `curl https://watermark-service.valmet.com/actuator/health`.
   - Monitoring: Check Grafana/Prometheus. Check `server_monitoring.sh` logs (`/var/log/system_monitor.log`) on its next run to see if it correctly identifies the new instance.

5. **Update `server_monitoring.sh` (if used for automated monitoring):**
   - Edit `/opt/scripts/server_monitoring.sh` (or its actual path).
   - Add the new port to `JAVA_PORTS`:
     ```bash
     JAVA_PORTS=(8089 8082 8081 8083) # Added 8083
     ```
   - Add the new port and its start script path to `JAVA_APP_PATHS`:
     ```bash
     declare -A JAVA_APP_PATHS=(
         [8089]="/data/watermark_service_test/start_watermark_service.sh"
         [8082]="/data/watermark_service/start_watermark_service.sh"
         [8081]="/data/watermark_service/start_watermark_service.sh"
         [8083]="/data/watermark_service/start_watermark_service.sh" # Added for new instance
     )
     ```
   - Save the `server_monitoring.sh` script.
   - Execute `sudo systemctl restart server-monitor.service`.

6. **Update Prometheus/Grafana (Monitoring) for New Instance:** Scrape configurations and grafana dashboards need updating for the new instance.
    - Add the new target `v4465a.unx.vstage.co:8083` in the **/etc/prometheus/prometheus.yml** file.
    ```
    scrape_configs:
      - job_name: 'watermark-service'
         scheme: https
         metrics_path: '/actuator/prometheus'
         tls_config:
            insecure_skip_verify: true
         static_configs:
            - targets:
            - 'v4465a.unx.vstage.co:8081'
            - 'v4465a.unx.vstage.co:8082'
            - 'v4465a.unx.vstage.co:8089'
            - 'v4465a.unx.vstage.co:8083' # New instance
     ```
    - Execute the command `sudo systemctl restart prometheus` to reload prometheus service.

7. **Update Other Monitoring & Cron Scripts (If Necessary):**
   - If using the legacy `/data/watermark_service/monitor_watermark_service.sh`, update it for port 8083.
   - If you want 8083 to be a default port for `start_watermark_service.sh` (when run with no args), edit its `DEFAULT_PORTS` array.
   - Ensure log rotation and permission scripts cover the new instance's logs.

**Removing a Scaled Instance (e.g., 8083):**

1. **Remove from Nginx:**
   - Edit Nginx config, remove/comment `server localhost:8083;` from `upstream app_servers`.
   - `sudo nginx -t` then `sudo systemctl reload nginx`.
2. **Stop the Instance:**
   ```bash
   PID_TO_KILL=$(lsof -t -i :8083)
   if [ -n "$PID_TO_KILL" ]; then
       echo "Stopping instance on port 8083 (PID: $PID_TO_KILL)..."
       sudo kill -15 $PID_TO_KILL
   else
       echo "No instance found running on port 8083."
   fi
   ```
3. **Update `server_monitoring.sh`:** Remove port 8083 from `JAVA_PORTS` and `JAVA_APP_PATHS` and execute the command `sudo systemctl restart server-monitor.service`.
4. **Update Other Monitoring Scripts:** Revert changes made for port 8083 (e.g., in legacy monitor, or `DEFAULT_PORTS` in `start_watermark_service.sh`).
5. **Cleanup (Optional):** Remove old log files.

**Note:** Replace placeholders like `<your_username>`. The functionality heavily relies on the `start_watermark_service.sh` script and potentially the `server_monitoring.sh` script. Understanding their configurations (especially hardcoded paths like `JAR_PATH` and arrays like `JAVA_PORTS`) is critical for version updates, rollbacks, and scaling.
