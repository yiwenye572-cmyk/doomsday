<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { chooseOption, getSessionState, submitTurn, useComebackCard } from "../api/game";
import type { OptionPayload, PlotPayload, SessionState } from "../types/api";
import ActionInput from "../components/game/ActionInput.vue";
import NarrativePanel from "../components/game/NarrativePanel.vue";
import OptionsGrid from "../components/game/OptionsGrid.vue";
import StatusPanel from "../components/game/StatusPanel.vue";

const route = useRoute();
const router = useRouter();

const sessionId = computed(() => String(route.params.sessionId || ""));

const state = ref<SessionState | null>(null);
const plot = ref<PlotPayload | null>(null);
const options = ref<OptionPayload[]>([]);
const loading = ref(false);
const pendingAction = ref(false);
const notice = ref("");

const headline = computed(() => `会话 ${sessionId.value}`);

async function refreshState() {
  if (!sessionId.value) {
    return;
  }
  state.value = await getSessionState(sessionId.value);
}

async function handleSubmit(input: string) {
  if (!state.value || pendingAction.value) {
    return;
  }
  pendingAction.value = true;
  notice.value = "";
  try {
    const data = await submitTurn(sessionId.value, {
      expectedVersion: state.value.version,
      playerInput: input,
      clientTime: Date.now(),
    });
    plot.value = data.plot;
    options.value = data.options;
    await refreshState();
  } catch (e) {
    await handleError(e);
  } finally {
    pendingAction.value = false;
  }
}

async function handleChoose(optionId: string) {
  if (!state.value || pendingAction.value) {
    return;
  }
  pendingAction.value = true;
  notice.value = "";
  try {
    const turn = state.value.turn;
    await chooseOption(sessionId.value, turn, {
      expectedVersion: state.value.version,
      optionId,
    });
    await refreshState();
  } catch (e) {
    await handleError(e);
  } finally {
    pendingAction.value = false;
  }
}

async function handleComeback() {
  if (!state.value || pendingAction.value) {
    return;
  }
  pendingAction.value = true;
  notice.value = "";
  try {
    const result = await useComebackCard(sessionId.value, {
      expectedVersion: state.value.version,
    });
    notice.value = result.applied ? "翻盘卡已生效" : "翻盘卡未生效";
    await refreshState();
  } catch (e) {
    await handleError(e);
  } finally {
    pendingAction.value = false;
  }
}

async function handleError(error: unknown) {
  const message = error instanceof Error ? error.message : "请求失败";
  notice.value = message;
  if (message.toLowerCase().includes("version")) {
    await refreshState();
  }
}

async function init() {
  loading.value = true;
  notice.value = "";
  try {
    await refreshState();
  } catch (e) {
    notice.value = e instanceof Error ? e.message : "状态加载失败";
  } finally {
    loading.value = false;
  }
}

function goReplay() {
  router.push(`/replay/${sessionId.value}`);
}

onMounted(init);
</script>

<template>
  <main class="page-wrap game-page">
    <header class="panel topbar">
      <div>
        <p class="meta">Ruin Rain Session</p>
        <h1>{{ headline }}</h1>
      </div>
      <div class="top-actions">
        <button class="btn" @click="router.push('/')">新会话</button>
        <button class="btn" @click="goReplay">回放</button>
        <button class="btn btn--admin" @click="router.push('/admin')">Admin</button>
      </div>
    </header>

    <section class="layout" v-if="!loading">
      <div class="main-col">
        <NarrativePanel :plot="plot" />
        <ActionInput :loading="pendingAction" @submit="handleSubmit" @comeback="handleComeback" />
        <OptionsGrid :options="options" :loading="pendingAction" @choose="handleChoose" />
      </div>
      <StatusPanel :state="state" />
    </section>

    <section class="panel loading-box" v-else>正在加载会话状态...</section>

    <p class="notice processing" v-if="pendingAction && !notice">
      AI 正在生成剧情中，请稍候（约 15-30 秒）…
    </p>
    <p class="notice" v-if="notice">{{ notice }}</p>
  </main>
</template>

<style scoped>
.game-page {
  display: grid;
  gap: 14px;
}

.topbar {
  padding: 14px 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

h1 {
  margin: 2px 0 0;
  font-size: 28px;
  font-family: var(--font-display);
  letter-spacing: 0.04em;
}

.meta {
  margin: 0;
  color: var(--text-03);
  font-family: var(--font-mono);
  font-size: 12px;
}

.top-actions {
  display: flex;
  gap: 8px;
}

.layout {
  display: grid;
  grid-template-columns: minmax(0, 7fr) minmax(280px, 5fr);
  gap: 14px;
  align-items: start;
}

.main-col {
  display: grid;
  gap: 12px;
}

.loading-box {
  padding: 20px;
}

.notice {
  margin: 0;
  font-size: 13px;
  color: var(--danger);
}

.notice.processing {
  color: var(--text-03);
}

.btn--admin {
  opacity: 0.7;
  font-size: 11px;
}

@media (max-width: 1199px) {
  .layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 767px) {
  h1 {
    font-size: 22px;
  }

  .topbar {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }
}
</style>
