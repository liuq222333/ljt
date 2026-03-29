package com.example.demo.demos.common.trace;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * HTTP 请求自动注入 trace 的过滤器。
 * 从请求 Header 读取 X-Request-Id / X-Trace-Id，没有则自动生成。
 * 同时写入 SLF4J MDC，使日志自动携带追踪 ID。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceFilter implements Filter {

    private static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpReq = (HttpServletRequest) request;

            String requestId = httpReq.getHeader(HEADER_REQUEST_ID);
            String traceId = httpReq.getHeader(HEADER_TRACE_ID);

            // 初始化 TraceContext
            TraceContext.init(requestId, traceId);

            // 写入 MDC，使日志自动携带
            MDC.put("requestId", TraceContext.getRequestId());
            MDC.put("traceId", TraceContext.getTraceId());

            // 在响应头中回传，方便前端 / 调用方追踪
            if (response instanceof HttpServletResponse) {
                HttpServletResponse httpResp = (HttpServletResponse) response;
                httpResp.setHeader(HEADER_REQUEST_ID, TraceContext.getRequestId());
                httpResp.setHeader(HEADER_TRACE_ID, TraceContext.getTraceId());
            }

            chain.doFilter(request, response);
        } finally {
            TraceContext.clear();
            MDC.clear();
        }
    }
}
