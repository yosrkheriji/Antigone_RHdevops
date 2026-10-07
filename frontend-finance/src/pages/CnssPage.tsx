import React, { useEffect, useState } from 'react';
import { HiOutlineChevronLeft, HiOutlineChevronRight, HiOutlineCheckCircle, HiOutlineRefresh } from 'react-icons/hi';
import { cnssService } from '../api/cnssService';
import ErreurBanner from '../components/ui/ErreurBanner';
import { messageErreur } from '../utils/apiError';
import { DeclarationCnss } from '../types';

const fmt = (n: number | null | undefined) =>
  (n ?? 0).toLocaleString('fr-TN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const TRIMESTRES = [1, 2, 3, 4];

const statutBadge = (statut: string) =>
  statut === 'PAYE'
    ? 'bg-success-50 text-success-600 dark:bg-success-500/10 dark:text-success-400'
    : 'bg-error-50 text-error-600 dark:bg-error-500/10 dark:text-error-400';

/** Échéance légale : le 15 du mois suivant la fin du trimestre. */
const echeance = (annee: number, trimestre: number) => {
  const d = new Date(annee, trimestre * 3, 15); // mois JS 0-indexé → trimestre*3 = mois suivant la fin
  return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
};

const estEnRetard = (annee: number, trimestre: number) => new Date() > new Date(annee, trimestre * 3, 15);

const CnssPage: React.FC = () => {
  const [annee, setAnnee] = useState(new Date().getFullYear());
  const [declarations, setDeclarations] = useState<DeclarationCnss[]>([]);
  const [loading, setLoading] = useState(true);
  const [erreur, setErreur] = useState<string | null>(null);
  const [busy, setBusy] = useState<number | null>(null);

  const loadData = async () => {
    setLoading(true);
    setErreur(null);
    try {
      const res = await cnssService.getByAnnee(annee);
      setDeclarations(res.data.data || []);
    } catch (e) {
      setErreur(messageErreur(e));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [annee]);

  const byTrimestre = (t: number) => declarations.find((d) => d.trimestre === t);

  /** Calcule les montants depuis les bulletins de paie du trimestre puis enregistre la déclaration. */
  const calculerEtEnregistrer = async (trimestre: number) => {
    setBusy(trimestre);
    try {
      const suggestion = await cnssService.getSuggestion(annee, trimestre);
      await cnssService.save(suggestion.data.data);
      await loadData();
    } catch (e: any) {
      alert(e?.response?.data?.message || 'Erreur lors du calcul');
    } finally {
      setBusy(null);
    }
  };

  const payer = async (id: number) => {
    setBusy(id);
    try {
      await cnssService.payer(id);
      await loadData();
    } catch (e: any) {
      alert(e?.response?.data?.message || 'Erreur');
    } finally {
      setBusy(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-title-sm font-extrabold tracking-tight text-transparent bg-clip-text bg-gradient-to-r from-gray-900 to-[#683b77] dark:from-white dark:to-[#ab78c3]">
            CNSS trimestriel
          </h1>
          <p className="text-theme-sm text-gray-500 dark:text-gray-400 mt-1">
            Déclarations CNSS calculées depuis les bulletins de paie du trimestre
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={() => setAnnee(annee - 1)} className="p-2 rounded-lg border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800">
            <HiOutlineChevronLeft size={16} />
          </button>
          <span className="text-theme-sm font-semibold text-gray-800 dark:text-white min-w-[70px] text-center">{annee}</span>
          <button onClick={() => setAnnee(annee + 1)} className="p-2 rounded-lg border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800">
            <HiOutlineChevronRight size={16} />
          </button>
        </div>
      </div>

      {erreur && <ErreurBanner message={erreur} onRetry={loadData} />}

      {loading ? (
        <div className="py-20 text-center text-gray-400">Chargement...</div>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {TRIMESTRES.map((t) => {
            const d = byTrimestre(t);
            return (
              <div key={t} className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-dark">
                <div className="mb-3 flex items-center justify-between">
                  <h3 className="text-theme-sm font-bold text-gray-800 dark:text-white">T{t} {annee}</h3>
                  {d && (
                    <span className={`rounded-full px-2.5 py-1 text-theme-xs font-semibold ${statutBadge(d.statut)}`}>
                      {d.statut}
                    </span>
                  )}
                </div>

                <p className={`mb-3 text-theme-xs ${estEnRetard(annee, t) && d?.statut !== 'PAYE' ? 'font-semibold text-error-500' : 'text-gray-400'}`}>
                  Échéance : {echeance(annee, t)}
                </p>

                {d ? (
                  <div className="space-y-2 text-theme-sm">
                    <div className="flex justify-between">
                      <span className="text-gray-400">Part salariale</span>
                      <span className="text-gray-700 dark:text-gray-200">{fmt(d.montantSalarie)} DT</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-400">Part patronale</span>
                      <span className="text-gray-700 dark:text-gray-200">{fmt(d.montantEmployeur)} DT</span>
                    </div>
                    {(d.montantPenalite ?? 0) > 0 && (
                      <div className="flex justify-between">
                        <span className="text-gray-400">Pénalité</span>
                        <span className="text-error-500">{fmt(d.montantPenalite)} DT</span>
                      </div>
                    )}
                    <div className="flex justify-between border-t border-gray-100 pt-2 dark:border-gray-800">
                      <span className="font-semibold text-gray-600 dark:text-gray-300">Total</span>
                      <span className="font-bold text-gray-800 dark:text-white">{fmt(d.montantTotal)} DT</span>
                    </div>
                    {d.datePaiement && (
                      <p className="text-theme-xs text-gray-400">Payé le {d.datePaiement}</p>
                    )}
                  </div>
                ) : (
                  <p className="py-4 text-center text-theme-xs text-gray-400">Aucune déclaration</p>
                )}

                <div className="mt-4 flex gap-2">
                  <button
                    onClick={() => calculerEtEnregistrer(t)}
                    disabled={busy === t}
                    className="flex flex-1 items-center justify-center gap-1.5 rounded-lg border border-brand-200 bg-brand-50 px-3 py-2 text-theme-xs font-semibold text-brand-600 hover:bg-brand-100 disabled:opacity-50 dark:border-brand-500/30 dark:bg-brand-500/10 dark:text-brand-400"
                  >
                    <HiOutlineRefresh size={14} /> {d ? 'Recalculer' : 'Calculer'}
                  </button>
                  {d && d.statut !== 'PAYE' && (
                    <button
                      onClick={() => payer(d.id)}
                      disabled={busy === d.id}
                      className="rounded-lg bg-success-500 px-3 py-2 text-white hover:bg-success-600 disabled:opacity-50"
                      title="Marquer payée"
                    >
                      <HiOutlineCheckCircle size={15} />
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default CnssPage;
