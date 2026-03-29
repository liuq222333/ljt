CREATE TABLE knowledge_base (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category VARCHAR(50) COMMENT '兼容旧分类字段，如 faq/guide/rule/policy',
  doc_type VARCHAR(64) NOT NULL DEFAULT 'faq' COMMENT '正式文档类型，如 faq/refund_rule/reservation_rule',
  title VARCHAR(200) NOT NULL COMMENT '标题',
  content TEXT NOT NULL COMMENT '正文内容',
  summary VARCHAR(500) COMMENT '摘要',
  keywords VARCHAR(500) COMMENT '关键词，逗号分隔',
  related_questions TEXT COMMENT '相关问题，JSON 数组或换行文本',
  source_system VARCHAR(64) DEFAULT 'cms' COMMENT '来源系统',
  entity_type VARCHAR(32) COMMENT '关联实体类型，如 product/event/store',
  entity_id VARCHAR(64) COMMENT '关联实体 ID',
  city_ids VARCHAR(255) COMMENT '关联城市 ID，逗号分隔',
  category_ids VARCHAR(255) COMMENT '关联类目 ID，逗号分隔',
  tag_ids VARCHAR(255) COMMENT '关联标签 ID，逗号分隔',
  version VARCHAR(32) DEFAULT 'v1' COMMENT '版本号',
  priority INT DEFAULT 0 COMMENT '优先级，值越大越优先',
  owner VARCHAR(64) COMMENT '责任人或责任团队',
  language VARCHAR(16) DEFAULT 'zh-CN' COMMENT '语言',
  effective_from TIMESTAMP NULL COMMENT '生效开始时间',
  effective_to TIMESTAMP NULL COMMENT '生效结束时间',
  published_at TIMESTAMP NULL COMMENT '发布时间',
  view_count INT DEFAULT 0 COMMENT '查看次数',
  helpful_count INT DEFAULT 0 COMMENT '有帮助次数',
  status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_category (category),
  INDEX idx_doc_type (doc_type),
  INDEX idx_entity (entity_type, entity_id),
  INDEX idx_status (status),
  INDEX idx_effective (effective_from, effective_to),
  FULLTEXT idx_content (title, content, keywords)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE knowledge_vector (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  knowledge_id BIGINT NOT NULL COMMENT '知识 ID',
  vector_data TEXT NOT NULL COMMENT '向量数据，JSON 格式',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_knowledge (knowledge_id),
  FOREIGN KEY (knowledge_id) REFERENCES knowledge_base(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE chat_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id VARCHAR(100) COMMENT '会话 ID',
  user_question TEXT COMMENT '用户问题',
  ai_answer TEXT COMMENT 'AI 回答',
  knowledge_ids VARCHAR(500) COMMENT '使用的知识 ID 列表',
  is_helpful TINYINT COMMENT '是否有帮助：0-否，1-是',
  feedback_text TEXT COMMENT '反馈内容',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
