package com.example.demo.demos.search.snapshot;

import com.example.demo.demos.common.enums.SearchableStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * searchable_status 统一计算器。
 * 根据各维度状态（上架、审核、可见、有效期等）综合判定实体是否可搜索。
 * 与 W00 SearchableStatus 枚举对齐。
 */
@Component
public class SearchableStatusCalculator {

    private static final Logger log = LoggerFactory.getLogger(SearchableStatusCalculator.class);

    /**
     * 计算商品的 searchable_status。
     *
     * @param publishStatus 上架状态：on_shelf / off_shelf
     * @param auditStatus   审核状态：pending / approved / rejected
     * @param visibleStatus 可见状态：visible / hidden
     * @param sourceStatus  源系统状态（可为 null）
     * @return SearchableStatus 枚举
     */
    public SearchableStatus calculateProduct(String publishStatus, String auditStatus,
                                             String visibleStatus, String sourceStatus) {
        // 1. 源系统已删除或禁用
        if (sourceStatus != null && ("deleted".equalsIgnoreCase(sourceStatus)
                || "disabled".equalsIgnoreCase(sourceStatus))) {
            return SearchableStatus.DISABLED;
        }

        // 2. 审核未通过
        if (!"approved".equalsIgnoreCase(auditStatus)) {
            if ("pending".equalsIgnoreCase(auditStatus)) {
                return SearchableStatus.DRAFT;
            }
            return SearchableStatus.NOT_SEARCHABLE;
        }

        // 3. 未上架
        if (!"on_shelf".equalsIgnoreCase(publishStatus)) {
            return SearchableStatus.NOT_SEARCHABLE;
        }

        // 4. 不可见
        if (!"visible".equalsIgnoreCase(visibleStatus)) {
            return SearchableStatus.NOT_SEARCHABLE;
        }

        // 5. 全部通过 → 可搜索
        return SearchableStatus.SEARCHABLE;
    }

    /**
     * 计算活动的 searchable_status（含时间维度）。
     *
     * @param publishStatus 上架状态
     * @param auditStatus   审核状态
     * @param visibleStatus 可见状态
     * @param eventEndTime  活动结束时间（可为 null）
     * @return SearchableStatus 枚举
     */
    public SearchableStatus calculateEvent(String publishStatus, String auditStatus,
                                           String visibleStatus, LocalDateTime eventEndTime) {
        // 1. 审核未通过
        if (!"approved".equalsIgnoreCase(auditStatus)) {
            if ("pending".equalsIgnoreCase(auditStatus)) {
                return SearchableStatus.DRAFT;
            }
            return SearchableStatus.NOT_SEARCHABLE;
        }

        // 2. 未上架
        if (!"on_shelf".equalsIgnoreCase(publishStatus)) {
            return SearchableStatus.NOT_SEARCHABLE;
        }

        // 3. 不可见
        if (!"visible".equalsIgnoreCase(visibleStatus)) {
            return SearchableStatus.NOT_SEARCHABLE;
        }

        // 4. 活动已过期
        if (eventEndTime != null && eventEndTime.isBefore(LocalDateTime.now())) {
            return SearchableStatus.EXPIRED;
        }

        return SearchableStatus.SEARCHABLE;
    }

    /**
     * 计算门店的 searchable_status。
     *
     * @param publishStatus 发布状态
     * @param visibleStatus 可见状态
     * @return SearchableStatus 枚举
     */
    public SearchableStatus calculateStore(String publishStatus, String visibleStatus) {
        if (!"on_shelf".equalsIgnoreCase(publishStatus)) {
            return SearchableStatus.NOT_SEARCHABLE;
        }

        if (!"visible".equalsIgnoreCase(visibleStatus)) {
            return SearchableStatus.NOT_SEARCHABLE;
        }

        return SearchableStatus.SEARCHABLE;
    }
}
