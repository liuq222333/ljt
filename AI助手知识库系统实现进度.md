# AI 客服知识库系统实现进度

## 项目概述

将现有的 AI 助手升级为智能客服系统，通过 RAG（检索增强生成）技术，使其能够回答社区服务相关的常见问题。

## 技术方案

- **架构**: 用户提问 → 向量检索知识库 → 找到相关知识 → 结合知识+工具调用 → DeepSeek生成回答
- **向量数据库**: Redis 或 Milvus
- **向量模型**: DeepSeek Embedding API 或开源模型
- **知识存储**: MySQL 存储原始知识，Redis 存储向量

## 实现进度

### ✅ Phase 1: 知识库基础功能（已完成）

#### 1. 数据库设计
- ✅ `knowledge_base.sql` - 创建了3张表：
  - `knowledge_base` - 知识库主表（分类、标题、内容、关键词等）
  - `knowledge_vector` - 知识向量表（用于后续向量检索）
  - `chat_feedback` - 对话反馈表（收集用户反馈）

- ✅ `knowledge_data.sql` - 插入示例数据：
  - FAQ: 如何发布商品、如何报名活动、如何查看附近商品
  - 使用指南: 商品发布规范、活动创建指南
  - 业务规则: 交易安全规则、社区公约

#### 2. 后端实体和 Mapper
- ✅ `KnowledgeBase.java` - 知识库实体类
- ✅ `KnowledgeVector.java` - 知识向量实体类
- ✅ `ChatFeedback.java` - 对话反馈实体类
- ✅ `KnowledgeBaseMapper.java` - MyBatis-Plus Mapper
- ✅ `KnowledgeVectorMapper.java` - MyBatis-Plus Mapper
- ✅ `ChatFeedbackMapper.java` - MyBatis-Plus Mapper

#### 3. 服务层
- ✅ `KnowledgeDTO.java` - 数据传输对象
- ✅ `KnowledgeService.java` - 知识管理服务
  - 添加知识
  - 更新知识
  - 删除知识
  - 查询知识
  - 分页列表
  - 关键词搜索

- ✅ `KnowledgeSearchService.java` - 知识检索服务
  - 简单关键词搜索（Phase 1）
  - 向量检索接口（Phase 2 待实现）

- ✅ `EmbeddingService.java` - 向量化服务（框架，待实现）

- ✅ `KnowledgeEnhancer.java` - 知识增强服务
  - 提取用户问题
  - 检索相关知识
  - 增强系统提示词

#### 4. 控制器
- ✅ `KnowledgeController.java` - 知识管理 API
  - `POST /api/knowledge/add` - 添加知识
  - `GET /api/knowledge/list` - 知识列表（支持分页和分类筛选）
  - `GET /api/knowledge/{id}` - 知识详情
  - `PUT /api/knowledge/{id}` - 更新知识
  - `DELETE /api/knowledge/{id}` - 删除知识

#### 5. AI 助手集成
- ✅ 修改 `AgentChatServiceImpl.java`
  - 注入 `KnowledgeEnhancer`
  - 在构建提示词时检索知识库
  - 将相关知识添加到系统提示词中

#### 6. 前端管理页面
- ✅ `KnowledgeManagement.vue` - 知识库管理页面
  - 知识列表展示（分页）
  - 分类筛选
  - 添加/编辑知识
  - 删除知识
  - 状态管理

---

### ⏳ Phase 2: 向量检索功能（待实现）

#### 1. 集成向量模型
- ⏳ 调用 DeepSeek Embedding API
- ⏳ 或部署开源模型（text-embedding-ada-002）

#### 2. 向量存储
- ⏳ 配置 Redis Vector Search
- ⏳ 或集成 Milvus

#### 3. 实现向量检索
- ⏳ 完善 `EmbeddingService.embed()` 方法
- ⏳ 完善 `KnowledgeSearchService.vectorSearch()` 方法
- ⏳ 知识添加时自动生成向量
- ⏳ 查询时使用向量相似度搜索

---

### ⏳ Phase 3: 优化与监控（待实现）

#### 1. 反馈机制
- ⏳ 用户对回答点赞/点踩
- ⏳ 收集无法回答的问题
- ⏳ 持续补充知识库

#### 2. 效果分析
- ⏳ 知识命中率统计
- ⏳ 回答准确率分析
- ⏳ 用户满意度监控

---

## 使用说明

### 1. 数据库初始化
```bash
# 执行 SQL 脚本
mysql -u root -p your_database < backend/demo1/src/main/resources/sql/knowledge_base.sql
mysql -u root -p your_database < backend/demo1/src/main/resources/sql/knowledge_data.sql
```

### 2. 启动后端服务
```bash
cd backend/demo1
mvn spring-boot:run
```

### 3. 访问管理后台
- 打开浏览器访问管理后台
- 进入"知识库管理"页面
- 可以添加、编辑、删除知识

### 4. 测试 AI 客服
向 AI 助手提问：
- "如何发布商品？"
- "怎么报名活动？"
- "附近有什么商品？"

AI 会先检索知识库，如果找到相关知识会直接回答，否则会调用工具查询数据。

---

## 核心文件清单

### 后端文件
```
backend/demo1/src/main/
├── resources/sql/
│   ├── knowledge_base.sql          # 数据库表结构
│   └── knowledge_data.sql          # 示例数据
├── java/com/example/demo/demos/Agent/
│   ├── Entity/
│   │   ├── KnowledgeBase.java      # 知识库实体
│   │   ├── KnowledgeVector.java    # 知识向量实体
│   │   └── ChatFeedback.java       # 对话反馈实体
│   ├── Dao/
│   │   ├── KnowledgeBaseMapper.java
│   │   ├── KnowledgeVectorMapper.java
│   │   └── ChatFeedbackMapper.java
│   ├── Pojo/
│   │   └── KnowledgeDTO.java       # 数据传输对象
│   ├── Service/
│   │   ├── KnowledgeService.java   # 知识管理服务
│   │   ├── KnowledgeSearchService.java  # 知识检索服务
│   │   ├── EmbeddingService.java   # 向量化服务
│   │   ├── KnowledgeEnhancer.java  # 知识增强服务
│   │   └── Impl/
│   │       └── AgentChatServiceImpl.java  # AI 对话服务（已修改）
│   └── Controller/
│       └── KnowledgeController.java  # 知识管理 API
```

### 前端文件
```
front-admin/src/components/
└── KnowledgeManagement.vue         # 知识库管理页面
```

---

## 技术亮点

1. **RAG 架构** - 检索增强生成，提高回答准确性
2. **知识库分类** - FAQ、指南、规则、政策等多种类型
3. **关键词搜索** - Phase 1 使用 MySQL FULLTEXT 索引
4. **向量检索** - Phase 2 支持语义相似度搜索
5. **提示词增强** - 自动将相关知识注入到 AI 上下文
6. **反馈机制** - 收集用户反馈持续优化

---

## 下一步计划

1. **实现向量检索** - 提高知识匹配准确率
2. **添加更多知识** - 补充完整的 FAQ 和使用指南
3. **优化提示词** - 调整知识注入格式
4. **添加反馈功能** - 收集用户对回答的评价
5. **效果监控** - 统计知识命中率和用户满意度

