package com.example.demo.demos.Agent.Service;

import com.example.demo.demos.common.enums.FollowUpType;
import com.example.demo.demos.common.enums.TaskType;
import com.example.demo.demos.Agent.Pojo.CandidateSlots;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 规则引擎降级解析器 — 当 LLM 不可用时，用关键词匹配做简单意图分类。
 * 对应 Query Parser 设计文档 §10.2 规则引擎降级。
 */
@Component
public class RuleFallbackParser {

    private static final Logger log = LoggerFactory.getLogger(RuleFallbackParser.class);

    // ---- 关键词表 ----

    private static final List<String> FAQ_KEYWORDS = Arrays.asList(
            "退款", "规则", "怎么", "须知", "注意事项", "使用说明", "限制",
            "如何", "流程", "政策", "条款", "预约", "取消");

    private static final List<String> REALTIME_KEYWORDS = Arrays.asList(
            "现在", "还有", "还能", "营业", "开门", "关门", "有票",
            "有库存", "还剩", "当前", "此刻", "目前");

    private static final List<String> EVENT_KEYWORDS = Arrays.asList(
            "活动", "展览", "演出", "表演", "音乐会", "演唱会",
            "展会", "赛事", "比赛", "节日", "庆典");

    private static final List<String> STORE_KEYWORDS = Arrays.asList(
            "门店", "店铺", "商场", "游乐场", "游乐园", "乐园",
            "餐厅", "影院", "电影院", "KTV");

    private static final List<String> SEARCH_KEYWORDS = Arrays.asList(
            "推荐", "找", "有什么", "搜索", "看看", "帮我找",
            "想要", "想买", "有没有", "哪些", "哪个");

    private static final List<String> NEGATION_KEYWORDS = Arrays.asList(
            "不要", "不想", "换一个", "换个", "别的", "算了",
            "不喜欢", "不好", "太贵", "太远");

    private static final List<String> FOLLOW_UP_CONSTRAINT_KEYWORDS = Arrays.asList(
            "便宜一点", "再便宜", "近一点", "再近", "好一点",
            "评分高", "换成", "改成");

    private static final List<String> CHITCHAT_KEYWORDS = Arrays.asList(
            "你好", "谢谢", "再见", "哈哈", "嗯", "好的",
            "ok", "谢啦", "拜拜", "晚安", "早安");

    // ---- 地域关键词 ----

    private static final List<String> LOCATION_KEYWORDS = Arrays.asList(
            "附近", "周边", "离我近", "身边", "旁边");

    // ---- 时间关键词 ----

    private static final List<String> TIME_KEYWORDS = Arrays.asList(
            "今天", "明天", "后天", "周末", "这个周末", "下周",
            "本周", "这周", "下个月");

    // ---- 价格关键词 ----

    private static final List<String> PRICE_KEYWORDS = Arrays.asList(
            "便宜", "贵", "免费", "以内", "以下", "元左右",
            "多少钱", "价格", "性价比");

