import { computed, ref } from "vue";
const props = defineProps();
const emit = defineEmits();
const level = ref("L0");
const tabs = ["L0", "L1", "L2"];
const visible = computed(() => props.entries.slice().sort((a, b) => b.toTurn - a.toTurn || b.timestamp - a.timestamp));
function switchLevel(next) {
    if (next === level.value) {
        return;
    }
    level.value = next;
    emit("refresh", next);
}
function label(levelName) {
    if (levelName === "L0")
        return "即时记忆";
    if (levelName === "L1")
        return "阶段摘要";
    return "长期归档";
}
function toTime(ts) {
    const value = Number.isFinite(ts) ? ts : Date.now();
    return new Date(value).toLocaleString();
}
function dayPhase(row) {
    if (!row.dayIndex || !row.timePhaseLabel) {
        return "";
    }
    return ` · 第${row.dayIndex}天 · ${row.timePhaseLabel}`;
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['tab']} */ ;
/** @type {__VLS_StyleScopedClasses['hint']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel diary-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.header, __VLS_intrinsicElements.header)({
    ...{ class: "diary-head" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "tabs" },
});
for (const [tab] of __VLS_getVForSourceType((__VLS_ctx.tabs))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.switchLevel(tab);
            } },
        key: (tab),
        ...{ class: "tab" },
        ...{ class: ({ active: tab === __VLS_ctx.level }) },
        type: "button",
    });
    (tab);
    (__VLS_ctx.label(tab));
}
if (__VLS_ctx.loading) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "hint" },
    });
}
else if (__VLS_ctx.error) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "hint error" },
    });
    (__VLS_ctx.error);
}
else if (__VLS_ctx.visible.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.ul, __VLS_intrinsicElements.ul)({
        ...{ class: "list" },
    });
    for (const [row] of __VLS_getVForSourceType((__VLS_ctx.visible))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.li, __VLS_intrinsicElements.li)({
            ...{ class: "item" },
            key: (`${row.level}-${row.fromTurn}-${row.toTurn}-${row.timestamp}`),
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "meta" },
        });
        (row.level);
        (row.fromTurn);
        (row.toTurn);
        (__VLS_ctx.dayPhase(row));
        (__VLS_ctx.toTime(row.timestamp));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "summary" },
        });
        (row.summary);
        if (row.tags.length) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
                ...{ class: "tags" },
            });
            (row.tags.join("  #"));
        }
    }
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "hint" },
    });
}
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['diary-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['diary-head']} */ ;
/** @type {__VLS_StyleScopedClasses['tabs']} */ ;
/** @type {__VLS_StyleScopedClasses['tab']} */ ;
/** @type {__VLS_StyleScopedClasses['hint']} */ ;
/** @type {__VLS_StyleScopedClasses['hint']} */ ;
/** @type {__VLS_StyleScopedClasses['error']} */ ;
/** @type {__VLS_StyleScopedClasses['list']} */ ;
/** @type {__VLS_StyleScopedClasses['item']} */ ;
/** @type {__VLS_StyleScopedClasses['meta']} */ ;
/** @type {__VLS_StyleScopedClasses['summary']} */ ;
/** @type {__VLS_StyleScopedClasses['tags']} */ ;
/** @type {__VLS_StyleScopedClasses['hint']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            level: level,
            tabs: tabs,
            visible: visible,
            switchLevel: switchLevel,
            label: label,
            toTime: toTime,
            dayPhase: dayPhase,
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
//# sourceMappingURL=DiaryPanel.vue.js.map