package com.example.demo.demos.CommunityMarket.Service.Impl;

import com.example.demo.demos.CommunityMarket.DTO.ProductAndSellerQueryDTO;
import com.example.demo.demos.CommunityMarket.DTO.ProductNearbyDTO;
import com.example.demo.demos.CommunityMarket.Dao.ProductsMapper;
import com.example.demo.demos.CommunityMarket.Pojo.Product;
import com.example.demo.demos.CommunityMarket.Pojo.ProductImages;
import com.example.demo.demos.CommunityMarket.Pojo.UserProducts;
import com.example.demo.demos.CommunityMarket.Service.ProductsService;
import com.example.demo.demos.PageResponse.PageResponse;
import com.example.demo.demos.generic.Resp;
import com.example.demo.demos.Notification.Pojo.NotificationMessage;
import com.example.demo.demos.Notification.Service.NotificationSender;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.example.demo.demos.CommunityMarket.DTO.ProductQueryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.demos.Login.Mapper.LoginMapper;
import com.example.demo.demos.Login.Entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Circle;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductsServiceImpl implements ProductsService {
    private static final Logger log = LoggerFactory.getLogger(ProductsServiceImpl.class);

    @Autowired
    private ProductsMapper productsMapper;
    @Autowired
    private LoginMapper loginMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private NotificationSender notificationSender;
    // 启动后确保 RediSearch 索引存在；不存在则创建
    @javax.annotation.PostConstruct
    private void init() {
        ensureRediSearchIndex();
    }

    @Override
    public List<Product> getAllProducts() {
        List<Product> products = productsMapper.getAllProducts();
        return products;
    }

    @Override
    public PageResponse<Product> getProducts(ProductQueryDTO query) {
        // 默认分页参数
        int page = (query.getPage() == null || query.getPage() < 1) ? 1 : query.getPage();
        int size = (query.getSize() == null || query.getSize() < 1) ? 10 : query.getSize();
        // 排序参数
        String sortNorm = (query.getSort() == null || query.getSort().isEmpty()) ? "comprehensive" : query.getSort();
        // 排序方式参数
        if (!("comprehensive".equals(sortNorm) || "latest".equals(sortNorm) || "price".equals(sortNorm))) {
            sortNorm = "comprehensive";
        }
        // 排序方式参数
        String orderNorm = (query.getOrder() == null || query.getOrder().isEmpty()) ? "desc" : query.getOrder().toLowerCase();
        if (!("asc".equals(orderNorm) || "desc".equals(orderNorm))) {
            orderNorm = "desc";
        }
        // 设置排序参数
        query.setSort(sortNorm);
        query.setOrder(orderNorm);
        // 分页查询
        PageHelper.startPage(page, size);
        List<Product> products = productsMapper.getProducts(query);
        PageInfo<Product> pageInfo = new PageInfo<>(products);
        return new PageResponse<>(products, pageInfo.getPageNum(), pageInfo.getPageSize(), pageInfo.getTotal());
    }

    @Override
    public ResponseEntity<Product> getProductById(Long id) {
        if(productsMapper.getProductById(id) == null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(productsMapper.getProductById(id));
    }

    @Override
    public ResponseEntity<ProductAndSellerQueryDTO> getProductAndSeller(Integer id) {
        ProductAndSellerQueryDTO productAndSellerQueryDTO = productsMapper.getProductAndSellerById(id);
        if(productAndSellerQueryDTO == null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(productAndSellerQueryDTO);
    }

    @Override
    public ResponseEntity<List<ProductImages>> getProductImage(Integer id) {
        return ResponseEntity.ok(productsMapper.getProductImage(id));
    }

    @Transactional
    @Override
    public Resp<Void> addProduct(Product product) {
        if (product.getSellerId() == null) {
            return Resp.error("卖家信息缺失");
        }
        if (product.getStatus() == null || product.getStatus().trim().isEmpty()) {
            product.setStatus("在售");
        }
        if (product.getImageUrls() == null) {
            product.setImageUrls("[]");
        }
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        // 先插入商品，获取数据库生成的主键ID
        int rows = productsMapper.addProduct(product);
        if (rows <= 0) {
            return Resp.error("添加商品失败");
        }
        // 发布商品的同时将商品信息添加到我的商品表
        UserProducts userProduct = new UserProducts();
        userProduct.setUserId(product.getSellerId() == null ? null : Long.valueOf(product.getSellerId()));
        userProduct.setProductId(product.getId());
        userProduct.setCreatedAt(LocalDateTime.now());
        userProduct.setUpdatedAt(LocalDateTime.now());
        productsMapper.addToMyProducts(userProduct);
        // 将商品信息写入 RediSearch 索引使用的 Hash（product:{id}），用于后续服务端综合筛选
        Map<String, String> m = new HashMap<>();
        if (product.getTitle() != null) m.put("title", product.getTitle());
        if (product.getDescription() != null) m.put("description", product.getDescription());
        if (product.getPrice() != null) m.put("price", product.getPrice().toPlainString());
        if (product.getCategoryId() != null) m.put("categoryId", String.valueOf(product.getCategoryId()));
        if (product.getStatus() != null) m.put("status", product.getStatus());
        m.put("updatedAt", String.valueOf(System.currentTimeMillis()));
        // coords 存储为 "lng,lat" 以支持 RediSearch 的 GEOFILTER 半径过滤
        if (product.getLatitude() != null && product.getLongitude() != null) {
            String slng = String.format(java.util.Locale.US, "%.12f", product.getLongitude());
            String slat = String.format(java.util.Locale.US, "%.12f", product.getLatitude());
            m.put("coords", slng + "," + slat);
        }
        // 写入 Hash，Key：product:{id}
        if (!m.isEmpty()) {
            redisTemplate.opsForHash().putAll("product:" + product.getId(), m);
        }
        if (product.getLatitude() != null && product.getLongitude() != null) {
            redisTemplate.opsForGeo().add("geo:products", new Point(product.getLongitude(), product.getLatitude()), "product:" + product.getId());
        }
        // 发布成功后通知卖家
        pushProductPublishedNotice(product);
        return Resp.success();
    }

    @Override
    public ResponseEntity<List<Product>> getMyProducts(String userName) {
        User user = loginMapper.getUserByName(userName);
        if (user == null || user.getUserId() == null) {
            return ResponseEntity.notFound().build();
        }
        Long sellerId;
        try {
            sellerId = Long.valueOf(user.getUserId());
        } catch (NumberFormatException e) {
            return ResponseEntity.notFound().build();
        }
        List<Product> list = productsMapper.getProductsBySellerId(sellerId);
        return ResponseEntity.ok(list == null ? java.util.Collections.emptyList() : list);
    }

    private void pushProductPublishedNotice(Product product) {
        try {
            if (product.getSellerId() == null) {
                return;
            }
            NotificationMessage msg = new NotificationMessage();
            msg.setTargetUserId(Long.valueOf(product.getSellerId()));
            msg.setTargetType("USER");
            msg.setKind("PRODUCT_PUBLISHED");
            msg.setTitle("商品发布成功");
            String title = product.getTitle() == null ? "" : product.getTitle();
            msg.setContent("您的商品【" + title + "】已发布成功");
            msg.setCreatedAt(LocalDateTime.now());
            notificationSender.send(msg);
        } catch (Exception e) {
            log.warn("pushProductPublishedNotice failed, productId={}", product.getId(), e);
        }
    }

    @Override
    public Resp<Void> takeDownProduct(Long productId) {
        int rows = productsMapper.setStockZero(productId);
        return rows > 0 ? Resp.success() : Resp.error("下架失败");
    }

    @Override
    public Resp<Void> increaseStock(Long productId, Integer delta) {
        if (delta == null || delta <= 0) return Resp.error("数量必须为正数");
        int rows = productsMapper.increaseStock(productId, delta);
        return rows > 0 ? Resp.success() : Resp.error("库存更新失败");
    }

    @Override
    public Resp<Void> updatePrice(Long productId, java.math.BigDecimal price) {
        if (price == null || price.compareTo(java.math.BigDecimal.ZERO) < 0) return Resp.error("价格不合法");
        int rows = productsMapper.updatePrice(productId, price);
        return rows > 0 ? Resp.success() : Resp.error("价格更新失败");
    }

    @Override
    public Resp<Void> updateLocation(Long productId, String location) {
        if (location == null || location.trim().isEmpty()) return Resp.error("地址不合法");
        int rows = productsMapper.updateLocation(productId, location.trim());
        return rows > 0 ? Resp.success() : Resp.error("地址更新失败");
    }

    @Override
    public ResponseEntity<List<ProductNearbyDTO>> getNearbyProducts(
            double lat,
            double lng,
            Double radiusKm,
            Integer limit,
            Integer offset,
            Integer categoryId,
            java.math.BigDecimal minPrice,
            java.math.BigDecimal maxPrice,
            String keyword
    ) {
        Double radius = radiusKm == null ? 5.0 : radiusKm;
        Integer lim = (limit == null || limit <= 0) ? 50 : limit;
        Integer off = (offset == null || offset < 0) ? 0 : offset;

        List<ProductNearbyDTO> list = new ArrayList<>();
        // 优先使用 Redis GEO 距离
        Circle circle = new Circle(new Point(lng, lat), new Distance(radius, Metrics.KILOMETERS));
        // 参数：Key，中心点，半径，参数
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs().includeDistance().sortAscending().limit(lim + off);
        // 获取指定半径内的商品
        GeoResults<RedisGeoCommands.GeoLocation<String>> geo = redisTemplate.opsForGeo().radius("geo:products", circle, args);
        // 遍历结果
        if (geo != null && geo.getContent() != null) {
            int skipped = 0;
            for (GeoResult<RedisGeoCommands.GeoLocation<String>> r : geo) {
                if (skipped < off) { skipped++; continue; }
                String name = r.getContent().getName();
                if (name == null || !name.startsWith("product:")) continue;
                Long pid = null; try { pid = Long.valueOf(name.substring(8)); } catch (Exception ignore) {}
                if (pid == null) continue;
                Product p = productsMapper.getProductById(pid);
                if (p == null) continue;
                if (categoryId != null && p.getCategoryId() != null && !categoryId.equals(p.getCategoryId())) continue;
                if (minPrice != null && p.getPrice() != null && p.getPrice().compareTo(minPrice) < 0) continue;
                if (maxPrice != null && p.getPrice() != null && p.getPrice().compareTo(maxPrice) > 0) continue;
                if (keyword != null && !keyword.trim().isEmpty()) {
                    String t = p.getTitle(); if (t == null || !t.contains(keyword)) continue;
                }
                ProductNearbyDTO dto = new ProductNearbyDTO();
                dto.setId(p.getId()); dto.setSellerId(p.getSellerId()); dto.setCategoryId(p.getCategoryId());
                dto.setTitle(p.getTitle()); dto.setDescription(p.getDescription()); dto.setPrice(p.getPrice());
                dto.setStockQuantity(p.getStockQuantity()); dto.setCondition(p.getCondition()); dto.setLocation(p.getLocation());
                dto.setImageUrls(p.getImageUrls()); dto.setStatus(p.getStatus()); dto.setCreatedAt(p.getCreatedAt()); dto.setUpdatedAt(p.getUpdatedAt());
                dto.setLatitude(p.getLatitude()); dto.setLongitude(p.getLongitude());
                dto.setDistanceKm(r.getDistance() != null ? r.getDistance().getValue() : null);
                dto.setDistanceSource("redis-geo");
                list.add(dto);
                if (list.size() >= lim) break;
            }
        }
        if (!list.isEmpty()) return ResponseEntity.ok(list);

        // 其次使用 RediSearch 过滤 + Haversine 距离
        String q = buildSearchQuery(categoryId, minPrice, maxPrice, keyword);
        List<Long> ids = rediSearchIds(q, lng, lat, radius, off, lim);
        for (Long pid : ids) {
            Product p = productsMapper.getProductById(pid);
            if (p == null) continue;
            if (categoryId != null && p.getCategoryId() != null && !categoryId.equals(p.getCategoryId())) continue;
            if (minPrice != null && p.getPrice() != null && p.getPrice().compareTo(minPrice) < 0) continue;
            if (maxPrice != null && p.getPrice() != null && p.getPrice().compareTo(maxPrice) > 0) continue;
            if (keyword != null && !keyword.trim().isEmpty()) {
                String t = p.getTitle(); if (t == null || !t.contains(keyword)) continue;
            }
            ProductNearbyDTO dto = new ProductNearbyDTO();
            dto.setId(p.getId()); dto.setSellerId(p.getSellerId()); dto.setCategoryId(p.getCategoryId());
            dto.setTitle(p.getTitle()); dto.setDescription(p.getDescription()); dto.setPrice(p.getPrice());
            dto.setStockQuantity(p.getStockQuantity()); dto.setCondition(p.getCondition()); dto.setLocation(p.getLocation());
            dto.setImageUrls(p.getImageUrls()); dto.setStatus(p.getStatus()); dto.setCreatedAt(p.getCreatedAt()); dto.setUpdatedAt(p.getUpdatedAt());
            dto.setLatitude(p.getLatitude()); dto.setLongitude(p.getLongitude());
            dto.setDistanceKm(p.getLatitude() != null && p.getLongitude() != null ? haversineKm(lat, lng, p.getLatitude(), p.getLongitude()) : null);
            dto.setDistanceSource("redisearch-haversine");
            list.add(dto);
            if (list.size() >= lim) break;
        }
        if (!list.isEmpty()) return ResponseEntity.ok(list);

        // 最后回退到 SQL Haversine
        java.util.List<com.example.demo.demos.CommunityMarket.DTO.ProductNearbyDTO> fallback = productsMapper.getNearbyProducts(lat, lng, radiusKm, limit, offset, categoryId, minPrice, maxPrice, keyword);
        if (fallback != null) {
            for (com.example.demo.demos.CommunityMarket.DTO.ProductNearbyDTO d : fallback) {
                d.setDistanceSource("sql-haversine");
            }
        }
        return ResponseEntity.ok(fallback == null ? java.util.Collections.emptyList() : fallback);
    }

    // 确保 RediSearch 索引存在；若不存在则创建：
    // ON HASH PREFIX product:；字段：title TEXT、description TEXT、price NUMERIC、categoryId TAG、status TAG、coords GEO、updatedAt NUMERIC
    private void ensureRediSearchIndex() {
        try {
            Object r = redisTemplate.execute((RedisCallback<Object>) c -> c.execute("FT._LIST"));
            boolean exists = false;
            if (r instanceof java.util.List) {
                java.util.List<?> l = (java.util.List<?>) r;
                for (Object o : l) {
                    String s = toStr(o);
                    if ("idx:products".equals(s)) { exists = true; break; }
                }
            }
            if (!exists) {
                byte[][] args = new byte[][]{
                        bs("idx:products"),
                        bs("ON"), bs("HASH"),
                        bs("PREFIX"), bs("1"), bs("product:"),
                        bs("SCHEMA"),
                        bs("title"), bs("TEXT"),
                        bs("description"), bs("TEXT"),
                        bs("price"), bs("NUMERIC"), bs("SORTABLE"),
                        bs("categoryId"), bs("TAG"),
                        bs("status"), bs("TAG"),
                        bs("coords"), bs("GEO"),
                        bs("updatedAt"), bs("NUMERIC"), bs("SORTABLE")
                };
                redisTemplate.execute((RedisCallback<Object>) c -> c.execute("FT.CREATE", args));
            }
        } catch (Exception ignore) {}
    }

    // 构造 RediSearch 查询表达式：
    // - 分类：@categoryId:{id}
    // - 价格区间：@price:[min max]
    // - 关键词：@title:(keyword)
    private String buildSearchQuery(Integer categoryId, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice, String keyword) {
        StringBuilder sb = new StringBuilder();
        if (categoryId != null) {
            sb.append("@categoryId:{").append(categoryId).append("} ");
        }
        if (minPrice != null || maxPrice != null) {
            String min = minPrice != null ? minPrice.toPlainString() : "-inf";
            String max = maxPrice != null ? maxPrice.toPlainString() : "+inf";
            sb.append("@price:[").append(min).append(" ").append(max).append("] ");
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sb.append("@title:(").append(keyword).append(") ");
        }
        String q = sb.toString().trim();
        return q.isEmpty() ? "*" : q;
    }

    // 执行 RediSearch 搜索：FT.SEARCH idx:products <query> GEOFILTER coords lng lat radius KM LIMIT offset limit
    // 解析返回的文档 ID（product:{id}）为 Long 列表
    private java.util.List<Long> rediSearchIds(String query, double lng, double lat, double radiusKm, int offset, int limit) {
        java.util.List<Long> ids = new ArrayList<>();
        try {
            java.util.List<byte[]> args = new ArrayList<>();
            args.add(bs("idx:products"));
            args.add(bs(query));
            args.add(bs("GEOFILTER"));
            args.add(bs("coords"));
            args.add(bs(String.valueOf(lng)));
            args.add(bs(String.valueOf(lat)));
            args.add(bs(String.valueOf(radiusKm)));
            args.add(bs("KM"));
            args.add(bs("LIMIT"));
            args.add(bs(String.valueOf(offset)));
            args.add(bs(String.valueOf(limit)));
            Object r = redisTemplate.execute((RedisCallback<Object>) c -> c.execute("FT.SEARCH", args.toArray(new byte[0][])));
            if (r instanceof java.util.List) {
                java.util.List<?> l = (java.util.List<?>) r;
                for (int i = 1; i < l.size(); i += 2) {
                    String id = toStr(l.get(i));
                    if (id != null && id.startsWith("product:")) {
                        try {
                            ids.add(Long.valueOf(id.substring(8)));
                        } catch (Exception ignore) {}
                    }
                }
            }
        } catch (Exception ignore) {}
        return ids;
    }

    private String toStr(Object o) {
        if (o == null) return null;
        if (o instanceof byte[]) return new String((byte[]) o, StandardCharsets.UTF_8);
        return String.valueOf(o);
    }

    private byte[] bs(String s) { return s.getBytes(StandardCharsets.UTF_8); }

    // 计算两点之间的球面距离（单位：公里），用于返回展示或在内存排序
    private Double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    // 批量迁移：将数据库中的商品写入 RediSearch 索引；
    // 对没有坐标的商品生成随机坐标并写回数据库
    public ResponseEntity<java.util.Map<String, Object>> migrateToRediSearch(boolean assignRandom) {
        java.util.List<com.example.demo.demos.CommunityMarket.Pojo.Product> all = productsMapper.getAllProducts();
        int total = all == null ? 0 : all.size();
        int migrated = 0;
        int randomized = 0;
        if (all != null) {
            for (com.example.demo.demos.CommunityMarket.Pojo.Product p : all) {
                Double lat = p.getLatitude();
                Double lng = p.getLongitude();
                if ((lat == null || lng == null) && assignRandom) {
                    double[] xy = randomChinaCoord();
                    lat = xy[0];
                    lng = xy[1];
                    try { productsMapper.updateCoordinates(p.getId(), lat, lng); } catch (Exception ignore) {}
                    randomized++;
                }
                java.util.Map<String, String> m = new java.util.HashMap<>();
                if (p.getTitle() != null) m.put("title", p.getTitle());
                if (p.getDescription() != null) m.put("description", p.getDescription());
                if (p.getPrice() != null) m.put("price", p.getPrice().toPlainString());
                if (p.getCategoryId() != null) m.put("categoryId", String.valueOf(p.getCategoryId()));
                if (p.getStatus() != null) m.put("status", p.getStatus());
                m.put("updatedAt", String.valueOf(System.currentTimeMillis()));
                if (lat != null && lng != null) {
                    String slng = String.format(java.util.Locale.US, "%.12f", lng);
                    String slat = String.format(java.util.Locale.US, "%.12f", lat);
                    m.put("coords", slng + "," + slat);
                }
                try { redisTemplate.opsForHash().putAll("product:" + p.getId(), m); migrated++; } catch (Exception ignore) {}
                if (lat != null && lng != null) {
                    try { redisTemplate.opsForGeo().add("geo:products", new Point(lng, lat), "product:" + p.getId()); } catch (Exception ignore) {}
                }
            }
        }
        java.util.Map<String, Object> res = new java.util.HashMap<>();
        res.put("total", total);
        res.put("migrated", migrated);
        res.put("randomized", randomized);
        return ResponseEntity.ok(res);
    }

    // 生成一个随机的中国境内坐标（粗略边界），用于填充缺失的经纬度
    private double[] randomChinaCoord() {
        java.util.Random r = new java.util.Random();
        double lat = 18.0 + r.nextDouble() * (53.0 - 18.0);
        double lng = 73.5 + r.nextDouble() * (135.0 - 73.5);
        return new double[]{lat, lng};
    }

    public ResponseEntity<java.util.Map<String, Object>> syncGeoFromDb() {
        java.util.List<com.example.demo.demos.CommunityMarket.Pojo.Product> all = productsMapper.getAllProducts();
        int total = all == null ? 0 : all.size();
        int updated = 0;
        if (all != null) {
            for (com.example.demo.demos.CommunityMarket.Pojo.Product p : all) {
                if (p.getLatitude() != null && p.getLongitude() != null) {
                    try {
                        redisTemplate.opsForGeo().add("geo:products", new org.springframework.data.geo.Point(p.getLongitude(), p.getLatitude()), "product:" + p.getId());
                        updated++;
                    } catch (Exception ignore) {}
                }
            }
        }
        java.util.Map<String, Object> res = new java.util.HashMap<>();
        res.put("total", total);
        res.put("updated", updated);
        return ResponseEntity.ok(res);
    }

}
