import api from './axios';
import { ApiResponse, DeclarationCnss } from '../types';

export const cnssService = {
  getByAnnee: (annee: number) => api.get<ApiResponse<DeclarationCnss[]>>(`/finance/cnss?annee=${annee}`),
  getSuggestion: (annee: number, trimestre: number) =>
    api.get<ApiResponse<DeclarationCnss>>(`/finance/cnss/suggestion?annee=${annee}&trimestre=${trimestre}`),
  save: (data: Partial<DeclarationCnss>) => api.post<ApiResponse<DeclarationCnss>>('/finance/cnss', data),
  payer: (id: number, datePaiement?: string) =>
    api.post<ApiResponse<DeclarationCnss>>(`/finance/cnss/${id}/payer`, datePaiement ? { datePaiement } : {}),
};
