package com.example.demo.demos.CommunityMarket.Controller;

import com.example.demo.demos.CommunityMarket.DTO.ProductAndSellerQueryDTO;
import com.example.demo.demos.CommunityMarket.Pojo.Product;
import com.example.demo.demos.CommunityMarket.Pojo.ProductImages;
import com.example.demo.demos.CommunityMarket.Service.ProductsService;
import com.example.demo.demos.PageResponse.PageResponse;
import com.example.demo.demos.CommunityMarket.DTO.ProductQueryDTO;
import com.example.demo.demos.CommunityMarket.DTO.ProductNearbyDTO;
import com.example.demo.demos.Login.Entity.User;
import com.example.demo.demos.Login.Service.LoginService;
import com.example.demo.config.MinioProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.demo.demos.generic.Resp;

import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductsController {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> LIST_STRING_TYPE = new TypeReference<List<String>>() {};
    private static final String PRODUCT_BUCKET = "community-marketplace";

    @Autowired
    private ProductsService productsService;

    @Autowired
    private LoginService loginService;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioProperties minioProperties;

    @Operation(summary = "获取所有商品")
    @RequestMapping("/getAllProducts")
    public List<Product> getAllProducts() {
        List<Product> list = productsService.getAllProducts();
        if (list != null) {
            list.forEach(this::fillPresignedImages);
        }
        return list;
    }

    @Operation(summary = "分页获取筛选后的商品")
    @RequestMapping("/getProducts")
    public PageResponse<Product> getProducts(@ModelAttribute ProductQueryDTO query) {
        PageResponse<Product> resp = productsService.getProducts(query);
        if (resp != null && resp.getItems() != null) {
            resp.getItems().forEach(this::fillPresignedImages);
        }
        return resp;
    }
    @Operation(summary = "根据id获取商品信息")
    @RequestMapping("/getProductById/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable("id") Long id) {
        ResponseEntity<Product> resp = productsService.getProductById(id);
        if (resp != null && resp.getBody() != null) {
            fillPresignedImages(resp.getBody());
        }
        return resp;
    }

    @Operation(summary = "根据id获取商品信息以及卖家信息")
    @RequestMapping("/getProductAndSeller/{id}")
    public ResponseEntity<ProductAndSellerQueryDTO> getProductAndSeller(@PathVariable("id") Integer id) {
        ResponseEntity<ProductAndSellerQueryDTO> resp = productsService.getProductAndSeller(id);
        if (resp != null && resp.getBody() != null) {
            fillPresignedImages(resp.getBody());
        }
        return resp;
    }
    @Operation(summary="根据商品id获取商品图片")
    @RequestMapping("/getProductImage/{id}")
    public ResponseEntity<List<ProductImages>> getProductImage(@PathVariable("id") Integer id) {
        return productsService.getProductImage(id);
    }

    @Operation(summary = "上传商品图片（MinIO）")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProductImage(@RequestPart("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            Map<String, String> resp = new HashMap<>();
            resp.put("message", "文件不能为空");
            return ResponseEntity.badRequest().body(resp);
        }
        String objectKey = buildObjectKey(file.getOriginalFilename());
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(getProductBucket())
                        .object(objectKey)
                        .contentType(file.getContentType())
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .build()
        );
        String url = presignGet(objectKey, 7, TimeUnit.DAYS);
        Map<String, String> resp = new HashMap<>();
        resp.put("key", objectKey);
        resp.put("url", url);
        resp.put("publicUrl", buildPublicUrl(objectKey));
        return ResponseEntity.ok(resp);
    }

    @Operation(summary="发布商品")
    @RequestMapping("/addProduct")
    public Resp<Void> addProduct(@RequestBody Product product,
                                 @RequestHeader(value = "Authorization", required = false) String authorization){
//        先从前端结构体中获取卖家id，如果为空，则从token中获取卖家id
        Integer sellerId = product.getSellerId();
        if (sellerId == null) {
//            如果卖家id为空，则从token中获取卖家id
            String username = extractUsernameFromAuthorization(authorization);
            if (username != null && !username.isEmpty()) {
//                如果用户名不为空，则从数据库中获取用户信息
                User user = loginService.getUserByName(username);
//                如果用户信息不为空，则将卖家id设置给商品
                if (user != null && user.getUserId() != null && !user.getUserId().trim().isEmpty()) {
                    try {
                        product.setSellerId(Integer.valueOf(user.getUserId()));
                    } catch (NumberFormatException ignore) {}
                }
            }
        }
        if (product.getSellerId() == null) {
            return Resp.error("卖家信息缺失");
        }
        List<String> imageKeys = parseImageKeys(product.getImageUrls());
        if (imageKeys.isEmpty()) {
            return Resp.error("商品图片不能为空");
        }
        try {
            product.setImageUrls(OBJECT_MAPPER.writeValueAsString(imageKeys));
        } catch (Exception e) {
            return Resp.error("图片参数异常");
        }
        return productsService.addProduct(product);
    }

    @Operation(summary = "按距离筛选附近商品")
    @GetMapping("/nearby")
    public ResponseEntity<java.util.List<com.example.demo.demos.CommunityMarket.DTO.ProductNearbyDTO>> nearby(
            @RequestParam("lat") double lat,
            @RequestParam("lng") double lng,
            @RequestParam(value = "radiusKm", required = false) Double radiusKm,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset,
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "minPrice", required = false) java.math.BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) java.math.BigDecimal maxPrice,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        ResponseEntity<java.util.List<ProductNearbyDTO>> resp = productsService.getNearbyProducts(lat, lng, radiusKm, limit, offset, categoryId, minPrice, maxPrice, keyword);
        if (resp != null && resp.getBody() != null) {
            resp.getBody().forEach(this::fillPresignedImages);
        }
        return resp;
    }

    @Operation(summary = "获取我的商品列表")
    @GetMapping("/myProducts")
    public ResponseEntity<List<Product>> getMyProducts(@RequestParam("userName") String userName) {
        ResponseEntity<List<Product>> resp = productsService.getMyProducts(userName);
        if (resp != null && resp.getBody() != null) {
            resp.getBody().forEach(this::fillPresignedImages);
        }
        return resp;
    }

    @Operation(summary = "下架我的商品")
    @PostMapping("/takeDown")
    public Resp<Void> takeDown(@RequestParam("productId") Long productId) {
        return productsService.takeDownProduct(productId);
    }

    @Operation(summary = "增加库存数量")
    @PostMapping("/increaseStock")
    public Resp<Void> increaseStock(@RequestParam("productId") Long productId,
                                    @RequestParam("delta") Integer delta) {
        return productsService.increaseStock(productId, delta);
    }

    @Operation(summary = "调整价格")
    @PostMapping("/updatePrice")
    public Resp<Void> updatePrice(@RequestParam("productId") Long productId,
                                  @RequestParam("price") java.math.BigDecimal price) {
        return productsService.updatePrice(productId, price);
    }

    @Operation(summary = "调整地址")
    @PostMapping("/updateLocation")
    public Resp<Void> updateLocation(@RequestParam("productId") Long productId,
                                     @RequestParam("location") String location) {
        return productsService.updateLocation(productId, location);
    }

    @Operation(summary = "批量迁移至 RediSearch，并为无坐标商品生成随机坐标")
    @PostMapping("/migrateRediSearch")
    public ResponseEntity<java.util.Map<String, Object>> migrateRediSearch(
            @RequestParam(value = "assignRandom", required = false, defaultValue = "true") boolean assignRandom
    ) {
        return ((com.example.demo.demos.CommunityMarket.Service.Impl.ProductsServiceImpl) productsService).migrateToRediSearch(assignRandom);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "同步数据库坐标到 Redis GEO")
    @PostMapping("/syncGeo")
    public ResponseEntity<java.util.Map<String, Object>> syncGeo() {
        return ((com.example.demo.demos.CommunityMarket.Service.Impl.ProductsServiceImpl) productsService).syncGeoFromDb();
    }

    private String extractUsernameFromAuthorization(String authorization) {
//        如果authorization为空，则返回null
        if (authorization == null || authorization.trim().isEmpty()) return null;
        String token = authorization.trim();
        if (token.toLowerCase().startsWith("bearer ")) {
            token = token.substring(7).trim();
        }
        // 判断token是否以"token-"开头
        if (!token.startsWith("token-")) return null;
        String payload = token.substring(6);
        try {
            // 解码payload 解码token
            String decoded = new String(Base64.getDecoder().decode(payload), StandardCharsets.UTF_8);
            int idx = decoded.indexOf(":");
            if (idx > 0) return decoded.substring(0, idx);
            return decoded;
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> parseImageKeys(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        try {
            List<String> parsed = OBJECT_MAPPER.readValue(raw, LIST_STRING_TYPE);
            List<String> cleaned = new ArrayList<>();
            for (String item : parsed) {
                if (StringUtils.hasText(item)) {
                    cleaned.add(item.trim().replace("`", ""));
                }
            }
            return cleaned;
        } catch (Exception ignored) {
            String trimmed = raw.trim();
            if (StringUtils.hasText(trimmed)) {
                return Collections.singletonList(trimmed.replace("`", ""));
            }
            return Collections.emptyList();
        }
    }

    private String buildObjectKey(String filename) {
        String clean = StringUtils.hasText(filename) ? filename.replaceAll("\\s+", "_") : "file";
        return "products/" + UUID.randomUUID() + "-" + clean;
    }

    private void fillPresignedImages(Product product) {
        if (product == null) return;
        List<String> urls = buildPresignedUrls(parseImageKeys(product.getImageUrls()));
        if (urls == null) return;
        try {
            product.setImageUrls(OBJECT_MAPPER.writeValueAsString(urls));
        } catch (Exception ignore) {}
    }

    private void fillPresignedImages(ProductAndSellerQueryDTO dto) {
        if (dto == null) return;
        List<String> urls = buildPresignedUrls(parseImageKeys(dto.getImageUrls()));
        if (urls == null) return;
        try {
            dto.setImageUrls(OBJECT_MAPPER.writeValueAsString(urls));
        } catch (Exception ignore) {}
    }

    private void fillPresignedImages(ProductNearbyDTO dto) {
        if (dto == null) return;
        List<String> urls = buildPresignedUrls(parseImageKeys(dto.getImageUrls()));
        if (urls == null) return;
        try {
            dto.setImageUrls(OBJECT_MAPPER.writeValueAsString(urls));
        } catch (Exception ignore) {}
    }

    private List<String> buildPresignedUrls(List<String> keys) {
        if (keys == null || keys.isEmpty()) return java.util.Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (String k : keys) {
            if (!StringUtils.hasText(k)) continue;
            String trimmed = k.trim();
            // 已是 HTTP，但不属于当前 MinIO endpoint，则直接返回原始地址，避免破坏外链
            if ((trimmed.startsWith("http://") || trimmed.startsWith("https://")) && extractObjectKey(trimmed) == null) {
                result.add(trimmed);
                continue;
            }
            String maybeKey = extractObjectKey(trimmed);
            String targetKey = StringUtils.hasText(maybeKey) ? maybeKey : trimmed;
            try {
                result.add(presignGet(targetKey, 7, TimeUnit.DAYS));
            } catch (Exception e) {
                result.add(buildPublicUrl(targetKey));
            }
        }
        return result;
    }

    private String presignGet(String objectKey, int amount, TimeUnit unit) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(getProductBucket())
                        .object(objectKey)
                        .method(Method.GET)
                        .expiry(amount, unit)
                        .build()
        );
    }

    private String buildPublicUrl(String key) {
        if (!StringUtils.hasText(key)) {
            return key;
        }
        String trimmed = key.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        String endpoint = StringUtils.hasText(minioProperties.getEndpoint()) ? minioProperties.getEndpoint().trim() : "";
        endpoint = StringUtils.trimTrailingCharacter(endpoint, '/');
        String bucket = getProductBucket();
        String path = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
        String bucketPath = bucket.isEmpty() ? "" : "/" + bucket;
        return endpoint + bucketPath + "/" + path;
    }

    private String getProductBucket() {
        return StringUtils.hasText(PRODUCT_BUCKET) ? PRODUCT_BUCKET : (StringUtils.hasText(minioProperties.getBucket()) ? minioProperties.getBucket().trim() : "");
    }

    /**
     * 从完整 URL 中提取对象 key，便于为旧数据生成预签名
     */
    private String extractObjectKey(String url) {
        if (!StringUtils.hasText(url)) return null;
        String trimmed = url.trim();
        if (!(trimmed.startsWith("http://") || trimmed.startsWith("https://"))) {
            return trimmed;
        }
        String endpoint = StringUtils.hasText(minioProperties.getEndpoint()) ? minioProperties.getEndpoint().trim() : "";
        if (StringUtils.hasText(endpoint)) {
            String normalizedEndpoint = StringUtils.trimTrailingCharacter(endpoint, '/');
            if (trimmed.startsWith(normalizedEndpoint)) {
                String remain = trimmed.substring(normalizedEndpoint.length());
                remain = remain.startsWith("/") ? remain.substring(1) : remain;
                String bucket = getProductBucket();
                if (StringUtils.hasText(bucket) && remain.startsWith(bucket)) {
                    String afterBucket = remain.substring(bucket.length());
                    afterBucket = afterBucket.startsWith("/") ? afterBucket.substring(1) : afterBucket;
                    return afterBucket;
                }
                return remain;
            }
        }
        // 如果 endpoint 不匹配，尝试根据 bucket 名截取
        String bucket = getProductBucket();
        if (StringUtils.hasText(bucket)) {
            int idx = trimmed.indexOf("/" + bucket + "/");
            if (idx >= 0) {
                String after = trimmed.substring(idx + bucket.length() + 2);
                return after;
            }
        }
        return null;
    }
}
