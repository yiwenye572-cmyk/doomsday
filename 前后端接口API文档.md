# 末日生存文字游戏平台｜前后端接口 API 文档（v1）

## 1. 文档范围
- 本文档覆盖 Vue3 前端与 Spring Boot 3 后端的联调接口。
- 对齐计划书中的多 Agent 编排、动态难度、RAG 检索、状态一致性与可观测要求。

## 2. 基础约定

### 2.1 基础信息
- Base URL: `/api/v1`
- Content-Type: `application/json`
- 鉴权方式（建议）：`Authorization: Bearer <token>`
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
- `UNAUTHORIZED`: 未授权
- `FORBIDDEN`: 禁止访问
- `NOT_FOUND`: 资源不存在
- `CONFLICT_VERSION`: 状态版本冲突（乐观锁）
- `TOOL_CALL_FAILED`: 工具调用失败
- `RULE_VIOLATION`: 规则约束不通过
- `RETRY_LATER`: 下游繁忙建议重试
- `INTERNAL_ERROR`: 系统内部错误

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
    "riskExposure": -4
  }
}
```

## 3.5 获取会话回放日志
- Method: `GET`
- Path: `/game/sessions/{sessionId}/replay`
- Query:
  - `fromTurn`（可选）
  - `toTurn`（可选）

响应 data：
```json
{
  "sessionId": "s_20260501_001",
  "events": [
    {
      "turn": 27,
      "playerInput": "...",
      "plotSummary": "...",
      "choice": "opt_b",
      "version": 19,
      "traceId": "tr_xxx"
    }
  ]
}
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

## 5.1 会话指标查询
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

## 5.2 链路追踪详情
- Method: `GET`
- Path: `/admin/traces/{traceId}`

响应 data（示例）：
```json
{
  "traceId": "tr_xxx",
  "agents": [
    {"name": "RouterAgent", "costMs": 46, "status": "OK"},
    {"name": "RetrievalAgent", "costMs": 128, "status": "OK"},
    {"name": "DifficultyDirectorAgent", "costMs": 34, "status": "OK"},
    {"name": "PlotGenerationAgent", "costMs": 712, "status": "OK"}
  ],
  "finalStatus": "OK"
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
