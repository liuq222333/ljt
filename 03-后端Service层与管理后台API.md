# 社区服务智能触达系统 - 详细技术文档（续2）

## 后端API详细说明（续）

### 模块八：管理后台模块 (Admin)

**包路径**: `com.example.demo.demosAdmin`

#### 8.1 AdminActivityController

**路径**: `/api/admin/local-act` 或 `/api/local-act/admin`

**类说明**: 管理员活动审核管理

**依赖服务**:
- `AdminActivityService` - 活动审核服务

**接口列表**:

##### 8.1.1 获取待审核活动列表
```java
GET /api/admin/local-act/reviews
```
**功能**: 获取待审核的活动列表，支持筛选和分页
**参数**:
- `status` - 审核状态（可选）：pending/approved/rejected
- `keyword` - 关键词搜索（可选）
- `page` - 页码（默认1）
- `size` - 每页数量（默认20）
**响应**: `Resp<List<LocalActivity>>`
```json
{
  "code": 200,
  "data": [
    {
      "id": 0,
      "title": "string",
      "description": "string",
      "category": "string",
      "startTime": "2024-01-01T10:00:00",
      "location": "string",
      "organizerId": 0,
      "reviewStatus": "pending",
      "submittedAt": "2024-01-01T09:00:00"
    }
  ]
}
```

##### 8.1.2 审核通过
```java
POST /api/admin/local-act/reviews/{id}/approve
```
**功能**: 审核通过活动，发布到正式活动表
**参数**:
- `id` - 活动ID
- `note` - 审核备注（可选）
**响应**: `Resp<Long>` - 返回正式活动ID
**业务逻辑**:
1. 验证活动存在且状态为待审核
2. 将活动数据复制到正式活动表
3. 更新审核状态为已通过
4. 记录审核备注和时间
5. 发送通知给活动组织者

##### 8.1.3 审核拒绝
```java
POST /api/admin/local-act/reviews/{id}/reject
```
**功能**: 拒绝活动申请
**参数**:
- `id` - 活动ID
- `note` - 拒绝原因（可选）
**响应**: `Resp<Void>`
**业务逻辑**:
1. 更新审核状态为已拒绝
2. 记录拒绝原因
3. 发送通知给活动组织者

---

#### 8.2 AdminNotificationController

**路径**: `/api/admin/notifications`

**类说明**: 管理员通知发布

**依赖服务**:
- `AdminNotificationService` - 通知管理服务

**接口列表**:

##### 8.2.1 系统发布通知
```java
POST /api/admin/notifications/add
```
**功能**: 管理员发布系统通知公告
**请求体**: `NotificationMessage`
```json
{
  "userId": 0,
  "title": "string",
  "content": "string",
  "type": "system",
  "priority": "high",
  "targetUsers": [1, 2, 3]
}
```
**响应**: `Resp<Void>`
**业务逻辑**:
1. 验证管理员权限
2. 如果指定了targetUsers，发送给指定用户
3. 如果未指定，发送给所有用户
4. 通过RabbitMQ异步发送
5. WebSocket实时推送给在线用户

---

### Service层详细说明

#### 9.1 LoginService

**包路径**: `com.example.demo.demos.Login.Service`

**类说明**: 登录认证业务逻辑

**主要方法**:

##### 9.1.1 validateLogin
```java
boolean validateLogin(String userName, String password)
```
**功能**: 验证用户登录凭证
**逻辑**:
1. 根据用户名查询用户
2. 比对密码（应使用加密）
3. 返回验证结果

##### 9.1.2 register
```java
boolean register(String userName, String password, String phone)
```
**功能**: 用户注册
**逻辑**:
1. 检查用户名是否已存在
2. 密码加密存储
3. 创建用户记录
4. 返回是否成功

##### 9.1.3 getUserByName
```java
User getUserByName(String userName)
```
**功能**: 根据用户名获取用户信息

##### 9.1.4 getUserById
```java
User getUserById(String userId)
```
**功能**: 根据用户ID获取用户信息

##### 9.1.5 changePassword
```java
boolean changePassword(String userId, String oldPassword, String newPassword)
```
**功能**: 修改密码
**逻辑**:
1. 验证旧密码
2. 更新为新密码
3. 返回是否成功

---

#### 9.2 UserService

**包路径**: `com.example.demo.demos.User.Service`

**类说明**: 用户信息管理业务逻辑

**主要方法**:

##### 9.2.1 updateUser
```java
void updateUser(String userId, String userName)
```
**功能**: 更新用户名

