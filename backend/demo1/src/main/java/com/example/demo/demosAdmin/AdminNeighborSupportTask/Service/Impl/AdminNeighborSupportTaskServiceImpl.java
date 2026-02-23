package com.example.demo.demosAdmin.AdminNeighborSupportTask.Service.Impl;

import com.example.demo.demos.LocalActive.Pojo.NeighborSupportTask;
import com.example.demo.demosAdmin.AdminNeighborSupportTask.Dao.AdminNeighborSupportTaskMapper;
import com.example.demo.demosAdmin.AdminNeighborSupportTask.Service.AdminNeighborSupportTaskService;
import com.example.demo.demos.Notification.Pojo.NotificationMessage;
import com.example.demo.demos.Notification.Service.NotificationSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminNeighborSupportTaskServiceImpl implements AdminNeighborSupportTaskService {

    private final AdminNeighborSupportTaskMapper adminMapper;
    private final NotificationSender notificationSender;

    @Override
    public List<NeighborSupportTask> listReviews(String status, String keyword, int page, int size) {
        int limit = Math.max(1, size);
        int offset = Math.max(0, (Math.max(1, page) - 1) * limit);
        return adminMapper.listAdminTasks(
                StringUtils.hasText(status) ? status : null,
                StringUtils.hasText(keyword) ? keyword : null,
                limit,
                offset
        );
    }

    @Override
    @Transactional
    public Long approve(Long adminTaskId) {
        NeighborSupportTask task = adminMapper.findAdminTaskById(adminTaskId);
        if (task == null) return null;

        task.setStatus("OPEN");

        int rows = adminMapper.insertToMain(task);
        if (rows <= 0 || task.getId() == null) {
            throw new IllegalStateException("审批通过插入主表失败");
        }

        adminMapper.deleteAdminTask(adminTaskId);
        try {
            if (task.getRequesterUserId() != null) {
                NotificationMessage msg = new NotificationMessage();
                msg.setKind("NEIGHBOR_TASK_REVIEW_APPROVED");
                msg.setTitle("互助任务审核通过");
                String t = task.getTitle() == null ? "" : task.getTitle();
                msg.setContent("您的互助任务【" + t + "】已审核通过并发布");
                msg.setTargetType("USER");
                msg.setTargetUserId(Long.valueOf(task.getRequesterUserId()));
                msg.setPriority(5);
                notificationSender.send(msg);
            }
        } catch (Exception ignore) {}

        return task.getId();
    }

    @Override
    @Transactional
    public void reject(Long adminTaskId) {
        adminMapper.updateAdminStatus(adminTaskId, "CANCELLED");
        NeighborSupportTask task = adminMapper.findAdminTaskById(adminTaskId);
        try {
            if (task != null && task.getRequesterUserId() != null) {
                NotificationMessage msg = new NotificationMessage();
                msg.setKind("NEIGHBOR_TASK_REVIEW_REJECTED");
                msg.setTitle("互助任务审核未通过");
                String t = task.getTitle() == null ? "" : task.getTitle();
                msg.setContent("您的互助任务【" + t + "】未通过审核");
                msg.setTargetType("USER");
                msg.setTargetUserId(Long.valueOf(task.getRequesterUserId()));
                msg.setPriority(4);
                notificationSender.send(msg);
            }
        } catch (Exception ignore) {}
    }
}
