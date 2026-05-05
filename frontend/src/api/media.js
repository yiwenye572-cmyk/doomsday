import http from "./http";
export async function generateImage(req) {
    const response = await http.post(`/media/images/generate`, req);
    return response.data.data;
}
export async function gallerySearch(query, limit = 5) {
    const response = await http.get(`/media/images/gallery-search`, { params: { q: query, limit } });
    return response.data.data;
}
//# sourceMappingURL=media.js.map