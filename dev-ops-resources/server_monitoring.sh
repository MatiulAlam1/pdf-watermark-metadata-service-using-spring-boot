#!/bin/bash

# Enhanced System Reliability Monitor Script
# Description: Monitors services, CPU, and storage on RHEL 9.2
# Services: Tomcat, Java apps, Grafana, Prometheus, Loki, Promtail
# Additional: CPU load, storage space, inode usage
# Note: Email alerts removed in favor of enhanced logging

## Configuration Section - Adjust these values for your environment
## ------------------------------------------------------------

# Service Configuration
JAVA_PORTS=(8089 8081 8082)           # Ports used by Java applications
JAVA_HEAP_LIMIT_PERCENT=80            # Heap memory threshold percentage
MAX_RESTART_ATTEMPTS=3                # Max restart attempts before alert
REDIS_PORT=6379
LOG_FILE="/var/log/system_monitor.log" # Main log file path
ALERT_LOG="/var/log/system_monitor_alerts.log" # Critical alerts log

# Systemd service names
GRAFANA_SERVICE="grafana-server"
PROMETHEUS_SERVICE="prometheus"
LOKI_SERVICE="loki"
PROMTAIL_SERVICE="promtail"
REDIS_SERVICE="redis"



# Java application paths
declare -A JAVA_APP_PATHS=(
    [8089]="/data/watermark_service_test/start_watermark_service.sh"
    [8081]="/data/watermark_service/start_watermark_service.sh"
    [8082]="/data/watermark_service/start_watermark_service.sh"
)

# CPU Monitoring Thresholds
CPU_LOAD_THRESHOLD=80                 # Percentage (5-minute average)
CPU_CORE_THRESHOLD=90                 # Percentage (per core)
HIGH_LOAD_DURATION=300                # Seconds before taking action

# Storage Monitoring Thresholds
DISK_USAGE_THRESHOLD=90               # Percentage
INODE_USAGE_THRESHOLD=90              # Percentage
PARTITIONS_USAGE_THRESHOLD=90
CRITICAL_PARTITIONS=("/" "/var" "/opt") # Partitions to monitor
PARTITIONS_ALERT=("/var" "/data" "/tmp")

# Global variables
declare -A RESTART_COUNTS             # Tracks restart attempts
declare -A HIGH_LOAD_TIMESTAMPS       # Tracks high CPU load duration
LAST_CPU_ALERT=0                      # Timestamp of last CPU alert
LAST_STORAGE_ALERT=0                  # Timestamp of last storage alert
ALERT_COOLDOWN=3600                   # Seconds between repeat alerts (1 hour)

## ------------------------------------------------------------
## Function Definitions

# Initialize restart counters and timestamps
initialize_counters() {
    # Service restart counters
    RESTART_COUNTS["java"]=0
    RESTART_COUNTS["$GRAFANA_SERVICE"]=0
    RESTART_COUNTS["$PROMETHEUS_SERVICE"]=0
    RESTART_COUNTS["$LOKI_SERVICE"]=0
    RESTART_COUNTS["$PROMTAIL_SERVICE"]=0
	RESTART_COUNTS["redis"]=0
    
    # CPU load timestamps
    HIGH_LOAD_TIMESTAMPS["5min"]=0
    HIGH_LOAD_TIMESTAMPS["per_core"]=0
    
    log_message "Initialized monitoring counters and timestamps"
}

# Log messages with timestamp
log_message() {
    local timestamp=$(date "+%Y-%m-%d %H:%M:%S")
    echo "[$timestamp] $1" >> "$LOG_FILE"
    logger -t SystemMonitor "$1"  # Also send to syslog
}

# Log critical alerts to separate file and syslog
log_alert() {
    local alert_type="$1"
    local subject="$2"
    local message="$3"
    local current_time=$(date +%s)
    local last_alert=0
    local timestamp=$(date "+%Y-%m-%d %H:%M:%S")

    # Determine which cooldown to check
    case "$alert_type" in
        "cpu") last_alert=$LAST_CPU_ALERT ;;
        "storage") last_alert=$LAST_STORAGE_ALERT ;;
        *) last_alert=0 ;;
    esac

    # Check if we're in cooldown period
    if (( current_time - last_alert < ALERT_COOLDOWN )); then
        log_message "Alert suppressed (cooldown active): $subject"
        return
    fi

    # Format detailed alert message
    local formatted_alert="