##### 9.2.2 updateUserInfo
```java
void updateUserInfo(UserInfoDTO userInfoDTO)
```
**功能**: 更新用户详细信息

##### 9.2.3 deleteUser
```java
void deleteUser(String userId, String password)
```
**功能**: 删除用户（需验证密码）

##### 9.2.4 getUserIdByName
```java
Integer getUserIdByName(String userName)
```
**功能**: 根据用户名获取用户ID

##### 9.2.5 findById
```java
User findById(String userId)
```
**功能**: 根据ID查询用户

---

#### 9.3 ProductsService

**包路径**: `com.example.demo.demos.CommunityMarket.Service`

**类说明**: 商品管理业务逻辑

**主要方法**:

##### 9.3.1 getAllProducts
```java
List<Product> getAllProducts()
```
**功能**: 获取所有商品

##### 9.3.2 getProducts
```java
PageResponse<Product> getProducts(ProductQueryDTO query)
```
**功能**: 分页查询商品，支持多条件筛选

##### 9.3.3 getProductById
```java
ResponseEntity<Product> getProductById(Long id)
```
**功能**: 根据ID获取商品详情

##### 9.3.4 getProductAndSeller
```java
ResponseEntity<ProductAndSellerQueryDTO> getProductAndSeller(Integer id)
```
**功能**: 获取商品及卖家信息

##### 9.3.5 addProduct
```java
Resp<Void> addProduct(Product product)
```
**功能**: 发布新商品
**逻辑**:
1. 验证商品信息完整性
2. 验证卖家ID
3. 处理图片URL
4. 保存商品到数据库
5. 同步到Redis索引

##### 9.3.6 getNearbyProducts
```java
ResponseEntity<List<ProductNearbyDTO>> getNearbyProducts(
    double lat, double lng, Double radiusKm, Integer limit,
    Integer offset, Integer categoryId, BigDecimal minPrice,
    BigDecimal maxPrice, String keyword)
```
**功能**: 查询附近商品
**逻辑**:
1. 使用Redis GEO查询指定半径内的商品
2. 应用分类、价格、关键词筛选
3. 计算距离
4. 按距离排序返回

##### 9.3.7 getMyProducts
```java
ResponseEntity<List<Product>> getMyProducts(String userName)
```
**功能**: 获取用户发布的商品

##### 9.3.8 takeDownProduct
```java
Resp<Void> takeDownProduct(Long productId)
```
**功能**: 下架商品

##### 9.3.9 increaseStock
```java
Resp<Void> increaseStock(Long productId, Integer delta)
```
**功能**: 增加库存

##### 9.3.10 updatePrice
```java
Resp<Void> updatePrice(Long productId, BigDecimal price)
```
**功能**: 更新价格

##### 9.3.11 updateLocation
```java
Resp<Void> updateLocation(Long productId, String location)
```
**功能**: 更新位置

---

#### 9.4 CartService

**包路径**: `com.example.demo.demos.CommunityMarket.Service`

**类说明**: 购物车和订单业务逻辑

**主要方法**:

##### 9.4.1 addToCart
```java
boolean addToCart(Cart cart)
```
**功能**: 添加商品到购物车
**逻辑**:
1. 检查商品是否已在购物车
2. 如果存在，更新数量
3. 如果不存在，新增记录

##### 9.4.2 getCartItems
```java
ResponseEntity<List<CartShowDTO>> getCartItems(String userName)
```
**功能**: 获取购物车商品列表
**逻辑**:
1. 查询用户购物车
2. 关联商品信息
3. 关联卖家信息
4. 返回完整DTO

##### 9.4.3 deleteCartItem
```java
Resp<Void> deleteCartItem(String userName, int productId)
```
**功能**: 删除购物车商品

##### 9.4.4 buyCartItems
```java
Resp<Void> buyCartItems(String userName, int productId, int quantity)
```
**功能**: 购买单个商品
**逻辑**:
1. 验证库存充足
2. 扣减库存
3. 创建订单
4. 从购物车删除

##### 9.4.5 buySelected
```java
Resp<Void> buySelected(PurchaseRequest request)
```
**功能**: 批量购买
**逻辑**:
1. 遍历所有商品
2. 验证库存
3. 批量扣减库存（事务）
4. 批量创建订单
5. 批量删除购物车

##### 9.4.6 getOrders
```java
ResponseEntity<List<Orders>> getOrders(String userName)
```
**功能**: 获取用户订单列表

