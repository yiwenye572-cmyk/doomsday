import http from "./http";
export async function getAgentMetrics() {
    const res = await http.get("/admin/metrics/agents");
    return res.data.data;
}
export async function getRecentTraces(limit = 20) {
    const res = await http.get("/admin/metrics/traces", {
        params: { limit },
    });
    return res.data.data;
}
export async function getTrace(traceId) {
    const res = await http.get(`/admin/metrics/traces/${traceId}`);
    return res.data.data;
}
export async function getToolSummary() {
    const res = await http.get("/admin/tools/summary");
    return res.data.data;
}
export async function getToolAudits(limit = 20) {
    const res = await http.get("/admin/tools/audits", {
        params: { limit },
    });
    return res.data.data;
}
export async function getRegisteredTools() {
    const res = await http.get("/admin/tools");
    return res.data.data;
}
//# sourceMappingURL=admin.js.map