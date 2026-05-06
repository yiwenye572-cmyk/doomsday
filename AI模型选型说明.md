# AI 模型选型说明

> 本文档说明本项目中各 Agent 使用的阿里云百炼（DashScope）模型类型及调用方式。  
> **请在阅读后确认加 ✅，或指定你想替换的模型名称。**

---

## 1. 当前模型配置（待你确认）

| Agent / 用途 | 模型名称 | 模型系列 | 说明 |
|---|---|---|---|
| Plot Generation（叙事生成） | `qwen-plus` | 千问 Plus | 主力生成模型，质量/成本平衡好，**需确认是否替换为 `qwen3.6-plus`** |
| Option Generation（选项生成） | `qwen-plus` | 千问 Plus | 结构化输出 JSON 4 选项 |
| Router（意图分类） | `qwen-turbo` | 千问 Turbo | 轻量分类任务，响应快、Token 消耗低 |
| Narration（文风润色） | `qwen-turbo` | 千问 Turbo | 对已生成文案做轻量风格统一 |
| Embedding（向量检索） | `text-embedding-v3` | 通义 Embedding | 1536 维，与 PgVector 匹配，**需确认是否替换为 `text-embedding-v4`** |

---

## 2. 阿里云百炼当前在售主力模型速览

### 2.1 文本生成（Chat）

| 模型名称 | 定位 | 特点 |
|---|---|---|
| `qwen3.6-max-preview` | 最强能力，预览版 | 最新旗舰，能力最强，价格最高 |
| `qwen3.6-plus` | 高质量生产版 | 性能优秀，价格居中 |
| `qwen3.6-flash` | 快速廉价版 | 响应快，适合分类/路由等轻任务 |
| `qwen-max` | 经典旗舰（稳定） | 老一代旗舰，文档丰富，Spring AI 示例多 |
| `qwen-plus` | 经典均衡（稳定） | **推荐默认**，质量好，Spring AI Alibaba 官方示例默认用此 |
| `qwen-turbo` | 经典快速（稳定） | 轻量任务首选，延迟低 |
| `qwen-long` | 长上下文 | 支持 1M token 上下文，适合长剧情记忆 |
| `deepseek-v4-pro` | 三方接入 | DeepSeek R2 级能力，API 格式与千问一致 |
| `deepseek-v4-flash` | 三方接入 | DeepSeek 轻量版 |

### 2.2 向量 Embedding

| 模型名称 | 维度 | 说明 |
|---|---|---|
| `text-embedding-v4` | 可选：512/1024/2048 | **最新版**，多维度，推荐新项目使用 |
| `text-embedding-v3` | 可选：1024/1536/2048 | **当前配置**，1536 维对应现有 PgVector schema |

> ⚠️ **重要**：如果切换 Embedding 模型，必须同步调整 `embedding VECTOR(1536)` 的维度设置，并清空已有 embedding 数据重新入库。

### 2.3 重排序（Rerank，用于 RAG 召回后重排）

| 模型名称 | 说明 |
|---|---|
| `qwen3-rerank` | 最新重排序模型，适合 RAG 精排 |
| `gte-rerank` | 经典重排序模型 |

---

## 3. 本项目调用方式说明

### 3.1 ChatClient（对话/生成/分类）

Spring AI Alibaba 通过 `ChatClient` 统一封装，底层调用 DashScope Chat API。

```java
// 叙事生成（PlotGenerationAgent）- 使用 qwen-plus
chatClient.prompt()
    .system("你是一个末日文字冒险游戏叙事引擎...")
    .user(buildUserPrompt(ctx))
    .call()
    .content();

// 意图分类（RouterAgent）- 使用 qwen-turbo
chatClient.prompt()
    .user(classifyPrompt)
    .call()
    .entity(RouterOutput.class);  // 结构化 JSON 输出
```

**配置文件关键字段**（`application.yml`）：
```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-plus   # 默认主力模型，各 Agent 可按需 override
```

### 3.2 EmbeddingModel + VectorStore（RAG 向量检索）

Spring AI Alibaba 自动注入 `DashScopeEmbeddingModel` bean，配合 `PgVectorStore` 使用：

```java
// RetrievalAgent 向量相似度搜索
List<Document> docs = vectorStore.similaritySearch(
    SearchRequest.builder()
        .query(playerInput)
        .topK(3)
        .similarityThreshold(0.6)
        .build()
);
```

**配置**：
```yaml
spring:
  ai:
    dashscope:
      embedding:
        options:
          model: text-embedding-v3
    vectorstore:
      pgvector:
        dimensions: 1536
        distance-type: COSINE_DISTANCE
        initialize-schema: false   # 由 Flyway V3 管理建表
```

### 3.3 Function Calling / Tool（预留）

Spring AI 提供 `@Tool` 注解或 `FunctionCallback` 方式注册工具：

```java
@Tool(description = "查询当前游戏状态")
public String queryGameState(String sessionId) { ... }
```

目前 P1 阶段暂不启用，P3 阶段接入。

---

## 4. ⚠️ 需要你确认的问题

1. **主力叙事模型**：用 `qwen-plus`（稳定，Spring AI 示例多）还是 `qwen3.6-plus`（最新，效果更好但较新）？
2. **Embedding 模型**：保持 `text-embedding-v3`（1024 维，当前 schema 匹配）还是升级到 `text-embedding-v4`（需改维度）？
3. **Router/Narration**：用 `qwen-turbo` 还是 `qwen3.6-flash`（更新的轻量模型）？
4. **是否接入第三方模型**：如 `deepseek-v4-pro`（通过百炼调用，API 格式一致）？

---

## 5. API Key 获取方式

1. 登录 [阿里云百炼控制台](https://bailian.console.aliyun.com/)
2. 左侧菜单 → **API-KEY 管理** → 新建或复制 Key
3. 复制后填入：
   - 本地开发：`.env` 文件或系统环境变量 `DASHSCOPE_API_KEY=sk-xxx`
   - Docker：`docker-compose.yml` 中 `DASHSCOPE_API_KEY=${DASHSCOPE_API_KEY}` 或直接填写

---

## 6. Token 消耗与成本估算（参考）

| Agent | 模型 | 估算 Input/Output tokens | 单回合消耗 |
|---|---|---|---|
| Router | qwen-turbo | ~200 / 30 | 极低 |
| Retrieval | text-embedding-v3 | ~100 | 极低 |
| Plot Generation | qwen-plus | ~800 / 400 | 中等 |
| Option Generation | qwen-plus | ~600 / 300 | 中等 |
| Narration | qwen-turbo | ~600 / 400 | 低 |
| **合计/回合** | - | ~2300 / 1130 | **约 0.01-0.03 元** |

> 实际消耗与剧情长度、Context 长度有关。启用 L1 摘要后 token 可大幅下降（目标 ≥40%）。
