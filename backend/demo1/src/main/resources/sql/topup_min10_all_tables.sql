SET @target_rows := 10;

DROP TEMPORARY TABLE IF EXISTS tmp_seq10;
CREATE TEMPORARY TABLE tmp_seq10 (
    n INT PRIMARY KEY
);
INSERT INTO tmp_seq10 (n) VALUES
    (1),(2),(3),(4),(5),(6),(7),(8),(9),(10),
    (11),(12),(13),(14),(15),(16),(17),(18),(19),(20);

SET @seed_ts := DATE_FORMAT(NOW(), '%Y%m%d%H%i%s');

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM agent_tools));
INSERT INTO agent_tools (
    name, display_name, description, params_schema, version, enabled, tags, roles_allowed, rate_limit, created_at, updated_at
)
SELECT
    CONCAT('seed_tool_', @seed_ts, '_', LPAD(n, 2, '0')),
    CONCAT('Seed Tool ', n),
    CONCAT('Auto generated seed tool #', n),
    JSON_OBJECT('type', 'object', 'properties', JSON_OBJECT('q', JSON_OBJECT('type', 'string'))),
    '1.0.0',
    1,
    JSON_ARRAY('seed', 'auto'),
    JSON_ARRAY('admin', 'user'),
    '60/m',
    NOW(),
    NOW()
FROM tmp_seq10
WHERE n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM audit_log));
SET @base := COALESCE((SELECT COUNT(*) FROM audit_log), 0);
INSERT INTO audit_log (
    request_id, trace_id, user_id, timestamp, raw_input, parsed_intent, tool_calls, final_answer, is_degraded, risk_level, duration_ms, tokens_used, created_at
)
SELECT
    CONCAT('req_seed_', @seed_ts, '_', LPAD(n, 2, '0')),
    CONCAT('trace_seed_', @seed_ts, '_', LPAD(n, 2, '0')),
    CAST(((n - 1) % (SELECT COUNT(*) FROM users) + 1) AS CHAR),
    NOW(),
    CONCAT('seed raw input ', n),
    '{"taskType":"faq_query"}',
    '[{"tool":"backend_api"}]',
    CONCAT('seed answer ', n),
    0,
    'normal',
    100 + n,
    200 + n,
    NOW()
FROM tmp_seq10
WHERE n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM cart_item));
INSERT INTO cart_item (user_name, product_id, quantity, price, created_at)
SELECT
    u.username,
    p.id,
    (n % 3) + 1,
    p.price,
    NOW()
FROM (
    SELECT n, ((n - 1) % (SELECT COUNT(*) FROM users) + 1) AS rn_user, ((n - 1) % (SELECT COUNT(*) FROM products) + 1) AS rn_product
    FROM tmp_seq10
    WHERE n <= @need
) s
JOIN (
    SELECT user_id, username, ROW_NUMBER() OVER (ORDER BY user_id) AS rn
    FROM users
) u ON u.rn = s.rn_user
JOIN (
    SELECT id, price, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM products
) p ON p.rn = s.rn_product;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM category));
SET @base := COALESCE((SELECT MAX(category_id) FROM category), 0);
INSERT INTO category (
    category_id, parent_category_id, category_name, category_level, category_path, is_leaf, sort_order, status, created_at, updated_at
)
SELECT
    @base + n,
    0,
    CONCAT('SeedCategory', @base + n),
    1,
    CONCAT('/', @base + n),
    1,
    n,
    'enabled',
    NOW(),
    NOW()
FROM tmp_seq10
WHERE n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM tag));
SET @base := COALESCE((SELECT MAX(tag_id) FROM tag), 0);
INSERT INTO tag (
    tag_id, tag_name, tag_type, status, created_at, updated_at
)
SELECT
    @base + n,
    CONCAT('seed_tag_', @base + n),
    'general',
    'enabled',
    NOW(),
    NOW()
FROM tmp_seq10
WHERE n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM topic));
INSERT INTO topic (
    name, slug, description, cover_key, status, created_at, updated_at, feeds_count
)
SELECT
    CONCAT('Seed Topic ', @seed_ts, '-', n),
    CONCAT('seed-topic-', @seed_ts, '-', n),
    CONCAT('Auto seed topic #', n),
    CONCAT('topic/cover/', @seed_ts, '/', n, '.png'),
    'ACTIVE',
    NOW(),
    NOW(),
    0
