package com.example.demo.demos.LocalActive.Service.Impl;

import com.example.demo.demos.LocalActive.DTO.NearbyActivityDTO;
import com.example.demo.demos.LocalActive.Service.LocalActivitySearchService;
import com.example.demo.demos.LocalActive.Dao.LocalActivityMapper;
import com.example.demo.demos.LocalActive.Pojo.LocalActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalActivitySearchServiceImpl implements LocalActivitySearchService {

    private final StringRedisTemplate stringRedisTemplate;
    private final LocalActivityMapper activityMapper;
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public List<NearbyActivityDTO> searchNearby(double lat, double lon, double radiusKm, String category, String keyword, int size) {
        log.info("正在查询附近活动");
        String query = buildQuery(lat, lon, radiusKm, category, keyword);
        try {
            ensureIndex();
            List<Object> raw = executeSearch(query, size);
            List<NearbyActivityDTO> list = parseResult(raw, lat, lon);
            return list;
        } catch (Exception e) {
            return fallbackSearch(lat, lon, radiusKm, category, keyword, size);
        }
    }

    @Override
    public int syncFromDb() {
        List<LocalActivity> all = activityMapper.listAll();
        if (CollectionUtils.isEmpty(all)) return 0;
        for (LocalActivity a : all) {
            writeDoc(a);
        }
        return all.size();
    }

    private String buildQuery(double lat, double lon, double radiusKm, String category, String keyword) {
        StringBuilder sb = new StringBuilder();
        sb.append("@status:{PUBLISHED}");
        if (StringUtils.hasText(category)) {
            sb.append(" @category:{").append(category).append("}");
        }
        if (StringUtils.hasText(keyword)) {
            sb.append(" (").append(keyword).append(")");
        }
        sb.append(" @loc:[").append(lon).append(" ").append(lat).append(" ").append(radiusKm).append(" km]");
        return sb.toString();
    }

    private List<Object> executeSearch(String query, int size) {
        return stringRedisTemplate.execute((RedisConnection connection) -> {
            List<byte[]> args = new ArrayList<>();
            args.add("idx:local_activity".getBytes(StandardCharsets.UTF_8));
            args.add(query.getBytes(StandardCharsets.UTF_8));
            args.add("LIMIT".getBytes(StandardCharsets.UTF_8));
            args.add("0".getBytes(StandardCharsets.UTF_8));
            args.add(String.valueOf(size).getBytes(StandardCharsets.UTF_8));
            return (List<Object>) connection.execute("FT.SEARCH", args.toArray(new byte[0][]));
        });
    }

    private void ensureIndex() {
        try {
            Object r = stringRedisTemplate.execute((RedisConnection c) -> c.execute("FT._LIST"));
            boolean exists = false;
            if (r instanceof java.util.List) {
                java.util.List<?> l = (java.util.List<?>) r;
                for (Object o : l) {
                    String s = o instanceof byte[] ? new String((byte[]) o, StandardCharsets.UTF_8) : String.valueOf(o);
                    if ("idx:local_activity".equals(s)) { exists = true; break; }
                }
            }
            if (!exists) {
                byte[][] args = new byte[][]{
                        bs("idx:local_activity"),
                        bs("ON"), bs("HASH"),
                        bs("PREFIX"), bs("1"), bs("activity:"),
                        bs("SCHEMA"),
                        bs("status"), bs("TAG"),
                        bs("category"), bs("TAG"),
                        bs("title"), bs("TEXT"),
                        bs("location_text"), bs("TEXT"),
                        bs("cover_url"), bs("TEXT"),
                        bs("startAt"), bs("TEXT"),
                        bs("endAt"), bs("TEXT"),
                        bs("lat"), bs("NUMERIC"),
                        bs("lon"), bs("NUMERIC"),
                        bs("loc"), bs("GEO")
                };
                stringRedisTemplate.execute((RedisConnection c) -> c.execute("FT.CREATE", args));
            }
        } catch (Exception ignore) {}
    }

    private byte[] bs(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private List<NearbyActivityDTO> fallbackSearch(double lat, double lon, double radiusKm, String category, String keyword, int size) {
        List<LocalActivity> all = activityMapper.listAll();
        if (all == null || all.isEmpty()) return new ArrayList<>();
        java.util.List<NearbyActivityDTO> list = new java.util.ArrayList<>();
        for (LocalActivity a : all) {
            if (a == null) continue;
            String st = a.getStatus();
            if (st != null && !"PUBLISHED".equals(st)) continue;
            if (StringUtils.hasText(category)) {
                String c = a.getCategoryCode();
                if (c == null || !c.equals(category)) continue;
            }
            if (StringUtils.hasText(keyword)) {
                String t = a.getTitle();
                String d = a.getDescription();
                String loc = a.getLocationText();
                String kw = keyword;
                boolean hit = (t != null && t.contains(kw)) || (d != null && d.contains(kw)) || (loc != null && loc.contains(kw));
                if (!hit) continue;
            }
            Double lat2 = a.getLatitude();
            Double lon2 = a.getLongitude();
            Double dist = null;
            if (lat2 != null && lon2 != null) {
                dist = distanceKm(lat, lon, lat2, lon2);
                if (dist != null && dist > radiusKm) continue;
            }
            NearbyActivityDTO dto = new NearbyActivityDTO();
            dto.setId(a.getId());
            dto.setTitle(a.getTitle());
            dto.setCategory(a.getCategoryCode());
            dto.setLocation(a.getLocationText());
            dto.setStatus(a.getStatus());
            dto.setStartAt(a.getStartAt() == null ? null : a.getStartAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            dto.setEndAt(a.getEndAt() == null ? null : a.getEndAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            dto.setCoverUrl(a.getAddress());
            dto.setLatitude(lat2);
            dto.setLongitude(lon2);
            dto.setDistanceKm(dist);
            list.add(dto);
        }
        list.sort((a, b) -> {
            Double da = a.getDistanceKm();
            Double db = b.getDistanceKm();
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return Double.compare(da, db);
        });
        if (list.size() > size) return list.subList(0, size);
        return list;
    }

    private List<NearbyActivityDTO> parseResult(List<Object> raw, double lat, double lon) {
        if (CollectionUtils.isEmpty(raw) || raw.size() < 2) {
            return new ArrayList<>();
        }
        List<NearbyActivityDTO> list = new ArrayList<>();
        for (int i = 1; i < raw.size(); i += 2) {
            if (!(raw.get(i + 1) instanceof List)) continue;
            List<Object> fields = (List<Object>) raw.get(i + 1);
            Map<String, String> map = toMap(fields);
            NearbyActivityDTO dto = new NearbyActivityDTO();
            dto.setId(parseLong(map.get("id")));
            dto.setTitle(map.get("title"));
            dto.setCategory(map.get("category"));
            dto.setLocation(map.get("location_text"));
            dto.setStatus(map.getOrDefault("status", "PUBLISHED"));
            dto.setStartAt(parseDate(map.get("startAt")));
            dto.setEndAt(parseDate(map.get("endAt")));
            dto.setCoverUrl(map.get("cover_url"));
            dto.setLatitude(parseDouble(map.get("lat")));
            dto.setLongitude(parseDouble(map.get("lon")));
            if (dto.getLatitude() != null && dto.getLongitude() != null) {
                dto.setDistanceKm(distanceKm(lat, lon, dto.getLatitude(), dto.getLongitude()));
            }
            list.add(dto);
        }
        return list;
    }

    private Map<String, String> toMap(List<Object> fields) {
        Map<String, String> map = fields.stream()
                .map(o -> o instanceof byte[] ? new String((byte[]) o, StandardCharsets.UTF_8) : Objects.toString(o, ""))
                .collect(Collectors.toList())
                .stream()
                .collect(Collectors.toMap(
                        k -> k,
                        v -> v,
                        (a, b) -> b));
        // fields are [key1, val1, key2, val2...], rebuild map
        return map;
    }

    private String parseDate(String iso) {
        if (!StringUtils.hasText(iso)) return null;
        try {
            LocalDateTime dt = LocalDateTime.parse(iso, ISO_FMT);
            return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception e) {
            return iso;
        }
    }

    private Long parseLong(String v) {
        try {
            return Long.valueOf(v);
        } catch (Exception e) {
            return null;
        }
    }

    private Double parseDouble(String v) {
        try {
            return Double.valueOf(v);
        } catch (Exception e) {
            return null;
        }
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(R * c * 10.0) / 10.0;
    }

    private void writeDoc(LocalActivity a) {
        if (a == null || a.getId() == null) return;
        String key = "activity:" + a.getId();
        java.util.Map<String, String> doc = new java.util.HashMap<>();
        doc.put("id", String.valueOf(a.getId()));
        doc.put("title", safe(a.getTitle()));
        doc.put("subtitle", safe(a.getSubtitle()));
        doc.put("category", safe(a.getCategoryCode()));
        doc.put("status", safe(a.getStatus()));
        doc.put("location_text", safe(a.getLocationText()));
        doc.put("cover_url", safe(a.getAddress())); // fallback: address if no cover_url column in pojo
        doc.put("startAt", a.getStartAt() == null ? "" : a.getStartAt().format(ISO_FMT));
        doc.put("endAt", a.getEndAt() == null ? "" : a.getEndAt().format(ISO_FMT));
        if (a.getLatitude() != null && a.getLongitude() != null) {
            doc.put("lat", String.valueOf(a.getLatitude()));
            doc.put("lon", String.valueOf(a.getLongitude()));
            doc.put("loc", a.getLongitude() + "," + a.getLatitude());
        }
        stringRedisTemplate.opsForHash().putAll(key, doc);
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }
}
