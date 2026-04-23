package com.example.demo.demos.Agent.Runtime;

import com.example.demo.demos.Agent.Entity.ApiRoute;
import com.example.demo.demos.Agent.Pojo.AgentChatMessage;
import com.example.demo.demos.Agent.Pojo.AgentChatRequest;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.example.demo.demos.Agent.Service.ApiRouteService;
import com.example.demo.demos.Agent.Service.BackendApiProxyService;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LocalActivityActionAdapter {

    private static final List<String> ACTIVITY_KEYWORDS = Arrays.asList(
            "活动", "同城活动", "本地活动", "最近活动", "附近活动"
    );
    private static final List<String> NEARBY_KEYWORDS = Arrays.asList(
            "附近", "周边", "离我近", "周围"
    );
    private static final List<String> CREATE_KEYWORDS = Arrays.asList(
            "创建", "发布", "发起", "组织", "新增", "办"
    );
    private static final List<String> CONFIRM_KEYWORDS = Arrays.asList(
            "确认", "确认执行", "执行", "提交", "可以创建", "可以发布", "开始创建"
    );
    private static final List<String> CANCEL_KEYWORDS = Arrays.asList(
            "取消", "不用了", "算了", "不查了", "不创建了", "不执行"
    );

    private static final Pattern LAT_LON_LABEL_PATTERN = Pattern.compile(
            "(?:纬度|lat)\\s*[:：]?\\s*(-?\\d+(?:\\.\\d+)?)\\D+(?:经度|lon|lng)\\s*[:：]?\\s*(-?\\d+(?:\\.\\d+)?)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern LAT_LON_INLINE_PATTERN = Pattern.compile(
            "(-?\\d{1,2}\\.\\d{3,})\\s*[,，]\\s*(-?\\d{2,3}\\.\\d{3,})"
    );
    private static final Pattern RADIUS_PATTERN = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*(?:公里|km|KM)"
    );
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "(?:用户名|发布者|组织者)\\s*(?:是|为|:|：)?\\s*([A-Za-z0-9_\\-\\u4e00-\\u9fa5]+)"
    );
    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "(?:活动名称|标题|名称)\\s*(?:是|为|:|：)?\\s*([^,，。；;\\n]+)"
    );
    private static final Pattern CREATE_INLINE_TITLE_PATTERN = Pattern.compile(
            "(?:创建|发布|发起|组织|新增|办)(?:一个|一场|个|场)?\\s*([^,，。；;\\n]+?)(?:活动)?(?:[,，。；;\\n]|$)"
    );
    private static final Pattern CATEGORY_PATTERN = Pattern.compile(
            "(?:分类|类型|活动类型)\\s*(?:是|为|:|：)?\\s*([^,，。；;\\n]+)"
    );
    private static final Pattern DATE_ISO_PATTERN = Pattern.compile(
            "((?:20\\d{2})[-/](?:0?[1-9]|1[0-2])[-/](?:0?[1-9]|[12]\\d|3[01]))"
    );
    private static final Pattern DATE_CN_PATTERN = Pattern.compile(
            "((?:0?[1-9]|1[0-2])月(?:0?[1-9]|[12]\\d|3[01])日)"
    );
    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile(
            "(\\d{1,2})(?::|点)(\\d{2})?\\s*(?:开始)?\\s*(?:到|至|-|~)\\s*(\\d{1,2})(?::|点)(\\d{2})?"
    );
    private static final Pattern TIME_START_PATTERN = Pattern.compile(
            "(?:开始时间|开始)\\s*(?:是|为|:|：)?\\s*(\\d{1,2})(?::|点)(\\d{2})?"
    );
    private static final Pattern TIME_END_PATTERN = Pattern.compile(
            "(?:结束时间|结束)\\s*(?:是|为|:|：)?\\s*(\\d{1,2})(?::|点)(\\d{2})?"
    );
    private static final Pattern LOCATION_PATTERN = Pattern.compile(
            "(?:地点|位置|地址)\\s*(?:是|为|:|：)?\\s*([^,，。；;\\n]+)"
    );
    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile(
            "(?:描述|介绍|内容)\\s*(?:是|为|:|：)?\\s*([^\\n]+)"
    );
    private static final Pattern CAPACITY_PATTERN = Pattern.compile(
            "(?:人数|名额|容量)\\s*(?:是|为|:|：)?\\s*(\\d+)"
    );
    private static final Pattern FEE_PATTERN = Pattern.compile(
            "(?:费用|收费|票价)\\s*(?:是|为|:|：)?\\s*([^,，。；;\\n]+)"
    );

    private final ApiRouteService apiRouteService;
    private final BackendApiProxyService backendApiProxyService;
    private final ActionConversationStore actionConversationStore;

    public LocalActivityActionAdapter(ApiRouteService apiRouteService,
                                      BackendApiProxyService backendApiProxyService,
                                      ActionConversationStore actionConversationStore) {
        this.apiRouteService = apiRouteService;
        this.backendApiProxyService = backendApiProxyService;
        this.actionConversationStore = actionConversationStore;
    }

    public ActionIntentReviewService.ActionReviewResult review(String sessionId,
                                                               AgentChatMessage latestMessage,
                                                               ParsedIntent parsedIntent,
                                                               AgentChatRequest request,
                                                               String authorization,
                                                               ActionConversationStore.PendingAction pendingAction) {
        ActionIntentReviewService.ActionReviewResult result = new ActionIntentReviewService.ActionReviewResult();
        String text = latestMessage == null ? null : latestMessage.getContent();
        if (!StringUtils.hasText(text)) {
            return result;
        }
        String normalized = text.trim();
        ActionConversationStore.PendingAction working = isLocalActivityPending(pendingAction) ? pendingAction.copy() : null;

        if (working != null && containsAny(normalized, CANCEL_KEYWORDS)) {
            actionConversationStore.clear(sessionId);
            result.setHandled(true);
            result.setOutcome(ActionIntentReviewService.ActionOutcome.CANCELLED);
            result.setPendingAction(working);
            return result;
        }

        ActivityMode mode = determineMode(normalized, parsedIntent, working);
        if (mode == ActivityMode.NONE) {
            return result;
        }

        if (working == null) {
            working = createPendingAction(sessionId, mode);
        }

        ApiRoute route = selectRoute(mode);
        if (route == null) {
            result.setHandled(true);
            result.setOutcome(ActionIntentReviewService.ActionOutcome.FAILED);
            result.setPendingAction(working);
            result.setInvocationResult(buildMessageResult(500, "当前未配置可用的活动接口"));
            return result;
        }
        working.setRouteId(route.getId());
        working.setAction(route.getAction());
        working.setOperationType(route.getOperationType());
        working.setRouteDescription(route.getDescription());

        if (mode == ActivityMode.CREATE) {
            mergeCreatePayload(working, normalized, request);
            recomputeCreateMissingFields(working);
            result.setHandled(true);
            result.setPendingAction(working.copy());

            if (!CollectionUtils.isEmpty(working.getMissingFields())) {
                working.setAwaitingConfirmation(false);
                actionConversationStore.put(sessionId, working);
                result.setOutcome(ActionIntentReviewService.ActionOutcome.NEED_CLARIFICATION);
                return result;
            }

            if (containsAny(normalized, CONFIRM_KEYWORDS)) {
                BackendApiProxyService.InvocationResult invocationResult = invoke(working, authorization);
                actionConversationStore.clear(sessionId);
                result.setInvocationResult(summarizeCreateResult(invocationResult));
                result.setOutcome(isSuccess(invocationResult)
                        ? ActionIntentReviewService.ActionOutcome.EXECUTED
                        : ActionIntentReviewService.ActionOutcome.FAILED);
                return result;
            }

            working.setAwaitingConfirmation(true);
            actionConversationStore.put(sessionId, working);
            result.setOutcome(ActionIntentReviewService.ActionOutcome.NEED_CONFIRMATION);
            return result;
        }

        if (mode == ActivityMode.NEARBY) {
            Coordinates coordinates = resolveCoordinates(normalized, request);
            if (coordinates == null) {
                working.setMissingFields(Collections.singletonList("当前位置坐标"));
                actionConversationStore.put(sessionId, working);
                result.setHandled(true);
                result.setOutcome(ActionIntentReviewService.ActionOutcome.NEED_CLARIFICATION);
                result.setPendingAction(working);
                return result;
            }
            working.getParams().put("lat", coordinates.lat);
            working.getParams().put("lon", coordinates.lon);
            working.getParams().put("radiusKm", extractRadiusKm(normalized));
            working.getParams().put("size", 10);
            working.setMissingFields(Collections.<String>emptyList());
            BackendApiProxyService.InvocationResult invocationResult = invoke(working, authorization);
            actionConversationStore.clear(sessionId);
            result.setHandled(true);
            result.setPendingAction(working);
            result.setInvocationResult(summarizeNearbyResult(invocationResult));
            result.setOutcome(isSuccess(invocationResult)
                    ? ActionIntentReviewService.ActionOutcome.EXECUTED
                    : ActionIntentReviewService.ActionOutcome.FAILED);
            return result;
        }

        working.getParams().put("status", "PUBLISHED");
        working.getParams().put("page", 1);
        working.getParams().put("size", 10);
        working.setMissingFields(Collections.<String>emptyList());
        BackendApiProxyService.InvocationResult invocationResult = invoke(working, authorization);
        result.setHandled(true);
        result.setPendingAction(working);
        result.setInvocationResult(summarizeListResult(invocationResult));
        result.setOutcome(isSuccess(invocationResult)
                ? ActionIntentReviewService.ActionOutcome.EXECUTED
                : ActionIntentReviewService.ActionOutcome.FAILED);
        return result;
    }

    private boolean isLocalActivityPending(ActionConversationStore.PendingAction pendingAction) {
        return pendingAction != null && "local_activity".equalsIgnoreCase(pendingAction.getResource());
    }

    private ActivityMode determineMode(String text,
                                       ParsedIntent parsedIntent,
                                       ActionConversationStore.PendingAction pendingAction) {
        if (isLocalActivityPending(pendingAction)) {
            if ("create".equalsIgnoreCase(pendingAction.getAction())) {
                return ActivityMode.CREATE;
            }
            if ("nearby".equalsIgnoreCase(pendingAction.getAction())) {
                return ActivityMode.NEARBY;
            }
            if ("list".equalsIgnoreCase(pendingAction.getAction())) {
                return ActivityMode.LIST;
            }
        }
        if (looksLikeCreateActivity(text)) {
            return ActivityMode.CREATE;
        }
        if (!looksLikeActivityQuery(text, parsedIntent)) {
            return ActivityMode.NONE;
        }
        return containsAny(text, NEARBY_KEYWORDS) ? ActivityMode.NEARBY : ActivityMode.LIST;
    }

    private boolean looksLikeCreateActivity(String text) {
        return containsAny(text, CREATE_KEYWORDS) && containsAny(text, ACTIVITY_KEYWORDS);
    }

    private boolean looksLikeActivityQuery(String text, ParsedIntent parsedIntent) {
        if (containsAny(text, ACTIVITY_KEYWORDS)) {
            return true;
        }
        return parsedIntent != null
                && parsedIntent.getTaskType() != null
                && "event_search".equalsIgnoreCase(parsedIntent.getTaskType().getCode());
    }

    private ActionConversationStore.PendingAction createPendingAction(String sessionId, ActivityMode mode) {
        ActionConversationStore.PendingAction pendingAction = new ActionConversationStore.PendingAction();
        pendingAction.setSessionId(sessionId);
        pendingAction.setResource("local_activity");
        pendingAction.setOperationType(mode == ActivityMode.CREATE ? "CREATE" : "READ");
        pendingAction.setAction(mode == ActivityMode.NEARBY ? "nearby" : mode == ActivityMode.LIST ? "list" : "create");
        pendingAction.setDisplayName(mode == ActivityMode.NEARBY ? "查询附近活动"
                : mode == ActivityMode.LIST ? "查询活动" : "创建活动");
        pendingAction.setRouteKeywords(mode == ActivityMode.CREATE
                ? Arrays.asList("活动", "创建", "发布")
                : Arrays.asList("活动", mode == ActivityMode.NEARBY ? "附近" : "列表"));
        return pendingAction;
    }

    private ApiRoute selectRoute(ActivityMode mode) {
        String operationType = mode == ActivityMode.CREATE ? "CREATE" : "READ";
        List<ApiRoute> routes = apiRouteService.listEnabledRoutes("local_activity", operationType);
        if (CollectionUtils.isEmpty(routes)) {
            return null;
        }
        List<String> preferredActions = preferredActions(mode);
        for (ApiRoute route : routes) {
            if (route != null && preferredActions.contains(route.getAction().toLowerCase(Locale.ROOT))) {
                return route;
            }
        }
        return null;
    }

    private List<String> preferredActions(ActivityMode mode) {
        if (mode == ActivityMode.CREATE) {
            return Arrays.asList("create", "create_activity");
        }
        if (mode == ActivityMode.NEARBY) {
            return Arrays.asList("nearby", "list_nearby");
        }
        if (mode == ActivityMode.LIST) {
            return Arrays.asList("list", "list_activities");
        }
        return Collections.emptyList();
    }

    private void mergeCreatePayload(ActionConversationStore.PendingAction pendingAction,
                                    String text,
                                    AgentChatRequest request) {
        Map<String, Object> payload = pendingAction.getPayload();
        putIfAbsent(payload, "username", extractUsername(text, request));
        putIfAbsent(payload, "title", extractTitle(text));
        putIfAbsent(payload, "category", extractCategory(text, payload));
        putIfAbsent(payload, "date", resolveCreateDate(text));
        String normalizedDate = normalizeDateValue(stringValue(payload.get("date")));
        if (StringUtils.hasText(normalizedDate)) {
            payload.put("date", normalizedDate);
        }
        TimeRange timeRange = extractTimeRange(text);
        if (timeRange != null) {
            putIfAbsent(payload, "timeStart", timeRange.start);
            putIfAbsent(payload, "timeEnd", timeRange.end);
        }
        putIfAbsent(payload, "location", extractLocation(text));
        putIfAbsent(payload, "description", extractDescription(text));
        if (!payload.containsKey("capacity")) {
            Integer capacity = extractCapacity(text);
            if (capacity != null) {
                payload.put("capacity", capacity);
            }
        }
        putIfAbsent(payload, "fee", extractFee(text));
        payload.put("status", "PUBLISHED");
    }

    private void recomputeCreateMissingFields(ActionConversationStore.PendingAction pendingAction) {
        List<String> missing = new ArrayList<String>();
        Map<String, Object> payload = pendingAction.getPayload();
        require(payload, "username", "发布用户名", missing);
        require(payload, "title", "活动名称", missing);
        require(payload, "category", "活动分类", missing);
        require(payload, "date", "活动日期", missing);
        require(payload, "timeStart", "开始时间", missing);
        require(payload, "timeEnd", "结束时间", missing);
        require(payload, "location", "活动地点", missing);
        require(payload, "description", "活动描述", missing);
        pendingAction.setMissingFields(missing);
    }

    private String extractUsername(String text, AgentChatRequest request) {
        Matcher matcher = USERNAME_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        if (request == null || request.getUserProfile() == null) {
            return null;
        }
        Object username = request.getUserProfile().get("username");
        if (username == null) {
            username = request.getUserProfile().get("userName");
        }
        return username == null ? null : String.valueOf(username).trim();
    }

    private String extractTitle(String text) {
        Matcher matcher = TITLE_PATTERN.matcher(text);
        if (matcher.find()) {
            return cleanTitle(matcher.group(1));
        }
        matcher = CREATE_INLINE_TITLE_PATTERN.matcher(text);
        if (matcher.find()) {
            return cleanTitle(matcher.group(1));
        }
        return null;
    }

    private String cleanTitle(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String cleaned = raw.trim()
                .replace("帮我", "")
                .replace("请帮我", "")
                .trim();
        if ("活动".equals(cleaned)) {
            return null;
        }
        return cleaned;
    }

    private String extractCategory(String text, Map<String, Object> payload) {
        String existing = stringValue(payload.get("category"));
        if (StringUtils.hasText(existing)) {
            return existing;
        }
        Matcher matcher = CATEGORY_PATTERN.matcher(text);
        if (matcher.find()) {
            return normalizeCategoryCode(matcher.group(1));
        }
        return inferCategory(text);
    }

    private String inferCategory(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, Arrays.asList("羽毛球", "足球", "篮球", "跑步", "运动", "骑行"))) {
            return "sport";
        }
        if (containsAny(normalized, Arrays.asList("音乐", "乐队", "演唱", "演出"))) {
            return "music";
        }
        if (containsAny(normalized, Arrays.asList("市集", "集市", "摆摊", "跳蚤"))) {
            return "market";
        }
        if (containsAny(normalized, Arrays.asList("美食", "餐", "品鉴", "吃"))) {
            return "food";
        }
        return null;
    }

    private String normalizeCategoryCode(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("运动") || "sport".equals(normalized)) {
            return "sport";
        }
        if (normalized.contains("音乐") || "music".equals(normalized)) {
            return "music";
        }
        if (normalized.contains("市集") || normalized.contains("集市") || "market".equals(normalized)) {
            return "market";
        }
        if (normalized.contains("美食") || "food".equals(normalized)) {
            return "food";
        }
        return raw.trim();
    }

    private String extractDate(String text) {
        Matcher matcher = DATE_ISO_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replace('/', '-');
        }
        matcher = DATE_CN_PATTERN.matcher(text);
        if (matcher.find()) {
            String value = matcher.group(1).replace("日", "");
            String[] parts = value.split("月");
            if (parts.length == 2) {
                int year = Year.now().getValue();
                return String.format(Locale.ROOT, "%d-%02d-%02d", year, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            }
        }
        return null;
    }

    private String resolveCreateDate(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher isoMatcher = Pattern.compile("(20\\d{2})[-/](1[0-2]|0?[1-9])[-/](3[01]|[12]\\d|0?[1-9])(?!\\d)").matcher(text);
        if (isoMatcher.find()) {
            return String.format(Locale.ROOT,
                    "%s-%02d-%02d",
                    isoMatcher.group(1),
                    Integer.parseInt(isoMatcher.group(2)),
                    Integer.parseInt(isoMatcher.group(3)));
        }
        Matcher cnMatcher = Pattern.compile("(0?[1-9]|1[0-2])月(0?[1-9]|[12]\\d|3[01])日").matcher(text);
        if (cnMatcher.find()) {
            return String.format(Locale.ROOT,
                    "%d-%02d-%02d",
                    Year.now().getValue(),
                    Integer.parseInt(cnMatcher.group(1)),
                    Integer.parseInt(cnMatcher.group(2)));
        }
        return extractDate(text);
    }

    private String normalizeDateValue(String rawDate) {
        if (!StringUtils.hasText(rawDate)) {
            return null;
        }
        Matcher matcher = Pattern.compile("(20\\d{2})-(\\d{1,2})-(\\d{1,2})").matcher(rawDate.trim());
        if (matcher.matches()) {
            return String.format(Locale.ROOT,
                    "%s-%02d-%02d",
                    matcher.group(1),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)));
        }
        return rawDate;
    }

    private TimeRange extractTimeRange(String text) {
        Matcher matcher = TIME_RANGE_PATTERN.matcher(text);
        if (matcher.find()) {
            return new TimeRange(formatTime(matcher.group(1), matcher.group(2)), formatTime(matcher.group(3), matcher.group(4)));
        }
        Matcher startMatcher = TIME_START_PATTERN.matcher(text);
        Matcher endMatcher = TIME_END_PATTERN.matcher(text);
        if (startMatcher.find() && endMatcher.find()) {
            return new TimeRange(
                    formatTime(startMatcher.group(1), startMatcher.group(2)),
                    formatTime(endMatcher.group(1), endMatcher.group(2))
            );
        }
        return null;
    }

    private String formatTime(String hourText, String minuteText) {
        int hour = Integer.parseInt(hourText);
        int minute = StringUtils.hasText(minuteText) ? Integer.parseInt(minuteText) : 0;
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
    }

    private String extractLocation(String text) {
        Matcher matcher = LOCATION_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String extractDescription(String text) {
        Matcher matcher = DESCRIPTION_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private Integer extractCapacity(String text) {
        Matcher matcher = CAPACITY_PATTERN.matcher(text);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(1));
        }
        return null;
    }

    private String extractFee(String text) {
        Matcher matcher = FEE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        if (text.contains("免费")) {
            return "免费";
        }
        if (text.toUpperCase(Locale.ROOT).contains("AA")) {
            return "AA";
        }
        return null;
    }

    private Coordinates resolveCoordinates(String text, AgentChatRequest request) {
        Coordinates coordinates = extractCoordinatesFromText(text);
        if (coordinates != null) {
            return coordinates;
        }
        Map<String, Object> userProfile = request == null ? null : request.getUserProfile();
        if (userProfile == null || userProfile.isEmpty()) {
            return null;
        }
        Double lat = toDouble(userProfile.get("lat"));
        if (lat == null) {
            lat = toDouble(userProfile.get("latitude"));
        }
        Double lon = toDouble(userProfile.get("lon"));
        if (lon == null) {
            lon = toDouble(userProfile.get("lng"));
        }
        if (lon == null) {
            lon = toDouble(userProfile.get("longitude"));
        }
        return lat == null || lon == null ? null : new Coordinates(lat, lon);
    }

    private Coordinates extractCoordinatesFromText(String text) {
        Matcher labelMatcher = LAT_LON_LABEL_PATTERN.matcher(text);
        if (labelMatcher.find()) {
            return new Coordinates(toDouble(labelMatcher.group(1)), toDouble(labelMatcher.group(2)));
        }
        Matcher inlineMatcher = LAT_LON_INLINE_PATTERN.matcher(text);
        if (inlineMatcher.find()) {
            return new Coordinates(toDouble(inlineMatcher.group(1)), toDouble(inlineMatcher.group(2)));
        }
        return null;
    }

    private double extractRadiusKm(String text) {
        Matcher matcher = RADIUS_PATTERN.matcher(text);
        if (matcher.find()) {
            Double value = toDouble(matcher.group(1));
            if (value != null && value > 0D) {
                return value;
            }
        }
        return 20D;
    }

    private BackendApiProxyService.InvocationResult invoke(ActionConversationStore.PendingAction pendingAction,
                                                           String authorization) {
        BackendApiProxyService.InvocationRequest request = new BackendApiProxyService.InvocationRequest();
        request.setResource(pendingAction.getResource());
        request.setAction(pendingAction.getAction());
        request.setAuthorization(authorization);
        if (!CollectionUtils.isEmpty(pendingAction.getParams())) {
            request.setParams(new LinkedHashMap<String, Object>(pendingAction.getParams()));
        }
        if (!CollectionUtils.isEmpty(pendingAction.getPayload())) {
            request.setPayload(new LinkedHashMap<String, Object>(pendingAction.getPayload()));
        }
        return backendApiProxyService.invoke(request, authorization);
    }

    private BackendApiProxyService.InvocationResult summarizeCreateResult(BackendApiProxyService.InvocationResult rawResult) {
        if (!isSuccess(rawResult)) {
            return rawResult;
        }
        Map<String, Object> body = bodyMap(rawResult);
        Map<String, Object> data = nestedMap(body == null ? null : body.get("data"));
        String activityId = data == null ? null : stringValue(data.get("activityId"));
        String status = data == null ? null : normalizeStatus(stringValue(data.get("status")));
        String message = "活动已创建";
        if (StringUtils.hasText(activityId)) {
            message += "，activityId=" + activityId;
        }
        if (StringUtils.hasText(status)) {
            message += "，状态=" + status;
        }
        return buildMessageResult(200, message, data);
    }

    private BackendApiProxyService.InvocationResult summarizeListResult(BackendApiProxyService.InvocationResult rawResult) {
        if (!isSuccess(rawResult)) {
            return rawResult;
        }
        List<Map<String, Object>> items = extractDataItems(rawResult);
        String summary;
        if (items.isEmpty()) {
            summary = "当前没有命中的活动。";
        } else {
            List<String> parts = new ArrayList<String>();
            int limit = Math.min(items.size(), 3);
            for (int i = 0; i < limit; i++) {
                Map<String, Object> item = items.get(i);
                String title = displayTitle(item);
                String location = displayLocation(item);
                String status = normalizeStatus(stringValue(item.get("status")));
                parts.add(title + formatSuffix(location, status));
            }
            summary = "当前有 " + items.size() + " 个活动，前几个是：" + String.join("；", parts) + "。";
        }
        return buildMessageResult(200, summary, items);
    }

    private BackendApiProxyService.InvocationResult summarizeNearbyResult(BackendApiProxyService.InvocationResult rawResult) {
        if (!isSuccess(rawResult)) {
            return rawResult;
        }
        List<Map<String, Object>> items = extractDataItems(rawResult);
        String summary;
        if (items.isEmpty()) {
            summary = "你附近暂时没有命中的活动。";
        } else {
            List<String> parts = new ArrayList<String>();
            int limit = Math.min(items.size(), 3);
            for (int i = 0; i < limit; i++) {
                Map<String, Object> item = items.get(i);
                String title = displayTitle(item);
                String distance = formatDistance(item.get("distanceKm"));
                String location = displayLocation(item);
                parts.add(title + formatSuffix(location, distance));
            }
            summary = "你附近找到 " + items.size() + " 个活动，前几个是：" + String.join("；", parts) + "。";
        }
        return buildMessageResult(200, summary, items);
    }

    private BackendApiProxyService.InvocationResult buildMessageResult(int code, String message) {
        return buildMessageResult(code, message, null);
    }

    private BackendApiProxyService.InvocationResult buildMessageResult(int code, String message, Object data) {
        BackendApiProxyService.InvocationResult result = new BackendApiProxyService.InvocationResult();
        result.setPresentationHint("direct_response");
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("code", code);
        payload.put("message", message);
        if (data != null) {
            payload.put("data", data);
        }
        result.setData(payload);
        return result;
    }

    private boolean isSuccess(BackendApiProxyService.InvocationResult result) {
        if (result == null || !"direct_response".equals(result.getPresentationHint())) {
            return false;
        }
        Map<String, Object> body = bodyMap(result);
        if (body == null) {
            return true;
        }
        Object code = body.get("code");
        if (code instanceof Number) {
            return ((Number) code).intValue() == 200;
        }
        return code == null || "200".equals(String.valueOf(code));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> bodyMap(BackendApiProxyService.InvocationResult result) {
        if (result == null || !(result.getData() instanceof Map)) {
            return null;
        }
        return (Map<String, Object>) result.getData();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractDataItems(BackendApiProxyService.InvocationResult result) {
        Map<String, Object> body = bodyMap(result);
        if (body == null) {
            return Collections.emptyList();
        }
        Object data = body.get("data");
        if (!(data instanceof List)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (Object item : (List<?>) data) {
            if (item instanceof Map) {
                items.add((Map<String, Object>) item);
            }
        }
        return items;
    }

    private String formatDistance(Object value) {
        Double distance = toDouble(value);
        if (distance == null) {
            return null;
        }
        return distance <= 1D
                ? new BigDecimal(distance).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "km"
                : new BigDecimal(distance).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "km";
    }

    private String formatSuffix(String first, String second) {
        List<String> parts = new ArrayList<String>();
        if (StringUtils.hasText(first)) {
            parts.add(first);
        }
        if (StringUtils.hasText(second)) {
            parts.add(second);
        }
        return parts.isEmpty() ? "" : "（" + String.join("，", parts) + "）";
    }

    private String displayTitle(Map<String, Object> item) {
        String title = firstNonBlank(item.get("title"), item.get("subtitle"));
        if (!looksPlaceholderText(title)) {
            return title;
        }
        String category = normalizeCategory(firstNonBlank(item.get("category"), item.get("categoryCode")));
        String id = stringValue(item.get("id"));
        if (!looksPlaceholderText(category) && StringUtils.hasText(id)) {
            return category + "活动#" + id;
        }
        if (StringUtils.hasText(id)) {
            return "活动#" + id;
        }
        return "活动";
    }

    private String displayLocation(Map<String, Object> item) {
        return firstNonBlank(
                item.get("locationText"),
                firstNonBlank(item.get("location"), item.get("address"))
        );
    }

    private boolean looksPlaceholderText(String text) {
        if (!StringUtils.hasText(text)) {
            return true;
        }
        String normalized = text.trim();
        if ("null".equalsIgnoreCase(normalized) || "undefined".equalsIgnoreCase(normalized)) {
            return true;
        }
        return normalized.matches("\\d{1,3}");
    }

    private String normalizeCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return category;
        }
        String normalized = category.trim().toLowerCase(Locale.ROOT);
        if ("sport".equals(normalized)) {
            return "运动";
        }
        if ("market".equals(normalized)) {
            return "市集";
        }
        if ("music".equals(normalized)) {
            return "音乐";
        }
        if ("food".equals(normalized)) {
            return "美食";
        }
        return category.trim();
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return status;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("PUBLISHED".equals(normalized)) {
            return "已发布";
        }
        if ("DRAFT".equals(normalized)) {
            return "草稿";
        }
        if ("CANCELLED".equals(normalized)) {
            return "已取消";
        }
        return status.trim();
    }

    private void putIfAbsent(Map<String, Object> target, String key, Object value) {
        if (target.containsKey(key)) {
            Object existing = target.get(key);
            if (existing instanceof String && !StringUtils.hasText((String) existing) && value != null) {
                target.put(key, value);
            }
            return;
        }
        if (value != null && (!(value instanceof String) || StringUtils.hasText((String) value))) {
            target.put(key, value);
        }
    }

    private void require(Map<String, Object> payload, String key, String label, List<String> missing) {
        Object value = payload.get(key);
        if (value == null) {
            missing.add(label);
            return;
        }
        if (value instanceof String && !StringUtils.hasText((String) value)) {
            missing.add(label);
        }
    }

    private String firstNonBlank(Object first, Object second) {
        String firstText = stringValue(first);
        if (StringUtils.hasText(firstText)) {
            return firstText;
        }
        return stringValue(second);
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return StringUtils.hasText(text) ? text : null;
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean containsAny(String text, List<String> keywords) {
        if (!StringUtils.hasText(text) || CollectionUtils.isEmpty(keywords)) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private enum ActivityMode {
        NONE,
        LIST,
        NEARBY,
        CREATE
    }

    private static class Coordinates {
        private final Double lat;
        private final Double lon;

        private Coordinates(Double lat, Double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }

    private static class TimeRange {
        private final String start;
        private final String end;

        private TimeRange(String start, String end) {
            this.start = start;
            this.end = end;
        }
    }
}
