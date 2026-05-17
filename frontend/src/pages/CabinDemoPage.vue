<template>
  <div class="cabin-demo-page">
    <h1>Cabin Demo</h1>
    <div class="canvas-container">
      <CabinCanvas :width="800" :height="600" />
    </div>
    <div class="stage-container">
      <CabinStage :width="800" :height="600" />
    </div>
    <div class="tray-container">
      <ItemTray :items="items" @dragstart="handleDragStart" />
    </div>
    <div class="inspector-container">
      <Inspector :selectedItem="selectedItem" @view-story="viewStory" @rotate-item="rotateItem" @delete-item="deleteItem" />
    </div>
  </div>
</template>

<script>
import CabinCanvas from '@/components/game/CabinCanvas.vue';
import CabinStage from '@/components/game/CabinStage.vue';
import ItemTray from '@/components/game/ItemTray.vue';
import Inspector from '@/components/game/Inspector.vue';

export default {
  name: 'CabinDemoPage',
  components: {
    CabinCanvas,
    CabinStage,
    ItemTray,
    Inspector
  },
  data() {
    return {
      items: [
        { id: 'item1', name: 'Bed', type: 'bed', image: '/assets/bed.png', x: 0, y: 0 },
        { id: 'item2', name: 'Table', type: 'table', image: '/assets/table.png', x: 0, y: 0 }
      ],
      selectedItem: null
    };
  },
  methods: {
    handleDragStart(item) {
      console.log('Dragging item:', item);
    },
    viewStory(item) {
      console.log('View story for item:', item);
    },
    rotateItem(item) {
      console.log('Rotate item:', item);
    },
    deleteItem(item) {
      console.log('Delete item:', item);
    }
  }
};
</script>

<style scoped>
.cabin-demo-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
}
.canvas-container,
.stage-container,
.tray-container,
.inspector-container {
  margin: 20px 0;
}
</style>