import React, { useEffect, useState } from 'react';
import {
  HiOutlineChevronLeft,
  HiOutlineChevronRight,
  HiOutlinePlus,
  HiOutlineTrash,
  HiOutlineX,
  HiOutlineCheckCircle,
  HiOutlineArchive,
} from 'react-icons/hi';
import { chargesService } from '../api/chargesService';
import { referentielService } from '../api/referentielService';
import ErreurBanner from '../components/ui/ErreurBanner';
import { messageErreur } from '../utils/apiError';
import { ChargeFixeRequest, ChargeVariable, EtatChargeFixe, ResumeCharges, StatutChargeFixe } from '../types';

const fmt = (n: number | null | undefined) =>
  (n ?? 0).toLocaleString('fr-TN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const currentMois = () => new Date().toISOString().slice(0, 7);

const emptyFixeForm: ChargeFixeRequest = { label: '', montant: 0, tauxTva: null, jourEcheance: null, cycleMois: 1 };

const STATUT_LABEL: Record<StatutChargeFixe, string> = {
  PAYEE: 'Payée',
  PARTIELLE: 'Partielle',
  NON_PAYEE: 'Non payée',
  NON_DUE: 'Non due',
};

const statutBadge = (statut: StatutChargeFixe) => {
  switch (statut) {
    case 'PAYEE':
      return 'bg-success-50 text-success-600 dark:bg-success-500/10 dark:text-success-400';
    case 'PARTIELLE':
      return 'bg-warning-50 text-warning-600 dark:bg-warning-500/10 dark:text-warning-400';
    case 'NON_PAYEE':
      return 'bg-error-50 text-error-600 dark:bg-error-500/10 dark:text-error-400';
    default:
      return 'bg-gray-100 text-gray-500 dark:bg-gray-700/50 dark:text-gray-400';
  }
};

const ChargesPage: React.FC = () => {
  const [mois, setMois] = useState(currentMois());
  const [etats, setEtats] = useState<EtatChargeFixe[]>([]);
  const [variables, setVariables] = useState<ChargeVariable[]>([]);
  const [resume, setResume] = useState<ResumeCharges | null>(null);
  const [cycles, setCycles] = useState<number[]>([]);
  const [categories, setCategories] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [erreur, setErreur] = useState<string | null>(null);

  const [fixeModalOpen, setFixeModalOpen] = useState(false);
  const [fixeForm, setFixeForm] = useState<ChargeFixeRequest>(emptyFixeForm);
  const [editingFixeId, setEditingFixeId] = useState<number | null>(null);

  const [variableModalOpen, setVariableModalOpen] = useState(false);
  const [variableForm, setVariableForm] = useState({ label: '', montant: '', tauxTva: '', categorie: '' });

  const loadData = async () => {
    setLoading(true);
    setErreur(null);
    try {
      const [etatRes, varRes, resumeRes, parRes] = await Promise.all([
        chargesService.getEtatFixes(mois),
        chargesService.getVariables(mois),
        chargesService.getResume(mois),
        referentielService.getParametresFacturation(),
      ]);
      setEtats(etatRes.data.data || []);
      setVariables(varRes.data.data || []);
      setResume(resumeRes.data.data || null);
      setCycles(parRes.data.data?.cyclesChargeFixe || []);
      setCategories(parRes.data.data?.categoriesCharge || []);
    } catch (e) {
      setErreur(messageErreur(e));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mois]);

  const changeMois = (delta: number) => {
    const [y, m] = mois.split('-').map(Number);
    const d = new Date(y, m - 1 + delta, 1);
    setMois(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
  };

  const openCreateFixe = () => {
    setEditingFixeId(null);
    setFixeForm(emptyFixeForm);
    setFixeModalOpen(true);
  };

  const openEditFixe = (c: EtatChargeFixe) => {
    setEditingFixeId(c.id);
    setFixeForm({ label: c.label, montant: c.montant, tauxTva: c.tauxTva, jourEcheance: c.jourEcheance, cycleMois: c.cycleMois });
    setFixeModalOpen(true);
  };

  const submitFixe = async () => {
    if (!fixeForm.label.trim() || !fixeForm.montant) return;
    try {
      if (editingFixeId) await chargesService.updateFixe(editingFixeId, fixeForm);
      else await chargesService.createFixe(fixeForm);
      setFixeModalOpen(false);
      await loadData();
    } catch (e: any) {
      alert(e?.response?.data?.message || 'Erreur');
    }
  };

  const archiveFixe = async (id: number) => {
    if (!confirm('Archiver cette charge fixe ?')) return;
    await chargesService.archiveFixe(id);
    await loadData();
  };

  const payerFixe = async (c: EtatChargeFixe) => {
    try {
      await chargesService.enregistrerPaiement(c.id, mois, c.resteAPayer > 0 ? c.resteAPayer : c.montant);
      await loadData();
    } catch (e: any) {
      alert(e?.response?.data?.message || 'Erreur');
    }
  };

  const submitVariable = async () => {
    const montant = parseFloat(variableForm.montant);
    if (!variableForm.label.trim() || !montant) return;
    try {
      await chargesService.createVariable({
        mois,
        label: variableForm.label,
        montant,
        tauxTva: variableForm.tauxTva ? parseFloat(variableForm.tauxTva) : null,
        categorie: variableForm.categorie || null,
      });
      setVariableModalOpen(false);
      setVariableForm({ label: '', montant: '', tauxTva: '', categorie: '' });
      await loadData();
    } catch (e: any) {
      alert(e?.response?.data?.message || 'Erreur');
    }
  };

  const deleteVariable = async (id: number) => {
    if (!confirm('Supprimer cette charge ?')) return;
    await chargesService.deleteVariable(id);
    await loadData();
  };

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-title-sm font-extrabold tracking-tight text-transparent bg-clip-text bg-gradient-to-r from-gray-900 to-[#683b77] dark:from-white dark:to-[#ab78c3]">
            Charges
          </h1>
          <p className="text-theme-sm text-gray-500 dark:text-gray-400 mt-1">Charges fixes récurrentes et charges ponctuelles de l'agence</p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={() => changeMois(-1)} className="p-2 rounded-lg border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800">
            <HiOutlineChevronLeft size={16} />
          </button>
          <span className="text-theme-sm font-semibold text-gray-800 dark:text-white min-w-[110px] text-center">{mois}</span>
          <button onClick={() => changeMois(1)} className="p-2 rounded-lg border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800">
            <HiOutlineChevronRight size={16} />
          </button>
        </div>
      </div>

      {erreur && <ErreurBanner message={erreur} onRetry={loadData} />}

      {/* Synthèse */}
      {resume && (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
          {[
            { label: 'Total charges', value: resume.totalCharges },
            { label: 'Fixes dues', value: resume.totalFixesDues },
            { label: 'Fixes payées', value: resume.totalFixesPayees },
            { label: 'Reste à payer', value: resume.totalFixesRestantes },
            { label: 'Cumul impayé', value: resume.cumulImpayeAnterieur },
            { label: 'TVA déductible', value: resume.tvaDeductible },
          ].map((item) => (
            <div key={item.label} className="rounded-2xl border border-gray-200 bg-white p-4 dark:border-gray-800 dark:bg-gray-dark">
              <p className="text-theme-xs text-gray-400">{item.label}</p>
              <p className="mt-1 text-theme-sm font-bold text-gray-800 dark:text-white">{fmt(item.value)} DT</p>
            </div>
          ))}
        </div>
      )}

      {/* Charges fixes */}
      <div>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-theme-md font-bold text-gray-800 dark:text-white">Charges fixes</h2>
          <button
            onClick={openCreateFixe}
            className="flex items-center gap-2 rounded-lg bg-brand-500 px-3 py-2 text-theme-xs font-semibold text-white hover:bg-brand-600"
          >
            <HiOutlinePlus size={14} /> Ajouter
          </button>
        </div>
        <div className="overflow-x-auto rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-gray-dark">
          <table className="w-full text-left text-theme-sm">
            <thead className="bg-gray-50 dark:bg-gray-800/50 text-theme-xs text-gray-500 dark:text-gray-400">
              <tr>
                <th className="px-4 py-3 font-semibold">Libellé</th>
                <th className="px-4 py-3 font-semibold">Cycle</th>
                <th className="px-4 py-3 font-semibold">Échéance</th>
                <th className="px-4 py-3 font-semibold text-right">Montant TTC</th>
                <th className="px-4 py-3 font-semibold text-right">HT</th>
                <th className="px-4 py-3 font-semibold text-right">Payé</th>
                <th className="px-4 py-3 font-semibold text-right">Reste</th>
                <th className="px-4 py-3 font-semibold text-right">Cumul impayé</th>
                <th className="px-4 py-3 font-semibold">Statut</th>
                <th className="px-4 py-3 font-semibold text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={10} className="px-4 py-8 text-center text-gray-400">Chargement...</td></tr>
              ) : etats.length === 0 ? (
                <tr><td colSpan={10} className="px-4 py-8 text-center text-gray-400">Aucune charge fixe</td></tr>
              ) : (
                etats.map((c) => (
                  <tr key={c.id} className={`border-t border-gray-100 dark:border-gray-800 ${!c.dueCeMois ? 'opacity-55' : ''}`}>
                    <td className="px-4 py-3 cursor-pointer font-medium text-gray-800 dark:text-white" onClick={() => openEditFixe(c)}>
                      {c.label}
                    </td>
                    <td className="px-4 py-3">{c.cycleMois === 1 ? 'Mensuel' : `Tous les ${c.cycleMois} mois`}</td>
                    <td className="px-4 py-3">{c.jourEcheance ? `Le ${c.jourEcheance}` : '—'}</td>
                    <td className="px-4 py-3 text-right">{fmt(c.montant)}</td>
                    <td className="px-4 py-3 text-right text-gray-500 dark:text-gray-400">{fmt(c.montantHt)}</td>
                    <td className="px-4 py-3 text-right">{fmt(c.montantPaye)}</td>
                    <td className="px-4 py-3 text-right font-semibold">{fmt(c.resteAPayer)}</td>
                    <td className={`px-4 py-3 text-right ${c.cumulImpaye > 0 ? 'font-semibold text-error-500' : 'text-gray-400'}`}>
                      {fmt(c.cumulImpaye)}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`rounded-full px-2.5 py-1 text-theme-xs font-semibold ${statutBadge(c.statut)}`}>
                        {STATUT_LABEL[c.statut]}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end gap-2">
                        {c.dueCeMois && c.resteAPayer > 0 && (
                          <button
                            onClick={() => payerFixe(c)}
                            className="rounded-lg bg-success-500 p-2 text-white hover:bg-success-600"
                            title={`Payer le reste (${fmt(c.resteAPayer)} DT)`}
                          >
                            <HiOutlineCheckCircle size={15} />
                          </button>
                        )}
                        <button
                          onClick={() => archiveFixe(c.id)}
                          className="rounded-lg border border-gray-200 dark:border-gray-700 p-2 text-gray-500 hover:bg-gray-50 dark:hover:bg-gray-800"
                          title="Archiver"
                        >
                          <HiOutlineArchive size={15} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Charges variables */}
      <div>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-theme-md font-bold text-gray-800 dark:text-white">Charges variables — {mois}</h2>
          <button
            onClick={() => setVariableModalOpen(true)}
            className="flex items-center gap-2 rounded-lg bg-brand-500 px-3 py-2 text-theme-xs font-semibold text-white hover:bg-brand-600"
          >
            <HiOutlinePlus size={14} /> Ajouter
          </button>
        </div>
        <div className="overflow-x-auto rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-gray-dark">
          <table className="w-full text-left text-theme-sm">
            <thead className="bg-gray-50 dark:bg-gray-800/50 text-theme-xs text-gray-500 dark:text-gray-400">
              <tr>
                <th className="px-4 py-3 font-semibold">Libellé</th>
                <th className="px-4 py-3 font-semibold">Catégorie</th>
                <th className="px-4 py-3 font-semibold text-right">TTC</th>
                <th className="px-4 py-3 font-semibold text-right">HT</th>
                <th className="px-4 py-3 font-semibold text-right">TVA</th>
                <th className="px-4 py-3 font-semibold text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {variables.length === 0 ? (
                <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">Aucune charge variable ce mois</td></tr>
              ) : (
                variables.map((v) => (
                  <tr key={v.id} className="border-t border-gray-100 dark:border-gray-800">
                    <td className="px-4 py-3 font-medium text-gray-800 dark:text-white">{v.label}</td>
                    <td className="px-4 py-3">{v.categorie || '—'}</td>
                    <td className="px-4 py-3 text-right">{fmt(v.montant)}</td>
                    <td className="px-4 py-3 text-right text-gray-500 dark:text-gray-400">{fmt(v.montantHt)}</td>
                    <td className="px-4 py-3 text-right text-gray-500 dark:text-gray-400">{fmt(v.montantTva)}</td>
                    <td className="px-4 py-3 text-right">
                      <button
                        onClick={() => deleteVariable(v.id)}
                        className="rounded-lg border border-gray-200 dark:border-gray-700 p-2 text-error-500 hover:bg-error-50 dark:hover:bg-error-500/10"
                      >
                        <HiOutlineTrash size={15} />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal charge fixe */}
      {fixeModalOpen && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
          <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-2xl dark:bg-gray-800">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="text-base font-bold text-gray-900 dark:text-white">
                {editingFixeId ? 'Modifier la charge fixe' : 'Nouvelle charge fixe'}
              </h3>
              <button onClick={() => setFixeModalOpen(false)} className="text-gray-400 hover:text-gray-600">
                <HiOutlineX size={18} />
              </button>
            </div>
            <div className="space-y-3">
              <div>
                <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">Libellé</label>
                <input
                  value={fixeForm.label}
                  onChange={(e) => setFixeForm({ ...fixeForm, label: e.target.value })}
                  className="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">Montant TTC</label>
                  <input
                    type="number"
                    value={fixeForm.montant || ''}
                    onChange={(e) => setFixeForm({ ...fixeForm, montant: parseFloat(e.target.value) || 0 })}
                    className="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">TVA (%)</label>
                  <input
                    type="number"
                    value={fixeForm.tauxTva ?? ''}
                    onChange={(e) => setFixeForm({ ...fixeForm, tauxTva: e.target.value ? parseFloat(e.target.value) : null })}
                    className="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">Cycle</label>
                  <select
                    value={fixeForm.cycleMois}
                    onChange={(e) => setFixeForm({ ...fixeForm, cycleMois: parseInt(e.target.value, 10) })}
                    className="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-2 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                  >
                    {cycles.map((c) => (
                      <option key={c} value={c}>{c === 1 ? 'Mensuel' : `${c} mois`}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">Jour échéance</label>
                  <input
                    type="number"
                    min={1}
                    max={28}
                    value={fixeForm.jourEcheance ?? ''}
                    onChange={(e) => setFixeForm({ ...fixeForm, jourEcheance: e.target.value ? parseInt(e.target.value, 10) : null })}
                    className="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                  />
                </div>
              </div>
            </div>
            <div className="mt-5 flex gap-2">
              <button
                onClick={() => setFixeModalOpen(false)}
                className="flex-1 rounded-xl border border-gray-200 px-4 py-2.5 text-theme-sm font-medium text-gray-600 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-700"
              >
                Annuler
              </button>
              <button
                onClick={submitFixe}
                className="flex-1 rounded-xl bg-brand-500 px-4 py-2.5 text-theme-sm font-semibold text-white hover:bg-brand-600"
              >
                Enregistrer
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal charge variable */}
      {variableModalOpen && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
          <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-2xl dark:bg-gray-800">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="text-base font-bold text-gray-900 dark:text-white">Nouvelle charge — {mois}</h3>
              <button onClick={() => setVariableModalOpen(false)} className="text-gray-400 hover:text-gray-600">
                <HiOutlineX size={18} />
              </button>
            </div>
            <div className="space-y-3">
              <div>
                <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">Libellé</label>
                <input
                  value={variableForm.label}
                  onChange={(e) => setVariableForm({ ...variableForm, label: e.target.value })}
                  className="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                  autoFocus
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">Montant TTC</label>
                  <input
                    type="number"
                    value={variableForm.montant}
                    onChange={(e) => setVariableForm({ ...variableForm, montant: e.target.value })}
                    className="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">TVA (%)</label>
                  <input
                    type="number"
                    value={variableForm.tauxTva}
                    onChange={(e) => setVariableForm({ ...variableForm, tauxTva: e.target.value })}
                    className="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                  />
                </div>
              </div>
              <div>
                <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">Catégorie</label>
                <select
                  value={variableForm.categorie}
                  onChange={(e) => setVariableForm({ ...variableForm, categorie: e.target.value })}
                  className="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-2 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                >
                  <option value="">— Aucune —</option>
                  {categories.map((c) => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              </div>
            </div>
            <div className="mt-5 flex gap-2">
              <button
                onClick={() => setVariableModalOpen(false)}
                className="flex-1 rounded-xl border border-gray-200 px-4 py-2.5 text-theme-sm font-medium text-gray-600 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-700"
              >
                Annuler
              </button>
              <button
                onClick={submitVariable}
                className="flex-1 rounded-xl bg-brand-500 px-4 py-2.5 text-theme-sm font-semibold text-white hover:bg-brand-600"
              >
                Ajouter
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ChargesPage;
