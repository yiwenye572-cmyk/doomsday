# 重构回顾 — Spring AI (DashScope) 集成注意事项

## 问题与修复
- DashScope Builder API 变更：使用 `maxToken`（单数）而非 `maxTokens`；builder 方法签名需传 `Integer`。已在 `AiConfig.java` 里兼容。
- 所有 LLM 调用点必须加 try/catch 并提供本地模板或静态 fallback，以保证服务可用性。
- 向量检索（pgvector/VectorStore）需对 `topK`、`similarityThreshold` 做上限，避免无界查询。
- 避免将 `target/`、编译产物、日志等提交到 Git；建议在 `.gitignore` 中明确排除。

## 测试与验证
- 本地构建：`mvn -DskipTests clean package`（建议在容器与本机分别验证）。
- 容器验证：`docker compose build backend` + 启动后做冒烟（创建会话、提交回合、确认 options=4）。

## 部署注意
- 必要环境变量：`DASHSCOPE_API_KEY`、数据库/redis 连接、Spring profile。生产环境请用密钥管理。
- 模型与 provider 切换点集中在 `AiConfig.java`，便于未来扩展与流量灰度切换。

## 性能与安全建议
- 对 LLM 与向量检索设置超时与限流；对热点接口增加监控告警。
- 日志中不要输出完整 prompt 或用户敏感字段，需脱敏或降级记录。

## 后续改进
- 增加 E2E 集成测试（mock LLM + mock VectorStore）。
- 添加模型使用统计与费用追踪，支持按需切换。
