package com.example.demo.demos.common.schema;

import com.example.demo.demos.common.enums.AnswerType;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * compose_response 节点的最终输出。
 */
@Data
public class FinalAnswer {

    private AnswerType answerType;
    private String answerText;
    private List<EntityCard> cards = new ArrayList<EntityCard>();
    private List<String> disclaimers = new ArrayList<String>();
    private List<Citation> citations = new ArrayList<Citation>();
    private List<String> nextActions = new ArrayList<String>();
    private DebugTrace debugTrace;

    @Data
    public static class EntityCard {
        private String entityId;
        private String entityType;
        private String title;
        private String subtitle;
        private String imageUrl;
        private String priceText;
        private List<String> tags = new ArrayList<String>();
        private String locationText;
        private String realtimeStatusText;
        private String recommendReason;
    }

    @Data
    public static class Citation {
        private String docId;
        private String docTitle;
        private String snippet;
        private double confidence;
    }

    @Data
    public static class DebugTrace {
        private String requestId;
        private String traceId;
        private List<NodeTrace> nodeTraces = new ArrayList<NodeTrace>();
        private long totalDurationMs;
        private boolean degraded;
        private String degradeReason;
        private Map<String, Object> metadata = new LinkedHashMap<String, Object>();
    }

    @Data
    public static class NodeTrace {
        private String nodeName;
        private long startTimeMs;
        private long durationMs;
        private boolean success = true;
        private String errorMessage;
        private String inputSummary;
        private String outputSummary;
    }
}
