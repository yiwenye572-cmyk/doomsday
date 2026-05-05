import axios from "axios";
export class ApiBusinessError extends Error {
    code;
    traceId;
    constructor(code, message, traceId) {
        super(message);
        this.name = "ApiBusinessError";
        this.code = code;
        this.traceId = traceId;
    }
}
const http = axios.create({
    baseURL: "/api/v1",
    timeout: 60000, // LLM 调用可能耗时 20-30s，60s 宽限
});
http.interceptors.request.use((config) => {
    const traceId = globalThis.crypto?.randomUUID?.() ?? `${Date.now()}`;
    config.headers["X-Trace-Id"] = traceId;
    return config;
});
http.interceptors.response.use((response) => {
    const payload = response.data;
    if (!payload || typeof payload !== "object") {
        throw new ApiBusinessError("INTERNAL_ERROR", "Invalid response payload");
    }
    if (payload.code !== "OK") {
        throw new ApiBusinessError(payload.code, payload.message, payload.traceId);
    }
    // keep the AxiosResponse shape but replace .data with the parsed payload
    response.data = payload;
    return response;
}, (error) => {
    if (error.response?.data) {
        const payload = error.response.data;
        throw new ApiBusinessError(payload.code, payload.message, payload.traceId);
    }
    throw new ApiBusinessError("NETWORK_ERROR", error.message || "Network failure");
});
export default http;
//# sourceMappingURL=http.js.map