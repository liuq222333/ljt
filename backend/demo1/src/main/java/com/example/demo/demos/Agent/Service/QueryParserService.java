package com.example.demo.demos.Agent.Service;

import com.example.demo.demos.Agent.Config.AgentAiProperties;
import com.example.demo.demos.common.enums.FollowUpType;
import com.example.demo.demos.common.enums.TaskType;
import com.example.demo.demos.Agent.Pojo.AgentChatMessage;
import com.example.demo.demos.Agent.Pojo.CandidateSlots;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.example.demo.demos.Agent.Service.helper.deepseek.DeepSeekPayload;
import com.example.demo.demos.Agent.Service.helper.deepseek.DeepSeekResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Query Parser 核心服务 — 将用户自然语言输入解析为结构化的 ParsedIntent。
 * 对应 Query Parser 设计文档的整体实现。
 *
 * 实现方式：LLM + 结构化 JSON 输出（V1）。
 * 降级策略：LLM 超时或失败时，自动切换到 RuleFallbackParser。
 */
@Service
public class QueryParserService {

    private static final Logger log = LoggerFactory.getLogger(QueryParserService.class);

    /** LLM 调用超时阈值（毫秒），超过则降级 */
    private static final long LLM_TIMEOUT_MS = 500;

    /** LLM 输出格式异常时的最大重试次数 */
    private static final int MAX_RETRY = 1;

    private final RestTemplate restTemplate;
    private final AgentAiProperties properties;
    private final RuleFallbackParser ruleFallbackParser;
    private final ObjectMapper objectMapper;

    /**
     * System Prompt — 指导 LLM 做意图识别并输出结构化 JSON。
     * 与 Query Parser 设计文档 §11.2 Prompt 模板对齐。
     */
    private static final String INTENT_SYSTEM_PROMPT = "你是一个意图识别助手。根据用户的输入和对话上下文，输出以下结构化 JSON（只输出 JSON，不要输出任何其他内容）：\n"
            + "\n"
            + "{\n"
            + "  \"task_type\": \"枚举值，见下方列表\",\n"
            + "  \"intent_confidence\": 0.0~1.0,\n"
            + "  \"query_text\": \"用户原始输入\",\n"
            + "  \"candidate_slots\": {\n"
            + "    \"keyword\": \"核心搜索词（可选）\",\n"
            + "    \"category_text\": \"类目文本（可选）\",\n"
            + "    \"crowd_tag_text\": \"人群标签如亲子（可选）\",\n"
            + "    \"scene_tag_text\": \"场景标签如周末（可选）\",\n"
            + "    \"city_text\": \"城市（可选）\",\n"
            + "    \"district_text\": \"区县（可选）\",\n"
            + "    \"location_text\": \"位置表达如附近（可选）\",\n"
            + "    \"price_text\": \"价格表达如200以内（可选）\",\n"
            + "    \"date_text\": \"时间表达如明天（可选）\",\n"
            + "    \"sort_text\": \"排序偏好如便宜的（可选）\",\n"
            + "    \"entity_type\": \"目标实体类型 product/event/store（可选）\",\n"
            + "    \"entity_ref\": \"实体指代如这个、第一个（可选）\"\n"
            + "  },\n"
            + "  \"need_realtime\": true/false,\n"
            + "  \"need_explanation\": true/false,\n"
            + "  \"need_recommendation\": true/false,\n"
            + "  \"is_follow_up\": true/false,\n"
            + "  \"follow_up_type\": \"枚举值（仅追问时填写，否则为null）\",\n"
            + "  \"is_negation\": true/false,\n"
            + "  \"negated_entities\": []\n"
            + "}\n"
            + "\n"
            + "task_type 可选值：\n"
            + "- product_search: 商品搜索/推荐\n"
            + "- event_search: 活动搜索/推荐\n"
            + "- store_search: 门店搜索\n"
            + "- faq_query: FAQ/规则问答\n"
            + "- realtime_query: 强实时查询\n"
            + "- mixed_search_knowledge: 搜索+知识\n"
            + "- mixed_search_realtime: 搜索+实时\n"
            + "- follow_up: 多轮追问\n"
            + "- clarification_response: 回答澄清\n"
            + "- chitchat: 闲聊/无法识别\n"
            + "\n"
            + "follow_up_type 可选值（仅 is_follow_up=true 时填写）：\n"
            + "- add_constraint: 追加条件\n"
            + "- change_constraint: 修改条件\n"
            + "- negate_result: 否定结果\n"
            + "- select_entity: 选择实体\n"
            + "- ask_detail: 追问详情\n"
            + "- switch_topic: 切换话题\n"
            + "\n"
            + "注意：\n"
            + "1. candidate_slots 中只放用户说了的内容，不要自己推断\n"
            + "2. intent_confidence 要真实反映你的判断确定度\n"
            + "3. 如果用户说\"这个\"\"那个\"，在 candidate_slots 中用 entity_ref 记录\n"
            + "4. 只输出 JSON，不要输出任何解释文字";

