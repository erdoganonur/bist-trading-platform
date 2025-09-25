#!/bin/bash
# =============================================================================
# Redis Cluster Backup Script for BIST Trading Platform
# =============================================================================

set -euo pipefail

# Configuration
REDIS_NODES=(
    "localhost:7001"
    "localhost:7002"
    "localhost:7003"
    "localhost:7004"
    "localhost:7005"
    "localhost:7006"
)

BACKUP_DIR="/opt/redis/backups"
RETENTION_DAYS=7
LOG_FILE="/var/log/redis/backup.log"
DATE=$(date +%Y%m%d_%H%M%S)

# S3 Configuration (optional)
S3_BUCKET="${REDIS_S3_BUCKET:-}"
S3_PREFIX="redis-backups"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Logging
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Create backup directory
create_backup_dir() {
    mkdir -p "$BACKUP_DIR/$DATE" || {
        log "ERROR: Failed to create backup directory"
        exit 1
    }
}

# Backup individual node
backup_node() {
    local node=$1
    local host=${node%:*}
    local port=${node#*:}
    local node_backup_dir="$BACKUP_DIR/$DATE/node_${host}_${port}"

    mkdir -p "$node_backup_dir"

    log "Backing up Redis node $node..."

    # Get node role
    local role=$(redis-cli -h "$host" -p "$port" info replication 2>/dev/null | grep "role:" | cut -d: -f2 | tr -d '\r')

    if [[ "$role" != "master" && "$role" != "slave" ]]; then
        log "WARNING: Node $node role is '$role', skipping..."
        return 1
    fi

    # Create RDB backup
    if redis-cli -h "$host" -p "$port" --rdb "$node_backup_dir/dump.rdb" > /dev/null 2>&1; then
        log "RDB backup completed for $node"
    else
        log "ERROR: RDB backup failed for $node"
        return 1
    fi

    # Save configuration
    redis-cli -h "$host" -p "$port" config get "*" > "$node_backup_dir/config.txt" 2>/dev/null || true

    # Save cluster configuration if applicable
    if redis-cli -h "$host" -p "$port" cluster nodes > "$node_backup_dir/cluster.txt" 2>/dev/null; then
        log "Cluster configuration saved for $node"
    fi

    # Save node info
    {
        echo "# Redis Node Information - $DATE"
        echo "# Node: $node"
        echo "# Role: $role"
        echo ""
        redis-cli -h "$host" -p "$port" info all 2>/dev/null || true
    } > "$node_backup_dir/info.txt"

    # Compress backup
    if command -v gzip &> /dev/null; then
        gzip "$node_backup_dir/dump.rdb" 2>/dev/null || true
    fi

    return 0
}

# Backup all nodes
backup_all_nodes() {
    log "Starting Redis cluster backup..."

    local successful_backups=0
    local total_nodes=${#REDIS_NODES[@]}

    for node in "${REDIS_NODES[@]}"; do
        if backup_node "$node"; then
            successful_backups=$((successful_backups + 1))
        fi
    done

    log "Backup completed: $successful_backups/$total_nodes nodes successful"

    if [[ "$successful_backups" -eq 0 ]]; then
        log "ERROR: All node backups failed"
        return 1
    fi

    return 0
}

# Create cluster backup manifest
create_manifest() {
    local manifest_file="$BACKUP_DIR/$DATE/manifest.json"

    {
        echo "{"
        echo "  \"backup_date\": \"$DATE\","
        echo "  \"cluster_nodes\": ["

        local first=true
        for node in "${REDIS_NODES[@]}"; do
            [[ "$first" == "true" ]] && first=false || echo ","
            echo -n "    \"$node\""
        done

        echo ""
        echo "  ],"
        echo "  \"backup_type\": \"full\","
        echo "  \"retention_days\": $RETENTION_DAYS"
        echo "}"
    } > "$manifest_file"

    log "Backup manifest created: $manifest_file"
}

# Upload to S3 (if configured)
upload_to_s3() {
    if [[ -z "$S3_BUCKET" ]]; then
        log "S3 backup not configured, skipping upload"
        return 0
    fi

    if ! command -v aws &> /dev/null; then
        log "WARNING: AWS CLI not found, skipping S3 upload"
        return 0
    fi

    log "Uploading backup to S3..."

    local backup_archive="$BACKUP_DIR/redis-backup-$DATE.tar.gz"

    # Create archive
    tar -czf "$backup_archive" -C "$BACKUP_DIR" "$DATE/" || {
        log "ERROR: Failed to create backup archive"
        return 1
    }

    # Upload to S3
    if aws s3 cp "$backup_archive" "s3://$S3_BUCKET/$S3_PREFIX/redis-backup-$DATE.tar.gz"; then
        log "Backup uploaded to S3 successfully"
        rm -f "$backup_archive"
    else
        log "ERROR: Failed to upload backup to S3"
        return 1
    fi

    return 0
}

# Clean old backups
cleanup_old_backups() {
    log "Cleaning up backups older than $RETENTION_DAYS days..."

    # Local cleanup
    find "$BACKUP_DIR" -maxdepth 1 -type d -name "????????_??????" -mtime "+$RETENTION_DAYS" -exec rm -rf {} \; 2>/dev/null || true

    # S3 cleanup (if configured)
    if [[ -n "$S3_BUCKET" ]] && command -v aws &> /dev/null; then
        local cutoff_date=$(date -d "$RETENTION_DAYS days ago" +%Y%m%d_%H%M%S)

        aws s3 ls "s3://$S3_BUCKET/$S3_PREFIX/" | awk '{print $4}' | while read -r file; do
            if [[ "$file" < "redis-backup-$cutoff_date.tar.gz" ]]; then
                aws s3 rm "s3://$S3_BUCKET/$S3_PREFIX/$file"
                log "Deleted old S3 backup: $file"
            fi
        done
    fi

    log "Cleanup completed"
}

# Verify backup integrity
verify_backup() {
    local backup_path="$BACKUP_DIR/$DATE"
    local errors=0

    log "Verifying backup integrity..."

    for node in "${REDIS_NODES[@]}"; do
        local host=${node%:*}
        local port=${node#*:}
        local node_backup_dir="$backup_path/node_${host}_${port}"

        if [[ -f "$node_backup_dir/dump.rdb" || -f "$node_backup_dir/dump.rdb.gz" ]]; then
            log "✓ Backup exists for $node"
        else
            log "✗ Missing backup for $node"
            errors=$((errors + 1))
        fi
    done

    if [[ -f "$backup_path/manifest.json" ]]; then
        log "✓ Manifest file exists"
    else
        log "✗ Missing manifest file"
        errors=$((errors + 1))
    fi

    if [[ "$errors" -eq 0 ]]; then
        log "Backup verification successful"
        return 0
    else
        log "Backup verification failed with $errors errors"
        return 1
    fi
}

# Restore from backup
restore_backup() {
    local restore_date="$1"
    local restore_path="$BACKUP_DIR/$restore_date"

    if [[ ! -d "$restore_path" ]]; then
        log "ERROR: Backup $restore_date not found"
        exit 1
    fi

    read -p "Are you sure you want to restore from backup $restore_date? This will replace current data! (yes/no): " -r
    if [[ $REPLY != "yes" ]]; then
        log "Restore cancelled"
        exit 0
    fi

    log "Restoring from backup $restore_date..."

    for node in "${REDIS_NODES[@]}"; do
        local host=${node%:*}
        local port=${node#*:}
        local node_backup_dir="$restore_path/node_${host}_${port}"

        if [[ -f "$node_backup_dir/dump.rdb" || -f "$node_backup_dir/dump.rdb.gz" ]]; then
            log "Restoring node $node..."

            # Stop Redis temporarily (if possible)
            # This would require additional logic to safely stop/start Redis nodes

            log "WARNING: Manual restore required for $node"
            log "Copy $node_backup_dir/dump.rdb* to Redis data directory"
        fi
    done

    log "Restore process initiated. Manual steps may be required."
}

# List available backups
list_backups() {
    echo "Available backups:"
    echo "=================="

    if [[ -d "$BACKUP_DIR" ]]; then
        for backup in "$BACKUP_DIR"/????????_??????; do
            if [[ -d "$backup" ]]; then
                local backup_name=$(basename "$backup")
                local backup_size=$(du -sh "$backup" | cut -f1)
                echo "$backup_name ($backup_size)"
            fi
        done
    else
        echo "No backups found"
    fi
}

# Show usage
usage() {
    echo "Usage: $0 [OPTION]"
    echo ""
    echo "Options:"
    echo "  -h, --help           Show this help message"
    echo "  -b, --backup         Create backup (default)"
    echo "  -r, --restore DATE   Restore from backup"
    echo "  -l, --list           List available backups"
    echo "  -v, --verify DATE    Verify backup integrity"
    echo "  -c, --cleanup        Clean old backups"
    echo ""
    echo "Examples:"
    echo "  $0                           # Create backup"
    echo "  $0 --backup                 # Same as above"
    echo "  $0 --restore 20231201_143000 # Restore from backup"
    echo "  $0 --list                   # List backups"
    echo "  $0 --verify 20231201_143000  # Verify backup"
}

# Main function
main() {
    # Create directories
    mkdir -p "$(dirname "$LOG_FILE")" "$BACKUP_DIR" 2>/dev/null || true

    case "${1:-backup}" in
        -h|--help|help)
            usage
            ;;
        -b|--backup|backup)
            create_backup_dir
            if backup_all_nodes; then
                create_manifest
                verify_backup
                upload_to_s3
                cleanup_old_backups
                log "Backup process completed successfully"
            else
                log "Backup process completed with errors"
                exit 1
            fi
            ;;
        -r|--restore|restore)
            if [[ -z "${2:-}" ]]; then
                echo "Error: Backup date required for restore"
                usage
                exit 1
            fi
            restore_backup "$2"
            ;;
        -l|--list|list)
            list_backups
            ;;
        -v|--verify|verify)
            if [[ -z "${2:-}" ]]; then
                echo "Error: Backup date required for verify"
                usage
                exit 1
            fi
            verify_backup
            ;;
        -c|--cleanup|cleanup)
            cleanup_old_backups
            ;;
        *)
            echo "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"