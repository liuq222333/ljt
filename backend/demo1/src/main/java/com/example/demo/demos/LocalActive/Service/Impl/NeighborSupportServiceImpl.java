package com.example.demo.demos.LocalActive.Service.Impl;

import com.example.demo.demos.LocalActive.DTO.NeighborSupportTaskDTO;
import com.example.demo.demos.LocalActive.DTO.NeighborSupportTaskRequest;
import com.example.demo.demos.LocalActive.DTO.NeighborSupportTaskResponse;
import com.example.demo.demos.LocalActive.Dao.NeighborSupportTaskMapper;
import com.example.demo.demos.LocalActive.Pojo.NeighborSupportTask;
import com.example.demo.demos.LocalActive.Service.NeighborSupportService;
import com.example.demo.demos.Login.Entity.User;
import com.example.demo.demos.Login.Service.LoginService;
import com.example.demo.demos.Notification.Service.NotificationSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class NeighborSupportServiceImpl implements NeighborSupportService {

    private final NeighborSupportTaskMapper taskMapper;
    private final LoginService loginService;
    private final NotificationSender notificationSender;

    @Override
    public NeighborSupportTaskResponse createTask(NeighborSupportTaskRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername())) {
            throw new ResponseStatusException(BAD_REQUEST, "缺少用户名");
        }
        if (!StringUtils.hasText(request.getTitle()) || !StringUtils.hasText(request.getCategory())) {
            throw new ResponseStatusException(BAD_REQUEST, "标题与分类为必填项");
        }

        User requester = loginService.getUserByName(request.getUsername());
        if (requester == null || !StringUtils.hasText(requester.getUserId())) {
            throw new ResponseStatusException(NOT_FOUND, "用户不存在");
        }

        NeighborSupportTask task = new NeighborSupportTask();
        task.setRequesterUserId(parseUserId(requester.getUserId()));
        task.setTitle(request.getTitle());
        task.setCategoryCode(request.getCategory());
        task.setDescription(request.getDescription());
        task.setLocationText(request.getLocation());
        task.setLatitude(request.getLatitude());
        task.setLongitude(request.getLongitude());
        task.setStartTime(parseDateTime(request.getStartTime()));
        task.setEndTime(parseDateTime(request.getEndTime()));
        task.setVolunteerSlots(request.getVolunteerSlots() == null ? 1 : request.getVolunteerSlots());
        task.setFilledSlots(0);
        task.setPriority(resolvePriority(request.getPriority()));
        task.setRewardPoints(request.getRewardPoints() == null ? 0 : request.getRewardPoints());
        task.setStatus("OPEN");

        int rows = taskMapper.insertTask(task);
        if (rows <= 0 || task.getId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "创建互助任务失败");
        }
        sendCreatedNotification(task, requester);
        return new NeighborSupportTaskResponse(task.getId(), task.getStatus());
    }

    @Override
    public List<NeighborSupportTaskDTO> listTasks(String status) {
        return taskMapper.listTasks(status);
    }

    private void sendCreatedNotification(NeighborSupportTask task, User requester) {
        try {
            com.example.demo.demos.Notification.Pojo.NotificationMessage msg = new com.example.demo.demos.Notification.Pojo.NotificationMessage();
            msg.setKind("NEIGHBOR_SUPPORT_CREATED");
            msg.setTitle("互助任务已发布");
            msg.setContent("您发布的互助任务已创建成功：" + task.getTitle());
            msg.setTargetType("USER");
            msg.setTargetUserId(parseLong(requester.getUserId()));
            msg.setPriority(5);
            notificationSender.send(msg);
        } catch (Exception e) {
            // ignore notification failure
        }
    }

    private Integer parseUserId(String userId) {
        try {
            return Integer.valueOf(userId);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(BAD_REQUEST, "用户ID无效");
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            // 兼容前端 ISO 字符串（含 Z/毫秒）
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // 尝试无时区格式
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(BAD_REQUEST, "时间格式需为 ISO 8601，例如 2023-10-01T09:30 或 2023-10-01T09:30:00Z");
        }
    }

    private String resolvePriority(String priority) {
        if (!StringUtils.hasText(priority)) {
            return "MEDIUM";
        }
        String p = priority.toUpperCase();
        if ("LOW".equals(p) || "HIGH".equals(p)) {
            return p;
        }
        return "MEDIUM";
    }

    private Long parseLong(String val) {
        try {
            return Long.valueOf(val);
        } catch (Exception e) {
            return null;
        }
    }
}
