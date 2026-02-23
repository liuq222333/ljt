package com.example.demo.demos.Agent.Controller;

import com.example.demo.demos.Agent.Pojo.AgentChatRequest;
import com.example.demo.demos.Agent.Pojo.AgentChatResponse;
import com.example.demo.demos.Agent.Service.AgentChatService;
import com.example.demo.demos.generic.Resp;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 社区助手的 REST 入口。
 * 控制器保持足够“轻”，只做基础的请求校验与参数承载，
 * 具体的业务编排和模型调用由 {@link AgentChatService} 负责。
 */
@RestController
@RequestMapping("/api/agent")
public class AgentChatController {

    private final AgentChatService agentChatService;

    public AgentChatController(AgentChatService agentChatService) {
        this.agentChatService = agentChatService;
    }

    /**
     * 接收前端发来的对话轮次，并透传可选的 Authorization 头，
     * 以便服务在需要时调用受保护的后端接口；同时将异常统一封装为 {@link Resp}。
     */
    @PostMapping("/chat")
    public Resp<AgentChatResponse> chat(@RequestBody AgentChatRequest request,
                                        @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            return Resp.success(agentChatService.chat(request, authorization));
        } catch (IllegalArgumentException ex) {
            return Resp.error(400, ex.getMessage());
        } catch (Exception ex) {
            return Resp.error("智能助手服务调用失败: " + ex.getMessage());
        }
    }
}
