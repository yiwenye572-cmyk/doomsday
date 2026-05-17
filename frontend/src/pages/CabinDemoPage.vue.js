import CabinCanvas from '@/components/game/CabinCanvas.vue';
import CabinStage from '@/components/game/CabinStage.vue';
import ItemTray from '@/components/game/ItemTray.vue';
import Inspector from '@/components/game/Inspector.vue';
export default (await import('vue')).defineComponent({
    name: 'CabinDemoPage',
    components: {
        CabinCanvas,
        CabinStage,
        ItemTray,
        Inspector
    },
    data() {
        return {
            items: [
                { id: 'item1', name: 'Bed', type: 'bed', image: '/assets/bed.png', x: 0, y: 0 },
                { id: 'item2', name: 'Table', type: 'table', image: '/assets/table.png', x: 0, y: 0 }
            ],
            selectedItem: null
        };
    },
    methods: {
        handleDragStart(item) {
            console.log('Dragging item:', item);
        },
        viewStory(item) {
            console.log('View story for item:', item);
        },
        rotateItem(item) {
            console.log('Rotate item:', item);
        },
        deleteItem(item) {
            console.log('Delete item:', item);
        }
    }
});
const __VLS_ctx = {};
const __VLS_componentsOption = {
    CabinCanvas,
    CabinStage,
    ItemTray,
    Inspector
};
let __VLS_components;
let __VLS_directives;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "cabin-demo-page" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "canvas-container" },
});
const __VLS_0 = {}.CabinCanvas;
/** @type {[typeof __VLS_components.CabinCanvas, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    width: (800),
    height: (600),
}));
const __VLS_2 = __VLS_1({
    width: (800),
    height: (600),
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "stage-container" },
});
const __VLS_4 = {}.CabinStage;
/** @type {[typeof __VLS_components.CabinStage, ]} */ ;
// @ts-ignore
const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
    width: (800),
    height: (600),
}));
const __VLS_6 = __VLS_5({
    width: (800),
    height: (600),
}, ...__VLS_functionalComponentArgsRest(__VLS_5));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "tray-container" },
});
const __VLS_8 = {}.ItemTray;
/** @type {[typeof __VLS_components.ItemTray, ]} */ ;
// @ts-ignore
const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
    ...{ 'onDragstart': {} },
    items: (__VLS_ctx.items),
}));
const __VLS_10 = __VLS_9({
    ...{ 'onDragstart': {} },
    items: (__VLS_ctx.items),
}, ...__VLS_functionalComponentArgsRest(__VLS_9));
let __VLS_12;
let __VLS_13;
let __VLS_14;
const __VLS_15 = {
    onDragstart: (__VLS_ctx.handleDragStart)
};
var __VLS_11;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "inspector-container" },
});
const __VLS_16 = {}.Inspector;
/** @type {[typeof __VLS_components.Inspector, ]} */ ;
// @ts-ignore
const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({
    ...{ 'onViewStory': {} },
    ...{ 'onRotateItem': {} },
    ...{ 'onDeleteItem': {} },
    selectedItem: (__VLS_ctx.selectedItem),
}));
const __VLS_18 = __VLS_17({
    ...{ 'onViewStory': {} },
    ...{ 'onRotateItem': {} },
    ...{ 'onDeleteItem': {} },
    selectedItem: (__VLS_ctx.selectedItem),
}, ...__VLS_functionalComponentArgsRest(__VLS_17));
let __VLS_20;
let __VLS_21;
let __VLS_22;
const __VLS_23 = {
    onViewStory: (__VLS_ctx.viewStory)
};
const __VLS_24 = {
    onRotateItem: (__VLS_ctx.rotateItem)
};
const __VLS_25 = {
    onDeleteItem: (__VLS_ctx.deleteItem)
};
var __VLS_19;
/** @type {__VLS_StyleScopedClasses['cabin-demo-page']} */ ;
/** @type {__VLS_StyleScopedClasses['canvas-container']} */ ;
/** @type {__VLS_StyleScopedClasses['stage-container']} */ ;
/** @type {__VLS_StyleScopedClasses['tray-container']} */ ;
/** @type {__VLS_StyleScopedClasses['inspector-container']} */ ;
var __VLS_dollars;
let __VLS_self;
//# sourceMappingURL=CabinDemoPage.vue.js.map