package com.example.demo.demos.CommunityFeed.Controller;

import com.example.demo.config.MinioProperties;
import com.example.demo.demos.CommunityFeed.DTO.CommunityFeedCommentRequest;
import com.example.demo.demos.CommunityFeed.DTO.CommunityFeedPostRequest;
import com.example.demo.demos.CommunityFeed.DTO.CommunityFeedResponse;
import com.example.demo.demos.CommunityFeed.Pojo.CommunityFeed;
import com.example.demo.demos.CommunityFeed.Pojo.CommunityFeedComment;
import com.example.demo.demos.CommunityFeed.Service.CommunityFeedService;
import com.example.demo.demos.User.Service.UserService;
import com.example.demo.demos.User.Pojo.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/community-feed")
@RequiredArgsConstructor
public class CommunityFeedController {

    private final CommunityFeedService communityFeedService;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Operation(summary = "发布动态")
    @PostMapping("/posts")
    public ResponseEntity<?> createPost(@RequestBody CommunityFeedPostRequest request) {
        CommunityFeed feed = communityFeedService.createPost(request);
        return ResponseEntity.ok(feed);
    }

    @Operation(summary = "列表动态（按时间倒序）")
    @GetMapping("/posts")
    public ResponseEntity<List<CommunityFeedResponse>> listFeeds(
            @RequestParam(value = "visibility", required = false) String visibility,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        List<CommunityFeed> list = communityFeedService.listFeeds(visibility, page, size);
        return ResponseEntity.ok(toResponseList(list));
    }

    @Operation(summary = "动态详情")
    @GetMapping("/posts/{id}")
    public ResponseEntity<?> getFeed(@PathVariable("id") Long id) {
        CommunityFeed feed = communityFeedService.getFeed(id);
        if (feed == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(feed));
    }

    @Operation(summary = "点赞")
    @PostMapping("/posts/{id}/like")
    public ResponseEntity<?> like(@PathVariable("id") Long id, @RequestParam("userId") Long userId) {
        communityFeedService.like(id, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "取消点赞")
    @DeleteMapping("/posts/{id}/like")
    public ResponseEntity<?> unlike(@PathVariable("id") Long id, @RequestParam("userId") Long userId) {
        communityFeedService.unlike(id, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "发表评论")
    @PostMapping("/posts/{id}/comments")
    public ResponseEntity<?> addComment(@PathVariable("id") Long id, @RequestBody CommunityFeedCommentRequest request) {
        if (request == null || request.getUserId() == null || request.getUserId() <= 0 || !StringUtils.hasText(request.getContent())) {
            return ResponseEntity.badRequest().body("参数错误");
        }
        CommunityFeedComment comment = communityFeedService.addComment(id, request.getUserId(), request.getContent());
        return ResponseEntity.ok(comment);
    }

    @Operation(summary = "评论列表")
    @GetMapping("/posts/{id}/comments")
    public ResponseEntity<List<CommunityFeedComment>> listComments(@PathVariable("id") Long id,
                                                                   @RequestParam(value = "page", defaultValue = "1") int page,
                                                                   @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(communityFeedService.listComments(id, page, size));
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable("postId") Long postId,
                                           @PathVariable("commentId") Long commentId,
                                           @RequestParam("userId") Long userId) {
        try {
            log.info("正在删除评论");
            boolean ok = communityFeedService.deleteComment(postId, commentId, userId);
            if (ok) {
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.status(403).body("无权删除或不存在");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("删除评论失败：" + e.getMessage());
        }
    }

    @Operation(summary = "删除动态")
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<?> deletePost(@PathVariable("id") Long id,
                                        @RequestParam("userId") Long userId) {
        boolean ok = communityFeedService.deleteFeed(id, userId);
        if (ok) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(403).body("无权删除或不存在");
    }

    @Operation(summary = "点赞用户列表（仅返回 userId）")
    @GetMapping("/posts/{id}/likes")
    public ResponseEntity<List<Long>> listLikeUsers(@PathVariable("id") Long id,
                                                    @RequestParam(value = "page", defaultValue = "1") int page,
                                                    @RequestParam(value = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(communityFeedService.listLikeUsers(id, page, size));
    }

    @Operation(summary = "上传用户头像（存MinIO，并更新用户表avatar_key）")
    @PostMapping(value = "/avatar/upload", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadAvatar(@RequestParam("userId") String userId,
                                          @RequestParam("file") MultipartFile file) {
        if (userId == null  || file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("参数错误");
        }
        User user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.badRequest().body("用户不存在");
        }
        try {
            return ResponseEntity.ok(communityFeedService.uploadAvatar(userId, file));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("上传失败");
        }
    }

    // ---- helper: convert entity to response with signed image urls ----
    private List<CommunityFeedResponse> toResponseList(List<CommunityFeed> list) {
        List<CommunityFeedResponse> out = new ArrayList<>();
        if (list == null) return out;
        for (CommunityFeed feed : list) {
            CommunityFeedResponse r = toResponse(feed);
            if (r != null) out.add(r);
        }
        return out;
    }

    private CommunityFeedResponse toResponse(CommunityFeed feed) {
        if (feed == null) return null;
        CommunityFeedResponse r = new CommunityFeedResponse();
        r.setId(feed.getId());
        r.setUserId(feed.getUserId());
        r.setContent(feed.getContent());
        r.setVisibility(feed.getVisibility());
        r.setLocationText(feed.getLocationText());
        r.setLikesCount(feed.getLikesCount());
        r.setCommentsCount(feed.getCommentsCount());
        r.setStatus(feed.getStatus());
        r.setCreatedAt(feed.getCreatedAt());
        r.setUpdatedAt(feed.getUpdatedAt());
        r.setImages(buildImageUrls(parseImages(feed.getImages())));
        
        // Map user info
        r.setUsername(feed.getUsername());
        if (StringUtils.hasText(feed.getUserAvatar())) {
            r.setUserAvatar(presignGet(feed.getUserAvatar()));
        }
        
        return r;
    }

    private List<String> parseImages(String raw) {
        if (!StringUtils.hasText(raw)) return new ArrayList<>();
        try {
            return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<String> buildImageUrls(List<String> keys) {
        List<String> urls = new ArrayList<>();
        if (keys == null) return urls;
        for (String key : keys) {
            if (!StringUtils.hasText(key)) continue;
            if (key.startsWith("http://") || key.startsWith("https://")) {
                urls.add(key);
            } else {
                urls.add(presignGet(key));
            }
        }
        return urls;
    }

    private String presignGet(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .method(Method.GET)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            return objectKey; // fallback to key
        }
    }
}
