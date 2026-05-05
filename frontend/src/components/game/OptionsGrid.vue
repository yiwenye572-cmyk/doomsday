<script setup lang="ts">
import type { OptionPayload } from "../../types/api";

const props = defineProps<{
  options: OptionPayload[];
  loading?: boolean;
}>();

const emit = defineEmits<{
  choose: [optionId: string];
}>();

function choose(optionId: string) {
  if (props.loading) {
    return;
  }
  emit("choose", optionId);
}
</script>

<template>
  <section class="options-grid">
    <button
      v-for="option in options"
      :key="option.id"
      class="option-card"
      :disabled="loading"
      @click="choose(option.id)"
    >
      <div class="option-head">
        <strong>{{ option.id.toUpperCase() }}</strong>
        <span class="tag">{{ option.riskLevel }}</span>
      </div>
      <p class="option-text">{{ option.text }}</p>
      <p class="option-effect">{{ option.expectedEffect }}</p>
    </button>
  </section>
</template>

<style scoped>
.options-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.option-card {
  text-align: left;
  border-radius: 16px;
  border: 1px solid var(--line-soft);
  background: linear-gradient(180deg, rgba(29, 38, 53, 0.66), rgba(10, 14, 20, 0.9));
  padding: 14px;
  display: grid;
  gap: 10px;
  transition: transform var(--transition-fast), border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.option-card:hover {
  transform: translateY(-2px);
  border-color: rgba(255, 149, 112, 0.5);
  box-shadow: var(--shadow-1);
}

.option-card:active {
  transform: translateY(0) scale(0.99);
}

.option-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.option-text {
  margin: 0;
  line-height: 1.6;
}

.option-effect {
  margin: 0;
  color: var(--text-03);
  font-size: 12px;
}

@media (max-width: 767px) {
  .options-grid {
    grid-template-columns: 1fr;
  }
}
</style>
