#!/bin/bash
# Log Rotation Script for System Monitor Logs
# Rotates /var/log/system_monitor.log and /var/log/system_monitor_alerts.log hourly
# Version: 1.0

# Configuration
LOG_FILES=(
    "/var/log/system_monitor.log"
    "/var/log/system_monitor_alerts.log"
)
BACKUP_DIR="/var/log/system_monitor_backups"
MAX_BACKUPS=24  # Keep 24 hours (1 day) of backups

# Create backup directory if it doesn't exist
mkdir -p "$BACKUP_DIR"
chmod 750 "$BACKUP_DIR"
chown root:root "$BACKUP_DIR"

# Rotate logs function
rotate_logs() {
    local timestamp=$(date +"%Y%m%d_%H%M%S")

    for logfile in "${LOG_FILES[@]}"; do
        if [ -f "$logfile" ]; then
            # Get base filename without directory
            local basefile=$(basename "$logfile")

            # Create backup with timestamp
            local backup_file="${BACKUP_DIR}/${basefile}.${timestamp}"
            cp "$logfile" "$backup_file"
            chmod 640 "$backup_file"

            # Truncate the original log file
            > "$logfile"

            echo "Rotated ${logfile} to ${backup_file}"
        fi
    done

    # Clean up old backups (keep only MAX_BACKUPS per log)
    for logfile in "${LOG_FILES[@]}"; do
        local basefile=$(basename "$logfile")
        ls -t "${BACKUP_DIR}/${basefile}."* 2>/dev/null | tail -n +$(($MAX_BACKUPS + 1)) | xargs rm -f --
    done
}

# Main execution
rotate_logs

exit 0
