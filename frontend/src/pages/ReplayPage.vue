<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getReplay } from "../api/game";

const route = useRoute();
const router = useRouter();

const text = ref("");
const chapters = ref<string[]>([]);
const loading = ref(false);
const error = ref("");

const sessionId = String(route.params.sessionId || "");

async function loadReplay() {
  loading.value = true;
  error.value = "";
  try {
    text.value = await getReplay(sessionId);
    chapters.value = text.value
      .split(/\n\s*\n/g)
      .map((chunk) => chunk.trim())
      .filter(Boolean);
  } catch (e) {
    error.value = e instanceof Error ? e.message : "回放获取失败";
  } finally {
    loading.value = false;
  }
}

onMounted(loadReplay);
</script>

<template>
  <main class="page-wrap replay-page">
    <header class="panel replay-head">
      <div>
        <p class="meta">Session Replay</p>
        <h1>回放 {{ sessionId }}</h1>
      </div>
      <button class="btn" @click="router.push(`/game/${sessionId}`)">返回会话</button>
    </header>

    <article class="panel replay-body" v-if="!loading && !error">
      <div class="novel" v-if="chapters.length">
        <section class="chapter" v-for="(chapter, idx) in chapters" :key="`${idx}-${chapter.slice(0, 16)}`">
          <p>{{ chapter }}</p>
        </section>
      </div>
      <p class="empty" v-else>暂无回放内容</p>
    </article>

    <section class="panel replay-body" v-if="loading">回放加载中...</section>
    <section class="panel replay-body error" v-if="error">{{ error }}</section>
  </main>
</template>

<style scoped>
.replay-page {
  display: grid;
  gap: 14px;
}

.replay-head {
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

.replay-body {
  padding: 20px;
}

.novel {
  display: grid;
  gap: 14px;
}

.chapter {
  border-left: 2px solid rgba(255, 255, 255, 0.08);
  padding: 4px 0 4px 12px;
}

.chapter p {
  margin: 0;
  white-space: pre-line;
  color: var(--text-01);
  line-height: 1.8;
}

.empty {
  margin: 0;
  color: var(--text-03);
}

.error {
  color: var(--danger);
}

@media (max-width: 767px) {
  .replay-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
