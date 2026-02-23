package com.example.demo.demos.CommunityFeed.Controller;

import com.example.demo.config.MinioProperties;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/community-feed")
@RequiredArgsConstructor
public class CommunityFeedUploadController {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * 直接上传图片到 MinIO，由后端接收文件后写入对象存储。
     * @param file 客户端上传的文件对象（表单字段名为"file"）
     * @return 包含对象存储Key和访问URL的响应实体
     * 响应格式示例：{"key": "feed/uuid-filename", "url": "预签名访问地址"}
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestPart("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            Map<String, String> resp = new HashMap<>();
            resp.put("message", "文件不能为空");
            return ResponseEntity.badRequest().body(resp);
        }
        // 生成对象存储的 key（包含随机UUID和原始文件名）
        // 存储路径格式：feed/<UUID>-<清理后的文件名>
        String objectKey = buildObjectKey(file.getOriginalFilename());
        // 将文件上传到 MinIO
        // 配置参数说明：
        // - bucket: 从配置文件获取的存储桶名称
        // - object: 生成的存储对象路径
        // - contentType: 保持上传文件的原始MIME类型
        // - stream: 文件输入流及大小信息
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioProperties.getBucket())
                        .object(objectKey)
                        .contentType(file.getContentType())
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .build()
        );
        // 获取预签名访问URL（7天有效期）
        // 用于生成可公开访问的临时URL
        String url = presignGet(objectKey, 7, TimeUnit.DAYS);
        Map<String, String> resp = new HashMap<>();
        resp.put("key", objectKey);
        resp.put("url", url);
        log.info("文件上传成功，返回结果：{}", resp.toString());
        return ResponseEntity.ok(resp);
    }

    /**
     * 生成PUT预签名URL，允许前端直接将文件上传到MinIO服务器。
     * 该接口通过文件名生成存储路径，并返回可直接用于上传的临时URL。
     *
     * @param filename 客户端提交的原始文件名（表单字段名为"filename"）
     * @return 包含对象存储Key和上传URL的响应实体
     * 响应格式示例：{"key": "feed/uuid-filename", "putUrl": "预签名PUT地址"}
     */
    @PostMapping("/upload-url")
    public ResponseEntity<?> uploadUrl(@RequestParam("filename") String filename) throws Exception {
        // 校验文件名有效性，防止空值或非法字符导致存储异常
        if (!StringUtils.hasText(filename)) {
            Map<String, String> resp = new HashMap<>();
            resp.put("message", "文件名不能为空");
            return ResponseEntity.badRequest().body(resp);
        }

        // 构建对象存储路径：
        // 1. 使用buildObjectKey方法生成带UUID的存储路径
        // 2. 路径格式：feed/<UUID>-<清理后的文件名>
        String objectKey = buildObjectKey(filename);

        // 生成预签名PUT请求地址：
        // - bucket: 从配置文件获取的存储桶名称
        // - object: 生成的存储对象路径
        // - method: PUT方法允许前端直接上传
        // - expiry: 设置15分钟有效期（符合安全规范）
        String putUrl = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(minioProperties.getBucket())
                        .object(objectKey)
                        .method(Method.PUT)
                        .expiry(15, TimeUnit.MINUTES)
                        .build()
        );

        // 构造响应数据，包含：
        // - key: 对象存储路径（用于后续文件访问）
        // - putUrl: 前端直接上传使用的预签名地址
        Map<String, String> resp = new HashMap<>();
        resp.put("key", objectKey);
        resp.put("putUrl", putUrl);
        log.info("生成预签名URL成功，返回结果：{}", resp.toString());
        return ResponseEntity.ok(resp);
    }

    private String buildObjectKey(String filename) {
        String clean = StringUtils.hasText(filename) ? filename.replaceAll("\\s+", "_") : "file";
        return "feed/" + UUID.randomUUID() + "-" + clean;
    }

    private String presignGet(String objectKey, int amount, TimeUnit unit) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(minioProperties.getBucket())
                        .object(objectKey)
                        .method(Method.GET)
                        .expiry(amount, unit)
                        .build()
        );
    }
}
