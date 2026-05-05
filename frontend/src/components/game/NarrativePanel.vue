<script setup lang="ts">
import type { PlotPayload, GenerateImageResponse, GalleryImageItem } from "../../types/api";
import ImageCard from "./ImageCard.vue";

const props = defineProps<{
  plot: PlotPayload | null;
  image?: GenerateImageResponse | GalleryImageItem | null;
}>();
const emit = defineEmits<{
  (e: "regenerate"): void;
}>();
</script>

<template>
  <section class="panel narrative-panel">
    <header class="narrative-head">
      <h2>雨夜记录</h2>
      <span class="tag" v-if="plot">置信度 {{ Math.round(plot.confidence * 100) }}%</span>
    </header>

    <p class="empty" v-if="!plot">暂无剧情推进，输入你的行动开始这一夜。</p>

    <article class="plot" v-else>
      {{ plot.text }}
    </article>

    <ImageCard v-if="props.image" :image="props.image" @regenerate="() => emit('regenerate')" />

    <footer class="citations" v-if="plot && plot.citations?.length">
      <span class="meta">证据来源</span>
      <div class="citation-list">
        <span class="tag" v-for="item in plot.citations" :key="item">{{ item }}</span>
      </div>
    </footer>
  </section>
</template>

<style scoped>
.narrative-panel {
  padding: 20px;
  display: grid;
  gap: 14px;
  min-height: 300px;
}

.narrative-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 30px;
  letter-spacing: 0.04em;
}

.plot {
  margin: 0;
  line-height: 1.82;
  letter-spacing: 0.01em;
  color: var(--text-01);
  white-space: pre-wrap;
}

.empty {
  margin: 0;
  color: var(--text-02);
}

.citations {
  border-top: 1px dashed var(--line-soft);
  padding-top: 12px;
  display: grid;
  gap: 8px;
}

.citation-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.meta {
  font-size: 12px;
  color: var(--text-03);
}
</style>
