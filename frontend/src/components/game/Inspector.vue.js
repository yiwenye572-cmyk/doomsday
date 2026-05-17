import { ref, watch } from "vue";
import { getItemStory } from "../../api/game";
const props = defineProps();
const __VLS_emit = defineEmits();
// ── 故事状态 ──────────────────────────────────────────────────────────────
const storyVisible = ref(false);
const storyLoading = ref(false);
const storyStatus = ref("");
const storyText = ref(null);
const storyError = ref(null);
// 切换物品时重置故事面板
watch(() => props.selectedItem?.id, () => {
    storyVisible.value = false;
    storyLoading.value = false;
    storyStatus.value = "";
    storyText.value = null;
    storyError.value = null;
});
let pollTimer = null;
const MAX_POLLS = 15; // 最多轮询 15 次 × 2s = 30s
const POLL_INTERVAL_MS = 2000;
async function triggerStory() {
    if (!props.selectedItem || !props.sessionId)
        return;
    storyVisible.value = true;
    storyLoading.value = true;
    storyStatus.value = "PENDING";
    storyText.value = null;
    storyError.value = null;
    if (pollTimer)
        clearTimeout(pollTimer);
    await poll(0);
}
async function poll(count) {
    if (!props.selectedItem)
        return;
    try {
        const res = await getItemStory(props.sessionId, props.selectedItem.id, props.selectedItem.type);
        const d = res.data;
        storyStatus.value = d.status;
        if (d.status === "DONE") {
            storyText.value = d.story;
            storyLoading.value = false;
            return;
        }
        if (d.status === "FAILED") {
            storyError.value = d.errorMessage ?? "未知错误";
            storyLoading.value = false;
            return;
        }
        // PENDING / RUNNING — 继续轮询
        if (count < MAX_POLLS) {
            pollTimer = setTimeout(() => poll(count + 1), POLL_INTERVAL_MS);
        }
        else {
            storyError.value = "生成超时，请稍后重试";
            storyStatus.value = "FAILED";
            storyLoading.value = false;
        }
    }
    catch (e) {
        storyError.value = "网络错误，请重试";
        storyStatus.value = "FAILED";
        storyLoading.value = false;
    }
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['danger']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "inspector" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
if (__VLS_ctx.selectedItem) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (__VLS_ctx.selectedItem.name);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (__VLS_ctx.selectedItem.type);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (__VLS_ctx.selectedItem.x);
    (__VLS_ctx.selectedItem.y);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "btn-row" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.triggerStory) },
        disabled: (__VLS_ctx.storyLoading),
    });
    (__VLS_ctx.storyLoading ? '生成中…' : '查看故事');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.selectedItem))
                    return;
                __VLS_ctx.$emit('rotate-item', __VLS_ctx.selectedItem);
            } },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.selectedItem))
                    return;
                __VLS_ctx.$emit('delete-item', __VLS_ctx.selectedItem);
            } },
        ...{ class: "danger" },
    });
    const __VLS_0 = {}.transition;
    /** @type {[typeof __VLS_components.Transition, typeof __VLS_components.transition, typeof __VLS_components.Transition, typeof __VLS_components.transition, ]} */ ;
    // @ts-ignore
    const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
        name: "fade",
    }));
    const __VLS_2 = __VLS_1({
        name: "fade",
    }, ...__VLS_functionalComponentArgsRest(__VLS_1));
    __VLS_3.slots.default;
    if (__VLS_ctx.storyVisible) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "story-panel" },
        });
        if (__VLS_ctx.storyStatus === 'DONE') {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "story-text" },
            });
            (__VLS_ctx.storyText);
        }
        else if (__VLS_ctx.storyStatus === 'FAILED') {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "story-error" },
            });
            (__VLS_ctx.storyError);
        }
        else {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "story-pending" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span)({
                ...{ class: "spinner" },
            });
        }
    }
    var __VLS_3;
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "empty" },
    });
}
/** @type {__VLS_StyleScopedClasses['inspector']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-row']} */ ;
/** @type {__VLS_StyleScopedClasses['danger']} */ ;
/** @type {__VLS_StyleScopedClasses['story-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['story-text']} */ ;
/** @type {__VLS_StyleScopedClasses['story-error']} */ ;
/** @type {__VLS_StyleScopedClasses['story-pending']} */ ;
/** @type {__VLS_StyleScopedClasses['spinner']} */ ;
/** @type {__VLS_StyleScopedClasses['empty']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            storyVisible: storyVisible,
            storyLoading: storyLoading,
            storyStatus: storyStatus,
            storyText: storyText,
            storyError: storyError,
            triggerStory: triggerStory,
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
//# sourceMappingURL=Inspector.vue.js.map