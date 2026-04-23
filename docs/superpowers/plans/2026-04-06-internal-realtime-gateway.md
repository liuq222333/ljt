# Internal Realtime Gateway Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a formal internal realtime gateway in `backend/demo1` with health, single query, and batch query endpoints, using `product` as the first real provider and reserving `store` / `event` protocol providers.

**Architecture:** Keep the existing realtime orchestrator as the product execution engine, add a provider registry layer on top of it, and expose a dedicated gateway controller under `/api/realtime/gateway`. Non-product entities return explicit `provider_not_implemented` responses instead of fake success.

**Tech Stack:** Spring Boot, existing realtime models/services, JUnit 5, Mockito.

---

### Task 1: Add formal gateway service and provider abstraction

**Files:**
- Create: `D:\code\aaaaljt\backend\demo1\src\main\java\com\example\demo\demos\realtime\gateway\RealtimeEntityProvider.java`
- Create: `D:\code\aaaaljt\backend\demo1\src\main\java\com\example\demo\demos\realtime\gateway\ProductRealtimeProvider.java`
- Create: `D:\code\aaaaljt\backend\demo1\src\main\java\com\example\demo\demos\realtime\gateway\StoreRealtimeProvider.java`
- Create: `D:\code\aaaaljt\backend\demo1\src\main\java\com\example\demo\demos\realtime\gateway\EventRealtimeProvider.java`
- Create: `D:\code\aaaaljt\backend\demo1\src\main\java\com\example\demo\demos\realtime\gateway\RealtimeProviderRegistry.java`
- Create: `D:\code\aaaaljt\backend\demo1\src\main\java\com\example\demo\demos\realtime\gateway\InternalRealtimeGatewayService.java`

- [ ] **Step 1: Write provider-focused failing tests**

Create unit tests that prove:
- `product` requests delegate to the orchestrator
- `store` / `event` return `implemented=false`
- unknown `entityType` returns a failed response

- [ ] **Step 2: Implement provider interface and registry**

Define a minimal provider contract:
- `supports(entityType)`
- `query(request)`
- `health()`

Registry behavior:
- resolve provider by lowercased `entityType`
- expose provider readiness summary

- [ ] **Step 3: Implement product/store/event providers**

Implementation rules:
- `ProductRealtimeProvider` delegates to `RealtimeQueryOrchestratorService`
- `StoreRealtimeProvider` and `EventRealtimeProvider` return explicit non-implemented responses with:
  - `realtimeStatus`
  - `queryMeta.implemented=false`
  - `queryMeta.reason=provider_not_implemented`

- [ ] **Step 4: Implement internal gateway service**

Service must provide:
- `health()`
- `query(request)`
- `batchQuery(requests)`

---

### Task 2: Expose formal gateway endpoints

**Files:**
- Create: `D:\code\aaaaljt\backend\demo1\src\main\java\com\example\demo\demos\realtime\controller\RealtimeGatewayController.java`
- Create: `D:\code\aaaaljt\backend\demo1\src\main\java\com\example\demo\demos\realtime\model\RealtimeBatchQueryRequest.java`
- Create: `D:\code\aaaaljt\backend\demo1\src\main\java\com\example\demo\demos\realtime\model\RealtimeBatchQueryResponse.java`
- Modify: `D:\code\aaaaljt\backend\demo1\src\main\java\com\example\demo\demos\realtime\controller\RealtimeAdminController.java`

- [ ] **Step 1: Add failing controller/service tests**

Cover:
- `GET /api/realtime/gateway/health`
- `POST /api/realtime/gateway/query`
- `POST /api/realtime/gateway/query/batch`

- [ ] **Step 2: Implement gateway controller**

Expose:
- `/api/realtime/gateway/health`
- `/api/realtime/gateway/query`
- `/api/realtime/gateway/query/batch`

Keep existing `/api/realtime/query` and admin endpoints unchanged for compatibility.

- [ ] **Step 3: Add batch request/response models**

Batch request:
- `requests: List<RealtimeQueryRequest>`

Batch response:
- `results: List<RealtimeQueryResponse>`
- `batchMeta`

---

### Task 3: Verify and document the new internal gateway

**Files:**
- Create: `D:\code\aaaaljt\backend\demo1\src\test\java\com\example\demo\demos\realtime\gateway\InternalRealtimeGatewayServiceTest.java`
- Modify: `D:\code\aaaaljt\智能检索设计\开发进度.txt`

- [ ] **Step 1: Add focused tests**

Run targeted tests for:
- provider registry/service
- controller
- existing realtime orchestrator compatibility

- [ ] **Step 2: Run compile and realtime test suite**

Run:
- `mvn -q "-Dtest=InternalRealtimeGatewayServiceTest,RealtimeQueryOrchestratorServiceTest,HttpRealtimeGatewayClientTest,ProductRealtimeFallbackServiceTest,RealtimeMockGatewayServiceTest" -DforkCount=0 test`
- `mvn -q -DskipTests compile`

- [ ] **Step 3: Update progress notes**

Document that:
- internal realtime gateway now exists
- `product` is implemented
- `store` / `event` are protocol-reserved only
