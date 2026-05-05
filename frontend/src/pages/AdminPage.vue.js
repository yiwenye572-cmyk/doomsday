import { onMounted, ref } from "vue";
import { getAgentMetrics, getRecentTraces } from "../api/admin";
const metrics = ref([]);
const traces = ref([]);
const selectedTrace = ref(null);
const loading = ref(false);
const error = ref("");
async function refresh() {
    loading.value = true;
    error.value = "";
    try {
        const [m, t] = await Promise.all([getAgentMetrics(), getRecentTraces(30)]);
        metrics.value = m.sort((a, b) => b.avgMs - a.avgMs);
        traces.value = t;
    }
    catch (e) {
        error.value = e instanceof Error ? e.message : "加载失败";
    }
    finally {
        loading.value = false;
    }
}
function selectTrace(trace) {
    selectedTrace.value = selectedTrace.value?.traceId === trace.traceId ? null : trace;
}
function formatTime(ts) {
    return new Date(ts).toLocaleTimeString("zh-CN", { hour12: false });
}
function statusColor(status) {
    if (status === "OK" || status === "success")
        return "var(--ok)";
    if (status === "error" || status === "ABORTED")
        return "var(--danger)";
    return "var(--text-03)";
}
function successRatePct(rate) {
    return (rate * 100).toFixed(1) + "%";
}
onMounted(refresh);
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
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.refresh) },
    ...{ class: "btn" },
    disabled: (__VLS_ctx.loading),
});
(__VLS_ctx.loading ? "加载中…" : "刷新");
if (__VLS_ctx.error) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "notice" },
    });
    (__VLS_ctx.error);
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
/** @type {__VLS_StyleScopedClasses['page-wrap']} */ ;
/** @type {__VLS_StyleScopedClasses['admin-page']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['topbar']} */ ;
/** @type {__VLS_StyleScopedClasses['meta']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['notice']} */ ;
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
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            metrics: metrics,
            traces: traces,
            selectedTrace: selectedTrace,
            loading: loading,
            error: error,
            refresh: refresh,
            selectTrace: selectTrace,
            formatTime: formatTime,
            statusColor: statusColor,
            successRatePct: successRatePct,
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