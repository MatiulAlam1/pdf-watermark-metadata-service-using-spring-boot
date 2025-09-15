## Watermark Service Deployment & Monitoring


| Field           | Details                               |
| --------------- | ------------------------------------- |
| **Author**      | Matiul Alam/BJIT AD                 |
| **Date**        | 2025-06-25                            |
| **Requirement ID**| https://cimbug.valmet.com/cimbug/app_cimbug/cimbug_report.php?id=20108 |
| **Service Version**| 1.0.1                                 |        

## Description

This document outlines the steps to build the Watermark Service JAR file locally, deploy it to the servers `v4465a.unx.vstage.co` and `v4520a.unx.vstage.co` for both Test and Production environments. The current version being deployed is **1.0.1**, requiring **Java 17**. The JAR file is uploaded as `valmet-watermark-service-1.0.1.jar` and then renamed to `valmet-watermark-service.jar` on the servers. The start script uses this fixed JAR name and does not need to be updated for new versions.

**Key Environment Details:**

- **Test Environment:**
  - **v4465a.unx.vstage.co:** Runs on port `8089` using the Spring Boot `test` profile.
  - **v4520a.unx.vstage.co:** Runs on port `8090` using the Spring Boot `test` profile.
- **Production Environment:**
  - **v4465a.unx.vstage.co:** Runs on ports `8081` & `8082` using the Spring Boot `prod` profile.
  - **v4520a.unx.vstage.co:** Runs on ports `8083` & `8084` using the Spring Boot `prod` profile.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Building the Application (Locally)](#building-the-application-locally)
3. [Deployment to Servers v4465a.unx.vstage.co and v4520a.unx.vstage.co](#deployment-to-servers-v4465aunxvstageco-and-v4520aunxvstageco)
   - [Test Environment](#test-environment)
   - [Production Environment](#production-environment)
4. [Restarting the Service (Using Sudo)](#restarting-the-service-using-sudo)
5. [Verification](#verification)
6. [Configuration](#configuration)
7. [Load Balancer (Nginx)](#load-balancer-nginx)
8. [Monitoring (Prometheus & Grafana)](#monitoring-prometheus--grafana)
9. [Scheduled Tasks / Cron Jobs](#scheduled-tasks--cron-jobs)
10. [Server-Level System Reliability Monitoring (`server_monitoring.sh`)](#server-level-system-reliability-monitoring-server_monitoringsh)
11. [Rollback Strategy (Basic)](#rollback-strategy-basic)
12. [Horizontal Scaling: Adding a New Production Instance](#horizontal-scaling-adding-a-new-production-instance)

## Prerequisites

**For Building (Local Machine):**

- **Java Development Kit (JDK):** **Version 17** or higher.
- **Apache Maven:** **Version 3.4.1 or 3.4.5**.
- **CVS Client:** To check out the source code.
- **CVS Access:** Credentials and access to the project's CVS repository.

**For Deployment (Servers: `v4465a.unx.vstage.co` and `v4520a.unx.vstage.co`):**

- **SSH Access:** SSH access to both `v4465a.unx.vstage.co` and `v4520a.unx.vstage.co` with a user account (`<your_username>`).
- **Sudo Privileges:** The user `<your_username>` must have `sudo` privileges to execute the `start_watermark_service.sh` script and potentially the `server_monitoring.sh` script.
- **Java Runtime Environment (JRE):** **Version 17** or higher installed on both servers.
- **Permissions:**
  - Write permissions for `<your_username>` to the deployment directories:
    - `/data/watermark_service_test/`
    - `/data/watermark_service/`
  - The user the service runs as (likely `root` due to `sudo` execution of the scripts, unless scripts switch user) needs permissions to write application logs to:
    - `/var/log/watermark-service-test/`
    - `/var/log/watermark-service/`
  - Permissions to write to `/var/log/start_watermark_service.log` (used by `start_watermark_service.sh`) and `/var/log/system_monitor.log`, `/var/log/system_monitor_alerts.log` (used by `server_monitoring.sh`).
- **Restart Scripts:**
  - **`start_watermark_service.sh`**: This script (one for test, one for production) must exist in the respective deployment directories and be executable. It uses a fixed JAR file name `valmet-watermark-service.jar`.
  - **`server_monitoring.sh`**: An "Enhanced System Reliability Monitor Script" (e.g., `/opt/scripts/server_monitoring.sh`) is used for broader server health checks.
- **Deployed JAR File:** The JAR file is uploaded as `valmet-watermark-service-<version>.jar` and renamed to `valmet-watermark-service.jar` in the deployment directory.
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
   *(Ensure your project's `pom.xml` is configured to produce the desired version, e.g., `1.0.1`, and uses Java 17. The final artifact name should be `valmet-watermark-service-1.0.1.jar`.)*
   ```bash
   mvn clean package
   ```
6. **Locate the JAR file:** The build process should generate the specific JAR file, for example:
   - `target/valmet-watermark-service-1.0.1.jar`
   *Verify this file exists.*

## Deployment to Servers `v4465a.unx.vstage.co` and `v4520a.unx.vstage.co`

This process involves transferring the newly built JAR file to the servers, renaming it to `valmet-watermark-service.jar`, and then running the restart script using `sudo`. The start script uses the fixed JAR name `valmet-watermark-service.jar` and does not need to be updated for new versions.

### Test Environment

- **Servers:**
  - **v4465a.unx.vstage.co:**
    - **Port:** `8089`
    - **Deployment Directory:** `/data/watermark_service_test/`
    - **Spring Boot Profile:** `test`
    - **Restart Script:** `/data/watermark_service_test/start_watermark_service.sh`
    - **Application Log File:** `/var/log/watermark-service-test/instance_8089_logger.log`
  - **v4520a.unx.vstage.co:**
    - **Port:** `8090`
    - **Deployment Directory:** `/data/watermark_service_test/`
    - **Spring Boot Profile:** `test`
    - **Restart Script:** `/data/watermark_service_test/start_watermark_service.sh`
    - **Application Log File:** `/var/log/watermark-service-test/instance_8090_logger.log`

**Example Deployment Steps (Test - New Version 1.0.1, assuming current is 1.0.0):**

1. **Build `valmet-watermark-service-1.0.1.jar` locally.**
2. **SSH into each server:**
   ```bash
   ssh <your_username>@v4465a.unx.vstage.co
   ssh <your_username>@v4520a.unx.vstage.co
   ```
3. **Backup the existing `valmet-watermark-service.jar` (if it exists):**
   ```bash
   # On each server, in /data/watermark_service_test/
   cd /data/watermark_service_test/
   if [ -f valmet-watermark-service.jar ]; then
       sudo mv valmet-watermark-service.jar valmet-watermark-service.jar.bak_$(date +%Y%m%d%H%M)
   fi
   ```
4. **Transfer the new JAR (from your local machine):**
   ```bash
   scp target/valmet-watermark-service-1.0.1.jar <your_username>@v4465a.unx.vstage.co:/data/watermark_service_test/
   scp target/valmet-watermark-service-1.0.1.jar <your_username>@v4520a.unx.vstage.co:/data/watermark_service_test/
   ```
5. **Rename the uploaded JAR to `valmet-watermark-service.jar`:**
   ```bash
   # On each server
   cd /data/watermark_service_test/
   sudo mv valmet-watermark-service-1.0.1.jar valmet-watermark-service.jar
   ```
6. **Restart the service using sudo:**
   ```bash
   # On v4465a.unx.vstage.co
   cd /data/watermark_service_test/
   sudo ./start_watermark_service.sh 8089

   # On v4520a.unx.vstage.co
   cd /data/watermark_service_test/
   sudo ./start_watermark_service.sh 8090
   ```
7. **Verify:** Check logs for `test` profile activation and service status for ports 8089 and 8090 (see [Verification](#verification)).
8. **Exit SSH:** `exit`

### Production Environment

- **Servers:**
  - **v4465a.unx.vstage.co:**
    - **Ports:** `8081`, `8082`
    - **Deployment Directory:** `/data/watermark_service/`
    - **Spring Boot Profile:** `prod`
    - **Restart Script:** `/data/watermark_service/start_watermark_service.sh`
    - **Application Log Files:**
      - `/var/log/watermark-service/instance_8081_logger.log`
      - `/var/log/watermark-service/instance_8082_logger.log`
  - **v4520a.unx.vstage.co:**
    - **Ports:** `8083`, `8084`
    - **Deployment Directory:** `/data/watermark_service/`
    - **Spring Boot Profile:** `prod`
    - **Restart Script:** `/data/watermark_service/start_watermark_service.sh`
    - **Application Log Files:**
      - `/var/log/watermark-service/instance_8083_logger.log`
      - `/var/log/watermark-service/instance_8084_logger.log`

**Example Deployment Steps (Production - New Version 1.0.1, assuming current was 1.0.0):**

1. **Build `valmet-watermark-service-1.0.1.jar` locally.**
2. **SSH into each server:**
   ```bash
   ssh <your_username>@v4465a.unx.vstage.co
   ssh <your_username>@v4520a.unx.vstage.co
   ```
3. **Backup the existing `valmet-watermark-service.jar` (if it exists):**
   ```bash
   # On each server, in /data/watermark_service/
   cd /data/watermark_service/
   if [ -f valmet-watermark-service.jar ]; then
       sudo mv valmet-watermark-service.jar valmet-watermark-service.jar.bak_$(date +%Y%m%d%H%M)
   fi
   ```
4. **Transfer the new JAR (from your local machine):**
   ```bash
   scp target/valmet-watermark-service-1.0.1.jar <your_username>@v4465a.unx.vstage.co:/data/watermark_service/
   scp target/valmet-watermark-service-1.0.1.jar <your_username>@v4520a.unx.vstage.co:/data/watermark_service/
   ```
5. **Rename the uploaded JAR to `valmet-watermark-service.jar`:**
   ```bash
   # On each server
   cd /data/watermark_service/
   sudo mv valmet-watermark-service-1.0.1.jar valmet-watermark-service.jar
   ```
6. **Restart the service instance(s) using sudo:**
   - **For a rolling update (recommended for zero downtime):**
     ```bash
     # On v4465a.unx.vstage.co
     cd /data/watermark_service/
     # 1. Remove 8081 from Nginx load balancer & reload Nginx (see Nginx section)
     # 2. Restart instance 8081:
     sudo ./start_watermark_service.sh 8081
     # 3. Verify instance 8081 is healthy (logs, health check)
     # 4. Add 8081 back to Nginx, remove 8082 from Nginx & reload Nginx
     # 5. Restart instance 8082:
     sudo ./start_watermark_service.sh 8082
     # 6. Verify instance 8082 is healthy
     # 7. Add 8082 back to Nginx & reload Nginx

     # On v4520a.unx.vstage.co
     cd /data/watermark_service/
     # Repeat similar steps for ports 8083 and 8084
     sudo ./start_watermark_service.sh 8083
     # Verify, adjust Nginx, then:
     sudo ./start_watermark_service.sh 8084
     # Verify, adjust Nginx
     ```
   - **To restart all default production instances at once (causes downtime):**
     ```bash
     # On v4465a.unx.vstage.co
     cd /data/watermark_service/
     sudo ./start_watermark_service.sh # Restarts 8081 and 8082 by default

     # On v4520a.unx.vstage.co
     cd /data/watermark_service/
     sudo ./start_watermark_service.sh # Restarts 8083 and 8084 by default
     ```
7. **Verify:** Check logs for `prod` profile activation and service status on ports 8081, 8082, 8083, and 8084 (see [Verification](#verification)). The `nohup` output of the `start_watermark_service.sh` script itself will be in `/var/log/start_watermark_service.log`.
8. **Exit SSH:** `exit`

## Restarting the Service (Using Sudo)

The `start_watermark_service.sh` script (in `/data/watermark_service/` for production, and a similar one in `/data/watermark_service_test/` for test) handles stopping any running instance on the specified port(s) and starting new ones using the fixed JAR file `valmet-watermark-service.jar`. It activates the appropriate Spring Boot profile (`prod` for production, `test` for test). **It must be executed with `sudo`.**

- **Test:**
  ```bash
  # On v4465a.unx.vstage.co
  ssh <your_username>@v4465a.unx.vstage.co
  cd /data/watermark_service_test/
  sudo ./start_watermark_service.sh 8089
  exit

  # On v4520a.unx.vstage.co
  ssh <your_username>@v4520a.unx.vstage.co
  cd /data/watermark_service_test/
  sudo ./start_watermark_service.sh 8090
  exit
  ```

- **Production:**
  ```bash
  # On v4465a.unx.vstage.co
  ssh <your_username>@v4465a.unx.vstage.co
  cd /data/watermark_service/
  # To restart all default production instances (8081 & 8082)
  sudo ./start_watermark_service.sh
  # Or to restart specific instances
  sudo ./start_watermark_service.sh 8081
  sudo ./start_watermark_service.sh 8082
  exit

  # On v4520a.unx.vstage.co
  ssh <your_username>@v4520a.unx.vstage.co
  cd /data/watermark_service/
  # To restart all default production instances (8083 & 8084)
  sudo ./start_watermark_service.sh
  # Or to restart specific instances
  sudo ./start_watermark_service.sh 8083
  sudo ./start_watermark_service.sh 8084
  exit
  ```

## Verification

After restarting the service, verify it's running correctly on each server:

1. **Check `start_watermark_service.sh` Script's Startup Log:**
   ```bash
   # SSH into the server first
   tail -f /var/log/start_watermark_service.log
   ```

2. ** Check Application Instance Logs:**
   ```bash
   # SSH into the server first: ssh <your_username>@v4465a.unx.vstage.co or v4520a.unx.vstage.co

   # Test
   tail -f /var/log/watermark-service-test/instance_8089_logger.log  # v4465a
   tail -f /var/log/watermark-service-test/instance_8090_logger.log  # v4520a

   # Production
   tail -f /var/log/watermark-service/instance_8081_logger.log  # v4465a
   tail -f /var/log/watermark-service/instance_8082_logger.log  # v4465a
   tail -f /var/log/watermark-service/instance_8083_logger.log  # v4520a
   tail -f /var/log/watermark-service/instance_8084_logger.log  # v4520a
   ```

3. **Check Process Status & JAR Version:**
   ```bash
   # SSH into the server first
   ps aux | grep valmet-watermark-service.jar | grep java
   jps -l | grep valmet-watermark-service
   ```

4. **Check Health Endpoint (if available):**
   - **Test:** `curl https://watermark-service-test.valmet.com/actuator/health` (via Nginx) or `curl https://localhost:<port>/actuator/health` (directly on server)
   - **Production:** `curl https://watermark-service.valmet.com/actuator/health` (via Nginx) or `curl https://localhost:<port>/actuator/health` (directly on server)

5. **Check Monitoring Dashboards:** Review Grafana dashboards for performance metrics and errors.

## Configuration

- Environment-specific configurations are managed *outside* the JAR, primarily using **Spring Boot profiles**.
- The **`start_watermark_service.sh`** script is responsible for:
  - Executing the Java application using the fixed JAR file `valmet-watermark-service.jar`.
  - Activating the correct Spring Boot profile (`test` or `prod`) via the `--spring.profiles.active` argument.
  - Setting the server port for each instance using the `--server.port` argument.
- Profile-specific properties are typically defined in files like `application-test.properties` or `application-prod.properties` located alongside the JAR or packaged within the JAR.
- **Application Log Configuration:** The application itself is configured to use the `server.port` to create distinct log files (e.g., `instance_<port>_logger.log`).

## Load Balancer (Nginx)

Nginx on `v4465a.unx.vstage.co` acts as the master load balancer, distributing traffic to both local instances (ports `8081` and `8082`) and those on `v4520a.unx.vstage.co` (ports `8083` and `8084`). Currently, `v4465a.unx.vstage.co` is the master server because the DNS points to it. If the server `v4465a.unx.vstage.co` goes down, then the DNS can be updated to point to `v4520a.unx.vstage.co`, which also has Nginx installed and can serve requests as a backup.

**Nginx Configuration Overview:**

- **Master Server (`v4465a.unx.vstage.co`):**
  - Listens on port `443` for HTTPS traffic.
  - Uses an `upstream app_servers` block to load balance requests across local and remote instances:
    ```nginx
    upstream app_servers {
      server localhost:8081;              # v4465a production instance 1
      server localhost:8082;              # v4465a production instance 2
      server v4520a.unx.vstage.co:8083;   # v4520a production instance 1
      server v4520a.unx.vstage.co:8084;   # v4520a production instance 2
    }
    ```
  - Proxies requests to the backend servers using `proxy_pass https://app_servers;`.

- **Backup Server (`v4520a.unx.vstage.co`):**
  - Also has Nginx installed, configured similarly to handle local instances (ports `8083` and `8084`).
  - Can serve as a backup in case the master server fails, upon DNS update.

**Testing and Reloading Nginx Configuration:**

After making changes to Nginx configuration files, test and reload Nginx:

1. **Test Configuration:**
   ```bash
   sudo nginx -t
   ```

2. **Reload Nginx:**
   ```bash
   sudo systemctl reload nginx
   ```

**Rolling Updates (Production Environment - Zero Downtime):**

1. Modify Nginx config to mark one backend server down.
2. Test and reload Nginx.
3. Deploy to/restart that instance using `start_watermark_service.sh`.
4. Verify instance.
5. Re-enable instance in Nginx, mark next instance down, test, reload Nginx.
6. Repeat for other instances.
7. Re-enable all instances in Nginx, test, reload.

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

Several automated tasks are configured via cron on both `v4465a.unx.vstage.co` and `v4520a.unx.vstage.co` servers.

1. **Watermark Service Monitoring (Production - Legacy)**
   - **Schedule:** Every 2 minutes (`*/2 * * * *`)
   - **Command:** `/data/watermark_service/monitor_watermark_service.sh`
   - **Log File:** `/var/log/monitor_watermark_service.log`

2. **HashiCorp Vault Monitoring**
   - **Schedule:** Every 1 minute (`*/1 * * * *`)
   - **Command:** `/opt/vault_scripts/monitor_hashicorp_vault_service.sh`
   - **Log File:** `/var/log/monitor_hashicorp_vault_service.log`

3. **Vault Key Rotation**
   - **Schedule:** Monthly, 1st day, 2:00 AM (`0 2 1 * *`)
   - **Command:** `/opt/vault_scripts/rotate_rsa_key_pair_password_in_hashicorp_vault_service.sh`
   - **Log File:** `/var/log/monitor_hashicorp_vault_service.log`

4. **Log Rotation/Cleanup (`log_rotate_and_cleanup.sh`)**
   - **Schedule:** Quarterly, 1st day of month, Midnight (`0 0 1 */3 *`)
   - **Command:** `/opt/scripts/log_rotate_and_cleanup.sh`
   - **Log File:** `/var/log/log_rotation.log`

5. **Log Permissions for Monitoring (`log_permission.sh`)**
   - **Schedule:** Hourly (`0 * * * *`)
   - **Command:** `/usr/local/bin/log_permission.sh`

6. **Grafana Monitoring**
   - **Schedule:** Every 1 minute (`* * * * *`)
   - **Command:** `pgrep grafana || systemctl start grafana-server`

7. **System Reliability Monitor (`server_monitoring.sh`) via systemctl:**
   - **Schedule:** Checks whether the server_monitoring.sh script is running every minute using systemctl.
   - **Command:** `/opt/scripts/check_monitor.sh`
   - **Log File:** `/var/log/system_monitor.log`
   
8. **System Reliability Monitoring Log Rotation/Cleanup (`/opt/scripts/rotate_monitor_logs.sh`)**
   - **Schedule:** Executes every hour.
   - **Command:** `/opt/scripts/rotate_monitor_logs.sh`
   - **Log File:** `/var/log/rotate_monitor_logs.log`

## Server-Level System Reliability Monitoring (`server_monitoring.sh`)

An "Enhanced System Reliability Monitor Script" (e.g., `/opt/scripts/server_monitoring.sh`) is in place for broader server health checks in both servers (`v4465a.unx.vstage.co` and `v4520a.unx.vstage.co`). This script monitors various services, including the Java applications (like Watermark Service instances), systemd services (Grafana, Prometheus, etc.), CPU load, and storage.

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

**Key Configuration Points in `server_monitoring.sh` (e.g.,`v4465a.unx.vstage.co`) :**

- **`JAVA_PORTS=(8089 8082 8081)`:**
  - This array defines which ports the monitor script will check for running Java applications.
  - **Crucially, when scaling the Watermark Service (or other Java apps) to new ports (e.g., adding an instance on a new port like `8085`), this `JAVA_PORTS` array within the `server_monitoring.sh` script MUST be updated to include the new port number.** After adding new port(8085) execute the command **sudo systemctl restart server-monitor.service**. Otherwise, the new instance will not be monitored or auto-restarted by this script.
- **`declare -A JAVA_APP_PATHS=( [8089]="/data/watermark_service_test/start_watermark_service.sh" [8082]="/data/watermark_service/start_watermark_service.sh" [8081]="/data/watermark_service/start_watermark_service.sh" )`:**
  - This associative array maps a Java application's port to its specific start script.
  - When a Java application on a monitored port is found to be down or problematic, this monitor script will attempt to restart it using `sudo sh <app_path> <port>`. For example, if the app on port 8081 fails, it will run `sudo sh /data/watermark_service/start_watermark_service.sh 8081`.
  - **When adding a new Java instance on a new port that needs monitoring (e.g. port `8085`), an entry must be added to `JAVA_APP_PATHS` for that port, pointing to its corresponding start script.**
    ```bash
    # Example: If adding instance on port 8085 using the prod start script
    # In server_monitoring.sh, update JAVA_APP_PATHS:
    # JAVA_APP_PATHS=(
    #    [8089]="/data/watermark_service_test/start_watermark_service.sh"
    #    [8082]="/data/watermark_service/start_watermark_service.sh"
    #    [8081]="/data/watermark_service/start_watermark_service.sh"
    #    [8085]="/data/watermark_service/start_watermark_service.sh" # Add this line
    # )
    # And also add 8085 to the JAVA_PORTS array.
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


## Rollback Strategy (Basic)

If deployment of a new version fails and you need to revert to a previous version, assuming you have backed up the previous `valmet-watermark-service.jar`:

1. **Identify failure:** Check application instance logs, health endpoints, Grafana, and `server_monitoring.sh` logs.
2. **Restore the previous JAR:**
   ```bash
   # On each server, in the deployment directory (e.g., /data/watermark_service/ for production)
   cd /data/watermark_service/
   sudo mv valmet-watermark-service.jar valmet-watermark-service.jar.failed_$(date +%Y%m%d%H%M)
   sudo mv valmet-watermark-service.jar.bak_$(date +%Y%m%d%H%M) valmet-watermark-service.jar
   # Note: Replace $(date +%Y%m%d%H%M) with the actual backup timestamp, e.g., valmet-watermark-service.jar.bak_202506241117
   ```
3. **Restart the service using the start script, which will stop any running instance and start with the restored JAR:**
   ```bash
   sudo ./start_watermark_service.sh
   ```
4. **Verify the service is functional on the previous version.**

## Horizontal Scaling: Adding a New Production Instance

This section describes how to add a new production Watermark Service instance, for example, running on port 8085, to the existing setup on `v4465a.unx.vstage.co`. This assumes the server has sufficient resources. The new instance will run the JAR version currently specified in the `JAR_PATH` of `/data/watermark_service/start_watermark_service.sh`.

**Prerequisites & Considerations:**

- **Server Resources:** Ensure `v4465a.unx.vstage.co` has adequate CPU, memory, and disk space.
- **`start_watermark_service.sh` Script Usage:**
  - The existing `/data/watermark_service/start_watermark_service.sh` script (from the prompt) can start an instance on a new port by passing the port number as an argument (e.g., `sudo ./start_watermark_service.sh 8085`).
  - If you want the new port (e.g., 8085) to become part of the default set of ports managed when `start_watermark_service.sh` is run without any arguments, you would need to edit that script and add the new port to its `DEFAULT_PORTS` array:
    ```bash
    # Inside /data/watermark_service/start_watermark_service.sh
    # DEFAULT_PORTS=(8081 8082)
    # Change to:
    # DEFAULT_PORTS=(8081 8082 8085)
    ```
- **Application Logging:** The Spring Boot application is configured (e.g., via `logback.spring.xml`) to create a distinct log file for the new instance, typically based on the port (e.g., `/var/log/watermark-service/instance_8085_logger.log`).
- **Log Directory and Permissions:** Ensure `/var/log/watermark-service/` exists. The `log_permission.sh` cron job should correctly set permissions for the new log file.

**Steps to Add a New Production Instance (e.g., on Port 8085):**

1. **Update Nginx Configuration:**
   - SSH into `v4465a.unx.vstage.co`.
   - Edit the Nginx configuration file (e.g., `/etc/nginx/nginx.conf`) with the `upstream app_servers` block.
   - Add the new server instance:
     ```nginx
     upstream app_servers {
       server localhost:8081;
       server localhost:8082;
       server v4520a.unx.vstage.co:8083;
       server v4520a.unx.vstage.co:8084;
       server localhost:8085; # New instance
     }
     ```
   - Test: `sudo nginx -t`. If successful, reload: `sudo systemctl reload nginx`.

2. **Ensure Application JAR is Present:**
   - Verify that the JAR file specified in `JAR_PATH` within `/data/watermark_service/start_watermark_service.sh` (e.g., `valmet-watermark-service-1.0.1.jar`) exists in `/data/watermark_service/`. This JAR will be used.

3. **Start the New Application Instance:**
   - Navigate to the production deployment directory:
     ```bash
     cd /data/watermark_service/
     ```
   - Execute the `start_watermark_service.sh` script with `sudo`, providing the new port:
     ```bash
     sudo ./start_watermark_service.sh 8085
     ```
     *(This uses the JAR from `JAR_PATH` in the script, for port 8085, with prod profile. Script output to `/var/log/start_watermark_service.log`)*

4. **Verify the New Instance:**
   - `start_watermark_service.sh` Log: `tail -f /var/log/start_watermark_service.log`
   - Application Log: `tail -f /var/log/watermark-service/instance_8085_logger.log` (Check for prod profile, no errors).
   - Process Status: `ps aux | grep valmet-watermark-service.*.jar | grep "port=8085"` and `jps -l | grep valmet-watermark-service`.
   - Health Endpoint (Directly): `curl https://localhost:8085/actuator/health` (Look for `"status":"UP"`).
   - Health Endpoint (Via Nginx): `curl https://watermark-service.valmet.com/actuator/health`.
   - Monitoring: Check Grafana/Prometheus. Check `server_monitoring.sh` logs (`/var/log/system_monitor.log`) on its next run to see if it correctly identifies the new instance.

5. **Update `server_monitoring.sh` (if used for automated monitoring):**
   - Edit `/opt/scripts/server_monitoring.sh` (or its actual path).
   - Add the new port to `JAVA_PORTS`:
     ```bash
     JAVA_PORTS=(8089 8082 8081 8085) # Added 8085
     ```
   - Add the new port and its start script path to `JAVA_APP_PATHS`:
     ```bash
     declare -A JAVA_APP_PATHS=(
         [8089]="/data/watermark_service_test/start_watermark_service.sh"
         [8082]="/data/watermark_service/start_watermark_service.sh"
         [8081]="/data/watermark_service/start_watermark_service.sh"
         [8085]="/data/watermark_service/start_watermark_service.sh" # Added for new instance
     )
     ```
   - Save the `server_monitoring.sh` script.
   - Execute `sudo systemctl restart server-monitor.service`.

6. **Update Prometheus/Grafana (Monitoring) for New Instance:** Scrape configurations and grafana dashboards need updating for the new instance.
    - Add the new target `v4465a.unx.vstage.co:8085` in the **/etc/prometheus/prometheus.yml** file.
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
            - 'v4520a.unx.vstage.co:8083'
            - 'v4520a.unx.vstage.co:8084'
            - 'v4520a.unx.vstage.co:8090'
            - 'v4465a.unx.vstage.co:8085' # New instance
     ```
    - Execute the command `sudo systemctl restart prometheus` to reload prometheus service.

7. **Update Other Monitoring & Cron Scripts (If Necessary):**
   - If using the legacy `/data/watermark_service/monitor_watermark_service.sh`, update it for port 8085.
   - If you want 8085 to be a default port for `start_watermark_service.sh` (when run with no args), edit its `DEFAULT_PORTS` array.
   - Ensure log rotation and permission scripts cover the new instance's logs.

**Removing a Scaled Instance (e.g., 8085):**

1. **Remove from Nginx:**
   - Edit Nginx config, remove/comment `server localhost:8085;` from `upstream app_servers`.
   - `sudo nginx -t` then `sudo systemctl reload nginx`.
2. **Stop the Instance:**
   ```bash
   PID_TO_KILL=$(lsof -t -i :8085)
   if [ -n "$PID_TO_KILL" ]; then
       echo "Stopping instance on port 8085 (PID: $PID_TO_KILL)..."
       sudo kill -15 $PID_TO_KILL
   else
       echo "No instance found running on port 8085."
   fi
   ```
3. **Update `server_monitoring.sh`:** Remove port 8085 from `JAVA_PORTS` and `JAVA_APP_PATHS` and execute the command `sudo systemctl restart server-monitor.service`.
4. **Update Other Monitoring Scripts:** Revert changes made for port 8085 (e.g., in legacy monitor, or `DEFAULT_PORTS` in `start_watermark_service.sh`).
5. **Cleanup (Optional):** Remove old log files.

**Note:** Replace placeholders like `<your_username>`. The functionality heavily relies on the `start_watermark_service.sh` script and potentially the `server_monitoring.sh` script. Understanding their configurations (especially hardcoded paths like `JAR_PATH` and arrays like `JAVA_PORTS`) is critical for version updates, rollbacks, and scaling.