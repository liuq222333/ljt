package com.example.demo.demos.Agent.Runtime.adapter;

import com.example.demo.demos.Agent.Entity.ApiRoute;
import com.example.demo.demos.Agent.Runtime.NormalizedRouteData;
import com.example.demo.demos.Agent.Runtime.RouteEntityCandidate;
import com.example.demo.demos.Agent.Service.BackendApiProxyService;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

abstract class AbstractRouteResultAdapter implements RouteResultAdapter {

    @Override
    public NormalizedRouteData adapt(ApiRoute route, BackendApiProxyService.InvocationResult result) {
        NormalizedRouteData data = new NormalizedRouteData();
        data.setEntityType(resolveEntityType(route));
        data.setSourceRouteId(route == null || route.getId() == null ? null : String.valueOf(route.getId()));
        data.setSourceResource(route == null ? null : route.getResource());
        data.setSourceAction(route == null ? null : route.getAction());
        data.setPresentationHint(route == null ? null : route.getPresentationHint());
        Map<String, Object> body = bodyMap(result);
        data.setRawStatus(resolveStatus(body));
        data.getRawMeta().put("presentationHint", result == null ? null : result.getPresentationHint());
        data.getRawMeta().put("body", body == null ? result == null ? null : result.getData() : body);
        if (result == null || !"direct_response".equals(result.getPresentationHint())) {
            data.setDegraded(true);
            data.setErrorMessage(extractMessage(body, "backend_route_failed"));
            return data;
        }
        List<Map<String, Object>> items = extractItems(body == null ? result.getData() : body);
        for (Map<String, Object> item : items) {
            RouteEntityCandidate candidate = toCandidate(item, data.getEntityType());
            if (candidate != null) {
                data.getItems().add(candidate);
            }
        }
        data.setTotal(resolveTotal(body, data.getItems().size()));
        return data;
    }

    protected RouteEntityCandidate toCandidate(Map<String, Object> item, String entityType) {
        if (item == null) {
            return null;
        }
        RouteEntityCandidate candidate = new RouteEntityCandidate();
        candidate.setEntityId(firstNonBlank(
                item.get("id"),
                item.get("entityId"),
                item.get(entityType + "Id"),
                item.get("activityId"),
                item.get("storeId")
        ));
        candidate.setTitle(displayTitle(item, entityType));
        candidate.setSubtitle(firstNonBlank(item.get("subtitle"), item.get("summary"), item.get("summaryText"), item.get("description")));
        candidate.setImageUrl(resolveImageUrl(item));
        candidate.setLocationText(firstNonBlank(item.get("locationText"), item.get("location"), item.get("address"), item.get("venue")));
        candidate.setPriceText(formatPrice(firstNonBlank(item.get("priceText"), item.get("price"), item.get("fee")), item.get("currency")));
        candidate.setRealtimeStatusText(firstNonBlank(item.get("realtimeStatusText"), item.get("businessStatus"), item.get("statusText")));
        appendIfPresent(candidate.getTags(), normalizeStatus(firstNonBlank(item.get("status"), item.get("state"))));
        appendIfPresent(candidate.getTags(), normalizeCategory(firstNonBlank(item.get("category"), item.get("categoryCode"), item.get("type"))));
        appendIfPresent(candidate.getHighlights(), buildTimeText(item));
        appendIfPresent(candidate.getHighlights(), formatDistance(item.get("distanceKm")));
        candidate.getRaw().putAll(item);
        return candidate;
    }

    protected String resolveEntityType(ApiRoute route) {
        if (route != null && StringUtils.hasText(route.getEntityType())) {
            return route.getEntityType();
        }
        String resource = route == null ? null : route.getResource();
        if (StringUtils.hasText(resource)) {
            String normalized = resource.toLowerCase(Locale.ROOT);
            if (normalized.contains("activity") || normalized.contains("event")) {
                return "event";
            }
            if (normalized.contains("store") || normalized.contains("shop")) {
                return "store";
            }
            if (normalized.contains("product")) {
                return "product";
            }
        }
        return "entity";
    }

    protected String displayTitle(Map<String, Object> item, String entityType) {
        String title = firstNonBlank(item.get("title"), item.get("name"));
        if (!looksPlaceholderText(title)) {
            return title;
        }
        String id = firstNonBlank(item.get("id"), item.get("activityId"), item.get("storeId"));
        String prefix = "event".equalsIgnoreCase(entityType) ? "活动"
                : "store".equalsIgnoreCase(entityType) ? "门店" : "结果";
        return StringUtils.hasText(id) ? prefix + "#" + id : prefix;
    }

    protected String resolveImageUrl(Map<String, Object> item) {
        if (item == null) {
            return null;
        }
        String direct = firstNonBlank(
                item.get("imageUrl"),
                item.get("image_url"),
                item.get("coverUrl"),
                item.get("cover_url"),
                item.get("coverImage"),
                item.get("cover_image"),
                item.get("cover"),
                item.get("image"),
                item.get("thumbnailUrl"),
                item.get("thumbnail_url"),
                item.get("thumbnail")
        );
        if (StringUtils.hasText(direct)) {
            return direct;
        }
        return firstImageFrom(
                item.get("imageUrls"),
                item.get("image_urls"),
                item.get("images"),
                item.get("photos"),
                item.get("mediaUrls"),
                item.get("media_urls")
        );
    }

