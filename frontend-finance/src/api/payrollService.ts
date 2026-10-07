import api from './axios';
import {
  ApiResponse,
  BaremeIrpp,
  BulletinPaie,
  ElementSalaire,
  FichePaie,
  ParametresPaie,
  TotauxPaie,
  AcompteSalaire,
} from '../types';

export const payrollService = {
  getParametres: () => api.get<ApiResponse<ParametresPaie>>('/finance/paie/parametres'),
  updateParametres: (data: ParametresPaie) => api.put<ApiResponse<ParametresPaie>>('/finance/paie/parametres', data),

  listBaremes: () => api.get<ApiResponse<BaremeIrpp[]>>('/finance/paie/baremes-irpp'),
  createBareme: (data: Partial<BaremeIrpp>) => api.post<ApiResponse<BaremeIrpp>>('/finance/paie/baremes-irpp', data),

  calculer: (employeId: number, mois: string, elements?: ElementSalaire[]) =>
    api.post<ApiResponse<FichePaie>>('/finance/paie/calcul', { employeId, mois, elements }),

  genererBulletin: (employeId: number, mois: string, elements?: ElementSalaire[]) =>
    api.post<ApiResponse<BulletinPaie>>('/finance/paie/generer', { employeId, mois, elements }),

  genererTout: (mois: string) => api.post<ApiResponse<BulletinPaie[]>>(`/finance/paie/generer-tout/${mois}`),

  getBulletins: (mois: string) => api.get<ApiResponse<BulletinPaie[]>>(`/finance/paie/bulletins?mois=${mois}`),
  getBulletinsByEmploye: (employeId: number) =>
    api.get<ApiResponse<BulletinPaie[]>>(`/finance/paie/bulletins/employe/${employeId}`),
  getImpayes: (avant: string) => api.get<ApiResponse<BulletinPaie[]>>(`/finance/paie/impayes?avant=${avant}`),
  getTotaux: (mois: string) => api.get<ApiResponse<TotauxPaie>>(`/finance/paie/totaux?mois=${mois}`),
  getAcomptes: (employeId: number, mois: string) =>
    api.get<ApiResponse<AcompteSalaire[]>>(`/finance/paie/acomptes?employeId=${employeId}&mois=${mois}`),

  marquerPaye: (bulletinId: number, date?: string) =>
    api.post<ApiResponse<BulletinPaie>>(`/finance/paie/bulletins/${bulletinId}/payer`, date ? { date } : {}),

  enregistrerAcompte: (employeId: number, mois: string, montant: number, date?: string, note?: string) =>
    api.post<ApiResponse<BulletinPaie>>('/finance/paie/acompte', { employeId, mois, montant, date, note }),
};
