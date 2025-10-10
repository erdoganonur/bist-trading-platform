# AlgoLab Database Session Storage

## Overview

Database-based session storage for AlgoLab API authentication. This replaces file-based storage with production-ready PostgreSQL persistence.

**Date:** 2025-10-10
**Version:** 1.0.0
**Status:** ✅ Production Ready

---

## Features

✅ **PostgreSQL-backed session storage**
✅ **Automatic session expiration** (24 hours default)
✅ **Scheduled cleanup** of old sessions
✅ **Multi-user support** with user-level session isolation
✅ **WebSocket status tracking**
✅ **Security audit trail** (IP address, user agent)
✅ **Optimized queries** with strategic indexes
✅ **Fallback to file storage** if database unavailable

---

## Database Schema

### Table: `algolab_sessions`

```sql
CREATE TABLE algolab_sessions (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES user_entity(id),
    token VARCHAR(500) NOT NULL,
    hash VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    last_refresh_at TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT true,
    websocket_connected BOOLEAN DEFAULT false,
    websocket_last_connected TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    termination_reason VARCHAR(100),
    terminated_at TIMESTAMP
);
```

### Indexes

- `idx_algolab_user_id` - User lookup
- `idx_algolab_active` - Active session filtering
- `idx_algolab_expires_at` - Expiration queries
- `idx_algolab_user_active` - Composite for user+active queries
- `idx_algolab_token` - Token lookup (partial index on active)
- `idx_algolab_websocket` - WebSocket status (partial index on active)

### View: `v_algolab_active_sessions`

Summary view showing active sessions with expiry status:

```sql
SELECT * FROM v_algolab_active_sessions;
```

---

## Configuration

### application.yml (Production)

```yaml
algolab:
  session:
    storage: database
    expiration-hours: 24
    retention-days: 30
    cleanup-cron: "0 0 * * * *"  # Every hour
    auto-cleanup: true
```

### application-dev.yml (Development)

```yaml
algolab:
  session:
    storage: file  # Can override with ALGOLAB_SESSION_STORAGE env var
    file-path: ./algolab-session-dev.json
    expiration-hours: 12
    retention-days: 7
    cleanup-cron: "0 0 */6 * * *"  # Every 6 hours
    auto-cleanup: true
```

### Environment Variables

```bash
# Override session storage type
ALGOLAB_SESSION_STORAGE=database

# Database connection (already configured)
DB_URL=jdbc:postgresql://localhost:5432/bist_trading
DB_USERNAME=bist_user
DB_PASSWORD=bist_password
```

---

## Usage

### Saving a Session

```java
@Autowired
private AlgoLabSessionManager sessionManager;

// Set user context (optional)
sessionManager.setCurrentUserId(userId);

// Save session
sessionManager.saveSession(token, hash);
```

### Loading a Session

```java
AlgoLabSession session = sessionManager.loadSession();

if (session != null && session.getToken() != null) {
    // Use session
    String token = session.getToken();
    String hash = session.getHash();
}
```

### Clearing a Session (Logout)

```java
sessionManager.clearSession();
```

### Updating WebSocket Status

```java
sessionManager.updateWebSocketStatus(true);  // Connected
sessionManager.updateWebSocketStatus(false); // Disconnected
```

### Getting Statistics

```java
var stats = sessionManager.getStatistics();
log.info("Active sessions: {}", stats.getTotalActiveSessions());
log.info("User sessions: {}", stats.getUserActiveSessions());
```

---

## REST API Endpoints

All under `/api/v1/broker/sessions`:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/statistics` | GET | Get session statistics |
| `/active` | GET | List all active sessions |
| `/user/{userId}` | GET | Get sessions for specific user |
| `/expired` | GET | List expired but active sessions |
| `/cleanup` | POST | Manually trigger cleanup |
| `/user/{userId}` | DELETE | Deactivate all user sessions |
| `/set-user/{userId}` | POST | Set current user ID |
| `/health` | GET | Session storage health check |

### Examples

**Get Statistics:**
```bash
curl http://localhost:8081/api/v1/broker/sessions/statistics
```

**Response:**
```json
{
  "storage": "database",
  "totalActiveSessions": 15,
  "userActiveSessions": 1,
  "error": null
}
```

**Get Active Sessions:**
```bash
curl http://localhost:8081/api/v1/broker/sessions/active
```

**Trigger Cleanup:**
```bash
curl -X POST http://localhost:8081/api/v1/broker/sessions/cleanup
```

**Deactivate User Sessions:**
```bash
curl -X DELETE http://localhost:8081/api/v1/broker/sessions/user/{userId}
```

---

## Automatic Cleanup

### Scheduled Task

Runs every hour (configurable via `algolab.session.cleanup-cron`):

1. **Deactivates expired sessions** - Sets `active=false` for sessions past `expires_at`
2. **Deletes old inactive sessions** - Hard deletes sessions older than retention period (30 days)

### Manual Cleanup

```bash
curl -X POST http://localhost:8081/api/v1/broker/sessions/cleanup
```

### Cleanup Logs

```
INFO  Deactivated 5 expired sessions
INFO  Deleted 23 old inactive sessions (older than 30 days)
```

---

## Session Lifecycle

### 1. Creation

```
User Login → AlgoLabAuthService
          → SessionManager.saveSession()
          → Database Insert
          → expires_at = now + 24 hours
