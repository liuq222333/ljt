package com.example.demo.demos.LocalActive.Service.Impl;

import com.example.demo.config.MinioProperties;
import com.example.demo.demos.LocalActive.DTO.LocalActMediaUploadResponse;
import com.example.demo.demos.LocalActive.Service.LocalActMediaService;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
@RequiredArgsConstructor
public class LocalActMediaServiceImpl implements LocalActMediaService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024L * 1024L;
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Set<String> ALLOWED_TYPES = new HashSet<>(
            Arrays.asList("image/jpeg", "image/png", "image/webp", "image/gif")
    );

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public LocalActMediaUploadResponse uploadCover(MultipartFile file, String scene) {
        validateFile(file);
        String objectKey = buildObjectKey(file.getOriginalFilename(), scene);
        try {
            ensureBucket();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket())
                            .object(objectKey)
                            .contentType(file.getContentType())
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .build()
            );
            LocalActMediaUploadResponse response = new LocalActMediaUploadResponse();
            response.setObjectKey(objectKey);
            response.setUrl(presignGet(objectKey));
            response.setPublicUrl(buildPublicUrl(objectKey));
            response.setContentType(file.getContentType());
            response.setSize(file.getSize());
            return response;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "图片上传失败");
        }
    }

    @Override
    public String resolveCoverUrl(String storedValue) {
        if (!StringUtils.hasText(storedValue)) {
            return storedValue;
        }
        String trimmed = storedValue.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        try {
            return presignGet(trimmed);
        } catch (Exception ignored) {
            return buildPublicUrl(trimmed);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "图片不能为空");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new ResponseStatusException(BAD_REQUEST, "图片大小不能超过5MB");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(BAD_REQUEST, "只支持 JPG/PNG/WebP/GIF 图片");
        }
    }

    private String buildObjectKey(String filename, String scene) {
        String prefix = "story".equalsIgnoreCase(scene) ? "local-act/stories" : "local-act/activities";
        String extension = resolveExtension(filename);
        return prefix + "/" + LocalDate.now().format(DAY_FORMATTER) + "/" + UUID.randomUUID() + extension;
    }

    private String resolveExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return ".jpg";
        }
        String clean = filename.trim().toLowerCase(Locale.ROOT);
        int idx = clean.lastIndexOf('.');
        if (idx < 0 || idx == clean.length() - 1) {
            return ".jpg";
        }
        String ext = clean.substring(idx);
        if (".jpeg".equals(ext) || ".jpg".equals(ext) || ".png".equals(ext) || ".webp".equals(ext) || ".gif".equals(ext)) {
            return ext;
        }
        return ".jpg";
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket()).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket()).build());
        }
    }

    private String presignGet(String objectKey) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(bucket())
                        .object(objectKey)
                        .method(Method.GET)
                        .expiry(7, TimeUnit.DAYS)
                        .build()
        );
    }

    private String buildPublicUrl(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return objectKey;
        }
        String endpoint = StringUtils.hasText(minioProperties.getEndpoint()) ? minioProperties.getEndpoint().trim() : "";
        endpoint = StringUtils.trimTrailingCharacter(endpoint, '/');
        String key = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
        return endpoint + "/" + bucket() + "/" + key;
    }

    private String bucket() {
        if (!StringUtils.hasText(minioProperties.getBucket())) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "MinIO bucket 未配置");
        }
        return minioProperties.getBucket().trim();
    }
}
