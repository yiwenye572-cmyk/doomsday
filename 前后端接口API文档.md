# 末日生存文字游戏平台｜前后端接口 API 文档（v1）

## 1. 文档范围
- 本文档覆盖 Vue3 前端与 Spring Boot 3 后端的联调接口。
- 对齐计划书中的多 Agent 编排、动态难度、RAG 检索、状态一致性与可观测要求。

## 1.1 重要 API 速览（当前对外主链路）

### A. 游戏主链路（已实现）
- `POST /api/v1/game/sessions`：创建会话
- `GET /api/v1/game/sessions/{sessionId}/state`：查询状态
- `POST /api/v1/game/sessions/{sessionId}/turns`：提交回合输入（核心）
- `POST /api/v1/game/sessions/{sessionId}/turns/{turn}/choose`：提交选项选择
- `POST /api/v1/game/sessions/{sessionId}/comeback-card`：触发翻盘卡

### B. 可观测管理（已实现）
- `GET /api/v1/admin/metrics/agents`：Agent 聚合指标（含阶段耗时/Token）
- `GET /api/v1/admin/metrics/traces`：最近 Trace 列表
- `GET /api/v1/admin/metrics/traces/{traceId}`：单条 Trace 详情

### C. 多模态最小闭环（已实现）
- `POST /api/v1/media/images/generate`：优先文生图，失败/超时自动回退图库
- `GET /api/v1/media/images/gallery-search`：显式图库检索

### D. 关键规划接口（规划中）
- `POST /api/v1/admin/evals/jobs`：创建离线评测任务
- `GET /api/v1/admin/evals/jobs/{jobId}`：查询评测任务结果

### E. 关键联调约束（强烈建议）
- 写接口统一带 `Idempotency-Key`。
- 有状态写入统一传 `expectedVersion`（防并发覆盖）。
- 前后端统一透传 `X-Trace-Id` 便于链路排障。

## 2. 基础约定

### 2.1 基础信息
- Base URL: `/api/v1`
- Content-Type: `application/json`
- 鉴权方式（当前实现）：暂未启用鉴权（预留 `Authorization: Bearer <token>`）
- 幂等请求头（关键写接口）：`Idempotency-Key: <uuid>`
- 链路追踪头：`X-Trace-Id: <trace_id>`（可选，后端也可自动生成）

### 2.2 统一响应体
```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "tr_20260501_xxx",
  "timestamp": 1760000000000
}
```

### 2.3 通用错误码
- `OK`: 成功
- `BAD_REQUEST`: 参数错误
- `NOT_FOUND`: 资源不存在
- `CONFLICT_VERSION`: 状态版本冲突（乐观锁）
- `RULE_VIOLATION`: 规则约束不通过
- `AGENT_ABORT`: 责任链中止
- `INJECTED_FAILURE`: 故障注入触发（仅演练环境）
- `INTERNAL_ERROR`: 系统内部错误

### 2.4 AI 返回字段说明（与前端约定）
- `plot.citations`: 数组，元素为字符串，格式如 `event_card:ev_033` 或 `lorebook:lb_zone_7`，前端用于高亮/查看证据来源。
- `plot.confidence`: 浮点数（0.0-1.0），表示模型置信度；低置信度时前端可展示“低置信度”提示并允许玩家复议。
- `options`: 必须返回恰好 4 个元素（若后端降级为静态候选也应保证 4 个），前端可断言长度并做兜底处理。
- `difficultyDelta` / `stateDelta`: 后端建议性的变更，前端仅用于展示，实际生效以 `state` 接口返回的 `version` 为准。

### 2.5 故障注入（仅测试环境）
- 开关：`game.chaos.enabled=true` 时生效。
- 请求头：`X-Doomsday-Failpoint`
- 支持值：
  - `submitTurn.beforeOrchestrator`
  - `submitTurn.afterOrchestrator`
  - `chooseOption.beforeApply`
  - `comeback.beforeApply`

---

## 3. 前端核心业务接口

## 3.1 创建游戏会话
- Method: `POST`
- Path: `/game/sessions`
- 说明：开始一局新游戏，玩家选择难度档位。

请求体：
```json
{
  "playerId": "u_1001",
  "difficulty": "SURVIVOR",
  "worldVersion": "world_v1",
  "styleProfile": "grim_realism"
}
```

`difficulty` 枚举：
- `SEEKER`（求生）
- `SURVIVOR`（幸存）
- `HELL`（地狱）

