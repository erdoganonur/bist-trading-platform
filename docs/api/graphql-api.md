# GraphQL API Documentation - BIST Trading Platform

## Overview

The BIST Trading Platform GraphQL Gateway provides a unified, type-safe API for all trading platform operations. Built with Netflix DGS Framework and Spring Boot 3.3.4, it offers comprehensive schema coverage with Turkish market compliance and enterprise-grade security.

**🚀 GraphQL Gateway URL**: `http://localhost:8090/graphql`
**🎮 GraphiQL Playground**: `http://localhost:8090/graphiql`

## Key Features

- 🔄 **Unified API**: Single endpoint for all operations
- 🛡️ **Type Safety**: Comprehensive GraphQL schema with validation
- 🔐 **JWT Security**: Role-based access control with Turkish compliance
- ⚡ **Performance**: DataLoader pattern for N+1 prevention
- 🏛️ **BIST Compliance**: Turkish market regulations and TCKN validation
- 📊 **Custom Scalars**: Decimal, DateTime, and TCKN types

## GraphQL Schema Overview

The platform exposes comprehensive GraphQL schema covering all trading domains:

### Core Types

```graphql
# User Management
type User {
  id: ID!
  email: String!
  username: String!
  profile: UserProfile!
  preferences: UserPreferences
  orders(filter: OrderFilter, first: Int, after: String): OrderConnection!
  portfolio: Portfolio!
  sessions: [UserSession!]!
  createdAt: DateTime!
  updatedAt: DateTime!
}

# Trading Operations
type Order {
  id: ID!
  userId: ID!
  symbol: String!
  side: OrderSide!
  type: OrderType!
  status: OrderStatus!
  quantity: Decimal!
  price: Decimal
  stopPrice: Decimal
  timeInForce: TimeInForce!
  filledQuantity: Decimal!
  remainingQuantity: Decimal!
  averagePrice: Decimal
  commission: Decimal
  marketData: MarketData
  fills: [OrderFill!]!
  createdAt: DateTime!
  updatedAt: DateTime!
}

# Market Data
type MarketData {
  symbol: String!
  price: Decimal!
  volume: Decimal!
  high: Decimal!
  low: Decimal!
  open: Decimal!
  close: Decimal!
  change: Decimal!
  changePercent: Decimal!
  timestamp: DateTime!
  technicalIndicators: TechnicalIndicators
  marketDepth: MarketDepth
}

# Portfolio Management
type Portfolio {
  userId: ID!
  totalValue: Decimal!
  cash: Decimal!
  positions: [Position!]!
  performance: PortfolioPerformance!
  updatedAt: DateTime!
}
```

### Custom Scalars

```graphql
# Financial calculations with precision
scalar Decimal

# ISO 8601 datetime with timezone
scalar DateTime

# Turkish Identity Number with validation
scalar TCKN

# Turkish decimal formatting (comma separator)
scalar TurkishDecimal
```

### Enums

```graphql
enum OrderSide {
  BUY
  SELL
}

enum OrderType {
  MARKET
  LIMIT
  STOP_LIMIT
  STOP_MARKET
}

enum OrderStatus {
  PENDING
  OPEN
  PARTIALLY_FILLED
  FILLED
  CANCELLED
  REJECTED
  EXPIRED
}

enum TimeInForce {
  DAY
  GTC  # Good Till Cancelled
  IOC  # Immediate Or Cancel
  FOK  # Fill Or Kill
}

enum KYCLevel {
  BASIC
  INTERMEDIATE
  ADVANCED
}

enum UserStatus {
  ACTIVE
  INACTIVE
  SUSPENDED
  PENDING_VERIFICATION
}
```

## Core Operations

### 1. User Management

#### Get Current User
```graphql
query GetCurrentUser {
  me {
    id
    email
    username
    profile {
      firstName
      lastName
      dateOfBirth
      nationality
      kycLevel
      status
    }
    preferences {
      language
      timezone
      currency
      notifications {
        emailNotifications
        smsNotifications
        tradingAlerts
      }
    }
  }
}
```

#### Update User Profile
```graphql
mutation UpdateUserProfile($input: UpdateProfileInput!) {
  updateProfile(input: $input) {
    id
    profile {
      firstName
      lastName
      phoneNumber
      address {
        street
        city
        postalCode
        country
      }
    }
    updatedAt
  }
}
```

**Variables:**
```json
{
  "input": {
    "firstName": "Ahmet",
    "lastName": "Yılmaz",
    "phoneNumber": "+905551234567",
    "address": {
      "street": "Levent Mahallesi",
      "city": "İstanbul",
      "postalCode": "34330",
      "country": "Turkey"
    }
  }
}
```

### 2. Market Data Operations

#### Get Real-time Market Data
```graphql
query GetMarketData($symbol: String!) {
  marketData(symbol: $symbol) {
    symbol
    price
    volume
    high
    low
    open
    change
    changePercent
    timestamp
    technicalIndicators {
      rsi
      macd {
        macd
        signal
        histogram
      }
      movingAverages {
        ma5
        ma10
        ma20
        ma50
        ma200
      }
    }
    marketDepth {
      bids {
        price
        quantity
      }
      asks {
        price
        quantity
      }
    }
  }
}
```

