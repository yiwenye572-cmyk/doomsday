<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import { getAgentMetrics, getRecentTraces, getToolSummary, getToolAudits, getMetricsOverview } from "../api/admin";
import type { AgentMetricsSummary, TraceDetail, ToolSummary, ToolAuditItem, TraceMetricsComparison } from "../api/admin";
import http from "../api/http";
import type { ApiResponse } from "../types/api";

const metrics = ref<AgentMetricsSummary[]>([]);
const traces = ref<TraceDetail[]>([]);
const toolSummary = ref<ToolSummary[]>([]);
const toolAudits = ref<ToolAuditItem[]>([]);
const overview = ref<TraceMetricsComparison | null>(null);
const selectedTrace = ref<TraceDetail | null>(null);
const loading = ref(false);
const error = ref("");

// Diary 操作
const diarySessionId = ref("");
const diaryFromTurn = ref(1);
const diaryToTurn = ref(10);
const diaryResult = ref("");
const diaryLoading = ref(false);

// 自动刷新
let autoRefreshTimer: ReturnType<typeof setInterval> | null = null;
const autoRefresh = ref(true);

// 全局 KPI（从现有数据聚合）
const kpi = computed(() => {
  const totalCalls = metrics.value.reduce((s, m) => s + m.totalCalls, 0);
  const totalFail = metrics.value.reduce((s, m) => s + m.failCalls, 0);
  const overallSuccessRate = totalCalls > 0 ? ((totalCalls - totalFail) / totalCalls * 100).toFixed(1) : "—";
  const avgToken = metrics.value.length > 0
    ? (metrics.value.reduce((s, m) => s + m.avgTokens, 0) / metrics.value.length).toFixed(0)
    : "—";
  // P95 耗时：取 traces 中按 elapsedMs 排序的 95% 分位
  const sorted = [...traces.value].sort((a, b) => a.elapsedMs - b.elapsedMs);
  const p95 = sorted.length > 0
    ? sorted[Math.floor(sorted.length * 0.95)] ?? sorted[sorted.length - 1]
    : null;
  const p95Ms = p95 ? p95.elapsedMs : null;
  const toolTotalCalls = toolSummary.value.reduce((s, t) => s + t.totalCalls, 0);
  return {
    totalCalls,
    overallSuccessRate,
    avgToken,
    p95Ms,
    toolTotalCalls,
    conflictRate: overview.value?.current?.conflictRate ?? 0,
    eventHitRate: overview.value?.current?.eventHitRate ?? 0,
    eventPrecision: overview.value?.current?.eventPrecision ?? 0,
    vectorSimilarity: overview.value?.current?.avgVectorSimilarity ?? 0,
    cacheHitRate: overview.value?.current?.cacheHitRate ?? 0,
    hallucinationRate: overview.value?.current?.hallucinationRate ?? 0,
  };
});

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    const [m, t, ts, ta, ov] = await Promise.all([
      getAgentMetrics(),
      getRecentTraces(30),
      getToolSummary(),
      getToolAudits(20),
      getMetricsOverview(30),
    ]);
    metrics.value = m.sort((a, b) => b.avgMs - a.avgMs);
    traces.value = t;
    toolSummary.value = ts;
    toolAudits.value = ta;
    overview.value = ov;
  } catch (e) {
    error.value = e instanceof Error ? e.message : "加载失败";
  } finally {
    loading.value = false;
  }
}

function pct(value: number | null | undefined) {
  if (value == null || Number.isNaN(value)) return "—";
  return (value * 100).toFixed(1) + "%";
}

function deltaPct(curr: number | null | undefined, prev: number | null | undefined) {
  if (curr == null || prev == null) return "—";
  const delta = (curr - prev) * 100;
  return `${delta >= 0 ? "+" : ""}${delta.toFixed(1)}%`;
}

function deltaMs(curr: number | null | undefined, prev: number | null | undefined) {
  if (curr == null || prev == null) return "—";
  const delta = curr - prev;
  return `${delta >= 0 ? "+" : ""}${delta.toFixed(0)}ms`;
}

