# 社区服务智能触达系统 - 详细技术文档（续1）

## 后端API详细说明（续）

### 模块四：本地活动模块 (LocalActive)

**包路径**: `com.example.demo.demos.LocalActive`

#### 4.1 LocalActEnrollmentController

**路径**: `/api/local-act`

**类说明**: 本地活动报名、创建、查询等核心功能

**依赖服务**:
- `LocalActEnrollmentService` - 报名服务
- `LocalActivityService` - 活动服务
- `LocalScheduleTemplateService` - 日程模板服务
- `LocalActivitySearchService` - 活动搜索服务
- `StringRedisTemplate` - Redis操作

**接口列表**:

##### 4.1.1 获取用户报名记录
```java
GET /api/local-act/enrollments
```
**功能**: 查询用户的活动报名记录，支持多条件筛选
**参数**:
- `username` - 用户名（必填）
- `status` - 报名状态（可选）：pending/confirmed/cancelled
- `period` - 时间段（可选）：upcoming/past/all
- `keyword` - 关键词搜索（可选）
**响应**: `LocalActEnrollmentListResponse`
```json
{
  "items": [
    {
      "enrollmentId": 0,
      "activityId": 0,
      "activityTitle": "string",
      "activityTime": "2024-01-01T10:00:00",
      "location": "string",
      "status": "confirmed",
      "enrolledAt": "2024-01-01T09:00:00"
    }
  ],
  "stats": {
    "total": 10,
    "upcoming": 5,
    "past": 5,
    "cancelled": 0
  }
}
```