    protected String firstImageFrom(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            String image = firstImageValue(value);
            if (StringUtils.hasText(image)) {
                return image;
            }
        }
        return null;
    }

    protected String firstImageValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Iterable<?>) {
            for (Object item : (Iterable<?>) value) {
                String image = firstNonBlank(item);
                if (StringUtils.hasText(image)) {
                    return cleanImageText(image);
                }
            }
            return null;
        }
        Class<?> valueClass = value.getClass();
        if (valueClass.isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                String image = firstNonBlank(Array.get(value, i));
                if (StringUtils.hasText(image)) {
                    return cleanImageText(image);
                }
            }
            return null;
        }
        String text = firstNonBlank(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        text = cleanImageText(text);
        if (text.startsWith("[") && text.endsWith("]")) {
            String body = text.substring(1, text.length() - 1).trim();
            if (!StringUtils.hasText(body)) {
                return null;
            }
            String[] parts = body.split(",");
            for (String part : parts) {
                String image = cleanImageText(part);
                if (StringUtils.hasText(image)) {
                    return image;
                }
            }
            return null;
        }
        return text;
    }

    protected String cleanImageText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String cleaned = text.trim();
        while (cleaned.startsWith("\"") || cleaned.startsWith("'") || cleaned.startsWith("`")) {
            cleaned = cleaned.substring(1).trim();
        }
        while (cleaned.endsWith("\"") || cleaned.endsWith("'") || cleaned.endsWith("`")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        return firstNonBlank(cleaned);
    }

    protected String buildTimeText(Map<String, Object> item) {
        String date = firstNonBlank(item.get("date"), item.get("activityDate"), item.get("startDate"), item.get("startAt"), item.get("start_at"));
        String start = firstNonBlank(item.get("timeStart"), item.get("startTime"));
        String end = firstNonBlank(item.get("timeEnd"), item.get("endTime"));
        List<String> parts = new ArrayList<String>();
        appendIfPresent(parts, date);
        if (StringUtils.hasText(start) && StringUtils.hasText(end)) {
            parts.add(start + "-" + end);
        } else {
            appendIfPresent(parts, start);
        }
        return parts.isEmpty() ? null : String.join(" ", parts);
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> bodyMap(BackendApiProxyService.InvocationResult result) {
        if (result == null || !(result.getData() instanceof Map)) {
            return null;
        }
        return (Map<String, Object>) result.getData();
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> extractItems(Object bodyOrData) {
        if (bodyOrData == null) {
            return new ArrayList<Map<String, Object>>();
        }
        Object data = bodyOrData;
        if (bodyOrData instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) bodyOrData;
            data = firstExisting(map, "data", "items", "records", "list", "content");
        }
        if (data instanceof Map) {
            Object nested = firstExisting((Map<?, ?>) data, "items", "records", "list", "content");
            if (nested instanceof List) {
                data = nested;
            } else {
                List<Map<String, Object>> singleton = new ArrayList<Map<String, Object>>();
                singleton.add((Map<String, Object>) data);
                return singleton;
            }
        }
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        if (data instanceof List<?>) {
            for (Object item : (List<?>) data) {
                if (item instanceof Map) {
                    items.add((Map<String, Object>) item);
                }
            }
        }
        return items;
    }

    protected long resolveTotal(Map<String, Object> body, int itemCount) {
        if (body != null) {
            Object total = firstExisting(body, "total", "count", "totalElements");
            Long parsed = toLong(total);
            if (parsed != null) {
                return parsed;
            }
            Object data = body.get("data");
            if (data instanceof Map) {
                Long nested = toLong(firstExisting((Map<?, ?>) data, "total", "count", "totalElements"));
                if (nested != null) {
                    return nested;
                }
            }
        }
        return itemCount;
    }

    protected String resolveStatus(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        return firstNonBlank(body.get("code"), body.get("status"));
    }

    protected String extractMessage(Map<String, Object> body, String fallback) {
        String message = body == null ? null : firstNonBlank(body.get("message"), body.get("msg"), body.get("error"));
        return StringUtils.hasText(message) ? message : fallback;
    }

    protected Object firstExisting(Map<?, ?> map, String... keys) {
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

    protected String firstNonBlank(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (StringUtils.hasText(text) && !"null".equalsIgnoreCase(text) && !"undefined".equalsIgnoreCase(text)) {
                return text;
            }
        }
        return null;
    }

    protected void appendIfPresent(List<String> target, String value) {
        if (target != null && StringUtils.hasText(value) && !target.contains(value)) {
            target.add(value);
        }
    }

    protected boolean looksPlaceholderText(String text) {
        if (!StringUtils.hasText(text)) {
            return true;
        }
        String normalized = text.trim();
        return "null".equalsIgnoreCase(normalized)
                || "undefined".equalsIgnoreCase(normalized)
                || normalized.matches("\\d{1,3}");
    }

    protected String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
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
        if ("OPEN".equals(normalized) || "OPENING".equals(normalized)) {
            return "营业中";
        }
        if ("CLOSED".equals(normalized)) {
            return "已休息";
        }
        return status.trim();
    }

    protected String normalizeCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return null;
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

    protected String formatDistance(Object value) {
        Double distance = toDouble(value);
        if (distance == null) {
            return null;
        }
        BigDecimal decimal = new BigDecimal(distance).setScale(distance <= 1D ? 2 : 1, RoundingMode.HALF_UP);
        return "距离 " + decimal.stripTrailingZeros().toPlainString() + "km";
    }

    protected String formatPrice(String price, Object currency) {
        if (!StringUtils.hasText(price)) {
            return null;
        }
        if (price.contains("元") || price.toUpperCase(Locale.ROOT).contains("CNY") || price.contains("免费")) {
            return price;
        }
        String unit = currency == null ? null : String.valueOf(currency);
        return StringUtils.hasText(unit) ? price + " " + unit : price;
    }

    protected Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    protected Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    protected Map<String, Object> copyRaw(Map<String, Object> value) {
        return CollectionUtils.isEmpty(value) ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(value);
    }
}