FROM tmp_seq10
WHERE n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM chat_feedback));
INSERT INTO chat_feedback (
    session_id, user_question, ai_answer, knowledge_ids, is_helpful, feedback_text, created_at
)
SELECT
    CONCAT('seed_session_', @seed_ts, '_', n),
    CONCAT('seed question ', n),
    CONCAT('seed answer ', n),
    '1,2',
    IF(n % 2 = 0, 1, 0),
    CONCAT('seed feedback ', n),
    NOW()
FROM tmp_seq10
WHERE n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM governance_admin_states));
INSERT INTO governance_admin_states (
    store_key, payload_json, created_at, updated_at
)
SELECT
    CONCAT('seed_store_', @seed_ts, '_', n),
    JSON_OBJECT('source', 'seed', 'index', n),
    NOW(),
    NOW()
FROM tmp_seq10
WHERE n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM governance_eval_cases));
INSERT INTO governance_eval_cases (
    case_name, bucket, query_text, expected_task_type, expected_plan_type, expected_answer_type, risk_level, tags_json, enabled, notes, created_at, updated_at
)
SELECT
    CONCAT('seed_case_', @seed_ts, '_', n),
    IF(n % 2 = 0, 'search', 'faq'),
    CONCAT('seed eval query ', n),
    IF(n % 2 = 0, 'product_search', 'faq_query'),
    IF(n % 2 = 0, 'search_only', 'knowledge_only'),
    IF(n % 2 = 0, 'recommendation', 'explanation'),
    'normal',
    JSON_ARRAY('seed', 'auto'),
    1,
    CONCAT('seed case note ', n),
    NOW(),
    NOW()
FROM tmp_seq10
WHERE n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM governance_eval_case_versions));
INSERT INTO governance_eval_case_versions (
    version_name, bucket, source_filter_json, total_cases, enabled_total, notes, created_by, created_at, updated_at
)
SELECT
    CONCAT('seed_version_', @seed_ts, '_', n),
    IF(n % 2 = 0, 'search', 'faq'),
    JSON_OBJECT('source', 'seed', 'idx', n),
    10,
    10,
    CONCAT('seed version note ', n),
    'seed',
    NOW(),
    NOW()
FROM tmp_seq10
WHERE n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM governance_eval_case_version_items));
INSERT INTO governance_eval_case_version_items (
    version_id, case_id, case_name, bucket, query_text, expected_task_type, expected_plan_type, expected_answer_type,
    risk_level, tags_json, enabled, notes, created_at
)
WITH v AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id DESC) AS rn
    FROM governance_eval_case_versions
    LIMIT 10
),
c AS (
    SELECT id, case_name, bucket, query_text, expected_task_type, expected_plan_type, expected_answer_type, risk_level, tags_json,
           ROW_NUMBER() OVER (ORDER BY id DESC) AS rn
    FROM governance_eval_cases
    LIMIT 10
)
SELECT
    v.id,
    c.id,
    c.case_name,
    c.bucket,
    c.query_text,
    c.expected_task_type,
    c.expected_plan_type,
    c.expected_answer_type,
    c.risk_level,
    c.tags_json,
    1,
    'seed version item',
    NOW()
FROM tmp_seq10 s
JOIN v ON v.rn = s.n
JOIN c ON c.rn = s.n
WHERE s.n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM governance_eval_runs));
INSERT INTO governance_eval_runs (
    bucket, source_type, version_id, total, passed_total, failed_total, pass_rate, created_at
)
SELECT
    IF(s.n % 2 = 0, 'search', 'faq'),
    'seed',
    v.id,
    10,
    8,
    2,
    0.8,
    NOW()
