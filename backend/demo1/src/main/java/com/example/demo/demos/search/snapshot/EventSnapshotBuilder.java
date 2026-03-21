package com.example.demo.demos.search.snapshot;

import com.example.demo.demos.LocalActive.Pojo.LocalActivity;
import com.example.demo.demos.common.enums.SearchableStatus;
import com.example.demo.demos.search.entity.EventSearchSnapshot;
import com.example.demo.demos.search.entity.SearchCategory;
import com.example.demo.demos.search.mapper.EventSearchSnapshotMapper;
import com.example.demo.demos.search.mapper.SearchCategoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动快照构建器 — 从 LocalActivity 构建 EventSearchSnapshot。
 */
@Component
public class EventSnapshotBuilder {

    private static final Logger log = LoggerFactory.getLogger(EventSnapshotBuilder.class);

    @Autowired
    private SearchCategoryMapper searchCategoryMapper;

    @Autowired
    private EventSearchSnapshotMapper eventSearchSnapshotMapper;

    @Autowired
    private SearchableStatusCalculator statusCalculator;

    /**
     * 从 LocalActivity 构建活动搜索快照。
     */
    public EventSearchSnapshot buildFromActivity(LocalActivity activity) {
        EventSearchSnapshot snapshot = new EventSearchSnapshot();

        snapshot.setEventId(activity.getId());
        snapshot.setOrganizerId(activity.getOrganizerUserId() != null
                ? activity.getOrganizerUserId().longValue() : 0L);
        snapshot.setTitle(activity.getTitle());
        snapshot.setSubtitle(activity.getSubtitle());
        snapshot.setSummaryText(activity.getDescription());

        // 地理位置
        if (activity.getLatitude() != null) {
            snapshot.setLat(BigDecimal.valueOf(activity.getLatitude()));
        }
        if (activity.getLongitude() != null) {
            snapshot.setLng(BigDecimal.valueOf(activity.getLongitude()));
        }

        // 价格
        if (activity.getFeeAmount() != null) {
            if ("free".equalsIgnoreCase(activity.getFeeType())) {
                snapshot.setMinPrice(BigDecimal.ZERO);
                snapshot.setMaxPrice(BigDecimal.ZERO);
            } else {
                snapshot.setMinPrice(activity.getFeeAmount());
                snapshot.setMaxPrice(activity.getFeeAmount());
            }
        }

        // 时间
        snapshot.setEventStartTime(activity.getStartAt());
        snapshot.setEventEndTime(activity.getEndAt());

        // 类目：从 categoryCode 映射
        enrichCategory(snapshot, activity.getCategoryCode());

        // 状态映射
        mapActivityStatus(snapshot, activity.getStatus());

        // 计算 searchable_status
        SearchableStatus searchableStatus = statusCalculator.calculateEvent(
                snapshot.getPublishStatus(),
                snapshot.getAuditStatus(),
                snapshot.getVisibleStatus(),
                snapshot.getEventEndTime()
        );
        snapshot.setSearchableStatus(searchableStatus.getCode());

        // 默认评分
        snapshot.setSalesCount(0L);
        snapshot.setRating(BigDecimal.ZERO);
        snapshot.setHotScore(BigDecimal.ZERO);
        snapshot.setRecommendScore(BigDecimal.ZERO);

        snapshot.setCreatedAt(LocalDateTime.now());
        snapshot.setUpdatedAt(LocalDateTime.now());
        snapshot.setSearchableUpdatedAt(LocalDateTime.now());

        return snapshot;
    }

    /**
     * 构建并保存（upsert）。
     */
    public EventSearchSnapshot buildAndSave(LocalActivity activity) {
        EventSearchSnapshot snapshot = buildFromActivity(activity);

        EventSearchSnapshot existing = eventSearchSnapshotMapper.selectByEventId(activity.getId());
        if (existing != null) {
            snapshot.setId(existing.getId());
            eventSearchSnapshotMapper.updateByEventId(snapshot);
            log.info("更新活动快照: eventId={}", activity.getId());
        } else {
            eventSearchSnapshotMapper.insert(snapshot);
            log.info("新建活动快照: eventId={}", activity.getId());
        }

        return snapshot;
    }

    private void enrichCategory(EventSearchSnapshot snapshot, String categoryCode) {
        if (categoryCode == null) {
            snapshot.setCategoryId(0L);
            snapshot.setCategoryName("未分类");
            snapshot.setCategoryPath("0");
            return;
        }

        try {
            Long categoryId = Long.parseLong(categoryCode);
            snapshot.setCategoryId(categoryId);
            SearchCategory category = searchCategoryMapper.selectById(categoryId);
            if (category != null) {
                snapshot.setCategoryName(category.getCategoryName());
                snapshot.setCategoryPath(category.getCategoryPath());
            } else {
                snapshot.setCategoryName("未分类");
                snapshot.setCategoryPath(String.valueOf(categoryId));
            }
        } catch (NumberFormatException e) {
            // categoryCode 不是数字，直接作为名称
            snapshot.setCategoryId(0L);
            snapshot.setCategoryName(categoryCode);
            snapshot.setCategoryPath("0");
        }
    }

    private void mapActivityStatus(EventSearchSnapshot snapshot, String activityStatus) {
        if (activityStatus == null) {
            snapshot.setPublishStatus("off_shelf");
            snapshot.setAuditStatus("pending");
            snapshot.setVisibleStatus("hidden");
            return;
        }

        switch (activityStatus.toLowerCase()) {
            case "published":
            case "active":
                snapshot.setPublishStatus("on_shelf");
                snapshot.setAuditStatus("approved");
                snapshot.setVisibleStatus("visible");
                break;
            case "draft":
                snapshot.setPublishStatus("off_shelf");
                snapshot.setAuditStatus("pending");
                snapshot.setVisibleStatus("hidden");
                break;
            case "cancelled":
            case "closed":
                snapshot.setPublishStatus("off_shelf");
                snapshot.setAuditStatus("approved");
                snapshot.setVisibleStatus("hidden");
                break;
            default:
                snapshot.setPublishStatus("off_shelf");
                snapshot.setAuditStatus("pending");
                snapshot.setVisibleStatus("hidden");
                break;
        }
    }
}
