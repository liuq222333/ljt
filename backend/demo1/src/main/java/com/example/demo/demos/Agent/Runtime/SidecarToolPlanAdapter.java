package com.example.demo.demos.Agent.Runtime;

import com.example.demo.demos.common.enums.PlanType;
import com.example.demo.demos.common.enums.ToolName;
import com.example.demo.demos.common.schema.ToolPlan;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class SidecarToolPlanAdapter {

    public AgentRuntime.RuntimePlan adapt(ToolPlan toolPlan) {
        if (toolPlan == null || toolPlan.getPlanType() == null) {
            throw new IllegalArgumentException("toolPlan must not be null");
        }
        AgentRuntime.RuntimePlan runtimePlan = new AgentRuntime.RuntimePlan();
        runtimePlan.planCode = toolPlan.getPlanType().getCode();
        runtimePlan.planSource = "python";
        runtimePlan.routingReason = toolPlan.getRoutingReason();
        runtimePlan.requiresClarification = toolPlan.isRequiresClarification();
        runtimePlan.clarificationPrompt = toolPlan.getClarificationPrompt();
        applyPlanType(runtimePlan, toolPlan);
        if (runtimePlan.requiresClarification) {
            runtimePlan.runSearch = false;
            runtimePlan.runKnowledge = false;
            runtimePlan.runRealtime = false;
        }
        return runtimePlan;
    }

    private void applyPlanType(AgentRuntime.RuntimePlan runtimePlan, ToolPlan toolPlan) {
        PlanType planType = toolPlan.getPlanType();
        switch (planType) {
            case CLARIFICATION_REQUIRED:
                runtimePlan.requiresClarification = true;
                break;
            case SEARCH_THEN_KNOWLEDGE:
                runtimePlan.runSearch = true;
                runtimePlan.runKnowledge = true;
                break;
            case SEARCH_THEN_REALTIME:
                runtimePlan.runSearch = true;
                runtimePlan.runRealtime = true;
                break;
            case SEARCH_THEN_PARALLEL:
            case PARALLEL:
                applyStepFlags(runtimePlan, toolPlan);
                break;
            case SINGLE_TOOL:
            default:
                applyStepFlags(runtimePlan, toolPlan);
                break;
        }
    }

    private void applyStepFlags(AgentRuntime.RuntimePlan runtimePlan, ToolPlan toolPlan) {
        if (toolPlan == null || CollectionUtils.isEmpty(toolPlan.getSteps())) {
            return;
        }
        for (ToolPlan.ToolStep step : toolPlan.getSteps()) {
            if (step == null || step.getToolName() == null) {
                continue;
            }
            ToolName toolName = step.getToolName();
            if (toolName == ToolName.STRUCTURED_SEARCH) {
                runtimePlan.runSearch = true;
            } else if (toolName == ToolName.KNOWLEDGE_RETRIEVAL) {
                runtimePlan.runKnowledge = true;
            } else if (toolName == ToolName.REALTIME_QUERY) {
                runtimePlan.runRealtime = true;
            }
        }
    }
}
