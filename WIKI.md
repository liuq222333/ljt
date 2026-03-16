# 社区服务智能触达系统 Wiki

## 项目概述

基于事件驱动与大模型 Agent 的社区服务智能触达系统，旨在为社区居民提供便捷的本地服务、社区互动和智能推荐功能。

### 技术栈

**后端**
- Spring Boot 2.6.13
- Java 17
- MyBatis Plus 3.5.3.2
- MySQL
- Redis
- RabbitMQ
- WebSocket
- MinIO (对象存储)
- Spring Security

**前端**
- Vue 3.5.22
- Vue Router 4.4.5
- Element Plus 2.11.7
- Pinia 2.2.6
- Axios 1.13.2
- Vite 7.1.11
- TypeScript

**管理后台**
- Vue 3.5.24
- TypeScript 5.9.3
- Vite 7.2.4

---

## 项目结构

```
aaaaljt/
├── backend/
│   └── demo1/                    # Spring Boot 后端项目
│       ├── src/main/java/
│       │   └── com/example/demo/
│       │       ├── demos/        # 用户端功能模块
│       │       └── demosAdmin/   # 管理端功能模块
│       └── pom.xml
├── front/
│   └── aaljt/                    # 用户端前端项目
│       ├── src/
│       │   ├── components/
│       │   ├── router/
│       │   └── App.vue
│       └── package.json
└── front-admin/                  # 管理后台前端项目
    ├── src/
    └── package.json
```

---

## 核心功能模块

### 1. 用户认证与管理 (Login & User)

**功能**
- 用户登录/注册
- 手机验证码登录
- 图形验证码
- 用户信息管理
- 个人资料编辑
- 使用统计

**后端接口**
- `LoginController` - 登录认证
- `UserController` - 用户信息管理

**前端页面**
- `login.vue` - 登录页面
- `loginCard.vue` - 登录卡片
- `PhoneVerificationCard.vue` - 手机验证
- `GraphicalCaptcha.vue` - 图形验证码
- `Profile.vue` - 个人资料
- `Usage.vue` - 使用统计
- `settings.vue` - 账户设置

---

### 2. 社区集市 (Community Marketplace)

**功能**
- 商品发布与浏览
- 商品分类管理
- 购物车功能
- 订单管理
- 附近商品推荐
- 商品搜索与筛选

**后端接口**
- `ProductsController` - 商品管理
- `CategoryController` - 分类管理
- `CartController` - 购物车管理

**数据模型**
- `Product` - 商品信息
- `Category` - 商品分类
- `Cart` - 购物车
- `Orders` - 订单
- `ProductImages` - 商品图片
- `UserProducts` - 用户商品关联

**前端页面**
- `CommunityMarketplace.vue` - 集市首页
- `CommunityMarketplaceFind.vue` - 商品搜索
- `ProductDetail.vue` - 商品详情
- `addProduct.vue` - 发布商品
- `addNewProduct.vue` - 新增商品
- `ShopCar.vue` - 购物车
- `MyProducts.vue` - 我的商品
- `MyOrder.vue` - 我的订单
- `cebianTool.vue` - 侧边工具栏

---

### 3. 本地活动 (Local Activity)

**功能**
- 活动发布与浏览
- 活动报名管理
- 活动热力图展示
- 活动日程模板
- 邻里互助任务
- 故事档案
- 地图视图
- 通知中心

**后端接口**
- `LocalActEnrollmentController` - 活动报名
- `NeighborSupportController` - 邻里互助
- `NeighborSeriesController` - 邻里系列活动
- `LocalActivityHeatMapController` - 活动热力图

**数据模型**
- `LocalActEnrollmentRecord` - 报名记录
- `LocalActivityScheduleTemplate` - 日程模板
- `NeighborSupportTask` - 互助任务

**前端页面**
- `LocalAct.vue` - 活动首页
- `PublishActivity.vue` - 发布活动
- `ActivityDetail.vue` - 活动详情
- `MyEnrollments.vue` - 我的报名
- `MapView.vue` - 地图视图
- `NotificationsCenter.vue` - 通知中心
- `ScheduleTemplates.vue` - 日程模板
- `NeighborSupport.vue` - 邻里互助
- `StoriesArchive.vue` - 故事档案
- `StoriesArchiveDetail.vue` - 故事详情
- `StoriesArchivePublish.vue` - 发布故事

---

### 4. 社区动态 (Community Feed)

**功能**
- 发布社区动态
- 浏览动态信息流
- 图片上传
- 动态互动

**后端接口**
- `CommunityFeedController` - 动态管理
- `CommunityFeedUploadController` - 图片上传

**前端页面**
- `CommunityFeed.vue` - 动态首页
- `publishPost.vue` - 发布动态

---

### 5. 社区服务 (Community Service)

**功能**
- 社区服务信息展示
- 服务分类浏览

**前端页面**
- `CommunityService.vue` - 社区服务页面

---

### 6. 通知系统 (Notification)

**功能**
- 实时消息推送 (WebSocket)
- 系统通知
- 消息队列处理 (RabbitMQ)
- 通知历史记录