===========================================================================
[$timestamp] CRITICAL ALERT: $subject
---------------------------------------------------------------------------
$message
===========================================================================
"

    # Log the alert to both files and syslog with high priority
    echo "$formatted_alert" >> "$ALERT_LOG"
    echo "[$timestamp] CRITICAL: $subject" >> "$LOG_FILE"
    logger -p local0.crit -t SystemMonitor "CRITICAL ALERT: $subject"
    
    # Update the appropriate last alert timestamp
    case "$alert_type" in
        "cpu") LAST_CPU_ALERT=$current_time ;;
        "storage") LAST_STORAGE_ALERT=$current_time ;;
    esac
    
    # Also write alert to console if running interactively
    if [ -t 1 ]; then
        echo -e "\e[1;31m$formatted_alert\e[0m"
    fi
}

# Check if service is running
is_service_running() {
    local service_name=$1
    systemctl is-active --quiet "$service_name"
    return $?
}

# Restart service with safety checks
restart_service() {
    local service_name=$1
    local reason=$2
    
    # Check if we've exceeded max restart attempts
    if [[ ${RESTART_COUNTS[$service_name]} -ge $MAX_RESTART_ATTEMPTS ]]; then
        log_message "CRITICAL: Max restart attempts reached for $service_name"
        log_alert "service" "Service restart limit reached" \
                   "Service $service_name failed $MAX_RESTART_ATTEMPTS times.\n\nCurrent status:\n$(systemctl status $service_name | head -n 10)"
        return 1
    fi

    log_message "Attempting to restart $service_name (Reason: $reason)"
    
    # Execute the restart
    systemctl restart "$service_name"
    local restart_status=$?
    
    # Verify restart was successful
    if [[ $restart_status -eq 0 ]] && is_service_running "$service_name"; then
        ((RESTART_COUNTS[$service_name]++))
        log_message "Successfully restarted $service_name (Attempt ${RESTART_COUNTS[$service_name]}/$MAX_RESTART_ATTEMPTS)"
        return 0
    else
        log_message "FAILED to restart $service_name (Exit code: $restart_status)"
        log_alert "service" "Service restart failed" \
                   "Failed to restart $service_name.\n\nExit code: $restart_status\n\nCurrent status:\n$(systemctl status $service_name | head -n 10)"
        return 1
    fi
}

