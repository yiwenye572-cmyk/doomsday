import http from "./http";
export async function getAgentMetrics() {
    const res = await http.get("/admin/metrics/agents");
    return res.data;
}
export async function getRecentTraces(limit = 20) {
    const res = await http.get("/admin/metrics/traces", {
        params: { limit },
    });
    return res.data;
}
export async function getTrace(traceId) {
    const res = await http.get(`/admin/metrics/traces/${traceId}`);
    return res.data;
}
//# sourceMappingURL=admin.js.map