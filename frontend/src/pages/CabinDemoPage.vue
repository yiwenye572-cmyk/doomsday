<template>
  <div class="cabin-demo-page">
    <h1>末日小屋</h1>

    <!-- 顶部工具栏区 -->
    <div class="toolbar-row">
      <RecommendationPanel
        :sessionId="sessionId"
        :currentVersion="currentVersion"
        @applied="onRecommendationApplied"
      />
    </div>

    <div class="main-layout">
      <!-- 左：画布 -->
      <div class="canvas-area">
        <CabinCanvas :width="640" :height="480" :initialItems="canvasItems" ref="canvasRef" />
      </div>

      <!-- 右：物品托盘 + 检查器 -->
      <div class="side-panel">
        <ItemTray :items="trayItems" @dragstart="handleDragStart" />
        <Inspector
          :selectedItem="selectedItem"
          :sessionId="sessionId"
          @rotate-item="rotateItem"
          @delete-item="deleteItem"
        />
      </div>
    </div>

    <!-- 状态栏 -->
    <div class="status-bar" v-if="statusMsg">{{ statusMsg }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import CabinCanvas, { type CabinItem } from "@/components/game/CabinCanvas.vue";
import ItemTray from "@/components/game/ItemTray.vue";
import Inspector from "@/components/game/Inspector.vue";
import RecommendationPanel from "@/components/game/RecommendationPanel.vue";

interface TrayItem { id: string; name: string; type: string; image: string; x: number; y: number; }

// ── Demo 数据（实际项目中从路由参数或 store 获取） ─────────────────────────
const sessionId      = ref("demo-session-001");
const currentVersion = ref(0);
const statusMsg      = ref<string | null>(null);
const canvasRef      = ref<InstanceType<typeof CabinCanvas> | null>(null);

const canvasItems = ref<CabinItem[]>([]);

const trayItems = ref<TrayItem[]>([
  { id: "item-bed-01",    name: "床",     type: "bed",    image: "/assets/bed.png",    x: 0, y: 0 },
  { id: "item-table-01",  name: "桌子",   type: "table",  image: "/assets/table.png",  x: 0, y: 0 },
  { id: "item-axe-01",    name: "斧头",   type: "axe",    image: "/assets/axe.png",    x: 0, y: 0 },
  { id: "item-medkit-01", name: "医疗包", type: "medkit", image: "/assets/medkit.png", x: 0, y: 0 },
  { id: "item-window-01", name: "窗户",   type: "window", image: "/assets/window.png", x: 0, y: 0 },
]);

const selectedItem = ref<TrayItem | null>(null);

// ── 推荐应用回调 ──────────────────────────────────────────────────────────
function onRecommendationApplied(newVersion: number, stateData: string) {
  currentVersion.value = newVersion;
  // 尝试将推荐布局解析并加载到画布
  try {
    const state = JSON.parse(stateData);
    if (Array.isArray(state?.items)) {
      canvasItems.value = state.items as CabinItem[];
    }
  } catch { /* ignore parse error */ }
  showStatus("✅ 推荐布局已应用！版本 → " + newVersion);
}

// ── 其他事件 ─────────────────────────────────────────────────────────────
function handleDragStart(item: unknown) {
  console.log("[CabinDemoPage] dragstart", item);
}
function rotateItem(item: unknown) {
  console.log("[CabinDemoPage] rotate", item);
}
function deleteItem(item: unknown) {
  console.log("[CabinDemoPage] delete", item);
}

function showStatus(msg: string) {
  statusMsg.value = msg;
  setTimeout(() => { statusMsg.value = null; }, 3000);
}
</script>

<style scoped>
.cabin-demo-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  background: #0e0e1a;
  min-height: 100vh;
  color: #ccc;
}

h1 { color: #e0c97f; margin: 0 0 16px; font-size: 20px; letter-spacing: 1px; }

.toolbar-row {
  width: 100%;
  max-width: 900px;
  margin-bottom: 12px;
  display: flex;
  justify-content: flex-end;
}

.main-layout {
  display: flex;
  gap: 16px;
  width: 100%;
  max-width: 900px;
  align-items: flex-start;
}

.canvas-area { flex: 1; }

.side-panel {
  width: 220px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.status-bar {
  margin-top: 12px;
  padding: 8px 20px;
  background: #1e3a1e;
  color: #98c379;
  border-radius: 4px;
  font-size: 13px;
}
</style>
