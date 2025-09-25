# BIST Trading Platform - Database Setup Guide

## Overview

This guide provides step-by-step instructions for setting up the PostgreSQL database for the BIST Trading Platform User Management service, including security configuration, performance optimization, and compliance requirements.

## 🔧 Prerequisites

### System Requirements
- PostgreSQL 16+ (recommended for latest security and performance features)
- Minimum 32GB RAM for production (8GB for development)
- SSD storage with at least 500GB free space
- Network connectivity with proper firewall configuration

### Required Extensions
- `uuid-ossp`: UUID generation support
- `pgcrypto`: Encryption and hashing functions
- `pg_stat_statements`: Query performance monitoring

### Tools Required
- `psql`: PostgreSQL command-line interface
- `pg_dump/pg_restore`: Backup and restore utilities
- Database administration tool (pgAdmin, DBeaver, etc.)

## 📋 Setup Steps

### Step 1: PostgreSQL Installation

#### Ubuntu/Debian
```bash
# Add PostgreSQL official APT repository
sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -
sudo apt-get update

# Install PostgreSQL 16
sudo apt-get install postgresql-16 postgresql-contrib-16 postgresql-client-16

# Install additional tools
sudo apt-get install postgresql-16-pgaudit postgresql-16-pg-stat-kcache
```

#### CentOS/RHEL
```bash
# Install PostgreSQL repository
sudo yum install -y https://download.postgresql.org/pub/repos/yum/reporpms/EL-7-x86_64/pgdg-redhat-repo-latest.noarch.rpm

# Install PostgreSQL 16
sudo yum install -y postgresql16-server postgresql16-contrib

# Initialize database
sudo /usr/pgsql-16/bin/postgresql-16-setup initdb
sudo systemctl enable postgresql-16
sudo systemctl start postgresql-16
```

#### Docker (Development)
```bash
# Create PostgreSQL container
docker run --name bist-postgres \
  -e POSTGRES_DB=bist_trading \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=SecureAdminPassword123! \
  -p 5432:5432 \
  -v postgres_data:/var/lib/postgresql/data \
  -d postgres:16-alpine

# Install extensions in container
docker exec -it bist-postgres psql -U postgres -d bist_trading -c "
CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";
CREATE EXTENSION IF NOT EXISTS \"pgcrypto\";
CREATE EXTENSION IF NOT EXISTS \"pg_stat_statements\";
"
```

### Step 2: PostgreSQL Configuration

