-- =============================================
-- 审计日志表 DDL
-- 与施工单 W01 审计日志 + 总设计文档 §15.4 对齐
-- 保留期限：普通日志 >= 90 天，高风险场景 >= 180 天
-- =============================================

CREATE TABLE IF NOT EXISTS audit_log (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    request_id      VARCHAR(64)     NOT NULL COMMENT '请求 ID',
    trace_id        VARCHAR(64)     NOT NULL COMMENT '追踪 ID',
    user_id         VARCHAR(64)     DEFAULT NULL COMMENT '用户 ID',
    timestamp       DATETIME        NOT NULL COMMENT '请求时间',
    raw_input       TEXT            DEFAULT NULL COMMENT '用户原始输入',
    parsed_intent   TEXT            DEFAULT NULL COMMENT '意图识别结果 (JSON)',
    tool_calls      TEXT            DEFAULT NULL COMMENT '工具调用记录 (JSON)',
    final_answer    TEXT            DEFAULT NULL COMMENT '最终回答摘要',
    is_degraded     TINYINT(1)      DEFAULT 0 COMMENT '是否发生降级 (0=否, 1=是)',
    risk_level      VARCHAR(16)     DEFAULT 'normal' COMMENT '风险等级 (normal/high)',
    duration_ms     BIGINT          DEFAULT NULL COMMENT '端到端耗时 (毫秒)',
    tokens_used     INT             DEFAULT NULL COMMENT '消耗的 token 数',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_request_id (request_id),
    INDEX idx_user_id_timestamp (user_id, timestamp),
    INDEX idx_risk_level (risk_level),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='审计日志表 — 记录每次请求的完整轨迹';
