package com.example.demo.demosAdmin.AdminActivity.Service.Impl;

import com.example.demo.demos.LocalActive.Pojo.LocalActivity;
import com.example.demo.demos.LocalActive.Dao.LocalActivityMapper;
import com.example.demo.demosAdmin.AdminActivity.Dao.AdminActivityMapper;
import com.example.demo.demosAdmin.AdminActivity.Service.AdminActivityService;
import com.example.demo.demos.Notification.Pojo.NotificationMessage;
import com.example.demo.demos.Notification.Service.NotificationSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminActivityServiceImpl implements AdminActivityService {

    private final AdminActivityMapper adminActivityMapper;
    private final LocalActivityMapper localActivityMapper;
    private final NotificationSender notificationSender;

    @Override
    public List<LocalActivity> listReviews(String status, String keyword, int page, int size) {
        int limit = Math.max(1, size);
        int offset = Math.max(0, (Math.max(1, page) - 1) * limit);
        return adminActivityMapper.listAdminActivities(
                StringUtils.hasText(status) ? status : null,
                StringUtils.hasText(keyword) ? keyword : null,
                limit,
                offset
        );
    }

    @Override
    @Transactional
    public Long approve(Long adminActivityId, String note) {
        LocalActivity admin = adminActivityMapper.findAdminActivityById(adminActivityId);
        if (admin == null) return null;
        admin.setStatus("PUBLISHED");
        if (StringUtils.hasText(note)) {
            admin.setReviewNote(note);
        }
        int rows = adminActivityMapper.insertToMain(admin);
        if (rows <= 0 || admin.getId() == null) {
            throw new IllegalStateException("审批通过插入主表失败");
        }
        List<String> tags = adminActivityMapper.listTags(adminActivityId);
        if (tags != null && !tags.isEmpty()) {
            // 将审核表中的标签同步到正式表
            localActivityMapper.insertTags(admin.getId(), tags);
            adminActivityMapper.deleteTags(adminActivityId);
        }
        adminActivityMapper.deleteAdminActivity(adminActivityId);
        try {
            if (admin.getOrganizerUserId() != null) {
                NotificationMessage msg = new NotificationMessage();
                msg.setKind("LOCAL_ACTIVITY_REVIEW_APPROVED");
                msg.setTitle("活动审核通过");
                String t = admin.getTitle() == null ? "" : admin.getTitle();
                msg.setContent("您的活动【" + t + "】已审核通过并发布");
                msg.setTargetType("USER");
                msg.setTargetUserId(Long.valueOf(admin.getOrganizerUserId()));
                msg.setPriority(5);
                notificationSender.send(msg);
            }
        } catch (Exception ignore) {}
        return admin.getId();
    }

    @Override
    @Transactional
    public void reject(Long adminActivityId, String note) {
        adminActivityMapper.updateAdminStatus(adminActivityId, "CANCELLED", StringUtils.hasText(note) ? note : null);
        LocalActivity admin = adminActivityMapper.findAdminActivityById(adminActivityId);
        try {
            if (admin != null && admin.getOrganizerUserId() != null) {
                NotificationMessage msg = new NotificationMessage();
                msg.setKind("LOCAL_ACTIVITY_REVIEW_REJECTED");
                msg.setTitle("活动审核未通过");
                String t = admin.getTitle() == null ? "" : admin.getTitle();
                String n = StringUtils.hasText(note) ? note : "";
                msg.setContent("您的活动【" + t + "】未通过审核" + (StringUtils.hasText(n) ? ("，原因：" + n) : ""));
                msg.setTargetType("USER");
                msg.setTargetUserId(Long.valueOf(admin.getOrganizerUserId()));
                msg.setPriority(4);
                notificationSender.send(msg);
            }
        } catch (Exception ignore) {}
    }
}
