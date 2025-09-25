# BIST Trading Platform - User Management Database Schema

## Overview

The User Management Schema provides a comprehensive, secure, and compliant database structure for managing users, organizations, and sessions in the BIST Trading Platform. The schema is designed with financial industry requirements in mind, including KYC/AML compliance, data encryption, audit trails, and high-performance requirements.

## 🔐 Security Features

### Data Encryption
- **PII Encryption**: All personally identifiable information (names, phone numbers, TCKN) is encrypted using AES-256-GCM
- **Password Security**: Bcrypt hashing with cost factor 12 for password storage
- **Session Security**: Cryptographically secure session tokens with proper lifecycle management

### Row Level Security (RLS)
- Enabled on all tables with comprehensive policies
- Users can only access their own data
- Organization members can only access data from their organizations
- Granular permissions based on user roles

### Audit & Compliance
- Complete audit trail with timestamps
- Soft delete functionality for data retention compliance
- GDPR/KVKK compliant PII handling
- KYC/AML data structure for regulatory compliance

## 📊 Database Schema

### Core Tables

#### 1. `users` - User Accounts
Primary table for storing user account information with encrypted PII data.

**Key Features:**
- UUID primary keys for security and scalability
- Encrypted storage of sensitive data (names, phone, TCKN)
- JSONB preferences for flexible user settings
- KYC/AML compliance data structure
- Multi-factor authentication support
- Account lockout and security features

**Important Columns:**
- `user_id` (UUID): Primary key
- `email`: Unique login identifier
- `encrypted_first_name/last_name`: AES-256-GCM encrypted names
- `encrypted_phone_number`: Encrypted phone for security
- `encrypted_tckn`: Turkish Citizenship Number (encrypted)
- `user_status`: Account status (ACTIVE, SUSPENDED, etc.)
- `kyc_status`: KYC verification status
- `preferences`: JSONB user preferences and settings
- `mfa_enabled`: Multi-factor authentication flag
- `deleted_at`: Soft delete timestamp

#### 2. `user_sessions` - Session Management
Manages active user sessions with comprehensive security tracking.

**Key Features:**
- Session token hashing for security
- Device fingerprinting and tracking
- IP address and geolocation logging
- Session lifecycle management
- Automatic cleanup capabilities

**Important Columns:**
- `session_id` (UUID): Primary key
- `user_id` (UUID): Reference to users table
- `session_token_hash`: Bcrypt hashed session token
- `device_info`: JSONB device and browser information
- `ip_address`: Client IP address
- `expires_at`: Session expiration timestamp
- `is_active`: Session status flag

#### 3. `organizations` - Corporate Entities
Manages corporate accounts and institutional traders.

**Key Features:**
- Corporate account management
- Trading limits and compliance settings
- Verification and KYC for organizations
- Multi-level organization hierarchy support

**Important Columns:**
- `organization_id` (UUID): Primary key
- `name`: Organization display name
- `legal_name`: Legal entity name
- `tax_number`: Tax identification number
- `settings`: JSONB trading limits and preferences
- `verification_data`: KYC/compliance documentation

#### 4. `organization_members` - User-Organization Relationships
Manages user memberships in organizations with role-based permissions.

**Key Features:**
- Role-based access control
- Individual trading limits per member
- Granular permission system
- Member lifecycle tracking

**Important Columns:**
- `member_id` (UUID): Primary key
- `organization_id`/`user_id`: Foreign key relationships
- `role`: Organization role (OWNER, ADMIN, TRADER, etc.)
- `permissions`: JSONB additional granular permissions
- `trading_enabled`: Trading permission flag
- `daily_limit`/`position_limit`: Individual trading limits

### Enumeration Types

#### User Status (`user_status`)
- `PENDING_VERIFICATION`: Waiting for email/phone verification
- `ACTIVE`: Active user account
- `SUSPENDED`: Temporarily suspended
- `DEACTIVATED`: User-initiated deactivation
- `BLOCKED`: Admin blocked account
- `KYC_PENDING`: KYC verification pending
- `KYC_REJECTED`: KYC verification rejected
- `CLOSED`: Permanently closed account

