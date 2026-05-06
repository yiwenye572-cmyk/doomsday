<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { getDefaultWorld, getWorldFactoryJob, initializeWorld } from "../api/world";
import type { GameWorldInitRequest, WorldFactoryJobResponse } from "../types/api";

const router = useRouter();

const loading = ref(false);
const polling = ref(false);
const error = ref("");
const notice = ref("");
const defaultWorldVersion = ref("world_v1");
const lastJob = ref<WorldFactoryJobResponse | null>(null);

const form = reactive<GameWorldInitRequest>({
  worldTheme: "废土科技衰败都市",
  eraStyle: "近未来崩坏",
  survivalTone: "高压求生",
  keyFaction: "拾荒者联盟",
  forbiddenRule: "禁止直接设定超自然无代价复活",
});

let pollTimer: number | null = null;

const worldVersionCandidate = computed(() => {
  if (lastJob.value?.worldVersion && lastJob.value.worldVersion.length > 0) {
    return lastJob.value.worldVersion;
  }
  return defaultWorldVersion.value;
});

function clearPollTimer() {
  if (pollTimer !== null) {
    window.clearInterval(pollTimer);
    pollTimer = null;
  }
}

function savePreferredWorldVersion(worldVersion: string) {
  localStorage.setItem("doomsday:preferredWorldVersion", worldVersion);
}

async function loadDefault() {
  try {
    const data = await getDefaultWorld();
    defaultWorldVersion.value = data.worldVersion;
  } catch (e) {
    defaultWorldVersion.value = "world_v1";
  }
}

async function pollJob(jobId: string) {
  try {
    const data = await getWorldFactoryJob(jobId);
    lastJob.value = data;
    if (data.status === "DONE") {
      savePreferredWorldVersion(data.worldVersion);
      notice.value = `世界生成完成，已切换为 ${data.worldVersion}`;
      polling.value = false;
      clearPollTimer();
    }
    if (data.status === "FAILED") {
      polling.value = false;
      clearPollTimer();
      error.value = data.errorMessage || "世界工厂任务失败";
    }
  } catch (e) {
    polling.value = false;
    clearPollTimer();
    error.value = e instanceof Error ? e.message : "查询任务状态失败";
  }
}

async function startInitialize() {
  if (loading.value || polling.value) {
    return;
  }
  loading.value = true;
  error.value = "";
  notice.value = "";
  clearPollTimer();

  try {
    const data = await initializeWorld(form);
    notice.value = `任务已提交：${data.jobId}`;
    polling.value = true;
    await pollJob(data.jobId);
    if (polling.value) {
      pollTimer = window.setInterval(() => {
        pollJob(data.jobId).catch(() => {});
      }, 2500);
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : "提交世界初始化任务失败";
  } finally {
    loading.value = false;
  }
}

function applyDefaultAndBack() {
  savePreferredWorldVersion(defaultWorldVersion.value);
  router.push("/");
}

function useCurrentAndBack() {
  savePreferredWorldVersion(worldVersionCandidate.value);
  router.push("/");
}

onMounted(() => {
  loadDefault().catch(() => {});
});

onBeforeUnmount(() => {
  clearPollTimer();
});
</script>

<template>
  <main class="page-wrap world-page">
    <header class="panel topbar">
      <div>
        <p class="meta">WORLD FACTORY</p>
        <h1>世界观初始化</h1>
      </div>
      <div class="actions">
        <button class="btn" @click="router.push('/')">返回开局</button>
        <button class="btn btn--accent" @click="useCurrentAndBack">使用当前版本</button>
      </div>
    </header>

    <section class="layout">
      <section class="panel block form-block">
        <h2>基础设定生成世界书</h2>

        <label class="field">
          <span>世界主题</span>
          <input class="input" v-model="form.worldTheme" maxlength="64" />
        </label>

        <label class="field">
          <span>时代风格</span>
          <input class="input" v-model="form.eraStyle" maxlength="64" />
        </label>

        <label class="field">
          <span>生存基调</span>
          <input class="input" v-model="form.survivalTone" maxlength="64" />
        </label>

        <label class="field">
          <span>关键势力（可选）</span>
          <input class="input" v-model="form.keyFaction" maxlength="64" />
        </label>

        <label class="field">
          <span>禁用规则（可选）</span>
          <input class="input" v-model="form.forbiddenRule" maxlength="120" />
        </label>

        <div class="submit-row">
          <button class="btn btn--accent" :disabled="loading || polling" @click="startInitialize">
            {{ loading ? "提交中..." : polling ? "生成中..." : "提交初始化任务" }}
          </button>
        </div>
      </section>

      <section class="panel block status-block">
        <h2>世界版本状态</h2>

        <div class="kv">
          <span>默认世界</span>
          <strong>{{ defaultWorldVersion }}</strong>
        </div>

        <div class="kv">
          <span>当前候选</span>
          <strong>{{ worldVersionCandidate }}</strong>
        </div>

        <div class="kv" v-if="lastJob">
          <span>最近任务</span>
          <strong>{{ lastJob.jobId }}</strong>
        </div>

        <div class="kv" v-if="lastJob">
          <span>状态</span>
          <strong>{{ lastJob.status }} ({{ lastJob.progress }}%)</strong>
        </div>

        <p class="hint" v-if="lastJob?.stage">阶段：{{ lastJob.stage }}</p>
        <p class="notice" v-if="notice">{{ notice }}</p>
        <p class="error" v-if="error">{{ error }}</p>

        <div class="status-actions">
          <button class="btn" @click="applyDefaultAndBack">使用默认世界并返回</button>
          <button class="btn btn--accent" @click="useCurrentAndBack">使用当前候选并返回</button>
        </div>
      </section>
    </section>
  </main>
</template>

<style scoped>
.world-page {
  display: grid;
  gap: 14px;
}

.topbar {
  padding: 14px 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.meta {
  margin: 0;
  color: var(--text-03);
  font-family: var(--font-mono);
  font-size: 12px;
}

h1 {
  margin: 2px 0 0;
  font-size: 28px;
  font-family: var(--font-display);
}

.layout {
  display: grid;
  grid-template-columns: minmax(0, 7fr) minmax(280px, 5fr);
  gap: 14px;
}

.block {
  padding: 18px;
}

h2 {
  margin: 0 0 14px;
  font-size: 18px;
  font-family: var(--font-display);
}

.field {
  display: grid;
  gap: 8px;
  margin-bottom: 12px;
}

.field span {
  color: var(--text-03);
  font-size: 13px;
}

.submit-row {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}

.kv {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--line-soft);
  padding: 8px 0;
}

.kv span {
  color: var(--text-03);
}

.kv strong {
  font-family: var(--font-mono);
  font-size: 12px;
}

.notice {
  margin: 10px 0 0;
  color: var(--ok);
  font-size: 13px;
}

.error {
  margin: 8px 0 0;
  color: var(--danger);
  font-size: 13px;
}

.hint {
  color: var(--text-03);
  font-size: 12px;
  margin-top: 8px;
}

.actions,
.status-actions {
  display: flex;
  gap: 8px;
}

.status-actions {
  margin-top: 14px;
  flex-wrap: wrap;
}

@media (max-width: 1024px) {
  .layout {
    grid-template-columns: 1fr;
  }

  .topbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
