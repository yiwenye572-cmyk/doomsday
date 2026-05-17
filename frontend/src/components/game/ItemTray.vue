<template>
  <div class="item-tray">
    <h3>Item Tray</h3>
    <div class="items">
      <div
        v-for="item in items"
        :key="item.id"
        class="item"
        draggable="true"
        @dragstart="onDragStart(item)"
      >
        <img :src="item.image" :alt="item.name" />
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ItemTray',
  props: {
    items: {
      type: Array,
      required: true
    }
  },
  methods: {
    onDragStart(item) {
      this.$emit('dragstart', item);
    }
  }
};
</script>

<style scoped>
.item-tray {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 10px;
  background-color: #f4f4f4;
  border: 1px solid #ccc;
  border-radius: 5px;
}
.item {
  width: 50px;
  height: 50px;
  display: flex;
  justify-content: center;
  align-items: center;
  border: 1px solid #ddd;
  border-radius: 3px;
  background-color: #fff;
  cursor: grab;
}
.item img {
  max-width: 100%;
  max-height: 100%;
}
</style>