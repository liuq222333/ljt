package com.example.demo.demos.CommunityFeed.Dao;

import com.example.demo.demos.CommunityFeed.Pojo.CommunityFeed;
import com.example.demo.demos.CommunityFeed.Pojo.CommunityFeedComment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CommunityFeedMapper {

    @Insert("INSERT INTO community_feed(user_id, content, images, visibility, location_text, status, created_at, updated_at) " +
            "VALUES(#{userId}, #{content}, #{images}, #{visibility}, #{locationText}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CommunityFeed feed);

    @Select({
            "<script>",
            "SELECT f.id, f.user_id AS userId, f.content, t.name,f.images, f.visibility, f.location_text AS locationText,",
            "       f.likes_count AS likesCount, f.comments_count AS commentsCount, f.status, f.created_at AS createdAt, f.updated_at AS updatedAt,",
            "       u.username AS username, u.avatar_key AS userAvatar",
            "  FROM community_feed f",
            "  LEFT JOIN users u ON f.user_id = u.user_id",
            "LEFT JOIN community_feed_topic ft ON ft.feed_id = f.id",
            "LEFT JOIN topic t ON ft.topic_id = t.id",
            " WHERE f.status = 'ACTIVE'",
            " <if test='visibility != null and visibility != \"\"'> AND f.visibility = #{visibility} </if>",
            " ORDER BY f.created_at DESC",
            " LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<CommunityFeed> listFeeds(@Param("visibility") String visibility, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT f.id, f.user_id AS userId, f.content, f.images, t.name, f.visibility, f.location_text AS locationText, " +
            "f.likes_count AS likesCount, f.comments_count AS commentsCount, f.status, f.created_at AS createdAt, f.updated_at AS updatedAt " +
            "FROM community_feed f " +
            "LEFT JOIN community_feed_topic ft ON ft.feed_id = f.id " +
            "LEFT JOIN topic t ON ft.topic_id = t.id " +
            "WHERE f.id = #{id} AND f.status = 'ACTIVE'")
    CommunityFeed findById(@Param("id") Long id);

    @Select("SELECT 1 FROM community_feed_like WHERE feed_id = #{feedId} AND user_id = #{userId} LIMIT 1")
    Integer existsLike(@Param("feedId") Long feedId, @Param("userId") Long userId);

    @Insert("INSERT INTO community_feed_like(feed_id, user_id, created_at) VALUES(#{feedId}, #{userId}, NOW())")
    int insertLike(@Param("feedId") Long feedId, @Param("userId") Long userId);

    @Delete("DELETE FROM community_feed_like WHERE feed_id = #{feedId} AND user_id = #{userId}")
    int deleteLike(@Param("feedId") Long feedId, @Param("userId") Long userId);

    @Update("UPDATE community_feed SET likes_count = likes_count + #{delta}, updated_at = NOW() WHERE id = #{feedId}")
    int updateLikes(@Param("feedId") Long feedId, @Param("delta") int delta);

    @Select("SELECT user_id FROM community_feed_like WHERE feed_id = #{feedId} ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Long> listLikeUsers(@Param("feedId") Long feedId, @Param("limit") int limit, @Param("offset") int offset);

    @Insert("INSERT INTO community_feed_comment(feed_id, user_id, content, status, created_at) VALUES(#{feedId}, #{userId}, #{content}, 'ACTIVE', NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertComment(CommunityFeedComment comment);

    @Select("SELECT id, feed_id AS feedId, user_id AS userId, content, status, created_at AS createdAt FROM community_feed_comment WHERE feed_id = #{feedId} AND status = 'ACTIVE' ORDER BY created_at ASC LIMIT #{limit} OFFSET #{offset}")
    List<CommunityFeedComment> listComments(@Param("feedId") Long feedId, @Param("limit") int limit, @Param("offset") int offset);

    @Update("UPDATE community_feed SET comments_count = comments_count + #{delta}, updated_at = NOW() WHERE id = #{feedId}")
    int updateComments(@Param("feedId") Long feedId, @Param("delta") int delta);

    @Update("UPDATE community_feed SET status = 'DELETED', updated_at = NOW() WHERE id = #{feedId}")
    int markDeleted(@Param("feedId") Long feedId);

    @Select("SELECT id, feed_id AS feedId, user_id AS userId, content, status, created_at AS createdAt FROM community_feed_comment WHERE id = #{id}")
    CommunityFeedComment findCommentById(@Param("id") Long id);

    @Update("UPDATE community_feed_comment SET status = 'DELETED' WHERE id = #{id}")
    int markCommentDeleted(@Param("id") Long id);
}
