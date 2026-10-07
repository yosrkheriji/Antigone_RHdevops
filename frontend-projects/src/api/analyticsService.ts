import api from './axios';

/* ── Types — miroir de ProjetsDashboardDTO (backend, package com.antigone.rh.bi) ── */

export interface ProjetsKpis {
  projetsTotal: number;
  projetsEnCours: number;
  projetsClotures: number;
  tachesTotal: number;
  tachesTerminees: number;
  tachesEnRetard: number;
  tauxCompletion: number;
  delaiMoyenTacheJours: number;
  dureeMoyenneProjetJours: number;
}

export interface RepartitionStatut {
  statut: string;
  nombre: number;
}

export interface PointMensuelProjets {
  mois: string;
  libelle: string;
  tachesCreees: number;
  tachesTerminees: number;
  tachesEnRetard: number;
}

export interface LigneCollaborateur {
  employe: string;
  departement: string;
  tachesTotal: number;
  tachesTerminees: number;
  tachesEnRetard: number;
  tauxCompletion: number;
  delaiMoyenJours: number;
}

export interface ProjetARisque {
  projet: string;
  client: string;
  statut: string;
  dateFin: string | null;
  tachesTotal: number;
  tachesEnRetard: number;
  tauxCompletion: number;
}

export interface LigneDelai {
  projet: string;
  delaiMoyenJours: number;
  ecartMoyenJours: number;
  tachesTerminees: number;
}

export interface ProjetsDashboard {
  kpis: ProjetsKpis;
  repartitionProjets: RepartitionStatut[];
  evolutionMensuelle: PointMensuelProjets[];
  chargeParCollaborateur: LigneCollaborateur[];
  projetsARisque: ProjetARisque[];
  delaiParProjet: LigneDelai[];
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
  getProjets: (debut: string, fin: string) =>
    api.get<Enveloppe<ProjetsDashboard>>('/analytics/projets', { params: { debut, fin } }),

  getEtat: () => api.get<Enveloppe<EtatEntrepot>>('/analytics/etat'),

  /** Recharge l'entrepot decisionnel — reserve a l'administrateur. */
  refresh: () => api.post<Enveloppe<{ lignes: number; dureeMs: number }>>('/analytics/refresh'),
};
