/**
 * Libelles humains des outils backend.
 *
 * Le nom technique (`PayrollLookupTool`) n'a aucun sens pour un utilisateur : il
 * lui faut savoir *ce qui se passe* pendant une generation qui peut occuper une
 * minute. C'est la difference entre une attente informee et un spinner fige.
 *
 * Un outil absent de cette table reste affichable : le repli produit un libelle
 * lisible plutot que de masquer l'etape.
 */
const TOOL_LABELS: Record<string, string> = {
  // Media plan
  BrandInfoTool: "Lecture de l'identité de la marque…",
  ListBrandsTool: 'Recherche des marques accessibles…',
  ProjectInfoTool: 'Consultation des projets en cours…',
  PreviousMediaPlansTool: 'Analyse des publications passées…',
  MediaPlanHistoryTool: "Relecture de l'historique éditorial…",
  RealizedContentTool: 'Revue des contenus déjà publiés…',
  MediaPlanGenerator: 'Rédaction du media plan…',
  MediaPlanPersistence: 'Enregistrement des publications…',
  GoogleDriveTool: 'Préparation du dossier Drive…',
  GoogleDriveLinkTool: 'Récupération du lien Drive…',

  // Relances clients
  InvoiceLookupTool: 'Recherche de la facture…',
  ListUnpaidInvoicesTool: 'Relevé des factures impayées…',
  EmailDraftTool: 'Rédaction de la relance…',

  // Paie
  PayrollLookupTool: 'Lecture du bulletin de paie…',
  ListEmployeesTool: 'Recherche de l’employé…',
};

/** Libelle affichable pendant l'execution. */
export function toolLabel(tool: string): string {
  const known = TOOL_LABELS[tool];
  if (known) return known;

  // Repli : « InvoiceLookupTool » → « Invoice Lookup… »
  const readable = tool
    .replace(/Tool$/, '')
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .trim();
  return readable ? `${readable}…` : 'Traitement…';
}

/** Libelle une fois l'etape terminee : le present progressif n'a plus lieu d'etre. */
export function toolLabelCompleted(tool: string): string {
  return toolLabel(tool).replace(/…$/, '');
}
