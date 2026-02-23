package com.example.demo.demos.Agent.Service.helper.deepseek;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek Chat Completions 的响应结构体。
 * 其中包含本次请求的 ID/模型、消息选择列表、用量统计等。
 * 可通过 {@link #extractMessage()} 便捷地取出首个消息。
 */
public class DeepSeekResponse {
    private String id;
    private String model;
    private List<Choice> choices = Collections.emptyList();
    private Map<String, Object> usage;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    public Map<String, Object> getUsage() {
        return usage;
    }

    public void setUsage(Map<String, Object> usage) {
        this.usage = usage;
    }

    /**
     * 便捷方法：提取首个 Choice 的消息内容。
     */
    public ResponseMessage extractMessage() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        Choice first = choices.get(0);
        if (first == null) {
            return null;
        }
        return first.getMessage();
    }

    public static class Choice {
        private ResponseMessage message;

        public ResponseMessage getMessage() {
            return message;
        }

        public void setMessage(ResponseMessage message) {
            this.message = message;
        }
    }

    public static class ResponseMessage {
        private String role;
        private String content;

        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls = Collections.emptyList();

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public List<ToolCall> getToolCalls() {
            return toolCalls;
        }

        public void setToolCalls(List<ToolCall> toolCalls) {
            this.toolCalls = toolCalls;
        }
    }

    public static class ToolCall {
        private String id;
        private String type;
        private ToolFunction function;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

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
        private String arguments;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getArguments() {
            return arguments;
        }

        public void setArguments(String arguments) {
            this.arguments = arguments;
        }
    }
}
