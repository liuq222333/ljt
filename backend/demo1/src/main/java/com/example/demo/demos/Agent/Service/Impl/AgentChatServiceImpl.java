package com.example.demo.demos.Agent.Service.Impl;

import com.example.demo.demos.Agent.Config.AgentAiProperties;
import com.example.demo.demos.Agent.Pojo.AgentChatMessage;
import com.example.demo.demos.Agent.Pojo.AgentChatRequest;
import com.example.demo.demos.Agent.Pojo.AgentChatResponse;
import com.example.demo.demos.Agent.Service.AgentChatService;
import com.example.demo.demos.Agent.Service.helper.AgentToolHandler;
import com.example.demo.demos.Agent.Service.helper.deepseek.DeepSeekPayload;
import com.example.demo.demos.Agent.Service.helper.deepseek.DeepSeekResponse;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Concrete {@link AgentChatService} implementation that orchestrates the
 * DeepSeek HTTP call and the follow-up tool execution.
 */
@Service
public class AgentChatServiceImpl implements AgentChatService {

    private static final String DEFAULT_SYSTEM_PROMPT =
            "You are a helpful assistant for a community marketplace. Respond in concise Simplified Chinese and, when users clearly want to publish an item, call the publish_product tool with structured arguments.";

    private final RestTemplate restTemplate;
    private final AgentAiProperties properties;
    private final AgentToolHandler agentToolHandler;

    public AgentChatServiceImpl(RestTemplateBuilder restTemplateBuilder,
                                AgentAiProperties properties,
                                AgentToolHandler agentToolHandler) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
        this.properties = properties;
        this.agentToolHandler = agentToolHandler;
    }

    @Override
    public AgentChatResponse chat(AgentChatRequest request, String authorization) {
        if (request == null || CollectionUtils.isEmpty(request.getMessages())) {
            throw new IllegalArgumentException("Conversation cannot be empty");
        }
        if (!StringUtils.hasText(properties.getKey())) {
            throw new IllegalStateException("DeepSeek API key is not configured");
        }
        //构建payload
        DeepSeekPayload payload = buildPayload(request);
        payload.setTools(agentToolHandler.getToolDefinitions());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(properties.getKey());

        HttpEntity<DeepSeekPayload> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<DeepSeekResponse> response = restTemplate.postForEntity(
                    properties.getUrl(), entity, DeepSeekResponse.class);

            DeepSeekResponse body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("DeepSeek API returned an empty body");
            }

            DeepSeekResponse.ResponseMessage message = body.extractMessage();
            if (message != null && !CollectionUtils.isEmpty(message.getToolCalls())) {
                AgentChatResponse toolResp = agentToolHandler.handleToolCalls(body, message.getToolCalls(), authorization);
                if (toolResp != null) {
                    return toolResp;
                }
            }

            String reply = message != null ? message.getContent() : null;
            if (!StringUtils.hasText(reply)) {
                reply = "抱歉，我暂时没有获取到有效回答哦~";
            }

            AgentChatResponse agentChatResponse = new AgentChatResponse();
            agentChatResponse.setReply(reply.trim());
            agentChatResponse.setModel(body.getModel());
            agentChatResponse.setRequestId(body.getId());
            agentChatResponse.setUsage(body.getUsage());
            return agentChatResponse;
        } catch (HttpStatusCodeException ex) {
            String detail = ex.getResponseBodyAsString(StandardCharsets.UTF_8);
            throw new IllegalStateException("Failed to call DeepSeek: " + ex.getRawStatusCode() + " - " + detail, ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to call DeepSeek: " + ex.getMessage(), ex);
        }
    }

    private DeepSeekPayload buildPayload(AgentChatRequest request) {
        DeepSeekPayload payload = new DeepSeekPayload();
        payload.setModel(StringUtils.hasText(request.getModel()) ? request.getModel() : properties.getModel());
        payload.setTemperature(request.getTemperature() != null ? request.getTemperature() : properties.getTemperature());
        payload.setMaxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : properties.getMaxTokens());
        payload.setStream(request.getStream() != null ? request.getStream() : properties.getStream());

        List<AgentChatMessage> history = request.getMessages().stream()
                .filter(Objects::nonNull)
                .map(AgentChatMessage::normalize)
                .filter(msg -> StringUtils.hasText(msg.getContent()))
                .collect(Collectors.toList());
        if (history.isEmpty()) {
            throw new IllegalArgumentException("Conversation cannot be empty");
        }
        List<AgentChatMessage> finalMessages = new ArrayList<>(history.size() + 1);
        AgentChatMessage system = new AgentChatMessage();
        system.setRole("system");
        system.setContent(DEFAULT_SYSTEM_PROMPT);
        finalMessages.add(system);
        finalMessages.addAll(history);
        payload.setMessages(finalMessages);
        return payload;
    }
}