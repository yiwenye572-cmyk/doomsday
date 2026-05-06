import http from "./http";
export async function getDefaultWorld() {
    const response = await http.get("/game/worlds/default");
    return response.data.data;
}
export async function initializeWorld(payload) {
    const response = await http.post("/game/worlds/initialize", payload);
    return response.data.data;
}
export async function getWorldFactoryJob(jobId) {
    const response = await http.get(`/admin/world-factory/jobs/${jobId}`);
    return response.data.data;
}
//# sourceMappingURL=world.js.map