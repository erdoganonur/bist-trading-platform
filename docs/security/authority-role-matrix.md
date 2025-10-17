# Authority and Role Matrix - BIST Trading Platform

## Overview

This document provides a comprehensive mapping of all authorities and roles used in the BIST Trading Platform's authorization system. The platform uses Spring Security's method-level security with `@PreAuthorize` annotations.

## Authorization Model

The platform uses two types of authorization:
- **Roles**: Broad categorizations (e.g., ADMIN, TRADER, USER)
- **Authorities**: Fine-grained permissions for specific operations (e.g., `market:read`, `portfolio:read`)

## Available Roles

| Role | Description | Typical Use Cases |
|------|-------------|-------------------|
| `ROLE_ADMIN` | System administrator with elevated privileges | System configuration, cache management, user administration |
| `ROLE_TRADER` | Professional trader with trading permissions | Active trading, portfolio management, order execution |
| `ROLE_USER` | Basic authenticated user | Market data viewing, profile management |

## Available Authorities

### Market Data Access

| Authority | Description | Required By | Endpoints |
|-----------|-------------|-------------|-----------|
| `market:read` | Read access to market data | All trading operations | `/api/v1/market-data/**` |

**Granted By Default**: Should be granted to all active users

**Endpoints Protected**:
- `GET /api/v1/market-data/quotes/{symbol}` - Get real-time quote
- `GET /api/v1/market-data/orderbook/{symbol}` - Get order book
- `GET /api/v1/market-data/trades/{symbol}/recent` - Get recent trades
- `GET /api/v1/market-data/stats/{symbol}/daily` - Get daily statistics
- `GET /api/v1/market-data/stats/{symbol}/intraday` - Get intraday statistics
- `GET /api/v1/market-data/movers/top` - Get top movers

### Trading Operations

| Authority | Description | Required By | Endpoints |
|-----------|-------------|-------------|-----------|
| `trading:read` | Read access to trading data | Portfolio viewing | `/api/v1/symbols/*/price`, `/api/v1/symbols/*/details` |
| `trading:place` | Permission to place new orders | Order execution | `POST /api/v1/broker/orders` |
| `trading:modify` | Permission to modify existing orders | Order management | `PUT /api/v1/broker/orders/{orderId}` |
| `trading:cancel` | Permission to cancel orders | Order management | `DELETE /api/v1/broker/orders/{orderId}` |

**Granted By Default**: Only to TRADER role or explicitly granted users

**Endpoints Protected**:
- `POST /api/v1/broker/orders` - Place new order (`trading:place`)
- `PUT /api/v1/broker/orders/{orderId}` - Modify order (`trading:modify`)
- `DELETE /api/v1/broker/orders/{orderId}` - Cancel order (`trading:cancel`)

### Portfolio Management

| Authority | Description | Required By | Endpoints |
|-----------|-------------|-------------|-----------|
| `portfolio:read` | View portfolio and positions | Portfolio viewing, broker operations | `/api/v1/broker/portfolio`, `/api/v1/broker/positions` |

**Granted By Default**: To TRADER role or users with active broker accounts

**Endpoints Protected**:
- `GET /api/v1/broker/portfolio` - Get portfolio summary (`portfolio:read`)
- `GET /api/v1/broker/positions` - Get open positions (`portfolio:read`)
- `GET /api/v1/symbols/*/price` - Get symbol price (requires `trading:read` OR `portfolio:read`)

### Order History

| Authority | Description | Required By | Endpoints |
|-----------|-------------|-------------|-----------|
| `orders:read` | View order history | Order tracking | `/api/v1/broker/orders/history` |

**Granted By Default**: To TRADER role or users with trading history

**Endpoints Protected**:
- `GET /api/v1/broker/orders/history` - Get order history (`orders:read`)

## Role-Authority Mapping

### Recommended Authority Grants by Role

```
ROLE_ADMIN:
  ├─ market:read ✓
  ├─ trading:read ✓
  ├─ trading:place ✓
  ├─ trading:modify ✓
  ├─ trading:cancel ✓
  ├─ portfolio:read ✓
  └─ orders:read ✓

ROLE_TRADER:
  ├─ market:read ✓
  ├─ trading:read ✓
  ├─ trading:place ✓
  ├─ trading:modify ✓
  ├─ trading:cancel ✓
  ├─ portfolio:read ✓
  └─ orders:read ✓

ROLE_USER (Basic):
  ├─ market:read ✓ (Read-only market data)
  ├─ trading:read ✗ (No trading access)
  ├─ trading:place ✗
  ├─ trading:modify ✗
  ├─ trading:cancel ✗
  ├─ portfolio:read ✗ (No portfolio access)
  └─ orders:read ✗
```

