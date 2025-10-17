# Authority Quick Reference Card

## All Available Authorities

```
Roles:
  - ROLE_ADMIN
  - ROLE_TRADER
  - ROLE_USER

Authorities:
  - market:read          → Market data access
  - trading:read         → Trading data read access
  - trading:place        → Place orders
  - trading:modify       → Modify orders
  - trading:cancel       → Cancel orders
  - portfolio:read       → View portfolio/positions
  - orders:read          → View order history
```

## Quick Grant Commands

### Grant All Authorities (Full Access)
```sql
UPDATE user_entities
SET authorities = 'ROLE_ADMIN,ROLE_TRADER,ROLE_USER,market:read,trading:read,trading:place,trading:modify,trading:cancel,portfolio:read,orders:read'
WHERE username = 'username';
```

### Grant Trader Authorities
```sql
UPDATE user_entities
SET authorities = 'ROLE_TRADER,ROLE_USER,market:read,trading:read,trading:place,trading:modify,trading:cancel,portfolio:read,orders:read'
WHERE username = 'username';
```

### Grant Read-Only Market Access
```sql
UPDATE user_entities
SET authorities = 'ROLE_USER,market:read'
WHERE username = 'username';
```

## Common Fixes

### Fix 403 on Portfolio Operations
```sql
UPDATE user_entities
SET authorities = authorities || ',portfolio:read'
WHERE username = 'username' AND authorities NOT LIKE '%portfolio:read%';
```

### Fix 403 on Market Data
```sql
UPDATE user_entities
SET authorities = authorities || ',market:read'
WHERE username = 'username' AND authorities NOT LIKE '%market:read%';
```

### Fix 403 on Trading
```sql
UPDATE user_entities
SET authorities = authorities || ',trading:place,trading:modify,trading:cancel'
WHERE username = 'username';
```

## Check User Authorities

```sql
SELECT username, authorities
FROM user_entities
WHERE username = 'username';
```

## Authority Hierarchy

```
ROLE_ADMIN (Full Access)
  ├─ All system operations
  ├─ Cache management
  └─ User administration

ROLE_TRADER (Trading Access)
  ├─ Place/modify/cancel orders
  ├─ View portfolio
  └─ View order history

ROLE_USER (Basic Access)
  └─ Market data viewing only
```

For detailed documentation, see [authority-role-matrix.md](./authority-role-matrix.md)
