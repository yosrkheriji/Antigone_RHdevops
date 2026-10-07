import api from './axios';
import { ApiResponse, Client, Employe, ParametresFacturation } from '../types';

/**
 * Données de référence du module Finance. Ces endpoints sont gardés par
 * VIEW_FINANCE, contrairement à /clients et /employes qui exigent les
 * permissions RH/Projets.
 */
export const referentielService = {
  getClients: () => api.get<ApiResponse<Client[]>>('/finance/clients'),

  /** Employés dont le contrat couvre le mois demandé (archivés en cours de mois inclus). */
  getEmployesDuMois: (mois: string) => api.get<ApiResponse<Employe[]>>(`/finance/employes?mois=${mois}`),
  getTousEmployes: () => api.get<ApiResponse<Employe[]>>('/finance/employes'),

  getParametresFacturation: () =>
    api.get<ApiResponse<ParametresFacturation>>('/finance/parametres-facturation'),
};
