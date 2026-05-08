-- 本地活动审核恢复脚本
-- 审核链路统一使用 local_activity.status：
-- DRAFT 草稿，REVIEWING 待审核，PUBLISHED 已发布，CANCELLED 已取消/驳回。
-- local_activity_admin 保留历史数据，不再作为新活动审核主表。

ALTER TABLE local_activity
    MODIFY COLUMN status ENUM('DRAFT','REVIEWING','PENDING_REVIEW','PUBLISHED','CANCELLED')
    DEFAULT 'DRAFT'
    COMMENT '状态';

UPDATE local_activity
SET status = 'REVIEWING'
WHERE status = 'PENDING_REVIEW';

ALTER TABLE local_activity
    MODIFY COLUMN status ENUM('DRAFT','REVIEWING','PUBLISHED','CANCELLED')
    DEFAULT 'DRAFT'
    COMMENT '状态';

SET @review_status_idx_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'local_activity'
      AND INDEX_NAME = 'idx_local_activity_review_status'
);

SET @create_review_status_idx_sql := IF(
    @review_status_idx_exists > 0,
    'SELECT 1',
    'CREATE INDEX idx_local_activity_review_status ON local_activity(status, created_at)'
);

PREPARE create_review_status_idx_stmt FROM @create_review_status_idx_sql;
EXECUTE create_review_status_idx_stmt;
DEALLOCATE PREPARE create_review_status_idx_stmt;
