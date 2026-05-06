<script setup lang="ts">
import { computed, ref } from "vue";
import type { DiaryEntryView, DiaryLevel } from "../../types/api";

const props = defineProps<{
  loading: boolean;
  entries: DiaryEntryView[];
  error?: string;
}>();

const emit = defineEmits<{
  refresh: [level: DiaryLevel];
}>();

const level = ref<DiaryLevel>("L0");

const tabs: DiaryLevel[] = ["L0", "L1", "L2"];

const visible = computed(() =>
  props.entries.slice().sort((a, b) => b.toTurn - a.toTurn || b.timestamp - a.timestamp),
);

function switchLevel(next: DiaryLevel) {
  if (next === level.value) {
    return;
  }
  level.value = next;
  emit("refresh", next);
}

function label(levelName: DiaryLevel) {
  if (levelName === "L0") return "即时记忆";
  if (levelName === "L1") return "阶段摘要";
  return "长期归档";
}

function toTime(ts: number) {
  const value = Number.isFinite(ts) ? ts : Date.now();
  return new Date(value).toLocaleString();
}
</script>

<template>
  <section class="panel diary-panel">
    <header class="diary-head">
      <h3>游戏日记</h3>
      <div class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab"
          class="tab"
          :class="{ active: tab === level }"
          @click="switchLevel(tab)"
          type="button"
        >
          {{ tab }} · {{ label(tab) }}
        </button>
      </div>
    </header>

    <p class="hint" v-if="loading">正在加载日记...</p>
    <p class="hint error" v-else-if="error">{{ error }}</p>
    <ul class="list" v-else-if="visible.length">
      <li class="item" v-for="row in visible" :key="`${row.level}-${row.fromTurn}-${row.toTurn}-${row.timestamp}`">
        <p class="meta">{{ row.level }} · 回合 {{ row.fromTurn }}-{{ row.toTurn }} · {{ toTime(row.timestamp) }}</p>
        <p class="summary">{{ row.summary }}</p>
        <p class="tags" v-if="row.tags.length"># {{ row.tags.join("  #") }}</p>
      </li>
    </ul>
    <p class="hint" v-else>该层级暂无日记记录</p>
  </section>
</template>

<style scoped>
.diary-panel {
  padding: 14px;
  display: grid;
  gap: 10px;
}

.diary-head {
  display: grid;
  gap: 10px;
}

h3 {
  margin: 0;
  font-size: 18px;
}

.tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tab {
  border: 1px solid var(--line-02);
  background: rgba(255, 255, 255, 0.03);
  color: var(--text-02);
  border-radius: 999px;
  font-size: 12px;
  padding: 4px 10px;
  cursor: pointer;
}

.tab.active {
  border-color: var(--accent);
  color: var(--accent);
}

.list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 8px;
}

.item {
  border: 1px solid var(--line-02);
  border-radius: 10px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.02);
}

.meta {
  margin: 0;
  color: var(--text-03);
  font-size: 12px;
}

.summary {
  margin: 6px 0 0;
  line-height: 1.45;
}

.tags {
  margin: 8px 0 0;
  color: var(--text-03);
  font-size: 12px;
}

.hint {
  margin: 0;
  color: var(--text-03);
  font-size: 13px;
}

.hint.error {
  color: var(--danger);
}
</style>