#### postgresql.conf Optimization for Financial Trading
```ini
# =============================================================================
# PostgreSQL Configuration for BIST Trading Platform
# Optimized for high-throughput financial trading workloads
# =============================================================================

# CONNECTION AND AUTHENTICATION
max_connections = 200                    # Adjust based on expected load
superuser_reserved_connections = 3

# MEMORY CONFIGURATION (for 32GB server)
shared_buffers = 8GB                     # 25% of total RAM
effective_cache_size = 24GB              # 75% of total RAM
work_mem = 256MB                         # For complex queries
maintenance_work_mem = 2GB               # For maintenance operations
wal_buffers = 64MB                       # WAL buffer size

# QUERY PLANNER
random_page_cost = 1.1                   # SSD optimized
effective_io_concurrency = 200           # SSD concurrent operations
seq_page_cost = 1.0                      # Sequential access cost

# WAL CONFIGURATION (High Availability)
wal_level = replica                      # Enable replication
wal_log_hints = on                       # For pg_rewind
max_wal_senders = 10                     # Maximum replication connections
max_replication_slots = 10               # Replication slot limit
hot_standby = on                         # Read queries on standby

# WAL PERFORMANCE
wal_compression = on                     # Compress WAL records
wal_buffers = 64MB                       # WAL write buffers
checkpoint_completion_target = 0.9       # Spread checkpoints
max_wal_size = 4GB                       # Maximum WAL size
min_wal_size = 1GB                       # Minimum WAL size

# PERFORMANCE OPTIMIZATION
shared_preload_libraries = 'pg_stat_statements,pgaudit'
pg_stat_statements.max = 10000           # Track more statements
pg_stat_statements.track = all           # Track all statements

# LOGGING FOR FINANCIAL COMPLIANCE
log_destination = 'stderr,csvlog'        # Multiple log formats
log_directory = 'log'                    # Log directory
log_filename = 'postgresql-%Y-%m-%d_%H%M%S.log'
log_rotation_age = 1d                    # Daily log rotation
log_rotation_size = 100MB                # Size-based rotation
log_min_duration_statement = 100ms       # Log slow queries
log_checkpoints = on                     # Log checkpoint info
log_connections = on                     # Log connections
log_disconnections = on                  # Log disconnections
log_lock_waits = on                      # Log lock waits
log_statement = 'mod'                    # Log modifications

# SECURITY SETTINGS
ssl = on                                 # Enable SSL/TLS
ssl_cert_file = 'server.crt'            # SSL certificate
ssl_key_file = 'server.key'             # SSL private key
ssl_ciphers = 'ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-GCM-SHA256'
password_encryption = scram-sha-256      # Strong password encryption

# AUTOVACUUM (Critical for high-write workloads)
autovacuum = on                          # Enable autovacuum
autovacuum_max_workers = 4               # Parallel vacuum workers
autovacuum_naptime = 30s                 # Vacuum frequency
autovacuum_vacuum_threshold = 50         # Minimum updates for vacuum
autovacuum_vacuum_scale_factor = 0.1     # Fraction of table for vacuum
autovacuum_analyze_threshold = 50        # Minimum updates for analyze
autovacuum_analyze_scale_factor = 0.05   # Fraction of table for analyze

# LOCK MANAGEMENT
deadlock_timeout = 1s                    # Deadlock detection timeout
max_locks_per_transaction = 256          # Lock table size

# BACKGROUND WRITER
bgwriter_delay = 200ms                   # Background writer delay
bgwriter_lru_maxpages = 100             # Pages written per round
bgwriter_lru_multiplier = 2.0           # Multiplier for next round

# TIMEZONE
timezone = 'Europe/Istanbul'             # Turkish timezone for BIST
```

#### pg_hba.conf Security Configuration
```
# =============================================================================
# PostgreSQL Host-Based Authentication
# Security configuration for BIST Trading Platform
# =============================================================================

# TYPE  DATABASE        USER            ADDRESS                 METHOD

# Local connections (Unix socket)
local   all             postgres                                peer
local   bist_trading    app_user                               scram-sha-256
local   bist_trading    app_readonly                           scram-sha-256
local   bist_trading    app_backup                             scram-sha-256

# Local IPv4 connections
host    bist_trading    app_user        127.0.0.1/32           scram-sha-256
host    bist_trading    app_readonly    127.0.0.1/32           scram-sha-256

# Application server connections (adjust IP ranges as needed)
host    bist_trading    app_user        10.0.0.0/8             scram-sha-256  # Private networks
host    bist_trading    app_user        172.16.0.0/12          scram-sha-256
host    bist_trading    app_user        192.168.0.0/16         scram-sha-256

# Replication connections
host    replication     replicator      10.0.0.0/8             scram-sha-256
host    replication     replicator      172.16.0.0/12          scram-sha-256
host    replication     replicator      192.168.0.0/16         scram-sha-256

# Monitoring connections
host    bist_trading    app_readonly    10.0.0.0/8             scram-sha-256  # Monitoring tools

# Backup connections
host    bist_trading    app_backup      10.0.0.100/32          scram-sha-256  # Backup server

# SSL-only connections for production
hostssl bist_trading    app_user        0.0.0.0/0              scram-sha-256  # Require SSL
hostssl bist_trading    app_readonly    0.0.0.0/0              scram-sha-256

# Deny all other connections
host    all             all             0.0.0.0/0              reject
```

### Step 3: Database and User Creation

#### Create Database and Extensions
```sql
-- Connect as postgres superuser
\c postgres

-- Create the main database
CREATE DATABASE bist_trading
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1
    TEMPLATE = template0;

-- Connect to the new database
\c bist_trading

-- Create required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Optional extensions for advanced features
CREATE EXTENSION IF NOT EXISTS "pgaudit";  -- Audit logging
CREATE EXTENSION IF NOT EXISTS "pg_trgm";  -- Trigram matching for search
CREATE EXTENSION IF NOT EXISTS "btree_gin"; -- GIN indexes on btree types
```

