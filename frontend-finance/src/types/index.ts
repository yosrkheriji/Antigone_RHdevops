// =====================
// COMMUN
// =====================

export interface ApiResponse<T> {
  success: boolean;
  message: string | null;
  data: T;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  compteId: number;
  employeId: number;
  username: string;
  nom: string;
  prenom: string;
  email: string;
  roles: string[];
  permissions: string[];
  mustChangePassword: boolean;
  genre: string | null;
  message: string;
  imageUrl: string | null;
  token?: string;
  tokenExpiresAt?: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

// =====================
// EMPLOYÉ (vue paie) — /api/finance/employes
// =====================

export interface Employe {
  id: number;
  matricule: string | null;
  nom: string;
  prenom: string;
  email: string | null;
  cin: string | null;
  /** Numéro d'affiliation CNSS */
  cnss: string | null;
  /** RIB pour le virement du salaire */
  ribBancaire: string | null;
  poste: string | null;
  departement: string | null;
  typeContrat: string | null;
  dateEmbauche: string | null;
  dateFinContrat: string | null;
  salaire: number | null;
  modeSalaire: 'BRUT' | 'NET';
  archived: boolean | null;
  dateArchivage: string | null;
  /** true si le contrat couvre le mois interrogé */
  actifCeMois: boolean | null;
  /** true si CIVP / Freelance / Stage → aucune retenue */
  exonere: boolean;
}

// =====================
// PARAMÈTRES DE FACTURATION (référentiels)
// =====================

export interface ParametresFacturation {
  tvaDefaut: number;
  timbreFiscalDefaut: number;
  cyclesChargeFixe: number[];
  categoriesRevenu: string[];
  categoriesCharge: string[];
}

// =====================
// FINANCE — Paie / Charges / CNSS
// =====================

export type StatutPaie = 'IMPAYE' | 'PARTIEL' | 'PAYE';

export interface TrancheIrpp {
  plafond: number | null;
  taux: number;
}

export interface BaremeIrpp {
  id: number;
  effectiveFrom: string;
  tranches: TrancheIrpp[];
  notes: string | null;
}

export interface ParametresPaie {
  cnssSalarie: number;
  solidariteSalarie: number;
  cnssPatronale: number;
  tfp: number;
  foprolos: number;
  at: number;
  abattement: number;
  notes: string | null;
}

export interface ElementSalaire {
  id: string;
  label: string;
  type: 'gain' | 'deduction' | 'net_only';
  unit: 'montant' | 'jours';
  amount: number;
  enabled: boolean;
  affectsBrut: boolean;
  affectsCnss: boolean;
  affectsIrpp: boolean;
}

export interface FichePaie {
  salaireBrut: number;
  bonus: number;
  deductionAbsences: number;
  brutEffectif: number;
  cnssSalarie: number;
  salaireImposable: number;
  abattementMontant: number;
  revenuNetImposable: number;
  irppMensuel: number;
  solidariteSalarie: number;
  net: number;
  acompte: number;
  netAPayer: number;
  chargesEmployeur: number;
  coutTotal: number;
  cnssEmployeurDetail: number;
  tfpDetail: number;
  foprolosDetail: number;
  atDetail: number;
}

export interface BulletinPaie extends FichePaie {
  id: number;
  employeId: number;
  employeNom: string;
  employeMatricule: string;
  mois: string;
  elements: ElementSalaire[];
  statut: StatutPaie;
  datePaiement: string | null;
  dateCalcul: string;
}

export interface AcompteSalaire {
  id: number;
  employeId: number;
  mois: string;
  montant: number;
  date: string;
  note: string | null;
}

export interface TotauxPaie {
  mois: string;
  nbEmployes: number;
  masseBrute: number;
  masseNette: number;
  cnssSalarie: number;
  cnssEmployeur: number;
  cnssTotal: number;
  irppTotal: number;
  tfpTotal: number;
  foprolosTotal: number;
  coutTotal: number;
  netPaye: number;
  netRestant: number;
}

export interface ChargeFixe {
  id: number;
  label: string;
  montant: number;
  tauxTva: number | null;
  jourEcheance: number | null;
  cycleMois: number;
  archived: boolean;
  archivedAt: string | null;
  dateCreation: string;
  montantMensualise: number;
}

export interface ChargeFixeRequest {
  label: string;
  montant: number;
  tauxTva?: number | null;
  jourEcheance?: number | null;
  cycleMois: number;
}

export interface PaiementChargeFixe {
  id: number;
  chargeFixeId: number;
  mois: string;
  montant: number;
  datePaiement: string;
}

export interface ChargeVariable {
  id: number;
  mois: string;
  label: string;
  montant: number;
  tauxTva: number | null;
  date: string;
  categorie: string | null;
  description: string | null;
  montantHt: number;
  montantTva: number;
}

export type StatutChargeFixe = 'PAYEE' | 'PARTIELLE' | 'NON_PAYEE' | 'NON_DUE';

export interface EtatChargeFixe {
  id: number;
  label: string;
  montant: number;
  tauxTva: number | null;
  jourEcheance: number | null;
  cycleMois: number;
  dueCeMois: boolean;
  montantPaye: number;
  resteAPayer: number;
  statut: StatutChargeFixe;
  cumulImpaye: number;
  montantHt: number;
  montantTva: number;
}

export interface ResumeCharges {
  mois: string;
  totalFixesDues: number;
  totalFixesPayees: number;
  totalFixesRestantes: number;
  cumulImpayeAnterieur: number;
  totalVariables: number;
  totalCharges: number;
  tvaDeductible: number;
}

export interface DeclarationCnss {
  id: number;
  annee: number;
  trimestre: number;
  montantSalarie: number;
  montantEmployeur: number;
  montantPenalite: number;
  montantTotal: number;
  statut: StatutPaie;
  datePaiement: string | null;
}

// =====================
// FACTURATION & ENCAISSEMENTS
// =====================

export type TypeDocument = 'FACTURE' | 'DEVIS';
export type StatutFacture = 'EN_ATTENTE' | 'PARTIEL' | 'PAYEE';

export interface LigneDocument {
  designation: string;
  quantite: number;
  prixUnitaire: number;
  selectionnee?: boolean;
}

export interface Facture {
  id: number;
  numero: string;
  type: TypeDocument;
  clientId: number | null;
  clientNom: string | null;
  dateEmission: string;
  dateEcheance: string | null;
  lignes: LigneDocument[];
  totalHtManuel: number | null;
  totalHt: number;
  tauxTva: number;
  montantTva: number;
  timbreFiscal: number;
  totalTtc: number;
  montantPaye: number;
  montantRestant: number;
  statut: StatutFacture;
  paidAt: string | null;
  notes: string | null;
}

export interface FactureRequest {
  type: TypeDocument;
  clientId?: number | null;
  dateEmission?: string;
  dateEcheance?: string | null;
  lignes: LigneDocument[];
  totalHtManuel?: number | null;
  tauxTva?: number | null;
  timbreFiscal?: number | null;
  notes?: string | null;
}

export interface PaiementFacture {
  id: number;
  factureId: number;
  montant: number;
  datePaiement: string;
  note: string | null;
}

export interface ServiceCatalogue {
  id: number;
  designation: string;
  prixDefaut: number;
  categorie: string | null;
  description: string | null;
  actif: boolean;
}

export interface TemplateFacture {
  id: number;
  nom: string;
  type: TypeDocument;
  lignes: LigneDocument[];
  tauxTva: number | null;
  timbreFiscal: number | null;
  notes: string | null;
}

export interface RelanceClient {
  id: number;
  factureId: number;
  factureNumero: string;
  clientNom: string | null;
  dateRelance: string;
  envoyee: boolean;
  note: string | null;
}

export interface ContactClient {
  id: number;
  clientId: number;
  nom: string;
  poste: string | null;
  email: string | null;
  telephone: string | null;
  genre: string | null;
  notes: string | null;
}

export interface AutreRevenu {
  id: number;
  mois: string;
  label: string;
  montant: number;
  tauxTva: number;
  date: string;
  categorie: string | null;
  description: string | null;
  montantHt: number;
  montantTva: number;
}

// =====================
// DETTES
// =====================

export interface Dette {
  id: number;
  label: string;
  creancier: string | null;
  montantTotal: number;
  montantPaye: number;
  soldeRestant: number;
  dateDebut: string | null;
  dateEcheance: string | null;
  notes: string | null;
  soldee: boolean;
}

export interface DettePaiement {
  id: number;
  detteId: number;
  montant: number;
  datePaiement: string;
  note: string | null;
}

// =====================
// TVA & TABLEAUX DE BORD
// =====================

export interface DeclarationTva {
  mois: string;
  tvaCollecteeFactures: number;
  tvaCollecteeAutresRevenus: number;
  tvaCollectee: number;
  tvaDeductible: number;
  tvaNette: number;
  aReverser: boolean;
}

export interface VueEncaissements {
  mois: string;
  totalFacture: number;
  totalEncaisse: number;
  totalPending: number;
  totalRemaining: number;
  totalAutresRevenus: number;
  grandTotal: number;
  partEncaisse: number;
  partAutresRevenus: number;
}

export interface VueDecaissements {
  mois: string;
  masseBrute: number;
  masseNette: number;
  chargesPatronales: number;
  coutTotalSalaires: number;
  netRestantAPayer: number;
  netReporte: number;
  chargesFixesDues: number;
  chargesFixesPayees: number;
  chargesFixesRestantes: number;
  chargesVariables: number;
  tvaSurCharges: number;
  dettesSoldeRestant: number;
  irpp: number;
  tfp: number;
  foprolos: number;
  totalTaxesDues: number;
  totalDecaissements: number;
}

export interface ResultatNet {
  mois: string;
  totalRevenus: number;
  totalDepenses: number;
  resultatNet: number;
  margePourcent: number;
}

// Client (vue facturation) — /api/finance/clients
export interface Client {
  id: number;
  nom: string;
  email: string | null;
  telephone: string | null;
  adresse: string | null;
  matriculeFiscale: string | null;
  rne: string | null;
  cycleFacturation: number | null;
  emailReceiverNom: string | null;
  emailReceiverGenre: string | null;
  contactNom: string | null;
  contactEmail: string | null;
  contactTelephone: string | null;
}