## Authorization Checks by Controller

### MarketDataController

| Endpoint | Method | Authorization Required |
|----------|--------|------------------------|
| `/api/v1/market-data/quotes/{symbol}` | GET | `hasAuthority('market:read')` |
| `/api/v1/market-data/orderbook/{symbol}` | GET | `hasAuthority('market:read')` |
| `/api/v1/market-data/trades/{symbol}/recent` | GET | `hasAuthority('market:read')` |
| `/api/v1/market-data/stats/{symbol}/daily` | GET | `hasAuthority('market:read')` |
| `/api/v1/market-data/stats/{symbol}/intraday` | GET | `hasAuthority('market:read')` |
| `/api/v1/market-data/movers/top` | GET | `hasAuthority('market:read')` |
| `/api/v1/market-data/admin/refresh` | POST | `hasRole('ADMIN')` |
| `/api/v1/market-data/health` | GET | `isAuthenticated()` |

### BrokerController

| Endpoint | Method | Authorization Required |
|----------|--------|------------------------|
| `POST /api/v1/broker/orders` | POST | `hasAuthority('trading:place')` |
| `PUT /api/v1/broker/orders/{orderId}` | PUT | `hasAuthority('trading:modify')` |
| `DELETE /api/v1/broker/orders/{orderId}` | DELETE | `hasAuthority('trading:cancel')` |
| `GET /api/v1/broker/orders/{orderId}` | GET | `isAuthenticated()` |
| `GET /api/v1/broker/portfolio` | GET | `hasAuthority('portfolio:read')` |
| `GET /api/v1/broker/positions` | GET | `hasAuthority('portfolio:read')` |
| `GET /api/v1/broker/balance` | GET | `isAuthenticated()` |
| `GET /api/v1/broker/status` | GET | `isAuthenticated()` |
| `GET /api/v1/broker/orders/history` | GET | `hasAuthority('orders:read')` |

### SymbolController

| Endpoint | Method | Authorization Required |
|----------|--------|------------------------|
| `GET /api/v1/symbols` | GET | `isAuthenticated()` |
| `GET /api/v1/symbols/search` | GET | `isAuthenticated()` |
| `GET /api/v1/symbols/{symbol}` | GET | `isAuthenticated()` |
| `GET /api/v1/symbols/{symbol}/price` | GET | `hasAnyAuthority('trading:read', 'portfolio:read')` |
| `GET /api/v1/symbols/{symbol}/details` | GET | `isAuthenticated()` |
| `GET /api/v1/symbols/{symbol}/history` | GET | `isAuthenticated()` |
| `GET /api/v1/symbols/sectors` | GET | `isAuthenticated()` |
| `GET /api/v1/symbols/sectors/{sector}` | GET | `isAuthenticated()` |

### UserController

| Endpoint | Method | Authorization Required |
|----------|--------|------------------------|
| `GET /api/v1/users/profile` | GET | `isAuthenticated()` |
| `PUT /api/v1/users/profile` | PUT | `isAuthenticated()` |
| `POST /api/v1/users/change-password` | POST | `isAuthenticated()` |
| All other user endpoints | * | `isAuthenticated()` |

### BrokerAuthController

| Endpoint | Method | Authorization Required |
|----------|--------|------------------------|
| `POST /api/v1/broker/auth/login` | POST | `isAuthenticated()` |
| `POST /api/v1/broker/auth/verify-otp` | POST | `isAuthenticated()` |
| `GET /api/v1/broker/auth/status` | GET | `isAuthenticated()` |
| `POST /api/v1/broker/auth/logout` | POST | `isAuthenticated()` |

### CacheManagementController (Admin Only)