# Check Java application by port
# Check Java application by port
# Check Java application by port with CPU and memory monitoring
check_java_app() {
    local port=$1
    local app_path=${JAVA_APP_PATHS[$port]}
    
    log_message "Checking Java application on port $port"
    
    # Check if port is listening
    if ss -tulnp | grep -q ":$port "; then
        log_message "Port $port is listening - Java application is running"
        
        # Get process ID
        local pid=$(lsof -i :$port -t | head -n 1)
        if [[ -z "$pid" ]]; then
            log_message "No PID found for Java app on port $port (but port is listening)"
            return
        fi
        
        log_message "Found Java process ID: $pid for port $port"
        
        # ===== Check CPU usage =====
        # Using top in batch mode to get CPU percentage
        local cpu_usage=$(top -b -n 1 -p $pid | tail -n 1 | awk '{print $9}')
        if [[ -z "$cpu_usage" ]]; then
            # Alternative method if top fails
            cpu_usage=$(ps -p $pid -o %cpu --no-headers | tr -d ' ')
        fi
        
        log_message "Java app on port $port current CPU usage: ${cpu_usage}%"
        
        # ===== Check Memory usage =====
        # Get memory usage in KB and convert to MB
        local mem_kb=$(ps -p $pid -o rss --no-headers | tr -d ' ')
        local mem_mb=$((mem_kb / 1024))
        
        # Get max memory allowed in MB
        local max_mem=1024  # Default 1GB threshold
        
        # Calculate percentage
        local mem_percent=0
        if [[ $max_mem -gt 0 ]]; then
            mem_percent=$(echo "scale=2; ($mem_mb * 100) / $max_mem" | bc)
        fi
        
        log_message "Java app on port $port memory usage: ${mem_mb}MB (${mem_percent}% of ${max_mem}MB limit)"
        
        # ===== Check Heap usage using /proc filesystem =====
        local heap_usage=""
        if [[ -r "/proc/$pid/smaps" ]]; then
            # Extract Java heap info from smaps
            local heap_kb=$(grep -A 25 "\[heap\]" /proc/$pid/smaps 2>/dev/null | grep -m 1 "Rss:" | awk '{print $2}')
            if [[ -n "$heap_kb" ]]; then
                local heap_mb=$((heap_kb / 1024))
                heap_usage=$(echo "scale=2; ($heap_mb * 100) / $max_mem" | bc)
                log_message "Java app on port $port heap size: ${heap_mb}MB (${heap_usage}% of max heap)"
            fi
        fi
        
        # If heap_usage couldn't be determined, use overall memory as proxy
        if [[ -z "$heap_usage" ]]; then
            heap_usage=$mem_percent
            log_message "Using overall memory as proxy for heap usage: ${heap_usage}%"
        fi
        
        # ===== Decision logic =====
        # Check if CPU usage exceeds threshold
        local CPU_THRESHOLD=80  # Set CPU threshold to 80%
        
        if (( $(echo "$cpu_usage > $CPU_THRESHOLD" | bc -l) )); then
            log_message "Java app on port $port CPU usage ${cpu_usage}% exceeds threshold ${CPU_THRESHOLD}%"
            restart_java_app $port "High CPU usage (${cpu_usage}%)"
            return
        fi
        
        # Check if memory usage exceeds threshold
        if (( $(echo "$mem_percent > $JAVA_HEAP_LIMIT_PERCENT" | bc -l) )); then
            log_message "Java app on port $port memory usage ${mem_percent}% exceeds threshold ${JAVA_HEAP_LIMIT_PERCENT}%"
            restart_java_app $port "High memory usage (${mem_mb}MB, ${mem_percent}%)"
            return
        fi
        
        # Check if heap usage exceeds threshold
        if [[ -n "$heap_usage" ]] && (( $(echo "$heap_usage > $JAVA_HEAP_LIMIT_PERCENT" | bc -l) )); then
            log_message "Java app on port $port heap usage ${heap_usage}% exceeds threshold ${JAVA_HEAP_LIMIT_PERCENT}%"
            restart_java_app $port "Heap memory overflow (${heap_usage}%)"
            return
        fi
        
        log_message "Java app on port $port is healthy (CPU: ${cpu_usage}%, Memory: ${mem_percent}%)"
        
    else
        log_message "Java app on port $port is not listening"
        restart_java_app $port "Port not listening"
    fi
}

# Restart Java application
restart_java_app() {
    local port=$1
    local reason=$2
    local app_path=${JAVA_APP_PATHS[$port]}
    
    if [[ ${RESTART_COUNTS["java"]} -ge $MAX_RESTART_ATTEMPTS ]]; then
        log_message "CRITICAL: Max restart attempts reached for Java apps"
        log_alert "service" "Java app restart limit reached" \
                   "Java applications failed $MAX_RESTART_ATTEMPTS times.\n\nPort $port is affected.\n\nManual intervention required."
        return 1
    fi

    log_message "Restarting Java app on port $port (Reason: $reason)"
    
    # Kill existing process if found
    local pid=$(lsof -i :$port -t | head -n 1)
    if [[ -n "$pid" ]]; then
        kill -9 "$pid"
        sleep 2
    fi
    
    # Start the application with proper logging
    nohup sh "$app_path" $port >> "/var/log/java_app_$port.log" 2>&1 &
    local java_status=$?
    
    # Verify startup
    sleep 5  # Allow time for startup
    
    if [[ $java_status -eq 0 ]] && ss -tulnp | grep -q ":$port "; then
        ((RESTART_COUNTS["java"]++))
        log_message "Successfully restarted Java app on port $port (Attempt ${RESTART_COUNTS["java"]}/$MAX_RESTART_ATTEMPTS)"
    else
        log_message "FAILED to restart Java app on port $port (Exit code: $java_status)"
        log_alert "service" "Java app restart failed" \
                   "Failed to restart Java application on port $port.\n\nExit code: $java_status\n\nCheck /var/log/java_app_$port.log for details."
    fi
}

