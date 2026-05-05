import http from "./http";
import type { ApiResponse } from "../types/api";

export interface AgentMetricsSummary {
  agentName: string;
  totalCalls: number;
  successCalls: number;
  failCalls: number;
  avgMs: number;
  successRate: number;
}

export interface AgentSpan {
  agentName: string;
  elapsedMs: number;
  status: string;
  errorMessage: string | null;
}

export interface TraceDetail {
  traceId: string;
  sessionId: string;
  turn: number;
  startedAt: number;
  elapsedMs: number;
  finalStatus: string;
  spans: AgentSpan[];
}

export async function getAgentMetrics(): Promise<AgentMetricsSummary[]> {
  const res = await http.get<ApiResponse<AgentMetricsSummary[]>>("/admin/metrics/agents");
  return (res as unknown as ApiResponse<AgentMetricsSummary[]>).data;
}

export async function getRecentTraces(limit = 20): Promise<TraceDetail[]> {
  const res = await http.get<ApiResponse<TraceDetail[]>>("/admin/metrics/traces", {
    params: { limit },
  });
  return (res as unknown as ApiResponse<TraceDetail[]>).data;
}

export async function getTrace(traceId: string): Promise<TraceDetail> {
  const res = await http.get<ApiResponse<TraceDetail>>(`/admin/metrics/traces/${traceId}`);
  return (res as unknown as ApiResponse<TraceDetail>).data;
}
