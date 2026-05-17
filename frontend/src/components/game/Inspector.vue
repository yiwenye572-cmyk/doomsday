<template>
  <div class="inspector">
    <h3>Inspector</h3>
    <div v-if="selectedItem">
      <p><strong>Name:</strong> {{ selectedItem.name }}</p>
      <p><strong>Type:</strong> {{ selectedItem.type }}</p>
      <p><strong>Position:</strong> ({{ selectedItem.x }}, {{ selectedItem.y }})</p>
      <button @click="viewStory">View Story</button>
      <button @click="rotateItem">Rotate</button>
      <button @click="deleteItem">Delete</button>
    </div>
    <div v-else>
      <p>No item selected</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Inspector',
  props: {
    selectedItem: {
      type: Object,
      default: null
    }
  },
  methods: {
    viewStory() {
      this.$emit('view-story', this.selectedItem);
    },
    rotateItem() {
      this.$emit('rotate-item', this.selectedItem);
    },
    deleteItem() {
      this.$emit('delete-item', this.selectedItem);
    }
  }
};
</script>

<style scoped>
.inspector {
  padding: 10px;
  background-color: #f9f9f9;
  border: 1px solid #ddd;
  border-radius: 5px;
}
button {
  margin: 5px;
  padding: 5px 10px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 3px;
  cursor: pointer;
}
button:hover {
  background-color: #0056b3;
}
</style>