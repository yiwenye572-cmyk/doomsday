<template>
  <div class="cabin-canvas">
    <!-- Canvas rendering for pixel-style cabin -->
    <canvas ref="canvas" :width="width" :height="height"></canvas>
  </div>
</template>

<script>
import { onMounted, ref } from 'vue';

export default {
  name: 'CabinCanvas',
  props: {
    width: {
      type: Number,
      default: 800
    },
    height: {
      type: Number,
      default: 600
    }
  },
  setup(props) {
    const canvas = ref(null);

    onMounted(() => {
      const ctx = canvas.value.getContext('2d');
      // Initialize canvas drawing logic here
      ctx.fillStyle = '#000';
      ctx.fillRect(0, 0, props.width, props.height);
    });

    return {
      canvas
    };
  }
};
</script>

<style scoped>
.cabin-canvas {
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #2b2b2b;
}
canvas {
  image-rendering: pixelated;
  border: 1px solid #ccc;
}
</style>