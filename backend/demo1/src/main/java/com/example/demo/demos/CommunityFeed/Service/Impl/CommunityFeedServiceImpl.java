package com.example.demo.demos.CommunityFeed.Service.Impl;

import com.example.demo.config.MinioProperties;
import com.example.demo.demos.CommunityFeed.DTO.CommunityFeedPostRequest;
import com.example.demo.demos.CommunityFeed.Dao.CommunityFeedMapper;
import com.example.demo.demos.CommunityFeed.Dao.UserAvatarMapper;
import com.example.demo.demos.CommunityFeed.Pojo.CommunityFeed;
import com.example.demo.demos.CommunityFeed.Pojo.CommunityFeedComment;
import com.example.demo.demos.CommunityFeed.Service.CommunityFeedService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CommunityFeedServiceImpl implements CommunityFeedService {

    private final CommunityFeedMapper communityFeedMapper;
    private final UserAvatarMapper userAvatarMapper;
    private final ObjectMapper objectMapper;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public CommunityFeed createPost(CommunityFeedPostRequest request) {
        validate(request);
        CommunityFeed feed = new CommunityFeed();
        feed.setUserId(request.getUserId());
        feed.setContent(request.getContent().trim());
        feed.setImages(toJsonArray(request.getImages()));
        feed.setVisibility(StringUtils.hasText(request.getVisibility()) ? request.getVisibility() : "PUBLIC");
        feed.setLocationText(StringUtils.hasText(request.getLocationText()) ? request.getLocationText().trim() : null);
        feed.setStatus("ACTIVE");
        int rows = communityFeedMapper.insert(feed);
        if (rows <= 0 || feed.getId() == null) {
            throw new IllegalStateException("??????");
        }
        return feed;
    }

    @Override
    public CommunityFeed getFeed(Long feedId) {
        if (feedId == null || feedId <= 0) {
            throw new IllegalArgumentException("????");
        }
        return communityFeedMapper.findById(feedId);
    }

    @Override
    public List<CommunityFeed> listFeeds(String visibility, int page, int size) {
        int limit = Math.max(1, size);
        int offset = Math.max(0, (Math.max(1, page) - 1) * limit);
        return communityFeedMapper.listFeeds(StringUtils.hasText(visibility) ? visibility : null, limit, offset);
    }

    @Override
    @Transactional
    public void like(Long feedId, Long userId) {
        requireIds(feedId, userId);
        if (communityFeedMapper.existsLike(feedId, userId) != null) {
            return; // ???
        }
        communityFeedMapper.insertLike(feedId, userId);
        communityFeedMapper.updateLikes(feedId, 1);
    }

    @Override
    @Transactional
    public void unlike(Long feedId, Long userId) {
        requireIds(feedId, userId);
        int rows = communityFeedMapper.deleteLike(feedId, userId);
        if (rows > 0) {
            communityFeedMapper.updateLikes(feedId, -1);
        }
    }

    @Override
    @Transactional
    public CommunityFeedComment addComment(Long feedId, Long userId, String content) {
        requireIds(feedId, userId);
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("????????");
        }
        CommunityFeedComment comment = new CommunityFeedComment();
        comment.setFeedId(feedId);
        comment.setUserId(userId);
        comment.setContent(content.trim());
        communityFeedMapper.insertComment(comment);
        communityFeedMapper.updateComments(feedId, 1);
        return comment;
    }

    @Override
    public List<CommunityFeedComment> listComments(Long feedId, int page, int size) {
        if (feedId == null || feedId <= 0) {
            throw new IllegalArgumentException("????");
        }
        int limit = Math.max(1, size);
        int offset = Math.max(0, (Math.max(1, page) - 1) * limit);
        return communityFeedMapper.listComments(feedId, limit, offset);
    }

    @Override
    public List<Long> listLikeUsers(Long feedId, int page, int size) {
        if (feedId == null || feedId <= 0) {
            throw new IllegalArgumentException("????");
        }
        int limit = Math.max(1, size);
        int offset = Math.max(0, (Math.max(1, page) - 1) * limit);
        return communityFeedMapper.listLikeUsers(feedId, limit, offset);
    }

    @Override
    @Transactional
    public boolean deleteFeed(Long feedId, Long userId) {
        if (feedId == null || feedId <= 0 || userId == null || userId <= 0) {
            return false;
        }
        CommunityFeed feed = communityFeedMapper.findById(feedId);
        if (feed == null || !String.valueOf(userId).equals(String.valueOf(feed.getUserId()))) {
            return false;
        }
        int rows = communityFeedMapper.markDeleted(feedId);
        return rows > 0;
    }

    @Override
    @Transactional
    public boolean deleteComment(Long feedId, Long commentId, Long userId) {
        if (commentId == null || commentId <= 0 || userId == null || userId <= 0) {
            return false;
        }
        CommunityFeedComment comment = communityFeedMapper.findCommentById(commentId);
        if (comment == null) {
            return false;
        }
        Long realFeedId = comment.getFeedId();
        // ???????????
        CommunityFeed feed = (realFeedId != null) ? communityFeedMapper.findById(realFeedId) : null;
        boolean canDelete = String.valueOf(userId).equals(String.valueOf(comment.getUserId()))
                || (feed != null && String.valueOf(userId).equals(String.valueOf(feed.getUserId())));
        if (!canDelete) {
            return false;
        }
        int rows = communityFeedMapper.markCommentDeleted(commentId);
        if (rows > 0 && realFeedId != null) {
            communityFeedMapper.updateComments(realFeedId, -1);
            return true;
        }
        return rows > 0;
    }

    private void requireIds(Long feedId, Long userId) {
        if (feedId == null || feedId <= 0 || userId == null || userId <= 0) {
            throw new IllegalArgumentException("????");
        }
    }

    private void validate(CommunityFeedPostRequest request) {
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new IllegalArgumentException("?????");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("??????");
        }
        List<String> images = request.getImages();
        if (!CollectionUtils.isEmpty(images) && images.size() > 9) {
            throw new IllegalArgumentException("???? 9 ???");
        }
    }

    private String toJsonArray(List<String> images) {
        if (CollectionUtils.isEmpty(images)) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(images);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("????????", e);
        }
    }

    @Override
    @Transactional
    public Map<String, String> uploadAvatar(String userId, org.springframework.web.multipart.MultipartFile file) {
        if (userId == null || file == null || file.isEmpty()) {
            throw new IllegalArgumentException("????");
        }
        String objectKey = "avatar/" + userId + "-" + System.currentTimeMillis() + "-" + file.getOriginalFilename();
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .contentType(file.getContentType())
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .build()
            );
            userAvatarMapper.updateAvatarKey(userId, objectKey);
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .method(Method.GET)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
            Map<String, String> resp = new HashMap<>();
            resp.put("key", objectKey);
            resp.put("url", url);
            return resp;
        } catch (Exception e) {
            throw new IllegalStateException("??????", e);
        }
    }
}
