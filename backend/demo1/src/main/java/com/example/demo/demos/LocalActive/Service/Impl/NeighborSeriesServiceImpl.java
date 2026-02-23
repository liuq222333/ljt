package com.example.demo.demos.LocalActive.Service.Impl;

import com.example.demo.demos.LocalActive.DTO.LocalStoryDetail;
import com.example.demo.demos.LocalActive.DTO.LocalStoryListItem;
import com.example.demo.demos.LocalActive.DTO.LocalStoryCreateRequest;
import com.example.demo.demos.LocalActive.Dao.LocalActivityStoryMapper;
import com.example.demo.demos.LocalActive.Pojo.LocalActivityStory;
import com.example.demo.demos.LocalActive.Service.NeighborSeriesService;
import com.example.demo.demos.Login.Entity.User;
import com.example.demo.demos.Login.Service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class NeighborSeriesServiceImpl implements NeighborSeriesService {

    private final LocalActivityStoryMapper storyMapper;
    private final LoginService loginService;

    @Override
    public List<LocalStoryListItem> listStories(String keyword, String visibility, int page, int size) {
        int limit = Math.max(1, size);
        int offset = Math.max(0, (Math.max(1, page) - 1) * limit);
        return storyMapper.listStories(StringUtils.hasText(keyword) ? keyword : null,
                StringUtils.hasText(visibility) ? visibility : null, limit, offset);
    }

    @Override
    public LocalStoryDetail getStoryDetail(Long id) {
        LocalStoryDetail detail = storyMapper.findById(id);
        if (detail == null) {
            throw new ResponseStatusException(NOT_FOUND, "故事不存在");
        }
        return detail;
    }

    @Override
    public Long createStory(LocalStoryCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername())) {
            throw new ResponseStatusException(NOT_FOUND, "缺少用户名");
        }
        if (!StringUtils.hasText(request.getTitle()) || !StringUtils.hasText(request.getContent())) {
            throw new ResponseStatusException(NOT_FOUND, "标题和内容不能为空");
        }
        User user = loginService.getUserByName(request.getUsername());
        if (user == null || !StringUtils.hasText(user.getUserId())) {
            throw new ResponseStatusException(NOT_FOUND, "用户不存在");
        }

        if (StringUtils.hasText(request.getCoverUrl()) && request.getCoverUrl().length() > 500) {
            throw new ResponseStatusException(NOT_FOUND, "封面链接过长（<=500字符）");
        }
        if (StringUtils.hasText(request.getSummary()) && request.getSummary().length() > 300) {
            throw new ResponseStatusException(NOT_FOUND, "摘要过长（<=300字符）");
        }
        if (StringUtils.hasText(request.getTitle()) && request.getTitle().length() > 150) {
            throw new ResponseStatusException(NOT_FOUND, "标题过长（<=150字符）");
        }

        LocalActivityStory story = new LocalActivityStory();
        story.setActivityId(request.getActivityId());
        story.setAuthorUserId(parseUserId(user.getUserId()));
        story.setTitle(request.getTitle());
        story.setCoverUrl(request.getCoverUrl());
        story.setSummary(request.getSummary());
        story.setContent(request.getContent());
        story.setVisibility(StringUtils.hasText(request.getVisibility()) ? request.getVisibility() : "PUBLIC");
        story.setLikes(0);

        int rows = storyMapper.insertStory(story);
        if (rows <= 0 || story.getId() == null) {
            throw new ResponseStatusException(NOT_FOUND, "创建故事失败");
        }
        return story.getId();
    }

    private Integer parseUserId(String userId) {
        try {
            return Integer.valueOf(userId);
        } catch (Exception e) {
            throw new ResponseStatusException(NOT_FOUND, "用户ID无效");
        }
    }
}
