<template>
  <div class="cabin-demo-page">
    <h1>末日小屋</h1>

    <!-- 顶部工具栏区 -->
    <div class="toolbar-row">
      <!-- AI 生成背景 -->
      <button
        class="btn-gen-bg"
        :disabled="genLoading"
        @click="generateBg"
      >
        <span v-if="genLoading" class="spinner">⏳</span>
        <span v-else>✨ 生成背景</span>
        <span v-if="genLoading" class="gen-status">{{ genStatusText }}</span>
      </button>

      <RecommendationPanel
        :sessionId="sessionId"
        :currentVersion="currentVersion"
        @applied="onRecommendationApplied"
      />
    </div>

    <div class="main-layout">
      <!-- 左：画布 -->
      <div class="canvas-area">
        <CabinCanvas
          :width="640"
          :height="480"
          :initialItems="canvasItems"
          :bgImage="cabinBgImage"
          :showGrid="showGrid"
          ref="canvasRef"
        />
        <div class="canvas-toolbar">
          <label class="grid-toggle">
            <input type="checkbox" v-model="showGrid" /> 显示网格
          </label>
        </div>
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
import { submitImageGen, queryImageGen } from "@/api/game";

interface TrayItem { id: string; name: string; type: string; image: string; x: number; y: number; }

// ── Demo 数据（实际项目中从路由参数或 store 获取） ─────────────────────────
const sessionId      = ref("demo-session-001");
const currentVersion = ref(0);
const statusMsg      = ref<string | null>(null);
const canvasRef      = ref<InstanceType<typeof CabinCanvas> | null>(null);
const showGrid       = ref(true);

// 背景图：放置于 public/assets/cabin-bg.jpg（AI 生成后替换）
const cabinBgImage = ref('/assets/cabin-bg.jpg');

const canvasItems = ref<CabinItem[]>([]);

const trayItems = ref<TrayItem[]>([
  { id: "item-bed-01",    name: "床",     type: "bed",    image: "/assets/items/bed.png",    x: 0, y: 0 },
  { id: "item-table-01",  name: "桌子",   type: "table",  image: "/assets/items/table.png",  x: 0, y: 0 },
  { id: "item-axe-01",    name: "斧头",   type: "axe",    image: "/assets/items/axe.png",    x: 0, y: 0 },
  { id: "item-medkit-01", name: "医疗包", type: "medkit", image: "/assets/items/medkit.png", x: 0, y: 0 },
  { id: "item-window-01", name: "窗户",   type: "window", image: "/assets/items/window.png", x: 0, y: 0 },
]);

const selectedItem = ref<TrayItem | null>(null);

// ── AI 背景生成 ──────────────────────────────────────────────────────────
const genLoading    = ref(false);
const genStatusText = ref("");

async function generateBg() {
  genLoading.value    = true;
  genStatusText.value = "提交中…";
  try {
    const res = await submitImageGen("", "cabin_bg");
    // 若缓存命中，直接使用返回的 imageUrl
    if (res.status === "SUCCEEDED" && res.imageUrl) {
      cabinBgImage.value  = res.imageUrl;
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
          cabinBgImage.value  = r.imageUrl;
          genLoading.value    = false;
          showStatus("✅ 背景图已生成！");
        } else if (r.status === "FAILED" || tries >= 20) {
          clearInterval(poll);
          genLoading.value    = false;
          showStatus(r.status === "FAILED" ? "❌ 生成失败：" + (r.message ?? "") : "⏱ 超时，稍后重试");
        } else {
          genStatusText.value = `生成中… (${tries * 3}s)`;
        }
      } catch {
        clearInterval(poll);
        genLoading.value = false;
        showStatus("❌ 轮询出错");
      }
    }, 3000);
  } catch (e: unknown) {
    showStatus("❌ 提交失败：" + (e instanceof Error ? e.message : String(e)));
    genLoading.value = false;
  }
}

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
  background:
    radial-gradient(ellipse at 30% 20%, rgba(60, 40, 10, 0.35) 0%, transparent 55%),
    radial-gradient(ellipse at 80% 80%, rgba(10, 30, 50, 0.4) 0%, transparent 55%),
    #0a0c14;
  min-height: 100vh;
  color: #ccc;
}

h1 {
  color: #e0c97f;
  margin: 0 0 16px;
  font-size: 22px;
  letter-spacing: 3px;
  text-transform: uppercase;
  text-shadow: 0 0 18px rgba(224, 201, 127, 0.4);
}

.btn-gen-bg {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 6px;
  border: 1px solid rgba(224, 201, 127, 0.5);
  background: rgba(224, 201, 127, 0.08);
  color: #e0c97f;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}
.btn-gen-bg:hover:not(:disabled) {
  background: rgba(224, 201, 127, 0.18);
  border-color: #e0c97f;
}
.btn-gen-bg:disabled {
  opacity: 0.6;
  cursor: default;
}
.gen-status {
  font-size: 11px;
  color: #999;
  margin-left: 4px;
}

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

.canvas-toolbar {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.grid-toggle {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #888;
  cursor: pointer;
  user-select: none;
}

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
