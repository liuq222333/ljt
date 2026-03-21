-- =============================================
-- 结构化快照层 DDL
-- 与施工单 W02 + 结构化层sql表设计文档 对齐
-- 包含：category / tag / query_expand_dict
--       product_search_snapshot / event_search_snapshot / store_search_snapshot
-- =============================================

-- =========================================================
-- 1) 类目表
-- =========================================================
CREATE TABLE IF NOT EXISTS `category` (
  `category_id`           BIGINT NOT NULL COMMENT '类目ID',
  `parent_category_id`    BIGINT NOT NULL DEFAULT 0 COMMENT '父类目ID，0表示根节点',
  `category_name`         VARCHAR(128) NOT NULL COMMENT '类目名称',
  `category_level`        TINYINT NOT NULL COMMENT '类目层级，从1开始',
  `category_path`         VARCHAR(1024) NOT NULL COMMENT '类目路径，如 1/12/123',
  `is_leaf`               TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否叶子节点：0否 1是',
  `sort_order`            INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  `status`                VARCHAR(32) NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled/disabled',
  `created_at`            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (`category_id`),
  KEY `idx_parent_category_id` (`parent_category_id`),
  KEY `idx_category_level` (`category_level`),
  KEY `idx_status_sort_order` (`status`, `sort_order`),
  KEY `idx_category_path` (`category_path`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='类目树表';


-- =========================================================
-- 2) 标签表
-- =========================================================
CREATE TABLE IF NOT EXISTS `tag` (
  `tag_id`                BIGINT NOT NULL COMMENT '标签ID',
  `tag_name`              VARCHAR(128) NOT NULL COMMENT '标签名称',
  `tag_type`              VARCHAR(64) NOT NULL DEFAULT 'general' COMMENT '标签类型，如 general/audience/scene/feature',
  `status`                VARCHAR(32) NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled/disabled',
  `created_at`            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (`tag_id`),
  UNIQUE KEY `uk_tag_type_name` (`tag_type`, `tag_name`),
  KEY `idx_tag_name` (`tag_name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';


-- =========================================================
-- 3) 查询扩展词典表
-- =========================================================
CREATE TABLE IF NOT EXISTS `query_expand_dict` (
  `id`                    BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `query_term`            VARCHAR(128) NOT NULL COMMENT '原始查询词',
  `expand_terms_json`     JSON DEFAULT NULL COMMENT '扩展词列表JSON，如 ["手办","模型","公仔"]',
  `expand_categories_json` JSON DEFAULT NULL COMMENT '扩展类目JSON，如 [1001,1002]',
  `expand_tags_json`      JSON DEFAULT NULL COMMENT '扩展标签JSON，如 [2001,2002]',
  `status`                VARCHAR(32) NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled/disabled',
  `updated_at`            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_at`            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_query_term` (`query_term`),
  KEY `idx_status_updated_at` (`status`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='高频查询扩展词典表';


-- =========================================================
-- 4) 商品搜索快照表
-- =========================================================
CREATE TABLE IF NOT EXISTS `product_search_snapshot` (
  `id`                    BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `product_id`            BIGINT NOT NULL COMMENT '商品ID',
  `seller_id`             BIGINT NOT NULL COMMENT '商家/卖家ID',
  `store_id`              BIGINT DEFAULT NULL COMMENT '门店ID',
  `store_name`            VARCHAR(256) DEFAULT NULL COMMENT '门店名称',
  `product_type`          VARCHAR(64) DEFAULT NULL COMMENT '商品类型',
  `title`                 VARCHAR(256) NOT NULL COMMENT '商品标题',
  `subtitle`              VARCHAR(512) DEFAULT NULL COMMENT '商品副标题',
  `summary_text`          VARCHAR(2000) DEFAULT NULL COMMENT '商品摘要，用于描述匹配',
  `category_id`           BIGINT NOT NULL COMMENT '主类目ID',
  `category_name`         VARCHAR(128) NOT NULL COMMENT '主类目名称',
  `category_path`         VARCHAR(1024) NOT NULL COMMENT '类目路径',
  `tag_ids`               JSON DEFAULT NULL COMMENT '标签ID数组JSON',
  `tag_names`             JSON DEFAULT NULL COMMENT '标签名称数组JSON',
  `city_id`               BIGINT DEFAULT NULL COMMENT '城市ID',
  `city_name`             VARCHAR(64) DEFAULT NULL COMMENT '城市名称',
  `district_id`           BIGINT DEFAULT NULL COMMENT '区县ID',
  `district_name`         VARCHAR(64) DEFAULT NULL COMMENT '区县名称',
  `business_area_id`      BIGINT DEFAULT NULL COMMENT '商圈ID',
  `business_area_name`    VARCHAR(128) DEFAULT NULL COMMENT '商圈名称',
  `lat`                   DECIMAL(10,7) DEFAULT NULL COMMENT '纬度',
  `lng`                   DECIMAL(10,7) DEFAULT NULL COMMENT '经度',
  `base_price`            DECIMAL(12,2) DEFAULT NULL COMMENT '基础价格',
  `display_price`         DECIMAL(12,2) DEFAULT NULL COMMENT '展示价格',
  `currency`              VARCHAR(16) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  `cover_image`           VARCHAR(1024) DEFAULT NULL COMMENT '封面图URL',
  `sales_count`           BIGINT NOT NULL DEFAULT 0 COMMENT '销量',
  `rating`                DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '评分，如 4.70',
  `review_count`          BIGINT NOT NULL DEFAULT 0 COMMENT '评价数',
  `hot_score`             DECIMAL(14,4) NOT NULL DEFAULT 0.0000 COMMENT '热度分',
  `recommend_score`       DECIMAL(14,4) NOT NULL DEFAULT 0.0000 COMMENT '推荐综合分',
  `searchable_status`     VARCHAR(32) NOT NULL DEFAULT 'disabled' COMMENT '搜索状态：searchable/not_searchable/expired/draft/disabled',
  `publish_status`        VARCHAR(32) NOT NULL DEFAULT 'off_shelf' COMMENT '上架状态：on_shelf/off_shelf',
  `audit_status`          VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '审核状态：pending/approved/rejected',
  `visible_status`        VARCHAR(32) NOT NULL DEFAULT 'hidden' COMMENT '可见状态：visible/hidden',
  `source_status`         VARCHAR(32) DEFAULT NULL COMMENT '源系统状态',
  `created_at`            DATETIME NOT NULL COMMENT '源对象创建时间',
  `updated_at`            DATETIME NOT NULL COMMENT '源对象更新时间',
  `searchable_updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照可搜索更新时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_id` (`product_id`),

  KEY `idx_category_id` (`category_id`),
  KEY `idx_city_district_biz` (`city_id`, `district_id`, `business_area_id`),
  KEY `idx_display_price` (`display_price`),
  KEY `idx_sales_count` (`sales_count`),
  KEY `idx_rating` (`rating`),
  KEY `idx_hot_score` (`hot_score`),
  KEY `idx_recommend_score` (`recommend_score`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_updated_at` (`updated_at`),
  KEY `idx_searchable_updated_at` (`searchable_updated_at`),

  KEY `idx_search_filter_status` (`searchable_status`, `publish_status`, `audit_status`, `visible_status`),
  KEY `idx_search_filter_city_category` (`searchable_status`, `publish_status`, `audit_status`, `visible_status`, `city_id`, `category_id`),

  KEY `idx_store_id` (`store_id`),
  KEY `idx_seller_id` (`seller_id`),
  KEY `idx_lat_lng` (`lat`, `lng`),
  KEY `idx_category_path` (`category_path`(255)),
  KEY `idx_title` (`title`),
  KEY `idx_store_name` (`store_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品搜索快照表';


-- =========================================================
-- 5) 活动搜索快照表
-- =========================================================
CREATE TABLE IF NOT EXISTS `event_search_snapshot` (
  `id`                    BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `event_id`              BIGINT NOT NULL COMMENT '活动ID',
  `organizer_id`          BIGINT NOT NULL COMMENT '主办方ID',
  `venue_id`              BIGINT DEFAULT NULL COMMENT '场馆/门店ID',
  `venue_name`            VARCHAR(256) DEFAULT NULL COMMENT '场馆/门店名称',
  `title`                 VARCHAR(256) NOT NULL COMMENT '活动标题',
  `subtitle`              VARCHAR(512) DEFAULT NULL COMMENT '活动副标题',
  `summary_text`          VARCHAR(2000) DEFAULT NULL COMMENT '活动摘要',
  `category_id`           BIGINT NOT NULL COMMENT '主类目ID',
  `category_name`         VARCHAR(128) NOT NULL COMMENT '主类目名称',
  `category_path`         VARCHAR(1024) NOT NULL COMMENT '类目路径',
  `tag_ids`               JSON DEFAULT NULL COMMENT '标签ID数组JSON',
  `tag_names`             JSON DEFAULT NULL COMMENT '标签名称数组JSON',
  `audience_tags`         JSON DEFAULT NULL COMMENT '适合人群标签JSON',
  `city_id`               BIGINT DEFAULT NULL COMMENT '城市ID',
  `city_name`             VARCHAR(64) DEFAULT NULL COMMENT '城市名称',
  `district_id`           BIGINT DEFAULT NULL COMMENT '区县ID',
  `district_name`         VARCHAR(64) DEFAULT NULL COMMENT '区县名称',
  `lat`                   DECIMAL(10,7) DEFAULT NULL COMMENT '纬度',
  `lng`                   DECIMAL(10,7) DEFAULT NULL COMMENT '经度',
  `min_price`             DECIMAL(12,2) DEFAULT NULL COMMENT '最低价格',
  `max_price`             DECIMAL(12,2) DEFAULT NULL COMMENT '最高价格',
  `event_start_time`      DATETIME DEFAULT NULL COMMENT '活动开始时间',
  `event_end_time`        DATETIME DEFAULT NULL COMMENT '活动结束时间',
  `weekday_mask`          VARCHAR(32) DEFAULT NULL COMMENT '星期掩码，如 1,2,3,4,5',
  `searchable_status`     VARCHAR(32) NOT NULL DEFAULT 'disabled' COMMENT '搜索状态：searchable/not_searchable/expired/draft/disabled',
  `publish_status`        VARCHAR(32) NOT NULL DEFAULT 'off_shelf' COMMENT '上架状态：on_shelf/off_shelf',
  `audit_status`          VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '审核状态：pending/approved/rejected',
  `visible_status`        VARCHAR(32) NOT NULL DEFAULT 'hidden' COMMENT '可见状态：visible/hidden',
  `sales_count`           BIGINT NOT NULL DEFAULT 0 COMMENT '销量/报名量',
  `rating`                DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '评分',
  `hot_score`             DECIMAL(14,4) NOT NULL DEFAULT 0.0000 COMMENT '热度分',
  `recommend_score`       DECIMAL(14,4) NOT NULL DEFAULT 0.0000 COMMENT '推荐综合分',
  `created_at`            DATETIME NOT NULL COMMENT '源对象创建时间',
  `updated_at`            DATETIME NOT NULL COMMENT '源对象更新时间',
  `searchable_updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照可搜索更新时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_id` (`event_id`),

  KEY `idx_category_id` (`category_id`),
  KEY `idx_city_district` (`city_id`, `district_id`),
  KEY `idx_min_price` (`min_price`),
  KEY `idx_max_price` (`max_price`),
  KEY `idx_event_start_time` (`event_start_time`),
  KEY `idx_event_end_time` (`event_end_time`),
  KEY `idx_sales_count` (`sales_count`),
  KEY `idx_rating` (`rating`),
  KEY `idx_hot_score` (`hot_score`),
  KEY `idx_recommend_score` (`recommend_score`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_updated_at` (`updated_at`),
  KEY `idx_searchable_updated_at` (`searchable_updated_at`),

  KEY `idx_search_filter_status` (`searchable_status`, `publish_status`, `audit_status`, `visible_status`),
  KEY `idx_search_filter_city_category_time` (`searchable_status`, `publish_status`, `audit_status`, `visible_status`, `city_id`, `category_id`, `event_start_time`),

  KEY `idx_venue_id` (`venue_id`),
  KEY `idx_organizer_id` (`organizer_id`),
  KEY `idx_lat_lng` (`lat`, `lng`),
  KEY `idx_category_path` (`category_path`(255)),
  KEY `idx_title` (`title`),
  KEY `idx_venue_name` (`venue_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动搜索快照表';


-- =========================================================
-- 6) 门店搜索快照表
-- =========================================================
CREATE TABLE IF NOT EXISTS `store_search_snapshot` (
  `id`                    BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `store_id`              BIGINT NOT NULL COMMENT '门店ID',
  `merchant_id`           BIGINT NOT NULL COMMENT '商户ID',
  `store_name`            VARCHAR(256) NOT NULL COMMENT '门店名称',
  `title`                 VARCHAR(256) DEFAULT NULL COMMENT '门店标题',
  `summary_text`          VARCHAR(2000) DEFAULT NULL COMMENT '门店摘要',
  `category_id`           BIGINT DEFAULT NULL COMMENT '主类目ID',
  `category_name`         VARCHAR(128) DEFAULT NULL COMMENT '主类目名称',
  `category_path`         VARCHAR(1024) DEFAULT NULL COMMENT '类目路径',
  `tag_names`             JSON DEFAULT NULL COMMENT '标签名称数组JSON',
  `city_id`               BIGINT DEFAULT NULL COMMENT '城市ID',
  `district_id`           BIGINT DEFAULT NULL COMMENT '区县ID',
  `business_area_id`      BIGINT DEFAULT NULL COMMENT '商圈ID',
  `lat`                   DECIMAL(10,7) DEFAULT NULL COMMENT '纬度',
  `lng`                   DECIMAL(10,7) DEFAULT NULL COMMENT '经度',
  `avg_price`             DECIMAL(12,2) DEFAULT NULL COMMENT '人均/均价',
  `rating`                DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '评分',
  `review_count`          BIGINT NOT NULL DEFAULT 0 COMMENT '评价数',
  `hot_score`             DECIMAL(14,4) NOT NULL DEFAULT 0.0000 COMMENT '热度分',
  `searchable_status`     VARCHAR(32) NOT NULL DEFAULT 'disabled' COMMENT '搜索状态：searchable/not_searchable/expired/draft/disabled',
  `publish_status`        VARCHAR(32) NOT NULL DEFAULT 'off_shelf' COMMENT '发布状态：on_shelf/off_shelf',
  `visible_status`        VARCHAR(32) NOT NULL DEFAULT 'hidden' COMMENT '可见状态：visible/hidden',
  `base_open_time_desc`   VARCHAR(512) DEFAULT NULL COMMENT '基础营业时间说明',
  `created_at`            DATETIME NOT NULL COMMENT '源对象创建时间',
  `updated_at`            DATETIME NOT NULL COMMENT '源对象更新时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_id` (`store_id`),

  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_city_district_biz` (`city_id`, `district_id`, `business_area_id`),
  KEY `idx_avg_price` (`avg_price`),
  KEY `idx_rating` (`rating`),
  KEY `idx_review_count` (`review_count`),
  KEY `idx_hot_score` (`hot_score`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_updated_at` (`updated_at`),

  KEY `idx_search_filter_status` (`searchable_status`, `publish_status`, `visible_status`),
  KEY `idx_search_filter_city_category` (`searchable_status`, `publish_status`, `visible_status`, `city_id`, `category_id`),

  KEY `idx_lat_lng` (`lat`, `lng`),
  KEY `idx_category_path` (`category_path`(255)),
  KEY `idx_store_name` (`store_name`),
  KEY `idx_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店搜索快照表';
