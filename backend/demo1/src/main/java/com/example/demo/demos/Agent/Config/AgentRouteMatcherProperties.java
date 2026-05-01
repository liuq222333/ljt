package com.example.demo.demos.Agent.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "agent.route-matcher")
public class AgentRouteMatcherProperties {

    private boolean enabled = true;

    private int autoExecuteThreshold = 72;

    private int clarifyThreshold = 55;

    private int closeScoreGap = 8;

    private double rawKeywordWeight = 0.08D;
}
