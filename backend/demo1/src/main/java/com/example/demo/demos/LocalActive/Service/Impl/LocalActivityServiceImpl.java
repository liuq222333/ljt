package com.example.demo.demos.LocalActive.Service.Impl;

import com.example.demo.demos.LocalActive.DTO.LocalActCreateRequest;
import com.example.demo.demos.LocalActive.DTO.LocalActCreateResponse;
import com.example.demo.demos.LocalActive.Dao.LocalActivityMapper;
import com.example.demo.demos.LocalActive.Pojo.LocalActivity;
import com.example.demo.demos.LocalActive.Service.LocalActivityService;
import com.example.demo.demos.Login.Entity.User;
import com.example.demo.demos.Login.Service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class LocalActivityServiceImpl implements LocalActivityService {

    private final LocalActivityMapper activityMapper;
    private final LoginService loginService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public LocalActCreateResponse createActivity(LocalActCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername())) {
            throw new ResponseStatusException(BAD_REQUEST, "缺少用户名");
        }
        User organizer = loginService.getUserByName(request.getUsername());
        if (organizer == null || !StringUtils.hasText(organizer.getUserId())) {
            throw new ResponseStatusException(NOT_FOUND, "无法找到发布者信息");
        }
        if ("PUBLISHED".equalsIgnoreCase(request.getStatus())) {
            if (!StringUtils.hasText(request.getTitle())
                    || !StringUtils.hasText(request.getCategory())
                    || !StringUtils.hasText(request.getDate())
                    || !StringUtils.hasText(request.getTimeStart())
                    || !StringUtils.hasText(request.getTimeEnd())
                    || !StringUtils.hasText(request.getLocation())
                    || !StringUtils.hasText(request.getDescription())) {
                throw new ResponseStatusException(BAD_REQUEST, "缺少必填项");
            }
        }
        LocalActivity activity = buildActivity(request, organizer);
        int rows = activityMapper.insertActivity(activity);
        //如果插入失败，则返回错误插入失败，则抛出异常
        if (rows <= 0 || activity.getId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "活动创建失败");
        }
        insertTags(activity.getId(), request.getTags());
        return new LocalActCreateResponse(activity.getId(), activity.getStatus());
    }

    // 插入标签
    private void insertTags(Long activityId, List<String> tags) {
        if (activityId == null || tags == null || tags.isEmpty()) {
            return;
        }
        activityMapper.insertTags(activityId, tags);
    }

    private LocalActivity buildActivity(LocalActCreateRequest request, User organizer) {
        LocalActivity activity = new LocalActivity();
        activity.setOrganizerUserId(parseUserId(organizer.getUserId()));
        activity.setTitle(request.getTitle());
        activity.setSubtitle(request.getSubtitle());
        activity.setCategoryCode(request.getCategory());
        activity.setDescription(request.getDescription());
        activity.setLocationText(request.getLocation());
        activity.setCapacity(request.getCapacity() == null ? 0 : request.getCapacity());
        activity.setFeeType(resolveFeeType(request.getFee()));
        activity.setFeeAmount(resolveFeeAmount(request.getFee()));
        activity.setAllowWaitlist("yes".equalsIgnoreCase(request.getWaiting()));
        activity.setRequireCheckin(!"no".equalsIgnoreCase(request.getCheckin()));
        activity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "DRAFT");
        activity.setStartAt(resolveDateTime(request.getDate(), request.getTimeStart()));
        activity.setEndAt(resolveDateTime(request.getDate(), request.getTimeEnd()));
        activity.setReminderMinutes(resolveReminder(request.getReminder()));
        activity.setReviewNote(request.getReviewNote());
        return activity;
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
    public List<LocalActivity> listActivities(String status, int page, int size) {
        int limit = Math.max(1, size);
        int offset = Math.max(0, (Math.max(1, page) - 1) * limit);
        return activityMapper.listActivities(StringUtils.hasText(status) ? status : null, limit, offset);
    }
}