##### 4.1.2 创建活动
```java
POST /api/local-act/activities
```
**功能**: 创建新活动，提交到管理员审核表
**请求体**: `LocalActCreateRequest`
```json
{
  "title": "string",
  "description": "string",
  "category": "string",
  "startTime": "2024-01-01T10:00:00",
  "endTime": "2024-01-01T12:00:00",
  "location": "string",
  "latitude": 0.0,
  "longitude": 0.0,
  "maxParticipants": 50,
  "organizerId": 0,
  "contactInfo": "string",
  "tags": ["tag1", "tag2"]
}
```
**响应**: `Resp<LocalActCreateResponse>`
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "activityId": 123,
    "status": "pending_review",
    "message": "活动已提交，等待管理员审核"
  }
}
```

##### 4.1.3 创建日程模板
```java
POST /api/local-act/schedule-templates
```
**功能**: 创建固定日程模板（如每周固定活动）
**请求体**: `LocalActScheduleTemplateRequest`
```json
{
  "title": "string",
  "description": "string",
  "category": "string",
  "recurrenceRule": "WEEKLY",
  "dayOfWeek": 1,
  "startTime": "10:00:00",
  "duration": 120,
  "location": "string",
  "organizerId": 0
}
```
**响应**: `Resp<LocalActScheduleTemplateResponse>`

##### 4.1.4 查询活动列表
```java
GET /api/local-act/activities/list
```
**功能**: 分页查询活动列表
**参数**:
- `status` - 活动状态（可选）：active/completed/cancelled
- `page` - 页码（默认1）
- `size` - 每页数量（默认10）
**响应**: `Resp<List<LocalActivity>>`

##### 4.1.5 附近活动查询
```java
GET /api/local-act/activities/nearby
```
**功能**: 基于地理位置查询附近活动（使用RediSearch）
**参数**:
- `lat` - 纬度（必填）
- `lon` - 经度（必填）
- `radiusKm` - 搜索半径（默认50公里）
- `category` - 分类筛选（可选）
- `keyword` - 关键词搜索（可选）
- `size` - 返回数量（默认20）
**响应**: `Resp<List<NearbyActivityDTO>>`
```json
{
  "code": 200,
  "data": [
    {
      "activityId": 0,
      "title": "string",
      "category": "string",
      "startTime": "2024-01-01T10:00:00",
      "location": "string",
      "distance": 2.5,
      "participants": 20,
      "maxParticipants": 50
    }
  ]
}
```

##### 4.1.6 同步活动到Redis
```java
POST /api/local-act/activities/sync-redis
```
**功能**: 批量同步数据库活动到Redis索引
**响应**: `Resp<Integer>` - 同步数量

##### 4.1.7 Redis连接测试
```java
GET /api/local-act/redis/ping
```
**功能**: 测试Redis连接状态
**响应**: `Resp<String>`
```json
{
  "code": 200,
  "data": "write/read ok, value=pong"
}
```

---

#### 4.2 NeighborSupportController

**路径**: `/api/neighbor-support`

**类说明**: 邻里互助任务管理

**依赖服务**:
- `NeighborSupportService` - 互助服务

**接口列表**:

##### 4.2.1 发布互助任务
```java
POST /api/neighbor-support/tasks
```
**功能**: 发布邻里互助任务
**请求体**: `NeighborSupportTaskRequest`
```json
{
  "title": "string",
  "description": "string",
  "category": "help_needed",
  "publisherId": 0,
  "location": "string",
  "deadline": "2024-01-01T18:00:00",
  "reward": "string"
}
```
**响应**: `Resp<NeighborSupportTaskResponse>`
```json
{
  "code": 200,
  "data": {
    "taskId": 123,
    "status": "open",
    "createdAt": "2024-01-01T10:00:00"
  }
}
```

##### 4.2.2 查询互助任务列表
```java
GET /api/neighbor-support/tasks
```
**功能**: 查询邻里互助任务列表
**参数**:
- `status` - 任务状态（可选）：open/in_progress/completed/cancelled
**响应**: `Resp<List<NeighborSupportTaskDTO>>`

---

### 模块五：社区动态模块 (CommunityFeed)

**包路径**: `com.example.demo.demos.CommunityFeed`

#### 5.1 CommunityFeedController

**路径**: `/api/community-feed`

**类说明**: 社区动态发布、浏览、互动功能

**依赖服务**:
- `CommunityFeedService` - 动态服务
- `MinioClient` - 图片存储
- `UserService` - 用户服务

**接口列表**:

##### 5.1.1 发布动态
```java
POST /api/community-feed/posts
```
**功能**: 发布社区动态
**请求体**: `CommunityFeedPostRequest`
```json
{
  "userId": 0,
  "content": "string",
  "images": ["key1", "key2"],
  "visibility": "public",
  "locationText": "string"
}
```
**响应**: `CommunityFeed`

##### 5.1.2 动态列表
```java
GET /api/community-feed/posts
```
**功能**: 获取动态列表（按时间倒序）
**参数**:
- `visibility` - 可见性（可选）：public/friends/private
- `page` - 页码（默认1）
- `size` - 每页数量（默认10）
**响应**: `List<CommunityFeedResponse>`
```json
[
  {
    "id": 0,
    "userId": 0,
    "username": "string",
    "userAvatar": "预签名URL",
    "content": "string",
    "images": ["预签名URL1", "预签名URL2"],
    "visibility": "public",
    "locationText": "string",
    "likesCount": 10,
    "commentsCount": 5,
    "status": "active",
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  }
]
```

##### 5.1.3 动态详情
```java
GET /api/community-feed/posts/{id}
```
**功能**: 获取单条动态详情
**响应**: `CommunityFeedResponse`

##### 5.1.4 点赞动态
```java
POST /api/community-feed/posts/{id}/like
```
**功能**: 给动态点赞
**参数**:
- `userId` - 用户ID
**响应**: HTTP 200 OK

##### 5.1.5 取消点赞
```java
DELETE /api/community-feed/posts/{id}/like
```
**功能**: 取消点赞
**参数**:
- `userId` - 用户ID
**响应**: HTTP 200 OK

##### 5.1.6 发表评论
```java
POST /api/community-feed/posts/{id}/comments
```
**功能**: 对动态发表评论
**请求体**: `CommunityFeedCommentRequest`
```json
{
  "userId": 0,
  "content": "string"
}
```
**响应**: `CommunityFeedComment`
```json
{
  "id": 0,
  "postId": 0,
  "userId": 0,
  "content": "string",
  "createdAt": "2024-01-01T10:00:00"
}
```

##### 5.1.7 评论列表
```java
GET /api/community-feed/posts/{id}/comments
```
**功能**: 获取动态的评论列表
**参数**:
- `page` - 页码（默认1）
- `size` - 每页数量（默认10）
**响应**: `List<CommunityFeedComment>`

##### 5.1.8 删除评论
```java
DELETE /api/community-feed/posts/{postId}/comments/{commentId}
```
**功能**: 删除评论（仅评论作者或动态作者可删除）
**参数**:
- `userId` - 用户ID
**响应**: HTTP 200 OK / 403 Forbidden

##### 5.1.9 删除动态
```java
DELETE /api/community-feed/posts/{id}
```
**功能**: 删除动态（仅作者可删除）
**参数**:
- `userId` - 用户ID
**响应**: HTTP 200 OK / 403 Forbidden

##### 5.1.10 点赞用户列表
```java
GET /api/community-feed/posts/{id}/likes
```
**功能**: 获取点赞用户ID列表
**参数**:
- `page` - 页码（默认1）
- `size` - 每页数量（默认20）
**响应**: `List<Long>` - 用户ID列表

##### 5.1.11 上传头像
```java
POST /api/community-feed/avatar/upload
Content-Type: multipart/form-data
```
**功能**: 上传用户头像到MinIO并更新用户表
**参数**:
- `userId` - 用户ID
- `file` - 图片文件
**响应**:
```json
{
  "avatarKey": "avatars/uuid-filename.jpg",
  "avatarUrl": "预签名URL"
}
```

---

### 模块六：通知系统模块 (Notification)

**包路径**: `com.example.demo.demos.Notification`

#### 6.1 NotificationController

**路径**: `/api/notifications`

**类说明**: 通知公告管理，支持实时推送

**依赖服务**:
- `NotificationService` - 通知服务

**接口列表**:

##### 6.1.1 添加通知
```java
POST /api/notifications/add
```
**功能**: 创建新通知公告
**请求体**: `Notification`
```json
{
  "userId": 0,
  "title": "string",
  "content": "string",
  "type": "system",
  "priority": "normal",
  "readStatus": 0
}
```
**响应**: `Resp<Void>`

##### 6.1.2 获取通知列表
```java
GET /api/notifications
```
**功能**: 获取用户的通知列表
**参数**:
- `userId` - 用户ID（必填）
- `readStatus` - 读取状态（可选）：0-未读，1-已读
**响应**: `Resp<List<Notification>>`

##### 6.1.3 标记单条已读
```java
PATCH /api/notifications/{id}/read
```
**功能**: 标记指定通知为已读
**参数**:
- `userId` - 用户ID
**响应**: `Resp<Void>`

##### 6.1.4 全部标记已读
```java
POST /api/notifications/markAllRead
```
**功能**: 标记用户所有通知为已读
**参数**:
- `userId` - 用户ID
**响应**: `Resp<Void>`

##### 6.1.5 未读数量
```java
GET /api/notifications/unreadCount?userId={userId}
```
**功能**: 获取用户未读通知数量
**响应**: `Resp<Integer>`

##### 6.1.6 未读数量（兼容接口）
```java
GET /api/notifications/unread?userId={userId}
```
**功能**: 获取未读数量（兼容旧版路径）
**响应**: `Resp<Integer>`

##### 6.1.7 异步发送通知
```java
POST /api/notifications/send
```
**功能**: 通过RabbitMQ异步发送通知
**请求体**: `NotificationMessage`
```json
{
  "userId": 0,
  "title": "string",
  "content": "string",
  "type": "system",
  "priority": "high"
}
```
**响应**: `Resp<Void>`
**业务逻辑**:
1. 将通知消息发送到RabbitMQ队列
2. 消息监听器异步处理
3. 通过WebSocket实时推送给在线用户
4. 保存到数据库

##### 6.1.8 删除通知
```java
DELETE /api/notifications/{id}
```
**功能**: 删除指定通知
**参数**:
- `userId` - 用户ID（权限验证）
**响应**: `Resp<Void>`

---

### 模块七：AI智能助手模块 (Agent)

**包路径**: `com.example.demo.demos.Agent`

#### 7.1 AgentChatController

**路径**: `/api/agent`

**类说明**: AI智能助手对话接口，集成DeepSeek大模型

**依赖服务**:
- `AgentChatService` - 对话服务

**接口列表**:

##### 7.1.1 智能对话
```java
POST /api/agent/chat
```
**功能**: 与AI助手进行对话，支持工具调用
**请求头**: `Authorization: Bearer token` (可选)
**请求体**: `AgentChatRequest`
```json
{
  "messages": [
    {
      "role": "user",
      "content": "帮我查询附近的活动"
    }
  ],
  "sessionId": "string"
}
```
**响应**: `Resp<AgentChatResponse>`
```json
{
  "code": 200,
  "data": {
    "reply": "string",
    "toolCalls": [
      {
        "toolName": "searchNearbyActivities",
        "parameters": {},
        "result": {}
      }
    ],
    "sessionId": "string"
  }
}
```
**业务逻辑**:
1. 接收用户消息历史
2. 调用DeepSeek API进行对话
3. 如果需要工具调用，执行相应的后端API
4. 将工具结果返回给模型
5. 返回最终回复给用户

**支持的工具**:
- 查询附近活动
- 查询附近商品
- 查询用户信息
- 发布动态
- 创建活动
- 等等（通过ApiRoute配置）

---

继续下一部分...
