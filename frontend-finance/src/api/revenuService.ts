import api from './axios';
import { ApiResponse, AutreRevenu } from '../types';

export const revenuService = {
  getByMois: (mois: string) => api.get<ApiResponse<AutreRevenu[]>>(`/finance/revenus?mois=${mois}`),
  create: (data: Partial<AutreRevenu>) => api.post<ApiResponse<AutreRevenu>>('/finance/revenus', data),
  update: (id: number, data: Partial<AutreRevenu>) =>
    api.put<ApiResponse<AutreRevenu>>(`/finance/revenus/${id}`, data),
  remove: (id: number) => api.delete<ApiResponse<void>>(`/finance/revenus/${id}`),
};
