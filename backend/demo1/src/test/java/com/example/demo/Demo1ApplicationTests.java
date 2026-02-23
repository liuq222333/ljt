package com.example.demo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class Demo1ApplicationTests {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void redisConnection() {
        String key = "conn:test";
        String value = String.valueOf(System.currentTimeMillis());
        redisTemplate.opsForValue().set(key, value);
        String got = redisTemplate.opsForValue().get(key);
        Assertions.assertEquals(value, got);
    }

}
