package com.example.demo.demos.exception;

import com.example.demo.demos.common.error.BizException;
import com.example.demo.demos.common.error.ErrorCode;
import com.example.demo.demos.common.trace.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理统一业务异常（BizException）。
     * 根据错误码区分 HTTP 状态码。
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Object> handleBizException(BizException ex, WebRequest request) {
        log.warn("业务异常: code={}, message={}, requestId={}",
                ex.getCode(), ex.getMessage(), TraceContext.getRequestId());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("code", ex.getCode());
        body.put("message", ex.getMessage());
        body.put("requestId", TraceContext.getRequestId());
        body.put("path", extractPath(request));

        HttpStatus status = mapToHttpStatus(ex.getCode());
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<Object> handleResourceConflict(ResourceConflictException ex, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());
        body.put("requestId", TraceContext.getRequestId());
        body.put("path", extractPath(request));

        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    /**
     * 兜底：处理所有未捕获的异常。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex, WebRequest request) {
        log.error("未捕获异常: requestId={}, error={}",
                TraceContext.getRequestId(), ex.getMessage(), ex);

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("code", 500);
        body.put("message", "服务器内部错误，请稍后重试");
        body.put("requestId", TraceContext.getRequestId());
        body.put("path", extractPath(request));

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 根据业务错误码映射 HTTP 状态码。
     */
    private HttpStatus mapToHttpStatus(int code) {
        if (code == ErrorCode.AGENT_PROMPT_INJECTION) {
            return HttpStatus.BAD_REQUEST;
        }
        if (code == ErrorCode.AGENT_RATE_LIMITED) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        if (code == ErrorCode.AGENT_TOKEN_QUOTA_EXCEEDED) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        if (code == ErrorCode.AGENT_SESSION_EXPIRED) {
            return HttpStatus.GONE;
        }
        if (code == ErrorCode.AGENT_TIMEOUT || code == ErrorCode.AGENT_LLM_TIMEOUT
                || code == ErrorCode.SEARCH_ES_TIMEOUT || code == ErrorCode.REALTIME_TIMEOUT) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        if (code == ErrorCode.AGENT_LLM_UNAVAILABLE || code == ErrorCode.SEARCH_ES_UNAVAILABLE
                || code == ErrorCode.KNOWLEDGE_INDEX_UNAVAILABLE || code == ErrorCode.REALTIME_UNAVAILABLE) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        // 默认内部错误
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String extractPath(WebRequest request) {
        String desc = request.getDescription(false);
        return desc.startsWith("uri=") ? desc.substring(4) : desc;
    }
}
