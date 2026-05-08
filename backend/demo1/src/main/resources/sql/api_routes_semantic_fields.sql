ALTER TABLE api_routes
  ADD COLUMN intent_types JSON NULL COMMENT '可命中的意图类型，如 ["product_search"]',
  ADD COLUMN trigger_keywords JSON NULL COMMENT '触发关键词，如 ["商品","在售","有货","搜索"]',
  ADD COLUMN trigger_examples JSON NULL COMMENT '触发例句，如 ["有什么商品在售","推荐几个水果"]',
  ADD COLUMN entity_type VARCHAR(50) NULL COMMENT '返回实体类型 product/event/store',
  ADD COLUMN safety_level VARCHAR(20) NOT NULL DEFAULT 'READ' COMMENT 'READ/WRITE_SAFE/WRITE_DANGEROUS',
  ADD COLUMN require_authorization TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否要求 Authorization',
  ADD COLUMN presentation_hint VARCHAR(50) NULL COMMENT 'product_cards/activity_cards/store_cards/direct_text',
  ADD COLUMN match_priority INT NOT NULL DEFAULT 0 COMMENT '同分时优先级';
