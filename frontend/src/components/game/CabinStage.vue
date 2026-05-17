<template>
  <div class="cabin-stage">
    <!-- SVG rendering for minimal-style cabin -->
    <svg :width="width" :height="height" xmlns="http://www.w3.org/2000/svg">
      <rect :width="width" :height="height" fill="#f0f0f0" />
      <!-- Add SVG elements here -->
    </svg>
  </div>
</template>

<script>
export default {
  name: 'CabinStage',
  props: {
    width: {
      type: Number,
      default: 800
    },
    height: {
      type: Number,
      default: 600
    }
  }
};
</script>

<style scoped>
.cabin-stage {
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #ffffff;
}
svg {
  border: 1px solid #ccc;
}
</style>