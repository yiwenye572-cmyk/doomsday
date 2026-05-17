import http from "./http";
function idempotencyKey() {
    const uuid = globalThis.crypto?.randomUUID?.();
    return uuid ?? `idem-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}
export async function createSession(payload) {
    const response = await http.post("/game/sessions", payload);
    return response.data.data;
}
export async function getSessionState(sessionId) {
    const response = await http.get(`/game/sessions/${sessionId}/state`);
    return response.data.data;
}
export async function submitTurn(sessionId, payload) {
    const response = await http.post(`/game/sessions/${sessionId}/turns`, payload, {
        headers: {
            "Idempotency-Key": idempotencyKey(),
        },
    });
    return response.data.data;
}
export async function chooseOption(sessionId, turn, payload) {
    const response = await http.post(`/game/sessions/${sessionId}/turns/${turn}/choose`, payload);
    return response.data.data;
}
export async function useComebackCard(sessionId, payload) {
    const response = await http.post(`/game/sessions/${sessionId}/comeback-card`, payload);
    return response.data.data;
}
export async function getReplay(sessionId, range) {
    const response = await http.get(`/game/sessions/${sessionId}/replay`, {
        params: range,
    });
    return response.data.data;
}
/** 获取小屋状态 */
export async function getCabinState(sessionId) {
    const response = await http.get(`/game/sessions/${sessionId}/cabin`);
    return response.data.data;
}
/** 提交物品变更（Redis Lua CAS；409 = 版本冲突） */
export async function updateCabinState(sessionId, expectedVersion, changes) {
    const response = await http.post(`/game/sessions/${sessionId}/cabin/update`, { idempotencyKey: idempotencyKey(), expectedVersion, changes });
    return response.data.data;
}
/** 休息触发 */
export async function restCabin(sessionId, durationHours, newTimeOfDay) {
    const response = await http.post(`/game/sessions/${sessionId}/cabin/rest`, {
        sessionId,
        durationHours,
        newTimeOfDay,
    });
    return response.data.data;
}
/** 出门携带物品 */
export async function takeCabinItems(sessionId, itemIds) {
    const response = await http.post(`/game/sessions/${sessionId}/cabin/take`, {
        idempotencyKey: idempotencyKey(),
        itemIds,
    });
    return response.data.data;
}
/** 归来放入待整理区 */
export async function returnCabinItems(sessionId, foundItems) {
    const response = await http.post(`/game/sessions/${sessionId}/cabin/return`, {
        idempotencyKey: idempotencyKey(),
        foundItems,
    });
    return response.data.data;
}
//# sourceMappingURL=game.js.map