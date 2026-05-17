export default (await import('vue')).defineComponent({
    name: 'CabinStage',
    props: {
        width: {
            type: Number,
            default: 800
        },
        height: {
            type: Number,
            default: 600
        }
    }
});
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "cabin-stage" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.svg, __VLS_intrinsicElements.svg)({
    width: (__VLS_ctx.width),
    height: (__VLS_ctx.height),
    xmlns: "http://www.w3.org/2000/svg",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.rect)({
    width: (__VLS_ctx.width),
    height: (__VLS_ctx.height),
    fill: "#f0f0f0",
});
/** @type {__VLS_StyleScopedClasses['cabin-stage']} */ ;
var __VLS_dollars;
let __VLS_self;
//# sourceMappingURL=CabinStage.vue.js.map