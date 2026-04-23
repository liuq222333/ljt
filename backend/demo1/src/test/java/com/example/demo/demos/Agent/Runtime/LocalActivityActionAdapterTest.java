package com.example.demo.demos.Agent.Runtime;

import com.example.demo.demos.Agent.Entity.ApiRoute;
import com.example.demo.demos.Agent.Pojo.AgentChatMessage;
import com.example.demo.demos.Agent.Pojo.AgentChatRequest;
import com.example.demo.demos.Agent.Service.ApiRouteService;
import com.example.demo.demos.Agent.Service.BackendApiProxyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalActivityActionAdapterTest {

    @Mock
    private ApiRouteService apiRouteService;
    @Mock
    private BackendApiProxyService backendApiProxyService;

    private ActionConversationStore actionConversationStore;
    private LocalActivityActionAdapter adapter;

    @BeforeEach
    void setUp() {
        actionConversationStore = new ActionConversationStore();
        adapter = new LocalActivityActionAdapter(apiRouteService, backendApiProxyService, actionConversationStore);
    }

    @Test
    void reviewShouldExecuteListRouteForActivityListQuery() {
        when(apiRouteService.listEnabledRoutes("local_activity", "READ")).thenReturn(routes());
        when(backendApiProxyService.invoke(any(), eq(null))).thenReturn(successResult(listItem("社区羽毛球", "幸福广场", "PUBLISHED")));

        ActionIntentReviewService.ActionReviewResult result = adapter.review(
                "s1",
                message("当前有哪些活动"),
                null,
                new AgentChatRequest(),
                null,
                null
        );

        assertTrue(result.isHandled());
        assertEquals(ActionIntentReviewService.ActionOutcome.EXECUTED, result.getOutcome());
        assertEquals("list", result.getPendingAction().getAction());
        assertTrue(String.valueOf(((Map<?, ?>) result.getInvocationResult().getData()).get("message")).contains("当前有 1 个活动"));

        ArgumentCaptor<BackendApiProxyService.InvocationRequest> captor = ArgumentCaptor.forClass(BackendApiProxyService.InvocationRequest.class);
        verify(backendApiProxyService).invoke(captor.capture(), eq(null));
        assertEquals("local_activity", captor.getValue().getResource());
        assertEquals("list", captor.getValue().getAction());
        assertEquals("PUBLISHED", captor.getValue().getParams().get("status"));
    }

    @Test
    void reviewShouldAskForCoordinatesWhenNearbyQueryHasNoLocation() {
        when(apiRouteService.listEnabledRoutes("local_activity", "READ")).thenReturn(routes());

        ActionIntentReviewService.ActionReviewResult result = adapter.review(
                "s2",
                message("我附近有什么活动"),
                null,
                new AgentChatRequest(),
                null,
                null
        );

        assertTrue(result.isHandled());
        assertEquals(ActionIntentReviewService.ActionOutcome.NEED_CLARIFICATION, result.getOutcome());
        assertEquals("nearby", result.getPendingAction().getAction());
        assertTrue(result.getPendingAction().getMissingFields().contains("当前位置坐标"));
    }

    @Test
    void reviewShouldUseUserProfileCoordinatesForNearbyQuery() {
        when(apiRouteService.listEnabledRoutes("local_activity", "READ")).thenReturn(routes());
        when(backendApiProxyService.invoke(any(), eq(null))).thenReturn(successNearbyResult());

        AgentChatRequest request = new AgentChatRequest();
        request.getUserProfile().put("latitude", 35.1D);
        request.getUserProfile().put("longitude", 118.6D);

        ActionIntentReviewService.ActionReviewResult result = adapter.review(
                "s3",
                message("我附近有什么活动"),
                null,
                request,
                null,
                null
        );

        assertTrue(result.isHandled());
        assertEquals(ActionIntentReviewService.ActionOutcome.EXECUTED, result.getOutcome());
        assertEquals("nearby", result.getPendingAction().getAction());
        assertTrue(String.valueOf(((Map<?, ?>) result.getInvocationResult().getData()).get("message")).contains("你附近找到 1 个活动"));
    }

    @Test
    void reviewShouldFallbackToReadableActivityLabelWhenTitleIsPlaceholder() {
        when(apiRouteService.listEnabledRoutes("local_activity", "READ")).thenReturn(routes());
        when(backendApiProxyService.invoke(any(), eq(null))).thenReturn(successResult(placeholderListItem()));

        ActionIntentReviewService.ActionReviewResult result = adapter.review(
                "s4",
                message("当前有哪些活动"),
                null,
                new AgentChatRequest(),
                null,
                null
        );

        assertTrue(result.isHandled());
        assertEquals(ActionIntentReviewService.ActionOutcome.EXECUTED, result.getOutcome());
        assertTrue(String.valueOf(((Map<?, ?>) result.getInvocationResult().getData()).get("message")).contains("活动#26"));
    }

    @Test
    void reviewShouldAskForMissingFieldsWhenCreatingActivity() {
        when(apiRouteService.listEnabledRoutes("local_activity", "CREATE")).thenReturn(createRoutes());

        AgentChatRequest request = new AgentChatRequest();
        request.getUserProfile().put("username", "alice");

        ActionIntentReviewService.ActionReviewResult result = adapter.review(
                "s5",
                message("帮我创建一个羽毛球活动"),
                null,
                request,
                null,
                null
        );

        assertTrue(result.isHandled());
        assertEquals(ActionIntentReviewService.ActionOutcome.NEED_CLARIFICATION, result.getOutcome());
        assertEquals("create", result.getPendingAction().getAction());
        assertTrue(result.getPendingAction().getMissingFields().contains("活动日期"));
        assertEquals("alice", result.getPendingAction().getPayload().get("username"));
        assertEquals("sport", result.getPendingAction().getPayload().get("category"));
    }

    @Test
    void reviewShouldExecuteCreateRouteAfterConfirmation() {
        when(apiRouteService.listEnabledRoutes("local_activity", "CREATE")).thenReturn(createRoutes());
        when(backendApiProxyService.invoke(any(), eq(null))).thenReturn(successCreateResult());

        AgentChatRequest request = new AgentChatRequest();
        request.getUserProfile().put("username", "alice");

        ActionIntentReviewService.ActionReviewResult first = adapter.review(
                "s6",
                message("帮我创建一个羽毛球活动，日期2026-04-20，18:00到20:00，地点市民广场，描述社区羽毛球交流赛"),
                null,
                request,
                null,
                null
        );

        assertEquals(ActionIntentReviewService.ActionOutcome.NEED_CONFIRMATION, first.getOutcome());
        assertEquals("2026-04-20", first.getPendingAction().getPayload().get("date"));

        ActionIntentReviewService.ActionReviewResult second = adapter.review(
                "s6",
                message("确认执行"),
                null,
                request,
                null,
                first.getPendingAction()
        );

        assertTrue(second.isHandled());
        assertEquals(ActionIntentReviewService.ActionOutcome.EXECUTED, second.getOutcome());
        assertTrue(String.valueOf(((Map<?, ?>) second.getInvocationResult().getData()).get("message")).contains("activityId=301"));

        ArgumentCaptor<BackendApiProxyService.InvocationRequest> captor = ArgumentCaptor.forClass(BackendApiProxyService.InvocationRequest.class);
        verify(backendApiProxyService, times(1)).invoke(captor.capture(), eq(null));
        assertEquals("2026-04-20", ((Map<?, ?>) captor.getValue().getPayload()).get("date"));
    }

    private AgentChatMessage message(String content) {
        AgentChatMessage message = new AgentChatMessage();
        message.setRole("user");
        message.setContent(content);
        return message;
    }

    private List<ApiRoute> routes() {
        List<ApiRoute> routes = new ArrayList<ApiRoute>();
        routes.add(route(1L, "list", "查询活动列表", "/api/local-act/activities/list"));
        routes.add(route(2L, "nearby", "查询附近活动", "/api/local-act/activities/nearby"));
        return routes;
    }

    private List<ApiRoute> createRoutes() {
        List<ApiRoute> routes = new ArrayList<ApiRoute>();
        routes.add(routeWithOperation(3L, "create", "CREATE", "创建活动", "POST", "/api/local-act/activities"));
        return routes;
    }

    private ApiRoute route(Long id, String action, String description, String path) {
        ApiRoute route = new ApiRoute();
        route.setId(id);
        route.setResource("local_activity");
        route.setAction(action);
        route.setOperationType("READ");
        route.setHttpMethod("GET");
        route.setDescription(description);
        route.setPathTemplate(path);
        route.setEnabled(1);
        return route;
    }

    private ApiRoute routeWithOperation(Long id, String action, String operationType, String description, String method, String path) {
        ApiRoute route = new ApiRoute();
        route.setId(id);
        route.setResource("local_activity");
        route.setAction(action);
        route.setOperationType(operationType);
        route.setHttpMethod(method);
        route.setDescription(description);
        route.setPathTemplate(path);
        route.setEnabled(1);
        return route;
    }

    private BackendApiProxyService.InvocationResult successResult(Map<String, Object> item) {
        BackendApiProxyService.InvocationResult result = new BackendApiProxyService.InvocationResult();
        result.setPresentationHint("direct_response");
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("code", 200);
        payload.put("message", "ok");
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
        data.add(item);
        payload.put("data", data);
        result.setData(payload);
        return result;
    }

    private BackendApiProxyService.InvocationResult successNearbyResult() {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("title", "社区读书会");
        item.put("location", "市民中心");
        item.put("distanceKm", 1.2D);
        return successResult(item);
    }

    private BackendApiProxyService.InvocationResult successCreateResult() {
        BackendApiProxyService.InvocationResult result = new BackendApiProxyService.InvocationResult();
        result.setPresentationHint("direct_response");
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("code", 200);
        payload.put("message", "ok");
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("activityId", 301);
        data.put("status", "PUBLISHED");
        payload.put("data", data);
        result.setData(payload);
        return result;
    }

    private Map<String, Object> listItem(String title, String location, String status) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("title", title);
        item.put("locationText", location);
        item.put("status", status);
        return item;
    }

    private Map<String, Object> placeholderListItem() {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("id", 26);
        item.put("title", "1");
        item.put("locationText", "幸福广场");
        item.put("status", "PUBLISHED");
        return item;
    }
}
