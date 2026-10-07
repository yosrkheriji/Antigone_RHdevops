import api from './axios';
import { ApiResponse } from '../types';

/* ── Types — miroir de FinanceDashboardDTO (backend, package com.antigone.rh.bi) ──
 * Les montants « produits / charges / resultat » sont en base ENGAGEMENT
 * (comptable) ; « encaissements / decaissements » en base TRESORERIE (cash).
 */

export interface FinanceKpis {
  produitsHt: number;
  chargesTotales: number;
  masseSalariale: number;
  resultat: number;
  margePourcent: number;
  encaissements: number;
  decaissements: number;
  tresorerieNette: number;
  creancesEnCours: number;
  creancesEnRetard: number;
}

export interface PointMensuelFinance {
  mois: string;
  libelle: string;
  produits: number;
  charges: number;
  resultat: number;
  encaissements: number;
  decaissements: number;
}

export interface RepartitionCharge {
  nature: string;
  montant: number;
}

export interface LigneClient {
  client: string;
  caHt: number;
  nombreFactures: number;
  resteDu: number;
}

export interface TrancheAge {
  tranche: string;
  montant: number;
  nombreFactures: number;
}

export interface FactureEnRetard {
  numero: string;
  client: string;
  dateEcheance: string | null;
  resteDu: number;
  joursRetard: number;
}

export interface FinanceDashboard {
  kpis: FinanceKpis;
  evolutionMensuelle: PointMensuelFinance[];
  repartitionCharges: RepartitionCharge[];
  topClients: LigneClient[];
  balanceAgee: TrancheAge[];
  facturesEnRetard: FactureEnRetard[];
}

export interface EtatEntrepot {
  dernierChargement: string | null;
  dureeMs: number | null;
  lignesChargees: number | null;
  statut: string;
  message: string | null;
  volumes: { table: string; lignes: number }[];
}

export const analyticsService = {
  getFinance: (debut: string, fin: string) =>
    api.get<ApiResponse<FinanceDashboard>>('/analytics/finance', { params: { debut, fin } }),

  getEtat: () => api.get<ApiResponse<EtatEntrepot>>('/analytics/etat'),

  /** Recharge l'entrepot decisionnel — reserve a l'administrateur. */
  refresh: () => api.post<ApiResponse<{ lignes: number; dureeMs: number }>>('/analytics/refresh'),
};
