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
//# sourceMappingURL=game.js.map