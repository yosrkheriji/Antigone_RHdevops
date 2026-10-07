import React, { useEffect, useMemo, useState } from 'react';
import {
  HiOutlineCash,
  HiOutlineCheckCircle,
  HiOutlinePlus,
  HiOutlineTrash,
  HiOutlineX,
  HiOutlineSparkles,
} from 'react-icons/hi';
import { useAiAssistant } from '../components/ai/AiAssistantContext';
import { facturationService } from '../api/facturationService';
import { referentielService } from '../api/referentielService';
import ErreurBanner from '../components/ui/ErreurBanner';
import { messageErreur } from '../utils/apiError';
import {
  Client,
  Facture,
  LigneDocument,
  ParametresFacturation,
  ServiceCatalogue,
  StatutFacture,
  TypeDocument,
} from '../types';

const fmt = (n: number | null | undefined) =>
  (n ?? 0).toLocaleString('fr-TN', { minimumFractionDigits: 3, maximumFractionDigits: 3 });

const STATUT_LABEL: Record<StatutFacture, string> = {
  EN_ATTENTE: 'En attente',
  PARTIEL: 'Partiel',
  PAYEE: 'Payée',
};

const statutBadge = (statut: StatutFacture) => {
  switch (statut) {
    case 'PAYEE':
      return 'bg-success-50 text-success-600 dark:bg-success-500/10 dark:text-success-400';
    case 'PARTIEL':
      return 'bg-warning-50 text-warning-600 dark:bg-warning-500/10 dark:text-warning-400';
    default:
      return 'bg-error-50 text-error-600 dark:bg-error-500/10 dark:text-error-400';
  }
};

const ligneVide = (): LigneDocument => ({ designation: '', quantite: 1, prixUnitaire: 0, selectionnee: true });