#### KYC Status (`kyc_status`)
- `NOT_STARTED`: KYC process not initiated
- `IN_PROGRESS`: Documents under review
- `PENDING_DOCUMENTS`: Additional documents required
- `APPROVED`: KYC approved
- `REJECTED`: KYC rejected
- `EXPIRED`: KYC approval expired

#### Organization Roles (`organization_role`)
- `OWNER`: Organization owner with full control
- `ADMIN`: Administrative privileges
- `TRADER`: Trading privileges
- `VIEWER`: View-only access
- `COMPLIANCE_OFFICER`: Compliance management
- `RISK_MANAGER`: Risk management functions

## 🔧 Utility Functions

### Encryption Functions
```sql
-- Encrypt sensitive data
encrypt_sensitive_data(plaintext TEXT, encryption_key TEXT DEFAULT current_setting('app.encryption_key', true))

-- Decrypt sensitive data
decrypt_sensitive_data(ciphertext TEXT, encryption_key TEXT DEFAULT current_setting('app.encryption_key', true))
```

### Password Functions
```sql
-- Hash password with bcrypt
hash_password(password TEXT)

-- Verify password against hash
verify_password(password TEXT, hash TEXT)
```

### User Context Functions
```sql
-- Set current user for RLS
set_current_user_id(user_uuid UUID)

-- Get current user context
get_current_user_id()

-- Set encryption key (application startup)
set_encryption_key(key_hex TEXT)
```

## ⚡ Performance Optimizations

### Indexes
The schema includes comprehensive indexing strategy:

#### Primary Indexes
- **B-tree indexes** on frequently queried columns (email, status, timestamps)
- **Partial indexes** for active users and common query patterns
- **Composite indexes** for multi-column queries

#### Advanced Indexes
- **GIN indexes** on JSONB columns for preferences and settings
- **Performance-optimized indexes** for session management
- **Covering indexes** to reduce table lookups

### Query Optimization
- Optimized for high-frequency read operations
- Efficient soft delete handling
- Session cleanup optimization
- Bulk operation support

## 🚀 Usage Examples

### User Registration with Encryption
```sql
-- Create new user with encrypted PII
INSERT INTO users (
    email,
    password_hash,
    encrypted_first_name,
    encrypted_last_name,
    encrypted_phone_number,
    encrypted_tckn
) VALUES (
    'user@example.com',
    hash_password('UserPassword123!'),
    encrypt_sensitive_data('John'),
    encrypt_sensitive_data('Doe'),
    encrypt_sensitive_data('+905551234567'),
    encrypt_sensitive_data('12345678901')
);
```

### Session Management
```sql
-- Create new session
INSERT INTO user_sessions (
    user_id,
    session_token_hash,
    session_type,
    device_info,
    ip_address,
    expires_at
) VALUES (
    'user-uuid-here',
    crypt('session-token', gen_salt('bf')),
    'WEB',
    '{"browser": "Chrome", "os": "Windows", "version": "95.0"}',
    '192.168.1.100',
    CURRENT_TIMESTAMP + INTERVAL '24 hours'
);
```

### Organization Management
```sql
-- Add user to organization
INSERT INTO organization_members (
    organization_id,
    user_id,
    role,
    trading_enabled,
    daily_limit
) VALUES (
    'org-uuid-here',
    'user-uuid-here',
    'TRADER',
    true,
    100000.00
);
```

## 🔒 Security Best Practices

### Encryption Key Management
1. **Environment Variables**: Store encryption keys in secure environment variables
2. **Key Rotation**: Implement regular key rotation procedures
3. **Key Derivation**: Use proper key derivation functions (KDF)
4. **Backup Encryption**: Encrypt database backups with separate keys

