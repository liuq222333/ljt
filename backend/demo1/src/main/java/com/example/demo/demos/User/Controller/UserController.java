package com.example.demo.demos.User.Controller;

import com.example.demo.demos.User.DTO.UserInfoDTO;
import com.example.demo.demos.User.Pojo.User;
import com.example.demo.demos.User.Service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import com.example.demo.config.MinioProperties;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.Map;

import static net.sf.jsqlparser.util.validation.metadata.NamedObject.user;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private MinioProperties minioProperties;
    @Operation(summary = "根据用户id修改用户名称")
    @PostMapping("/updateUser")
    public ResponseEntity<Map<String, String>> updateUser(@RequestParam String userId, @RequestParam String userName){
        userService.updateUser(userId, userName);
        return ResponseEntity.ok(Collections.singletonMap("message", "用户名更新成功"));
    }

    @Operation(summary = "根据用户id更新用户信息")
    @PostMapping("/updateUserInfo")
    public ResponseEntity<Map<String, String>> updateUserInfo(@RequestBody UserInfoDTO userInfoDTO){
        userService.updateUserInfo(userInfoDTO);
        return ResponseEntity.ok(Collections.singletonMap("message", "用户信息更新成功"));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/deleteUser")
    public ResponseEntity<Map<String, String>> deleteUser(@RequestParam String userId, @RequestParam String password){
        userService.deleteUser(userId, password);
        return ResponseEntity.ok(Collections.singletonMap("message", "用户删除成功"));
    }

    @Operation(summary = "根据用户名获取用户id")
    @GetMapping("/getUserIdByName")
    public Integer getUserIdByName(@RequestParam String userName) {
        Integer id = userService.getUserIdByName(userName);

        return id;
    }

    @Operation(summary = "根据用户id获取用户信息")
    @GetMapping("/getUserById")
    public ResponseEntity<com.example.demo.demos.User.Pojo.User> getUserById(@RequestParam String userId) {
        User user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.status(404).body(null);
        }
        // 如果存在 avatarKey，生成预签名 URL
        if (StringUtils.hasText(user.getAvatarKey())) {
            try {
                String url = minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .bucket(minioProperties.getBucket())
                                .object(user.getAvatarKey())
                                .method(Method.GET)
                                .expiry(1, java.util.concurrent.TimeUnit.HOURS)
                                .build()
                );
                // user.setAvatarUrl(url);
                // 前端逻辑兼容: 如果 avatarKey 是 http 开头则直接使用
                // 因此这里直接替换 avatarKey 为预签名 URL
                user.setAvatarKey(url);
            } catch (Exception e) {
                // ignore，如果失败则保持原样
            }
        }
        return ResponseEntity.ok(user);
    }
}