const FacturesPage: React.FC = () => {
  const [type, setType] = useState<TypeDocument>('FACTURE');
  const [documents, setDocuments] = useState<Facture[]>([]);
  const [clients, setClients] = useState<Client[]>([]);
  const [services, setServices] = useState<ServiceCatalogue[]>([]);
  const [parametres, setParametres] = useState<ParametresFacturation | null>(null);
  const [loading, setLoading] = useState(true);
  const [erreur, setErreur] = useState<string | null>(null);

  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState({
    clientId: '' as string,
    dateEmission: new Date().toISOString().slice(0, 10),
    dateEcheance: '',
    lignes: [ligneVide()] as LigneDocument[],
    totalHtManuel: '',
    tauxTva: '',
    timbreFiscal: '',
    notes: '',
  });

  const assistant = useAiAssistant();
  const [paiementTarget, setPaiementTarget] = useState<Facture | null>(null);
  const [paiementMontant, setPaiementMontant] = useState('');

  const loadData = async () => {
    setLoading(true);
    setErreur(null);
    try {
      const [docRes, cliRes, srvRes, parRes] = await Promise.all([
        facturationService.getByType(type),
        referentielService.getClients(),
        facturationService.getServices(),
        referentielService.getParametresFacturation(),
      ]);
      setDocuments(docRes.data.data || []);
      setClients(cliRes.data.data || []);
      setServices(srvRes.data.data || []);
      setParametres(parRes.data.data || null);
    } catch (e) {
      setErreur(messageErreur(e));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [type]);

  const estDevis = type === 'DEVIS';

  const clientSelectionne = useMemo(
    () => clients.find((c) => String(c.id) === form.clientId) ?? null,
    [clients, form.clientId],
  );

  /**
   * Aperçu live de la tarification, identique au calcul serveur :
   * HT = max(Σ lignes, HT manuel) ; TVA et timbre nuls pour un devis.
   */
  const apercu = useMemo(() => {
    const sommeLignes = form.lignes
      .filter((l) => l.selectionnee !== false)
      .reduce((s, l) => s + (Number(l.quantite) || 0) * (Number(l.prixUnitaire) || 0), 0);
    const ht = Math.max(sommeLignes, Number(form.totalHtManuel) || 0);
    const taux = estDevis ? 0 : Number(form.tauxTva) || 0;
    const tva = estDevis ? 0 : (ht * taux) / 100;
    const timbre = estDevis ? 0 : Number(form.timbreFiscal) || 0;
    return { sommeLignes, ht, tva, timbre, ttc: ht + tva + timbre };
  }, [form, estDevis]);

  const openCreate = () => {
    setEditingId(null);
    setForm({
      clientId: '',
      dateEmission: new Date().toISOString().slice(0, 10),
      dateEcheance: '',
      lignes: [ligneVide()],
      totalHtManuel: '',
      tauxTva: String(parametres?.tvaDefaut ?? ''),
      timbreFiscal: String(parametres?.timbreFiscalDefaut ?? ''),
      notes: '',
    });
    setModalOpen(true);
  };

  const openEdit = (doc: Facture) => {
    setEditingId(doc.id);
    setForm({
      clientId: doc.clientId ? String(doc.clientId) : '',
      dateEmission: doc.dateEmission,
      dateEcheance: doc.dateEcheance ?? '',
      lignes: doc.lignes.length ? doc.lignes : [ligneVide()],
      totalHtManuel: doc.totalHtManuel != null ? String(doc.totalHtManuel) : '',
      tauxTva: String(doc.tauxTva ?? parametres?.tvaDefaut ?? ''),
      timbreFiscal: String(doc.timbreFiscal ?? parametres?.timbreFiscalDefaut ?? ''),
      notes: doc.notes ?? '',
    });
    setModalOpen(true);
  };

  const submit = async () => {
    const payload = {
      type,
      clientId: form.clientId ? Number(form.clientId) : null,
      dateEmission: form.dateEmission,
      dateEcheance: form.dateEcheance || null,
      lignes: form.lignes.filter((l) => l.designation.trim()),
      totalHtManuel: form.totalHtManuel ? Number(form.totalHtManuel) : null,
      tauxTva: estDevis ? null : Number(form.tauxTva),
      timbreFiscal: estDevis ? null : Number(form.timbreFiscal),
      notes: form.notes || null,
    };
    try {
      if (editingId) await facturationService.update(editingId, payload);
      else await facturationService.create(payload);
      setModalOpen(false);
      await loadData();
    } catch (e: any) {
      alert(e?.response?.data?.message || 'Erreur');
    }
  };

  const remove = async (id: number) => {
    if (!confirm('Supprimer ce document ?')) return;
    await facturationService.remove(id);
    await loadData();
  };

  const solder = async (id: number) => {
    await facturationService.marquerPayee(id);
    await loadData();
  };

  const submitPaiement = async () => {
    if (!paiementTarget) return;
    const montant = Number(paiementMontant);
    if (!montant || montant <= 0) return;
    try {
      await facturationService.enregistrerPaiement(paiementTarget.id, montant);
      setPaiementTarget(null);
      setPaiementMontant('');
      await loadData();
    } catch (e: any) {
      alert(e?.response?.data?.message || 'Erreur');
    }
  };

  const setLigne = (index: number, patch: Partial<LigneDocument>) => {
    setForm((f) => ({
      ...f,
      lignes: f.lignes.map((l, i) => (i === index ? { ...l, ...patch } : l)),
    }));
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-title-sm font-extrabold tracking-tight text-transparent bg-clip-text bg-gradient-to-r from-gray-900 to-[#683b77] dark:from-white dark:to-[#ab78c3]">
            {estDevis ? 'Devis' : 'Factures'}
          </h1>
          <p className="text-theme-sm text-gray-500 dark:text-gray-400 mt-1">
            {estDevis
              ? 'Les devis ne portent ni TVA ni timbre fiscal'
              : parametres
                ? `TVA ${parametres.tvaDefaut} % et timbre fiscal ${fmt(parametres.timbreFiscalDefaut)} DT par défaut`
                : 'Chargement des paramètres de facturation...'}
          </p>
        </div>

        <div className="flex items-center gap-2">
          <div className="flex rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden">
            {(['FACTURE', 'DEVIS'] as TypeDocument[]).map((t) => (
              <button
                key={t}
                onClick={() => setType(t)}
                className={`px-4 py-2 text-theme-xs font-semibold ${
                  type === t
                    ? 'bg-brand-500 text-white'
                    : 'bg-transparent text-gray-500 hover:bg-gray-50 dark:text-gray-400 dark:hover:bg-gray-800'
                }`}
              >
                {t === 'FACTURE' ? 'Factures' : 'Devis'}
              </button>
            ))}
          </div>
          <button
            onClick={openCreate}
            className="flex items-center gap-2 rounded-lg bg-brand-500 px-4 py-2.5 text-theme-sm font-semibold text-white hover:bg-brand-600"
          >
            <HiOutlinePlus size={16} /> Nouveau
          </button>
        </div>
      </div>

      {erreur && <ErreurBanner message={erreur} onRetry={loadData} />}

      <div className="overflow-x-auto rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-gray-dark">
        <table className="w-full text-left text-theme-sm">
          <thead className="bg-gray-50 dark:bg-gray-800/50 text-theme-xs text-gray-500 dark:text-gray-400">
            <tr>
              <th className="px-4 py-3 font-semibold">Numéro</th>
              <th className="px-4 py-3 font-semibold">Client</th>
              <th className="px-4 py-3 font-semibold">Émission</th>
              <th className="px-4 py-3 font-semibold text-right">HT</th>
              {!estDevis && <th className="px-4 py-3 font-semibold text-right">TVA</th>}
              {!estDevis && <th className="px-4 py-3 font-semibold text-right">Timbre</th>}
              <th className="px-4 py-3 font-semibold text-right">TTC</th>
              {!estDevis && <th className="px-4 py-3 font-semibold text-right">Payé</th>}
              {!estDevis && <th className="px-4 py-3 font-semibold text-right">Restant</th>}
              {!estDevis && <th className="px-4 py-3 font-semibold">Statut</th>}
              <th className="px-4 py-3 font-semibold text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={11} className="px-4 py-10 text-center text-gray-400">Chargement...</td></tr>
            ) : documents.length === 0 ? (
              <tr><td colSpan={11} className="px-4 py-10 text-center text-gray-400">Aucun document</td></tr>
            ) : (
              documents.map((d) => (
                <tr key={d.id} className="border-t border-gray-100 dark:border-gray-800">
                  <td
                    className="px-4 py-3 cursor-pointer font-semibold text-gray-800 dark:text-white"
                    onClick={() => openEdit(d)}
                  >
                    {d.numero}
                  </td>
                  <td className="px-4 py-3">{d.clientNom || '—'}</td>
                  <td className="px-4 py-3">{d.dateEmission}</td>
                  <td className="px-4 py-3 text-right">{fmt(d.totalHt)}</td>
                  {!estDevis && <td className="px-4 py-3 text-right text-gray-500 dark:text-gray-400">{fmt(d.montantTva)}</td>}
                  {!estDevis && <td className="px-4 py-3 text-right text-gray-500 dark:text-gray-400">{fmt(d.timbreFiscal)}</td>}
                  <td className="px-4 py-3 text-right font-semibold">{fmt(d.totalTtc)}</td>
                  {!estDevis && <td className="px-4 py-3 text-right">{fmt(d.montantPaye)}</td>}
                  {!estDevis && (
                    <td className={`px-4 py-3 text-right ${d.montantRestant > 0 ? 'font-semibold text-error-500' : 'text-gray-400'}`}>
                      {fmt(d.montantRestant)}
                    </td>
                  )}
                  {!estDevis && (
                    <td className="px-4 py-3">
                      <span className={`rounded-full px-2.5 py-1 text-theme-xs font-semibold ${statutBadge(d.statut)}`}>
                        {STATUT_LABEL[d.statut]}
                      </span>
                    </td>
                  )}
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-2">
                      {!estDevis && d.statut !== 'PAYEE' && (
                        <>
                          <button
                            onClick={() => { setPaiementTarget(d); setPaiementMontant(String(d.montantRestant)); }}
                            className="rounded-lg border border-gray-200 dark:border-gray-700 p-2 text-gray-500 hover:bg-gray-50 dark:hover:bg-gray-800"
                            title="Enregistrer un paiement"
                          >
                            <HiOutlineCash size={15} />
                          </button>
                          <button
                            onClick={() => solder(d.id)}
                            className="rounded-lg bg-success-500 p-2 text-white hover:bg-success-600"
                            title="Solder la facture"
                          >
                            <HiOutlineCheckCircle size={15} />
                          </button>
                          {/* Ouvre l'assistant avec la demande deja formulee : le
                              numero de facture est sous les yeux de l'utilisateur,
                              le lui faire retaper n'aurait aucun interet. */}
                          {assistant && (
                            <button
                              onClick={() =>
                                assistant.askAssistant(
                                  `Génère une relance pour la facture ${d.numero}`,
                                )
                              }
                              className="rounded-lg border border-brand-200 dark:border-brand-500/40 p-2 text-brand-500 hover:bg-brand-50 dark:hover:bg-brand-500/10"
                              title="Générer une relance avec l'assistant IA"
                            >
                              <HiOutlineSparkles size={15} />
                            </button>
                          )}
                        </>
                      )}
                      <button
                        onClick={() => remove(d.id)}
                        className="rounded-lg border border-gray-200 dark:border-gray-700 p-2 text-error-500 hover:bg-error-50 dark:hover:bg-error-500/10"
                      >
                        <HiOutlineTrash size={15} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Modal création / édition */}
      {modalOpen && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/40 backdrop-blur-sm p-4 overflow-y-auto">
          <div className="my-8 w-full max-w-2xl rounded-2xl bg-white p-6 shadow-2xl dark:bg-gray-800">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="text-base font-bold text-gray-900 dark:text-white">
                {editingId ? 'Modifier' : 'Nouveau'} {estDevis ? 'devis' : 'facture'}
              </h3>
              <button onClick={() => setModalOpen(false)} className="text-gray-400 hover:text-gray-600">
                <HiOutlineX size={18} />
              </button>
            </div>

            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <div>
                <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">Client</label>
                <select
                  value={form.clientId}
                  onChange={(e) => setForm({ ...form, clientId: e.target.value })}
                  className="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-2 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                >
                  <option value="">— Aucun —</option>
                  {clients.map((c) => (
                    <option key={c.id} value={c.id}>{c.nom}</option>
                  ))}
                </select>
                {clients.length === 0 && !loading && !erreur && (
                  <p className="mt-1 text-[11px] text-warning-500">Aucun client enregistré en base</p>
                )}
              </div>
              <div>
                <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">Émission</label>
                <input
                  type="date"
                  value={form.dateEmission}
                  onChange={(e) => setForm({ ...form, dateEmission: e.target.value })}
                  className="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                />
              </div>
              <div>
                <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">Échéance</label>
                <input
                  type="date"
                  value={form.dateEcheance}
                  onChange={(e) => setForm({ ...form, dateEcheance: e.target.value })}
                  className="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                />
              </div>
            </div>

            {/* Identité fiscale du client sélectionné */}
            {clientSelectionne && (
              <div className="mt-3 rounded-xl bg-gray-50 p-3 dark:bg-gray-700/40">
                <div className="grid grid-cols-2 gap-x-4 gap-y-1 text-theme-xs sm:grid-cols-4">
                  {[
                    ['Matricule fiscal', clientSelectionne.matriculeFiscale],
                    ['RNE', clientSelectionne.rne],
                    ['Email', clientSelectionne.email],
                    ['Téléphone', clientSelectionne.telephone],
                    ['Adresse', clientSelectionne.adresse],
                    ['Contact', clientSelectionne.contactNom],
                    [
                      'Cycle de facturation',
                      clientSelectionne.cycleFacturation
                        ? `Tous les ${clientSelectionne.cycleFacturation} mois`
                        : null,
                    ],
                  ].map(([label, value]) => (
                    <div key={label as string}>
                      <p className="text-gray-400">{label}</p>
                      <p className="text-gray-700 dark:text-gray-200">{value || '—'}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Lignes */}
            <div className="mt-4">
              <div className="mb-2 flex items-center justify-between">
                <label className="text-theme-xs font-semibold text-gray-500 dark:text-gray-400">Lignes de prestation</label>
                <button
                  onClick={() => setForm({ ...form, lignes: [...form.lignes, ligneVide()] })}
                  className="text-theme-xs font-semibold text-brand-500 hover:text-brand-600"
                >
                  + Ajouter une ligne
                </button>
              </div>
              <div className="space-y-2">
                {form.lignes.map((l, i) => (
                  <div key={i} className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      checked={l.selectionnee !== false}
                      onChange={(e) => setLigne(i, { selectionnee: e.target.checked })}
                      title="Inclure dans le total HT"
                      className="h-4 w-4 shrink-0 accent-[var(--brand)]"
                    />
                    <input
                      list="services-catalogue"
                      value={l.designation}
                      onChange={(e) => {
                        const service = services.find((s) => s.designation === e.target.value);
                        setLigne(i, {
                          designation: e.target.value,
                          ...(service ? { prixUnitaire: service.prixDefaut } : {}),
                        });
                      }}
                      placeholder="Désignation"
                      className="h-9 flex-1 rounded-lg border border-gray-300 bg-transparent px-3 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                    />
                    <input
                      type="number"
                      value={l.quantite}
                      onChange={(e) => setLigne(i, { quantite: Number(e.target.value) })}
                      placeholder="Qté"
                      className="h-9 w-16 rounded-lg border border-gray-300 bg-transparent px-2 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                    />
                    <input
                      type="number"
                      value={l.prixUnitaire}
                      onChange={(e) => setLigne(i, { prixUnitaire: Number(e.target.value) })}
                      placeholder="P.U."
                      className="h-9 w-24 rounded-lg border border-gray-300 bg-transparent px-2 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                    />
                    <span className="w-24 shrink-0 text-right text-theme-xs text-gray-500 dark:text-gray-400">
                      {fmt((Number(l.quantite) || 0) * (Number(l.prixUnitaire) || 0))}
                    </span>
                    <button
                      onClick={() => setForm({ ...form, lignes: form.lignes.filter((_, j) => j !== i) })}
                      className="shrink-0 text-error-500 hover:text-error-600"
                    >
                      <HiOutlineTrash size={15} />
                    </button>
                  </div>
                ))}
              </div>
              <datalist id="services-catalogue">
                {services.map((s) => (
                  <option key={s.id} value={s.designation} />
                ))}
              </datalist>
            </div>

            {/* Tarification */}
            <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
              <div>
                <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">
                  Total HT manuel
                </label>
                <input
                  type="number"
                  value={form.totalHtManuel}
                  onChange={(e) => setForm({ ...form, totalHtManuel: e.target.value })}
                  className="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                />
                <p className="mt-1 text-[11px] text-gray-400">Plancher : le HT retenu est le plus élevé des deux</p>
              </div>
              {!estDevis && (
                <div>
                  <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">TVA (%)</label>
                  <input
                    type="number"
                    value={form.tauxTva}
                    onChange={(e) => setForm({ ...form, tauxTva: e.target.value })}
                    className="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                  />
                </div>
              )}
              {!estDevis && (
                <div>
                  <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">Timbre fiscal</label>
                  <input
                    type="number"
                    step="0.001"
                    value={form.timbreFiscal}
                    onChange={(e) => setForm({ ...form, timbreFiscal: e.target.value })}
                    className="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                  />
                </div>
              )}
            </div>

            {/* Aperçu */}
            <div className="mt-4 rounded-xl bg-gray-50 p-4 dark:bg-gray-700/40">
              <div className="space-y-1 text-theme-sm">
                <div className="flex justify-between text-gray-500 dark:text-gray-400">
                  <span>Σ lignes sélectionnées</span><span>{fmt(apercu.sommeLignes)} DT</span>
                </div>
                <div className="flex justify-between text-gray-700 dark:text-gray-200">
                  <span>Total HT retenu</span><span>{fmt(apercu.ht)} DT</span>
                </div>
                {!estDevis && (
                  <>
                    <div className="flex justify-between text-gray-500 dark:text-gray-400">
                      <span>TVA</span><span>{fmt(apercu.tva)} DT</span>
                    </div>
                    <div className="flex justify-between text-gray-500 dark:text-gray-400">
                      <span>Timbre fiscal</span><span>{fmt(apercu.timbre)} DT</span>
                    </div>
                  </>
                )}
                <div className="flex justify-between border-t border-gray-200 pt-1.5 font-bold text-gray-800 dark:border-gray-600 dark:text-white">
                  <span>Total TTC</span><span>{fmt(apercu.ttc)} DT</span>
                </div>
              </div>
            </div>

            <div className="mt-4">
              <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">Notes</label>
              <textarea
                value={form.notes}
                onChange={(e) => setForm({ ...form, notes: e.target.value })}
                rows={2}
                className="w-full rounded-lg border border-gray-300 bg-transparent px-3 py-2 text-theme-sm dark:border-gray-600 dark:text-gray-200"
              />
            </div>

            <div className="mt-5 flex gap-2">
              <button
                onClick={() => setModalOpen(false)}
                className="flex-1 rounded-xl border border-gray-200 px-4 py-2.5 text-theme-sm font-medium text-gray-600 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-700"
              >
                Annuler
              </button>
              <button
                onClick={submit}
                className="flex-1 rounded-xl bg-brand-500 px-4 py-2.5 text-theme-sm font-semibold text-white hover:bg-brand-600"
              >
                Enregistrer
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal paiement */}
      {paiementTarget && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
          <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-2xl dark:bg-gray-800">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="text-base font-bold text-gray-900 dark:text-white">
                Paiement — {paiementTarget.numero}
              </h3>
              <button onClick={() => setPaiementTarget(null)} className="text-gray-400 hover:text-gray-600">
                <HiOutlineX size={18} />
              </button>
            </div>
            <p className="mb-3 text-theme-sm text-gray-500 dark:text-gray-400">
              Restant à encaisser : <strong>{fmt(paiementTarget.montantRestant)} DT</strong>
            </p>
            <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">Montant (DT)</label>
            <input
              type="number"
              value={paiementMontant}
              onChange={(e) => setPaiementMontant(e.target.value)}
              className="mb-4 h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-theme-sm dark:border-gray-600 dark:text-gray-200"
              autoFocus
            />
            <div className="flex gap-2">
              <button
                onClick={() => setPaiementTarget(null)}
                className="flex-1 rounded-xl border border-gray-200 px-4 py-2.5 text-theme-sm font-medium text-gray-600 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-700"
              >
                Annuler
              </button>
              <button
                onClick={submitPaiement}
                className="flex-1 rounded-xl bg-brand-500 px-4 py-2.5 text-theme-sm font-semibold text-white hover:bg-brand-600"
              >
                Enregistrer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default FacturesPage;
