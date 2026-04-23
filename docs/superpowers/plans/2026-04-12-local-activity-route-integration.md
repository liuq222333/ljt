# Local Activity Route Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让聊天主链支持“当前有哪些活动 / 我附近有什么活动”，并统一走数据库 `api_routes` 执行链。

**Architecture:** 在现有 `AgentRuntime -> ActionIntentReviewService -> BackendApiProxyService` 主链上新增一个 `LocalActivityActionAdapter`。adapter 负责活动查询意图识别、参数抽取、route 选择和结果摘要化；实际接口执行仍统一走数据库 route 和 `BackendApiProxyService`。

**Tech Stack:** Spring Boot, MyBatis-Plus, RestTemplate, JUnit, Mockito

---

### Task 1: Add local activity adapter

**Files:**
- Create: `D:/code/aaaaljt/backend/demo1/src/main/java/com/example/demo/demos/Agent/Runtime/LocalActivityActionAdapter.java`
- Test: `D:/code/aaaaljt/backend/demo1/src/test/java/com/example/demo/demos/Agent/Runtime/LocalActivityActionAdapterTest.java`

- [ ] **Step 1: Write the failing adapter tests**

Add tests for:
- `当前有哪些活动` -> selects `local_activity/list`
- `我附近有什么活动` with lat/lon -> selects `local_activity/nearby`
- `我附近有什么活动` without lat/lon -> asks for location

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
mvn -q "-Dtest=LocalActivityActionAdapterTest" -DforkCount=0 test
```

Expected: FAIL because adapter class does not exist.

- [ ] **Step 3: Implement minimal adapter**

Create `LocalActivityActionAdapter` with responsibilities:
- detect local activity read intent
- extract `lat/lon/radiusKm/keyword/category`
- choose route candidates from `api_routes`
- build `BackendApiProxyService.InvocationRequest`
- format list/nearby responses into concise Chinese summaries

- [ ] **Step 4: Run adapter tests**

Run:
```powershell
mvn -q "-Dtest=LocalActivityActionAdapterTest" -DforkCount=0 test
```

Expected: PASS

### Task 2: Wire adapter into action review

**Files:**
- Modify: `D:/code/aaaaljt/backend/demo1/src/main/java/com/example/demo/demos/Agent/Runtime/ActionIntentReviewService.java`
- Test: `D:/code/aaaaljt/backend/demo1/src/test/java/com/example/demo/demos/Agent/Runtime/AgentRuntimeTest.java`

- [ ] **Step 1: Add failing runtime tests**

Add tests for:
- local activity list query enters adapter path
- nearby activity query without location returns clarification
- product action flow still works and is not stolen by activity adapter

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
mvn -q "-Dtest=AgentRuntimeTest" -DforkCount=0 test
```

Expected: FAIL on new local activity expectations.

- [ ] **Step 3: Integrate adapter**

Update `ActionIntentReviewService` to:
- inject `LocalActivityActionAdapter`
- check adapter before product write flow only when message matches local activity read intent
- return handled review result from adapter when matched

- [ ] **Step 4: Run runtime tests**

Run:
```powershell
mvn -q "-Dtest=AgentRuntimeTest,LocalActivityActionAdapterTest" -DforkCount=0 test
```

Expected: PASS

### Task 3: Seed or verify api_routes entries

**Files:**
- Modify: `D:/code/aaaaljt/backend/demo1/src/test/java/com/example/demo/demos/Agent/Runtime/LocalActivityActionAdapterTest.java`
- Optional Docs: `D:/code/aaaaljt/智能检索设计/开发进度.txt`

- [ ] **Step 1: Assert required routes exist in tests**

In test fixtures, provide mocked routes for:
- `resource=local_activity, action=list, operation_type=READ, path=/api/local-act/activities/list`
- `resource=local_activity, action=nearby, operation_type=READ, path=/api/local-act/activities/nearby`

- [ ] **Step 2: If live DB seeding is needed, add minimal verification note**

Document that production/local DB must include the two enabled `api_routes` records.

- [ ] **Step 3: Run focused test suite**

Run:
```powershell
mvn -q "-Dtest=LocalActivityActionAdapterTest,AgentRuntimeTest" -DforkCount=0 test
```

Expected: PASS

### Task 4: End-to-end verification

**Files:**
- Modify if needed: `D:/code/aaaaljt/backend/demo1/src/main/java/com/example/demo/demos/Agent/Runtime/RuntimeAnswerComposer.java`

- [ ] **Step 1: Compile backend**

Run:
```powershell
mvn -q -DskipTests compile
```

Expected: BUILD SUCCESS

- [ ] **Step 2: Run full focused suite**

Run:
```powershell
mvn -q "-Dtest=AgentRuntimeTest,LocalActivityActionAdapterTest,AdminLaunchServiceTest" -DforkCount=0 test
```

Expected: PASS

- [ ] **Step 3: Smoke test live interface**

Verify:
- `POST /api/agent/chat` with `当前有哪些活动`
- `POST /api/agent/chat` with `我附近有什么活动`

Expected:
- first returns activity list summary or explicit empty result
- second returns nearby result if coordinates available, otherwise clarification