#### Get Multiple Market Data
```graphql
query GetMultipleMarketData($symbols: [String!]!) {
  marketDataMultiple(symbols: $symbols) {
    symbol
    price
    change
    changePercent
    volume
    timestamp
  }
}
```

**Variables:**
```json
{
  "symbols": ["THYAO", "AKBNK", "GARAN", "ISCTR", "SISE"]
}
```

### 3. Trading Operations

#### Create Order
```graphql
mutation CreateOrder($input: CreateOrderInput!) {
  createOrder(input: $input) {
    success
    order {
      id
      symbol
      side
      type
      quantity
      price
      status
      createdAt
    }
    errors {
      message
      code
    }
  }
}
```

**Variables:**
```json
{
  "input": {
    "symbol": "THYAO",
    "side": "BUY",
    "type": "LIMIT",
    "quantity": "100",
    "price": "45.50",
    "timeInForce": "DAY"
  }
}
```

#### Get Order History
```graphql
query GetOrderHistory(
  $filter: OrderFilter,
  $first: Int,
  $after: String
) {
  orders(filter: $filter, first: $first, after: $after) {
    edges {
      node {
        id
        symbol
        side
        type
        status
        quantity
        price
        filledQuantity
        averagePrice
        commission
        createdAt
        updatedAt
      }
    }
    pageInfo {
      hasNextPage
      hasPreviousPage
      startCursor
      endCursor
    }
    totalCount
  }
}
```

**Variables:**
```json
{
  "filter": {
    "symbol": "THYAO",
    "status": ["FILLED", "PARTIALLY_FILLED"],
    "dateRange": {
      "from": "2024-01-01T00:00:00Z",
      "to": "2024-12-31T23:59:59Z"
    }
  },
  "first": 10
}
```

#### Cancel Order
```graphql
mutation CancelOrder($orderId: ID!, $reason: String) {
  cancelOrder(id: $orderId, reason: $reason) {
    success
    order {
      id
      status
      updatedAt
    }
    message
  }
}
```

### 4. Portfolio Operations

#### Get Portfolio
```graphql
query GetPortfolio {
  portfolio {
    totalValue
    cash
    positions {
      symbol
      quantity
      averagePrice
      marketValue
      unrealizedPnl
      unrealizedPnlPercent
      dayChange
      dayChangePercent
    }
    performance {
      totalReturn
      totalReturnPercent
      dayReturn
      dayReturnPercent
      weekReturn
      monthReturn
      yearReturn
    }
    updatedAt
  }
}
```

#### Get Portfolio Performance History
```graphql
query GetPortfolioPerformance($period: TimePeriod!) {
  portfolioPerformance(period: $period) {
    date
    totalValue
    dayReturn
    cumulativeReturn
    benchmark
  }
}
```

## Real-time Subscriptions

### Market Data Updates
```graphql
subscription MarketDataUpdates($symbols: [String!]!) {
  marketDataUpdates(symbols: $symbols) {
    symbol
    price
    volume
    change
    changePercent
    timestamp
  }
}
```

### Order Status Updates
```graphql
subscription OrderStatusUpdates($userId: ID!) {
  orderStatusUpdates(userId: $userId) {
    orderId
    status
    filledQuantity
    remainingQuantity
    averagePrice
    timestamp
  }
}
```

### Portfolio Updates
```graphql
subscription PortfolioUpdates($userId: ID!) {
  portfolioUpdates(userId: $userId) {
    totalValue
    dayReturn
    positions {
      symbol
      marketValue
      unrealizedPnl
    }
    timestamp
  }
}
```

## Authentication & Authorization

### JWT Authentication
All GraphQL operations (except public queries) require JWT authentication:

```http
POST /graphql
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "query": "query GetCurrentUser { me { id email } }"
}
```

### Role-Based Access Control

The system implements comprehensive RBAC:

```graphql
# Admin-only operations
query {
  users(first: 10) {  # Requires ADMIN role
    edges {
      node {
        id
        email
        status
      }
    }
  }
}

# User-specific data access
query {
  me {  # Accessible by authenticated users
    profile {
      firstName
      lastName
    }
  }
}

# Trading operations
mutation {
  createOrder(input: $input) {  # Requires verified KYC
    success
    order { id }
  }
}
```

### Permission Matrix

| Operation | Required Role | Additional Requirements |
|-----------|---------------|------------------------|
| `me` | USER | Authenticated |
| `users` | ADMIN | Admin privileges |
| `createOrder` | USER | Verified KYC, trading enabled |
| `cancelOrder` | USER | Own orders or ADMIN |
| `marketData` | USER | Basic market data access |
| `premiumMarketData` | PREMIUM_USER | Premium subscription |
| `adminOperations` | ADMIN | Administrative access |

## Error Handling