    public QueryParserService(RestTemplateBuilder restTemplateBuilder,
                              AgentAiProperties properties,
                              RuleFallbackParser ruleFallbackParser) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(LLM_TIMEOUT_MS))
                .setReadTimeout(Duration.ofMillis(LLM_TIMEOUT_MS))
                .build();
        this.properties = properties;
        this.ruleFallbackParser = ruleFallbackParser;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 解析用户输入意图 — 主入口。
     *
     * @param currentMessage 当前用户输入
     * @param recentMessages 最近的对话历史（可选）
     * @return 结构化意图识别结果
     */
    public ParsedIntent parse(String currentMessage, List<AgentChatMessage> recentMessages) {
        if (!StringUtils.hasText(currentMessage)) {
            ParsedIntent empty = new ParsedIntent();
            empty.setTaskType(TaskType.CHITCHAT);
            empty.setQueryText("");
            empty.setIntentConfidence(0.0);
            return empty;
        }

        // 尝试 LLM 解析
        for (int attempt = 0; attempt <= MAX_RETRY; attempt++) {
            try {
                ParsedIntent result = callLlmForIntent(currentMessage, recentMessages);
                if (result != null) {
                    log.info("QueryParser LLM 解析成功: taskType={}, confidence={}",
                            result.getTaskType(), result.getIntentConfidence());
                    return result;
                }
            } catch (Exception e) {
                log.warn("QueryParser LLM 调用失败 (attempt {}/{}): {}",
                        attempt + 1, MAX_RETRY + 1, e.getMessage());
            }
        }

        // LLM 不可用，降级到规则引擎
        log.info("QueryParser 降级到规则引擎: input={}", currentMessage);
        return ruleFallbackParser.parse(currentMessage);
    }

    /**
     * 调用 LLM 进行意图识别。
     */
    private ParsedIntent callLlmForIntent(String currentMessage, List<AgentChatMessage> recentMessages) {
        // 构建消息列表
        List<AgentChatMessage> messages = new ArrayList<>();

        // System Prompt
        AgentChatMessage systemMsg = new AgentChatMessage();
        systemMsg.setRole("system");
        systemMsg.setContent(INTENT_SYSTEM_PROMPT);
        messages.add(systemMsg);

        // 如果有上下文，加入最近消息（最多 6 条）
        if (recentMessages != null && !recentMessages.isEmpty()) {
            int start = Math.max(0, recentMessages.size() - 6);
            for (int i = start; i < recentMessages.size(); i++) {
                messages.add(recentMessages.get(i));
            }
        }

        // 当前用户输入
        AgentChatMessage userMsg = new AgentChatMessage();
        userMsg.setRole("user");
        userMsg.setContent(currentMessage);
        messages.add(userMsg);

        // 构建 Payload
        DeepSeekPayload payload = new DeepSeekPayload();
        payload.setModel(properties.getModel());
        payload.setTemperature(0.1); // 意图识别用低温度保证稳定性
        payload.setMaxTokens(512);   // 意图 JSON 不需要太多 token
        payload.setStream(false);
        payload.setMessages(messages);

        // 调用 LLM
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(properties.getKey());

        HttpEntity<DeepSeekPayload> entity = new HttpEntity<>(payload, headers);

        long startTime = System.currentTimeMillis();
        ResponseEntity<DeepSeekResponse> response = restTemplate.postForEntity(
                properties.getUrl(), entity, DeepSeekResponse.class);
        long elapsed = System.currentTimeMillis() - startTime;
        log.debug("QueryParser LLM 调用耗时: {}ms", elapsed);

        DeepSeekResponse body = response.getBody();
        if (body == null) {
            return null;
        }

        DeepSeekResponse.ResponseMessage message = body.extractMessage();
        if (message == null || !StringUtils.hasText(message.getContent())) {
            return null;
        }

        // 解析 LLM 返回的 JSON
        return parseLlmResponse(message.getContent(), currentMessage);
    }

    /**
     * 解析 LLM 返回的 JSON 字符串为 ParsedIntent。
     */
    private ParsedIntent parseLlmResponse(String jsonContent, String originalInput) {
        try {
            // 提取 JSON（LLM 可能在 JSON 前后带有 markdown 标记）
            String json = extractJson(jsonContent);
            JsonNode root = objectMapper.readTree(json);

            ParsedIntent intent = new ParsedIntent();
            intent.setQueryText(originalInput);

            // task_type
            intent.setTaskType(TaskType.fromCode(
                    getTextValue(root, "task_type")));

            // intent_confidence
            intent.setIntentConfidence(
                    root.has("intent_confidence") ? root.get("intent_confidence").asDouble(0.5) : 0.5);

            // 二级标记
            intent.setNeedRealtime(getBoolValue(root, "need_realtime"));
            intent.setNeedExplanation(getBoolValue(root, "need_explanation"));
            intent.setNeedRecommendation(getBoolValue(root, "need_recommendation"));
            intent.setFollowUp(getBoolValue(root, "is_follow_up"));
            intent.setNegation(getBoolValue(root, "is_negation"));

            // follow_up_type
            intent.setFollowUpType(FollowUpType.fromCode(
                    getTextValue(root, "follow_up_type")));

            // negated_entities
            if (root.has("negated_entities") && root.get("negated_entities").isArray()) {
                List<String> negated = new ArrayList<>();
                root.get("negated_entities").forEach(n -> negated.add(n.asText()));
                intent.setNegatedEntities(negated);
            }

            // candidate_slots
            if (root.has("candidate_slots")) {
                JsonNode slotsNode = root.get("candidate_slots");
                CandidateSlots slots = new CandidateSlots();
                slots.setKeyword(getTextValue(slotsNode, "keyword"));
                slots.setCategoryText(getTextValue(slotsNode, "category_text"));
                slots.setCrowdTagText(getTextValue(slotsNode, "crowd_tag_text"));
                slots.setSceneTagText(getTextValue(slotsNode, "scene_tag_text"));
                slots.setCityText(getTextValue(slotsNode, "city_text"));
                slots.setDistrictText(getTextValue(slotsNode, "district_text"));
                slots.setLocationText(getTextValue(slotsNode, "location_text"));
                slots.setPriceText(getTextValue(slotsNode, "price_text"));
                slots.setDateText(getTextValue(slotsNode, "date_text"));
                slots.setSortText(getTextValue(slotsNode, "sort_text"));
                slots.setEntityType(getTextValue(slotsNode, "entity_type"));
                slots.setEntityRef(getTextValue(slotsNode, "entity_ref"));
                intent.setCandidateSlots(slots);
            }

            return intent;

        } catch (Exception e) {
            log.warn("QueryParser JSON 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 LLM 输出中提取 JSON 字符串。
     * LLM 有时会用 ```json ... ``` 包裹或在前后加文字说明。
     */
    private String extractJson(String content) {
        if (content == null) {
            return "{}";
        }
        String trimmed = content.trim();

        // 去掉 markdown 代码块标记
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        trimmed = trimmed.trim();

        // 找到第一个 { 和最后一个 }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }

        return trimmed;
    }

    private String getTextValue(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        String val = node.get(field).asText();
        return StringUtils.hasText(val) && !"null".equals(val) ? val : null;
    }

    private boolean getBoolValue(JsonNode node, String field) {
        if (node == null || !node.has(field)) {
            return false;
        }
        return node.get(field).asBoolean(false);
    }
}
