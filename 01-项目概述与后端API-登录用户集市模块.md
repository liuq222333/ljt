# 社区服务智能触达系统 - 详细技术文档

## 目录

1. [项目概述](#项目概述)
2. [技术架构](#技术架构)
3. [后端API详细说明](#后端api详细说明)
4. [前端页面详细说明](#前端页面详细说明)
5. [数据库设计](#数据库设计)
6. [部署指南](#部署指南)

---

## 项目概述

### 项目简介
基于事件驱动与大模型 Agent 的社区服务智能触达系统，为社区居民提供本地活动、社区集市、邻里互助、社区动态等综合服务。

### 核心特性
- **事件驱动架构** - RabbitMQ 消息队列实现异步处理
- **AI 智能助手** - 集成 DeepSeek 大模型提供智能对话
- **实时通信** - WebSocket + RabbitMQ 实现实时消息推送
- **地理位置服务** - Redis GEO + RediSearch 实现附近商品/活动推荐
- **对象存储** - MinIO 存储图片和文件
- **分布式缓存** - Redis 缓存热点数据

### 技术栈

**后端技术**
- Spring Boot 2.6.13
- Java 17
- MyBatis Plus 3.5.3.2
- MySQL 数据库
- Redis 缓存
- RabbitMQ 消息队列
- WebSocket 实时通信
- MinIO 对象存储
- Spring Security 安全框架
- SpringDoc OpenAPI 3.0 (Swagger)
- PageHelper 分页插件
- Lombok 简化代码

**前端技术**
- Vue 3.5.22 (Composition API)
- TypeScript
- Vue Router 4.4.5
- Pinia 2.2.6 状态管理
- Element Plus 2.11.7 UI 组件库
- Axios 1.13.2 HTTP 客户端
- Vite 7.1.11 构建工具
- SockJS + STOMP WebSocket 客户端

---

## 技术架构

### 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        前端层                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  用户端 Vue3  │  │ 管理后台 Vue3 │  │  移动端 H5   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            ↓ HTTP/WebSocket
┌─────────────────────────────────────────────────────────────┐
│                      应用服务层                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Spring Boot 应用服务                      │  │
│  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐        │  │
│  │  │登录认证│ │社区集市│ │本地活动│ │AI助手 │        │  │
│  │  └────────┘ └────────┘ └────────┘ └────────┘        │  │
│  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐        │  │
│  │  │社区动态│ │通知系统│ │用户管理│ │管理后台│        │  │
│  │  └────────┘ └────────┘ └────────┘ └────────┘        │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                      中间件层                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │  MySQL   │  │  Redis   │  │ RabbitMQ │  │  MinIO   │  │
│  │  数据库   │  │  缓存    │  │ 消息队列  │  │ 对象存储  │  │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 项目目录结构

```
aaaaljt/
├── backend/demo1/                          # 后端项目
│   ├── src/main/java/com/example/demo/
│   │   ├── Demo1Application.java          # 启动类
│   │   ├── config/                        # 配置类
│   │   │   ├── MinioProperties.java       # MinIO配置
│   │   │   └── ...
│   │   ├── demos/                         # 用户端功能模块
│   │   │   ├── Login/                     # 登录认证模块
│   │   │   ├── User/                      # 用户管理模块
│   │   │   ├── CommunityMarket/           # 社区集市模块
│   │   │   ├── LocalActive/               # 本地活动模块
│   │   │   ├── CommunityFeed/             # 社区动态模块
│   │   │   ├── Notification/              # 通知系统模块
│   │   │   ├── Agent/                     # AI助手模块
│   │   │   ├── security/                  # 安全配置
│   │   │   ├── exception/                 # 异常处理
│   │   │   ├── generic/                   # 通用响应
│   │   │   └── PageResponse/              # 分页响应
│   │   └── demosAdmin/                    # 管理端功能模块
│   │       ├── Notification/              # 通知管理
│   │       ├── AdminActivity/             # 活动管理
│   │       └── UserManagement/            # 用户管理
│   ├── src/main/resources/
│   │   ├── application.yml                # 应用配置
│   │   └── application.properties         # 属性配置
│   └── pom.xml                            # Maven依赖
├── front/aaljt/                           # 用户端前端
│   ├── src/
│   │   ├── components/                    # 组件目录
│   │   │   ├── LoginPages/                # 登录页面
│   │   │   ├── Home/                      # 首页
│   │   │   │   ├── CommunityMarketplace/  # 社区集市
│   │   │   │   ├── LocalAct/              # 本地活动
│   │   │   │   ├── CommunityFeed/         # 社区动态
│   │   │   │   └── CommunityService/      # 社区服务
│   │   │   ├── User/                      # 用户中心
│   │   │   ├── Notification/              # 通知中心
│   │   │   ├── PublicServices/            # 公共服务
│   │   │   └── Business/                  # 商家服务
│   │   ├── router/                        # 路由配置
│   │   └── App.vue                        # 根组件
│   └── package.json
└── front-admin/                           # 管理后台前端
    ├── src/
    │   └── components/                    # 管理组件
    │       ├── Home.vue                   # 管理首页
    │       ├── UserManagement.vue         # 用户管理
    │       ├── ActivityReview.vue         # 活动审核
    │       ├── TaskReview.vue             # 任务审核
    │       ├── NotificationPublish.vue    # 通知发布
    │       ├── MarketProductsManagement.vue # 商品管理
    │       └── ApiManagement.vue          # API管理
    └── package.json
```

---

## 后端API详细说明

### 模块一：登录认证模块 (Login)

**包路径**: `com.example.demo.demos.Login`

#### 1.1 LoginController

**路径**: `/api`

**类说明**: 处理用户登录、注册、验证码等认证相关功能

**依赖服务**:
- `LoginService` - 登录业务逻辑
- `UserService` - 用户服务
- `StringRedisTemplate` - Redis操作

**接口列表**:

##### 1.1.1 用户登录
```java
POST /api/login
```
**功能**: 用户名密码登录
**请求体**:
```json
{
  "username": "string",
  "password": "string"
}
```
**响应**:
```json
{
  "token": "string",
  "userName": "string",
  "userId": 0
}
```
**业务逻辑**:
1. 验证用户名和密码是否为空
2. 调用 `loginService.validateLogin()` 验证凭证
3. 生成 token (Base64编码: username:timestamp)
4. 返回 token、用户名和用户ID

##### 1.1.2 用户注册
```java
POST /api/register
```
**功能**: 新用户注册
**请求体**:
```json
{
  "username": "string",
  "password": "string",
  "phone": "string"
}
```
**响应**: HTTP 201 Created / 409 Conflict
**业务逻辑**:
1. 调用 `loginService.register()` 创建用户
2. 检查用户名是否已存在
3. 成功返回201，失败返回409

##### 1.1.3 检查用户是否存在
```java
GET /api/UserIsExist?userName={userName}
```
**功能**: 检查用户名是否已被注册
**响应**:
```json
{
  "exists": true
}
```

##### 1.1.4 获取用户信息（按用户名）
```java
GET /api/getUserByName?userName={userName}
```
**功能**: 根据用户名获取用户详细信息
**响应**: User对象 / 404 Not Found

##### 1.1.5 获取用户信息（按ID）
```java
GET /api/getUserById/{userId}
```
**功能**: 根据用户ID获取用户详细信息
**响应**: User对象 / 404 Not Found

##### 1.1.6 发送短信验证码
```java
POST /api/sms/send-code
```
**功能**: 发送6位数字验证码到手机
**请求体**:
```json
{
  "phone": "13800138000"
}
```
**响应**:
```json
{
  "message": "验证码已发送"
}
```
**业务逻辑**:
1. 验证手机号格式（正则: `^1[3-9]\\d{9}$`）
2. 使用Redis SETNX实现60秒冷却时间
3. 生成6位随机验证码
4. 存储到Redis，有效期5分钟
5. 日志记录验证码（生产环境应发送短信）

**限流机制**:
- 60秒内只能发送一次
- 超过限制返回 HTTP 429 Too Many Requests
- 返回剩余冷却秒数

##### 1.1.7 验证短信验证码
```java
POST /api/sms/verify-code
```
**功能**: 验证用户输入的验证码
**请求体**:
```json
{
  "phone": "13800138000",
  "code": "123456"
}
```
**响应**: `true` / `false`
**业务逻辑**:
1. 从Redis获取存储的验证码
2. 比对用户输入
3. 验证成功后删除Redis中的验证码

##### 1.1.8 修改密码
```java
POST /api/changePassword
```
**功能**: 修改用户密码（需验证旧密码）
**请求体**:
```json
{
  "userId": "string",
  "oldPassword": "string",
  "newPassword": "string"
}
```
**响应**:
```json
{
  "message": "密码修改成功"
}
```
**业务逻辑**:
1. 验证参数完整性
2. 调用 `loginService.changePassword()` 验证旧密码
3. 更新为新密码

---

### 模块二：用户管理模块 (User)

**包路径**: `com.example.demo.demos.User`

#### 2.1 UserController

**路径**: `/api/user`

**类说明**: 处理用户信息管理、头像上传等功能

**依赖服务**:
- `UserService` - 用户业务逻辑
- `MinioClient` - MinIO客户端
- `MinioProperties` - MinIO配置

**接口列表**:

##### 2.1.1 修改用户名
```java
POST /api/user/updateUser
```
**功能**: 根据用户ID修改用户名
**参数**:
- `userId` - 用户ID
- `userName` - 新用户名
**响应**:
```json
{
  "message": "用户名更新成功"
}
```

##### 2.1.2 更新用户信息
```java
POST /api/user/updateUserInfo
```
**功能**: 更新用户详细信息
**请求体**: `UserInfoDTO`
```json
{
  "userId": "string",
  "userName": "string",
  "email": "string",
  "phone": "string",
  "address": "string",
  "bio": "string"
}
```
**响应**:
```json
{
  "message": "用户信息更新成功"
}
```

##### 2.1.3 删除用户
```java
DELETE /api/user/deleteUser
```
**功能**: 删除用户账号（需验证密码）
**参数**:
- `userId` - 用户ID
- `password` - 密码验证
**响应**:
```json
{
  "message": "用户删除成功"
}
```

##### 2.1.4 根据用户名获取用户ID
```java
GET /api/user/getUserIdByName?userName={userName}
```
**功能**: 通过用户名查询用户ID
**响应**: `Integer` 用户ID

##### 2.1.5 根据ID获取用户信息
```java
GET /api/user/getUserById?userId={userId}
```
**功能**: 获取用户完整信息，包含头像预签名URL
**响应**: User对象
**业务逻辑**:
1. 查询用户信息
2. 如果存在 `avatarKey`，生成MinIO预签名URL（有效期1小时）
3. 将预签名URL替换到 `avatarKey` 字段返回

---

### 模块三：社区集市模块 (CommunityMarket)

**包路径**: `com.example.demo.demos.CommunityMarket`

#### 3.1 CategoryController

**路径**: `/api/categories`

**类说明**: 商品分类管理

**接口列表**:

##### 3.1.1 获取所有分类
```java
GET /api/categories/getAllCategories
```
**功能**: 获取所有商品分类列表
**响应**: `List<Category>`

##### 3.1.2 获取子分类
```java
GET /api/categories/getAllCategories/parentId/{parentId}
```
**功能**: 根据父分类ID获取所有子分类
**响应**: `List<Category>`

---

#### 3.2 CartController

**路径**: `/api/carts`

**类说明**: 购物车和订单管理

**接口列表**:

##### 3.2.1 添加到购物车
```java
POST /api/carts/add
```
**功能**: 将商品添加到购物车
**请求体**: `Cart`
```json
{
  "userName": "string",
  "productId": 0,
  "quantity": 1
}
```
**响应**: `Resp<Void>`

##### 3.2.2 获取购物车商品
```java
GET /api/carts/getCartItems?userName={userName}
```
**功能**: 获取用户购物车中的所有商品
**响应**: `List<CartShowDTO>`
**返回字段**:
- 商品信息（名称、价格、图片等）
- 购物车数量
- 卖家信息

##### 3.2.3 删除购物车商品
```java
DELETE /api/carts/deleteCartItem
```
**功能**: 从购物车删除指定商品
**参数**:
- `userName` - 用户名
- `productId` - 商品ID
**响应**: `Resp<Void>`

##### 3.2.4 购买单个商品
```java
POST /api/carts/buyCartItems
```
**功能**: 购买购物车中的单个商品，生成订单
**参数**:
- `userName` - 用户名
- `productId` - 商品ID
- `quantity` - 购买数量
**响应**: `Resp<Void>`
**业务逻辑**:
1. 验证库存
2. 扣减库存
3. 创建订单记录
4. 从购物车删除

##### 3.2.5 批量购买
```java
POST /api/carts/buySelected
```
**功能**: 批量购买购物车选中商品
**请求体**: `PurchaseRequest`
```json
{
  "userName": "string",
  "items": [
    {
      "productId": 0,
      "quantity": 1
    }
  ]
}
```
**响应**: `Resp<Void>`
**业务逻辑**:
1. 遍历所有选中商品
2. 验证每个商品库存
3. 批量扣减库存
4. 批量创建订单
5. 从购物车删除已购买商品

##### 3.2.6 获取我的订单
```java
GET /api/carts/orders?userName={userName}
```
**功能**: 获取用户的所有订单列表
**响应**: `List<Orders>`

---

#### 3.3 ProductsController

**路径**: `/api/products`

**类说明**: 商品管理核心控制器，处理商品CRUD、图片上传、附近商品推荐等

**依赖服务**:
- `ProductsService` - 商品业务逻辑
- `LoginService` - 用户认证
- `MinioClient` - 对象存储
- `MinioProperties` - MinIO配置

**接口列表**:

##### 3.3.1 获取所有商品
```java
GET /api/products/getAllProducts
```
**功能**: 获取所有商品列表（包含预签名图片URL）
**响应**: `List<Product>`

##### 3.3.2 分页获取商品
```java
GET /api/products/getProducts
```
**功能**: 分页查询商品，支持多条件筛选
**参数**: `ProductQueryDTO`
- `page` - 页码
- `size` - 每页数量
- `categoryId` - 分类ID
- `minPrice` - 最低价格
- `maxPrice` - 最高价格
- `keyword` - 关键词搜索
- `status` - 商品状态
**响应**: `PageResponse<Product>`

##### 3.3.3 根据ID获取商品
```java
GET /api/products/getProductById/{id}
```
**功能**: 获取商品详细信息
**响应**: `Product`

##### 3.3.4 获取商品和卖家信息
```java
GET /api/products/getProductAndSeller/{id}
```
**功能**: 获取商品详情及卖家信息
**响应**: `ProductAndSellerQueryDTO`
**返回字段**:
- 商品完整信息
- 卖家用户名
- 卖家联系方式
- 卖家评分

##### 3.3.5 获取商品图片
```java
GET /api/products/getProductImage/{id}
```
**功能**: 获取商品的所有图片
**响应**: `List<ProductImages>`

##### 3.3.6 上传商品图片
```java
POST /api/products/upload
Content-Type: multipart/form-data
```
**功能**: 上传商品图片到MinIO
**参数**: `file` - MultipartFile
**响应**:
```json
{
  "key": "products/uuid-filename.jpg",
  "url": "预签名URL（7天有效）",
  "publicUrl": "公开访问URL"
}
```
**业务逻辑**:
1. 验证文件不为空
2. 生成唯一对象键: `products/{UUID}-{filename}`
3. 上传到MinIO的 `community-marketplace` bucket
4. 生成7天有效期的预签名URL
5. 返回对象键和URL

##### 3.3.7 发布商品
```java
POST /api/products/addProduct
```
**功能**: 发布新商品
**请求体**: `Product`
```json
{
  "name": "string",
  "description": "string",
  "price": 0.00,
  "stock": 0,
  "categoryId": 0,
  "location": "string",
  "latitude": 0.0,
  "longitude": 0.0,
  "imageUrls": "[\"key1\", \"key2\"]",
  "sellerId": 0
}
```
**请求头**: `Authorization: Bearer token` (可选)
**响应**: `Resp<Void>`
**业务逻辑**:
1. 从请求体或Token中获取卖家ID
2. 解析图片键列表（JSON数组）
3. 验证至少有一张图片
4. 调用 `productsService.addProduct()` 保存商品

##### 3.3.8 附近商品查询
```java
GET /api/products/nearby
```
**功能**: 基于地理位置查询附近商品（使用Redis GEO）
**参数**:
- `lat` - 纬度（必填）
- `lng` - 经度（必填）
- `radiusKm` - 搜索半径（公里，默认50）
- `limit` - 返回数量限制
- `offset` - 偏移量
- `categoryId` - 分类筛选
- `minPrice` - 最低价格
- `maxPrice` - 最高价格
- `keyword` - 关键词搜索
**响应**: `List<ProductNearbyDTO>`
**返回字段**:
- 商品信息
- 距离（公里）
- 卖家信息

##### 3.3.9 获取我的商品
```java
GET /api/products/myProducts?userName={userName}
```
**功能**: 获取用户发布的所有商品
**响应**: `List<Product>`

##### 3.3.10 下架商品
```java
POST /api/products/takeDown?productId={productId}
```
**功能**: 下架指定商品
**响应**: `Resp<Void>`

##### 3.3.11 增加库存
```java
POST /api/products/increaseStock
```
**功能**: 增加商品库存数量
**参数**:
- `productId` - 商品ID
- `delta` - 增加数量
**响应**: `Resp<Void>`

##### 3.3.12 调整价格
```java
POST /api/products/updatePrice
```
**功能**: 修改商品价格
**参数**:
- `productId` - 商品ID
- `price` - 新价格
**响应**: `Resp<Void>`

##### 3.3.13 调整地址
```java
POST /api/products/updateLocation
```
**功能**: 修改商品位置信息
**参数**:
- `productId` - 商品ID
- `location` - 新地址
**响应**: `Resp<Void>`

##### 3.3.14 迁移到RediSearch
```java
POST /api/products/migrateRediSearch
```
**功能**: 批量迁移商品数据到RediSearch索引
**参数**:
- `assignRandom` - 是否为无坐标商品生成随机坐标（默认true）
**响应**:
```json
{
  "total": 100,
  "success": 98,
  "failed": 2
}
```

##### 3.3.15 同步地理坐标
```java
POST /api/products/syncGeo
```
**功能**: 同步数据库坐标到Redis GEO
**响应**:
```json
{
  "synced": 100
}
```

---

继续下一部分...