响应体：
```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "sessionId": "s_20260501_001",
    "initialState": {
      "version": 1,
      "hp": 100,
      "stamina": 100,
      "infection": 0,
      "location": "safe_house"
    },
    "challengeBand": [0.45, 0.7]
  },
  "traceId": "tr_xxx",
  "timestamp": 1760000000000
}
```

## 3.2 查询会话状态
- Method: `GET`
- Path: `/game/sessions/{sessionId}/state`
- 说明：用于前端刷新状态面板、背包、当前挑战指数。

响应 data：
```json
{
  "sessionId": "s_20260501_001",
  "version": 17,
  "hp": 62,
  "stamina": 35,
  "infection": 12,
  "location": "old_gas_station",
  "inventory": ["knife", "bandage"],
  "challengeIndex": 0.68,
  "challengeBand": [0.45, 0.7],
  "turn": 27
}
```

## 3.3 提交玩家回合输入（主接口）
- Method: `POST`
- Path: `/game/sessions/{sessionId}/turns`
- 说明：核心接口，触发 Router/Retrieval/Difficulty/Plot/Option/Guard/Commit 链路。

请求头：
- `Idempotency-Key` 必传

请求体：
```json
{
  "expectedVersion": 17,
  "playerInput": "我尝试绕到加油站后门搜刮药品",
  "clientTime": 1760000000123
}
```

响应 data：
```json
{
  "turn": 28,
  "newVersion": 18,
  "plot": {
    "text": "你贴着油罐阴影前行，潮湿的混凝土散发着铁锈与腐败味。门闩半断，屋内拖拽痕迹一路延向配电间。你知道自己没有太多体力可浪费，但这里可能藏着足以续命的药。远处传来玻璃摩擦声，像有什么东西正缓慢挪动。",
    "citations": ["event_card:ev_033", "lorebook:lb_zone_7"],
    "confidence": 0.84
  },
  "options": [
    {
      "id": "opt_a",
      "text": "撬开储物柜，快速搜药后撤离（激进）",
      "riskLevel": "HIGH",
      "expectedEffect": "可能获得稀有药品，但噪音显著上升"
    },
    {
      "id": "opt_b",
      "text": "原地观察 30 秒，先确认是否有感染者巡逻（稳健）",
      "riskLevel": "MEDIUM_LOW",
      "expectedEffect": "降低遭遇战概率，但推进速度略慢"
    },
    {
      "id": "opt_c",
      "text": "拆解附近废铁与布料，先做简易防护（资源导向）",
      "riskLevel": "MEDIUM",
      "expectedEffect": "短期收益一般，但可提升后续生存容错"
    },
    {
      "id": "opt_d",
      "text": "沿油站后墙潜行，尝试发现隐藏入口（探索导向）",
      "riskLevel": "MEDIUM_HIGH",
      "expectedEffect": "可能触发支线与高价值线索，也可能遭遇伏击"
    }
  ],
  "difficultyDelta": {
    "threat": 0.08,
    "loot": -0.05,
    "eventRate": 0.03,
    "bossProbability": 0.0
  },
  "stateDelta": {
    "stamina": -6,
    "noise": 12,
    "flagsAdded": ["found_medical_trace"]
  }
}
```

错误示例（版本冲突）：
```json
{
  "code": "CONFLICT_VERSION",
  "message": "state version mismatch",
  "data": {
    "expectedVersion": 17,
    "actualVersion": 18
  },
  "traceId": "tr_xxx",
  "timestamp": 1760000001111
}
```

## 3.4 提交选项选择
- Method: `POST`
- Path: `/game/sessions/{sessionId}/turns/{turn}/choose`
- 说明：玩家从 4 个选项中选择其一，推进状态机。

请求体：
```json
{
  "expectedVersion": 18,
  "optionId": "opt_b"
}
```

响应 data：
```json
{
  "turn": 28,
  "selected": "opt_b",
  "applied": true,
  "newVersion": 19,
  "stateDelta": {
    "stamina": -2,
    "noise": 0,
    "flagsAdded": []
  }
}
```

## 3.5 获取会话回放日志
- Method: `GET`
- Path: `/game/sessions/{sessionId}/replay`
- Query:
  - `fromTurn`（可选）
  - `toTurn`（可选）

响应 data（当前实现）：
```text
"MVP阶段暂未落库回放，sessionId=s_xxx, fromTurn=..., toTurn=..."
```

## 3.6 触发绝境翻盘卡
- Method: `POST`
- Path: `/game/sessions/{sessionId}/comeback-card`
- 说明：一次性机制，后端需校验濒危条件与单局次数。

请求体：
```json
{
  "expectedVersion": 24,
  "reason": "near_death"
}
```

