-- 知识库表
CREATE TABLE knowledge_base (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category VARCHAR(50) COMMENT '分类：faq/guide/rule/policy',
  title VARCHAR(200) NOT NULL COMMENT '标题',
  content TEXT NOT NULL COMMENT '内容',
  keywords VARCHAR(500) COMMENT '关键词（逗号分隔）',
  related_questions TEXT COMMENT '相关问题（JSON数组）',
  view_count INT DEFAULT 0 COMMENT '查看次数',
  helpful_count INT DEFAULT 0 COMMENT '有帮助次数',
  status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_category (category),
  INDEX idx_status (status),
  FULLTEXT idx_content (title, content, keywords)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 知识向量表
CREATE TABLE knowledge_vector (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  knowledge_id BIGINT NOT NULL COMMENT '知识ID',
  vector_data TEXT NOT NULL COMMENT '向量数据（JSON）',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_knowledge (knowledge_id),
  FOREIGN KEY (knowledge_id) REFERENCES knowledge_base(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 对话反馈表
CREATE TABLE chat_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id VARCHAR(100) COMMENT '会话ID',
  user_question TEXT COMMENT '用户问题',
  ai_answer TEXT COMMENT 'AI回答',
  knowledge_ids VARCHAR(500) COMMENT '使用的知识ID',
  is_helpful TINYINT COMMENT '是否有帮助：0-否，1-是',
  feedback_text TEXT COMMENT '反馈内容',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
