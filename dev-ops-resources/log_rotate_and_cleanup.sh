#!/bin/bash
set -eo pipefail

# Configuration
LOG_PATHS=(
    "/var/log/monitor_watermark_service.log"
    "/var/log/monitor_hashicorp_vault_service.log"
    "/var/log/start_watermark_service.log"
    "/var/log/watermark-service-test/*.log" # Wildcard pattern
    "/var/log/watermark-service/*.log"  # Wildcard pattern
)
BACKUP_ROOT="/var/log/watermark_service_monitoring_log_backup"
RETENTION_DAYS=365  # 1 year retention
COMPRESS_CMD="gzip"
TIMESTAMP=$(date +%Y-%m-%d)
LOCK_TIMEOUT=300  # 5 minutes in seconds
LOG_FILE="/var/log/log_rotation.log"

# Initialize log file
exec > >(tee -a "$LOG_FILE") 2>&1
echo "==== Rotation started at $(date) ===="

# Expand wildcards and verify files
declare -a ACTIVE_LOGS
shopt -s nullglob
for pattern in "${LOG_PATHS[@]}"; do
    for file in $pattern; do
        if [ -f "$file" ] && [ -s "$file" ]; then
            ACTIVE_LOGS+=("$file")
        else
            echo "WARNING: Skipping invalid/missing file: $file" >&2
        fi
    done
done
shopt -u nullglob

# Create backup directory with audit trail
mkdir -p "$BACKUP_ROOT/audit"
BACKUP_DIR="$BACKUP_ROOT/$TIMESTAMP"
mkdir -p "$BACKUP_DIR"
chmod 0750 "$BACKUP_DIR"
echo "Created backup directory: $BACKUP_DIR" | tee -a "$BACKUP_ROOT/audit/trail.log"

# Main rotation process
for LOG_FILE in "${ACTIVE_LOGS[@]}"; do
    FILE_INODE=$(stat -c %i "$LOG_FILE")
    FILE_OWNER=$(stat -c %U:%G "$LOG_FILE")

    TEMP_FILE=$(mktemp -p "$BACKUP_DIR" "$(basename "$LOG_FILE").XXXXXX")
    ARCHIVE_NAME="$(basename "$LOG_FILE")-${TIMESTAMP}-${FILE_INODE}.gz"
    LOCK_FILE="${LOG_FILE}.lock"

    echo "Processing: $LOG_FILE (inode: $FILE_INODE)"

    # Critical section with locking
    (
        if ! flock -x -w $LOCK_TIMEOUT 200; then
            echo "ERROR: Failed to acquire lock for $LOG_FILE" >&2
            exit 2
        fi

        # Atomic copy with metadata preservation
        if ! cp -ap "$LOG_FILE" "$TEMP_FILE"; then
            echo "ERROR: Copy failed for $LOG_FILE" >&2
            exit 3
        fi

        # Safe truncation with inode preservation
        if ! : > "$LOG_FILE"; then
            echo "ERROR: Truncation failed for $LOG_FILE" >&2
            exit 4
        fi

    ) 200>"$LOCK_FILE" || continue

    # Compression and finalization
    if $COMPRESS_CMD -c "$TEMP_FILE" > "$BACKUP_DIR/$ARCHIVE_NAME"; then
        # Preserve original ownership
        chown "$FILE_OWNER" "$BACKUP_DIR/$ARCHIVE_NAME"
        chmod 0640 "$BACKUP_DIR/$ARCHIVE_NAME"
        echo "SUCCESS: Rotated $LOG_FILE to $ARCHIVE_NAME"

        # Verify compressed file integrity
        if ! gzip -t "$BACKUP_DIR/$ARCHIVE_NAME"; then
            echo "CRITICAL: Corrupted archive detected: $ARCHIVE_NAME" >&2
            rm -f "$BACKUP_DIR/$ARCHIVE_NAME"
            exit 5
        fi

        # Cleanup temporary
        rm -f "$TEMP_FILE"
    else
        echo "ERROR: Compression failed for $LOG_FILE" >&2
        rm -f "$TEMP_FILE" "$BACKUP_DIR/$ARCHIVE_NAME"
        exit 6
    fi

    # Lock cleanup
    rm -f "$LOCK_FILE"
done

# Retention policy with integrity check
find "$BACKUP_ROOT" -mindepth 1 -maxdepth 1 -type d -mtime +$RETENTION_DAYS \
    -exec sh -c '
        echo "Processing deletion: $1"
        find "$1" -name "*.gz" -exec gzip -t {} \; || exit 1
        rm -rfv "$1" | tee -a "$BACKUP_ROOT/audit/trail.log"
    ' sh {} \;

echo "==== Rotation completed at $(date) ===="