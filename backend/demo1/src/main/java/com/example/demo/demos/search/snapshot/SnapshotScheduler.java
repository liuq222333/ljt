package com.example.demo.demos.search.snapshot;

import com.example.demo.demos.CommunityMarket.Dao.ProductsMapper;
import com.example.demo.demos.CommunityMarket.Pojo.Product;
import com.example.demo.demos.LocalActive.Dao.LocalActivityMapper;
import com.example.demo.demos.LocalActive.Pojo.LocalActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 快照定时调度器 — 全量/增量构建搜索快照。
 * 与施工单 W02 对齐。
 *
 * 全量构建：每天凌晨 2 点执行，遍历所有源数据重新构建快照。
 * 增量构建：每 5 分钟执行一次，仅处理最近更新的数据（W12 同步补偿阶段完善）。
 */
@Component
public class SnapshotScheduler {

    private static final Logger log = LoggerFactory.getLogger(SnapshotScheduler.class);

    @Autowired
    private ProductsMapper productsMapper;

    @Autowired
    private LocalActivityMapper localActivityMapper;

    @Autowired
    private ProductSnapshotBuilder productSnapshotBuilder;

    @Autowired
    private EventSnapshotBuilder eventSnapshotBuilder;

    /**
     * 全量构建商品快照 — 每天凌晨 2:00 执行。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void fullBuildProductSnapshots() {
        log.info("=== 开始全量构建商品快照 ===");
        long start = System.currentTimeMillis();
        int success = 0;
        int fail = 0;

        try {
            List<Product> products = productsMapper.getAllProducts();
            log.info("待构建商品数量: {}", products.size());

            for (Product product : products) {
                try {
                    productSnapshotBuilder.buildAndSave(product);
                    success++;
                } catch (Exception e) {
                    fail++;
                    log.error("商品快照构建失败: productId={}, error={}", product.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("全量构建商品快照异常: {}", e.getMessage(), e);
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("=== 全量构建商品快照完成: success={}, fail={}, elapsed={}ms ===", success, fail, elapsed);
    }

    /**
     * 全量构建活动快照 — 每天凌晨 2:30 执行。
     */
    @Scheduled(cron = "0 30 2 * * ?")
    public void fullBuildEventSnapshots() {
        log.info("=== 开始全量构建活动快照 ===");
        long start = System.currentTimeMillis();
        int success = 0;
        int fail = 0;

        try {
            List<LocalActivity> activities = localActivityMapper.listAll();
            log.info("待构建活动数量: {}", activities.size());

            for (LocalActivity activity : activities) {
                try {
                    eventSnapshotBuilder.buildAndSave(activity);
                    success++;
                } catch (Exception e) {
                    fail++;
                    log.error("活动快照构建失败: activityId={}, error={}", activity.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("全量构建活动快照异常: {}", e.getMessage(), e);
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("=== 全量构建活动快照完成: success={}, fail={}, elapsed={}ms ===", success, fail, elapsed);
    }

    /**
     * 手动触发全量构建（供接口调用）。
     */
    public void triggerFullBuild() {
        log.info("手动触发全量快照构建");
        fullBuildProductSnapshots();
        fullBuildEventSnapshots();
    }
}
