<template>
  <div class="inspector">
    <h3>检查器</h3>
    <div v-if="selectedItem">
      <p><strong>名称：</strong>{{ selectedItem.name }}</p>
      <p><strong>类型：</strong>{{ selectedItem.type }}</p>
      <p><strong>坐标：</strong>({{ selectedItem.x }}, {{ selectedItem.y }})</p>

      <div class="btn-row">
        <button @click="triggerStory" :disabled="storyLoading">
          {{ storyLoading ? '生成中…' : '查看故事' }}
        </button>
        <button @click="$emit('rotate-item', selectedItem)">旋转</button>
        <button class="danger" @click="$emit('delete-item', selectedItem)">删除</button>
      </div>

      <!-- 故事面板 -->
      <transition name="fade">
        <div v-if="storyVisible" class="story-panel">
          <div v-if="storyStatus === 'DONE'" class="story-text">{{ storyText }}</div>
          <div v-else-if="storyStatus === 'FAILED'" class="story-error">
            叙事生成失败：{{ storyError }}
          </div>
          <div v-else class="story-pending">
            <span class="spinner" /> 正在召唤档案员…
          </div>
        </div>
      </transition>
    </div>
    <div v-else class="empty">未选中物品</div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { getItemStory, type ItemStoryResult } from "../../api/game";

const props = defineProps<{
  selectedItem: { id: string; name: string; type: string; x: number; y: number } | null;
  sessionId: string;
}>();

defineEmits<{
  (e: "view-story", item: unknown): void;
  (e: "rotate-item", item: unknown): void;
  (e: "delete-item", item: unknown): void;
}>();

// ── 故事状态 ──────────────────────────────────────────────────────────────
const storyVisible = ref(false);
const storyLoading = ref(false);
const storyStatus = ref<ItemStoryResult["status"] | "">("");
const storyText   = ref<string | null>(null);
const storyError  = ref<string | null>(null);

// 切换物品时重置故事面板
watch(() => props.selectedItem?.id, () => {
  storyVisible.value = false;
  storyLoading.value = false;
  storyStatus.value = "";
  storyText.value = null;
  storyError.value = null;
});

let pollTimer: ReturnType<typeof setTimeout> | null = null;
const MAX_POLLS = 15;          // 最多轮询 15 次 × 2s = 30s
const POLL_INTERVAL_MS = 2000;

async function triggerStory() {
  if (!props.selectedItem || !props.sessionId) return;
  storyVisible.value = true;
  storyLoading.value = true;
  storyStatus.value = "PENDING";
  storyText.value = null;
  storyError.value = null;
  if (pollTimer) clearTimeout(pollTimer);

  await poll(0);
}

async function poll(count: number) {
  if (!props.selectedItem) return;
  try {
    const res = await getItemStory(
      props.sessionId,
      props.selectedItem.id,
      props.selectedItem.type,
    );
    const d = res.data;
    storyStatus.value = d.status;

    if (d.status === "DONE") {
      storyText.value = d.story;
      storyLoading.value = false;
      return;
    }
    if (d.status === "FAILED") {
      storyError.value = d.errorMessage ?? "未知错误";
      storyLoading.value = false;
      return;
    }
    // PENDING / RUNNING — 继续轮询
    if (count < MAX_POLLS) {
      pollTimer = setTimeout(() => poll(count + 1), POLL_INTERVAL_MS);
    } else {
      storyError.value = "生成超时，请稍后重试";
      storyStatus.value = "FAILED";
      storyLoading.value = false;
    }
  } catch (e) {
    storyError.value = "网络错误，请重试";
    storyStatus.value = "FAILED";
    storyLoading.value = false;
  }
}
</script>

<style scoped>
.inspector {
  padding: 12px;
  background: #1a1a2e;
  border: 1px solid #333;
  border-radius: 6px;
  color: #ccc;
  font-size: 14px;
}
h3 { color: #e0c97f; margin: 0 0 8px; }
.empty { color: #666; font-style: italic; }
.btn-row { display: flex; gap: 6px; flex-wrap: wrap; margin: 8px 0; }
button {
  padding: 5px 12px;
  background: #2a5298;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}
button:hover:not(:disabled) { background: #3a63b8; }
button:disabled { opacity: 0.5; cursor: not-allowed; }
button.danger { background: #7b2020; }
button.danger:hover { background: #a33030; }

.story-panel {
  margin-top: 10px;
  padding: 10px;
  background: #111;
  border-radius: 4px;
  border: 1px solid #2c2c3a;
  min-height: 60px;
}
.story-text { line-height: 1.7; color: #d4c9a8; white-space: pre-wrap; }
.story-error { color: #e06c75; }
.story-pending { color: #888; display: flex; align-items: center; gap: 8px; }

/* Spinner */
.spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid #555;
  border-top-color: #e0c97f;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Fade transition */
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
