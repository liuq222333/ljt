package com.example.demo.demos.LocalActive.Service.Impl;

import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentActionRecord;
import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentActionRequest;
import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentActionResponse;
import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentItem;
import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentListResponse;
import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentQuery;
import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentRecord;
import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentStats;
import com.example.demo.demos.LocalActive.Dao.LocalActEnrollmentMapper;
import com.example.demo.demos.LocalActive.Dao.LocalActivityMapper;
import com.example.demo.demos.LocalActive.DTO.LocalActivityDetail;
import com.example.demo.demos.LocalActive.Service.LocalActEnrollmentService;
import com.example.demo.demos.Login.Entity.User;
import com.example.demo.demos.Login.Service.LoginService;
import com.example.demo.demos.Notification.Pojo.NotificationMessage;
import com.example.demo.demos.Notification.Service.NotificationSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class LocalActEnrollmentServiceImpl implements LocalActEnrollmentService {

    private final LocalActEnrollmentMapper mapper;
    private final LocalActivityMapper activityMapper;
    private final LoginService loginService;
    private final NotificationSender notificationSender;
    private static final DateTimeFormatter REMINDER_FORMATTER =
            DateTimeFormatter.ofPattern("MM/dd HH:mm", Locale.getDefault());

    @Override
    public LocalActEnrollmentListResponse getUserEnrollments(LocalActEnrollmentQuery query) {
        if (query == null || query.getUsername() == null || query.getUsername().trim().isEmpty()) {
            return new LocalActEnrollmentListResponse(Collections.emptyList(),
                    new LocalActEnrollmentStats(0, 0, 0));
        }
        List<LocalActEnrollmentRecord> records = mapper.findEnrollments(query);
        List<LocalActEnrollmentItem> items = records.stream()
                .map(this::toItem)
                .collect(Collectors.toList());
        LocalDateTime now = LocalDateTime.now();
        int upcoming = (int) records.stream()
                .filter(r -> r.getStartAt() != null && !r.getStartAt().isBefore(now))
                .count();
        int confirmed = (int) records.stream()
                .filter(r -> "confirmed".equalsIgnoreCase(r.getStatus()))
                .count();
        int volunteerHours = confirmed * 2;
        return new LocalActEnrollmentListResponse(items,
                new LocalActEnrollmentStats(upcoming, confirmed, volunteerHours));
    }

    private LocalActEnrollmentItem toItem(LocalActEnrollmentRecord record) {
        LocalActEnrollmentItem item = new LocalActEnrollmentItem();
        item.setId(record.getEnrollmentId());
        item.setActivityId(record.getActivityId());
        item.setTitle(record.getTitle());
        item.setOrganizer(record.getOrganizer());
        item.setLocation(record.getLocation());
        item.setStatus(normalizeStatus(record.getStatus()));
        item.setStartAt(record.getStartAt());
        item.setReminder(buildReminder(record));
        item.setTags(parseTags(record.getTagsCsv()));
        return item;
    }

    @Override
    @Transactional
    public LocalActEnrollmentActionResponse enroll(Long activityId, LocalActEnrollmentActionRequest request) {
        if (activityId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "缺少活动ID");
        }
        if (request == null || !StringUtils.hasText(request.getUsername())) {
            throw new ResponseStatusException(BAD_REQUEST, "请先登录后再报名");
        }

        LocalActivityDetail activity = activityMapper.findDetailById(activityId);
        if (activity == null) {
            throw new ResponseStatusException(NOT_FOUND, "活动不存在");
        }
        if (!"PUBLISHED".equalsIgnoreCase(activity.getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "当前活动不可报名");
        }

        User user = loginService.getUserByName(request.getUsername());
        if (user == null || !StringUtils.hasText(user.getUserId())) {
            throw new ResponseStatusException(NOT_FOUND, "用户不存在");
        }
        Integer userId = parseUserId(user.getUserId());

        LocalActEnrollmentActionRecord existing = mapper.findLatestEnrollment(activityId, userId);
        if (existing != null && isActiveEnrollment(existing.getStatus())) {
            return new LocalActEnrollmentActionResponse(existing.getId(), normalizeStatus(existing.getStatus()), existing.getWaitlistRank());
        }

        String status = resolveEnrollmentStatus(activity);
        Integer waitlistRank = "waitlist".equals(status) ? mapper.countWaitlist(activityId) + 1 : null;
        Long enrollmentId;
        if (existing != null) {
            mapper.updateEnrollmentStatus(existing.getId(), status, waitlistRank);
            enrollmentId = existing.getId();
        } else {
            mapper.insertEnrollment(activityId, userId, status, waitlistRank);
            LocalActEnrollmentActionRecord inserted = mapper.findLatestEnrollment(activityId, userId);
            enrollmentId = inserted == null ? null : inserted.getId();
        }
        sendEnrollNotification(activity, user, status, waitlistRank);
        return new LocalActEnrollmentActionResponse(enrollmentId, normalizeStatus(status), waitlistRank);
    }

    @Override
    @Transactional
    public LocalActEnrollmentActionResponse cancelEnrollment(Long activityId, LocalActEnrollmentActionRequest request) {
        if (activityId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "缺少活动ID");
        }
        if (request == null || !StringUtils.hasText(request.getUsername())) {
            throw new ResponseStatusException(BAD_REQUEST, "请先登录后再取消报名");
        }
        LocalActivityDetail activity = activityMapper.findDetailById(activityId);
        if (activity == null) {
            throw new ResponseStatusException(NOT_FOUND, "活动不存在");
        }

        User user = loginService.getUserByName(request.getUsername());
        if (user == null || !StringUtils.hasText(user.getUserId())) {
            throw new ResponseStatusException(NOT_FOUND, "用户不存在");
        }
        Integer userId = parseUserId(user.getUserId());
        LocalActEnrollmentActionRecord existing = mapper.findLatestEnrollment(activityId, userId);
        if (existing == null || !isActiveEnrollment(existing.getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "没有可取消的报名记录");
        }
        if ("checked_in".equalsIgnoreCase(existing.getStatus()) || "completed".equalsIgnoreCase(existing.getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "已签到或已完成的活动不能取消报名");
        }

        String oldStatus = existing.getStatus();
        Integer oldRank = existing.getWaitlistRank();
        mapper.updateEnrollmentStatus(existing.getId(), "cancelled", null);
        if ("confirmed".equalsIgnoreCase(oldStatus)) {
            promoteFirstWaitlist(activityId);
        } else if ("waitlist".equalsIgnoreCase(oldStatus) && oldRank != null) {
            mapper.decrementWaitlistRanksAfter(activityId, oldRank);
        }
        return new LocalActEnrollmentActionResponse(existing.getId(), "cancelled", null);
    }

    private String normalizeStatus(String status) {
        return status == null ? null : status.toLowerCase(Locale.ROOT);
    }

    private void promoteFirstWaitlist(Long activityId) {
        LocalActEnrollmentActionRecord first = mapper.findFirstWaitlist(activityId);
        if (first == null) {
            return;
        }
        Integer oldRank = first.getWaitlistRank();
        mapper.updateEnrollmentStatus(first.getId(), "confirmed", null);
        if (oldRank != null) {
            mapper.decrementWaitlistRanksAfter(activityId, oldRank);
        }
    }

    private String resolveEnrollmentStatus(LocalActivityDetail activity) {
        int capacity = activity.getCapacity() == null ? 0 : activity.getCapacity();
        if (capacity <= 0) {
            return "confirmed";
        }
        int confirmed = mapper.countConfirmed(activity.getId());
        if (confirmed < capacity) {
            return "confirmed";
        }
        if (Boolean.TRUE.equals(activity.getAllowWaitlist())) {
            return "waitlist";
        }
        throw new ResponseStatusException(BAD_REQUEST, "活动名额已满");
    }

    private boolean isActiveEnrollment(String status) {
        return "confirmed".equalsIgnoreCase(status)
                || "pending".equalsIgnoreCase(status)
                || "waitlist".equalsIgnoreCase(status)
                || "checked_in".equalsIgnoreCase(status)
                || "completed".equalsIgnoreCase(status);
    }

    private Integer parseUserId(String userId) {
        try {
            return Integer.valueOf(userId);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(BAD_REQUEST, "用户ID无效");
        }
    }

    private List<String> parseTags(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String buildReminder(LocalActEnrollmentRecord record) {
        if ("pending".equalsIgnoreCase(record.getStatus())) {
            return "待审核 · 预计 12 小时内反馈";
        }
        if ("waitlist".equalsIgnoreCase(record.getStatus())) {
            String rank = record.getWaitlistRank() != null ? String.valueOf(record.getWaitlistRank()) : "-";
            return String.format("候补第 %s 位 · 有空位自动通知", rank);
        }
        if (record.getStartAt() != null) {
            return "活动前 24 小时提醒 · " + record.getStartAt().format(REMINDER_FORMATTER);
        }
        return "活动提醒";
    }

    private void sendEnrollNotification(LocalActivityDetail activity, User user, String status, Integer waitlistRank) {
        try {
            NotificationMessage msg = new NotificationMessage();
            String t = activity.getTitle() == null ? "" : activity.getTitle();
            if ("confirmed".equals(status)) {
                msg.setKind("LOCAL_ACTIVITY_ENROLLED");
                msg.setTitle("报名成功");
                msg.setContent("您已成功报名活动【" + t + "】");
            } else {
                String rank = waitlistRank != null ? "第 " + waitlistRank + " 位" : "";
                msg.setKind("LOCAL_ACTIVITY_WAITLIST");
                msg.setTitle("已加入候补");
                msg.setContent("活动【" + t + "】名额已满，您已加入候补列表" + rank);
            }
            msg.setTargetType("USER");
            msg.setTargetUserId(parseLong(user.getUserId()));
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