响应 data：
```json
{
  "applied": true,
  "newVersion": 25,
  "effect": {
    "hp": "+25",
    "stamina": "+20",
    "buff": "panic_resist_3_turns"
  },
  "remainingCount": 0
}
```

---

## 4. 离线世界观工厂接口（管理端）

> 状态：以下接口为规划项。注意：监控类管理接口已在第 5 节单独标注为“已实现”。

## 4.1 提交世界观说明书
- Method: `POST`
- Path: `/admin/world-factory/jobs`
- 说明：上传或提交 AI 世界观文本，启动离线处理。

请求体：
```json
{
  "worldVersion": "world_v2",
  "sourceType": "TEXT",
  "content": "...世界观说明书全文...",
  "forceRebuild": false
}
```

响应 data：
```json
{
  "jobId": "wf_job_001",
  "status": "PENDING"
}
```

## 4.2 查询离线任务状态
- Method: `GET`
- Path: `/admin/world-factory/jobs/{jobId}`

响应 data：
```json
{
  "jobId": "wf_job_001",
  "status": "RUNNING",
  "progress": 68,
  "stages": [
    {"name": "chunking", "status": "DONE"},
    {"name": "extract", "status": "DONE"},
    {"name": "tagging", "status": "RUNNING"},
    {"name": "indexing", "status": "PENDING"}
  ]
}
```

## 4.3 事件卡查询
- Method: `GET`
- Path: `/admin/knowledge/event-cards`
- Query:
  - `worldVersion`
  - `tag`
  - `difficultyTier`
  - `page`, `size`

## 4.4 规则校验测试
- Method: `POST`
- Path: `/admin/rules/validate`

请求体：
```json
{
  "sessionState": {"hp": 35, "infection": 40},
  "candidateEventId": "ev_033_variant_b"
}
```

响应 data：
```json
{
  "pass": false,
  "violations": ["infection_too_high_for_event"],
  "suggestions": ["switch_to_ev_033_variant_c"]
}
```

---

## 5. 可观测与运维接口（管理端）

> 状态说明：
> - 已实现：`/api/v1/admin/metrics/agents`、`/api/v1/admin/metrics/traces`、`/api/v1/admin/metrics/traces/{traceId}`
> - 已实现基础运维：`/actuator/health`、`/actuator/metrics`
> - 其余接口为规划中

## 5.1 Agent 聚合指标（已实现）
- Method: `GET`
- Path: `/admin/metrics/agents`
- 说明：返回各 Agent 的调用统计、阶段耗时与 token 聚合均值。

响应 data（示例）：
```json
[
  {
    "agentName": "PlotGenerationAgent",
    "totalCalls": 128,
    "successCalls": 126,
    "failCalls": 2,
    "avgMs": 4210.5,
    "avgQueueWaitMs": 34.1,
    "avgModelMs": 3860.7,
    "avgPostProcessMs": 315.7,
    "avgPromptTokens": 812.3,
    "avgCompletionTokens": 386.2,
    "avgTokens": 1198.5,
    "successRate": 0.984
  }
]
```

## 5.2 最近 Trace 列表（已实现）
- Method: `GET`
- Path: `/admin/metrics/traces`
- Query:
  - `limit`（可选，默认 20，最大 100）

响应 data（示例）：
```json
[
  {
    "traceId": "trace_ab12cd34ef56",
    "sessionId": "s_1777981433161_29a1",
    "turn": 4,
    "startedAt": 1777982006000,
    "elapsedMs": 8725,
    "finalStatus": "OK",
    "spans": [
      {
        "agentName": "RouterAgent",
        "elapsedMs": 118,
        "status": "success",
        "errorMessage": null,
        "queueWaitMs": 2,
        "modelMs": 76,
        "postProcessMs": 42,
        "promptTokens": 210,
        "completionTokens": 18,
        "totalTokens": 228,
        "tokensPerSecond": 3000.0,
        "modelName": "qwen-turbo"
      }
    ]
  }
]
```

## 5.3 单条 Trace 详情（已实现）
- Method: `GET`
- Path: `/admin/metrics/traces/{traceId}`
- 说明：返回完整链路 span，用于排查单次慢请求。

错误示例：
```json
{
  "code": "NOT_FOUND",
  "message": "trace not found",
  "data": null,
  "traceId": "tr_xxx",
  "timestamp": 1760000001111
}
```

## 5.4 会话聚合指标（规划中）
- Method: `GET`
- Path: `/admin/metrics/sessions`
- Query:
  - `from`
  - `to`
  - `difficulty`