### Access Control
1. **Principle of Least Privilege**: Grant minimal required permissions
2. **Role Separation**: Use different database users for different access levels
3. **Connection Limits**: Implement connection pooling and limits
4. **SSL/TLS**: Always use encrypted connections

### Monitoring and Audit
1. **Query Logging**: Monitor slow queries and suspicious patterns
2. **Failed Attempts**: Track and alert on failed login attempts
3. **Data Access**: Audit access to sensitive data
4. **Performance Metrics**: Monitor database performance and health

## 🎯 Compliance Features

### GDPR/KVKV Compliance
- **Data Encryption**: All PII encrypted at rest
- **Right to be Forgotten**: Soft delete with permanent deletion capability
- **Data Portability**: JSONB structure supports data export
- **Access Logging**: Complete audit trail for data access

### Financial Compliance
- **KYC/AML Structure**: Dedicated fields for compliance data
- **Audit Trail**: Immutable audit log for regulatory reporting
- **Data Retention**: Configurable retention policies
- **Risk Scoring**: Built-in risk assessment capabilities

## 🔧 Migration and Maintenance

### Migration Files
- `V001__create_user_tables.sql`: Forward migration
- `V001__create_user_tables.down.sql`: Rollback migration
- `create_app_user.sql`: Database user setup

### Maintenance Tasks
1. **Regular Vacuum**: Automated table maintenance
2. **Index Maintenance**: Monitor and rebuild indexes as needed
3. **Statistics Update**: Keep query planner statistics current
4. **Session Cleanup**: Remove expired sessions regularly

### Backup Strategy
1. **Full Backups**: Daily full database backups
2. **Incremental Backups**: Hourly incremental backups
3. **Point-in-Time Recovery**: WAL archiving for precise recovery
4. **Backup Encryption**: Encrypt all backup files

## 📈 Monitoring Metrics

### Key Performance Indicators
- **Connection Pool Utilization**: Monitor active connections
- **Query Performance**: Track slow queries and execution times
- **Index Usage**: Monitor index hit ratios
- **Table Bloat**: Track table and index bloat

### Security Metrics
- **Failed Login Attempts**: Monitor authentication failures
- **Session Anomalies**: Detect suspicious session patterns
- **Data Access Patterns**: Monitor access to sensitive data
- **Privilege Escalation**: Track role and permission changes

## 🔍 Troubleshooting

### Common Issues

#### Performance Problems
- **Symptom**: Slow user queries
- **Solution**: Check indexes on frequently queried columns
- **Prevention**: Regular ANALYZE and VACUUM operations

#### Connection Issues
- **Symptom**: Connection pool exhaustion
- **Solution**: Review connection pool configuration
- **Prevention**: Monitor connection usage patterns

#### Security Concerns
- **Symptom**: Suspicious login patterns
- **Solution**: Review audit logs and implement additional MFA
- **Prevention**: Regular security monitoring and alerting

### Diagnostic Queries
```sql
-- Check table sizes and bloat
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size,
    pg_stat_get_live_tuples(c.oid) as live_tuples,
    pg_stat_get_dead_tuples(c.oid) as dead_tuples
FROM pg_tables t
JOIN pg_class c ON c.relname = t.tablename
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Monitor active sessions
SELECT
    user_id,
    session_type,
    created_at,
    last_activity_at,
    expires_at,
    is_active
FROM user_sessions
WHERE is_active = true
ORDER BY last_activity_at DESC;

-- Check encryption function performance
EXPLAIN ANALYZE SELECT encrypt_sensitive_data('test data');
```

## 📚 Additional Resources

- [PostgreSQL Security Guide](https://www.postgresql.org/docs/current/security.html)
- [Flyway Migration Best Practices](https://flywaydb.org/documentation/concepts/migrations)
- [GDPR Compliance Guide](https://gdpr.eu/compliance/)
- [Financial Data Security Standards](https://www.bis.org/publ/bcbs239.htm)

---

*This documentation is part of the BIST Trading Platform Sprint 3 development cycle.*