# Monitor CPU usage and load
monitor_cpu() {
    local current_time=$(date +%s)
    local need_alert=0
    local alert_message=""
    
    # Get 5-minute load average
    local load_5min=$(uptime | awk -F'[a-z]:' '{print $2}' | awk -F, '{print $2}' | xargs)
    local cores=$(nproc)
    local load_percent=$(echo "($load_5min * 100) / $cores" | bc)
    
    # Check overall system load
    if (( $(echo "$load_percent > $CPU_LOAD_THRESHOLD" | bc -l) )); then
        if (( ${HIGH_LOAD_TIMESTAMPS["5min"]} == 0 )); then
            HIGH_LOAD_TIMESTAMPS["5min"]=$current_time
        elif (( current_time - ${HIGH_LOAD_TIMESTAMPS["5min"]} > HIGH_LOAD_DURATION )); then
            need_alert=1
            alert_message+="High system load detected for more than $((HIGH_LOAD_DURATION/60)) minutes.\n"
            alert_message+="5-minute load: $load_5min (${load_percent}% of ${cores} cores)\n"
        fi
    else
        HIGH_LOAD_TIMESTAMPS["5min"]=0
    fi
    
    # Check per-core usage using /proc/stat
    local high_cores=0
    local core_count=0

    # Read CPU stats from /proc/stat
    old_stats=$(cat /proc/stat | grep "^cpu[0-9]")
    sleep 1  # Wait to calculate delta
    new_stats=$(cat /proc/stat | grep "^cpu[0-9]")

    # Process each CPU core's stats
    while IFS= read -r new_line; do
        core_id=$(echo $new_line | awk '{print $1}')
        old_line=$(echo "$old_stats" | grep "$core_id ")
        
        # Extract values
        new_user=$(echo $new_line | awk '{print $2}')
        new_nice=$(echo $new_line | awk '{print $3}')
        new_sys=$(echo $new_line | awk '{print $4}')
        new_idle=$(echo $new_line | awk '{print $5}')
        
        old_user=$(echo $old_line | awk '{print $2}')
        old_nice=$(echo $old_line | awk '{print $3}')
        old_sys=$(echo $old_line | awk '{print $4}')
        old_idle=$(echo $old_line | awk '{print $5}')
        
        # Calculate deltas
        delta_user=$((new_user - old_user))
        delta_nice=$((new_nice - old_nice))
        delta_sys=$((new_sys - old_sys))
        delta_idle=$((new_idle - old_idle))
        
        # Calculate total and usage
        delta_total=$((delta_user + delta_nice + delta_sys + delta_idle))
        usage=0
        if [[ $delta_total -gt 0 ]]; then
            usage=$(( 100 * (delta_total - delta_idle) / delta_total ))
        fi
        
        log_message "DEBUG: Core $core_id usage: $usage%"
        
        # Check if this core is high
        if [[ $usage -gt $CPU_CORE_THRESHOLD ]]; then
            ((high_cores++))
        fi
        ((core_count++))
        
    done <<< "$new_stats"
    
    if (( high_cores > core_count/2 )); then  # If more than half cores are overloaded
        if (( ${HIGH_LOAD_TIMESTAMPS["per_core"]} == 0 )); then
            HIGH_LOAD_TIMESTAMPS["per_core"]=$current_time
        elif (( current_time - ${HIGH_LOAD_TIMESTAMPS["per_core"]} > HIGH_LOAD_DURATION )); then
            need_alert=1
            alert_message+="High per-core CPU usage detected for more than $((HIGH_LOAD_DURATION/60)) minutes.\n"
            alert_message+="${high_cores}/${core_count} cores above ${CPU_CORE_THRESHOLD}% utilization.\n"
        fi
    else
        HIGH_LOAD_TIMESTAMPS["per_core"]=0
    fi
    
    # Send alert if needed
    if (( need_alert )); then
        local top_processes=$(ps -eo pid,ppid,cmd,%mem,%cpu --sort=-%cpu | head -n 6)
        alert_message+="\nTop CPU processes:\n$top_processes"
        
        log_alert "cpu" "High CPU Load Detected" "$alert_message"
        log_message "High CPU load detected - alert logged"
    fi
}

