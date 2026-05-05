import http from "./http";
import type { ApiResponse, GenerateImageRequest, GenerateImageResponse, GalleryImageItem } from "../types/api";

export async function generateImage(req: GenerateImageRequest): Promise<GenerateImageResponse> {
  const response = await http.post<ApiResponse<GenerateImageResponse>>(`/media/images/generate`, req);
  return response.data.data;
}

export async function gallerySearch(query: string, limit = 5): Promise<GalleryImageItem[]> {
  const response = await http.get<ApiResponse<GalleryImageItem[]>>(`/media/images/gallery-search`, { params: { q: query, limit } });
  return response.data.data;
}
