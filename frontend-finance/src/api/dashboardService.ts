import api from './axios';
import { ApiResponse, DeclarationTva, ResultatNet, VueDecaissements, VueEncaissements } from '../types';

export const dashboardService = {
  getEncaissements: (mois: string) =>
    api.get<ApiResponse<VueEncaissements>>(`/finance/dashboard/encaissements?mois=${mois}`),
  getDecaissements: (mois: string) =>
    api.get<ApiResponse<VueDecaissements>>(`/finance/dashboard/decaissements?mois=${mois}`),
  getResultatNet: (mois: string) => api.get<ApiResponse<ResultatNet>>(`/finance/dashboard/resultat?mois=${mois}`),
  getTva: (mois: string) => api.get<ApiResponse<DeclarationTva>>(`/finance/dashboard/tva?mois=${mois}`),
};
