package com.example.demo.demos.LocalActive.Service.Impl;

import com.example.demo.demos.LocalActive.DTO.LocalActCreateRequest;
import com.example.demo.demos.LocalActive.DTO.LocalActCreateResponse;
import com.example.demo.demos.LocalActive.DTO.LocalActivityDetail;
import com.example.demo.demos.LocalActive.Dao.LocalActivityMapper;
import com.example.demo.demos.LocalActive.Pojo.LocalActivity;
import com.example.demo.demos.LocalActive.Service.LocalActMediaService;
import com.example.demo.demos.LocalActive.Service.LocalActivityService;
import com.example.demo.demos.Login.Entity.User;
import com.example.demo.demos.Login.Service.LoginService;
import com.example.demo.demos.Notification.Pojo.NotificationMessage;
import com.example.demo.demos.Notification.Service.NotificationSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class LocalActivityServiceImpl implements LocalActivityService {

    private final LocalActivityMapper activityMapper;
    private final LoginService loginService;
    private final LocalActMediaService mediaService;
    private final NotificationSender notificationSender;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_REVIEWING = "REVIEWING";
    private static final String STATUS_PUBLISHED = "PUBLISHED";

    @Override
    public LocalActCreateResponse createActivity(LocalActCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername())) {
            throw new ResponseStatusException(BAD_REQUEST, "缺少用户名");
        }
        User organizer = loginService.getUserByName(request.getUsername());
        if (organizer == null || !StringUtils.hasText(organizer.getUserId())) {
            throw new ResponseStatusException(NOT_FOUND, "无法找到发布者信息");
        }
        String targetStatus = resolveCreateStatus(request.getStatus());
        if (STATUS_REVIEWING.equals(targetStatus)) {
            List<String> missingFields = new ArrayList<>();
            addMissing(missingFields, request.getTitle(), "活动名称");
            addMissing(missingFields, request.getCategory(), "活动分类");
            addMissing(missingFields, request.getDate(), "活动日期");
            addMissing(missingFields, request.getTimeStart(), "开始时间");
            addMissing(missingFields, request.getTimeEnd(), "结束时间");
            addMissing(missingFields, request.getLocation(), "活动地点");
            addMissing(missingFields, request.getDescription(), "活动描述");
            addMissing(missingFields, request.getCoverUrl(), "封面图片 URL");
            if (!missingFields.isEmpty()) {
                throw new ResponseStatusException(BAD_REQUEST, "缺少必填项：" + String.join("、", missingFields));
            }
        }
        LocalActivity activity = buildActivity(request, organizer, targetStatus);
        int rows = activityMapper.insertActivity(activity);
        //如果插入失败，则返回错误插入失败，则抛出异常
        if (rows <= 0 || activity.getId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "活动创建失败");
        }
        insertTags(activity.getId(), request.getTags());
        sendCreatedNotification(activity, organizer);
        return new LocalActCreateResponse(activity.getId(), activity.getStatus());
    }

    // 插入标签
    private void insertTags(Long activityId, List<String> tags) {
        if (activityId == null || tags == null || tags.isEmpty()) {
            return;
        }
        activityMapper.insertTags(activityId, tags);
    }

    private LocalActivity buildActivity(LocalActCreateRequest request, User organizer, String targetStatus) {
        LocalActivity activity = new LocalActivity();
        activity.setOrganizerUserId(parseUserId(organizer.getUserId()));
        activity.setTitle(request.getTitle());
        activity.setSubtitle(request.getSubtitle());
        activity.setCategoryCode(request.getCategory());
        activity.setDescription(request.getDescription());
        activity.setLocationText(request.getLocation());
        activity.setAddress(StringUtils.hasText(request.getAddress()) ? request.getAddress() : request.getLocation());
        activity.setLatitude(request.getLatitude());
        activity.setLongitude(request.getLongitude());
        activity.setCoverUrl(request.getCoverUrl());
        activity.setCapacity(request.getCapacity() == null ? 0 : request.getCapacity());
        activity.setFeeType(resolveFeeType(request.getFee()));
        activity.setFeeAmount(resolveFeeAmount(request.getFee()));
        activity.setAllowWaitlist("yes".equalsIgnoreCase(request.getWaiting()));
        activity.setRequireCheckin(!"no".equalsIgnoreCase(request.getCheckin()));
        activity.setStatus(targetStatus);
        activity.setStartAt(resolveDateTime(request.getDate(), request.getTimeStart()));
        activity.setEndAt(resolveDateTime(request.getDate(), request.getTimeEnd()));
        activity.setReminderMinutes(resolveReminder(request.getReminder()));
        activity.setReviewNote(request.getReviewNote());
        return activity;
    }

    private String resolveCreateStatus(String status) {
        if (!StringUtils.hasText(status) || STATUS_DRAFT.equalsIgnoreCase(status)) {
            return STATUS_DRAFT;
        }
        if (STATUS_REVIEWING.equalsIgnoreCase(status)
                || STATUS_PUBLISHED.equalsIgnoreCase(status)
                || "PENDING_REVIEW".equalsIgnoreCase(status)) {
            return STATUS_REVIEWING;
        }
        throw new ResponseStatusException(BAD_REQUEST, "活动状态无效");
    }

    private void addMissing(List<String> missingFields, String value, String label) {
        if (!StringUtils.hasText(value)) {
            missingFields.add(label);
        }
    }

    // 解析用户 ID
    private Integer parseUserId(String userId) {
        try {
            return Integer.valueOf(userId);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(BAD_REQUEST, "发布者 ID 无效");
        }
    }

    // 解析费用类型
    private String resolveFeeType(String fee) {
        if (!StringUtils.hasText(fee) || "免费".equals(fee)) return "FREE";
        if ("AA".equalsIgnoreCase(fee)) return "AA";
        return "PAID";
    }

    // 解析费用金额

    private BigDecimal resolveFeeAmount(String fee) {
        if (!StringUtils.hasText(fee)) return BigDecimal.ZERO;
        try {
            return new BigDecimal(fee.replace("￥", "").replace("¥", "").trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDateTime resolveDateTime(String date, String time) {
        if (!StringUtils.hasText(date)) {
            return LocalDateTime.now();
        }
        LocalDate localDate = LocalDate.parse(date);
        LocalTime localTime = StringUtils.hasText(time) ? LocalTime.parse(time, TIME_FORMATTER) : LocalTime.of(0, 0);
        return LocalDateTime.of(localDate, localTime);
    }


    // 解析提醒时间
    private Integer resolveReminder(String reminder) {
        if ("3h".equalsIgnoreCase(reminder)) return 180;
        if ("24h".equalsIgnoreCase(reminder)) return 1440;
        return 60;
    }

    @Override
    public List<LocalActivity> listActivities(String status, String timeState, int page, int size) {
        int limit = Math.max(1, size);
        int offset = Math.max(0, (Math.max(1, page) - 1) * limit);
        List<LocalActivity> activities = activityMapper.listActivities(
                StringUtils.hasText(status) ? status : null,
                normalizeTimeState(timeState),
                limit,
                offset);
        resolveActivityCovers(activities);
        return activities;
    }

    @Override
    public List<LocalActivity> listMyActivities(String username, String status, String timeState, int page, int size) {
        Integer userId = resolveUserId(username);
        int limit = Math.max(1, size);
        int offset = Math.max(0, (Math.max(1, page) - 1) * limit);
        List<LocalActivity> activities = activityMapper.listActivitiesByOrganizer(
                userId,
                StringUtils.hasText(status) ? status : null,
                normalizeTimeState(timeState),
                limit,
                offset);
        resolveActivityCovers(activities);
        return activities;
    }

    @Override
    public List<LocalActivity> listFavoriteActivities(String username, int page, int size) {
        Integer userId = resolveUserId(username);
        int limit = Math.max(1, size);
        int offset = Math.max(0, (Math.max(1, page) - 1) * limit);
        List<LocalActivity> activities = activityMapper.listFavoriteActivities(userId, limit, offset);
        resolveActivityCovers(activities);
        return activities;
    }

    private String normalizeTimeState(String timeState) {
        if (!StringUtils.hasText(timeState)) {
            return null;
        }
        String normalized = timeState.trim().toLowerCase();
        if ("ongoing".equals(normalized) || "running".equals(normalized) || "current".equals(normalized)
                || "进行中".equals(normalized) || "正在进行".equals(normalized)) {
            return "ongoing";
        }
        if ("upcoming".equals(normalized) || "future".equals(normalized)
                || "即将开始".equals(normalized) || "未开始".equals(normalized)) {
            return "upcoming";
        }
        if ("ended".equals(normalized) || "past".equals(normalized) || "finished".equals(normalized)
                || "已结束".equals(normalized) || "结束".equals(normalized)) {
            return "ended";
        }
        return null;
    }

    @Override
    public LocalActivityDetail getActivityDetail(Long id, String username) {
        if (id == null) {
            throw new ResponseStatusException(BAD_REQUEST, "缺少活动ID");
        }
        LocalActivityDetail detail = activityMapper.findDetailById(id);
        if (detail == null) {
            throw new ResponseStatusException(NOT_FOUND, "活动不存在");
        }
        detail.setTags(activityMapper.listTags(id));
        detail.setCoverUrl(mediaService.resolveCoverUrl(detail.getCoverUrl()));
        if (StringUtils.hasText(username)) {
            detail.setEnrollmentStatus(normalizeStatus(activityMapper.findUserEnrollmentStatus(id, username)));
            try {
                Integer userId = resolveUserId(username);
                detail.setFavorited(activityMapper.countFavorite(id, userId) > 0);
            } catch (ResponseStatusException ignored) {
                detail.setFavorited(false);
            }
        } else {
            detail.setFavorited(false);
        }
        return detail;
    }

    @Override
    public void favoriteActivity(Long id, String username) {
        if (id == null) {
            throw new ResponseStatusException(BAD_REQUEST, "缺少活动ID");
        }
        if (activityMapper.findDetailById(id) == null) {
            throw new ResponseStatusException(NOT_FOUND, "活动不存在");
        }
        activityMapper.insertFavorite(id, resolveUserId(username));
    }

    @Override
    public void unfavoriteActivity(Long id, String username) {
        if (id == null) {
            throw new ResponseStatusException(BAD_REQUEST, "缺少活动ID");
        }
        activityMapper.deleteFavorite(id, resolveUserId(username));
    }

    private Integer resolveUserId(String username) {
        if (!StringUtils.hasText(username)) {
            throw new ResponseStatusException(BAD_REQUEST, "缺少用户名");
        }
        User user = loginService.getUserByName(username);
        if (user == null || !StringUtils.hasText(user.getUserId())) {
            throw new ResponseStatusException(NOT_FOUND, "用户不存在");
        }
        return parseUserId(user.getUserId());
    }

    private String normalizeStatus(String status) {
        return status == null ? null : status.toLowerCase(Locale.ROOT);
    }

    private void resolveActivityCovers(List<LocalActivity> activities) {
        if (activities == null || activities.isEmpty()) {
            return;
        }
        for (LocalActivity activity : activities) {
            if (activity != null) {
                activity.setCoverUrl(mediaService.resolveCoverUrl(activity.getCoverUrl()));
            }
        }
    }

    private void sendCreatedNotification(LocalActivity activity, User organizer) {
        try {
            NotificationMessage msg = new NotificationMessage();
            if (STATUS_REVIEWING.equals(activity.getStatus())) {
                msg.setKind("LOCAL_ACTIVITY_SUBMITTED");
                msg.setTitle("活动已提交审核");
                String t = activity.getTitle() == null ? "" : activity.getTitle();
                msg.setContent("您的活动【" + t + "】已提交审核，请等待管理员审核");
            } else {
                msg.setKind("LOCAL_ACTIVITY_DRAFT");
                msg.setTitle("活动草稿已保存");
                String t = activity.getTitle() == null ? "" : activity.getTitle();
                msg.setContent("您的活动【" + t + "】已保存为草稿");
            }
            msg.setTargetType("USER");
            msg.setTargetUserId(parseLong(organizer.getUserId()));
            msg.setPriority(5);
            notificationSender.send(msg);
        } catch (Exception ignore) {
            // 通知发送失败不影响主流程
        }
    }

    private Long parseLong(String val) {
        try {
            return Long.valueOf(val);
        } catch (Exception e) {
            return null;
        }
    }
}
