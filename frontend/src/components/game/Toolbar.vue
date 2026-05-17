<template>
  <div class="toolbar">
    <button class="btn" @click="$emit('save')" :disabled="saving">
      {{ saving ? '保存中…' : '💾 保存' }}
    </button>
    <button class="btn" @click="$emit('export')">📤 导出 JSON</button>
    <button class="btn" @click="triggerImport">📥 导入 JSON</button>
    <button class="btn" @click="$emit('undo')">↩ 撤销</button>
    <button class="btn btn--danger" @click="$emit('rest')">🛏 休息</button>
    <button class="btn" @click="$emit('leave')">🚪 出门</button>
    <input ref="fileInput" type="file" accept=".json" class="hidden-input" @change="onFileChange" />
    <span v-if="lastSaved" class="saved-hint">✓ {{ lastSaved }}</span>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

withDefaults(defineProps<{
  saving?: boolean;
  lastSaved?: string;
}>(), { saving: false });

const emit = defineEmits<{
  (e: 'save'): void;
  (e: 'export'): void;
  (e: 'import', json: string): void;
  (e: 'undo'): void;
  (e: 'rest'): void;
  (e: 'leave'): void;
}>();

const fileInput = ref<HTMLInputElement | null>(null);

function triggerImport() {
  fileInput.value?.click();
}

function onFileChange(ev: Event) {
  const file = (ev.target as HTMLInputElement).files?.[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = (e) => emit('import', e.target?.result as string);
  reader.readAsText(file);
  (ev.target as HTMLInputElement).value = '';
}
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(10, 14, 22, 0.85);
  border-bottom: 1px solid var(--line-soft, rgba(255,255,255,0.1));
  flex-wrap: wrap;
}

.btn {
  padding: 6px 14px;
  border-radius: 8px;
  border: 1px solid rgba(255,255,255,0.2);
  background: rgba(255,255,255,0.07);
  color: #e8eaf0;
  cursor: pointer;
  font-size: 13px;
  transition: background 0.15s;
}
.btn:hover:not(:disabled) {
  background: rgba(255,255,255,0.15);
}
.btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.btn--danger {
  border-color: rgba(255,120,80,0.5);
  color: #ffb09a;
}
.hidden-input {
  display: none;
}
.saved-hint {
  font-size: 12px;
  color: #7dd87d;
  margin-left: 4px;
}
</style>
