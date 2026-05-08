-- 本地活动基础闭环迁移脚本
-- 作用：
-- 1. 将报名表 activity_id 外键统一到正式活动表 local_activity(id)
-- 2. 新增本地活动收藏表 local_activity_favorite
--
-- 执行前建议先检查是否存在无法关联到 local_activity 的报名记录：
-- SELECT e.id, e.activity_id
-- FROM local_activity_enrollment e
-- LEFT JOIN local_activity a ON a.id = e.activity_id
-- WHERE a.id IS NULL;

SET @enrollment_activity_fk := (
    SELECT CONSTRAINT_NAME
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'local_activity_enrollment'
      AND COLUMN_NAME = 'activity_id'
      AND REFERENCED_TABLE_NAME IS NOT NULL
    LIMIT 1
);

SET @drop_enrollment_fk_sql := IF(
    @enrollment_activity_fk IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE local_activity_enrollment DROP FOREIGN KEY `', @enrollment_activity_fk, '`')
);

PREPARE drop_enrollment_fk_stmt FROM @drop_enrollment_fk_sql;
EXECUTE drop_enrollment_fk_stmt;
DEALLOCATE PREPARE drop_enrollment_fk_stmt;

ALTER TABLE local_activity_enrollment
    ADD CONSTRAINT fk_enrollment_activity
    FOREIGN KEY (activity_id) REFERENCES local_activity(id)
    ON DELETE RESTRICT ON UPDATE RESTRICT;

CREATE TABLE IF NOT EXISTS local_activity_favorite (
    id BIGINT NOT NULL AUTO_INCREMENT,
    activity_id BIGINT NOT NULL,
    user_id INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uniq_local_activity_favorite_user (activity_id, user_id),
    KEY idx_local_activity_favorite_user (user_id),
    CONSTRAINT fk_local_activity_favorite_activity
        FOREIGN KEY (activity_id) REFERENCES local_activity(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_local_activity_favorite_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
