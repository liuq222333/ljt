package com.example.demo.demos.common.error;

/**
 * 统一错误码常量类。
 * 错误码规范：XXYYYY
 * XX = 模块（10=Agent, 20=Search, 30=Knowledge, 40=Realtime, 50=Sync）
 * YYYY = 具体错误
 * 与施工单 W00 统一口径对齐。
 */
public final class ErrorCode {

    private ErrorCode() {
        // 常量类禁止实例化
    }

    // ==================== 10: Agent 模块 ====================

    /** 意图识别失败 */
    public static final int AGENT_PARSE_INTENT_FAILED = 100001;

    /** 参数标准化失败 */
    public static final int AGENT_NORMALIZE_PARAMS_FAILED = 100002;

    /** 路由决策失败 */
    public static final int AGENT_ROUTE_TOOLS_FAILED = 100003;

    /** 工具执行失败 */
    public static final int AGENT_EXECUTE_TOOLS_FAILED = 100004;

    /** 回答合成失败 */
    public static final int AGENT_COMPOSE_RESPONSE_FAILED = 100005;

    /** Session 不存在或已过期 */
    public static final int AGENT_SESSION_EXPIRED = 100006;

    /** 全链路超时 */
    public static final int AGENT_TIMEOUT = 100007;

    /** LLM 调用超时 */
    public static final int AGENT_LLM_TIMEOUT = 100008;

    /** LLM 返回格式异常 */
    public static final int AGENT_LLM_FORMAT_ERROR = 100009;

    /** LLM 服务不可用 */
    public static final int AGENT_LLM_UNAVAILABLE = 100010;

    /** Token 配额耗尽 */
    public static final int AGENT_TOKEN_QUOTA_EXCEEDED = 100011;

    /** Prompt Injection 检测到恶意输入 */
    public static final int AGENT_PROMPT_INJECTION = 100012;

    /** 用户限流 */
    public static final int AGENT_RATE_LIMITED = 100013;

    // ==================== 20: Search 模块 ====================

    /** ES 查询超时 */
    public static final int SEARCH_ES_TIMEOUT = 200001;

    /** ES 集群不可用 */
    public static final int SEARCH_ES_UNAVAILABLE = 200002;

    /** ES 查询无结果 */
    public static final int SEARCH_NO_RESULT = 200003;

    /** 快照构建失败 */
    public static final int SEARCH_SNAPSHOT_BUILD_FAILED = 200004;

    /** DSL 构建失败 */
    public static final int SEARCH_DSL_BUILD_FAILED = 200005;

    /** 索引不存在 */
    public static final int SEARCH_INDEX_NOT_FOUND = 200006;

    // ==================== 30: Knowledge 模块 ====================

    /** 知识检索无结果 */
    public static final int KNOWLEDGE_NO_RESULT = 300001;

    /** 向量化失败 */
    public static final int KNOWLEDGE_EMBEDDING_FAILED = 300002;

    /** 知识检索置信度过低 */
    public static final int KNOWLEDGE_LOW_CONFIDENCE = 300003;

    /** 知识冲突（同一主题多条矛盾知识） */
    public static final int KNOWLEDGE_CONFLICT = 300004;

    /** 知识文档已过期 */
    public static final int KNOWLEDGE_EXPIRED = 300005;

    /** 知识索引不可用 */
    public static final int KNOWLEDGE_INDEX_UNAVAILABLE = 300006;

    // ==================== 40: Realtime 模块 ====================

    /** 实时接口超时 */
    public static final int REALTIME_TIMEOUT = 400001;

    /** 实时接口返回异常 */
    public static final int REALTIME_RESPONSE_ERROR = 400002;

    /** 实时接口不可用 */
    public static final int REALTIME_UNAVAILABLE = 400003;

    /** 实时查询部分失败 */
    public static final int REALTIME_PARTIAL_FAILURE = 400004;

