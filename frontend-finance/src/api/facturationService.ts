import api from './axios';
import {
  ApiResponse,
  Facture,
  FactureRequest,
  PaiementFacture,
  RelanceClient,
  ServiceCatalogue,
  TemplateFacture,
  TypeDocument,
} from '../types';

const BASE = '/finance/facturation';

export const facturationService = {
  getByType: (type: TypeDocument) => api.get<ApiResponse<Facture[]>>(`${BASE}/documents?type=${type}`),
  getById: (id: number) => api.get<ApiResponse<Facture>>(`${BASE}/documents/${id}`),
  getByClient: (clientId: number) => api.get<ApiResponse<Facture[]>>(`${BASE}/documents/client/${clientId}`),
  getImpayees: () => api.get<ApiResponse<Facture[]>>(`${BASE}/impayees`),

  create: (data: FactureRequest) => api.post<ApiResponse<Facture>>(`${BASE}/documents`, data),
  update: (id: number, data: FactureRequest) => api.put<ApiResponse<Facture>>(`${BASE}/documents/${id}`, data),
  remove: (id: number) => api.delete<ApiResponse<void>>(`${BASE}/documents/${id}`),
  marquerPayee: (id: number, datePaiement?: string) =>
    api.post<ApiResponse<Facture>>(`${BASE}/documents/${id}/payer`, datePaiement ? { datePaiement } : {}),

  getPaiements: (id: number) => api.get<ApiResponse<PaiementFacture[]>>(`${BASE}/documents/${id}/paiements`),
  enregistrerPaiement: (id: number, montant: number, datePaiement?: string, note?: string) =>
    api.post<ApiResponse<Facture>>(`${BASE}/documents/${id}/paiements`, { montant, datePaiement, note }),
  supprimerPaiement: (paiementId: number) =>
    api.delete<ApiResponse<Facture>>(`${BASE}/paiements/${paiementId}`),

  getServices: () => api.get<ApiResponse<ServiceCatalogue[]>>(`${BASE}/services`),
  createService: (data: Partial<ServiceCatalogue>) =>
    api.post<ApiResponse<ServiceCatalogue>>(`${BASE}/services`, data),
  updateService: (id: number, data: Partial<ServiceCatalogue>) =>
    api.put<ApiResponse<ServiceCatalogue>>(`${BASE}/services/${id}`, data),
  deleteService: (id: number) => api.delete<ApiResponse<void>>(`${BASE}/services/${id}`),

  getTemplates: () => api.get<ApiResponse<TemplateFacture[]>>(`${BASE}/templates`),
  createTemplate: (data: Partial<TemplateFacture>) =>
    api.post<ApiResponse<TemplateFacture>>(`${BASE}/templates`, data),
  deleteTemplate: (id: number) => api.delete<ApiResponse<void>>(`${BASE}/templates/${id}`),

  getRelances: () => api.get<ApiResponse<RelanceClient[]>>(`${BASE}/relances`),
  createRelance: (factureId: number, dateRelance?: string, note?: string) =>
    api.post<ApiResponse<RelanceClient>>(`${BASE}/relances`, { factureId, dateRelance, note }),
  marquerRelanceEnvoyee: (id: number) =>
    api.post<ApiResponse<RelanceClient>>(`${BASE}/relances/${id}/envoyee`, {}),
};
