# 智能检索系统运维手册

## 1. 启动前检查
- 确认 `W12 baselineReport` 无未处理 failed / dead-letter 任务
- 确认 `W13 release preflight` 通过，且 regression run 未过期
- 确认 `W14 final-summary` 中 `finalReady=true`
- 确认 Elasticsearch、MySQL、Python Query Parser、Realtime Gateway 可达

## 2. 日常巡检
- 查看治理台总览和上线准备中心
- 检查 realtime circuit 是否意外打开
- 检查 metrics daily / error attribution trend 是否出现异常抬升
- 检查最近 release record 的 verification / governance summary

## 3. 异常处置优先级
1. 用户主链不可用：先切回保守回答或 fallback
2. 实时链路异常：先执行 circuit reset / smoke，再决定是否切 fallback
3. 索引异常：优先校验 alias，再决定 rebuild 或 rollback
4. 评估回归失败：停止发布流转，回退到最近稳定 release

## 4. 管理端必用入口
- `/admin/governance/dashboard`
- `/admin/governance/release`
- `/admin/launch/center`

## 5. 发布前动作
- 执行固定版本 regression
- 执行 smoke
- 记录 load test
- 记录故障演练
- 生成 checklist snapshot
- 查看 final summary
