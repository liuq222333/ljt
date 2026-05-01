package com.example.demo.demos.Agent.Service.llm;

import com.example.demo.demos.Agent.Config.DeepSeekProperties;
import com.example.demo.demos.Agent.Pojo.AgentChatMessage;
import com.example.demo.demos.Agent.Service.helper.deepseek.DeepSeekPayload;
import com.example.demo.demos.Agent.Service.helper.deepseek.DeepSeekResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 统一的 DeepSeek HTTP 客户端 bean。
 *
 * 现有设计中 RestTemplate / Headers / Payload 拼装散落在 QueryParserService.callLlmForIntent 内，
 * 本类把所有 LLM 出口集中：意图识别、Chitchat 自然回复、商品融合回复都从这里走。
 *
 * 不持有业务语义，只负责：
 *  1. 鉴权 / 序列化 / 超时
 *  2. 提取 first message content
 *  3. 统一日志埋点（耗时 / 失败原因 / token usage）
 *
 * 失败语义：所有公开方法在异常时抛 RuntimeException，调用方决定回退策略。
 */
@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    private final RestTemplate restTemplate;
    private final RestTemplateBuilder restTemplateBuilder;
    private final DeepSeekProperties properties;

    public DeepSeekClient(RestTemplateBuilder restTemplateBuilder, DeepSeekProperties properties) {
        this.restTemplateBuilder = restTemplateBuilder;
        this.properties = properties;
        long connectTimeoutMs = properties.getConnectTimeoutMs() == null ? 3000L : properties.getConnectTimeoutMs();
        long readTimeoutMs = properties.getReadTimeoutMs() == null ? 10000L : properties.getReadTimeoutMs();
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }

    /** key/url 是否就绪。所有 composer 在调用前可先用此方法快速短路。 */
    public boolean isAvailable() {
        return properties != null
                && StringUtils.hasText(properties.getKey())
                && StringUtils.hasText(properties.getUrl());
    }

    /**
     * 用 system + user 单轮调用 DeepSeek，返回纯文本回复。
     */
    public String chatText(String systemPrompt, String userPrompt, ChatOptions options) {
        List<AgentChatMessage> messages = buildSystemUser(systemPrompt, userPrompt);
        return executeForText(messages, options, false);
    }

    /**
     * 用 system + user 单轮调用 DeepSeek，强制 response_format=json_object。
     * 返回的字符串本身就是 JSON。
     */
    public String chatJson(String systemPrompt, String userPrompt, ChatOptions options) {
        List<AgentChatMessage> messages = buildSystemUser(systemPrompt, userPrompt);
        return executeForText(messages, options, true);
    }

    /**
     * 直接喂入完整消息列表（含 system / 历史 user / assistant / 当前 user）。
     * 给 QueryParser 之类带历史的调用方复用。
     */
    public String chatMessages(List<AgentChatMessage> messages, ChatOptions options) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("DeepSeekClient.chatMessages: messages must not be empty");
        }
        return executeForText(messages, options, options != null && options.isJsonMode());
    }

    private String executeForText(List<AgentChatMessage> messages, ChatOptions options, boolean jsonMode) {
        if (!isAvailable()) {
            throw new IllegalStateException("DeepSeek client not configured (missing key or url)");
        }

        DeepSeekPayload payload = new DeepSeekPayload();
        payload.setModel(properties.getModel());
        payload.setMessages(messages);
        payload.setStream(Boolean.FALSE);
        payload.setTemperature(resolveTemperature(options));
        payload.setMaxTokens(resolveMaxTokens(options));
        if (jsonMode) {
            payload.setResponseFormat(new DeepSeekPayload.ResponseFormat("json_object"));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(properties.getKey());
        HttpEntity<DeepSeekPayload> entity = new HttpEntity<>(payload, headers);

        RestTemplate caller = resolveRestTemplate(options);

        long started = System.currentTimeMillis();
        try {
            ResponseEntity<DeepSeekResponse> response = caller.postForEntity(
                    properties.getUrl(), entity, DeepSeekResponse.class);
            long elapsed = System.currentTimeMillis() - started;

            DeepSeekResponse body = response.getBody();
            if (body == null) {
                log.warn("DeepSeekClient chat empty body, latency={}ms", elapsed);
                return null;
            }
            DeepSeekResponse.ResponseMessage message = body.extractMessage();
            String content = message == null ? null : message.getContent();
            if (!StringUtils.hasText(content)) {
                log.warn("DeepSeekClient chat empty content, latency={}ms model={} usage={}",
                        elapsed, body.getModel(), body.getUsage());
                return null;
            }
            log.info("DeepSeekClient chat ok: latency={}ms tokens={} jsonMode={}",
                    elapsed, summarizeUsage(body.getUsage()), jsonMode);
            return content;
        } catch (RuntimeException ex) {
            long elapsed = System.currentTimeMillis() - started;
            log.warn("DeepSeekClient chat failed: latency={}ms reason={}", elapsed, ex.getMessage());
            throw ex;
        }
    }

    private List<AgentChatMessage> buildSystemUser(String systemPrompt, String userPrompt) {
        List<AgentChatMessage> messages = new ArrayList<>();
        if (StringUtils.hasText(systemPrompt)) {
            AgentChatMessage system = new AgentChatMessage();
            system.setRole("system");
            system.setContent(systemPrompt);
            messages.add(system);
        }
        AgentChatMessage user = new AgentChatMessage();
        user.setRole("user");
        user.setContent(userPrompt == null ? "" : userPrompt);
        messages.add(user);
        return messages;
    }

    private double resolveTemperature(ChatOptions options) {
        if (options != null && options.getTemperature() != null) {
            return options.getTemperature();
        }
        Double fallback = properties.getTemperature();
        return fallback == null ? 1.0 : fallback;
    }

    private int resolveMaxTokens(ChatOptions options) {
        if (options != null && options.getMaxTokens() != null) {
            return options.getMaxTokens();
        }
        Integer fallback = properties.getMaxTokens();
        return fallback == null ? 4096 : fallback;
    }

    private RestTemplate resolveRestTemplate(ChatOptions options) {
        if (options == null || options.getReadTimeoutOverrideMs() == null) {
            return restTemplate;
        }
        long connectTimeoutMs = properties.getConnectTimeoutMs() == null ? 3000L : properties.getConnectTimeoutMs();
        return restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(options.getReadTimeoutOverrideMs()))
                .build();
    }

    private String summarizeUsage(Map<String, Object> usage) {
        if (usage == null || usage.isEmpty()) {
            return "n/a";
        }
        Object prompt = usage.get("prompt_tokens");
        Object completion = usage.get("completion_tokens");
        Object total = usage.get("total_tokens");
        return "p=" + prompt + ",c=" + completion + ",t=" + total;
    }

    /** 调用参数对象，builder 风格构造，所有字段可选。 */
    public static class ChatOptions {
        private Double temperature;
        private Integer maxTokens;
        private boolean jsonMode;
        private Long readTimeoutOverrideMs;

        public static ChatOptions defaults() {
            return new ChatOptions();
        }

        public ChatOptions temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public ChatOptions maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public ChatOptions jsonMode(boolean jsonMode) {
            this.jsonMode = jsonMode;
            return this;
        }

        public ChatOptions readTimeoutOverrideMs(Long readTimeoutOverrideMs) {
            this.readTimeoutOverrideMs = readTimeoutOverrideMs;
            return this;
        }

        public Double getTemperature() {
            return temperature;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public boolean isJsonMode() {
            return jsonMode;
        }

        public Long getReadTimeoutOverrideMs() {
            return readTimeoutOverrideMs;
        }
    }
}
