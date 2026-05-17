<template>
  <div class="cabin-canvas-wrap" ref="wrapRef"
       @dragover.prevent
       @drop="onDrop">
    <canvas
      ref="canvas"
      :width="width"
      :height="height"
      class="cabin-canvas"
      @pointerdown="onPointerDown"
      @pointermove="onPointerMove"
      @pointerup="onPointerUp"
      @pointerleave="onPointerUp"
    />
    <!-- 冲突提示浮层 -->
    <div v-if="collisionItem" class="collision-tip">⚠ 位置冲突，无法放置</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';

export interface CabinItem {
  id: string;
  type: string;
  x: number;
  y: number;
  w: number;
  h: number;
  rotation: number;
  color?: string;
}

const props = withDefaults(defineProps<{
  width?: number;
  height?: number;
  gridSize?: number;
  initialItems?: CabinItem[];
}>(), {
  width: 800,
  height: 600,
  gridSize: 32,
  initialItems: () => []
});

const emit = defineEmits<{
  (e: 'change', items: CabinItem[]): void;
  (e: 'select', item: CabinItem | null): void;
}>();

const canvas = ref<HTMLCanvasElement | null>(null);
const wrapRef = ref<HTMLDivElement | null>(null);

const items = ref<CabinItem[]>(props.initialItems.map(i => ({ ...i })));
const undoStack = ref<CabinItem[][]>([]);
const selected = ref<CabinItem | null>(null);
const dragging = ref<CabinItem | null>(null);
const dragOffset = ref({ dx: 0, dy: 0 });
const collisionItem = ref<string | null>(null);

// ─── Undo/Redo ──────────────────────────────────────────────────────────

function pushUndo() {
  undoStack.value.push(items.value.map(i => ({ ...i })));
  if (undoStack.value.length > 50) undoStack.value.shift();
}

function undo() {
  if (undoStack.value.length === 0) return;
  items.value = undoStack.value.pop()!;
  draw();
  emit('change', items.value);
}

// ─── Grid snapping ──────────────────────────────────────────────────────

function snap(v: number): number {
  return Math.round(v / props.gridSize) * props.gridSize;
}

// ─── AABB Collision ─────────────────────────────────────────────────────

function hasCollision(item: CabinItem, exclude: string): boolean {
  return items.value.some(other => {
    if (other.id === exclude) return false;
    return !(item.x + item.w <= other.x ||
             other.x + other.w <= item.x ||
             item.y + item.h <= other.y ||
             other.y + other.h <= item.y);
  });
}

// ─── Drawing ────────────────────────────────────────────────────────────

function draw() {
  const cvs = canvas.value;
  if (!cvs) return;
  const ctx = cvs.getContext('2d')!;
  ctx.clearRect(0, 0, props.width, props.height);

  // Background
  ctx.fillStyle = '#1a1a2e';
  ctx.fillRect(0, 0, props.width, props.height);

  // Grid
  ctx.strokeStyle = 'rgba(255,255,255,0.06)';
  ctx.lineWidth = 1;
  for (let x = 0; x < props.width; x += props.gridSize) {
    ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, props.height); ctx.stroke();
  }
  for (let y = 0; y < props.height; y += props.gridSize) {
    ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(props.width, y); ctx.stroke();
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

function canvasPoint(e: PointerEvent): { x: number; y: number } {
  const rect = canvas.value!.getBoundingClientRect();
  return { x: e.clientX - rect.left, y: e.clientY - rect.top };
}

function onPointerDown(e: PointerEvent) {
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
      canvas.value!.setPointerCapture(e.pointerId);
      draw();
      return;
    }
  }
  selected.value = null;
  emit('select', null);
  draw();
}

function onPointerMove(e: PointerEvent) {
  if (!dragging.value) return;
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

function onPointerUp(_e: PointerEvent) {
  if (!dragging.value) return;
  if (collisionItem.value) {
    // rollback to last undo snapshot
    const last = undoStack.value[undoStack.value.length - 1];
    if (last) {
      items.value = last.map(i => ({ ...i }));
      undoStack.value.pop();
    }
  } else {
    emit('change', items.value);
  }
  collisionItem.value = null;
  dragging.value = null;
  draw();
}

// ─── Drop from ItemTray ─────────────────────────────────────────────────

function onDrop(e: DragEvent) {
  e.preventDefault();
  const raw = e.dataTransfer?.getData('application/cabin-item');
  if (!raw) return;
  const proto = JSON.parse(raw) as Omit<CabinItem, 'x' | 'y'>;
  const rect = canvas.value!.getBoundingClientRect();
  const x = snap(e.clientX - rect.left - (proto.w ?? props.gridSize) / 2);
  const y = snap(e.clientY - rect.top  - (proto.h ?? props.gridSize) / 2);
  const newItem: CabinItem = { ...proto, x, y };
  if (!hasCollision(newItem, newItem.id)) {
    pushUndo();
    items.value.push(newItem);
    draw();
    emit('change', items.value);
  }
}

// ─── Export / Import ────────────────────────────────────────────────────

function exportJson(): string {
  return JSON.stringify(items.value, null, 2);
}

function importJson(json: string) {
  pushUndo();
  items.value = JSON.parse(json);
  draw();
  emit('change', items.value);
}

// ─── Keyboard shortcuts ─────────────────────────────────────────────────

function handleKey(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'z') { undo(); }
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

defineExpose({ exportJson, importJson, undo });
</script>

<style scoped>
.cabin-canvas-wrap {
  position: relative;
  display: inline-block;
}
.cabin-canvas {
  image-rendering: pixelated;
  cursor: crosshair;
  display: block;
}
.collision-tip {
  position: absolute;
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(200, 40, 40, 0.85);
  color: #fff;
  padding: 4px 12px;
  border-radius: 8px;
  font-size: 13px;
  pointer-events: none;
}
</style>
