export default (await import('vue')).defineComponent({
    name: 'ItemTray',
    props: {
        items: {
            type: Array,
            required: true
        }
    },
    methods: {
        onDragStart(item) {
            this.$emit('dragstart', item);
        }
    }
});
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['item']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-tray" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "items" },
});
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.items))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onDragstart: (...[$event]) => {
                __VLS_ctx.onDragStart(item);
            } },
        key: (item.id),
        ...{ class: "item" },
        draggable: "true",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.img)({
        src: (item.image),
        alt: (item.name),
    });
}
/** @type {__VLS_StyleScopedClasses['item-tray']} */ ;
/** @type {__VLS_StyleScopedClasses['items']} */ ;
/** @type {__VLS_StyleScopedClasses['item']} */ ;
var __VLS_dollars;
let __VLS_self;
//# sourceMappingURL=ItemTray.vue.js.map