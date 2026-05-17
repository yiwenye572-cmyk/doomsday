import { ref } from "vue";
import CabinCanvas from "@/components/game/CabinCanvas.vue";
import ItemTray from "@/components/game/ItemTray.vue";
import Inspector from "@/components/game/Inspector.vue";
import RecommendationPanel from "@/components/game/RecommendationPanel.vue";
import { submitImageGen, queryImageGen } from "@/api/game";
// ── Demo 数据（实际项目中从路由参数或 store 获取） ─────────────────────────
const sessionId = ref("demo-session-001");
const currentVersion = ref(0);
const statusMsg = ref(null);
const canvasRef = ref(null);
const showGrid = ref(true);
// 背景图：放置于 public/assets/cabin-bg.jpg（AI 生成后替换）
const cabinBgImage = ref('/assets/cabin-bg.jpg');
const canvasItems = ref([]);
const trayItems = ref([
    { id: "item-bed-01", name: "床", type: "bed", image: "/assets/items/bed.png", x: 0, y: 0 },
    { id: "item-table-01", name: "桌子", type: "table", image: "/assets/items/table.png", x: 0, y: 0 },
    { id: "item-axe-01", name: "斧头", type: "axe", image: "/assets/items/axe.png", x: 0, y: 0 },
    { id: "item-medkit-01", name: "医疗包", type: "medkit", image: "/assets/items/medkit.png", x: 0, y: 0 },
    { id: "item-window-01", name: "窗户", type: "window", image: "/assets/items/window.png", x: 0, y: 0 },
]);
const selectedItem = ref(null);
// ── AI 背景生成 ──────────────────────────────────────────────────────────
const genLoading = ref(false);
const genStatusText = ref("");
async function generateBg() {
    genLoading.value = true;
    genStatusText.value = "提交中…";
    try {
        const res = await submitImageGen("", "cabin_bg");
        // 若缓存命中，直接使用返回的 imageUrl
        if (res.status === "SUCCEEDED" && res.imageUrl) {
            cabinBgImage.value = res.imageUrl;
            showStatus("✅ 背景已从缓存加载");
            return;
        }
        // 异步任务：轮询最多 20 次（每 3s）
        const taskId = res.taskId;
        let tries = 0;
        genStatusText.value = "生成中…";
        const poll = setInterval(async () => {
            tries++;
            try {
                const r = await queryImageGen(taskId);
                if (r.status === "SUCCEEDED" && r.imageUrl) {
                    clearInterval(poll);
                    cabinBgImage.value = r.imageUrl;
                    genLoading.value = false;
                    showStatus("✅ 背景图已生成！");
                }
                else if (r.status === "FAILED" || tries >= 20) {
                    clearInterval(poll);
                    genLoading.value = false;
                    showStatus(r.status === "FAILED" ? "❌ 生成失败：" + (r.message ?? "") : "⏱ 超时，稍后重试");
                }
                else {
                    genStatusText.value = `生成中… (${tries * 3}s)`;
                }
            }
            catch {
                clearInterval(poll);
                genLoading.value = false;
                showStatus("❌ 轮询出错");
            }
        }, 3000);
    }
    catch (e) {
        showStatus("❌ 提交失败：" + (e instanceof Error ? e.message : String(e)));
        genLoading.value = false;
    }
}
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
/** @type {__VLS_StyleScopedClasses['btn-gen-bg']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-gen-bg']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "cabin-demo-page" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "toolbar-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.generateBg) },
    ...{ class: "btn-gen-bg" },
    disabled: (__VLS_ctx.genLoading),
});
if (__VLS_ctx.genLoading) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "spinner" },
    });
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
}
if (__VLS_ctx.genLoading) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "gen-status" },
    });
    (__VLS_ctx.genStatusText);
}
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
    bgImage: (__VLS_ctx.cabinBgImage),
    showGrid: (__VLS_ctx.showGrid),
    ref: "canvasRef",
}));
const __VLS_8 = __VLS_7({
    width: (640),
    height: (480),
    initialItems: (__VLS_ctx.canvasItems),
    bgImage: (__VLS_ctx.cabinBgImage),
    showGrid: (__VLS_ctx.showGrid),
    ref: "canvasRef",
}, ...__VLS_functionalComponentArgsRest(__VLS_7));
/** @type {typeof __VLS_ctx.canvasRef} */ ;
var __VLS_10 = {};
var __VLS_9;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "canvas-toolbar" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "grid-toggle" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "checkbox",
});
(__VLS_ctx.showGrid);
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
/** @type {__VLS_StyleScopedClasses['btn-gen-bg']} */ ;
/** @type {__VLS_StyleScopedClasses['spinner']} */ ;
/** @type {__VLS_StyleScopedClasses['gen-status']} */ ;
/** @type {__VLS_StyleScopedClasses['main-layout']} */ ;
/** @type {__VLS_StyleScopedClasses['canvas-area']} */ ;
/** @type {__VLS_StyleScopedClasses['canvas-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['grid-toggle']} */ ;
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
            showGrid: showGrid,
            cabinBgImage: cabinBgImage,
            canvasItems: canvasItems,
            trayItems: trayItems,
            selectedItem: selectedItem,
            genLoading: genLoading,
            genStatusText: genStatusText,
            generateBg: generateBg,
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