<template>
  <div class="rec-panel">
    <!-- 触发按钮（面板关闭状态） -->
    <button v-if="!visible" class="btn-trigger" @click="fetch" :disabled="loading">
      {{ loading ? '分析中…' : '✨ 获取布局推荐' }}
    </button>

    <!-- 推荐面板 -->
    <transition name="slide">
      <div v-if="visible && recommendation" class="panel-body">
        <div class="panel-header">
          <span class="panel-title">🗺 布局推荐</span>
          <span class="conf-badge" :class="confClass">
            置信度 {{ Math.round(recommendation.confidence * 100) }}%
          </span>
        </div>

        <p class="reason">{{ recommendation.reason }}</p>

        <!-- 迷你预览画布 -->
        <div class="preview-wrap">
          <canvas ref="previewCanvas" :width="previewW" :height="previewH" />
        </div>

        <!-- 物品列表 -->
        <ul class="item-list">
          <li v-for="item in recommendation.items" :key="item.id">
            <span class="item-type">{{ item.type }}</span>
            <span class="item-pos">({{ item.x }}, {{ item.y }})</span>
          </li>
        </ul>

        <div v-if="errorMsg" class="error">{{ errorMsg }}</div>

        <div class="btn-row">
          <button class="btn-accept" @click="accept" :disabled="accepting">
            {{ accepting ? '应用中…' : '✅ 接受' }}
          </button>
          <button class="btn-reject" @click="reject">❌ 拒绝</button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from "vue";
import {
  getRecommendation,
  acceptRecommendation,
  rejectRecommendation,
  type LayoutRecommendation,
} from "../../api/game";

const props = defineProps<{
  sessionId: string;
  currentVersion: number;
}>();

const emit = defineEmits<{
  (e: "applied", newVersion: number, stateData: string): void;
}>();

// ── state ──────────────────────────────────────────────────────────────────
const visible         = ref(false);
const loading         = ref(false);
const accepting       = ref(false);
const errorMsg        = ref<string | null>(null);
const recommendation  = ref<LayoutRecommendation | null>(null);
const previewCanvas   = ref<HTMLCanvasElement | null>(null);

// 迷你预览画布尺寸（1:1 缩小到 200×150）
const previewW = 200;
const previewH = 150;
const SCALE_X  = previewW / 640;  // 原始画布假设 640×480
const SCALE_Y  = previewH / 480;

const TYPE_COLORS: Record<string, string> = {
  bed:      "#6a8fd8",
  window:   "#82d1d1",
  table:    "#c9a96e",
  medkit:   "#e06c75",
  axe:      "#888",
  weapon:   "#888",
  supply:   "#98c379",
  food:     "#98c379",
  default:  "#7d7d7d",
};

const confClass = computed(() => {
  const c = recommendation.value?.confidence ?? 0;
  if (c >= 0.7) return "conf-high";
  if (c >= 0.4) return "conf-mid";
  return "conf-low";
});

// 每次推荐更新时重绘预览
watch(recommendation, () => nextTick(drawPreview));

// ── methods ─────────────────────────────────────────────────────────────────
async function fetch() {
  if (!props.sessionId) return;
  loading.value  = true;
  errorMsg.value = null;
  try {
    recommendation.value = await getRecommendation(props.sessionId);
    visible.value = true;
  } catch (e: any) {
    errorMsg.value = e?.message ?? "获取推荐失败";
  } finally {
    loading.value = false;
  }
}

async function accept() {
  if (!recommendation.value || !props.sessionId) return;
  accepting.value = true;
  errorMsg.value  = null;
  try {
    const result = await acceptRecommendation(
      props.sessionId,
      recommendation.value.recommendationId,
      props.currentVersion,
    );
    if (result.conflict) {
      errorMsg.value = "版本冲突，请刷新后重试（" + (result.conflictMessage ?? "") + "）";
    } else {
      emit("applied", result.newVersion, result.stateData);
      close();
    }
  } catch (e: any) {
    errorMsg.value = e?.message ?? "应用推荐失败";
  } finally {
    accepting.value = false;
  }
}

async function reject() {
  if (!recommendation.value || !props.sessionId) return;
  try {
    await rejectRecommendation(props.sessionId, recommendation.value.recommendationId);
  } finally {
    close();
  }
}