响应 data（示例字段）：
```json
{
  "avgTurnLatencyMs": 1320,
  "p95TurnLatencyMs": 2380,
  "tokenPerTurnAvg": 1280,
  "challengeBandHitRate": 0.67,
  "threeFailStreakRate": 0.08
}
```

---

## 6. 前端接口分层建议（Vue3）
- `src/api/http.ts`: axios 实例、拦截器、traceId 注入。
- `src/api/game.ts`: 会话、回合、选项、回放。
- `src/api/admin.ts`: 离线工厂、规则测试、监控查询。
- `src/types/api.ts`: 所有请求/响应 TypeScript 类型。

推荐约定：
- 所有 API Promise 返回统一 `ApiResponse<T>`。
- 前端只消费 `data`，错误统一在拦截器转业务异常。
- 对写接口统一附带 `expectedVersion` + `Idempotency-Key`。

---

## 7. 版本策略
- v1 路径固定 `/api/v1`。
- 破坏性变更升 `/api/v2`。
- 字段新增遵循向后兼容，不删除旧字段。

---

## 8. 关键增强接口（项目完善计划）

> 状态说明：以下为“完善项目计划书”中的关键 API；除特别标注“已实现”外，默认为规划中。

## 8.1 离线评测集管理（规划中）

### 8.1.1 创建评测任务
- Method: `POST`
- Path: `/admin/evals/jobs`
- 说明：基于离线评测集触发一次完整评估。

请求体：
```json
{
  "datasetId": "eval_ds_v1",
  "candidateVersion": "backend_20260505",
  "modes": ["intent", "retrieval", "consistency", "latency"]
}
```

响应 data：
```json
{
  "jobId": "eval_job_001",
  "status": "PENDING"
}
```

### 8.1.2 查询评测任务
- Method: `GET`
- Path: `/admin/evals/jobs/{jobId}`

响应 data（示例）：
```json
{
  "jobId": "eval_job_001",
  "status": "DONE",
  "summary": {
    "intentAccuracy": 0.91,
    "retrievalRecallAt5": 0.86,
    "ruleConflictRate": 0.03,
    "p95LatencyMs": 9580,
    "avgTokensPerTurn": 1620
  }
}
```

## 8.2 文生图 + 图库兜底最小闭环（已实现最小闭环）

### 8.2.1 生成剧情配图
- Method: `POST`
- Path: `/media/images/generate`
- 说明：先尝试文生图，超时或失败则自动回退图库检索。
- 环境变量：`PEXELS_API_KEY` 必须配置，否则回退链路不可用。

请求体：
```json
{
  "sessionId": "s_20260501_001",
  "traceId": "trace_ab12cd34ef56",
  "prompt": "末日夜雨中的废弃加油站，冷色调，压抑氛围",
  "style": "grim_realism",
  "timeoutMs": 3000
}
```

响应 data：
```json
{
  "imageUrl": "https://cdn.example.com/game/scene_001.webp",
  "source": "generated",
  "fallback": false,
  "fallbackReason": null,
  "provider": "dashscope",
  "latencyMs": 1820
}
```

回退示例（当前环境常见）：
```json
{
  "imageUrl": "https://images.pexels.com/photos/31503000/pexels-photo-31503000.jpeg?...",
  "source": "gallery",
  "fallback": true,
  "fallbackReason": "dashscope status=403",
  "provider": "pexels",
  "latencyMs": 1057
}
```

### 8.2.2 图库检索（显式调用）
- Method: `GET`
- Path: `/media/images/gallery-search`
- Query:
  - `q`（剧情关键词）
  - `limit`（默认 5）

响应 data（示例）：
```json
[
  {
    "imageUrl": "https://images.pexels.com/xxx.jpeg",
    "provider": "pexels",
    "author": "John Doe",
    "license": "Pexels License"
  }
]
```

## 8.3 世界观预编译与冲突仲裁（已实现核心）

### 8.3.1 创建世界观预编译任务
- Method: `POST`
- Path: `/admin/world-factory/jobs`
- 说明：提交世界观原文后，启动离线预编译流水线（chunk/extract/tag/index）。

请求体：
```json
{
  "worldVersion": "world_v3",
  "sourceType": "TEXT",
  "content": "...世界观说明书全文...",
  "forceRebuild": false
}
```

响应 data：
```json
{
  "jobId": "wf_job_20260506_001",
  "status": "PENDING"
}
```

### 8.3.2 查询预编译任务状态
- Method: `GET`
- Path: `/admin/world-factory/jobs/{jobId}`

响应 data：
```json
{
  "jobId": "wf_job_20260506_001",
  "status": "RUNNING",
  "progress": 72,
  "stage": "tagging",
  "errorMessage": null
}
```

