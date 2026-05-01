# 前后端技术选项（简要评估）

## 后端（当前：Spring Boot）
- Spring Boot + Spring AI (DashScope/qwen 等)：与现有代码兼容性最佳，适合企业级服务、成熟生态、可插拔 provider。
  - 优点：开发效率高、Spring 生态（Data/JPA/MVC）+ 社区支持。
  - 缺点：较重，对启动/内存敏感；需注意 Spring AI 与 provider 版本兼容。
- 轻量 Go / Rust 服务：更低延迟与内存占用，适合高并发推理网关。
  - 优点：性能好、容易控制资源。
  - 缺点：重构成本高，生态在快速集成 LLM 上不如 Java 成熟。

## 模型提供方（建议列入备选）
- 阿里 DashScope (当前整合)：推荐用于国内部署与 qwen 系列模型。
- Qwen 家族（阿里/腾讯合作模型）：对中文表现优秀，可作为主力模型。
- OpenAI / Anthropic：多场景良好，但受网络/费用影响；适合国际化或对比测试。

## 前端（UI）
- React + Vite：生态丰富，组件多，适合复杂交互。
- Vue 3 + Vite：上手快，模板更简洁，适合快速迭代。
- Svelte：更高性能、包体积小，但团队经验成本较高。

## 实时交互 / 通信
- WebSocket（或 Socket.IO）：适合低延迟回合交互与多玩家场景。
- HTTP Polling / Long-poll：实现简单，适合单人或非实时场景。

## 部署选项
- Docker Compose：便于本地与小规模部署（当前已使用）。
- Kubernetes：生产可扩展、灵活，适合大规模流量与灰度策略。

-- 选型建议：保持现有 Spring Boot 主干，模型 provider 配置化，必要时用轻量推理网关（Go/Rust）做前置限流与缓存。
