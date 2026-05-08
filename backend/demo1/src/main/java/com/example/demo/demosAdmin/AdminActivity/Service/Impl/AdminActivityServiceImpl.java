package com.example.demo.demosAdmin.AdminActivity.Service.Impl;

import com.example.demo.demos.LocalActive.Pojo.LocalActivity;
import com.example.demo.demos.LocalActive.Dao.LocalActivityMapper;
import com.example.demo.demos.LocalActive.Service.LocalActMediaService;
import com.example.demo.demosAdmin.AdminActivity.Dao.AdminActivityMapper;
import com.example.demo.demosAdmin.AdminActivity.Service.AdminActivityService;
import com.example.demo.demos.Notification.Pojo.NotificationMessage;
import com.example.demo.demos.Notification.Service.NotificationSender;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminActivityServiceImpl implements AdminActivityService {

    private final AdminActivityMapper adminActivityMapper;
    private final LocalActivityMapper localActivityMapper;
    private final LocalActMediaService mediaService;
    private final NotificationSender notificationSender;

    private static final String STATUS_REVIEWING = "REVIEWING";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    @Override
    public List<LocalActivity> listReviews(String status, String keyword, int page, int size) {
        int limit = Math.max(1, size);
        int offset = Math.max(0, (Math.max(1, page) - 1) * limit);
        String normalizedStatus = normalizeReviewStatus(status);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        List<LocalActivity> activities = localActivityMapper.listActivities(normalizedStatus, null, limit, offset);
        if (StringUtils.hasText(normalizedKeyword)) {
            String loweredKeyword = normalizedKeyword.toLowerCase(Locale.ROOT);
            activities = localActivityMapper.listActivities(normalizedStatus, null, 500, 0).stream()
                    .filter(activity -> containsKeyword(activity, loweredKeyword))
                    .skip(offset)
                    .limit(limit)
                    .collect(Collectors.toList());
        }
        resolveCovers(activities);
        return activities;
    }

    @Override
    @Transactional
    public Long approve(Long adminActivityId, String note) {
        LocalActivity activity = findActivityOrThrow(adminActivityId);
        int rows = adminActivityMapper.updateActivityStatus(
                adminActivityId,
                STATUS_PUBLISHED,
                StringUtils.hasText(note) ? note : activity.getReviewNote()
        );
        if (rows <= 0) {
            throw new IllegalStateException("活动审核通过失败");
        }
        activity.setStatus(STATUS_PUBLISHED);
        try {
            if (activity.getOrganizerUserId() != null) {
                NotificationMessage msg = new NotificationMessage();
                msg.setKind("LOCAL_ACTIVITY_REVIEW_APPROVED");
                msg.setTitle("活动审核通过");
                String t = activity.getTitle() == null ? "" : activity.getTitle();
                msg.setContent("您的活动【" + t + "】已审核通过并发布");
                msg.setTargetType("USER");
                msg.setTargetUserId(Long.valueOf(activity.getOrganizerUserId()));
                msg.setPriority(5);
                notificationSender.send(msg);
            }
        } catch (Exception ignore) {}
        return activity.getId();
    }

    @Override
    @Transactional
    public void reject(Long adminActivityId, String note) {
        LocalActivity activity = findActivityOrThrow(adminActivityId);
        int rows = adminActivityMapper.updateActivityStatus(
                adminActivityId,
                STATUS_CANCELLED,
                StringUtils.hasText(note) ? note : activity.getReviewNote()
        );
        if (rows <= 0) {
            throw new IllegalStateException("活动审核驳回失败");
        }
        try {
            if (activity.getOrganizerUserId() != null) {
                NotificationMessage msg = new NotificationMessage();
                msg.setKind("LOCAL_ACTIVITY_REVIEW_REJECTED");
                msg.setTitle("活动审核未通过");
                String t = activity.getTitle() == null ? "" : activity.getTitle();
                String n = StringUtils.hasText(note) ? note : "";
                msg.setContent("您的活动【" + t + "】未通过审核" + (StringUtils.hasText(n) ? ("，原因：" + n) : ""));
                msg.setTargetType("USER");
                msg.setTargetUserId(Long.valueOf(activity.getOrganizerUserId()));
                msg.setPriority(4);
                notificationSender.send(msg);
            }
        } catch (Exception ignore) {}
    }

    private LocalActivity findActivityOrThrow(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少活动ID");
        }
        LocalActivity activity = adminActivityMapper.findActivityById(id);
        if (activity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "活动不存在");
        }
        return activity;
    }

    private String normalizeReviewStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        if ("ALL".equalsIgnoreCase(status)) {
            return null;
        }
        if ("PENDING_REVIEW".equalsIgnoreCase(status)) {
            return STATUS_REVIEWING;
        }
        return status;
    }

    private boolean containsKeyword(LocalActivity activity, String loweredKeyword) {
        if (activity == null || !StringUtils.hasText(loweredKeyword)) {
            return true;
        }
        return contains(activity.getTitle(), loweredKeyword)
                || contains(activity.getDescription(), loweredKeyword)
                || contains(activity.getSubtitle(), loweredKeyword)
                || contains(activity.getLocationText(), loweredKeyword)
                || contains(activity.getAddress(), loweredKeyword);
    }

    private boolean contains(String value, String loweredKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(loweredKeyword);
    }

    private void resolveCovers(List<LocalActivity> activities) {
        if (activities == null || activities.isEmpty()) {
            return;
        }
        for (LocalActivity activity : activities) {
            if (activity != null) {
                activity.setCoverUrl(mediaService.resolveCoverUrl(activity.getCoverUrl()));
            }
        }
    }
}
