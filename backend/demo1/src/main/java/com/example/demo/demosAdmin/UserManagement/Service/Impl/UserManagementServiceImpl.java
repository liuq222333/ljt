package com.example.demo.demosAdmin.UserManagement.Service.Impl;

import com.example.demo.demosAdmin.UserManagement.DTO.AdminUserDTO;
import com.example.demo.demosAdmin.UserManagement.DTO.AdminUserUpdateRequest;
import com.example.demo.demosAdmin.UserManagement.Dao.AdminUserMapper;
import com.example.demo.demosAdmin.UserManagement.Service.UserManagementService;
import com.example.demo.demos.Notification.Pojo.NotificationMessage;
import com.example.demo.demos.Notification.Service.NotificationSender;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final NotificationSender notificationSender;

    @Override
    public List<AdminUserDTO> listUsers(String keyword, int page, int size) {
        int limit = Math.max(1, size);
        int offset = Math.max(0, (Math.max(1, page) - 1) * limit);
        return adminUserMapper.listUsers(StringUtils.hasText(keyword) ? keyword : null, limit, offset);
    }

    @Override
    public void updateUser(String userId, AdminUserUpdateRequest request) {
        AdminUserDTO dto = new AdminUserDTO();
        dto.setUserId(userId);
        dto.setUserName(request.getUserName());
        dto.setEmail(request.getEmail());
        dto.setPhone(request.getPhone());
        dto.setAddress(request.getAddress());
        adminUserMapper.updateUser(dto);
    }

    @Override
    public void resetPassword(String userId, String newPassword) {
        String encoded = passwordEncoder.encode(newPassword);
        adminUserMapper.resetPassword(userId, encoded);
        try {
            Long uid = Long.valueOf(userId);
            NotificationMessage msg = new NotificationMessage();
            msg.setKind("USER_PASSWORD_RESET");
            msg.setTitle("密码已重置");
            msg.setContent("您的密码已被管理员重置，请尽快登录并修改");
            msg.setTargetType("USER");
            msg.setTargetUserId(uid);
            msg.setPriority(6);
            notificationSender.send(msg);
        } catch (Exception ignore) {}
    }

    @Override
    public void deleteUser(String userId) {
        adminUserMapper.deleteUser(userId);
    }
}