**后端实现**
- `NotificationController` - 通知接口
- `NotificationService` - 通知服务
- `NotificationSender` - 消息发送
- `NotificationMessageListener` - 消息监听
- `RabbitNotificationConfig` - RabbitMQ 配置
- `NotificationWebSocketConfig` - WebSocket 配置

**数据模型**
- `Notification` - 通知记录
- `NotificationMessage` - 通知消息

**前端页面**
- `Notification.vue` - 通知中心
- `SystemNotification.vue` - 系统通知

---

### 7. AI Agent 智能助手

**功能**
- 智能对话
- API 路由管理
- 工具调用
- DeepSeek 集成

**后端实现**
- `AgentChatController` - 对话接口
- `AgentChatService` - 对话服务
- `ApiRouteService` - API 路由管理
- `AgentToolHandler` - 工具处理器
- `AgentAiProperties` - AI 配置

**数据模型**
- `AgentChatRequest` - 对话请求
- `AgentChatResponse` - 对话响应
- `AgentChatMessage` - 对话消息
- `ApiRoute` - API 路由

---

### 8. 管理后台 (Admin)

**功能**
- 用户管理
- 活动审核
- 任务审核
- 通知发布
- 商品管理
- API 管理

**后端接口**
- `AdminNotificationController` - 通知管理
- `AdminActivityController` - 活动管理
- `UserManagementService` - 用户管理

**前端页面**
- `Home.vue` - 管理首页
- `UserManagement.vue` - 用户管理
- `ActivityReview.vue` - 活动审核
- `TaskReview.vue` - 任务审核
- `NotificationPublish.vue` - 通知发布
- `MarketProductsManagement.vue` - 商品管理
- `ApiManagement.vue` - API 管理

---

## 数据库设计

### 核心数据表

**用户相关**
- `user` - 用户基本信息

**社区集市**
- `product` - 商品信息
- `category` - 商品分类
- `cart` - 购物车
- `orders` - 订单
- `product_images` - 商品图片
- `user_products` - 用户商品关联

**本地活动**
- `local_act_enrollment` - 活动报名
- `local_activity_schedule_template` - 日程模板
- `neighbor_support_task` - 互助任务
- `neighbor_series` - 邻里系列活动

**通知系统**
- `notification` - 通知记录
- `notification_message` - 通知消息

**AI Agent**
- `api_route` - API 路由配置

---

## 环境配置

### 后端配置

**数据库**
- MySQL (需自行配置连接信息)

**Redis**
- 用于缓存和会话管理

**RabbitMQ**
- 消息队列服务
- 用于异步通知推送

**MinIO**
- 对象存储服务
- Endpoint: `http://localhost:9000`
- Access Key: `admin`
- Secret Key: `admin12345`
- Bucket: `community-feed`

### 前端配置

**Node.js 版本要求**
- Node.js ^20.19.0 或 >=22.12.0

---

## 开发指南

### 后端开发

**启动项目**
```bash
cd backend/demo1
mvn spring-boot:run
```

**主类**
- `com.example.demo.Demo1Application`

**端口**
- 默认端口: 8080

### 前端开发

**用户端**
```bash
cd front/aaljt
npm install
npm run dev
```

**管理后台**
```bash
cd front-admin
npm install
npm run dev
```

### 构建部署

**后端打包**
```bash
cd backend/demo1
mvn clean package
```

**前端打包**
```bash
# 用户端
cd front/aaljt
npm run build

# 管理后台
cd front-admin
npm run build
```

---

## API 文档

项目集成了 SpringDoc OpenAPI，启动后端服务后访问：
- Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## 安全配置

- 使用 Spring Security 进行安全管理
- 配置类: `SecurityConfig`
- 支持跨域请求 (CORS)
- 密码加密存储

---

## 异常处理

全局异常处理器: `GlobalExceptionHandler`
- 统一异常响应格式
- 资源冲突异常: `ResourceConflictException`
- 通用响应封装: `Resp`

---

## 分页支持

使用 PageHelper 实现分页功能
- 通用分页响应: `PageResponse`

---

## 文件上传

- 支持图片上传到 MinIO
- 社区动态图片上传
- 商品图片上传

---

## WebSocket 实时通信

- 实时通知推送
- 配置类: `NotificationWebSocketConfig`
- 前端使用 SockJS + STOMP 协议

---

## 项目特色

1. **事件驱动架构** - 使用 RabbitMQ 实现异步消息处理
2. **AI 智能助手** - 集成大模型 Agent 提供智能服务
3. **实时通知** - WebSocket + RabbitMQ 实现实时消息推送
4. **地理位置服务** - 支持附近商品/活动推荐
5. **热力图展示** - 活动热度可视化
6. **模块化设计** - 前后端分离，模块清晰
7. **管理后台** - 完善的后台管理功能

---

## 待完善功能

- 支付功能集成
- 更多 AI 功能扩展
- 数据统计与分析
- 移动端适配优化

---

## 开发团队

项目开发中，欢迎贡献代码和提出建议。

---

## 许可证

本项目仅供学习和研究使用。
