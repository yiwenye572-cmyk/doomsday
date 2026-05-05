const props = defineProps();
const emit = defineEmits();
function choose(optionId) {
    if (props.loading) {
        return;
    }
    emit("choose", optionId);
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['option-card']} */ ;
/** @type {__VLS_StyleScopedClasses['option-card']} */ ;
/** @type {__VLS_StyleScopedClasses['options-grid']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "options-grid" },
});
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.options))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.choose(option.id);
            } },
        key: (option.id),
        ...{ class: "option-card" },
        disabled: (__VLS_ctx.loading),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "option-head" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (option.id.toUpperCase());
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "tag" },
    });
    (option.riskLevel);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "option-text" },
    });
    (option.text);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "option-effect" },
    });
    (option.expectedEffect);
}
/** @type {__VLS_StyleScopedClasses['options-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['option-card']} */ ;
/** @type {__VLS_StyleScopedClasses['option-head']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['option-text']} */ ;
/** @type {__VLS_StyleScopedClasses['option-effect']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            choose: choose,
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
//# sourceMappingURL=OptionsGrid.vue.js.map