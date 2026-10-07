import React, { useEffect, useState } from 'react';
import {
  HiOutlineChevronLeft,
  HiOutlineChevronRight,
  HiOutlinePlus,
  HiOutlineTrash,
  HiOutlineX,
} from 'react-icons/hi';
import { revenuService } from '../api/revenuService';
import { referentielService } from '../api/referentielService';
import ErreurBanner from '../components/ui/ErreurBanner';
import { messageErreur } from '../utils/apiError';
import { AutreRevenu } from '../types';

const fmt = (n: number | null | undefined) =>
  (n ?? 0).toLocaleString('fr-TN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const currentMois = () => new Date().toISOString().slice(0, 7);

const RevenusPage: React.FC = () => {
  const [mois, setMois] = useState(currentMois());
  const [revenus, setRevenus] = useState<AutreRevenu[]>([]);
  const [categories, setCategories] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [erreur, setErreur] = useState<string | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState({ label: '', montant: '', tauxTva: '0', categorie: '', description: '' });

  const loadData = async () => {
    setLoading(true);
    setErreur(null);
    try {
      const [revRes, parRes] = await Promise.all([
        revenuService.getByMois(mois),
        referentielService.getParametresFacturation(),
      ]);
      setRevenus(revRes.data.data || []);
      setCategories(parRes.data.data?.categoriesRevenu || []);
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

  const totalTtc = revenus.reduce((s, r) => s + r.montant, 0);
  const totalHt = revenus.reduce((s, r) => s + r.montantHt, 0);
  const totalTva = revenus.reduce((s, r) => s + r.montantTva, 0);

  const submit = async () => {
    const montant = Number(form.montant);
    if (!form.label.trim() || !montant) return;
    try {
      await revenuService.create({
        mois,
        label: form.label,
        montant,
        tauxTva: Number(form.tauxTva) || 0,
        categorie: form.categorie || null,
        description: form.description || null,
      });
      setModalOpen(false);
      setForm({ label: '', montant: '', tauxTva: '0', categorie: '', description: '' });
      await loadData();
    } catch (e: any) {
      alert(e?.response?.data?.message || 'Erreur');
    }
  };

  const remove = async (id: number) => {
    if (!confirm('Supprimer ce revenu ?')) return;
    await revenuService.remove(id);
    await loadData();
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-title-sm font-extrabold tracking-tight text-transparent bg-clip-text bg-gradient-to-r from-gray-900 to-[#683b77] dark:from-white dark:to-[#ab78c3]">
            Autres revenus
          </h1>
          <p className="text-theme-sm text-gray-500 dark:text-gray-400 mt-1">
            Revenus hors facturation — le montant saisi est TTC, le HT en est extrait
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={() => changeMois(-1)} className="p-2 rounded-lg border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800">
            <HiOutlineChevronLeft size={16} />
          </button>
          <span className="text-theme-sm font-semibold text-gray-800 dark:text-white min-w-[110px] text-center">{mois}</span>
          <button onClick={() => changeMois(1)} className="p-2 rounded-lg border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800">
            <HiOutlineChevronRight size={16} />
          </button>
          <button
            onClick={() => setModalOpen(true)}
            className="ml-2 flex items-center gap-2 rounded-lg bg-brand-500 px-4 py-2.5 text-theme-sm font-semibold text-white hover:bg-brand-600"
          >
            <HiOutlinePlus size={16} /> Ajouter
          </button>
        </div>
      </div>

      {erreur && <ErreurBanner message={erreur} onRetry={loadData} />}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        {[
          { label: 'Total TTC', value: totalTtc },
          { label: 'Total HT', value: totalHt },
          { label: 'TVA incluse', value: totalTva },
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
              <th className="px-4 py-3 font-semibold">Catégorie</th>
              <th className="px-4 py-3 font-semibold">Date</th>
              <th className="px-4 py-3 font-semibold text-right">TTC</th>
              <th className="px-4 py-3 font-semibold text-right">HT</th>
              <th className="px-4 py-3 font-semibold text-right">TVA</th>
              <th className="px-4 py-3 font-semibold text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} className="px-4 py-10 text-center text-gray-400">Chargement...</td></tr>
            ) : revenus.length === 0 ? (
              <tr><td colSpan={7} className="px-4 py-10 text-center text-gray-400">Aucun revenu ce mois</td></tr>
            ) : (
              revenus.map((r) => (
                <tr key={r.id} className="border-t border-gray-100 dark:border-gray-800">
                  <td className="px-4 py-3 font-medium text-gray-800 dark:text-white">{r.label}</td>
                  <td className="px-4 py-3 capitalize">{r.categorie || '—'}</td>
                  <td className="px-4 py-3">{r.date}</td>
                  <td className="px-4 py-3 text-right font-semibold">{fmt(r.montant)}</td>
                  <td className="px-4 py-3 text-right text-gray-500 dark:text-gray-400">{fmt(r.montantHt)}</td>
                  <td className="px-4 py-3 text-right text-gray-500 dark:text-gray-400">{fmt(r.montantTva)}</td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => remove(r.id)}
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

      {modalOpen && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
          <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-2xl dark:bg-gray-800">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="text-base font-bold text-gray-900 dark:text-white">Nouveau revenu — {mois}</h3>
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
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">Montant TTC</label>
                  <input
                    type="number"
                    value={form.montant}
                    onChange={(e) => setForm({ ...form, montant: e.target.value })}
                    className="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">TVA (%)</label>
                  <input
                    type="number"
                    value={form.tauxTva}
                    onChange={(e) => setForm({ ...form, tauxTva: e.target.value })}
                    className="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                  />
                </div>
              </div>
              <div>
                <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">Catégorie</label>
                <select
                  value={form.categorie}
                  onChange={(e) => setForm({ ...form, categorie: e.target.value })}
                  className="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-2 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                >
                  <option value="">— Aucune —</option>
                  {categories.map((c) => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">Description</label>
                <input
                  value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
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
                Ajouter
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default RevenusPage;
