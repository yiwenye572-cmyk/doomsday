const __VLS_props = defineProps();
const emit = defineEmits();
// Emoji 占位，图片加载前 / 加载失败时显示
const EMOJI_MAP = {
    bed: '🛏',
    table: '🪑',
    axe: '🪓',
    medkit: '🧰',
    window: '🪟',
    radio: '📻',
    shelf: '📚',
    map: '🗺',
    can: '🥫',
    tool: '🔧',
    chest: '📦',
    lantern: '🪔',
};
function typeEmoji(type) {
    return EMOJI_MAP[type] ?? '📦';
}
function onDragStart(e, item) {
    // 传递物品数据供 CabinCanvas.onDrop 使用
    e.dataTransfer?.setData('application/cabin-item', JSON.stringify({ id: item.id, type: item.type, image: item.image, w: 64, h: 64, rotation: 0 }));
    emit('dragstart', item);
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['item-card']} */ ;
/** @type {__VLS_StyleScopedClasses['item-card']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-tray" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "tray-header" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "tray-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "tray-hint" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "items" },
});
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.items))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onDragstart: (...[$event]) => {
                __VLS_ctx.onDragStart($event, item);
            } },
        key: (item.id),
        ...{ class: "item-card" },
        draggable: "true",
        title: (item.name),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "item-icon" },
    });
    if (item.image) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.img)({
            ...{ onError: ((e) => (e.target.style.display = 'none')) },
            src: (item.image),
            alt: (item.name),
            ...{ class: "item-img" },
        });
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "item-emoji" },
    });
    (__VLS_ctx.typeEmoji(item.type));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "item-name" },
    });
    (item.name);
}
/** @type {__VLS_StyleScopedClasses['item-tray']} */ ;
/** @type {__VLS_StyleScopedClasses['tray-header']} */ ;
/** @type {__VLS_StyleScopedClasses['tray-title']} */ ;
/** @type {__VLS_StyleScopedClasses['tray-hint']} */ ;
/** @type {__VLS_StyleScopedClasses['items']} */ ;
/** @type {__VLS_StyleScopedClasses['item-card']} */ ;
/** @type {__VLS_StyleScopedClasses['item-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['item-img']} */ ;
/** @type {__VLS_StyleScopedClasses['item-emoji']} */ ;
/** @type {__VLS_StyleScopedClasses['item-name']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            typeEmoji: typeEmoji,
            onDragStart: onDragStart,
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
//# sourceMappingURL=ItemTray.vue.js.map