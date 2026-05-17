import { ref, computed, watch, nextTick } from "vue";
import { getRecommendation, acceptRecommendation, rejectRecommendation, } from "../../api/game";
const props = defineProps();
const emit = defineEmits();
// ── state ──────────────────────────────────────────────────────────────────
const visible = ref(false);
const loading = ref(false);
const accepting = ref(false);
const errorMsg = ref(null);
const recommendation = ref(null);
const previewCanvas = ref(null);
// 迷你预览画布尺寸（1:1 缩小到 200×150）
const previewW = 200;
const previewH = 150;
const SCALE_X = previewW / 640; // 原始画布假设 640×480
const SCALE_Y = previewH / 480;
const TYPE_COLORS = {
    bed: "#6a8fd8",
    window: "#82d1d1",
    table: "#c9a96e",
    medkit: "#e06c75",
    axe: "#888",
    weapon: "#888",
    supply: "#98c379",
    food: "#98c379",
    default: "#7d7d7d",
};
const confClass = computed(() => {
    const c = recommendation.value?.confidence ?? 0;
    if (c >= 0.7)
        return "conf-high";
    if (c >= 0.4)
        return "conf-mid";
    return "conf-low";
});
// 每次推荐更新时重绘预览
watch(recommendation, () => nextTick(drawPreview));
// ── methods ─────────────────────────────────────────────────────────────────
async function fetch() {
    if (!props.sessionId)
        return;
    loading.value = true;
    errorMsg.value = null;
    try {
        recommendation.value = await getRecommendation(props.sessionId);
        visible.value = true;
    }
    catch (e) {
        errorMsg.value = e?.message ?? "获取推荐失败";
    }
    finally {
        loading.value = false;
    }
}
async function accept() {
    if (!recommendation.value || !props.sessionId)
        return;
    accepting.value = true;
    errorMsg.value = null;
    try {
        const result = await acceptRecommendation(props.sessionId, recommendation.value.recommendationId, props.currentVersion);
        if (result.conflict) {
            errorMsg.value = "版本冲突，请刷新后重试（" + (result.conflictMessage ?? "") + "）";
        }
        else {
            emit("applied", result.newVersion, result.stateData);
            close();
        }
    }
    catch (e) {
        errorMsg.value = e?.message ?? "应用推荐失败";
    }
    finally {
        accepting.value = false;
    }
}
async function reject() {
    if (!recommendation.value || !props.sessionId)
        return;
    try {
        await rejectRecommendation(props.sessionId, recommendation.value.recommendationId);
    }
    finally {
        close();
    }
}
function close() {
    visible.value = false;
    recommendation.value = null;
    errorMsg.value = null;
}
// ── Preview Canvas ──────────────────────────────────────────────────────────
function drawPreview() {
    const canvas = previewCanvas.value;
    if (!canvas || !recommendation.value)
        return;
    const ctx = canvas.getContext("2d");
    if (!ctx)
        return;
    ctx.clearRect(0, 0, previewW, previewH);
    // 背景
    ctx.fillStyle = "#111";
    ctx.fillRect(0, 0, previewW, previewH);
    // 网格线
    ctx.strokeStyle = "#222";
    ctx.lineWidth = 0.5;
    const gs = 32 * SCALE_X;
    for (let x = 0; x < previewW; x += gs) {
        ctx.beginPath();
        ctx.moveTo(x, 0);
        ctx.lineTo(x, previewH);
        ctx.stroke();
    }
    for (let y = 0; y < previewH; y += gs * (SCALE_Y / SCALE_X)) {
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(previewW, y);
        ctx.stroke();
    }
    // 物品方块
    for (const item of recommendation.value.items) {
        const rx = item.x * SCALE_X;
        const ry = item.y * SCALE_Y;
        const rw = Math.max(item.w * SCALE_X, 10);
        const rh = Math.max(item.h * SCALE_Y, 8);
        const color = TYPE_COLORS[item.type?.toLowerCase()] ?? TYPE_COLORS.default;
        ctx.fillStyle = color;
        ctx.globalAlpha = 0.85;
        ctx.fillRect(rx, ry, rw, rh);
        ctx.globalAlpha = 1.0;
        // 物品类型文字
        ctx.fillStyle = "#fff";
        ctx.font = `${Math.max(7, 9 * SCALE_X * 3)}px monospace`;
        ctx.fillText(item.type?.slice(0, 4) ?? "?", rx + 2, ry + rh - 2);
    }
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['btn-trigger']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-trigger']} */ ;
/** @type {__VLS_StyleScopedClasses['item-list']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-accept']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-accept']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-reject']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "rec-panel" },
});
if (!__VLS_ctx.visible) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.fetch) },
        ...{ class: "btn-trigger" },
        disabled: (__VLS_ctx.loading),
    });
    (__VLS_ctx.loading ? '分析中…' : '✨ 获取布局推荐');
}
const __VLS_0 = {}.transition;
/** @type {[typeof __VLS_components.Transition, typeof __VLS_components.transition, typeof __VLS_components.Transition, typeof __VLS_components.transition, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    name: "slide",
}));
const __VLS_2 = __VLS_1({
    name: "slide",
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_3.slots.default;
if (__VLS_ctx.visible && __VLS_ctx.recommendation) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "panel-body" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "panel-header" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "panel-title" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "conf-badge" },
        ...{ class: (__VLS_ctx.confClass) },
    });
    (Math.round(__VLS_ctx.recommendation.confidence * 100));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "reason" },
    });
    (__VLS_ctx.recommendation.reason);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "preview-wrap" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.canvas)({
        ref: "previewCanvas",
        width: (__VLS_ctx.previewW),
        height: (__VLS_ctx.previewH),
    });
    /** @type {typeof __VLS_ctx.previewCanvas} */ ;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.ul, __VLS_intrinsicElements.ul)({
        ...{ class: "item-list" },
    });
    for (const [item] of __VLS_getVForSourceType((__VLS_ctx.recommendation.items))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.li, __VLS_intrinsicElements.li)({
            key: (item.id),
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "item-type" },
        });
        (item.type);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "item-pos" },
        });
        (item.x);
        (item.y);
    }
    if (__VLS_ctx.errorMsg) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "error" },
        });
        (__VLS_ctx.errorMsg);
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "btn-row" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.accept) },
        ...{ class: "btn-accept" },
        disabled: (__VLS_ctx.accepting),
    });
    (__VLS_ctx.accepting ? '应用中…' : '✅ 接受');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.reject) },
        ...{ class: "btn-reject" },
    });
}
var __VLS_3;
/** @type {__VLS_StyleScopedClasses['rec-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-trigger']} */ ;
/** @type {__VLS_StyleScopedClasses['panel-body']} */ ;
/** @type {__VLS_StyleScopedClasses['panel-header']} */ ;
/** @type {__VLS_StyleScopedClasses['panel-title']} */ ;
/** @type {__VLS_StyleScopedClasses['conf-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['reason']} */ ;
/** @type {__VLS_StyleScopedClasses['preview-wrap']} */ ;
/** @type {__VLS_StyleScopedClasses['item-list']} */ ;
/** @type {__VLS_StyleScopedClasses['item-type']} */ ;
/** @type {__VLS_StyleScopedClasses['item-pos']} */ ;
/** @type {__VLS_StyleScopedClasses['error']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-row']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-accept']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-reject']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            visible: visible,
            loading: loading,
            accepting: accepting,
            errorMsg: errorMsg,
            recommendation: recommendation,
            previewCanvas: previewCanvas,
            previewW: previewW,
            previewH: previewH,
            confClass: confClass,
            fetch: fetch,
            accept: accept,
            reject: reject,
        };
    },
    __typeEmits: {},
    __typeProps: {},
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
    __typeEmits: {},
    __typeProps: {},
});
; /* PartiallyEnd: #4569/main.vue */
//# sourceMappingURL=RecommendationPanel.vue.js.map