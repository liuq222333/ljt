package com.example.demo.demos.Agent.Python;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PythonSidecarModels {

    private PythonSidecarModels() {
    }

    @Data
    public static class AgentChatMessagePayload {
        @JsonProperty("role")
        private String role;

        @JsonProperty("content")
        private String content;
    }

    @Data
    public static class QueryParserRequestPayload {
        @JsonProperty("current_message")
        private String currentMessage;

        @JsonProperty("recent_messages")
        private List<AgentChatMessagePayload> recentMessages = new ArrayList<AgentChatMessagePayload>();

        @JsonProperty("session_context_summary")
        private SessionContextPayload sessionContextSummary = new SessionContextPayload();

        @JsonProperty("user_profile")
        private Map<String, Object> userProfile = new LinkedHashMap<String, Object>();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CandidateSlotsPayload {
        @JsonProperty("keyword")
        private String keyword;

        @JsonProperty("category_text")
        private String categoryText;

        @JsonProperty("crowd_tag_text")
        private String crowdTagText;

        @JsonProperty("scene_tag_text")
        private String sceneTagText;

        @JsonProperty("city_text")
        private String cityText;

        @JsonProperty("district_text")
        private String districtText;

        @JsonProperty("location_text")
        private String locationText;

        @JsonProperty("price_text")
        private String priceText;

        @JsonProperty("date_text")
        private String dateText;

        @JsonProperty("sort_text")
        private String sortText;

        @JsonProperty("entity_type")
        private String entityType;

        @JsonProperty("entity_ref")
        private String entityRef;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParsedIntentPayload {
        @JsonProperty("task_type")
        private String taskType;

        @JsonProperty("intent_confidence")
        private Double intentConfidence;

        @JsonProperty("query_text")
        private String queryText;

        @JsonProperty("candidate_slots")
        private CandidateSlotsPayload candidateSlots = new CandidateSlotsPayload();

        @JsonProperty("need_realtime")
        private Boolean needRealtime;

        @JsonProperty("need_explanation")
        private Boolean needExplanation;

        @JsonProperty("need_recommendation")
        private Boolean needRecommendation;

        @JsonProperty("is_follow_up")
        private Boolean followUp;

        @JsonProperty("is_negation")
        private Boolean negation;

        @JsonProperty("follow_up_type")
        private String followUpType;

        @JsonProperty("negated_entities")
        private List<String> negatedEntities = new ArrayList<String>();
    }

    @Data
    public static class NormalizedParamsPayload {
        @JsonProperty("task_type")
        private String taskType;

        @JsonProperty("keywords")
        private String keywords;

        @JsonProperty("entity_type")
        private String entityType;

        @JsonProperty("execution_ready")
        private Boolean executionReady;

        @JsonProperty("missing_required_slots")
        private List<String> missingRequiredSlots = new ArrayList<String>();

        @JsonProperty("validation_errors")
        private List<String> validationErrors = new ArrayList<String>();
    }

    @Data
    public static class SessionContextPayload {
        @JsonProperty("confirmed_constraints")
        private Map<String, Object> confirmedConstraints = new LinkedHashMap<String, Object>();

        @JsonProperty("pending_slots")
        private List<String> pendingSlots = new ArrayList<String>();

        @JsonProperty("candidate_entities")
        private List<Map<String, Object>> candidateEntities = new ArrayList<Map<String, Object>>();

        @JsonProperty("focused_entity_id")
        private String focusedEntityId;

        @JsonProperty("focused_entity_type")
        private String focusedEntityType;

        @JsonProperty("last_selected_entity_ids")
        private List<String> lastSelectedEntityIds = new ArrayList<String>();

        @JsonProperty("dialogue_turn_index")
        private Integer dialogueTurnIndex = 0;

        @JsonProperty("follow_up_context")
        private Map<String, Object> followUpContext = new LinkedHashMap<String, Object>();

        @JsonProperty("current_stage")
        private String currentStage = "initialized";

        @JsonProperty("clarification_history")
        private List<String> clarificationHistory = new ArrayList<String>();
    }

    @Data
    public static class ToolRouterRequestPayload {
        @JsonProperty("parsed_intent")
        private ParsedIntentPayload parsedIntent;

        @JsonProperty("normalized_params")
        private NormalizedParamsPayload normalizedParams;

        @JsonProperty("session_context")
        private SessionContextPayload sessionContext = new SessionContextPayload();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ToolStepPayload {
        @JsonProperty("step_id")
        private String stepId;

        @JsonProperty("tool_name")
        private String toolName;

        @JsonProperty("purpose")
        private String purpose;

        @JsonProperty("input_ref")
        private String inputRef;

        @JsonProperty("depends_on")
        private List<String> dependsOn = new ArrayList<String>();

        @JsonProperty("output_key")
        private String outputKey;

        @JsonProperty("optional")
        private Boolean optional;

        @JsonProperty("params")
        private Object params;

        @JsonProperty("timeout_ms")
        private Integer timeoutMs;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ToolPlanPayload {
        @JsonProperty("plan_id")
        private String planId;

        @JsonProperty("plan_type")
        private String planType;

        @JsonProperty("execution_mode")
        private String executionMode;

        @JsonProperty("requires_clarification")
        private Boolean requiresClarification;

        @JsonProperty("clarification_prompt")
        private String clarificationPrompt;

        @JsonProperty("steps")
        private List<ToolStepPayload> steps = new ArrayList<ToolStepPayload>();

        @JsonProperty("fallback_plan")
        private ToolPlanPayload fallbackPlan;

        @JsonProperty("routing_reason")
        private String routingReason;

        @JsonProperty("plan_confidence")
        private Double planConfidence;

        @JsonProperty("max_parallel_tools")
        private Integer maxParallelTools;
    }

    @Data
    public static class SearchItemPayload {
        @JsonProperty("product_id")
        private String productId;

        @JsonProperty("title")
        private String title;

        @JsonProperty("subtitle")
        private String subtitle;

        @JsonProperty("display_price")
        private String displayPrice;

        @JsonProperty("store_id")
        private String storeId;

        @JsonProperty("store_name")
        private String storeName;

        @JsonProperty("cover_image")
        private String coverImage;

        @JsonProperty("rating")
        private Double rating;

        @JsonProperty("sales_count")
        private Integer salesCount;

        @JsonProperty("city_name")
        private String cityName;

        @JsonProperty("district_name")
        private String districtName;

        @JsonProperty("business_area_name")
        private String businessAreaName;

        @JsonProperty("distance_m")
        private Integer distanceM;

        @JsonProperty("tag_names")
        private List<String> tagNames = new ArrayList<String>();

        @JsonProperty("recommend_score")
        private Double recommendScore;
    }

    @Data
    public static class SearchResultsPayload {
        @JsonProperty("entity_type")
        private String entityType;

        @JsonProperty("items")
        private List<SearchItemPayload> items = new ArrayList<SearchItemPayload>();

        @JsonProperty("total")
        private Integer total = 0;

        @JsonProperty("returned")
        private Integer returned = 0;

        @JsonProperty("source")
        private String source;

        @JsonProperty("query_snapshot_version")
        private String querySnapshotVersion;

        @JsonProperty("search_status")
        private String searchStatus = "not_started";
    }

    @Data
    public static class KnowledgeDocPayload {
        @JsonProperty("id")
        private String id;

        @JsonProperty("category")
        private String category;

        @JsonProperty("title")
        private String title;

        @JsonProperty("content")
        private String content;
    }

    @Data
    public static class RetrievedDocsPayload {
        @JsonProperty("items")
        private List<KnowledgeDocPayload> items = new ArrayList<KnowledgeDocPayload>();

        @JsonProperty("doc_count")
        private Integer docCount = 0;

        @JsonProperty("retrieval_status")
        private String retrievalStatus = "not_started";

        @JsonProperty("query_version")
        private String queryVersion;
    }

    @Data
    public static class RealtimeEntityPayload {
        @JsonProperty("entity_id")
        private String entityId;

        @JsonProperty("product_id")
        private String productId;

        @JsonProperty("status_text")
        private String statusText;

        @JsonProperty("confirm_level")
        private String confirmLevel;

        @JsonProperty("raw_payload")
        private Map<String, Object> rawPayload = new LinkedHashMap<String, Object>();
    }

    @Data
    public static class RealtimeResultsPayload {
        @JsonProperty("items")
        private List<RealtimeEntityPayload> items = new ArrayList<RealtimeEntityPayload>();

        @JsonProperty("checked_entity_count")
        private Integer checkedEntityCount = 0;

        @JsonProperty("realtime_status")
        private String realtimeStatus = "not_started";

        @JsonProperty("query_ts")
        private String queryTs;
    }

    @Data
    public static class ErrorStatePayload {
        @JsonProperty("has_error")
        private Boolean hasError = false;

        @JsonProperty("error_code")
        private String errorCode;

        @JsonProperty("error_message")
        private String errorMessage;

        @JsonProperty("failed_node")
        private String failedNode;

        @JsonProperty("recoverable")
        private Boolean recoverable = true;
    }

    @Data
    public static class ExecutionMetaPayload {
        @JsonProperty("request_id")
        private String requestId;

        @JsonProperty("trace_id")
        private String traceId;

        @JsonProperty("session_id")
        private String sessionId;

        @JsonProperty("degraded")
        private Boolean degraded = false;

        @JsonProperty("restored_from_checkpoint")
        private Boolean restoredFromCheckpoint = false;

        @JsonProperty("fallback_applied")
        private Boolean fallbackApplied = false;

        @JsonProperty("fallback_reason")
        private String fallbackReason;

        @JsonProperty("fallback_source_step")
        private String fallbackSourceStep;

        @JsonProperty("skipped_optional_steps")
        private List<String> skippedOptionalSteps = new ArrayList<String>();
    }

    @Data
    public static class ResponseComposerRequestPayload {
        @JsonProperty("parsed_intent")
        private ParsedIntentPayload parsedIntent;

        @JsonProperty("normalized_params")
        private NormalizedParamsPayload normalizedParams;

        @JsonProperty("search_results")
        private SearchResultsPayload searchResults = new SearchResultsPayload();

        @JsonProperty("retrieved_docs")
        private RetrievedDocsPayload retrievedDocs = new RetrievedDocsPayload();

        @JsonProperty("realtime_results")
        private RealtimeResultsPayload realtimeResults = new RealtimeResultsPayload();

        @JsonProperty("tool_plan")
        private ToolPlanPayload toolPlan;

        @JsonProperty("session_context")
        private SessionContextPayload sessionContext = new SessionContextPayload();

        @JsonProperty("execution_meta")
        private ExecutionMetaPayload executionMeta = new ExecutionMetaPayload();

        @JsonProperty("error_state")
        private ErrorStatePayload errorState = new ErrorStatePayload();
    }

    @Data
    public static class ActionRouteSummaryPayload {
        @JsonProperty("resource")
        private String resource;

        @JsonProperty("action")
        private String action;

        @JsonProperty("operation_type")
        private String operationType;

        @JsonProperty("route_id")
        private Long routeId;

        @JsonProperty("route_description")
        private String routeDescription;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PendingActionPayload {
        @JsonProperty("session_id")
        private String sessionId;

        @JsonProperty("resource")
        private String resource;

        @JsonProperty("action")
        private String action;

        @JsonProperty("operation_type")
        private String operationType;

        @JsonProperty("display_name")
        private String displayName;

        @JsonProperty("route_id")
        private Long routeId;

        @JsonProperty("route_description")
        private String routeDescription;

        @JsonProperty("params")
        private Map<String, Object> params = new LinkedHashMap<String, Object>();

        @JsonProperty("payload")
        private Map<String, Object> payload = new LinkedHashMap<String, Object>();

        @JsonProperty("missing_fields")
        private List<String> missingFields = new ArrayList<String>();

        @JsonProperty("route_keywords")
        private List<String> routeKeywords = new ArrayList<String>();

        @JsonProperty("awaiting_confirmation")
        private Boolean awaitingConfirmation = false;

        @JsonProperty("original_text")
        private String originalText;
    }

    @Data
    public static class ActionReviewRequestPayload {
        @JsonProperty("current_message")
        private String currentMessage;

        @JsonProperty("parsed_intent")
        private ParsedIntentPayload parsedIntent;

        @JsonProperty("pending_action")
        private PendingActionPayload pendingAction;

        @JsonProperty("available_routes")
        private List<ActionRouteSummaryPayload> availableRoutes = new ArrayList<ActionRouteSummaryPayload>();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ActionReviewResponsePayload {
        @JsonProperty("handled")
        private Boolean handled = false;

        @JsonProperty("outcome")
        private String outcome;

        @JsonProperty("pending_action")
        private PendingActionPayload pendingAction;

        @JsonProperty("missing_fields")
        private List<String> missingFields = new ArrayList<String>();

        @JsonProperty("executable_payload")
        private Map<String, Object> executablePayload = new LinkedHashMap<String, Object>();

        @JsonProperty("confirmation_message")
        private String confirmationMessage;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EntityCardPayload {
        @JsonProperty("entity_id")
        private String entityId;

        @JsonProperty("entity_type")
        private String entityType;

        @JsonProperty("title")
        private String title;

        @JsonProperty("subtitle")
        private String subtitle;

        @JsonProperty("image_url")
        private String imageUrl;

        @JsonProperty("price_text")
        private String priceText;

        @JsonProperty("tags")
        private List<String> tags = new ArrayList<String>();

        @JsonProperty("location_text")
        private String locationText;

        @JsonProperty("realtime_status_text")
        private String realtimeStatusText;

        @JsonProperty("recommend_reason")
        private String recommendReason;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CitationPayload {
        @JsonProperty("doc_id")
        private String docId;

        @JsonProperty("doc_title")
        private String docTitle;

        @JsonProperty("snippet")
        private String snippet;

        @JsonProperty("confidence")
        private Double confidence;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FinalAnswerPayload {
        @JsonProperty("answer_type")
        private String answerType;

        @JsonProperty("answer_text")
        private String answerText;

        @JsonProperty("cards")
        private List<EntityCardPayload> cards = new ArrayList<EntityCardPayload>();

        @JsonProperty("disclaimers")
        private List<String> disclaimers = new ArrayList<String>();

        @JsonProperty("citations")
        private List<CitationPayload> citations = new ArrayList<CitationPayload>();

        @JsonProperty("next_actions")
        private List<String> nextActions = new ArrayList<String>();

        @JsonProperty("debug_trace")
        private Map<String, Object> debugTrace = new LinkedHashMap<String, Object>();

        @JsonProperty("summary")
        private String summary;

        @JsonProperty("composer_meta")
        private Map<String, Object> composerMeta = new LinkedHashMap<String, Object>();
    }
}
