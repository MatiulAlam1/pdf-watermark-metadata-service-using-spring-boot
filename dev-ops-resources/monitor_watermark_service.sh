#!/bin/bash

# Function to check if a port is open and restart service if not
check_and_restart() {
    local port=$1
    local restart_command=$2
    nc -z localhost $port &> /dev/null
    if [ $? -ne 0 ]; then
        echo "Port $port is closed. Attempting to restart service..."
        eval $restart_command
    else
        echo "Port $port is open."
    fi
}

# Monitoring commands
restart_command_8081="/data/watermark_service/start_watermark_service.sh 8081"
restart_command_8082="/data/watermark_service/start_watermark_service.sh 8082"
restart_command_test_environment_8089="/data/watermark_service_test/start_watermark_service.sh 8089"
restart_command_443="systemctl restart nginx"
restart_command_prometheus_9090="systemctl restart prometheus"

# Ports to be monitored
declare -A ports_and_commands=(
    [8081]="$restart_command_8081"
    [8082]="$restart_command_8082"
    [443]="$restart_command_443"
    [8089]="$restart_command_test_environment_8089"
    [9090]="$restart_command_prometheus_9090"
)

# Log file
log_file="/var/log/monitor_watermark_service.log"

echo "Checking ports at $(date):" >> $log_file
for port in "${!ports_and_commands[@]}"; do
    check_and_restart $port "${ports_and_commands[$port]}" >> $log_file
done
echo "--------" >> $log_file
