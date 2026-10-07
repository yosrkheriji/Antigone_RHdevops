import api from './axios';
import {
  ApiResponse,
  ChargeFixe,
  ChargeFixeRequest,
  ChargeVariable,
  EtatChargeFixe,
  PaiementChargeFixe,
  ResumeCharges,
} from '../types';

export const chargesService = {
  getFixes: () => api.get<ApiResponse<ChargeFixe[]>>('/finance/charges/fixes'),
  createFixe: (data: ChargeFixeRequest) => api.post<ApiResponse<ChargeFixe>>('/finance/charges/fixes', data),
  updateFixe: (id: number, data: ChargeFixeRequest) =>
    api.put<ApiResponse<ChargeFixe>>(`/finance/charges/fixes/${id}`, data),
  archiveFixe: (id: number) => api.delete<ApiResponse<void>>(`/finance/charges/fixes/${id}`),

  getPaiements: (chargeFixeId: number) =>
    api.get<ApiResponse<PaiementChargeFixe[]>>(`/finance/charges/fixes/${chargeFixeId}/paiements`),
  enregistrerPaiement: (chargeFixeId: number, mois: string, montant?: number, datePaiement?: string) =>
    api.post<ApiResponse<PaiementChargeFixe>>(`/finance/charges/fixes/${chargeFixeId}/paiements`, {
      mois,
      montant,
      datePaiement,
    }),

  getVariables: (mois: string) => api.get<ApiResponse<ChargeVariable[]>>(`/finance/charges/variables?mois=${mois}`),
  createVariable: (data: Partial<ChargeVariable>) =>
    api.post<ApiResponse<ChargeVariable>>('/finance/charges/variables', data),
  updateVariable: (id: number, data: Partial<ChargeVariable>) =>
    api.put<ApiResponse<ChargeVariable>>(`/finance/charges/variables/${id}`, data),
  deleteVariable: (id: number) => api.delete<ApiResponse<void>>(`/finance/charges/variables/${id}`),

  getEtatFixes: (mois: string) =>
    api.get<ApiResponse<EtatChargeFixe[]>>(`/finance/charges/fixes/etat?mois=${mois}`),
  getResume: (mois: string) => api.get<ApiResponse<ResumeCharges>>(`/finance/charges/resume?mois=${mois}`),
};
