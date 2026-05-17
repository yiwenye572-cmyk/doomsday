<template>
  <div class="item-tray">
    <div class="tray-header">
      <span class="tray-title">物品托盘</span>
      <span class="tray-hint">拖入画布放置</span>
    </div>
    <div class="items">
      <div
        v-for="item in items"
        :key="item.id"
        class="item-card"
        draggable="true"
        :title="item.name"
        @dragstart="onDragStart($event, item)"
      >
        <div class="item-icon">
          <img
            v-if="item.image"
            :src="item.image"
            :alt="item.name"
            class="item-img"
            @error="(e) => ((e.target as HTMLImageElement).style.display = 'none')"
          />
          <span class="item-emoji">{{ typeEmoji(item.type) }}</span>
        </div>
        <span class="item-name">{{ item.name }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface TrayItem {
  id: string;
  name: string;
  type: string;
  image?: string;
  x?: number;
  y?: number;
}

defineProps<{ items: TrayItem[] }>();
const emit = defineEmits<{ (e: 'dragstart', item: TrayItem): void }>();

// Emoji 占位，图片加载前 / 加载失败时显示
const EMOJI_MAP: Record<string, string> = {
  bed:     '🛏',
  table:   '🪑',
  axe:     '🪓',
  medkit:  '🧰',
  window:  '🪟',
  radio:   '📻',
  shelf:   '📚',
  map:     '🗺',
  can:     '🥫',
  tool:    '🔧',
  chest:   '📦',
  lantern: '🪔',
};

function typeEmoji(type: string): string {
  return EMOJI_MAP[type] ?? '📦';
}

function onDragStart(e: DragEvent, item: TrayItem) {
  // 传递物品数据供 CabinCanvas.onDrop 使用
  e.dataTransfer?.setData(
    'application/cabin-item',
    JSON.stringify({ id: item.id, type: item.type, image: item.image, w: 64, h: 64, rotation: 0 }),
  );
  emit('dragstart', item);
}
</script>

<style scoped>
.item-tray {
  background: rgba(12, 14, 22, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  overflow: hidden;
}

.tray-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px 6px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.tray-title {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: #e0c97f;
  font-family: var(--font-display, monospace);
}

.tray-hint {
  font-size: 10px;
  color: #555;
}

.items {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px;
  padding: 8px;
  max-height: 340px;
  overflow-y: auto;
}

.item-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 4px 6px;
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.07);
  background: rgba(255, 255, 255, 0.04);
  cursor: grab;
  transition: background 0.15s, border-color 0.15s, transform 0.1s;
  user-select: none;
}

.item-card:hover {
  background: rgba(224, 201, 127, 0.1);
  border-color: rgba(224, 201, 127, 0.35);
  transform: translateY(-1px);
}

.item-card:active {
  cursor: grabbing;
  transform: scale(0.96);
}

.item-icon {
  position: relative;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  image-rendering: pixelated;
}

.item-emoji {
  font-size: 28px;
  line-height: 1;
  /* 图片加载成功后仍显示在下层，起背景作用 */
  z-index: 0;
}

.item-name {
  font-size: 11px;
  color: #aaa;
  text-align: center;
  white-space: nowrap;
}
</style>