function toggleAutoRefresh() {
  autoRefresh.value = !autoRefresh.value;
  if (autoRefresh.value) {
    autoRefreshTimer = setInterval(refresh, 30000);
  } else if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer);
    autoRefreshTimer = null;
  }
}

async function forceSummarize() {
  if (!diarySessionId.value.trim()) {
    diaryResult.value = "请填写 SessionId";
    return;
  }
  diaryLoading.value = true;
  diaryResult.value = "";
  try {
    const res = await http.post<ApiResponse<Record<string, unknown>>>("/admin/diary/force-summarize", {
      sessionId: diarySessionId.value.trim(),
      fromTurn: diaryFromTurn.value,
      toTurn: diaryToTurn.value,
    });
    const d = res.data.data as Record<string, unknown>;
    diaryResult.value = `成功 L${d.level}摘要 Turn ${d.fromTurn}~${d.toTurn}`;
  } catch (e) {
    diaryResult.value = e instanceof Error ? e.message : "操作失败";
  } finally {
    diaryLoading.value = false;
  }
}

function selectTrace(trace: TraceDetail) {
  selectedTrace.value = selectedTrace.value?.traceId === trace.traceId ? null : trace;
}

function formatTime(ts: number) {
  return new Date(ts).toLocaleTimeString("zh-CN", { hour12: false });
}

function statusColor(status: string) {
  if (status === "OK" || status === "success" || status === "SUCCESS") return "var(--ok)";
  if (status === "error" || status === "ABORTED" || status === "FAILED") return "var(--danger)";
  return "var(--text-03)";
}

function successRatePct(rate: number) {
  return (rate * 100).toFixed(1) + "%";
}

function toolSuccessRate(t: ToolSummary) {
  return t.totalCalls > 0 ? ((t.successCalls / t.totalCalls) * 100).toFixed(1) + "%" : "—";
}

onMounted(() => {
  refresh();
  autoRefreshTimer = setInterval(refresh, 30000);
});

onUnmounted(() => {
  if (autoRefreshTimer) clearInterval(autoRefreshTimer);
});
</script>