---

#### 9.5 LocalActEnrollmentService

**包路径**: `com.example.demo.demos.LocalActive.Service`

**类说明**: 活动报名业务逻辑

**主要方法**:

##### 9.5.1 getUserEnrollments
```java
LocalActEnrollmentListResponse getUserEnrollments(LocalActEnrollmentQuery query)
```
**功能**: 获取用户报名记录
**逻辑**:
1. 根据条件查询报名记录
2. 关联活动信息
3. 统计各状态数量
4. 返回列表和统计信息

---

#### 9.6 LocalActivityService

**包路径**: `com.example.demo.demos.LocalActive.Service`

**类说明**: 活动管理业务逻辑

**主要方法**:

##### 9.6.1 createActivity
```java
LocalActCreateResponse createActivity(LocalActCreateRequest request)
```
**功能**: 创建活动
**逻辑**:
1. 验证活动信息
2. 保存到待审核表
3. 发送通知给管理员
4. 返回活动ID和状态

##### 9.6.2 listActivities
```java
List<LocalActivity> listActivities(String status, int page, int size)
```
**功能**: 分页查询活动列表

---

#### 9.7 LocalActivitySearchService

**包路径**: `com.example.demo.demos.LocalActive.Service`

**类说明**: 活动搜索业务逻辑（RediSearch）

**主要方法**:

##### 9.7.1 searchNearby
```java
List<NearbyActivityDTO> searchNearby(
    double lat, double lon, double radiusKm,
    String category, String keyword, int size)
```
**功能**: 附近活动搜索
**逻辑**:
1. 使用RediSearch地理位置查询
2. 应用分类和关键词过滤
3. 计算距离
4. 按距离排序

##### 9.7.2 syncFromDb
```java
Integer syncFromDb()
```
**功能**: 同步数据库活动到Redis索引
**逻辑**:
1. 查询所有活动
2. 批量写入Redis索引
3. 返回同步数量

---

#### 9.8 NeighborSupportService

**包路径**: `com.example.demo.demos.LocalActive.Service`

**类说明**: 邻里互助业务逻辑

**主要方法**:

##### 9.8.1 createTask
```java
NeighborSupportTaskResponse createTask(NeighborSupportTaskRequest request)
```
**功能**: 创建互助任务

##### 9.8.2 listTasks
```java
List<NeighborSupportTaskDTO> listTasks(String status)
```
**功能**: 查询任务列表

---

#### 9.9 CommunityFeedService

**包路径**: `com.example.demo.demos.CommunityFeed.Service`

**类说明**: 社区动态业务逻辑

**主要方法**:

##### 9.9.1 createPost
```java
CommunityFeed createPost(CommunityFeedPostRequest request)
```
**功能**: 发布动态
**逻辑**:
1. 验证用户和内容
2. 处理图片URL
3. 保存动态
4. 返回动态对象

##### 9.9.2 listFeeds
```java
List<CommunityFeed> listFeeds(String visibility, int page, int size)
```
**功能**: 分页查询动态列表

##### 9.9.3 getFeed
```java
CommunityFeed getFeed(Long id)
```
**功能**: 获取动态详情

##### 9.9.4 like
```java
void like(Long id, Long userId)
```
**功能**: 点赞
**逻辑**:
1. 检查是否已点赞
2. 添加点赞记录
3. 增加点赞计数

##### 9.9.5 unlike
```java
void unlike(Long id, Long userId)
```
**功能**: 取消点赞

##### 9.9.6 addComment
```java
CommunityFeedComment addComment(Long id, Long userId, String content)
```
**功能**: 添加评论
**逻辑**:
1. 验证动态存在
2. 保存评论
3. 增加评论计数

##### 9.9.7 listComments
```java
List<CommunityFeedComment> listComments(Long id, int page, int size)
```
**功能**: 分页查询评论

##### 9.9.8 deleteComment
```java
boolean deleteComment(Long postId, Long commentId, Long userId)
```
**功能**: 删除评论（权限验证）

##### 9.9.9 deleteFeed
```java
boolean deleteFeed(Long id, Long userId)
```
**功能**: 删除动态（权限验证）

##### 9.9.10 uploadAvatar
```java
Map<String, String> uploadAvatar(String userId, MultipartFile file)
```
**功能**: 上传头像
**逻辑**:
1. 上传到MinIO
2. 更新用户表avatar_key
3. 返回对象键和URL

---

继续下一部分...