#### Run User Setup Script
```bash
# Execute the user setup script
psql -U postgres -d bist_trading -f platform-services/user-management-service/src/main/resources/db/setup/create_app_user.sql
```

### Step 4: Schema Migration

#### Using Flyway (Recommended)
```bash
# Install Flyway
wget -qO- https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/9.22.3/flyway-commandline-9.22.3-linux-x64.tar.gz | tar xvz && sudo ln -s `pwd`/flyway-9.22.3/flyway /usr/local/bin

# Configure Flyway
cat > flyway.conf << EOF
flyway.url=jdbc:postgresql://localhost:5432/bist_trading
flyway.user=app_user
flyway.password=SecureAppPassword123!
flyway.locations=filesystem:platform-services/user-management-service/src/main/resources/db/migration
flyway.schemas=public
flyway.table=flyway_schema_history
EOF

# Run migrations
flyway migrate

# Verify migration status
flyway info
```

#### Manual Migration (Alternative)
```bash
# Run the migration script directly
psql -U app_user -d bist_trading -f platform-services/user-management-service/src/main/resources/db/migration/V001__create_user_tables.sql
```

### Step 5: Security Configuration

#### Set Encryption Key
```bash
# Generate a secure 256-bit encryption key
ENCRYPTION_KEY=$(openssl rand -hex 32)
echo "Generated encryption key: $ENCRYPTION_KEY"

# Set the encryption key in your application environment
export DB_ENCRYPTION_KEY="$ENCRYPTION_KEY"

# Alternatively, set it directly in the database session (for testing)
psql -U app_user -d bist_trading -c "SELECT set_encryption_key('$ENCRYPTION_KEY');"
```

#### SSL Certificate Setup
```bash
# Generate self-signed certificate for development
sudo openssl req -new -x509 -days 365 -nodes -text -out /var/lib/postgresql/16/main/server.crt -keyout /var/lib/postgresql/16/main/server.key -subj "/CN=localhost"

# Set proper permissions
sudo chown postgres:postgres /var/lib/postgresql/16/main/server.crt
sudo chown postgres:postgres /var/lib/postgresql/16/main/server.key
sudo chmod 600 /var/lib/postgresql/16/main/server.key

# For production, use proper CA-signed certificates
```

### Step 6: Performance Verification

#### Test Database Performance
```sql
-- Test encryption performance
\timing on
SELECT encrypt_sensitive_data('Test data for encryption performance testing');
SELECT decrypt_sensitive_data(encrypt_sensitive_data('Test data'));

-- Test query performance
EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'test@example.com';

-- Check index usage
SELECT
    indexrelname as index_name,
    idx_tup_read,
    idx_tup_fetch,
    idx_tup_read + idx_tup_fetch as total_reads
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY total_reads DESC;
```

#### Connection Pool Testing
```bash
# Test connection limits
for i in {1..10}; do
    psql -U app_user -d bist_trading -c "SELECT current_timestamp, inet_client_addr();" &
done
wait

# Monitor active connections
psql -U postgres -d bist_trading -c "
SELECT
    datname,
    usename,
    client_addr,
    state,
    COUNT(*)
FROM pg_stat_activity
WHERE datname = 'bist_trading'
GROUP BY datname, usename, client_addr, state;
"
```

### Step 7: Monitoring Setup

#### Enable Query Statistics
```sql
-- Enable pg_stat_statements
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- View slow queries
SELECT
    query,
    calls,
    total_time,
    mean_time,
    rows
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;
```

#### Setup Monitoring Views
```sql
-- Create monitoring views
CREATE VIEW user_activity_summary AS
SELECT
    DATE_TRUNC('hour', created_at) as hour,
    COUNT(*) as new_users,
    COUNT(*) FILTER (WHERE user_status = 'ACTIVE') as active_users,
    COUNT(*) FILTER (WHERE email_verified = true) as verified_users
FROM users
WHERE deleted_at IS NULL
GROUP BY hour
ORDER BY hour DESC;

CREATE VIEW session_activity_summary AS
SELECT
    DATE_TRUNC('hour', created_at) as hour,
    session_type,
    COUNT(*) as total_sessions,
    COUNT(*) FILTER (WHERE is_active = true) as active_sessions,
    AVG(EXTRACT(EPOCH FROM (COALESCE(ended_at, CURRENT_TIMESTAMP) - created_at))/3600) as avg_duration_hours
FROM user_sessions
GROUP BY hour, session_type
ORDER BY hour DESC, session_type;
```

