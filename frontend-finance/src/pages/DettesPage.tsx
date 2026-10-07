import React, { useEffect, useState } from 'react';
import { HiOutlineCash, HiOutlinePlus, HiOutlineTrash, HiOutlineX } from 'react-icons/hi';
import { detteService } from '../api/detteService';
import ErreurBanner from '../components/ui/ErreurBanner';
import { messageErreur } from '../utils/apiError';
import { Dette } from '../types';

const fmt = (n: number | null | undefined) =>
  (n ?? 0).toLocaleString('fr-TN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const DettesPage: React.FC = () => {
  const [dettes, setDettes] = useState<Dette[]>([]);
  const [loading, setLoading] = useState(true);
  const [erreur, setErreur] = useState<string | null>(null);

  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState({ label: '', creancier: '', montantTotal: '', dateEcheance: '', notes: '' });

  const [paiementTarget, setPaiementTarget] = useState<Dette | null>(null);
  const [paiementMontant, setPaiementMontant] = useState('');

  const loadData = async () => {
    setLoading(true);
    setErreur(null);
    try {
      const res = await detteService.getAll();
      setDettes(res.data.data || []);
    } catch (e) {
      setErreur(messageErreur(e));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const totalDu = dettes.reduce((s, d) => s + d.montantTotal, 0);
  const totalPaye = dettes.reduce((s, d) => s + d.montantPaye, 0);
  const totalRestant = dettes.reduce((s, d) => s + d.soldeRestant, 0);

  const submit = async () => {
    const montantTotal = Number(form.montantTotal);
    if (!form.label.trim() || !montantTotal) return;
    try {
      await detteService.create({
        label: form.label,
        creancier: form.creancier || null,
        montantTotal,
        dateEcheance: form.dateEcheance || null,
        notes: form.notes || null,
      });
      setModalOpen(false);
      setForm({ label: '', creancier: '', montantTotal: '', dateEcheance: '', notes: '' });
      await loadData();
    } catch (e: any) {
      alert(e?.response?.data?.message || 'Erreur');
    }
  };

  const submitPaiement = async () => {
    if (!paiementTarget) return;
    const montant = Number(paiementMontant);
    if (!montant || montant <= 0) return;
    try {
      await detteService.enregistrerPaiement(paiementTarget.id, montant);
      setPaiementTarget(null);
      setPaiementMontant('');
      await loadData();
    } catch (e: any) {
      alert(e?.response?.data?.message || 'Erreur');
    }
  };

  const remove = async (id: number) => {
    if (!confirm('Supprimer cette dette et son historique de remboursement ?')) return;
    await detteService.remove(id);
    await loadData();
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-title-sm font-extrabold tracking-tight text-transparent bg-clip-text bg-gradient-to-r from-gray-900 to-[#683b77] dark:from-white dark:to-[#ab78c3]">
            Dettes
          </h1>
          <p className="text-theme-sm text-gray-500 dark:text-gray-400 mt-1">
            Solde restant = montant total − remboursements cumulés
          </p>
        </div>
        <button
          onClick={() => setModalOpen(true)}
          className="flex items-center gap-2 rounded-lg bg-brand-500 px-4 py-2.5 text-theme-sm font-semibold text-white hover:bg-brand-600"
        >
          <HiOutlinePlus size={16} /> Nouvelle dette
        </button>
      </div>

      {erreur && <ErreurBanner message={erreur} onRetry={loadData} />}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        {[
          { label: 'Total emprunté', value: totalDu },
          { label: 'Total remboursé', value: totalPaye },
          { label: 'Solde restant', value: totalRestant },
        ].map((item) => (
          <div key={item.label} className="rounded-2xl border border-gray-200 bg-white p-4 dark:border-gray-800 dark:bg-gray-dark">
            <p className="text-theme-xs text-gray-400">{item.label}</p>
            <p className="mt-1 text-theme-sm font-bold text-gray-800 dark:text-white">{fmt(item.value)} DT</p>
          </div>
        ))}
      </div>

      <div className="overflow-x-auto rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-gray-dark">
        <table className="w-full text-left text-theme-sm">
          <thead className="bg-gray-50 dark:bg-gray-800/50 text-theme-xs text-gray-500 dark:text-gray-400">
            <tr>
              <th className="px-4 py-3 font-semibold">Libellé</th>
              <th className="px-4 py-3 font-semibold">Créancier</th>
              <th className="px-4 py-3 font-semibold">Échéance</th>
              <th className="px-4 py-3 font-semibold text-right">Total</th>
              <th className="px-4 py-3 font-semibold text-right">Remboursé</th>
              <th className="px-4 py-3 font-semibold text-right">Solde restant</th>
              <th className="px-4 py-3 font-semibold text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} className="px-4 py-10 text-center text-gray-400">Chargement...</td></tr>
            ) : dettes.length === 0 ? (
              <tr><td colSpan={7} className="px-4 py-10 text-center text-gray-400">Aucune dette</td></tr>
            ) : (
              dettes.map((d) => (
                <tr key={d.id} className={`border-t border-gray-100 dark:border-gray-800 ${d.soldee ? 'opacity-55' : ''}`}>
                  <td className="px-4 py-3 font-medium text-gray-800 dark:text-white">{d.label}</td>
                  <td className="px-4 py-3">{d.creancier || '—'}</td>
                  <td className="px-4 py-3">{d.dateEcheance || '—'}</td>
                  <td className="px-4 py-3 text-right">{fmt(d.montantTotal)}</td>
                  <td className="px-4 py-3 text-right">{fmt(d.montantPaye)}</td>
                  <td className={`px-4 py-3 text-right font-semibold ${d.soldeRestant > 0 ? 'text-error-500' : 'text-success-500'}`}>
                    {fmt(d.soldeRestant)}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-2">
                      {!d.soldee && (
                        <button
                          onClick={() => { setPaiementTarget(d); setPaiementMontant(String(d.soldeRestant)); }}
                          className="rounded-lg border border-gray-200 dark:border-gray-700 p-2 text-gray-500 hover:bg-gray-50 dark:hover:bg-gray-800"
                          title="Enregistrer un remboursement"
                        >
                          <HiOutlineCash size={15} />
                        </button>
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

      {modalOpen && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
          <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-2xl dark:bg-gray-800">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="text-base font-bold text-gray-900 dark:text-white">Nouvelle dette</h3>
              <button onClick={() => setModalOpen(false)} className="text-gray-400 hover:text-gray-600">
                <HiOutlineX size={18} />
              </button>
            </div>
            <div className="space-y-3">
              <div>
                <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">Libellé</label>
                <input
                  value={form.label}
                  onChange={(e) => setForm({ ...form, label: e.target.value })}
                  className="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                  autoFocus
                />
              </div>
              <div>
                <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">Créancier</label>
                <input
                  value={form.creancier}
                  onChange={(e) => setForm({ ...form, creancier: e.target.value })}
                  className="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">Montant total</label>
                  <input
                    type="number"
                    value={form.montantTotal}
                    onChange={(e) => setForm({ ...form, montantTotal: e.target.value })}
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
              <div>
                <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">Notes</label>
                <input
                  value={form.notes}
                  onChange={(e) => setForm({ ...form, notes: e.target.value })}
                  className="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                />
              </div>
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
                Créer
              </button>
            </div>
          </div>
        </div>
      )}

      {paiementTarget && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
          <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-2xl dark:bg-gray-800">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="text-base font-bold text-gray-900 dark:text-white">
                Remboursement — {paiementTarget.label}
              </h3>
              <button onClick={() => setPaiementTarget(null)} className="text-gray-400 hover:text-gray-600">
                <HiOutlineX size={18} />
              </button>
            </div>
            <p className="mb-3 text-theme-sm text-gray-500 dark:text-gray-400">
              Solde restant : <strong>{fmt(paiementTarget.soldeRestant)} DT</strong>
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

export default DettesPage;
