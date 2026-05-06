import http from "./http";
export async function getDiary(sessionId, level, range) {
    const response = await http.get(`/game/sessions/${sessionId}/diary`, {
        params: {
            level,
            fromTurn: range?.fromTurn,
            toTurn: range?.toTurn,
        },
    });
    return response.data.data;
}
//# sourceMappingURL=diary.js.map