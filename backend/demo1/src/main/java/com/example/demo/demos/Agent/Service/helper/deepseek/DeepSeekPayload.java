package com.example.demo.demos.Agent.Service.helper.deepseek;

import com.example.demo.demos.Agent.Pojo.AgentChatMessage;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 调用 DeepSeek Chat Completions 的请求载体。
 * 包含模型、采样参数、历史消息以及可供模型选择的工具列表。
 */
public class DeepSeekPayload {
    private String model;
    private Double temperature;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private Boolean stream;
    private List<AgentChatMessage> messages;
    private List<ToolDefinition> tools;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    public List<AgentChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<AgentChatMessage> messages) {
        this.messages = messages;
    }

    public List<ToolDefinition> getTools() {
        return tools;
    }

    public void setTools(List<ToolDefinition> tools) {
        this.tools = tools;
    }

    public static class ToolDefinition {
        private String type = "function";
        private ToolFunction function;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public ToolFunction getFunction() {
            return function;
        }

        public void setFunction(ToolFunction function) {
            this.function = function;
        }
    }

    public static class ToolFunction {
        private String name;
        private String description;
        private java.util.Map<String, Object> parameters;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public java.util.Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(java.util.Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }
}