| Endpoint | Method | Authorization Required |
|----------|--------|------------------------|
| `POST /api/v1/cache/clear-all` | POST | `hasRole('ADMIN')` |
| `GET /api/v1/cache/stats` | GET | `hasAnyRole('ADMIN', 'TRADER')` |
| `GET /api/v1/cache/keys` | GET | `hasAnyRole('ADMIN', 'TRADER')` |
| `POST /api/v1/cache/auth/clear` | POST | `hasRole('ADMIN')` |
| `POST /api/v1/cache/quotes/clear` | POST | `hasRole('ADMIN')` |
| `GET /api/v1/cache/memory-usage` | GET | `hasRole('ADMIN')` |
| `GET /api/v1/cache/memory-usage/cleanup` | GET | `hasAnyRole('ADMIN', 'TRADER')` |

## How Authorities Are Stored

Authorities are stored in the `user_entities` table as a **comma-separated string** in the `authorities` column.

**Example**:
```sql
authorities = 'ROLE_ADMIN,ROLE_TRADER,ROLE_USER,market:read,trading:read,trading:place,trading:modify,trading:cancel,portfolio:read,orders:read'
```

## Granting Authorities to a User

### SQL Script to Grant All Authorities

```sql
-- Grant all authorities to a specific user
UPDATE user_entities
SET authorities = 'ROLE_ADMIN,ROLE_TRADER,ROLE_USER,market:read,trading:read,trading:place,trading:modify,trading:cancel,portfolio:read,orders:read'
WHERE username = 'your_username';

-- Verify the update
SELECT id, username, email, authorities, status
FROM user_entities
WHERE username = 'your_username';
```

### Grant Basic Trading Authorities (TRADER Role)

```sql
UPDATE user_entities
SET authorities = 'ROLE_TRADER,ROLE_USER,market:read,trading:read,trading:place,trading:modify,trading:cancel,portfolio:read,orders:read'
WHERE username = 'your_username';
```

### Grant Read-Only Authorities (Basic USER)

```sql
UPDATE user_entities
SET authorities = 'ROLE_USER,market:read'
WHERE username = 'your_username';
```

## Common 403 Forbidden Errors

### Error: "Access Denied - hasAuthority('portfolio:read') failed"

**Cause**: User doesn't have `portfolio:read` authority

**Solution**:
```sql
-- Add portfolio:read to existing authorities
UPDATE user_entities
SET authorities = authorities || ',portfolio:read'
WHERE username = 'your_username' AND authorities NOT LIKE '%portfolio:read%';
```

### Error: "Access Denied - hasAuthority('market:read') failed"

**Cause**: User doesn't have `market:read` authority

**Solution**:
```sql
-- Add market:read to existing authorities
UPDATE user_entities
SET authorities = authorities || ',market:read'
WHERE username = 'your_username' AND authorities NOT LIKE '%market:read%';
```

### Error: "Access Denied - hasRole('ADMIN') failed"

**Cause**: User doesn't have `ROLE_ADMIN` role

**Solution**:
```sql
-- Add ROLE_ADMIN to existing authorities
UPDATE user_entities
SET authorities = authorities || ',ROLE_ADMIN'
WHERE username = 'your_username' AND authorities NOT LIKE '%ROLE_ADMIN%';
```

## Best Practices

1. **Principle of Least Privilege**: Grant only the minimum authorities needed for a user's role
2. **Role-Based Grants**: Use role-based authority assignments for consistency
3. **Audit Authority Changes**: Log all authority modifications for security auditing
4. **Regular Reviews**: Periodically review user authorities to ensure they're still appropriate
5. **Testing**: Always test authority changes in a development environment first

## Security Considerations

- All authorities are checked server-side using Spring Security
- JWT tokens contain user authorities for stateless authentication
- Token refresh requires re-validation of authorities from database
- Session expiration automatically revokes all authorities
- Failed authorization attempts are logged for security monitoring

## Related Files

- **User Entity**: `/src/main/java/com/bisttrading/entity/UserEntity.java` (Line 61-62)
- **Security Config**: `/src/main/java/com/bisttrading/security/config/SecurityConfig.java`
- **Controllers with Authorization**:
  - `MarketDataController.java`
  - `BrokerController.java`
  - `SymbolController.java`
  - `BrokerAuthController.java`
  - `CacheManagementController.java`

## Support

For authority-related issues:
1. Check the user's authorities in the database
2. Verify the JWT token contains the correct authorities
3. Review the Spring Security logs for authorization failures
4. Consult this matrix to determine required authorities

---

**Last Updated**: 2025-10-16
**Version**: 1.0
**Author**: BIST Trading Platform Team
