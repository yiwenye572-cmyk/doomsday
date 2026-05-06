import http from "./http";
import type {
  ApiResponse,
  DefaultWorldResponse,
  GameWorldInitRequest,
  GameWorldInitResponse,
  WorldFactoryJobResponse,
} from "../types/api";

export async function getDefaultWorld(): Promise<DefaultWorldResponse> {
  const response = await http.get<ApiResponse<DefaultWorldResponse>>("/game/worlds/default");
  return response.data.data;
}

export async function initializeWorld(payload: GameWorldInitRequest): Promise<GameWorldInitResponse> {
  const response = await http.post<ApiResponse<GameWorldInitResponse>>("/game/worlds/initialize", payload);
  return response.data.data;
}

export async function getWorldFactoryJob(jobId: string): Promise<WorldFactoryJobResponse> {
  const response = await http.get<ApiResponse<WorldFactoryJobResponse>>(`/admin/world-factory/jobs/${jobId}`);
  return response.data.data;
}