FROM tmp_seq10 s
JOIN (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id DESC) AS rn
    FROM governance_eval_case_versions
    LIMIT 10
) v ON v.rn = s.n
WHERE s.n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM governance_eval_run_results));
INSERT INTO governance_eval_run_results (
    run_id, case_id, case_name, query_text, expected_task_type, actual_task_type, task_type_matched,
    expected_plan_type, actual_plan_type, plan_type_matched, expected_answer_type, actual_answer_type,
    answer_type_matched, degraded, passed, reply, created_at
)
WITH r AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id DESC) AS rn
    FROM governance_eval_runs
    LIMIT 10
),
c AS (
    SELECT id, case_name, query_text, expected_task_type, expected_plan_type, expected_answer_type,
           ROW_NUMBER() OVER (ORDER BY id DESC) AS rn
    FROM governance_eval_cases
    LIMIT 10
)
SELECT
    r.id,
    c.id,
    c.case_name,
    c.query_text,
    c.expected_task_type,
    c.expected_task_type,
    1,
    c.expected_plan_type,
    c.expected_plan_type,
    1,
    c.expected_answer_type,
    c.expected_answer_type,
    1,
    0,
    1,
    CONCAT('seed eval reply ', s.n),
    NOW()
FROM tmp_seq10 s
JOIN r ON r.rn = s.n
JOIN c ON c.rn = s.n
WHERE s.n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM governance_gray_configs));
INSERT INTO governance_gray_configs (
    config_name, query_bucket, traffic_percent, risk_level, enabled, target_version_json, notes, created_at, updated_at
)
SELECT
    CONCAT('seed_gray_', @seed_ts, '_', n),
    IF(n % 2 = 0, 'search', 'faq'),
    LEAST(100, 5 * n),
    'normal',
    1,
    JSON_OBJECT('router', 'v1', 'composer', 'v1'),
    'seed gray config',
    NOW(),
    NOW()
FROM tmp_seq10
WHERE n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM governance_metrics_daily));
SET @start_date := COALESCE((SELECT DATE_ADD(MAX(metric_date), INTERVAL 1 DAY) FROM governance_metrics_daily), DATE_SUB(CURDATE(), INTERVAL @target_rows DAY));
INSERT INTO governance_metrics_daily (
    metric_date, replay_total, degraded_total, degraded_rate, avg_duration_ms, error_total, faq_total, search_total, created_at, updated_at
)
SELECT
    DATE_ADD(@start_date, INTERVAL n - 1 DAY),
    100 + n,
    5 + (n % 3),
    0.05,
    800 + n,
    n % 2,
    40 + n,
    60 + n,
    NOW(),
    NOW()
FROM tmp_seq10
WHERE n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM governance_release_records));
INSERT INTO governance_release_records (
    release_name, target_scope, release_status, eval_case_version_id, baseline_eval_run_id, latest_eval_run_id,
    gray_strategy_json, version_snapshot_json, notes, created_by, created_at, updated_at
)
SELECT
    CONCAT('seed_release_', @seed_ts, '_', n),
    IF(n % 2 = 0, 'search', 'all'),
    IF(n % 3 = 0, 'released', 'draft'),
    (SELECT id FROM governance_eval_case_versions ORDER BY id DESC LIMIT 1),
    (SELECT id FROM governance_eval_runs ORDER BY id DESC LIMIT 1),
    (SELECT id FROM governance_eval_runs ORDER BY id DESC LIMIT 1),
    JSON_OBJECT('traffic_percent', LEAST(100, n * 10)),
    JSON_OBJECT('router', 'v1', 'composer', 'v1'),
    'seed release record',
    'seed',
    NOW(),
    NOW()
FROM tmp_seq10
WHERE n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM governance_replay_checkpoints));
INSERT INTO governance_replay_checkpoints (
    replay_record_id, checkpoint_order, node_name, state_snapshot_json, created_at
)
WITH rr AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id DESC) AS rn
    FROM governance_replay_records
    LIMIT 10
)
SELECT
    rr.id,
    s.n,
    CONCAT('seed_node_', s.n),
    JSON_OBJECT('seed', 1, 'step', s.n),
    NOW()
FROM tmp_seq10 s
JOIN rr ON rr.rn = s.n
WHERE s.n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM governance_replay_tool_ios));
INSERT INTO governance_replay_tool_ios (
    replay_record_id, step_order, step_id, tool_name, purpose, output_key, optional_step, execution_status, input_json, output_json, created_at
)
WITH rr AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id DESC) AS rn
    FROM governance_replay_records
    LIMIT 10
)
SELECT
    rr.id,
    s.n,
    CONCAT('seed_step_', s.n),
    'backend_api',
    'seed',
    CONCAT('seed_output_', s.n),
    0,
    'SUCCESS',
    JSON_OBJECT('query', CONCAT('seed query ', s.n)),
    JSON_OBJECT('reply', CONCAT('seed output ', s.n)),
    NOW()