# Monitor disk space and inodes
monitor_storage() {
    local need_alert=0
    local alert_message=""
    local current_time=$(date +%s)
    
    # Check each critical partition
    for partition in "${CRITICAL_PARTITIONS[@]}"; do
        # Skip non-existent partitions
        if [[ ! -d "$partition" ]]; then
            continue
        fi
        
        # Get disk usage
        local disk_usage=$(df -h "$partition" | awk 'NR==2 {print $5}' | tr -d '%')
        local inode_usage=$(df -i "$partition" | awk 'NR==2 {print $5}' | tr -d '%')
        
        # Check disk space
        if [[ $disk_usage -ge $DISK_USAGE_THRESHOLD ]]; then
            need_alert=1
            alert_message+="Partition $partition disk usage: ${disk_usage}% (threshold: ${DISK_USAGE_THRESHOLD}%)\n"
            
            # Find largest directories if we're at critical levels
            if [[ $disk_usage -ge 90 ]]; then
                local large_dirs=$(du -hx "$partition" --max-depth=3 2>/dev/null | sort -rh | head -n 5)
                alert_message+="Top 5 directories by size:\n$large_dirs\n\n"
            fi
        fi
        
        # Check inode usage
        if [[ $inode_usage -ge $INODE_USAGE_THRESHOLD ]]; then
            need_alert=1
            alert_message+="Partition $partition inode usage: ${inode_usage}% (threshold: ${INODE_USAGE_THRESHOLD}%)\n"
        fi
    done
    
    # Send alert if needed
    if (( need_alert )); then
        log_alert "storage" "Storage Usage Warning" "$alert_message"
        log_message "High storage usage detected - alert logged"
    fi
}



# Monitor storage and send email alert
monitor_storage_for_partitions(){
  local alert_message=""
  local need_alert=0
    for partition in "${PARTITIONS_ALERT[@]}"; do
        if [[ ! -d "$partition" ]]; then
            continue
        fi

        local usage=$(df -h "$partition" | awk 'NR==2 {print $5}' | tr -d '%')

        if [[ $usage -ge $PARTITIONS_USAGE_THRESHOLD ]]; then
            need_alert=1
            alert_message+="Partition $partition usage is ${usage}% (exceeds threshold of ${PARTITIONS_USAGE_THRESHOLD}%)\n"
        else
            echo "$(date '+%Y-%m-%d %H:%M:%S') - Partition $partition usage is ${usage}% (below threshold)" >> "$ALERT_LOG"
        fi
    done

    # Send alert if needed
    if (( need_alert )); then
        log_alert "storage" "Storage Usage Warning" "$alert_message"
    fi
    # Log the alert
    echo "monitor_storage_for_partitions: Storage monitoring completed" >> "$ALERT_LOG"
}

# Monitor system memory usage
monitor_system_memory() {
    log_message "Checking system memory usage"
    
    # Get memory information from /proc/meminfo
    local total_kb=$(grep MemTotal /proc/meminfo | awk '{print $2}')
    local available_kb=$(grep MemAvailable /proc/meminfo | awk '{print $2}')
    local free_kb=$(grep MemFree /proc/meminfo | awk '{print $2}')
    local buffers_kb=$(grep Buffers /proc/meminfo | awk '{print $2}')
    local cached_kb=$(grep "^Cached:" /proc/meminfo | awk '{print $2}')
    
    # Calculate used memory and usage percentage
    local used_kb=$((total_kb - free_kb - buffers_kb - cached_kb))
    if [[ -z "$available_kb" ]]; then
        # For older kernels that don't have MemAvailable
        available_kb=$((free_kb + buffers_kb + cached_kb))
    fi
    
    # Convert to MB for readability
    local total_mb=$((total_kb / 1024))
    local used_mb=$((used_kb / 1024))
    local available_mb=$((available_kb / 1024))
    
    # Calculate percentage
    local mem_percent=$(echo "scale=2; ($used_kb * 100) / $total_kb" | bc)
    
    log_message "System memory: ${used_mb}MB used of ${total_mb}MB total (${mem_percent}%)"
    log_message "Available memory: ${available_mb}MB"
    
    # Set threshold for system memory (adjust as needed)
    local MEM_THRESHOLD=90
    
    # Check if system memory usage exceeds threshold
    if (( $(echo "$mem_percent > $MEM_THRESHOLD" | bc -l) )); then
        # Alert on high memory usage
        log_message "WARNING: System memory usage (${mem_percent}%) exceeds threshold (${MEM_THRESHOLD}%)"
        
        # Get top memory consuming processes
        local top_memory_processes=$(ps -eo pid,ppid,cmd,%mem --sort=-%mem | head -n 6)
        
        local alert_message="System memory usage is critical: ${mem_percent}% (threshold: ${MEM_THRESHOLD}%)\n"
        alert_message+="Total memory: ${total_mb}MB, Used: ${used_mb}MB, Available: ${available_mb}MB\n\n"
        alert_message+="Top memory-consuming processes:\n$top_memory_processes"
        
        log_alert "memory" "High System Memory Usage" "$alert_message"
        
        # Check for specific memory issues
        if [[ -r "/proc/sys/vm/drop_caches" ]]; then
            log_message "Consider freeing page cache with: echo 3 > /proc/sys/vm/drop_caches"
        fi
        
        # Check swap usage
        local swap_total=$(grep SwapTotal /proc/meminfo | awk '{print $2}')
        local swap_free=$(grep SwapFree /proc/meminfo | awk '{print $2}')
        
        if [[ $swap_total -gt 0 ]]; then
            local swap_used=$((swap_total - swap_free))
            local swap_percent=$(echo "scale=2; ($swap_used * 100) / $swap_total" | bc)
            
            if (( $(echo "$swap_percent > 80" | bc -l) )); then
                log_message "WARNING: Swap usage is high (${swap_percent}%)"
                alert_message+="\nSwap usage is high: ${swap_percent}%"
            fi
        fi
    fi
    
    # Check for memory leaks in processes
    for port in "${JAVA_PORTS[@]}"; do
        local pid=$(lsof -i :$port -t | head -n 1)
        if [[ -n "$pid" ]]; then
            # Track memory growth over time (simplified)
            local pid_mem=$(ps -p $pid -o rss --no-headers | tr -d ' ')
            local pid_mem_mb=$((pid_mem / 1024))
            
            log_message "Java app on port $port is using ${pid_mem_mb}MB of memory"
        fi
    done
    
    return 0
}