```

### 2. Usage

```
Request → SessionManager.loadSession()
        → Database Query (user_id + active + not expired)
        → Return session if valid
```

### 3. Refresh (Keep-Alive)

```
Keep-Alive Task → SessionManager.refresh()
                → Update last_refresh_at
                → Extend expires_at
```

### 4. Expiration

```
Cleanup Task → Find sessions where expires_at < now
             → Set active = false
             → Set termination_reason = "EXPIRED"
             → Set terminated_at = now
```

### 5. Deletion

```
Cleanup Task → Find inactive sessions older than retention days
             → Hard delete from database
```

---

## Migration Guide

### From File to Database

1. **Run Flyway migration:**
   ```bash
   ./gradlew flywayMigrate
   ```

2. **Update configuration:**
   ```yaml
   algolab:
     session:
       storage: database  # Change from 'file'
   ```

3. **Restart application:**
   ```bash
   ./gradlew bootRun
   ```

4. **Verify:**
   ```bash
   curl http://localhost:8081/api/v1/broker/sessions/health
   ```

### Rollback to File

If needed, you can rollback to file storage:

```yaml
algolab:
  session:
    storage: file
    file-path: ./algolab-session.json
```

No code changes required - seamless fallback.

---

## Performance Considerations

### Indexes

All critical queries are covered by indexes:

- User session lookup: `idx_algolab_user_active` (composite)
- Expiration queries: `idx_algolab_expires_at`
- Active filtering: `idx_algolab_active`

### Query Optimization

**Finding active session:**
```sql
-- Optimized with composite index
SELECT * FROM algolab_sessions
WHERE user_id = ? AND active = true AND expires_at > NOW()
ORDER BY created_at DESC
LIMIT 1;
```

**Cleanup query:**
```sql
-- Uses index on expires_at
UPDATE algolab_sessions
SET active = false, termination_reason = 'EXPIRED'
WHERE active = true AND expires_at < NOW();
```

### Connection Pooling

Uses HikariCP with optimal settings:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

---

## Security Features

### Audit Trail

Each session records:
- **IP Address** - Where session was created
- **User Agent** - Client information
- **Creation timestamp** - When session started
- **Termination info** - Why and when session ended

### Session Isolation

- Each user's sessions are isolated
- Deactivating one user's sessions doesn't affect others
- Support for multiple concurrent sessions per user

### Token Security

- Tokens stored in database (encrypted at application level if needed)
- Hashes stored separately
- Old sessions automatically cleaned up

---

## Monitoring

### Metrics

Monitor via Actuator:

```bash
curl http://localhost:8081/actuator/metrics/algolab.sessions.active
```

### Health Check

```bash
curl http://localhost:8081/api/v1/broker/sessions/health
```

### Database View

```sql
SELECT * FROM v_algolab_active_sessions;
```

Shows:
- Active sessions
- Time until expiry
- Status (ACTIVE, EXPIRING_SOON, EXPIRED)

---

## Troubleshooting

### Sessions Not Persisting

**Problem:** Sessions lost on restart

**Solution:** Check storage configuration:
```bash
curl http://localhost:8081/api/v1/broker/sessions/health
```

Should show `"storage": "database"`

### Cleanup Not Running

**Problem:** Old sessions accumulating

**Check:**
1. Verify cron expression: `algolab.session.cleanup-cron`
2. Check logs for cleanup execution
3. Manually trigger: `POST /sessions/cleanup`

### Database Connection Issues

**Problem:** `repository not available` errors

**Solution:**
1. Check database is running: `pg_isready`
2. Verify connection settings in `application.yml`
3. Check Flyway migrations ran successfully

### Multiple Active Sessions

**Problem:** User has multiple active sessions

**Expected:** This is normal - users can have concurrent sessions

**To limit:** Modify `saveToDatabase()` to deactivate old sessions

---

## Best Practices

1. **Use Database in Production**
   - More reliable than file storage
   - Supports multiple instances
   - Better audit trail

2. **Set Appropriate Expiration**
   - 24 hours is good default
   - Shorter for high-security environments
   - Longer for development

3. **Monitor Session Growth**
   - Check active session count regularly
   - Set up alerts for unusual growth

4. **Regular Cleanup**
   - Keep cleanup enabled
   - Adjust retention period based on compliance needs

5. **User Context**
   - Set user ID when available: `setCurrentUserId()`
   - Provides better session isolation

---

## Summary

**Implemented Features:**
- ✅ PostgreSQL-backed session storage
- ✅ JPA entity with full audit fields
- ✅ Repository with optimized queries
- ✅ Automatic expiration (24 hours)
- ✅ Scheduled cleanup (hourly)
- ✅ REST API for management
- ✅ Seamless fallback to file storage
- ✅ WebSocket status tracking
- ✅ Multi-user support

**Benefits:**
- 🚀 Production-ready persistence
- 🔒 Security audit trail
- 📊 Better monitoring and visibility
- 🔄 Automatic cleanup
- 👥 Multi-user support
- 📈 Scalable to multiple instances

---

**Documentation Date:** 2025-10-10
**Migration Version:** V003
**Status:** Production Ready ✅
