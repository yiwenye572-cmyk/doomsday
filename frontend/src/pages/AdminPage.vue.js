import { computed, onMounted, onUnmounted, ref } from "vue";
import { getAgentMetrics, getRecentTraces, getToolSummary, getToolAudits } from "../api/admin";
import http from "../api/http";
const metrics = ref([]);
const traces = ref([]);
const toolSummary = ref([]);
const toolAudits = ref([]);
const selectedTrace = ref(null);
const loading = ref(false);
const error = ref("");
// Diary 操作
const diarySessionId = ref("");
const diaryFromTurn = ref(1);
const diaryToTurn = ref(10);
const diaryResult = ref("");
const diaryLoading = ref(false);
// 自动刷新
let autoRefreshTimer = null;
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
    return { totalCalls, overallSuccessRate, avgToken, p95Ms, toolTotalCalls };
});
async function refresh() {
    loading.value = true;
    error.value = "";
    try {
        const [m, t, ts, ta] = await Promise.all([
            getAgentMetrics(),
            getRecentTraces(30),
            getToolSummary(),
            getToolAudits(20),
        ]);
        metrics.value = m.sort((a, b) => b.avgMs - a.avgMs);
        traces.value = t;
        toolSummary.value = ts;
        toolAudits.value = ta;
    }
    catch (e) {
        error.value = e instanceof Error ? e.message : "加载失败";
    }
    finally {
        loading.value = false;
    }
}
function toggleAutoRefresh() {
    autoRefresh.value = !autoRefresh.value;
    if (autoRefresh.value) {
        autoRefreshTimer = setInterval(refresh, 30000);
    }
    else if (autoRefreshTimer) {
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
        const res = await http.post("/admin/diary/force-summarize", {
            sessionId: diarySessionId.value.trim(),
            fromTurn: diaryFromTurn.value,
            toTurn: diaryToTurn.value,
        });
        const d = res.data.data;
        diaryResult.value = `成功 L${d.level}摘要 Turn ${d.fromTurn}~${d.toTurn}`;
    }
    catch (e) {
        diaryResult.value = e instanceof Error ? e.message : "操作失败";
    }
    finally {
        diaryLoading.value = false;
    }
}
function selectTrace(trace) {
    selectedTrace.value = selectedTrace.value?.traceId === trace.traceId ? null : trace;
}
function formatTime(ts) {
    return new Date(ts).toLocaleTimeString("zh-CN", { hour12: false });
}
function statusColor(status) {
    if (status === "OK" || status === "success" || status === "SUCCESS")
        return "var(--ok)";
    if (status === "error" || status === "ABORTED" || status === "FAILED")
        return "var(--danger)";
    return "var(--text-03)";
}
function successRatePct(rate) {
    return (rate * 100).toFixed(1) + "%";
}
function toolSuccessRate(t) {
    return t.totalCalls > 0 ? ((t.successCalls / t.totalCalls) * 100).toFixed(1) + "%" : "—";
}
onMounted(() => {
    refresh();
    autoRefreshTimer = setInterval(refresh, 30000);
});
onUnmounted(() => {
    if (autoRefreshTimer)
        clearInterval(autoRefreshTimer);
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['metrics-table']} */ ;
/** @type {__VLS_StyleScopedClasses['metrics-table']} */ ;
/** @type {__VLS_StyleScopedClasses['metrics-table']} */ ;
/** @type {__VLS_StyleScopedClasses['trace-row']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.main, __VLS_intrinsicElements.main)({
    ...{ class: "page-wrap admin-page" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.header, __VLS_intrinsicElements.header)({
    ...{ class: "panel topbar" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "meta" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "topbar-actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.toggleAutoRefresh) },
    ...{ class: "btn btn-ghost" },
});
(__VLS_ctx.autoRefresh ? "自动刷新 ON" : "自动刷新 OFF");
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.refresh) },
    ...{ class: "btn" },
    disabled: (__VLS_ctx.loading),
});
(__VLS_ctx.loading ? "加载中…" : "手动刷新");
if (__VLS_ctx.error) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "notice" },
    });
    (__VLS_ctx.error);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "kpi-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "kpi-card panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "kpi-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "kpi-value" },
});
(__VLS_ctx.kpi.totalCalls);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "kpi-card panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "kpi-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "kpi-value" },
    ...{ style: ({ color: parseFloat(__VLS_ctx.kpi.overallSuccessRate) >= 90 ? 'var(--ok)' : 'var(--danger)' }) },
});
(__VLS_ctx.kpi.overallSuccessRate);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "kpi-card panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "kpi-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "kpi-value" },
    ...{ style: ({ color: (__VLS_ctx.kpi.p95Ms ?? 0) > 15000 ? 'var(--danger)' : (__VLS_ctx.kpi.p95Ms ?? 0) > 8000 ? '#f5a623' : 'var(--ok)' }) },
});
(__VLS_ctx.kpi.p95Ms != null ? __VLS_ctx.kpi.p95Ms + 'ms' : '—');
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "kpi-card panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "kpi-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "kpi-value" },
});
(__VLS_ctx.kpi.avgToken);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "kpi-card panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "kpi-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "kpi-value" },
});
(__VLS_ctx.kpi.toolTotalCalls);
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel section-box" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({
    ...{ class: "section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-wrap" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.table, __VLS_intrinsicElements.table)({
    ...{ class: "metrics-table" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.thead, __VLS_intrinsicElements.thead)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.tbody, __VLS_intrinsicElements.tbody)({});
if (__VLS_ctx.metrics.length === 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        colspan: "9",
        ...{ class: "empty-cell" },
    });
}
for (const [m] of __VLS_getVForSourceType((__VLS_ctx.metrics))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({
        key: (m.agentName),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        ...{ class: "agent-name" },
    });
    (m.agentName);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    (m.totalCalls);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    (m.successCalls);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        ...{ style: ({ color: m.failCalls > 0 ? 'var(--danger)' : 'inherit' }) },
    });
    (m.failCalls);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ style: ({ color: m.avgMs > 5000 ? 'var(--danger)' : m.avgMs > 2000 ? '#f5a623' : 'var(--ok)' }) },
    });
    (m.avgMs.toFixed(0));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        ...{ class: "mono" },
    });
    (m.avgModelMs.toFixed(0));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        ...{ class: "mono" },
    });
    (m.avgQueueWaitMs.toFixed(0));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        ...{ class: "mono" },
    });
    (m.avgTokens.toFixed(1));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ style: ({ color: __VLS_ctx.statusColor(m.successRate >= 0.9 ? 'OK' : 'error') }) },
    });
    (__VLS_ctx.successRatePct(m.successRate));
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel section-box" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({
    ...{ class: "section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-wrap" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.table, __VLS_intrinsicElements.table)({
    ...{ class: "metrics-table" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.thead, __VLS_intrinsicElements.thead)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.tbody, __VLS_intrinsicElements.tbody)({});
if (__VLS_ctx.toolSummary.length === 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        colspan: "7",
        ...{ class: "empty-cell" },
    });
}
for (const [t] of __VLS_getVForSourceType((__VLS_ctx.toolSummary))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({
        key: (t.toolName),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        ...{ class: "agent-name" },
    });
    (t.toolName);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    (t.totalCalls);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    (t.successCalls);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        ...{ style: ({ color: t.failedCalls > 0 ? 'var(--danger)' : 'inherit' }) },
    });
    (t.failedCalls);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ style: ({ color: t.avgMs > 3000 ? 'var(--danger)' : t.avgMs > 1000 ? '#f5a623' : 'var(--ok)' }) },
    });
    (t.avgMs.toFixed(0));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        ...{ class: "mono" },
    });
    (t.avgRetry.toFixed(2));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ style: ({ color: __VLS_ctx.statusColor(t.totalCalls > 0 && t.successCalls / t.totalCalls >= 0.9 ? 'OK' : 'error') }) },
    });
    (__VLS_ctx.toolSuccessRate(t));
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel section-box" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({
    ...{ class: "section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-wrap" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.table, __VLS_intrinsicElements.table)({
    ...{ class: "metrics-table" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.thead, __VLS_intrinsicElements.thead)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.tbody, __VLS_intrinsicElements.tbody)({});
if (__VLS_ctx.toolAudits.length === 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        colspan: "8",
        ...{ class: "empty-cell" },
    });
}
for (const [a, i] of __VLS_getVForSourceType((__VLS_ctx.toolAudits))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({
        key: (i),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        ...{ class: "mono" },
    });
    (a.createdAt ? a.createdAt.substring(11, 19) : '—');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        ...{ class: "agent-name" },
    });
    (a.toolName);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        ...{ class: "mono" },
    });
    (a.callerAgent || '—');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "status-badge" },
        ...{ style: ({ background: __VLS_ctx.statusColor(a.status) + '22', color: __VLS_ctx.statusColor(a.status) }) },
    });
    (a.status);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        ...{ class: "mono" },
    });
    (a.latencyMs);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        ...{ class: "mono" },
    });
    (a.retryCount);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        ...{ class: "mono" },
    });
    (a.compensated ? '是' : '—');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        ...{ class: "mono" },
        ...{ style: {} },
    });
    ((a.sessionId || '').substring(0, 18));
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel section-box" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({
    ...{ class: "section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-wrap" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.table, __VLS_intrinsicElements.table)({
    ...{ class: "metrics-table" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.thead, __VLS_intrinsicElements.thead)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.tbody, __VLS_intrinsicElements.tbody)({});
if (__VLS_ctx.traces.length === 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        colspan: "6",
        ...{ class: "empty-cell" },
    });
}
for (const [trace] of __VLS_getVForSourceType((__VLS_ctx.traces))) {
    (trace.traceId);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.selectTrace(trace);
            } },
        ...{ class: "trace-row" },
        ...{ class: ({ selected: __VLS_ctx.selectedTrace?.traceId === trace.traceId }) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        ...{ class: "mono" },
    });
    (trace.traceId.substring(0, 18));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        ...{ class: "mono" },
    });
    (trace.sessionId.substring(0, 20));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    (trace.turn);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ style: ({ color: trace.elapsedMs > 15000 ? 'var(--danger)' : trace.elapsedMs > 8000 ? '#f5a623' : 'var(--ok)' }) },
    });
    (trace.elapsedMs);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "status-badge" },
        ...{ style: ({ background: __VLS_ctx.statusColor(trace.finalStatus) + '22', color: __VLS_ctx.statusColor(trace.finalStatus) }) },
    });
    (trace.finalStatus);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        ...{ class: "mono" },
    });
    (__VLS_ctx.formatTime(trace.startedAt));
    if (__VLS_ctx.selectedTrace?.traceId === trace.traceId) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({
            ...{ class: "span-detail-row" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            colspan: "6",
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "span-list" },
        });
        for (const [span] of __VLS_getVForSourceType((trace.spans))) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                key: (span.agentName + '-' + span.elapsedMs),
                ...{ class: "span-item" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "span-name" },
            });
            (span.agentName);
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "span-time" },
                ...{ style: ({ color: span.elapsedMs > 5000 ? 'var(--danger)' : span.elapsedMs > 2000 ? '#f5a623' : 'var(--ok)' }) },
            });
            (span.elapsedMs);
            if (span.modelMs !== undefined) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "span-sub mono" },
                });
                (span.modelMs);
            }
            if (span.queueWaitMs !== undefined) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "span-sub mono" },
                });
                (span.queueWaitMs);
            }
            if (span.postProcessMs !== undefined) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "span-sub mono" },
                });
                (span.postProcessMs);
            }
            if (span.totalTokens !== undefined) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "span-sub mono" },
                });
                (span.totalTokens);
            }
            if (span.tokensPerSecond !== undefined) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "span-sub mono" },
                });
                (span.tokensPerSecond.toFixed(1));
            }
            if (span.modelName) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "span-sub mono" },
                });
                (span.modelName);
            }
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "span-status" },
                ...{ style: ({ color: __VLS_ctx.statusColor(span.status) }) },
            });
            (span.status);
            if (span.errorMessage) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "span-error" },
                });
                (span.errorMessage);
            }
        }
        if (trace.spans.length === 0) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "empty-cell" },
            });
        }
    }
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel section-box" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({
    ...{ class: "section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "diary-ops" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "ops-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ class: "ops-input" },
    placeholder: "会话 UUID",
});
(__VLS_ctx.diarySessionId);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "ops-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ class: "ops-input ops-input-sm" },
    type: "number",
    min: "1",
});
(__VLS_ctx.diaryFromTurn);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "ops-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ class: "ops-input ops-input-sm" },
    type: "number",
    min: "1",
});
(__VLS_ctx.diaryToTurn);
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.forceSummarize) },
    ...{ class: "btn" },
    disabled: (__VLS_ctx.diaryLoading),
});
(__VLS_ctx.diaryLoading ? "执行中…" : "执行摘要");
if (__VLS_ctx.diaryResult) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "ops-result mono" },
    });
    (__VLS_ctx.diaryResult);
}
/** @type {__VLS_StyleScopedClasses['page-wrap']} */ ;
/** @type {__VLS_StyleScopedClasses['admin-page']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['topbar']} */ ;
/** @type {__VLS_StyleScopedClasses['meta']} */ ;
/** @type {__VLS_StyleScopedClasses['topbar-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['notice']} */ ;
/** @type {__VLS_StyleScopedClasses['kpi-row']} */ ;
/** @type {__VLS_StyleScopedClasses['kpi-card']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['kpi-label']} */ ;
/** @type {__VLS_StyleScopedClasses['kpi-value']} */ ;
/** @type {__VLS_StyleScopedClasses['kpi-card']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['kpi-label']} */ ;
/** @type {__VLS_StyleScopedClasses['kpi-value']} */ ;
/** @type {__VLS_StyleScopedClasses['kpi-card']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['kpi-label']} */ ;
/** @type {__VLS_StyleScopedClasses['kpi-value']} */ ;
/** @type {__VLS_StyleScopedClasses['kpi-card']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['kpi-label']} */ ;
/** @type {__VLS_StyleScopedClasses['kpi-value']} */ ;
/** @type {__VLS_StyleScopedClasses['kpi-card']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['kpi-label']} */ ;
/** @type {__VLS_StyleScopedClasses['kpi-value']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['section-box']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['table-wrap']} */ ;
/** @type {__VLS_StyleScopedClasses['metrics-table']} */ ;
/** @type {__VLS_StyleScopedClasses['empty-cell']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-name']} */ ;
/** @type {__VLS_StyleScopedClasses['mono']} */ ;
/** @type {__VLS_StyleScopedClasses['mono']} */ ;
/** @type {__VLS_StyleScopedClasses['mono']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['section-box']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['table-wrap']} */ ;
/** @type {__VLS_StyleScopedClasses['metrics-table']} */ ;
/** @type {__VLS_StyleScopedClasses['empty-cell']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-name']} */ ;
/** @type {__VLS_StyleScopedClasses['mono']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['section-box']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['table-wrap']} */ ;
/** @type {__VLS_StyleScopedClasses['metrics-table']} */ ;
/** @type {__VLS_StyleScopedClasses['empty-cell']} */ ;
/** @type {__VLS_StyleScopedClasses['mono']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-name']} */ ;
/** @type {__VLS_StyleScopedClasses['mono']} */ ;
/** @type {__VLS_StyleScopedClasses['status-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['mono']} */ ;
/** @type {__VLS_StyleScopedClasses['mono']} */ ;
/** @type {__VLS_StyleScopedClasses['mono']} */ ;
/** @type {__VLS_StyleScopedClasses['mono']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['section-box']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['table-wrap']} */ ;
/** @type {__VLS_StyleScopedClasses['metrics-table']} */ ;
/** @type {__VLS_StyleScopedClasses['empty-cell']} */ ;
/** @type {__VLS_StyleScopedClasses['trace-row']} */ ;
/** @type {__VLS_StyleScopedClasses['mono']} */ ;
/** @type {__VLS_StyleScopedClasses['mono']} */ ;
/** @type {__VLS_StyleScopedClasses['status-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['mono']} */ ;
/** @type {__VLS_StyleScopedClasses['span-detail-row']} */ ;
/** @type {__VLS_StyleScopedClasses['span-list']} */ ;
/** @type {__VLS_StyleScopedClasses['span-item']} */ ;
/** @type {__VLS_StyleScopedClasses['span-name']} */ ;
/** @type {__VLS_StyleScopedClasses['span-time']} */ ;
/** @type {__VLS_StyleScopedClasses['span-sub']} */ ;
/** @type {__VLS_StyleScopedClasses['mono']} */ ;
/** @type {__VLS_StyleScopedClasses['span-sub']} */ ;
/** @type {__VLS_StyleScopedClasses['mono']} */ ;
/** @type {__VLS_StyleScopedClasses['span-sub']} */ ;
/** @type {__VLS_StyleScopedClasses['mono']} */ ;
/** @type {__VLS_StyleScopedClasses['span-sub']} */ ;
/** @type {__VLS_StyleScopedClasses['mono']} */ ;
/** @type {__VLS_StyleScopedClasses['span-sub']} */ ;
/** @type {__VLS_StyleScopedClasses['mono']} */ ;
/** @type {__VLS_StyleScopedClasses['span-sub']} */ ;
/** @type {__VLS_StyleScopedClasses['mono']} */ ;
/** @type {__VLS_StyleScopedClasses['span-status']} */ ;
/** @type {__VLS_StyleScopedClasses['span-error']} */ ;
/** @type {__VLS_StyleScopedClasses['empty-cell']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['section-box']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['diary-ops']} */ ;
/** @type {__VLS_StyleScopedClasses['ops-label']} */ ;
/** @type {__VLS_StyleScopedClasses['ops-input']} */ ;
/** @type {__VLS_StyleScopedClasses['ops-label']} */ ;
/** @type {__VLS_StyleScopedClasses['ops-input']} */ ;
/** @type {__VLS_StyleScopedClasses['ops-input-sm']} */ ;
/** @type {__VLS_StyleScopedClasses['ops-label']} */ ;
/** @type {__VLS_StyleScopedClasses['ops-input']} */ ;
/** @type {__VLS_StyleScopedClasses['ops-input-sm']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ops-result']} */ ;
/** @type {__VLS_StyleScopedClasses['mono']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            metrics: metrics,
            traces: traces,
            toolSummary: toolSummary,
            toolAudits: toolAudits,
            selectedTrace: selectedTrace,
            loading: loading,
            error: error,
            diarySessionId: diarySessionId,
            diaryFromTurn: diaryFromTurn,
            diaryToTurn: diaryToTurn,
            diaryResult: diaryResult,
            diaryLoading: diaryLoading,
            autoRefresh: autoRefresh,
            kpi: kpi,
            refresh: refresh,
            toggleAutoRefresh: toggleAutoRefresh,
            forceSummarize: forceSummarize,
            selectTrace: selectTrace,
            formatTime: formatTime,
            statusColor: statusColor,
            successRatePct: successRatePct,
            toolSuccessRate: toolSuccessRate,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
//# sourceMappingURL=AdminPage.vue.js.map