    // ==================== 50: Sync 模块 ====================

    /** 主表到快照同步失败 */
    public static final int SYNC_SNAPSHOT_FAILED = 500001;

    /** 快照到 ES 同步失败 */
    public static final int SYNC_ES_INDEX_FAILED = 500002;

    /** 知识到检索库同步失败 */
    public static final int SYNC_KNOWLEDGE_INDEX_FAILED = 500003;

    /** 死信队列写入失败 */
    public static final int SYNC_DEAD_LETTER_FAILED = 500004;

    /** 对账发现数据不一致 */
    public static final int SYNC_RECONCILIATION_MISMATCH = 500005;

    // ==================== 错误信息映射 ====================

    /**
     * 根据错误码获取默认错误信息。
     */
    public static String getMessage(int code) {
        switch (code) {
            case AGENT_PARSE_INTENT_FAILED:         return "意图识别失败";
            case AGENT_NORMALIZE_PARAMS_FAILED:     return "参数标准化失败";
            case AGENT_ROUTE_TOOLS_FAILED:          return "路由决策失败";
            case AGENT_EXECUTE_TOOLS_FAILED:        return "工具执行失败";
            case AGENT_COMPOSE_RESPONSE_FAILED:     return "回答合成失败";
            case AGENT_SESSION_EXPIRED:             return "会话不存在或已过期";
            case AGENT_TIMEOUT:                     return "请求超时，请稍后重试";
            case AGENT_LLM_TIMEOUT:                 return "AI 服务响应超时";
            case AGENT_LLM_FORMAT_ERROR:            return "AI 服务返回格式异常";
            case AGENT_LLM_UNAVAILABLE:             return "AI 服务暂时不可用";
            case AGENT_TOKEN_QUOTA_EXCEEDED:        return "调用额度已用完，请稍后重试";
            case AGENT_PROMPT_INJECTION:            return "检测到不安全的输入";
            case AGENT_RATE_LIMITED:                return "请求过于频繁，请稍后重试";
            case SEARCH_ES_TIMEOUT:                 return "搜索服务超时";
            case SEARCH_ES_UNAVAILABLE:             return "搜索服务暂时不可用";
            case SEARCH_NO_RESULT:                  return "未找到匹配结果";
            case SEARCH_SNAPSHOT_BUILD_FAILED:      return "数据快照构建失败";
            case SEARCH_DSL_BUILD_FAILED:           return "搜索查询构建失败";
            case SEARCH_INDEX_NOT_FOUND:            return "搜索索引不存在";
            case KNOWLEDGE_NO_RESULT:               return "未找到相关知识";
            case KNOWLEDGE_EMBEDDING_FAILED:        return "知识向量化失败";
            case KNOWLEDGE_LOW_CONFIDENCE:          return "知识匹配度过低";
            case KNOWLEDGE_CONFLICT:                return "存在多条矛盾知识，请以最新公告为准";
            case KNOWLEDGE_EXPIRED:                 return "相关知识已过期";
            case KNOWLEDGE_INDEX_UNAVAILABLE:       return "知识检索服务暂时不可用";
            case REALTIME_TIMEOUT:                  return "实时查询超时";
            case REALTIME_RESPONSE_ERROR:           return "实时数据异常";
            case REALTIME_UNAVAILABLE:              return "实时查询服务暂时不可用";
            case REALTIME_PARTIAL_FAILURE:          return "部分实时数据获取失败";
            case SYNC_SNAPSHOT_FAILED:              return "数据快照同步失败";
            case SYNC_ES_INDEX_FAILED:              return "搜索索引同步失败";
            case SYNC_KNOWLEDGE_INDEX_FAILED:       return "知识索引同步失败";
            case SYNC_DEAD_LETTER_FAILED:           return "死信队列写入失败";
            case SYNC_RECONCILIATION_MISMATCH:      return "数据对账不一致";
            default:                                return "未知错误";
        }
    }
}
