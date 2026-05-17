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
/**
 * 查询/触发物品叙事生成。
 * - 首次调用：后端创建任务，返回 HTTP 202 + status=PENDING
 * - 轮询调用：返回当前 status（RUNNING/DONE/FAILED）
 * - DONE：story 字段包含叙事文本
 * @param accepted  输出参数，表示是否返回了 202（由 axios 响应状态判断）
 */
export async function getItemStory(sessionId, itemId, itemType, itemMetadata) {
    const response = await http.get(`/game/sessions/${sessionId}/items/${itemId}/story`, { params: { itemType, itemMetadata }, validateStatus: (s) => s < 500 });
    return {
        accepted: response.status === 202,
        data: response.data.data,
    };
}
/** 生成布局推荐（GET） */
export async function getRecommendation(sessionId) {
    const response = await http.get(`/game/sessions/${sessionId}/cabin/recommendation`);
    return response.data.data;
}
/** 接受推荐，应用到小屋状态（POST accept） */
export async function acceptRecommendation(sessionId, recommendationId, expectedVersion) {
    const response = await http.post(`/game/sessions/${sessionId}/cabin/recommendation/${recommendationId}/accept`, { expectedVersion }, { validateStatus: (s) => s < 500 });
    return response.data.data;
}
/** 拒绝推荐（POST reject） */
export async function rejectRecommendation(sessionId, recommendationId) {
    await http.post(`/game/sessions/${sessionId}/cabin/recommendation/${recommendationId}/reject`, {});
}
//# sourceMappingURL=game.js.map