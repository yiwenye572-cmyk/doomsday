import { ref } from "vue";
import CabinCanvas from "@/components/game/CabinCanvas.vue";
import ItemTray from "@/components/game/ItemTray.vue";
import Inspector from "@/components/game/Inspector.vue";
import RecommendationPanel from "@/components/game/RecommendationPanel.vue";
// ── Demo 数据（实际项目中从路由参数或 store 获取） ─────────────────────────
const sessionId = ref("demo-session-001");
const currentVersion = ref(0);
const statusMsg = ref(null);
const canvasRef = ref(null);
const canvasItems = ref([]);
const trayItems = ref([
    { id: "item-bed-01", name: "床", type: "bed", image: "/assets/bed.png", x: 0, y: 0 },
    { id: "item-table-01", name: "桌子", type: "table", image: "/assets/table.png", x: 0, y: 0 },
    { id: "item-axe-01", name: "斧头", type: "axe", image: "/assets/axe.png", x: 0, y: 0 },
    { id: "item-medkit-01", name: "医疗包", type: "medkit", image: "/assets/medkit.png", x: 0, y: 0 },
    { id: "item-window-01", name: "窗户", type: "window", image: "/assets/window.png", x: 0, y: 0 },
]);
const selectedItem = ref(null);
// ── 推荐应用回调 ──────────────────────────────────────────────────────────
function onRecommendationApplied(newVersion, stateData) {
    currentVersion.value = newVersion;
    // 尝试将推荐布局解析并加载到画布
    try {
        const state = JSON.parse(stateData);
        if (Array.isArray(state?.items)) {
            canvasItems.value = state.items;
        }
    }
    catch { /* ignore parse error */ }
    showStatus("✅ 推荐布局已应用！版本 → " + newVersion);
}
// ── 其他事件 ─────────────────────────────────────────────────────────────
function handleDragStart(item) {
    console.log("[CabinDemoPage] dragstart", item);
}
function rotateItem(item) {
    console.log("[CabinDemoPage] rotate", item);
}
function deleteItem(item) {
    console.log("[CabinDemoPage] delete", item);
}
function showStatus(msg) {
    statusMsg.value = msg;
    setTimeout(() => { statusMsg.value = null; }, 3000);
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "cabin-demo-page" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "toolbar-row" },
});
/** @type {[typeof RecommendationPanel, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(RecommendationPanel, new RecommendationPanel({
    ...{ 'onApplied': {} },
    sessionId: (__VLS_ctx.sessionId),
    currentVersion: (__VLS_ctx.currentVersion),
}));
const __VLS_1 = __VLS_0({
    ...{ 'onApplied': {} },
    sessionId: (__VLS_ctx.sessionId),
    currentVersion: (__VLS_ctx.currentVersion),
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
let __VLS_3;
let __VLS_4;
let __VLS_5;
const __VLS_6 = {
    onApplied: (__VLS_ctx.onRecommendationApplied)
};
var __VLS_2;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "main-layout" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "canvas-area" },
});
/** @type {[typeof CabinCanvas, ]} */ ;
// @ts-ignore
const __VLS_7 = __VLS_asFunctionalComponent(CabinCanvas, new CabinCanvas({
    width: (640),
    height: (480),
    initialItems: (__VLS_ctx.canvasItems),
    ref: "canvasRef",
}));
const __VLS_8 = __VLS_7({
    width: (640),
    height: (480),
    initialItems: (__VLS_ctx.canvasItems),
    ref: "canvasRef",
}, ...__VLS_functionalComponentArgsRest(__VLS_7));
/** @type {typeof __VLS_ctx.canvasRef} */ ;
var __VLS_10 = {};
var __VLS_9;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "side-panel" },
});
/** @type {[typeof ItemTray, ]} */ ;
// @ts-ignore
const __VLS_12 = __VLS_asFunctionalComponent(ItemTray, new ItemTray({
    ...{ 'onDragstart': {} },
    items: (__VLS_ctx.trayItems),
}));
const __VLS_13 = __VLS_12({
    ...{ 'onDragstart': {} },
    items: (__VLS_ctx.trayItems),
}, ...__VLS_functionalComponentArgsRest(__VLS_12));
let __VLS_15;
let __VLS_16;
let __VLS_17;
const __VLS_18 = {
    onDragstart: (__VLS_ctx.handleDragStart)
};
var __VLS_14;
/** @type {[typeof Inspector, ]} */ ;
// @ts-ignore
const __VLS_19 = __VLS_asFunctionalComponent(Inspector, new Inspector({
    ...{ 'onRotateItem': {} },
    ...{ 'onDeleteItem': {} },
    selectedItem: (__VLS_ctx.selectedItem),
    sessionId: (__VLS_ctx.sessionId),
}));
const __VLS_20 = __VLS_19({
    ...{ 'onRotateItem': {} },
    ...{ 'onDeleteItem': {} },
    selectedItem: (__VLS_ctx.selectedItem),
    sessionId: (__VLS_ctx.sessionId),
}, ...__VLS_functionalComponentArgsRest(__VLS_19));
let __VLS_22;
let __VLS_23;
let __VLS_24;
const __VLS_25 = {
    onRotateItem: (__VLS_ctx.rotateItem)
};
const __VLS_26 = {
    onDeleteItem: (__VLS_ctx.deleteItem)
};
var __VLS_21;
if (__VLS_ctx.statusMsg) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "status-bar" },
    });
    (__VLS_ctx.statusMsg);
}
/** @type {__VLS_StyleScopedClasses['cabin-demo-page']} */ ;
/** @type {__VLS_StyleScopedClasses['toolbar-row']} */ ;
/** @type {__VLS_StyleScopedClasses['main-layout']} */ ;
/** @type {__VLS_StyleScopedClasses['canvas-area']} */ ;
/** @type {__VLS_StyleScopedClasses['side-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['status-bar']} */ ;
// @ts-ignore
var __VLS_11 = __VLS_10;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            CabinCanvas: CabinCanvas,
            ItemTray: ItemTray,
            Inspector: Inspector,
            RecommendationPanel: RecommendationPanel,
            sessionId: sessionId,
            currentVersion: currentVersion,
            statusMsg: statusMsg,
            canvasRef: canvasRef,
            canvasItems: canvasItems,
            trayItems: trayItems,
            selectedItem: selectedItem,
            onRecommendationApplied: onRecommendationApplied,
            handleDragStart: handleDragStart,
            rotateItem: rotateItem,
            deleteItem: deleteItem,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
//# sourceMappingURL=CabinDemoPage.vue.js.map