### GraphQL Error Format
```json
{
  "errors": [
    {
      "message": "Authentication required",
      "messageTR": "Kimlik doğrulama gerekli",
      "extensions": {
        "code": "UNAUTHENTICATED",
        "timestamp": "2024-01-15T10:30:00Z"
      },
      "path": ["me"],
      "locations": [{"line": 2, "column": 3}]
    }
  ],
  "data": null
}
```

### Common Error Codes

| Code | Description | Turkish Message |
|------|-------------|-----------------|
| `UNAUTHENTICATED` | Authentication required | Kimlik doğrulama gerekli |
| `FORBIDDEN` | Access denied | Erişim reddedildi |
| `INVALID_INPUT` | Invalid input data | Geçersiz girdi verisi |
| `ORDER_REJECTED` | Order rejected by broker | Emir komisyoncu tarafından reddedildi |
| `INSUFFICIENT_BALANCE` | Insufficient balance | Yetersiz bakiye |
| `KYC_REQUIRED` | KYC verification required | KYC doğrulaması gerekli |
| `MARKET_CLOSED` | Market is closed | Piyasa kapalı |

## Performance Optimization

### DataLoader Pattern
The GraphQL gateway implements DataLoader pattern to prevent N+1 queries:

```graphql
query GetOrdersWithMarketData {
  orders(first: 10) {
    edges {
      node {
        id
        symbol
        marketData {  # Batched with DataLoader
          price
          change
        }
      }
    }
  }
}
```

### Query Complexity Analysis
- **Basic User**: Max complexity 500
- **Premium User**: Max complexity 1,000
- **Professional**: Max complexity 2,000
- **Admin**: Max complexity 5,000
- **Unlimited**: No complexity limits

### Caching Strategy
- **Redis caching** for frequently accessed data
- **Per-user session caching** for personalized data
- **Market data caching** with TTL-based expiration
- **Query result caching** for expensive operations

## Rate Limiting

### Rate Limits by User Type

| User Type | Requests/Minute | Burst Capacity |
|-----------|-----------------|----------------|
| Basic | 60 | 100 |
| Premium | 120 | 200 |
| Professional | 300 | 500 |
| Admin | 600 | 1000 |

### Rate Limit Headers
```http
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1642248000
```

## Integration Examples

### JavaScript/TypeScript Client
```typescript
import { ApolloClient, InMemoryCache, gql } from '@apollo/client';

const client = new ApolloClient({
  uri: 'http://localhost:8090/graphql',
  cache: new InMemoryCache(),
  headers: {
    'Authorization': `Bearer ${jwtToken}`
  }
});

// Get market data
const GET_MARKET_DATA = gql`
  query GetMarketData($symbol: String!) {
    marketData(symbol: $symbol) {
      symbol
      price
      change
      changePercent
      timestamp
    }
  }
`;

const { data } = await client.query({
  query: GET_MARKET_DATA,
  variables: { symbol: 'THYAO' }
});
```

### cURL Examples
```bash
# Get current user
curl -X POST http://localhost:8090/graphql \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { me { id email profile { firstName lastName } } }"
  }'

# Create order
curl -X POST http://localhost:8090/graphql \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation CreateOrder($input: CreateOrderInput!) { createOrder(input: $input) { success order { id status } errors { message } } }",
    "variables": {
      "input": {
        "symbol": "THYAO",
        "side": "BUY",
        "type": "LIMIT",
        "quantity": "100",
        "price": "45.50"
      }
    }
  }'
```

## Monitoring & Metrics

### GraphQL Specific Metrics
- Query execution time
- Resolver performance
- Schema introspection usage
- Error rates by operation
- DataLoader effectiveness

### Health Endpoints
- **GraphQL Health**: `http://localhost:8090/actuator/health`
- **Schema Endpoint**: `http://localhost:8090/graphql/schema`
- **Metrics**: `http://localhost:8090/actuator/metrics`

## Development Tools

### GraphiQL Playground
Access the interactive GraphQL explorer at:
`http://localhost:8090/graphiql`

Features:
- 🔍 Schema exploration
- 📝 Query composition with autocomplete
- 🧪 Mutation testing
- 📊 Real-time subscription testing
- 🔐 JWT authentication integration

### Schema Introspection
```graphql
query IntrospectionQuery {
  __schema {
    types {
      name
      description
    }
    queryType {
      name
    }
    mutationType {
      name
    }
    subscriptionType {
      name
    }
  }
}
```

---

## Support & Resources

### Documentation Links
- **GraphQL Spec**: https://graphql.org/
- **Netflix DGS**: https://netflix.github.io/dgs/
- **Apollo Client**: https://www.apollographql.com/docs/react/

### Contact Information
- **GraphQL Team**: graphql@bist-trading.com
- **API Support**: api-support@bist-trading.com
- **Technical Issues**: [GitHub Issues](https://github.com/your-org/bist-trading-platform/issues)

---

**Last Updated**: December 2024
**GraphQL Schema Version**: 1.0
**API Gateway Version**: 1.0.0-SNAPSHOT