function close() {
  visible.value        = false;
  recommendation.value = null;
  errorMsg.value       = null;
}

// ── Preview Canvas ──────────────────────────────────────────────────────────
function drawPreview() {
  const canvas = previewCanvas.value;
  if (!canvas || !recommendation.value) return;
  const ctx = canvas.getContext("2d");
  if (!ctx) return;

  ctx.clearRect(0, 0, previewW, previewH);

  // 背景
  ctx.fillStyle = "#111";
  ctx.fillRect(0, 0, previewW, previewH);

  // 网格线
  ctx.strokeStyle = "#222";
  ctx.lineWidth = 0.5;
  const gs = 32 * SCALE_X;
  for (let x = 0; x < previewW; x += gs) {
    ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, previewH); ctx.stroke();
  }
  for (let y = 0; y < previewH; y += gs * (SCALE_Y / SCALE_X)) {
    ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(previewW, y); ctx.stroke();
  }

  // 物品方块
  for (const item of recommendation.value.items) {
    const rx = item.x * SCALE_X;
    const ry = item.y * SCALE_Y;
    const rw = Math.max(item.w * SCALE_X, 10);
    const rh = Math.max(item.h * SCALE_Y, 8);

    const color = TYPE_COLORS[item.type?.toLowerCase()] ?? TYPE_COLORS.default;
    ctx.fillStyle = color;
    ctx.globalAlpha = 0.85;
    ctx.fillRect(rx, ry, rw, rh);
    ctx.globalAlpha = 1.0;

    // 物品类型文字
    ctx.fillStyle = "#fff";
    ctx.font = `${Math.max(7, 9 * SCALE_X * 3)}px monospace`;
    ctx.fillText(item.type?.slice(0, 4) ?? "?", rx + 2, ry + rh - 2);
  }
}
</script>

<style scoped>
.rec-panel { position: relative; }

.btn-trigger {
  padding: 7px 16px;
  background: #2a5298;
  color: #fff;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 13px;
  letter-spacing: 0.3px;
}
.btn-trigger:hover:not(:disabled) { background: #3a63b8; }
.btn-trigger:disabled { opacity: 0.5; cursor: not-allowed; }

.panel-body {
  background: #1a1a2e;
  border: 1px solid #333;
  border-radius: 8px;
  padding: 14px;
  color: #ccc;
  min-width: 220px;
  font-size: 13px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.panel-title { font-weight: bold; color: #e0c97f; }

.conf-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: bold;
}
.conf-high { background: #2d6a2d; color: #98c379; }
.conf-mid  { background: #6a5a2d; color: #e5c07b; }
.conf-low  { background: #5a2d2d; color: #e06c75; }

.reason {
  font-style: italic;
  color: #888;
  font-size: 12px;
  margin: 0 0 10px;
  line-height: 1.5;
}

.preview-wrap {
  margin: 8px 0;
  border: 1px solid #333;
  border-radius: 4px;
  overflow: hidden;
  line-height: 0;
}
canvas { display: block; }

.item-list {
  list-style: none;
  padding: 0;
  margin: 8px 0;
  max-height: 100px;
  overflow-y: auto;
}
.item-list li {
  display: flex;
  justify-content: space-between;
  padding: 2px 0;
  border-bottom: 1px solid #2a2a3e;
  font-size: 12px;
}
.item-type { color: #d4c9a8; }
.item-pos  { color: #555; font-size: 11px; }

.error { color: #e06c75; font-size: 12px; margin: 6px 0; }

.btn-row { display: flex; gap: 8px; margin-top: 10px; }
.btn-accept {
  flex: 1;
  padding: 6px;
  background: #2d6a2d;
  color: #98c379;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}
.btn-accept:hover:not(:disabled) { background: #3d8a3d; }
.btn-accept:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-reject {
  flex: 1;
  padding: 6px;
  background: #5a2d2d;
  color: #e06c75;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}
.btn-reject:hover { background: #7a3d3d; }

/* Slide transition */
.slide-enter-active, .slide-leave-active {
  transition: all 0.3s ease;
}
.slide-enter-from, .slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
