package com.example.demo.demos.generic;

import lombok.Data;

/**
 * 通用 API 响应封装类
 * @param <T> 响应数据的类型
 */
@Data
public class Resp<T> {

    private int code;
    private String message;
    private T data;

    // 构造函数私有化，强制使用静态工厂方法
    private Resp(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // --- 成功响应 ---

    /**
     * 返回成功的响应，包含数据
     * @param data 响应数据
     * @param <T>  数据类型
     * @return Resp
     */
    public static <T> Resp<T> success(T data) {
        return new Resp<>(200, "操作成功", data);
    }

    /**
     * 返回成功的响应，不包含数据
     * @return Resp
     */
    public static <T> Resp<T> success() {
        return new Resp<>(200, "操作成功", null);
    }

    // --- 失败响应 ---

    /**
     * 返回失败的响应，包含错误码和错误信息
     * @param code    错误码
     * @param message 错误信息
     * @return Resp
     */
    public static <T> Resp<T> error(int code, String message) {
        return new Resp<>(code, message, null);
    }

    /**
     * 返回失败的响应，使用默认错误码 (500)
     * @param message 错误信息
     * @return Resp
     */
    public static <T> Resp<T> error(String message) {
        return new Resp<>(500, message, null);
    }
}