### Step 8: Backup Configuration

#### Automated Backup Script
```bash
#!/bin/bash
# backup_script.sh - Automated database backup

# Configuration
DB_NAME="bist_trading"
DB_USER="app_backup"
BACKUP_DIR="/var/backups/postgresql"
DATE=$(date +%Y%m%d_%H%M%S)

# Create backup directory
mkdir -p $BACKUP_DIR

# Full backup
pg_dump -U $DB_USER -h localhost -d $DB_NAME \
    --verbose --format=custom --compress=9 \
    --file="$BACKUP_DIR/bist_trading_full_$DATE.backup"

# Schema-only backup
pg_dump -U $DB_USER -h localhost -d $DB_NAME \
    --schema-only --verbose --format=plain \
    --file="$BACKUP_DIR/bist_trading_schema_$DATE.sql"

# Cleanup old backups (keep last 7 days)
find $BACKUP_DIR -name "*.backup" -mtime +7 -delete
find $BACKUP_DIR -name "*.sql" -mtime +7 -delete

echo "Backup completed: bist_trading_full_$DATE.backup"
```

#### Setup Cron Job
```bash
# Add to crontab (daily backups at 2 AM)
crontab -e
0 2 * * * /path/to/backup_script.sh >> /var/log/postgresql/backup.log 2>&1
```

## 🔍 Verification Checklist

### Post-Installation Verification

#### ✅ Database Creation
- [ ] Database `bist_trading` created successfully
- [ ] All required extensions installed
- [ ] Application users created with proper permissions
- [ ] SSL/TLS connections working

#### ✅ Schema Migration
- [ ] All tables created successfully
- [ ] Indexes created and optimized
- [ ] Triggers and functions working
- [ ] Row Level Security policies active

#### ✅ Security Configuration
- [ ] Encryption functions working
- [ ] Password hashing functional
- [ ] User permissions properly restricted
- [ ] SSL certificates configured

#### ✅ Performance Testing
- [ ] Query performance acceptable
- [ ] Connection pooling working
- [ ] Index usage optimized
- [ ] Autovacuum configured

#### ✅ Monitoring Setup
- [ ] Query statistics enabled
- [ ] Monitoring views created
- [ ] Logging configured
- [ ] Backup system tested

## 🚨 Troubleshooting

### Common Issues

#### Connection Problems
```bash
# Check PostgreSQL status
sudo systemctl status postgresql-16

# Check listening ports
sudo netstat -tlnp | grep 5432

# Test connection
psql -U app_user -h localhost -d bist_trading -c "SELECT current_timestamp;"
```

#### Permission Issues
```sql
-- Check user permissions
SELECT
    grantee,
    table_schema,
    table_name,
    privilege_type
FROM information_schema.role_table_grants
WHERE grantee = 'app_user';

-- Check RLS policies
SELECT * FROM pg_policies WHERE schemaname = 'public';
```

#### Performance Issues
```sql
-- Check table statistics
SELECT
    schemaname,
    tablename,
    n_tup_ins,
    n_tup_upd,
    n_tup_del,
    n_dead_tup,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze
FROM pg_stat_user_tables;

-- Check index usage
SELECT
    schemaname,
    tablename,
    indexname,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_tup_read DESC;
```

## 📚 Next Steps

1. **Application Integration**: Configure your Spring Boot application with the database
2. **Load Testing**: Perform load testing with realistic trading volumes
3. **Security Audit**: Conduct security assessment and penetration testing
4. **Monitoring Integration**: Set up Prometheus/Grafana monitoring
5. **Disaster Recovery**: Test backup and recovery procedures

## 🔗 Related Documentation

- [User Schema Documentation](user-schema.md)
- [Security Best Practices Guide](../security/database-security.md)
- [Performance Tuning Guide](../performance/database-optimization.md)
- [Monitoring and Alerting](../monitoring/database-monitoring.md)

---

*This setup guide is part of the BIST Trading Platform Sprint 3 development cycle.*