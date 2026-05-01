package com.example.demo.demos.Agent.Runtime;

import com.example.demo.demos.Agent.Entity.ApiRoute;
import com.example.demo.demos.Agent.Pojo.AgentChatMessage;
import com.example.demo.demos.Agent.Pojo.AgentChatRequest;
import com.example.demo.demos.Agent.Pojo.CandidateSlots;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RouteParamBuilder {

    private static final Pattern LAT_LON_LABEL_PATTERN = Pattern.compile(
            "(?:纬度|lat)\\s*[:：]?\\s*(-?\\d+(?:\\.\\d+)?)\\D+(?:经度|lon|lng)\\s*[:：]?\\s*(-?\\d+(?:\\.\\d+)?)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern LAT_LON_INLINE_PATTERN = Pattern.compile(
            "(-?\\d{1,2}\\.\\d{3,})\\s*[,，]\\s*(-?\\d{2,3}\\.\\d{3,})"
    );
    private static final Pattern RADIUS_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:公里|km|KM)");
    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");

    private final ObjectMapper objectMapper = new ObjectMapper();

    public BuildResult build(ApiRoute route,
                             AgentChatMessage latestMessage,
                             ParsedIntent parsedIntent,
                             AgentChatRequest request) {
        BuildResult result = new BuildResult();
        applySchemaDefaults(result.getParams(), route == null ? null : route.getQuerySchema());
        applySchemaDefaults(result.getPayload(), route == null ? null : route.getBodySchema());

        CandidateSlots slots = parsedIntent == null || parsedIntent.getCandidateSlots() == null
                ? new CandidateSlots() : parsedIntent.getCandidateSlots();
        String text = latestMessage == null ? null : latestMessage.getContent();
        String action = route == null ? null : route.getAction();
        String entityType = route == null ? null : route.getEntityType();

        if (isReadRoute(route)) {
            result.getParams().putIfAbsent("page", 1);
            result.getParams().putIfAbsent("size", 10);
        }
        if (isActivityRoute(route)) {
            result.getParams().putIfAbsent("status", "PUBLISHED");
        }
        if (StringUtils.hasText(slots.getKeyword())) {
            result.getParams().putIfAbsent("keyword", slots.getKeyword());
        }
        if (StringUtils.hasText(slots.getCategoryText())) {
            result.getParams().putIfAbsent("category", slots.getCategoryText());
            result.getParams().putIfAbsent("categoryText", slots.getCategoryText());
        }
        if (StringUtils.hasText(slots.getCityText())) {
            result.getParams().putIfAbsent("city", slots.getCityText());
        }
        if (StringUtils.hasText(slots.getDistrictText())) {
            result.getParams().putIfAbsent("district", slots.getDistrictText());
        }
        if (StringUtils.hasText(slots.getDateText())) {
            result.getParams().putIfAbsent("date", slots.getDateText());
        }
        applyPrice(result.getParams(), slots.getPriceText(), text);

        if (isNearbyAction(action) || "附近".equals(slots.getLocationText())) {
            Coordinates coordinates = resolveCoordinates(text, request);
            if (coordinates == null) {
                result.getMissingFields().add("当前位置坐标");
            } else {
                result.getParams().put("lat", coordinates.lat);
                result.getParams().put("lon", coordinates.lon);
            }
            result.getParams().putIfAbsent("radiusKm", extractRadiusKm(text, request));
        }

        if (StringUtils.hasText(entityType)) {
            result.getParams().putIfAbsent("entityType", entityType);
        }
        removeNullValues(result.getParams());
        removeNullValues(result.getPayload());
        applyRequiredFields(result.getMissingFields(), result.getParams(), route == null ? null : route.getQuerySchema());
        applyRequiredFields(result.getMissingFields(), result.getPayload(), route == null ? null : route.getBodySchema());
        return result;
    }

    private void applySchemaDefaults(Map<String, Object> target, String schema) {
        if (!StringUtils.hasText(schema)) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(schema);
            JsonNode properties = root.has("properties") ? root.get("properties") : root;
            if (properties == null || !properties.isObject()) {
                return;
            }
            java.util.Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode value = entry.getValue();
                if (value == null) {
                    continue;
                }
                if (value.isObject() && value.has("default")) {
                    target.putIfAbsent(entry.getKey(), toJavaValue(value.get("default")));
                } else if (value.isValueNode()) {
                    target.putIfAbsent(entry.getKey(), toJavaValue(value));
                }
            }
        } catch (Exception ignore) {
            // Malformed schema should not block route execution.
        }
    }

    private void applyRequiredFields(List<String> missingFields, Map<String, Object> params, String schema) {
        if (!StringUtils.hasText(schema)) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(schema);
            JsonNode required = root.get("required");
            if (required == null || !required.isArray()) {
                return;
            }
            for (JsonNode item : required) {
                String field = item.asText();
                Object value = params.get(field);
                if (value == null || value instanceof String && !StringUtils.hasText((String) value)) {
                    missingFields.add(field);
                }
            }
        } catch (Exception ignore) {
            // Ignore invalid required schema.
        }
    }

    private Object toJavaValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isInt() || node.isLong()) {
            return node.asLong();
        }
        if (node.isFloat() || node.isDouble() || node.isBigDecimal()) {
            return node.decimalValue();
        }
        return node.asText();
    }

    private boolean isReadRoute(ApiRoute route) {
        if (route == null || !StringUtils.hasText(route.getOperationType())) {
            return false;
        }
        return route.getOperationType().toUpperCase(Locale.ROOT).startsWith("READ");
    }

    private boolean isActivityRoute(ApiRoute route) {
        if (route == null) {
            return false;
        }
        return "event".equalsIgnoreCase(route.getEntityType())
                || "local_activity".equalsIgnoreCase(route.getResource())
                || StringUtils.hasText(route.getResource()) && route.getResource().toLowerCase(Locale.ROOT).contains("activity");
    }

    private boolean isNearbyAction(String action) {
        return StringUtils.hasText(action) && action.toLowerCase(Locale.ROOT).contains("nearby");
    }

    private void applyPrice(Map<String, Object> params, String priceText, String rawText) {
        String candidate = StringUtils.hasText(priceText) ? priceText : rawText;
        if (!StringUtils.hasText(candidate)) {
            return;
        }
        Matcher matcher = PRICE_PATTERN.matcher(candidate);
        List<BigDecimal> values = new ArrayList<BigDecimal>();
        while (matcher.find()) {
            values.add(new BigDecimal(matcher.group(1)));
        }
        if (values.isEmpty()) {
            return;
        }
        String lowered = candidate.toLowerCase(Locale.ROOT);
        if (lowered.contains("以内") || lowered.contains("以下") || lowered.contains("under") || lowered.contains("below")) {
            params.putIfAbsent("maxPrice", values.get(0));
        } else if (lowered.contains("以上") || lowered.contains("不少于") || lowered.contains("over") || lowered.contains("above")) {
            params.putIfAbsent("minPrice", values.get(0));
        } else if (values.size() >= 2) {
            params.putIfAbsent("minPrice", values.get(0));
            params.putIfAbsent("maxPrice", values.get(1));
        }
    }

    private Coordinates resolveCoordinates(String text, AgentChatRequest request) {
        Coordinates fromText = extractCoordinatesFromText(text);
        if (fromText != null) {
            return fromText;
        }
        Map<String, Object> profile = request == null ? null : request.getUserProfile();
        if (CollectionUtils.isEmpty(profile)) {
            return null;
        }
        Double lat = toDouble(firstValue(profile, "lat", "latitude"));
        Double lon = toDouble(firstValue(profile, "lon", "lng", "longitude"));
        return lat == null || lon == null ? null : new Coordinates(lat, lon);
    }

    private Coordinates extractCoordinatesFromText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
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

    private Object firstValue(Map<String, Object> map, String... keys) {
        if (map == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    private Double extractRadiusKm(String text, AgentChatRequest request) {
        if (StringUtils.hasText(text)) {
            Matcher matcher = RADIUS_PATTERN.matcher(text);
            if (matcher.find()) {
                Double value = toDouble(matcher.group(1));
                if (value != null && value > 0D) {
                    return value;
                }
            }
        }
        Object profileRadius = request == null ? null : firstValue(request.getUserProfile(), "nearbyRadiusKm", "radiusKm");
        Double radius = toDouble(profileRadius);
        return radius == null || radius <= 0D ? 20D : radius;
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

    private void removeNullValues(Map<String, Object> values) {
        if (values == null) {
            return;
        }
        List<String> nullKeys = new ArrayList<String>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getValue() == null) {
                nullKeys.add(entry.getKey());
            }
        }
        for (String key : nullKeys) {
            values.remove(key);
        }
    }

    @Data
    public static class BuildResult {
        private Map<String, Object> params = new LinkedHashMap<String, Object>();
        private Map<String, Object> payload = new LinkedHashMap<String, Object>();
        private Map<String, Object> pathVariables = new LinkedHashMap<String, Object>();
        private List<String> missingFields = new ArrayList<String>();
    }

    private static class Coordinates {
        private final Double lat;
        private final Double lon;

        private Coordinates(Double lat, Double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }
}
