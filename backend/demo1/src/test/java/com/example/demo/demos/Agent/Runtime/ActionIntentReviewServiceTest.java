package com.example.demo.demos.Agent.Runtime;

import com.example.demo.demos.Agent.Config.AgentActionReviewPythonProperties;
import com.example.demo.demos.Agent.Entity.ApiRoute;
import com.example.demo.demos.Agent.Pojo.AgentChatMessage;
import com.example.demo.demos.Agent.Service.ApiRouteService;
import com.example.demo.demos.Agent.Service.BackendApiProxyService;
import com.example.demo.demos.CommunityMarket.Pojo.Category;
import com.example.demo.demos.CommunityMarket.Service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionIntentReviewServiceTest {

    @Mock
    private ApiRouteService apiRouteService;
    @Mock
    private BackendApiProxyService backendApiProxyService;
    @Mock
    private CategoryService categoryService;

    private ActionConversationStore actionConversationStore;
    private ActionIntentReviewService service;

    @BeforeEach
    void setUp() {
        actionConversationStore = new ActionConversationStore();
        AgentActionReviewPythonProperties properties = new AgentActionReviewPythonProperties();
        properties.setEnabled(false);
        service = new ActionIntentReviewService(
                apiRouteService,
                backendApiProxyService,
                actionConversationStore,
                new LocalActivityActionAdapter(apiRouteService, backendApiProxyService, actionConversationStore),
                categoryService,
                properties,
                null,
                null
        );
    }

    @Test
    void reviewShouldIgnoreQuestionOrLearningIntent() {
        ActionIntentReviewService.ActionReviewResult result = service.review(
                "s1",
                message("红米手机的价格能改吗？"),
                null,
                null
        );

        assertFalse(result.isHandled());
        verify(apiRouteService, never()).listEnabledRoutes("product", "UPDATE");
    }

    @Test
    void reviewShouldRecognizeImplicitPriceUpdateWithNumberBeforeProduct() {
        when(apiRouteService.listEnabledRoutes("product", "UPDATE"))
                .thenReturn(Collections.singletonList(route(2L, "update_price", "UPDATE", "修改商品价格")));

        ActionIntentReviewService.ActionReviewResult result = service.review(
                "s2",
                message("5号商品价格999"),
                null,
                null
        );

        assertTrue(result.isHandled());
        assertEquals(ActionIntentReviewService.ActionOutcome.NEED_CONFIRMATION, result.getOutcome());
        assertEquals(5L, result.getPendingAction().getParams().get("productId"));
        assertEquals(new BigDecimal("999"), result.getPendingAction().getParams().get("price"));
    }

    @Test
    void reviewShouldUseSessionEntityWhenUserReferencesCurrentProduct() {
        when(apiRouteService.listEnabledRoutes("product", "UPDATE"))
                .thenReturn(Collections.singletonList(route(2L, "update_price", "UPDATE", "修改商品价格")));

        SessionState.SessionContext sessionContext = new SessionState.SessionContext();
        sessionContext.setFocusedEntityId("42");
        sessionContext.setLastSelectedEntityIds(Collections.singletonList("42"));
        sessionContext.setCandidateEntities(Collections.singletonList("42"));

        ActionIntentReviewService.ActionReviewResult result = service.review(
                "s2-ref",
                message("把这个改价 999"),
                null,
                null,
                null,
                sessionContext
        );

        assertEquals(ActionIntentReviewService.ActionOutcome.NEED_CONFIRMATION, result.getOutcome());
        assertEquals(42L, result.getPendingAction().getParams().get("productId"));
        assertEquals(new BigDecimal("999"), result.getPendingAction().getParams().get("price"));
    }

    @Test
    void reviewShouldApplyShortRepliesAccordingToMissingFields() {
        when(apiRouteService.listEnabledRoutes("product", "CREATE"))
                .thenReturn(Collections.singletonList(route(1L, "create", "CREATE", "发布商品")));
        when(categoryService.getAllCategories()).thenReturn(Collections.singletonList(category("10", "手机")));

        ActionIntentReviewService.ActionReviewResult first = service.review(
                "s3",
                message("发布一个红米手机"),
                null,
                null
        );
        assertEquals(ActionIntentReviewService.ActionOutcome.NEED_CLARIFICATION, first.getOutcome());

        ActionIntentReviewService.ActionReviewResult price = service.review("s3", message("999"), null, null);
        assertEquals(new BigDecimal("999"), price.getPendingAction().getPayload().get("price"));

        ActionIntentReviewService.ActionReviewResult stock = service.review("s3", message("10"), null, null);
        assertEquals(10, stock.getPendingAction().getPayload().get("stockQuantity"));

        ActionIntentReviewService.ActionReviewResult location = service.review("s3", message("一食堂"), null, null);
        assertEquals("一食堂", location.getPendingAction().getPayload().get("location"));
        assertTrue(location.getPendingAction().getMissingFields().contains("图片 URL"));
    }

    @Test
    void reviewShouldStripLocationMoveVerb() {
        when(apiRouteService.listEnabledRoutes("product", "CREATE"))
                .thenReturn(Collections.singletonList(route(1L, "create", "CREATE", "发布商品")));
        when(categoryService.getAllCategories()).thenReturn(Collections.singletonList(category("10", "手机")));

        ActionIntentReviewService.ActionReviewResult result = service.review(
                "s4",
                message("发布一个红米手机，价格999，库存10，分类手机，地点放到一食堂，图片 https://example.com/a.jpg"),
                null,
                null
        );

        assertEquals("一食堂", result.getPendingAction().getPayload().get("location"));
    }

    private AgentChatMessage message(String content) {
        AgentChatMessage message = new AgentChatMessage();
        message.setRole("user");
        message.setContent(content);
        return message;
    }

    private ApiRoute route(Long id, String action, String operationType, String description) {
        ApiRoute route = new ApiRoute();
        route.setId(id);
        route.setResource("product");
        route.setAction(action);
        route.setOperationType(operationType);
        route.setHttpMethod("POST");
        route.setPathTemplate("/api/products/" + action);
        route.setDescription(description);
        route.setEnabled(1);
        return route;
    }

    private Category category(String id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }
}