check_redis() {
    local port=$1  # Redis port passed as an argument
    log_message "Checking Redis application on port $port"
	

    # ===== Check if Port Is Listening =====
    if ss -tulnp | grep -q ":$port "; then
        log_message "Port $port is listening - Redis is running"
        # Get Process ID
        local pid=$(lsof -i :$port -t | head -n 1)
        if [[ -z "$pid" ]]; then
            log_message "No PID found for Redis on port $port (but port is listening)"
            restart_redis $port "No process found for Redis, restarting."
            return
        fi
        
        log_message "Found Redis process ID: $pid for port $port"

        # ===== Check CPU Usage =====
        # Using 'top' in batch mode to get CPU percentage
		local cpu_usage=$(top -b -n 2 -d 0.5 -p $pid | grep "$pid" | tail -n 1 | awk '{print $9}')

        if [[ -z "$cpu_usage" ]]; then
            # Fallback method if 'top' does not provide output
			echo "Fallback method if 'top' does not provide output"
            cpu_usage=$(ps -p $pid -o %cpu --no-headers | tr -d ' ')
        fi

        log_message "Redis on port $port current CPU usage: ${cpu_usage}%"
        
        # Define CPU threshold
        local CPU_THRESHOLD=80  # Set the CPU threshold to 80%
        if (( $(echo "$cpu_usage > $CPU_THRESHOLD" | bc -l) )); then
            log_message "Redis on port $port CPU usage ${cpu_usage}% exceeds threshold ${CPU_THRESHOLD}%"
            restart_redis $port "High CPU usage (${cpu_usage}%)"
            return
        fi

        # ===== Check Memory Usage =====
        # Use Redis CLI INFO command to fetch memory info
		local redis_info=$(redis-cli -p $port INFO memory | tr -d '\r')		
		local used_memory=$(echo "$redis_info" | grep "used_memory:" | awk -F: '{print $2}')
        local used_memory_rss=$(echo "$redis_info" | grep "used_memory_rss:" | awk -F: '{print $2}')

        # Convert memory usage from bytes to MB
        local used_memory_mb=$((used_memory / 1024 / 1024))
        local used_memory_rss_mb=$((used_memory_rss / 1024 / 1024))

        log_message "Redis on port $port memory usage: ${used_memory_mb}MB (${used_memory_rss_mb}MB RSS)"

        # Define memory limit (in MB)
        local MAX_MEMORY_MB=1024  # Default set to 1GB
        local memory_percent=$(echo "scale=2; ($used_memory_mb * 100) / $MAX_MEMORY_MB" | bc)

        # Check if memory usage exceeds threshold
        local MEMORY_THRESHOLD_PERCENT=90  # Threshold in percent
        if (( $(echo "$memory_percent > $MEMORY_THRESHOLD_PERCENT" | bc -l) )); then
            log_message "Redis on port $port memory usage ${memory_percent}% exceeds threshold ${MEMORY_THRESHOLD_PERCENT}%"
            restart_redis $port "High memory usage (${used_memory_mb}MB out of ${MAX_MEMORY_MB}MB)"
            return
        fi

        log_message "Redis on port $port is healthy (CPU: ${cpu_usage}%, Memory: ${memory_percent}%)"
        
    else
        log_message "Redis on port $port is not listening"
        restart_redis $port "Port not listening"
    fi
}


