import api from './axios';

/* ── Types — miroir de PresenceDashboardDTO (backend, package com.antigone.rh.bi) ── */

export interface PresenceKpis {
  joursSuivis: number;
  effectifSuivi: number;
  tauxPresence: number;
  tauxAbsenteisme: number;
  tauxTeletravail: number;
  heuresTravaillees: number;
  retardMoyenMinutes: number;
  joursRetard: number;
}

export interface PointMensuelPresence {
  mois: string;
  libelle: string;
  tauxPresence: number;
  tauxAbsenteisme: number;
  heures: number;
}

export interface RepartitionStatut {
  statut: string;
  jours: number;
}

export interface LigneDepartement {
  departement: string;
  effectif: number;
  tauxPresence: number;
  retardMoyenMinutes: number;
  heures: number;
}

export interface LigneEmployeRetard {
  employe: string;
  departement: string;
  retardTotalMinutes: number;
  joursRetard: number;
  heures: number;
}

export interface PointInactivite {
  semaine: string;
  minutesExcedentaires: number;
  montantDeduction: number;
}

export interface LigneConge {
  typeConge: string;
  jours: number;
  demandes: number;
}

export interface PresenceDashboard {
  kpis: PresenceKpis;
  evolutionMensuelle: PointMensuelPresence[];
  repartitionStatuts: RepartitionStatut[];
  parDepartement: LigneDepartement[];
  topRetards: LigneEmployeRetard[];
  inactivite: PointInactivite[];
  conges: LigneConge[];
}

export interface EtatEntrepot {
  dernierChargement: string | null;
  dureeMs: number | null;
  lignesChargees: number | null;
  statut: string;
  message: string | null;
  volumes: { table: string; lignes: number }[];
}

interface Enveloppe<T> {
  success: boolean;
  message?: string;
  data: T;
}

export const analyticsService = {
  getPresence: (debut: string, fin: string, departement?: string) =>
    api.get<Enveloppe<PresenceDashboard>>('/analytics/presence', {
      params: { debut, fin, ...(departement ? { departement } : {}) },
    }),

  getDepartements: () => api.get<Enveloppe<string[]>>('/analytics/departements'),

  getEtat: () => api.get<Enveloppe<EtatEntrepot>>('/analytics/etat'),

  /** Recharge l'entrepot decisionnel — reserve a l'administrateur. */
  refresh: () => api.post<Enveloppe<{ lignes: number; dureeMs: number }>>('/analytics/refresh'),
};
