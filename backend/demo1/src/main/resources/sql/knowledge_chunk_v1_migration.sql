CREATE TABLE IF NOT EXISTS knowledge_chunk (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  knowledge_id BIGINT NOT NULL COMMENT '知识 ID',
  chunk_no INT NOT NULL COMMENT 'chunk 序号',
  chunk_type VARCHAR(32) NOT NULL COMMENT 'chunk 类型',
  chunk_text TEXT NOT NULL COMMENT 'chunk 文本',
  metadata_json TEXT COMMENT 'chunk 元数据 JSON',
  status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_knowledge_chunk (knowledge_id, chunk_no),
  INDEX idx_chunk_status (status),
  FULLTEXT idx_chunk_text (chunk_text),
  FOREIGN KEY (knowledge_id) REFERENCES knowledge_base(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
