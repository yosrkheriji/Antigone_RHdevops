import api from './axios';
import { ApiResponse, Dette, DettePaiement } from '../types';

export const detteService = {
  getAll: () => api.get<ApiResponse<Dette[]>>('/finance/dettes'),
  create: (data: Partial<Dette>) => api.post<ApiResponse<Dette>>('/finance/dettes', data),
  update: (id: number, data: Partial<Dette>) => api.put<ApiResponse<Dette>>(`/finance/dettes/${id}`, data),
  remove: (id: number) => api.delete<ApiResponse<void>>(`/finance/dettes/${id}`),

  getPaiements: (id: number) => api.get<ApiResponse<DettePaiement[]>>(`/finance/dettes/${id}/paiements`),
  enregistrerPaiement: (id: number, montant: number, datePaiement?: string, note?: string) =>
    api.post<ApiResponse<Dette>>(`/finance/dettes/${id}/paiements`, { montant, datePaiement, note }),
  supprimerPaiement: (paiementId: number) =>
    api.delete<ApiResponse<Dette>>(`/finance/dettes/paiements/${paiementId}`),
};
