import http from "./http";
import type { ApiResponse, DiaryEntryView, DiaryLevel } from "../types/api";

export async function getDiary(
  sessionId: string,
  level: DiaryLevel,
  range?: { fromTurn?: number; toTurn?: number },
): Promise<DiaryEntryView[]> {
  const response = await http.get<ApiResponse<DiaryEntryView[]>>(`/game/sessions/${sessionId}/diary`, {
    params: {
      level,
      fromTurn: range?.fromTurn,
      toTurn: range?.toTurn,
    },
  });
  return response.data.data;
}