<template>
  <main class="page-wrap admin-page">
    <header class="panel topbar">
      <div>
        <p class="meta">Ruin Rain Admin</p>
        <h1>Agent 可观测性面板</h1>
      </div>
      <div class="topbar-actions">
        <button class="btn btn-ghost" @click="toggleAutoRefresh">
          {{ autoRefresh ? "自动刷新 ON" : "自动刷新 OFF" }}
        </button>
        <button class="btn" :disabled="loading" @click="refresh">
          {{ loading ? "加载中…" : "手动刷新" }}
        </button>
      </div>
    </header>

    <p v-if="error" class="notice">{{ error }}</p>

    <!-- KPI 汇总卡 -->
    <section class="kpi-row">
      <div class="kpi-card panel">
        <p class="kpi-label">总调用次数</p>
        <p class="kpi-value">{{ kpi.totalCalls }}</p>
      </div>
      <div class="kpi-card panel">
        <p class="kpi-label">整体成功率</p>
        <p class="kpi-value" :style="{ color: parseFloat(kpi.overallSuccessRate as string) >= 90 ? 'var(--ok)' : 'var(--danger)' }">
          {{ kpi.overallSuccessRate }}%
        </p>
      </div>
      <div class="kpi-card panel">
        <p class="kpi-label">P95 回合耗时</p>
        <p class="kpi-value" :style="{ color: (kpi.p95Ms ?? 0) > 15000 ? 'var(--danger)' : (kpi.p95Ms ?? 0) > 8000 ? '#f5a623' : 'var(--ok)' }">
          {{ overview?.current?.p95ElapsedMs != null ? overview.current.p95ElapsedMs + 'ms' : (kpi.p95Ms != null ? kpi.p95Ms + 'ms' : '—') }}
        </p>
        <p class="kpi-sub" v-if="overview">前窗 {{ overview.previous.p95ElapsedMs }}ms · Δ {{ deltaMs(overview.current.p95ElapsedMs, overview.previous.p95ElapsedMs) }}</p>
      </div>
      <div class="kpi-card panel">
        <p class="kpi-label">平均 Token/Agent</p>
        <p class="kpi-value">{{ kpi.avgToken }}</p>
      </div>
      <div class="kpi-card panel">
        <p class="kpi-label">Tool 调用总次数</p>
        <p class="kpi-value">{{ kpi.toolTotalCalls }}</p>
      </div>
      <div class="kpi-card panel">
        <p class="kpi-label">剧情冲突率</p>
        <p class="kpi-value" :style="{ color: kpi.conflictRate > 0.15 ? 'var(--danger)' : kpi.conflictRate > 0.08 ? '#f5a623' : 'var(--ok)' }">
          {{ pct(kpi.conflictRate) }}
        </p>
        <p class="kpi-sub" v-if="overview">前窗 {{ pct(overview.previous.conflictRate) }} · Δ {{ deltaPct(overview.current.conflictRate, overview.previous.conflictRate) }}</p>
      </div>
      <div class="kpi-card panel">
        <p class="kpi-label">事件命中率</p>
        <p class="kpi-value" :style="{ color: kpi.eventHitRate < 0.35 ? 'var(--danger)' : kpi.eventHitRate < 0.55 ? '#f5a623' : 'var(--ok)' }">
          {{ pct(kpi.eventHitRate) }}
        </p>
        <p class="kpi-sub" v-if="overview">前窗 {{ pct(overview.previous.eventHitRate) }} · Δ {{ deltaPct(overview.current.eventHitRate, overview.previous.eventHitRate) }}</p>
      </div>
      <div class="kpi-card panel">
        <p class="kpi-label">RAG 召回准确率</p>
        <p class="kpi-value" :style="{ color: kpi.eventPrecision < 0.55 ? 'var(--danger)' : kpi.eventPrecision < 0.75 ? '#f5a623' : 'var(--ok)' }">
          {{ pct(kpi.eventPrecision) }}
        </p>
        <p class="kpi-sub">口径：eventPrecision（事件命中 / 事件候选）</p>
      </div>
      <div class="kpi-card panel">
        <p class="kpi-label">RAG 召回率</p>
        <p class="kpi-value" :style="{ color: kpi.eventHitRate < 0.35 ? 'var(--danger)' : kpi.eventHitRate < 0.55 ? '#f5a623' : 'var(--ok)' }">
          {{ pct(kpi.eventHitRate) }}
        </p>
        <p class="kpi-sub">口径：eventHitRate（事件命中 / 样本）</p>
      </div>
      <div class="kpi-card panel">
        <p class="kpi-label">向量相似度均值</p>
        <p class="kpi-value" :style="{ color: kpi.vectorSimilarity < 0.55 ? 'var(--danger)' : kpi.vectorSimilarity < 0.7 ? '#f5a623' : 'var(--ok)' }">
          {{ kpi.vectorSimilarity.toFixed(3) }}
        </p>
        <p class="kpi-sub">由 RetrievalAgent 写入 trace extras</p>
      </div>
      <div class="kpi-card panel">
        <p class="kpi-label">Redis 缓存命中率</p>
        <p class="kpi-value" :style="{ color: kpi.cacheHitRate < 0.7 ? 'var(--danger)' : kpi.cacheHitRate < 0.85 ? '#f5a623' : 'var(--ok)' }">
          {{ pct(kpi.cacheHitRate) }}
        </p>
        <p class="kpi-sub">session / memory / idempotent 聚合命中率</p>
      </div>
      <div class="kpi-card panel">
        <p class="kpi-label">RAG 幻觉率</p>
        <p class="kpi-value" :style="{ color: kpi.hallucinationRate > 0.1 ? 'var(--danger)' : kpi.hallucinationRate > 0.05 ? '#f5a623' : 'var(--ok)' }">
          {{ pct(kpi.hallucinationRate) }}
        </p>
        <p class="kpi-sub">口径：conflictDetected 代理值</p>
      </div>
    </section>

    <!-- Agent 聚合指标 -->
    <section class="panel section-box">
      <h2 class="section-title">Agent 性能指标</h2>
      <div class="table-wrap">
        <table class="metrics-table">
          <thead>
            <tr>
              <th>Agent</th>
              <th>调用次数</th>
              <th>成功</th>
              <th>失败</th>
              <th>平均耗时(ms)</th>
              <th>平均模型(ms)</th>
              <th>平均排队(ms)</th>
              <th>平均Token</th>
              <th>成功率</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="metrics.length === 0">
              <td colspan="9" class="empty-cell">暂无数据（请先完成至少一次回合）</td>
            </tr>
            <tr v-for="m in metrics" :key="m.agentName">
              <td class="agent-name">{{ m.agentName }}</td>
              <td>{{ m.totalCalls }}</td>
              <td>{{ m.successCalls }}</td>
              <td :style="{ color: m.failCalls > 0 ? 'var(--danger)' : 'inherit' }">{{ m.failCalls }}</td>
              <td>
                <span :style="{ color: m.avgMs > 5000 ? 'var(--danger)' : m.avgMs > 2000 ? '#f5a623' : 'var(--ok)' }">
                  {{ m.avgMs.toFixed(0) }}
                </span>
              </td>
              <td class="mono">{{ m.avgModelMs.toFixed(0) }}</td>
              <td class="mono">{{ m.avgQueueWaitMs.toFixed(0) }}</td>
              <td class="mono">{{ m.avgTokens.toFixed(1) }}</td>
              <td>
                <span :style="{ color: statusColor(m.successRate >= 0.9 ? 'OK' : 'error') }">
                  {{ successRatePct(m.successRate) }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- Tool 调用监控 -->
    <section class="panel section-box">
      <h2 class="section-title">Tool 调用汇总</h2>
      <div class="table-wrap">
        <table class="metrics-table">
          <thead>
            <tr>
              <th>Tool</th>
              <th>调用次数</th>
              <th>成功</th>
              <th>失败</th>
              <th>平均耗时(ms)</th>
              <th>平均重试</th>
              <th>成功率</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="toolSummary.length === 0">
              <td colspan="7" class="empty-cell">暂无 Tool 调用数据</td>
            </tr>
            <tr v-for="t in toolSummary" :key="t.toolName">
              <td class="agent-name">{{ t.toolName }}</td>
              <td>{{ t.totalCalls }}</td>
              <td>{{ t.successCalls }}</td>
              <td :style="{ color: t.failedCalls > 0 ? 'var(--danger)' : 'inherit' }">{{ t.failedCalls }}</td>
              <td>
                <span :style="{ color: t.avgMs > 3000 ? 'var(--danger)' : t.avgMs > 1000 ? '#f5a623' : 'var(--ok)' }">
                  {{ t.avgMs.toFixed(0) }}
                </span>
              </td>
              <td class="mono">{{ t.avgRetry.toFixed(2) }}</td>
              <td>
                <span :style="{ color: statusColor(t.totalCalls > 0 && t.successCalls / t.totalCalls >= 0.9 ? 'OK' : 'error') }">
                  {{ toolSuccessRate(t) }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- Tool 审计明细 -->
    <section class="panel section-box">
      <h2 class="section-title">Tool 审计明细（最近 20 条）</h2>
      <div class="table-wrap">
        <table class="metrics-table">
          <thead>
            <tr>
              <th>时间</th>
              <th>Tool</th>
              <th>调用方 Agent</th>
              <th>状态</th>
              <th>耗时(ms)</th>
              <th>重试</th>
              <th>补偿</th>
              <th>SessionId</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="toolAudits.length === 0">
              <td colspan="8" class="empty-cell">暂无审计记录</td>
            </tr>
            <tr v-for="(a, i) in toolAudits" :key="i">
              <td class="mono">{{ a.createdAt ? a.createdAt.substring(11, 19) : '—' }}</td>
              <td class="agent-name">{{ a.toolName }}</td>
              <td class="mono">{{ a.callerAgent || '—' }}</td>
              <td>
                <span class="status-badge" :style="{ background: statusColor(a.status) + '22', color: statusColor(a.status) }">
                  {{ a.status }}
                </span>
              </td>
              <td class="mono">{{ a.latencyMs }}</td>
              <td class="mono">{{ a.retryCount }}</td>
              <td class="mono">{{ a.compensated ? '是' : '—' }}</td>
              <td class="mono" style="max-width:140px;overflow:hidden;text-overflow:ellipsis;">{{ (a.sessionId || '').substring(0, 18) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- 最近 Trace 列表 -->
    <section class="panel section-box">
      <h2 class="section-title">最近 Trace 链路</h2>
      <div class="table-wrap">
        <table class="metrics-table">
          <thead>
            <tr>
              <th>TraceId</th>
              <th>SessionId</th>
              <th>回合</th>
              <th>总耗时(ms)</th>
              <th>冲突/命中</th>
              <th>状态</th>
              <th>时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="traces.length === 0">
              <td colspan="7" class="empty-cell">暂无 Trace 数据</td>
            </tr>
            <template v-for="trace in traces" :key="trace.traceId">
              <tr
                class="trace-row"
                :class="{ selected: selectedTrace?.traceId === trace.traceId }"
                @click="selectTrace(trace)"
              >
                <td class="mono">{{ trace.traceId.substring(0, 18) }}…</td>
                <td class="mono">{{ trace.sessionId.substring(0, 20) }}…</td>
                <td>T{{ trace.turn }}</td>
                <td>
                  <span :style="{ color: trace.elapsedMs > 15000 ? 'var(--danger)' : trace.elapsedMs > 8000 ? '#f5a623' : 'var(--ok)' }">
                    {{ trace.elapsedMs }}
                  </span>
                </td>
                <td class="mono">
                  <span :style="{ color: trace.conflictDetected ? 'var(--danger)' : 'var(--ok)' }">{{ trace.conflictDetected ? '冲突' : '无冲突' }}</span>
                  /
                  <span :style="{ color: trace.eventHit ? 'var(--ok)' : 'var(--text-03)' }">{{ trace.eventHit ? '命中' : '未命中' }}</span>
                </td>
                <td>
                  <span class="status-badge" :style="{ background: statusColor(trace.finalStatus) + '22', color: statusColor(trace.finalStatus) }">
                    {{ trace.finalStatus }}
                  </span>
                </td>
                <td class="mono">{{ formatTime(trace.startedAt) }}</td>
              </tr>
              <!-- 展开 Span 明细 -->
              <tr v-if="selectedTrace?.traceId === trace.traceId" class="span-detail-row">
                <td colspan="7">
                  <div class="span-list">
                    <div
                      v-for="span in trace.spans"
                      :key="span.agentName + '-' + span.elapsedMs"
                      class="span-item"
                    >
                      <span class="span-name">{{ span.agentName }}</span>
                      <span
                        class="span-time"
                        :style="{ color: span.elapsedMs > 5000 ? 'var(--danger)' : span.elapsedMs > 2000 ? '#f5a623' : 'var(--ok)' }"
                      >
                        {{ span.elapsedMs }}ms
                      </span>
                      <span v-if="span.modelMs !== undefined" class="span-sub mono">模型 {{ span.modelMs }}ms</span>
                      <span v-if="span.queueWaitMs !== undefined" class="span-sub mono">排队 {{ span.queueWaitMs }}ms</span>
                      <span v-if="span.postProcessMs !== undefined" class="span-sub mono">后处理 {{ span.postProcessMs }}ms</span>
                      <span v-if="span.totalTokens !== undefined" class="span-sub mono">Token {{ span.totalTokens }}</span>
                      <span v-if="span.tokensPerSecond !== undefined" class="span-sub mono">TPS {{ span.tokensPerSecond.toFixed(1) }}</span>
                      <span v-if="span.modelName" class="span-sub mono">{{ span.modelName }}</span>
                      <span class="span-status" :style="{ color: statusColor(span.status) }">
                        {{ span.status }}
                      </span>
                      <span v-if="span.errorMessage" class="span-error">{{ span.errorMessage }}</span>
                    </div>
                    <div v-if="trace.spans.length === 0" class="empty-cell">暂无 Span 数据</div>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>
    </section>

    <!-- Diary 运维操作 -->
    <section class="panel section-box">
      <h2 class="section-title">Diary 运维：强制摘要</h2>
      <div class="diary-ops">
        <label class="ops-label">SessionId</label>
        <input v-model="diarySessionId" class="ops-input" placeholder="会话 UUID" />
        <label class="ops-label">From Turn</label>
        <input v-model.number="diaryFromTurn" class="ops-input ops-input-sm" type="number" min="1" />
        <label class="ops-label">To Turn</label>
        <input v-model.number="diaryToTurn" class="ops-input ops-input-sm" type="number" min="1" />
        <button class="btn" :disabled="diaryLoading" @click="forceSummarize">
          {{ diaryLoading ? "执行中…" : "执行摘要" }}
        </button>
        <span v-if="diaryResult" class="ops-result mono">{{ diaryResult }}</span>
      </div>
    </section>
  </main>
</template>

<style scoped>
.admin-page {
  display: grid;
  gap: 16px;
}

.topbar {
  padding: 14px 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.topbar-actions {
  display: flex;
  gap: 8px;
}

h1 {
  margin: 2px 0 0;
  font-size: 26px;
  font-family: var(--font-display);
  letter-spacing: 0.04em;
}

/* KPI 卡 */
.kpi-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.kpi-card {
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.kpi-label {
  margin: 0;
  font-size: 11px;
  font-family: var(--font-mono);
  color: var(--text-03);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.kpi-value {
  margin: 0;
  font-size: 24px;
  font-family: var(--font-mono);
  font-weight: 700;
  color: var(--text-01);
}

.kpi-sub {
  margin: 0;
  font-size: 11px;
  font-family: var(--font-mono);
  color: var(--text-03);
}

h2.section-title {
  margin: 0 0 14px;
  font-size: 14px;
  font-family: var(--font-mono);
  color: var(--text-03);
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.meta {
  margin: 0;
  color: var(--text-03);
  font-family: var(--font-mono);
  font-size: 12px;
}

.section-box {
  padding: 18px 20px;
}

.table-wrap {
  overflow-x: auto;
}

.metrics-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.metrics-table th {
  text-align: left;
  padding: 8px 12px;
  color: var(--text-03);
  font-family: var(--font-mono);
  font-size: 11px;
  border-bottom: 1px solid var(--line-soft);
  white-space: nowrap;
}

.metrics-table td {
  padding: 9px 12px;
  border-bottom: 1px solid rgba(180, 197, 217, 0.07);
  color: var(--text-02);
  vertical-align: middle;
}

.metrics-table tbody tr:hover td {
  background: rgba(255, 255, 255, 0.03);
}

.agent-name {
  font-family: var(--font-mono);
  color: var(--text-01) !important;
  font-size: 12px;
}

.mono {
  font-family: var(--font-mono);
  font-size: 11px;
}

.empty-cell {
  text-align: center;
  color: var(--text-03);
  padding: 24px !important;
}

.trace-row {
  cursor: pointer;
  transition: background var(--transition-fast);
}

.trace-row.selected td {
  background: rgba(255, 106, 61, 0.06);
}

.status-badge {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-family: var(--font-mono);
  font-weight: 600;
}

.span-detail-row td {
  background: rgba(0, 0, 0, 0.25);
  padding: 12px 20px !important;
}

.span-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.span-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.03);
  font-size: 12px;
}

.span-name {
  font-family: var(--font-mono);
  color: var(--text-01);
  font-size: 11px;
}

.span-time {
  font-family: var(--font-mono);
  font-weight: 600;
}

.span-status {
  font-size: 10px;
  font-family: var(--font-mono);
}

.span-sub {
  font-size: 10px;
  color: var(--text-03);
}

.span-error {
  color: var(--danger);
  font-size: 10px;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Diary 操作 */
.diary-ops {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.ops-label {
  font-size: 12px;
  font-family: var(--font-mono);
  color: var(--text-03);
}

.ops-input {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-sm);
  color: var(--text-01);
  font-family: var(--font-mono);
  font-size: 12px;
  padding: 6px 10px;
  width: 280px;
}

.ops-input-sm {
  width: 70px;
}

.ops-result {
  font-size: 12px;
  color: var(--ok);
  padding: 4px 8px;
  border: 1px solid var(--ok);
  border-radius: var(--radius-sm);
}

.btn-ghost {
  background: transparent;
  border: 1px solid var(--line-soft);
  color: var(--text-02);
}

.notice {
  margin: 0;
  font-size: 13px;
  color: var(--danger);
}
</style>
