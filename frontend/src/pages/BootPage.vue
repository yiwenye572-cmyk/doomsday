<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { createSession } from "../api/game";
import { getDefaultWorld } from "../api/world";
import type { CreateSessionRequest } from "../types/api";

const router = useRouter();
const loading = ref(false);
const error = ref("");

const form = reactive<CreateSessionRequest>({
  playerId: "u_1001",
  difficulty: "SURVIVOR",
  worldVersion: "world_v1",
  styleProfile: "grim_realism",
});

const difficultyOptions: Array<{ value: CreateSessionRequest["difficulty"]; title: string; desc: string }> = [
  { value: "SEEKER", title: "求生", desc: "压力可控，恢复窗口更友好。" },
  { value: "SURVIVOR", title: "幸存", desc: "标准废土体验，风险与收益均衡。" },
  { value: "HELL", title: "地狱", desc: "资源稀缺且高压频发，容错极低。" },
];

async function loadWorldVersion() {
  const preferred = localStorage.getItem("doomsday:preferredWorldVersion");
  if (preferred && preferred.trim().length > 0) {
    form.worldVersion = preferred.trim();
    return;
  }
  try {
    const data = await getDefaultWorld();
    form.worldVersion = data.worldVersion;
  } catch {
    form.worldVersion = "world_v1";
  }
}

function goWorldFactory() {
  router.push("/world-factory");
}

loadWorldVersion().catch(() => {});

async function startGame() {
  if (loading.value) {
    return;
  }
  loading.value = true;
  error.value = "";
  try {
    const data = await createSession(form);
    localStorage.setItem("doomsday:lastSessionId", data.sessionId);
    await router.push(`/game/${data.sessionId}`);
  } catch (e) {
    error.value = e instanceof Error ? e.message : "创建会话失败";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="page-wrap boot-page">
    <section class="panel hero">
      <p class="meta-line">DOOMSDAY / NARRATIVE / SURVIVE</p>
      <h1>废墟雨夜</h1>
      <p class="subtitle">
        你将从避难所踏入一座缓慢腐烂的城市。每一次决策都可能换来一小时生机，也可能带来不可逆的崩塌。
      </p>
    </section>

    <section class="panel setup">
      <h2>开局配置</h2>

      <label class="field">
        <span>玩家 ID</span>
        <input class="input" v-model="form.playerId" maxlength="32" />
      </label>

      <div class="difficulty-grid">
        <button
          v-for="item in difficultyOptions"
          :key="item.value"
          class="difficulty-card"
          :class="{ active: form.difficulty === item.value }"
          @click="form.difficulty = item.value"
        >
          <strong>{{ item.title }}</strong>
          <p>{{ item.desc }}</p>
        </button>
      </div>

      <label class="field">
        <span>世界版本（可在世界工厂中生成）</span>
        <input class="input" v-model="form.worldVersion" maxlength="64" />
      </label>

      <div class="actions">
        <button class="btn" @click="goWorldFactory">世界工厂</button>
        <button class="btn btn--accent" @click="startGame" :disabled="loading">
          {{ loading ? "创建中..." : "进入游戏" }}
        </button>
      </div>

      <p class="error" v-if="error">{{ error }}</p>
    </section>
  </main>
</template>

<style scoped>
.boot-page {
  display: grid;
  gap: 18px;
  grid-template-columns: 1.1fr 1fr;
  align-items: stretch;
}

.hero,
.setup {
  padding: 24px;
}

.meta-line {
  margin: 0;
  color: var(--text-03);
  font-family: var(--font-mono);
  letter-spacing: 0.16em;
  font-size: 12px;
}

h1 {
  margin: 8px 0 0;
  font-size: clamp(52px, 8vw, 96px);
  line-height: 0.95;
  font-family: var(--font-display);
  letter-spacing: 0.03em;
}

.subtitle {
  margin: 18px 0 0;
  line-height: 1.8;
  color: var(--text-02);
  max-width: 54ch;
}

h2 {
  margin: 0;
  font-size: 24px;
  font-family: var(--font-display);
  letter-spacing: 0.04em;
}

.field {
  margin-top: 16px;
  display: grid;
  gap: 8px;
}

.field span {
  color: var(--text-03);
  font-size: 13px;
}

.difficulty-grid {
  margin-top: 16px;
  display: grid;
  gap: 10px;
}

.difficulty-card {
  text-align: left;
  border-radius: 14px;
  border: 1px solid var(--line-soft);
  background: rgba(8, 12, 18, 0.55);
  padding: 12px;
  transition: border-color var(--transition-fast), transform var(--transition-fast), background var(--transition-fast);
}

.difficulty-card strong {
  font-size: 17px;
}

.difficulty-card p {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--text-02);
}

.difficulty-card:hover {
  transform: translateY(-1px);
}

.difficulty-card.active {
  border-color: rgba(255, 130, 87, 0.6);
  background: rgba(255, 106, 61, 0.12);
}

.actions {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

.error {
  margin: 12px 0 0;
  color: var(--danger);
  font-size: 13px;
}

@media (max-width: 1024px) {
  .boot-page {
    grid-template-columns: 1fr;
  }
}
</style>
