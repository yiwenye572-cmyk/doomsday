<template>
  <div class="app-shell">
    <div class="rain-overlay" aria-hidden="true"></div>
    <div class="grain-overlay" aria-hidden="true"></div>
    <router-view />
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  position: relative;
  isolation: isolate;
}

.rain-overlay,
.grain-overlay {
  position: fixed;
  inset: 0;
  pointer-events: none;
}

.rain-overlay {
  z-index: -2;
  background:
    linear-gradient(180deg, rgba(160, 185, 204, 0.08) 0%, rgba(160, 185, 204, 0) 14%),
    radial-gradient(circle at 20% 10%, rgba(164, 194, 210, 0.2) 0%, rgba(164, 194, 210, 0) 42%),
    radial-gradient(circle at 78% 6%, rgba(255, 102, 56, 0.13) 0%, rgba(255, 102, 56, 0) 34%),
    linear-gradient(160deg, #10151d 0%, #0a0d12 44%, #111824 100%);
}

.grain-overlay {
  z-index: -1;
  opacity: 0.2;
  background-image: radial-gradient(rgba(255, 255, 255, 0.15) 0.5px, transparent 0.5px);
  background-size: 3px 3px;
}
</style>
