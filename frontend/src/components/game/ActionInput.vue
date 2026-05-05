<script setup lang="ts">
import { ref } from "vue";

const emit = defineEmits<{
  submit: [value: string];
  comeback: [];
}>();

const props = defineProps<{
  loading?: boolean;
}>();

const input = ref("");

function submit() {
  const value = input.value.trim();
  if (!value || props.loading) {
    return;
  }
  emit("submit", value);
  input.value = "";
}
</script>

<template>
  <section class="panel action-input">
    <label class="meta">你的行动</label>
    <textarea
      v-model="input"
      class="textarea"
      maxlength="300"
      placeholder="例：我贴着墙沿巡逻盲区前进，先确认撤离路线。"
      :disabled="loading"
    ></textarea>
    <div class="action-row">
      <button class="btn" :disabled="loading" @click="emit('comeback')">使用翻盘卡</button>
      <button class="btn btn--accent" :disabled="loading || !input.trim()" @click="submit">
        提交回合
      </button>
    </div>
  </section>
</template>

<style scoped>
.action-input {
  padding: 16px;
  display: grid;
  gap: 12px;
}

.action-row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.action-row .btn {
  min-width: 128px;
}

@media (max-width: 767px) {
  .action-row {
    position: sticky;
    bottom: 8px;
    background: rgba(8, 10, 14, 0.82);
    padding-top: 8px;
    z-index: 2;
  }

  .action-row .btn {
    flex: 1;
  }
}
</style>
