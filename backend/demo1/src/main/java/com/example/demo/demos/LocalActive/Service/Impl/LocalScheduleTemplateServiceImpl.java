package com.example.demo.demos.LocalActive.Service.Impl;

import com.example.demo.demos.LocalActive.DTO.LocalActScheduleTemplateRequest;
import com.example.demo.demos.LocalActive.DTO.LocalActScheduleTemplateResponse;
import com.example.demo.demos.LocalActive.Dao.LocalActivityScheduleTemplateMapper;
import com.example.demo.demos.LocalActive.Pojo.LocalActivityScheduleTemplate;
import com.example.demo.demos.LocalActive.Service.LocalScheduleTemplateService;
import com.example.demo.demos.Login.Entity.User;
import com.example.demo.demos.Login.Service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class LocalScheduleTemplateServiceImpl implements LocalScheduleTemplateService {

    private final LocalActivityScheduleTemplateMapper templateMapper;
    private final LoginService loginService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public LocalActScheduleTemplateResponse createTemplate(LocalActScheduleTemplateRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername())) {
            throw new ResponseStatusException(BAD_REQUEST, "缺少用户名");
        }
        if (!StringUtils.hasText(request.getTitle()) || request.getWeekday() == null
                || !StringUtils.hasText(request.getStartTime()) || !StringUtils.hasText(request.getEndTime())) {
            throw new ResponseStatusException(BAD_REQUEST, "标题、周几、时间为必填项");
        }

        User user = loginService.getUserByName(request.getUsername());
        if (user == null || !StringUtils.hasText(user.getUserId())) {
            throw new ResponseStatusException(NOT_FOUND, "用户不存在");
        }

        LocalTime start = parseTime(request.getStartTime());
        LocalTime end = parseTime(request.getEndTime());
        if (!end.isAfter(start)) {
            throw new ResponseStatusException(BAD_REQUEST, "结束时间必须大于开始时间");
        }

        LocalActivityScheduleTemplate template = new LocalActivityScheduleTemplate();
        template.setOwnerUserId(parseUserId(user.getUserId()));
        template.setTitle(request.getTitle());
        template.setCategoryCode(request.getCategory());
        template.setWeekday(request.getWeekday());
        template.setStartTime(start);
        template.setEndTime(end);
        template.setLocationText(request.getLocation());
        template.setRecurrenceRule(request.getRecurrenceRule());
        template.setDefaultCapacity(request.getCapacity() == null ? 0 : request.getCapacity());
        template.setDefaultFeeType(resolveFeeType(request.getFeeType()));
        template.setReminderMinutes(request.getReminderMinutes() == null ? 1440 : request.getReminderMinutes());
        template.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "ACTIVE");

        int rows = templateMapper.insertTemplate(template);
        if (rows <= 0 || template.getId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "创建模板失败");
        }
        return new LocalActScheduleTemplateResponse(template.getId(), template.getStatus());
    }

    private Integer parseUserId(String userId) {
        try {
            return Integer.valueOf(userId);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(BAD_REQUEST, "用户ID无效");
        }
    }

    private LocalTime parseTime(String time) {
        try {
            return LocalTime.parse(time, TIME_FORMATTER);
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, "时间格式应为HH:mm");
        }
    }

    private String resolveFeeType(String feeType) {
        if (!StringUtils.hasText(feeType)) {
            return "FREE";
        }
        if ("AA".equalsIgnoreCase(feeType)) return "AA";
        if ("PAID".equalsIgnoreCase(feeType)) return "PAID";
        return "FREE";
    }
}