    /**
     * 使用规则引擎解析用户输入，生成 ParsedIntent。
     * 置信度固定为 0.3（降级模式）。
     */
    public ParsedIntent parse(String input) {
        ParsedIntent intent = new ParsedIntent();
        intent.setQueryText(input);
        intent.setIntentConfidence(0.3);

        if (!StringUtils.hasText(input)) {
            intent.setTaskType(TaskType.CHITCHAT);
            return intent;
        }

        String text = input.trim();

        // 1. 闲聊检测（短文本 + 闲聊关键词）
        if (text.length() <= 4 && containsAny(text, CHITCHAT_KEYWORDS)) {
            intent.setTaskType(TaskType.CHITCHAT);
            return intent;
        }

        // 2. 否定/追问检测
        if (containsAny(text, NEGATION_KEYWORDS)) {
            intent.setNegation(true);
            intent.setFollowUp(true);
            intent.setFollowUpType(FollowUpType.NEGATE_RESULT);
            intent.setTaskType(TaskType.FOLLOW_UP);
            return intent;
        }

        if (containsAny(text, FOLLOW_UP_CONSTRAINT_KEYWORDS)) {
            intent.setFollowUp(true);
            intent.setFollowUpType(FollowUpType.ADD_CONSTRAINT);
            intent.setTaskType(TaskType.FOLLOW_UP);
            extractSlots(text, intent.getCandidateSlots());
            return intent;
        }

        // 3. FAQ 检测
        boolean isFaq = containsAny(text, FAQ_KEYWORDS);

        // 4. 实时检测
        boolean isRealtime = containsAny(text, REALTIME_KEYWORDS);

        // 5. 搜索类型检测
        boolean isEvent = containsAny(text, EVENT_KEYWORDS);
        boolean isStore = containsAny(text, STORE_KEYWORDS);
        boolean isSearch = containsAny(text, SEARCH_KEYWORDS) || isEvent || isStore;

        // 6. 组合判定
        if (isFaq && !isSearch && !isRealtime) {
            intent.setTaskType(TaskType.FAQ_QUERY);
            intent.setNeedExplanation(true);
        } else if (isRealtime && !isSearch && !isFaq) {
            intent.setTaskType(TaskType.REALTIME_QUERY);
            intent.setNeedRealtime(true);
        } else if (isSearch && isRealtime) {
            intent.setTaskType(TaskType.MIXED_SEARCH_REALTIME);
            intent.setNeedRealtime(true);
            intent.setNeedRecommendation(true);
        } else if (isSearch && isFaq) {
            intent.setTaskType(TaskType.MIXED_SEARCH_KNOWLEDGE);
            intent.setNeedExplanation(true);
            intent.setNeedRecommendation(true);
        } else if (isEvent) {
            intent.setTaskType(TaskType.EVENT_SEARCH);
            intent.setNeedRecommendation(true);
        } else if (isStore) {
            intent.setTaskType(TaskType.STORE_SEARCH);
            intent.setNeedRecommendation(true);
        } else if (isSearch) {
            intent.setTaskType(TaskType.PRODUCT_SEARCH);
            intent.setNeedRecommendation(true);
        } else {
            // 默认当搜索处理（比闲聊更安全的降级策略）
            intent.setTaskType(TaskType.PRODUCT_SEARCH);
            intent.setIntentConfidence(0.2);
        }

        // 7. 实时标记补充
        if (isRealtime) {
            intent.setNeedRealtime(true);
        }

        // 8. 槽位提取
        extractSlots(text, intent.getCandidateSlots());

        return intent;
    }

    /**
     * 从文本中提取候选槽位（简单关键词匹配）。
     */
    private void extractSlots(String text, CandidateSlots slots) {
        // 位置
        for (String kw : LOCATION_KEYWORDS) {
            if (text.contains(kw)) {
                slots.setLocationText(kw);
                break;
            }
        }

        // 时间
        for (String kw : TIME_KEYWORDS) {
            if (text.contains(kw)) {
                slots.setDateText(kw);
                break;
            }
        }

        // 价格
        for (String kw : PRICE_KEYWORDS) {
            if (text.contains(kw)) {
                slots.setPriceText(extractPriceExpression(text, kw));
                break;
            }
        }

        // 实体类型推断
        if (containsAny(text, EVENT_KEYWORDS)) {
            slots.setEntityType("event");
        } else if (containsAny(text, STORE_KEYWORDS)) {
            slots.setEntityType("store");
        } else {
            slots.setEntityType("product");
        }

        // 人群标签
        if (text.contains("亲子") || text.contains("小朋友") || text.contains("儿童") || text.contains("孩子")) {
            slots.setCrowdTagText("亲子");
        }

        // 场景标签
        if (text.contains("周末")) {
            slots.setSceneTagText("周末");
        }

        // 关键词：去掉已识别的标签后，剩下的文本作为关键词
        slots.setKeyword(text);
    }

    /**
     * 提取价格表达（简单实现：返回包含价格关键词的片段）。
     */
    private String extractPriceExpression(String text, String keyword) {
        int idx = text.indexOf(keyword);
        int start = Math.max(0, idx - 5);
        int end = Math.min(text.length(), idx + keyword.length() + 5);
        return text.substring(start, end).trim();
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