FROM tmp_seq10 s
JOIN rr ON rr.rn = s.n
WHERE s.n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM knowledge_base));
INSERT INTO knowledge_base (
    category, doc_type, title, content, summary, keywords, related_questions, source_system, entity_type, entity_id,
    city_ids, category_ids, tag_ids, version, priority, owner, language, effective_from, effective_to, published_at,
    view_count, helpful_count, status, created_at, updated_at
)
SELECT
    'seed',
    'faq',
    CONCAT('Seed Knowledge ', @seed_ts, '-', n),
    CONCAT('This is seeded knowledge content #', n),
    CONCAT('seed summary ', n),
    'seed,knowledge',
    JSON_ARRAY(CONCAT('seed question ', n)),
    'seed',
    'product',
    CAST(n AS CHAR),
    '1',
    '1',
    '1',
    'v1',
    n,
    'seed',
    'zh-CN',
    NOW(),
    NULL,
    NOW(),
    0,
    0,
    1,
    NOW(),
    NOW()
FROM tmp_seq10
WHERE n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM knowledge_vector));
INSERT INTO knowledge_vector (
    knowledge_id, vector_data, created_at
)
SELECT
    src.id,
    CONCAT('[', ROUND(RAND() * 1, 6), ',', ROUND(RAND() * 1, 6), ',', ROUND(RAND() * 1, 6), ']'),
    NOW()
FROM (
    SELECT
        kb.id,
        ROW_NUMBER() OVER (ORDER BY kb.id) AS rn
    FROM knowledge_base kb
    LEFT JOIN knowledge_vector kv ON kv.knowledge_id = kb.id
    WHERE kv.id IS NULL
) src
JOIN tmp_seq10 s ON s.n = src.rn
WHERE s.n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM knowledge_chunk));
INSERT INTO knowledge_chunk (
    knowledge_id, chunk_no, chunk_type, chunk_text, metadata_json, status, created_at, updated_at
)
WITH kb AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM knowledge_base
    LIMIT 10
)
SELECT
    kb.id,
    1,
    'TEXT',
    CONCAT('seed chunk for knowledge ', kb.id),
    JSON_OBJECT('seed', 1),
    1,
    NOW(),
    NOW()
FROM tmp_seq10 s
JOIN kb ON kb.rn = s.n
WHERE s.n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM local_activity_admin));
INSERT INTO local_activity_admin (
    organizer_user_id, title, subtitle, category_code, description, location_text, address,
    latitude, longitude, cover_url, fee_type, fee_amount, capacity, allow_waitlist, require_checkin,
    status, start_at, end_at, contact_phone, reminder_minutes, review_note, created_at, updated_at
)
SELECT
    u.user_id,
    CONCAT('Seed Admin Activity ', @seed_ts, '-', n),
    CONCAT('Subtitle ', n),
    'SPORT',
    CONCAT('Seed local activity admin description ', n),
    CONCAT('Seed Plaza ', n),
    CONCAT('Seed Road ', n),
    30.500000 + n * 0.001,
    114.300000 + n * 0.001,
    CONCAT('https://seed.example.com/activity/admin/', n, '.jpg'),
    IF(n % 3 = 0, 'PAID', 'FREE'),
    IF(n % 3 = 0, 19.90, 0.00),
    20 + n,
    1,
    1,
    'PUBLISHED',
    DATE_ADD(NOW(), INTERVAL n DAY),
    DATE_ADD(DATE_ADD(NOW(), INTERVAL n DAY), INTERVAL 2 HOUR),
    CONCAT('1380000', LPAD(n, 4, '0')),
    1440,
    'seed admin activity',
    NOW(),
    NOW()