### 8.3.3 冲突仲裁（在线）
- Method: `POST`
- Path: `/admin/arbitration/evaluate`
- 说明：按“规则校验 -> 语义对齐 -> 高风险投票（riskScore >= 0.7）”输出裁决结果。

请求体：
```json
{
  "sessionId": "s_20260501_001",
  "candidateEventId": "ev_033_variant_b",
  "riskScore": 0.76,
  "stateSnapshot": {
    "hp": 35,
    "stamina": 28,
    "infection": 42
  }
}
```

### 8.3.4 游戏世界初始化（已实现）
- Method: `POST`
- Path: `/game/worlds/initialize`
- 说明：根据玩家输入的基础设定触发 AI 生成世界书并启动 WorldFactory 离线任务。

请求体：
```json
{
  "worldTheme": "极寒核冬天",
  "eraStyle": "工业废土",
  "survivalTone": "高压生存",
  "keyFaction": "夜巡队",
  "forbiddenRule": "高噪声动作不得连续两回合"
}
```

响应 data：
```json
{
  "worldVersion": "world_1778055039742",
  "jobId": "wf_job_fcd2e9af4a7c",
  "status": "RUNNING",
  "message": "world factory job accepted"
}
```

### 8.3.5 默认世界书（已实现）
- Method: `GET`
- Path: `/game/worlds/default`
- 说明：玩家跳过创建世界时，前端应调用本接口获取默认世界版本并建局。

响应 data：
```json
{
  "worldVersion": "world_v1",
  "title": "默认世界书",
  "description": "玩家跳过创建时使用该默认世界书，保障开局可玩。"
}
```

### 8.3.6 建局接口新增字段（已实现）
- `POST /game/sessions`：入参 `worldVersion` 已生效。
- `GET /game/sessions/{sessionId}/state`：响应新增 `worldVersion` 字段。

响应 data：
```json
{
  "pass": false,
  "layerResult": {
    "ruleValidation": "FAIL",
    "semanticAlignment": "PASS",
    "agentVoting": "REJECT"
  },
  "finalAction": "SWITCH_EVENT",
  "suggestedEventId": "ev_033_variant_c",
  "reason": "infection_too_high_for_event"
}
```

## 8.4 游戏日记系统（规划中）

### 8.4.1 查询会话日记
- Method: `GET`
- Path: `/game/sessions/{sessionId}/diary`
- Query:
  - `level`：`L0` | `L1` | `L2`
  - `fromTurn`（可选）
  - `toTurn`（可选）

响应 data（L1 示例）：
```json
[
  {
    "sessionId": "s_20260501_001",
    "level": "L1",
    "turnRange": "21-30",
    "summary": "你在油站区域完成两次高风险搜刮并建立临时补给点。",
    "tags": ["gas_station", "resource", "high_risk"],
    "createdAt": 1760001234567
  }
]
```

### 8.4.2 强制触发日记摘要
- Method: `POST`
- Path: `/admin/diary/force-summarize`

请求体：
```json
{
  "sessionId": "s_20260501_001",
  "fromTurn": 21,
  "toTurn": 30
}
```

响应 data：
```json
{
  "accepted": true,
  "jobId": "diary_job_001",
  "status": "PENDING"
}
```

## 8.5 ReAct 标准化 Tool Calling（规划中）

### 8.5.1 通用工具调用入口（内部编排使用）
- Method: `POST`
- Path: `/internal/tool/call`
- 说明：由编排器调用，不对前端暴露。

请求体：
```json
{
  "idempotencyKey": "tool_1746500000_xxx",
  "traceId": "trace_ab12cd34ef56",
  "callerAgent": "PlotGenerationAgent",
  "toolName": "WorldQueryTool",
  "timeoutMs": 1200,
  "payload": {
    "query": "old_gas_station medical stash"
  }
}
```

响应 data：
```json
{
  "toolName": "WorldQueryTool",
  "status": "SUCCESS",
  "retryCount": 1,
  "latencyMs": 88,
  "result": {
    "hits": ["event_card:ev_033", "lorebook:lb_zone_7"]
  },
  "errorCode": null
}
```

### 8.5.2 工具注册列表（管理端）
- Method: `GET`
- Path: `/admin/tools`

响应 data：
```json
[
  {
    "toolName": "WorldQueryTool",
    "enabled": true,
    "version": "v1",
    "sideEffect": false
  },
  {
    "toolName": "EntityStatePatchTool",
    "enabled": true,
    "version": "v1",
    "sideEffect": true
  }
]
```
