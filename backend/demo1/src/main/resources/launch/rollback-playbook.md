# 智能检索系统回滚预案

## 1. 触发条件
- 发布后关键指标显著恶化
- 评估回放出现高风险 regression
- realtime gateway 大面积失败且 fallback 无法接受
- 搜索索引 alias 指向异常或结果错误率明显上升

## 2. 回滚顺序
1. 停止 release record 继续流转
2. 固定当前故障窗口的 replay / metrics / error attribution
3. 若问题在索引层：
   - 查看 `products/index/rollback/candidates`
   - 执行自动回滚或手动切换 alias
4. 若问题在灰度策略：
   - 移除 gray config
   - 将 release record 回退到 `ready` 或 `rolled_back`
5. 若问题在 realtime：
   - 强制 fallback
   - 保留 smoke 结果和 circuit 状态截图

## 3. 回滚后验证
- 重新执行 smoke
- 检查 W12 baseline 和 alias validation
- 检查 W13 governance summary
- 记录 rollback 事件与处置说明

## 4. 必须沉淀的证据
- release record / release events
- final summary
- smoke 结果
- load test / drill 记录
- 关键 replay 样本
