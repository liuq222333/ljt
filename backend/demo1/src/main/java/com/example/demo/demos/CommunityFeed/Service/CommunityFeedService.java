package com.example.demo.demos.CommunityFeed.Service;

import com.example.demo.demos.CommunityFeed.DTO.CommunityFeedPostRequest;
import com.example.demo.demos.CommunityFeed.Pojo.CommunityFeed;
import com.example.demo.demos.CommunityFeed.Pojo.CommunityFeedComment;
import java.util.List;
import java.util.Map;

public interface CommunityFeedService {
    CommunityFeed createPost(CommunityFeedPostRequest request);

    CommunityFeed getFeed(Long feedId);

    /**
     * ????????????????? ACTIVE?visibility ???
     */
    List<CommunityFeed> listFeeds(String visibility, int page, int size);

    void like(Long feedId, Long userId);

    void unlike(Long feedId, Long userId);

    CommunityFeedComment addComment(Long feedId, Long userId, String content);

    List<CommunityFeedComment> listComments(Long feedId, int page, int size);

    List<Long> listLikeUsers(Long feedId, int page, int size);

    /**
     * ???????????
     * @return true ?????false ??????
     */
    boolean deleteFeed(Long feedId, Long userId);

    /**
     * ?????????????????
     * @return true ?????false ??????
     */
    boolean deleteComment(Long feedId, Long commentId, Long userId);

    /**
     * ??????? MinIO?????? avatar_key??? {key, url}
     */
    Map<String, String> uploadAvatar(String userId, org.springframework.web.multipart.MultipartFile file);
}
