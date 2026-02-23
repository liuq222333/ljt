package com.example.demo.demosAdmin.UserManagement.Controller;

import com.example.demo.demos.generic.Resp;
import com.example.demo.demosAdmin.UserManagement.DTO.AdminUserDTO;
import com.example.demo.demosAdmin.UserManagement.DTO.AdminUserUpdateRequest;
import com.example.demo.demosAdmin.UserManagement.Service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    @Operation(summary="获取所有用户")
    @GetMapping
    public Resp<List<AdminUserDTO>> listUsers(@RequestParam(value = "keyword", required = false) String keyword,
                                              @RequestParam(value = "page", defaultValue = "1") int page,
                                              @RequestParam(value = "size", defaultValue = "20") int size) {
        return Resp.success(userManagementService.listUsers(keyword, page, size));
    }

    @Operation(summary="更新用户")
    @PutMapping("/{userId}")
    public Resp<Void> updateUser(@PathVariable("userId") String userId,
                                 @RequestBody AdminUserUpdateRequest request) {
        userManagementService.updateUser(userId, request);
        return Resp.success();
    }

    @Operation(summary="重置用户密码")
    @PostMapping("/{userId}/reset-password")
    public Resp<Void> resetPassword(@PathVariable("userId") String userId,
                                    @RequestParam("password") String password) {
        userManagementService.resetPassword(userId, password);
        return Resp.success();
    }

    @Operation(summary="删除用户")
    @DeleteMapping("/{userId}")
    public Resp<Void> deleteUser(@PathVariable("userId") String userId) {
        try {
            userManagementService.deleteUser(userId);
            return Resp.success();
        } catch (DataIntegrityViolationException e) {
            return Resp.error("该用户已发布过活动或存在关联数据，无法直接删除。建议禁用该用户。");
        } catch (Exception e) {
            return Resp.error("删除用户失败: " + e.getMessage());
        }
    }
}