# Restart Redis application
restart_redis() {
    local port=$1      # Redis port
    local reason=$2    # Reason for restarting the Redis instance
    local service_name="redis"  # Default Redis service name; customize as needed

    log_message "Restarting Redis on port $port (Reason: $reason)"
    # Check if the maximum restart attempts have been reached
    if [[ ${RESTART_COUNTS["redis"]} -ge $MAX_RESTART_ATTEMPTS ]]; then
        log_message "CRITICAL: Max restart attempts reached for Redis"
		log_alert "service" "Service restart limit reached" \
		   "Service $REDIS_SERVICE failed $MAX_RESTART_ATTEMPTS times.\n\nCurrent status:\n$(systemctl status $REDIS_SERVICE | head -n 10)"		
        return 1
    fi

    # Attempt to stop the Redis process associated with the port
    local pid=$(lsof -i :$port -t | head -n 1)
    if [[ -n "$pid" ]]; then
        log_message "Killing Redis process ID $pid on port $port"
        kill -9 "$pid"
        sleep 2
    else
        log_message "No Redis process found running on port $port"
    fi

    # Restart the Redis service via systemctl
    log_message "Restarting Redis using systemctl for service '$REDIS_SERVICE'"
    sudo systemctl restart "$REDIS_SERVICE"

    # Wait to verify the startup
    sleep 5

    # Verify if Redis has successfully started and the port is listening

    if ss -tulnp | grep -q ":$port "; then
        ((RESTART_COUNTS["redis"]++))
        log_message "Successfully restarted Redis (via systemctl) on port $port (Attempt ${RESTART_COUNTS["redis"]}/$MAX_RESTART_ATTEMPTS)"
    else
        log_message "FAILED to restart Redis (via systemctl) on port $port"
		log_alert "service" "Service restart failed" \
		   "Failed to restart $REDIS_SERVICE.\n\nExit code: $restart_status\n\nCurrent status:\n$(systemctl status $REDIS_SERVICE | head -n 10)"		
    fi
}


# Main monitoring function
monitor_all() {
    log_message "Starting monitoring cycle.."
    # Service monitoring
    initialize_counters
    
    for port in "${JAVA_PORTS[@]}"; do
        check_java_app "$port"
    done
    
	check_redis "$REDIS_PORT"
	
    if ! is_service_running "$GRAFANA_SERVICE"; then
        log_message "Grafana service is not running"
        restart_service "$GRAFANA_SERVICE" "Service not running"
    fi
    
    if ! is_service_running "$PROMETHEUS_SERVICE"; then
        log_message "Prometheus service is not running"
        restart_service "$PROMETHEUS_SERVICE" "Service not running"
    fi
    
    if ! is_service_running "$LOKI_SERVICE"; then
        log_message "Loki service is not running"
        restart_service "$LOKI_SERVICE" "Service not running"
    fi
    
    if ! is_service_running "$PROMTAIL_SERVICE"; then
        log_message "Promtail service is not running"
        restart_service "$PROMTAIL_SERVICE" "Service not running"
    fi
    
    # System resource monitoring
    monitor_cpu
    monitor_storage
    monitor_storage_for_partitions
    monitor_system_memory
    
    log_message "Monitoring cycle completed"
}

## ------------------------------------------------------------
## Main Script Execution

main() {
    # Ensure script runs as root
    if [[ $EUID -ne 0 ]]; then
        echo "This script must be run as root" >&2
        exit 1
    fi
    
    # Create log directories if needed
    mkdir -p "$(dirname "$LOG_FILE")"
    mkdir -p "$(dirname "$ALERT_LOG")"
    touch "$LOG_FILE"
    touch "$ALERT_LOG"
    
    # Initialize counters
    # initialize_counters
	
    log_message "System reliability monitor started"

    monitor_all
    
    # # Main monitoring loop
    while true; do
        monitor_all
        sleep 60  # Check every minute
    done
}

# Start the script
main
