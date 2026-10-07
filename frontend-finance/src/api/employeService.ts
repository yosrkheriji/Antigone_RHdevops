import api from './axios';
import { ApiResponse, Employe } from '../types';

export const employeService = {
  /** Bascule le mode de saisie du salaire entre BRUT et NET. */
  updateModeSalaire: (id: number, modeSalaire: 'BRUT' | 'NET') =>
    api.patch<ApiResponse<Employe>>(`/employes/${id}/mode-salaire`, { modeSalaire }),
};