FROM (
    SELECT n, ((n - 1) % (SELECT COUNT(*) FROM users) + 1) AS rn_user
    FROM tmp_seq10
    WHERE n <= @need
) s
JOIN (
    SELECT user_id, ROW_NUMBER() OVER (ORDER BY user_id) AS rn
    FROM users
) u ON u.rn = s.rn_user;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM local_activity_enrollment));
INSERT INTO local_activity_enrollment (
    activity_id, user_id, status, waitlist_rank, extra_fields, checkin_code, checkin_at, cancelled_at, created_at, updated_at
)
WITH a AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM local_activity_admin
    LIMIT 10
),
u AS (
    SELECT user_id, ROW_NUMBER() OVER (ORDER BY user_id) AS rn
    FROM users
    LIMIT 10
)
SELECT
    a.id,
    u.user_id,
    CASE
        WHEN s.n % 5 = 0 THEN 'WAITLIST'
        WHEN s.n % 4 = 0 THEN 'CHECKED_IN'
        WHEN s.n % 3 = 0 THEN 'CANCELLED'
        ELSE 'CONFIRMED'
    END,
    CASE WHEN s.n % 5 = 0 THEN s.n ELSE NULL END,
    JSON_OBJECT('seed', 1, 'note', CONCAT('enrollment ', s.n)),
    CONCAT('CK', LPAD(s.n, 6, '0')),
    NULL,
    NULL,
    NOW(),
    NOW()
FROM tmp_seq10 s
JOIN a ON a.rn = s.n
JOIN u ON u.rn = s.n
WHERE s.n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM local_activity_media));
INSERT INTO local_activity_media (
    activity_id, media_type, media_url, sort_order
)
WITH a AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM local_activity_admin
    LIMIT 10
)
SELECT
    a.id,
    CASE WHEN s.n % 3 = 0 THEN 'DOC' WHEN s.n % 2 = 0 THEN 'GALLERY' ELSE 'COVER' END,
    CONCAT('https://seed.example.com/activity/media/', s.n, '.jpg'),
    s.n
FROM tmp_seq10 s
JOIN a ON a.rn = s.n
WHERE s.n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM local_activity_notification_rule));
INSERT INTO local_activity_notification_rule (
    activity_id, created_by, trigger_event, channel, offset_minutes, template_title, template_body, status, created_at, updated_at
)
WITH a AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM local_activity_admin
    LIMIT 10
),
u AS (
    SELECT user_id, ROW_NUMBER() OVER (ORDER BY user_id) AS rn
    FROM users
    LIMIT 10
)
SELECT
    a.id,
    u.user_id,
    CASE
        WHEN s.n % 4 = 0 THEN 'POST_EVENT'
        WHEN s.n % 3 = 0 THEN 'ON_WAITLIST_PROMOTE'
        WHEN s.n % 2 = 0 THEN 'BEFORE_START'
        ELSE 'ON_APPROVED'
    END,
    CASE WHEN s.n % 3 = 0 THEN 'SMS' WHEN s.n % 2 = 0 THEN 'EMAIL' ELSE 'INBOX' END,
    30 * s.n,
    CONCAT('Seed Notify Title ', s.n),
    CONCAT('Seed notify body ', s.n),
    'ACTIVE',
    NOW(),
    NOW()
FROM tmp_seq10 s
JOIN a ON a.rn = s.n
JOIN u ON u.rn = s.n
WHERE s.n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM local_activity_schedule_template));
INSERT INTO local_activity_schedule_template (
    owner_user_id, title, category_code, weekday, start_time, end_time, location_text, recurrence_rule,
    default_capacity, default_fee_type, reminder_minutes, status, created_at, updated_at
)
WITH u AS (
    SELECT user_id, ROW_NUMBER() OVER (ORDER BY user_id) AS rn
    FROM users
    LIMIT 10
)
SELECT
    u.user_id,
    CONCAT('Seed Schedule ', @seed_ts, '-', s.n),
    'SPORT',
    ((s.n - 1) % 7) + 1,
    MAKETIME(8 + (s.n % 4), 0, 0),
    MAKETIME(10 + (s.n % 4), 0, 0),
    CONCAT('Seed Location ', s.n),
    'WEEKLY',
    20 + s.n,
    IF(s.n % 2 = 0, 'FREE', 'AA'),
    1440,
    'ACTIVE',
    NOW(),
    NOW()
