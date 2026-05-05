<script setup lang="ts">
import { defineEmits, defineProps } from "vue";
import type { GenerateImageResponse, GalleryImageItem } from "../../types/api";

const props = defineProps<{
  image: GenerateImageResponse | GalleryImageItem | null;
}>();

const emit = defineEmits<{ (e: "regenerate"): void }>();

function onRegenerate() {
  emit("regenerate");
}
</script>

<template>
  <div v-if="image" class="image-card panel">
    <div class="image-wrap">
      <img :src="image.imageUrl || (image as any).imageUrl" alt="配图" />
    </div>
    <div class="card-foot">
      <div class="caption">
        <span class="badge source">{{ (image as any).provider ?? (image as any).source ?? '图库' }}</span>
        <span class="meta" v-if="(image as any).fallback">兜底</span>
      </div>
      <div class="actions">
        <button class="btn" @click="onRegenerate">重新生成</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.image-card {
  padding: 12px;
  display: grid;
  gap: 8px;
}
.image-wrap {
  background: var(--bg-02);
  border-radius: 8px;
  overflow: hidden;
  min-height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.image-wrap img {
  width: 100%;
  height: auto;
  display: block;
}
.card-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.badge.source {
  background: rgba(0,0,0,0.45);
  color: #fff;
  padding: 4px 8px;
  border-radius: 20px;
  font-size: 12px;
}
.actions .btn {
  padding: 6px 10px;
  font-size: 13px;
}
</style>
