<script setup lang="ts">
import { computed, ref } from "vue";
import type { SessionState } from "../../types/api";
import { restCabin } from "../../api/game";

const props = defineProps<{
  state: SessionState | null;
}>();

const challengePct = computed(() => {
  if (!props.state) {
    return 0;
  }
  return Math.max(0, Math.min(100, Math.round(props.state.challengeIndex * 100)));
});

const isResting = ref(false);
const updated = ref<{ stamina: number; time: string } | null>(null);

const displayStamina = computed(() => {
  if (updated.value) return updated.value.stamina;
  return props.state ? props.state.stamina : 0;
});

const displayTimeLabel = computed(() => {
  if (updated.value) return updated.value.time;
  return props.state ? props.state.timePhaseLabel : "-";
});

async function doRest() {
  if (!props.state) return;
  isResting.value = true;
  try {
    // restCabin 现在直接返回解包后的数据对象
    const data = await restCabin(props.state.sessionId, 1, "afternoon");
    updated.value = { stamina: data.updatedStamina, time: data.updatedTimeOfDay };
  } catch (e) {
    console.error("rest failed", e);
  } finally {
    isResting.value = false;
  }
}
</script>

<template>
  <aside class="panel status-panel">
    <h2 class="panel-title">生存面板</h2>
    <div class="metrics" v-if="state">
      <div class="metric-card">
        <span class="metric-label">HP</span>
        <strong class="metric-value">{{ state.hp }}</strong>
      </div>
      <div class="metric-card">
        <span class="metric-label">体力</span>
        <strong class="metric-value">{{ displayStamina }}</strong>
      </div>
      <div class="metric-card">
        <span class="metric-label">感染</span>
        <strong class="metric-value">{{ state.infection }}</strong>
      </div>
      <div class="metric-card">
        <span class="metric-label">回合</span>
        <strong class="metric-value">T{{ state.turn }}</strong>
      </div>
      <div class="metric-card">
        <span class="metric-label">日期进度</span>
        <strong class="metric-value">D{{ state.dayIndex }} · {{ state.turnInDay }}/{{ state.turnsPerDayTarget }}</strong>
      </div>
      <div class="metric-card metric-card--wide">
        <span class="metric-label">时段</span>
        <strong class="metric-value">{{ displayTimeLabel }}</strong>
      </div>
    </div>

    <div class="challenge" v-if="state">
      <div class="challenge-head">
        <span>挑战指数</span>
        <span class="mono">{{ challengePct }}%</span>
      </div>
      <div class="bar-track">
        <div class="bar-fill" :style="{ width: `${challengePct}%` }"></div>
      </div>
      <p class="meta">目标区间: {{ state.challengeBand[0] }} - {{ state.challengeBand[1] }}</p>
    </div>

    <div class="inventory" v-if="state">
      <h3>背包</h3>
      <div class="bag-tags">
        <span class="tag" v-for="item in state.inventory" :key="item">{{ item }}</span>
      </div>
    </div>

    <div class="actions" v-if="state">
      <button class="btn" @click="doRest" :disabled="isResting">{{ isResting ? '休息中...' : '休息 (+1h)' }}</button>
      <p class="meta" v-if="updated">休息完成：体力 {{ updated.stamina }}，时段 {{ updated.time }}</p>
    </div>

    <p class="meta" v-else>状态尚未加载。</p>
  </aside>
</template>

<style scoped>
.status-panel {
  padding: 20px;
  display: grid;
  gap: 16px;
}

.panel-title {
  margin: 0;
  font-size: 22px;
  letter-spacing: 0.04em;
  font-family: var(--font-display);
}

.metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.metric-card--wide {
  grid-column: span 2;
}

.metric-card {
  border: 1px solid var(--line-soft);
  border-radius: 12px;
  padding: 10px;
  background: rgba(6, 9, 14, 0.5);
}

.metric-label {
  font-size: 12px;
  color: var(--text-03);
}

.metric-value {
  display: block;
  margin-top: 6px;
  font-family: var(--font-mono);
  font-size: 20px;
}

.challenge {
  border: 1px solid var(--line-soft);
  border-radius: 12px;
  padding: 12px;
  background: rgba(6, 9, 14, 0.5);
}

.challenge-head {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 8px;
}

.bar-track {
  height: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #5b7fe8, #ff6a3d);
}

.bag-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.meta {
  margin: 0;
  font-size: 12px;
  color: var(--text-03);
}

.mono {
  font-family: var(--font-mono);
}
</style>