FROM tmp_seq10 s
JOIN u ON u.rn = s.n
WHERE s.n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM local_activity_story));
INSERT INTO local_activity_story (
    activity_id, author_user_id, title, cover_url, summary, content, visibility, likes, created_at, updated_at
)
WITH a AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM local_activity_admin
    LIMIT 10
),
u AS (
    SELECT user_id, ROW_NUMBER() OVER (ORDER BY user_id) AS rn
    FROM users
    LIMIT 10
)
SELECT
    a.id,
    u.user_id,
    CONCAT('Seed Story ', @seed_ts, '-', s.n),
    CONCAT('https://seed.example.com/activity/story/', s.n, '.jpg'),
    CONCAT('Seed story summary ', s.n),
    CONCAT('Seed story content ', s.n),
    CASE WHEN s.n % 3 = 0 THEN 'COMMUNITY' WHEN s.n % 2 = 0 THEN 'PUBLIC' ELSE 'PRIVATE' END,
    s.n,
    NOW(),
    NOW()
FROM tmp_seq10 s
JOIN a ON a.rn = s.n
JOIN u ON u.rn = s.n
WHERE s.n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM local_activity_tag));
INSERT INTO local_activity_tag (
    activity_id, tag
)
WITH a AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM local_activity
    LIMIT 10
)
SELECT
    a.id,
    CONCAT('seed_tag_', s.n)
FROM tmp_seq10 s
JOIN a ON a.rn = s.n
WHERE s.n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM neighbor_support_task_admin));
INSERT INTO neighbor_support_task_admin (
    requester_user_id, assignee_user_id, title, category_code, description, location_text, latitude, longitude,
    start_time, end_time, volunteer_slots, filled_slots, priority, reward_points, status, created_at, updated_at
)
WITH u AS (
    SELECT user_id, ROW_NUMBER() OVER (ORDER BY user_id) AS rn
    FROM users
    LIMIT 10
)
SELECT
    u.user_id,
    NULL,
    CONCAT('Seed Task Admin ', @seed_ts, '-', s.n),
    'DELIVERY',
    CONCAT('Seed neighbor support admin task ', s.n),
    CONCAT('Seed Community ', s.n),
    30.600000 + s.n * 0.001,
    114.200000 + s.n * 0.001,
    DATE_ADD(NOW(), INTERVAL s.n DAY),
    DATE_ADD(DATE_ADD(NOW(), INTERVAL s.n DAY), INTERVAL 1 HOUR),
    1 + (s.n % 3),
    0,
    CASE WHEN s.n % 3 = 0 THEN 'HIGH' WHEN s.n % 2 = 0 THEN 'MEDIUM' ELSE 'LOW' END,
    5 * s.n,
    'OPEN',
    NOW(),
    NOW()
FROM tmp_seq10 s
JOIN u ON u.rn = s.n
WHERE s.n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM neighbor_support_task));
INSERT INTO neighbor_support_task (
    requester_user_id, assignee_user_id, title, category_code, description, location_text, latitude, longitude,
    start_time, end_time, volunteer_slots, filled_slots, priority, reward_points, status, created_at, updated_at
)
WITH u AS (
    SELECT user_id, ROW_NUMBER() OVER (ORDER BY user_id) AS rn
    FROM users
    LIMIT 10
)
SELECT
    u.user_id,
    NULL,
    CONCAT('Seed Task ', @seed_ts, '-', s.n),
    'ERRAND',
    CONCAT('Seed neighbor support task ', s.n),
    CONCAT('Seed District ', s.n),
    30.700000 + s.n * 0.001,
    114.100000 + s.n * 0.001,
    DATE_ADD(NOW(), INTERVAL s.n DAY),
    DATE_ADD(DATE_ADD(NOW(), INTERVAL s.n DAY), INTERVAL 2 HOUR),
    1 + (s.n % 3),
    0,
    CASE WHEN s.n % 3 = 0 THEN 'HIGH' WHEN s.n % 2 = 0 THEN 'MEDIUM' ELSE 'LOW' END,
    10 * s.n,
    'OPEN',
    NOW(),
    NOW()
