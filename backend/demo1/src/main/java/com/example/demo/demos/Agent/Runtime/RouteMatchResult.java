package com.example.demo.demos.Agent.Runtime;

import com.example.demo.demos.Agent.Entity.ApiRoute;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RouteMatchResult {

    private boolean matched;
    private boolean requiresClarification;
    private String clarificationPrompt;
    private ApiRoute route;
    private double score;
    private double runnerUpScore;
    private String entityType;
    private String reason;
    private List<String> scoreReasons = new ArrayList<String>();

    public static RouteMatchResult noMatch(String reason) {
        RouteMatchResult result = new RouteMatchResult();
        result.setReason(reason);
        return result;
    }

    public static RouteMatchResult clarification(ApiRoute route,
                                                 double score,
                                                 double runnerUpScore,
                                                 String entityType,
                                                 String prompt,
                                                 List<String> reasons) {
        RouteMatchResult result = new RouteMatchResult();
        result.setRequiresClarification(true);
        result.setRoute(route);
        result.setScore(score);
        result.setRunnerUpScore(runnerUpScore);
        result.setEntityType(entityType);
        result.setClarificationPrompt(prompt);
        result.setReason("route_match_requires_clarification");
        if (reasons != null) {
            result.getScoreReasons().addAll(reasons);
        }
        return result;
    }

    public static RouteMatchResult matched(ApiRoute route,
                                           double score,
                                           double runnerUpScore,
                                           String entityType,
                                           List<String> reasons) {
        RouteMatchResult result = new RouteMatchResult();
        result.setMatched(true);
        result.setRoute(route);
        result.setScore(score);
        result.setRunnerUpScore(runnerUpScore);
        result.setEntityType(entityType);
        result.setReason("route_match_auto_execute");
        if (reasons != null) {
            result.getScoreReasons().addAll(reasons);
        }
        return result;
    }
}
