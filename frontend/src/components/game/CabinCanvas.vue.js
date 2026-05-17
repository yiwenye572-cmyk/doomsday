import { ref, onMounted, watch } from 'vue';
const props = withDefaults(defineProps(), {
    width: 800,
    height: 600,
    gridSize: 32,
    initialItems: () => []
});
const emit = defineEmits();
const canvas = ref(null);
const wrapRef = ref(null);
const items = ref(props.initialItems.map(i => ({ ...i })));
const undoStack = ref([]);
const selected = ref(null);
const dragging = ref(null);
const dragOffset = ref({ dx: 0, dy: 0 });
const collisionItem = ref(null);
// ─── Undo/Redo ──────────────────────────────────────────────────────────
function pushUndo() {
    undoStack.value.push(items.value.map(i => ({ ...i })));
    if (undoStack.value.length > 50)
        undoStack.value.shift();
}
function undo() {
    if (undoStack.value.length === 0)
        return;
    items.value = undoStack.value.pop();
    draw();
    emit('change', items.value);
}
// ─── Grid snapping ──────────────────────────────────────────────────────
function snap(v) {
    return Math.round(v / props.gridSize) * props.gridSize;
}
// ─── AABB Collision ─────────────────────────────────────────────────────
function hasCollision(item, exclude) {
    return items.value.some(other => {
        if (other.id === exclude)
            return false;
        return !(item.x + item.w <= other.x ||
            other.x + other.w <= item.x ||
            item.y + item.h <= other.y ||
            other.y + other.h <= item.y);
    });
}
// ─── Drawing ────────────────────────────────────────────────────────────
function draw() {
    const cvs = canvas.value;
    if (!cvs)
        return;
    const ctx = cvs.getContext('2d');
    ctx.clearRect(0, 0, props.width, props.height);
    // Background
    ctx.fillStyle = '#1a1a2e';
    ctx.fillRect(0, 0, props.width, props.height);
    // Grid
    ctx.strokeStyle = 'rgba(255,255,255,0.06)';
    ctx.lineWidth = 1;
    for (let x = 0; x < props.width; x += props.gridSize) {
        ctx.beginPath();
        ctx.moveTo(x, 0);
        ctx.lineTo(x, props.height);
        ctx.stroke();
    }
    for (let y = 0; y < props.height; y += props.gridSize) {
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(props.width, y);
        ctx.stroke();
    }
    // Items
    for (const item of items.value) {
        const isSelected = selected.value?.id === item.id;
        const isDragging = dragging.value?.id === item.id;
        const inConflict = collisionItem.value === item.id;
        ctx.save();
        ctx.fillStyle = inConflict ? 'rgba(255,80,80,0.5)'
            : isDragging ? 'rgba(100,180,255,0.7)'
                : (item.color ?? 'rgba(90,140,220,0.8)');
        ctx.strokeStyle = isSelected ? '#ffe77a' : 'rgba(255,255,255,0.3)';
        ctx.lineWidth = isSelected ? 2 : 1;
        ctx.fillRect(item.x, item.y, item.w, item.h);
        ctx.strokeRect(item.x, item.y, item.w, item.h);
        // Label
        ctx.fillStyle = '#fff';
        ctx.font = `${Math.min(12, item.h * 0.4)}px monospace`;
        ctx.fillText(item.type, item.x + 4, item.y + item.h / 2 + 4);
        ctx.restore();
    }
}
// ─── Pointer events ─────────────────────────────────────────────────────
function canvasPoint(e) {
    const rect = canvas.value.getBoundingClientRect();
    return { x: e.clientX - rect.left, y: e.clientY - rect.top };
}
function onPointerDown(e) {
    const { x, y } = canvasPoint(e);
    // hit-test from top (last = top)
    for (let i = items.value.length - 1; i >= 0; i--) {
        const item = items.value[i];
        if (x >= item.x && x <= item.x + item.w && y >= item.y && y <= item.y + item.h) {
            pushUndo();
            dragging.value = item;
            dragOffset.value = { dx: x - item.x, dy: y - item.y };
            selected.value = item;
            emit('select', item);
            canvas.value.setPointerCapture(e.pointerId);
            draw();
            return;
        }
    }
    selected.value = null;
    emit('select', null);
    draw();
}
function onPointerMove(e) {
    if (!dragging.value)
        return;
    const { x, y } = canvasPoint(e);
    const nx = snap(x - dragOffset.value.dx);
    const ny = snap(y - dragOffset.value.dy);
    // Clamp to canvas
    const cx = Math.max(0, Math.min(props.width - dragging.value.w, nx));
    const cy = Math.max(0, Math.min(props.height - dragging.value.h, ny));
    dragging.value.x = cx;
    dragging.value.y = cy;
    collisionItem.value = hasCollision(dragging.value, dragging.value.id) ? dragging.value.id : null;
    draw();
}
function onPointerUp(_e) {
    if (!dragging.value)
        return;
    if (collisionItem.value) {
        // rollback to last undo snapshot
        const last = undoStack.value[undoStack.value.length - 1];
        if (last) {
            items.value = last.map(i => ({ ...i }));
            undoStack.value.pop();
        }
    }
    else {
        emit('change', items.value);
    }
    collisionItem.value = null;
    dragging.value = null;
    draw();
}
// ─── Drop from ItemTray ─────────────────────────────────────────────────
function onDrop(e) {
    e.preventDefault();
    const raw = e.dataTransfer?.getData('application/cabin-item');
    if (!raw)
        return;
    const proto = JSON.parse(raw);
    const rect = canvas.value.getBoundingClientRect();
    const x = snap(e.clientX - rect.left - (proto.w ?? props.gridSize) / 2);
    const y = snap(e.clientY - rect.top - (proto.h ?? props.gridSize) / 2);
    const newItem = { ...proto, x, y };
    if (!hasCollision(newItem, newItem.id)) {
        pushUndo();
        items.value.push(newItem);
        draw();
        emit('change', items.value);
    }
}
// ─── Export / Import ────────────────────────────────────────────────────
function exportJson() {
    return JSON.stringify(items.value, null, 2);
}
function importJson(json) {
    pushUndo();
    items.value = JSON.parse(json);
    draw();
    emit('change', items.value);
}
// ─── Keyboard shortcuts ─────────────────────────────────────────────────
function handleKey(e) {
    if ((e.ctrlKey || e.metaKey) && e.key === 'z') {
        undo();
    }
}
onMounted(() => {
    draw();
    window.addEventListener('keydown', handleKey);
});
// re-draw when props.initialItems changes from parent
watch(() => props.initialItems, (val) => {
    items.value = val.map(i => ({ ...i }));
    draw();
}, { deep: true });
const __VLS_exposed = { exportJson, importJson, undo };
defineExpose(__VLS_exposed);
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_withDefaultsArg = (function (t) { return t; })({
    width: 800,
    height: 600,
    gridSize: 32,
    initialItems: () => []
});
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ onDragover: () => { } },
    ...{ onDrop: (__VLS_ctx.onDrop) },
    ...{ class: "cabin-canvas-wrap" },
    ref: "wrapRef",
});
/** @type {typeof __VLS_ctx.wrapRef} */ ;
__VLS_asFunctionalElement(__VLS_intrinsicElements.canvas)({
    ...{ onPointerdown: (__VLS_ctx.onPointerDown) },
    ...{ onPointermove: (__VLS_ctx.onPointerMove) },
    ...{ onPointerup: (__VLS_ctx.onPointerUp) },
    ...{ onPointerleave: (__VLS_ctx.onPointerUp) },
    ref: "canvas",
    width: (__VLS_ctx.width),
    height: (__VLS_ctx.height),
    ...{ class: "cabin-canvas" },
});
/** @type {typeof __VLS_ctx.canvas} */ ;
if (__VLS_ctx.collisionItem) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "collision-tip" },
    });
}
/** @type {__VLS_StyleScopedClasses['cabin-canvas-wrap']} */ ;
/** @type {__VLS_StyleScopedClasses['cabin-canvas']} */ ;
/** @type {__VLS_StyleScopedClasses['collision-tip']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            canvas: canvas,
            wrapRef: wrapRef,
            collisionItem: collisionItem,
            onPointerDown: onPointerDown,
            onPointerMove: onPointerMove,
            onPointerUp: onPointerUp,
            onDrop: onDrop,
        };
    },
    __typeEmits: {},
    __typeProps: {},
    props: {},
});
export default (await import('vue')).defineComponent({
    setup() {
        return {
            ...__VLS_exposed,
        };
    },
    __typeEmits: {},
    __typeProps: {},
    props: {},
});
; /* PartiallyEnd: #4569/main.vue */
//# sourceMappingURL=CabinCanvas.vue.js.map