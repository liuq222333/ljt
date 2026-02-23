package com.example.demo.demos.Login.Controller;

import com.example.demo.demos.Login.Entity.User;
import com.example.demo.demos.Login.Service.LoginService;
import com.example.demo.demos.Login.dto.LoginRequest;
import com.example.demo.demos.Login.dto.LoginResponse;
import com.example.demo.demos.User.Service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
@Slf4j
public class LoginController {

    private final LoginService loginService;
    private final UserService userService;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String SMS_LOCK_PREFIX = "sms:phone:lock:";
    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final long SMS_COOLDOWN_SECONDS = 60L;
    private static final long SMS_CODE_TTL_SECONDS = 300L;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    @Autowired
    public LoginController(LoginService loginService, UserService userService, StringRedisTemplate stringRedisTemplate) {
        this.loginService = loginService;
        this.userService = userService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String userName = request.getUsername();
        String password = request.getPassword();
        if (userName == null || password == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("用户名或密码为空");
        }
        boolean ok = loginService.validateLogin(userName, password);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户名或密码错误");
        }
        String token = genToken(userName);
        int userId = userService.getUserIdByName(userName);
        return ResponseEntity.ok(new LoginResponse(token, userName, userId));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest request) {
        String userName = request.getUsername();
        String password = request.getPassword();
        String phone = request.getPhone();
        boolean created = loginService.register(userName, password, phone);
        if (!created) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("用户名已存在");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body("注册成功");
    }

    @GetMapping("/UserIsExist")
    @Operation(summary = "用户是否存在")
    public ResponseEntity<Map<String, Boolean>> getUserIsExist(@RequestParam String userName) {
        User user = loginService.getUserByName(userName);
        boolean exists = user != null;
        return ResponseEntity.ok(Collections.singletonMap("exists", exists));
    }

    @GetMapping("/getUserByName")
    @Operation(summary = "获取用户信息")
    public ResponseEntity<?> getUserByName(@RequestParam String userName) {
        User user = loginService.getUserByName(userName);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("用户不存在");
        }
        return ResponseEntity.ok(user);
    }

    @GetMapping("/getUserById/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable String userId) {
        User user = loginService.getUserById(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("用户不存在");
        }
        return ResponseEntity.ok(user);
    }

    private String genToken(String userName) {
        String raw = userName + ":" + System.currentTimeMillis();
        return "token-" + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/sms/send-code")
    @Operation(summary = "发送短信验证码")
    public ResponseEntity<Map<String, String>> sendSmsCode(@RequestBody Map<String, String> payload) {
        String phone = payload.get("phone");
        if (phone == null || phone.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", "手机号不能为空"));
        }
        phone = phone.trim();
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", "手机号格式不正确"));
        }
        // 使用redis里面的setnx来判断60秒内是否已经发送过验证码，如果已经发送过则返回剩余秒数
        String lockKey = SMS_LOCK_PREFIX + phone;
        Boolean ok = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", SMS_COOLDOWN_SECONDS, TimeUnit.SECONDS);
                if (Boolean.FALSE.equals(ok)) {
                    Long ttl = stringRedisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
                    String remain = (ttl != null && ttl > 0) ? ttl.toString() : "0";
                
                    Map<String, String> body = new HashMap<>();
                    body.put("message", "60秒内已发送");
                    body.put("remaining", remain);
                
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);                
                }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        stringRedisTemplate.opsForValue()
                .set(SMS_CODE_PREFIX + phone, code, SMS_CODE_TTL_SECONDS, TimeUnit.SECONDS);
        log.info("向手机 {} 发送验证码: {}", phone, code);
        return ResponseEntity.ok(Collections.singletonMap("message", "验证码已发送"));
    }

    @PostMapping("/sms/verify-code")
    public ResponseEntity<Boolean> verifyCode(@RequestBody Map<String, String> payload) {
        String phone = payload.get("phone");
        String code = payload.get("code");
        String storedCode = stringRedisTemplate.opsForValue().get(SMS_CODE_PREFIX + phone);
        if (storedCode != null && storedCode.equals(code)) {
            stringRedisTemplate.delete(SMS_CODE_PREFIX + phone);
            return ResponseEntity.ok(true);
        } else {
            return ResponseEntity.ok(false);
        }
    }

    @PostMapping("/changePassword")
    @Operation(summary = "修改密码（校验旧密码）")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> payload) {
        String userId = payload.get("userId");
        String oldPassword = payload.get("oldPassword");
        String newPassword = payload.get("newPassword");
        if (userId == null || oldPassword == null || newPassword == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", "参数不完整"));
        }

        boolean updated = loginService.changePassword(userId, oldPassword, newPassword);
        if (!updated) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "原密码错误"));
        }
        return ResponseEntity.ok(Collections.singletonMap("message", "密码修改成功"));
    }
}