FROM tmp_seq10 s
JOIN u ON u.rn = s.n
WHERE s.n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM neighbor_support_assignment));
INSERT INTO neighbor_support_assignment (
    task_id, user_id, status, joined_at, completed_at
)
WITH ta AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM neighbor_support_task_admin
    LIMIT 10
),
u AS (
    SELECT user_id, ROW_NUMBER() OVER (ORDER BY user_id) AS rn
    FROM users
    LIMIT 10
)
SELECT
    ta.id,
    u.user_id,
    CASE WHEN s.n % 4 = 0 THEN 'DONE' WHEN s.n % 3 = 0 THEN 'REJECTED' WHEN s.n % 2 = 0 THEN 'CONFIRMED' ELSE 'APPLIED' END,
    NOW(),
    CASE WHEN s.n % 4 = 0 THEN NOW() ELSE NULL END
FROM tmp_seq10 s
JOIN ta ON ta.rn = s.n
JOIN u ON u.rn = s.n
WHERE s.n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM query_expand_dict));
INSERT INTO query_expand_dict (
    query_term, expand_terms_json, expand_categories_json, expand_tags_json, status, updated_at, created_at
)
SELECT
    CONCAT('seed_query_', @seed_ts, '_', n),
    JSON_ARRAY(CONCAT('seed_term_', n), CONCAT('seed_term_', n + 100)),
    JSON_ARRAY(1, 2),
    JSON_ARRAY('seed', 'auto'),
    'enabled',
    NOW(),
    NOW()
FROM tmp_seq10
WHERE n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM store_search_snapshot));
SET @base := COALESCE((SELECT MAX(store_id) FROM store_search_snapshot), 0);
INSERT INTO store_search_snapshot (
    store_id, merchant_id, store_name, title, summary_text, category_id, category_name, category_path, tag_names,
    city_id, district_id, business_area_id, lat, lng, avg_price, rating, review_count, hot_score,
    searchable_status, publish_status, visible_status, base_open_time_desc, created_at, updated_at
)
WITH u AS (
    SELECT user_id, ROW_NUMBER() OVER (ORDER BY user_id) AS rn
    FROM users
    LIMIT 10
)
SELECT
    @base + s.n,
    u.user_id,
    CONCAT('Seed Store ', @base + s.n),
    CONCAT('Seed Store Title ', s.n),
    CONCAT('Seed store summary ', s.n),
    NULL,
    NULL,
    NULL,
    JSON_ARRAY('seed', 'community'),
    NULL,
    NULL,
    NULL,
    30.800000 + s.n * 0.001,
    114.000000 + s.n * 0.001,
    30.00 + s.n,
    4.00,
    10 + s.n,
    50 + s.n,
    'searchable',
    'on_shelf',
    'visible',
    '09:00-21:00',
    NOW(),
    NOW()
FROM tmp_seq10 s
JOIN u ON u.rn = s.n
WHERE s.n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM community_feed_comment));
INSERT INTO community_feed_comment (
    feed_id, user_id, content, created_at, status
)
WITH f AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM community_feed
    LIMIT 10
),
u AS (
    SELECT user_id, ROW_NUMBER() OVER (ORDER BY user_id) AS rn
    FROM users
    LIMIT 10
)
SELECT
    f.id,
    u.user_id,
    CONCAT('seed comment ', @seed_ts, '-', s.n),
    NOW(),
    'ACTIVE'
FROM tmp_seq10 s
JOIN f ON f.rn = s.n
JOIN u ON u.rn = s.n
WHERE s.n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM community_feed_like));
INSERT IGNORE INTO community_feed_like (
    feed_id, user_id, created_at
)
WITH f AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM community_feed
    LIMIT 10
),
u AS (
    SELECT user_id, ROW_NUMBER() OVER (ORDER BY user_id) AS rn
    FROM users
    LIMIT 10
)
SELECT
    f.id,
    u.user_id,
    NOW()
FROM tmp_seq10 s
JOIN f ON f.rn = s.n
JOIN u ON u.rn = s.n
WHERE s.n <= @need;

SET @need := GREATEST(0, @target_rows - (SELECT COUNT(*) FROM community_feed_topic));
INSERT IGNORE INTO community_feed_topic (
    feed_id, topic_id, created_at
)
WITH f AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM community_feed
    LIMIT 10
),
t AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM topic
    LIMIT 10
)
SELECT
    f.id,
    t.id,
    NOW()
FROM tmp_seq10 s
JOIN f ON f.rn = s.n
JOIN t ON t.rn = s.n
WHERE s.n <= @need;

DROP TEMPORARY TABLE IF EXISTS tmp_seq10;
