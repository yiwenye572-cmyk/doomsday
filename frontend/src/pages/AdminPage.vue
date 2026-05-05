<script setup lang="ts">
import { onMounted, ref } from "vue";
import { getAgentMetrics, getRecentTraces } from "../api/admin";
import type { AgentMetricsSummary, TraceDetail } from "../api/admin";

const metrics = ref<AgentMetricsSummary[]>([]);
const traces = ref<TraceDetail[]>([]);
const selectedTrace = ref<TraceDetail | null>(null);
const loading = ref(false);
const error = ref("");

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    const [m, t] = await Promise.all([getAgentMetrics(), getRecentTraces(30)]);
    metrics.value = m.sort((a, b) => b.avgMs - a.avgMs);
    traces.value = t;
  } catch (e) {
    error.value = e instanceof Error ? e.message : "加载失败";
  } finally {
    loading.value = false;
  }
}

function selectTrace(trace: TraceDetail) {
  selectedTrace.value = selectedTrace.value?.traceId === trace.traceId ? null : trace;
}

function formatTime(ts: number) {
  return new Date(ts).toLocaleTimeString("zh-CN", { hour12: false });
}

function statusColor(status: string) {
  if (status === "OK" || status === "success") return "var(--ok)";
  if (status === "error" || status === "ABORTED") return "var(--danger)";
  return "var(--text-03)";
}

function successRatePct(rate: number) {
  return (rate * 100).toFixed(1) + "%";
}

onMounted(refresh);
</script>

<template>
  <main class="page-wrap admin-page">
    <header class="panel topbar">
      <div>
        <p class="meta">Ruin Rain Admin</p>
        <h1>Agent 可观测性面板</h1>
      </div>
      <button class="btn" :disabled="loading" @click="refresh">
        {{ loading ? "加载中…" : "刷新" }}
      </button>
    </header>

    <p v-if="error" class="notice">{{ error }}</p>

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
              <th>成功率</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="metrics.length === 0">
              <td colspan="6" class="empty-cell">暂无数据（请先完成至少一次回合）</td>
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
              <th>状态</th>
              <th>时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="traces.length === 0">
              <td colspan="6" class="empty-cell">暂无 Trace 数据</td>
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
                <td>
                  <span class="status-badge" :style="{ background: statusColor(trace.finalStatus) + '22', color: statusColor(trace.finalStatus) }">
                    {{ trace.finalStatus }}
                  </span>
                </td>
                <td class="mono">{{ formatTime(trace.startedAt) }}</td>
              </tr>
              <!-- 展开 Span 明细 -->
              <tr v-if="selectedTrace?.traceId === trace.traceId" class="span-detail-row">
                <td colspan="6">
                  <div class="span-list">
                    <div
                      v-for="span in trace.spans"
                      :key="span.agentName"
                      class="span-item"
                    >
                      <span class="span-name">{{ span.agentName }}</span>
                      <span
                        class="span-time"
                        :style="{ color: span.elapsedMs > 5000 ? 'var(--danger)' : span.elapsedMs > 2000 ? '#f5a623' : 'var(--ok)' }"
                      >
                        {{ span.elapsedMs }}ms
                      </span>
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

h1 {
  margin: 2px 0 0;
  font-size: 26px;
  font-family: var(--font-display);
  letter-spacing: 0.04em;
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

.span-error {
  color: var(--danger);
  font-size: 10px;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice {
  margin: 0;
  font-size: 13px;
  color: var(--danger);
}
</style>
