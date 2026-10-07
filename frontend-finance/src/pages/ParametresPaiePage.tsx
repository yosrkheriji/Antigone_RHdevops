import React, { useEffect, useState } from 'react';
import { HiOutlineSave } from 'react-icons/hi';
import { payrollService } from '../api/payrollService';
import ErreurBanner from '../components/ui/ErreurBanner';
import { messageErreur } from '../utils/apiError';
import { BaremeIrpp, ParametresPaie } from '../types';

const pct = (taux: number) => `${(taux * 100).toFixed(2).replace(/\.?0+$/, '')} %`;

const CHAMPS: Array<{ key: keyof ParametresPaie; label: string; aide: string }> = [
  { key: 'cnssSalarie', label: 'CNSS salarié', aide: 'Retenue sur la base CNSS du salarié' },
  { key: 'solidariteSalarie', label: 'Contribution de solidarité (CSS)', aide: 'Sur le revenu net imposable si abattement, sinon sur le salaire imposable' },
  { key: 'cnssPatronale', label: 'CNSS patronale', aide: 'Part employeur consolidée, sur le brut effectif' },
  { key: 'tfp', label: 'TFP', aide: 'Taxe de formation professionnelle, sur le brut effectif' },
  { key: 'foprolos', label: 'FOPROLOS', aide: 'Fonds de promotion du logement, sur le brut effectif' },
  { key: 'at', label: 'Accidents du travail', aide: 'Sur le brut effectif' },
  { key: 'abattement', label: 'Abattement forfaitaire', aide: 'Déduction sur le salaire imposable avant IRPP' },
];

const ParametresPaiePage: React.FC = () => {
  const [parametres, setParametres] = useState<ParametresPaie | null>(null);
  const [baremes, setBaremes] = useState<BaremeIrpp[]>([]);
  const [loading, setLoading] = useState(true);
  const [erreur, setErreur] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const loadData = async () => {
    setLoading(true);
    setErreur(null);
    try {
      const [paramRes, baremeRes] = await Promise.all([
        payrollService.getParametres(),
        payrollService.listBaremes(),
      ]);
      setParametres(paramRes.data.data);
      setBaremes(baremeRes.data.data || []);
    } catch (e) {
      setErreur(messageErreur(e));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const save = async () => {
    if (!parametres) return;
    setSaving(true);
    setMessage(null);
    try {
      await payrollService.updateParametres(parametres);
      setMessage('Paramètres enregistrés — les prochains calculs de paie les utiliseront.');
    } catch (e) {
      setMessage(messageErreur(e));
    } finally {
      setSaving(false);
    }
  };

  const baremeActuel = baremes[0];

  if (loading) return <div className="py-20 text-center text-gray-400">Chargement...</div>;

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-title-sm font-extrabold tracking-tight text-transparent bg-clip-text bg-gradient-to-r from-gray-900 to-[#683b77] dark:from-white dark:to-[#ab78c3]">
          Paramètres de paie
        </h1>
        <p className="text-theme-sm text-gray-500 dark:text-gray-400 mt-1">
          Taux de cotisation et barème IRPP appliqués à tous les bulletins
        </p>
      </div>

      {erreur && <ErreurBanner message={erreur} onRetry={loadData} />}

      {/* Taux */}
      {parametres && (
        <div className="rounded-2xl border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-dark">
          <h2 className="mb-4 text-theme-md font-bold text-gray-800 dark:text-white">Taux de cotisation</h2>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {CHAMPS.map((champ) => (
              <div key={champ.key}>
                <label className="mb-1 block text-theme-xs font-semibold text-gray-600 dark:text-gray-300">
                  {champ.label}
                </label>
                <div className="relative">
                  <input
                    type="number"
                    step="0.0001"
                    value={parametres[champ.key] as number}
                    onChange={(e) =>
                      setParametres({ ...parametres, [champ.key]: parseFloat(e.target.value) || 0 })
                    }
                    className="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 pr-16 text-theme-sm dark:border-gray-600 dark:text-gray-200"
                  />
                  <span className="absolute right-3 top-1/2 -translate-y-1/2 text-theme-xs text-gray-400">
                    = {pct(parametres[champ.key] as number)}
                  </span>
                </div>
                <p className="mt-1 text-[11px] text-gray-400">{champ.aide}</p>
              </div>
            ))}
          </div>

          <div className="mt-6 flex items-center gap-3">
            <button
              onClick={save}
              disabled={saving}
              className="flex items-center gap-2 rounded-lg bg-brand-500 px-4 py-2.5 text-theme-sm font-semibold text-white hover:bg-brand-600 disabled:opacity-50"
            >
              <HiOutlineSave size={16} />
              {saving ? 'Enregistrement...' : 'Enregistrer'}
            </button>
            {message && <p className="text-theme-xs text-gray-500 dark:text-gray-400">{message}</p>}
          </div>
        </div>
      )}

      {/* Barème IRPP */}
      <div className="rounded-2xl border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-dark">
        <div className="mb-4 flex items-baseline justify-between">
          <h2 className="text-theme-md font-bold text-gray-800 dark:text-white">Barème IRPP progressif</h2>
          {baremeActuel && (
            <span className="text-theme-xs text-gray-400">
              En vigueur depuis le {baremeActuel.effectiveFrom}
            </span>
          )}
        </div>

        {!baremeActuel ? (
          <p className="py-6 text-center text-theme-sm text-gray-400">Aucun barème enregistré</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-theme-sm">
              <thead className="bg-gray-50 dark:bg-gray-800/50 text-theme-xs text-gray-500 dark:text-gray-400">
                <tr>
                  <th className="px-4 py-3 font-semibold">Tranche annuelle</th>
                  <th className="px-4 py-3 font-semibold text-right">Taux marginal</th>
                </tr>
              </thead>
              <tbody>
                {baremeActuel.tranches.map((t, i) => {
                  const precedent = i === 0 ? 0 : baremeActuel.tranches[i - 1].plafond;
                  return (
                    <tr key={i} className="border-t border-gray-100 dark:border-gray-800">
                      <td className="px-4 py-3 text-gray-700 dark:text-gray-200">
                        {t.plafond === null
                          ? `Au-delà de ${(precedent ?? 0).toLocaleString('fr-TN')} DT`
                          : `De ${(precedent ?? 0).toLocaleString('fr-TN')} à ${t.plafond.toLocaleString('fr-TN')} DT`}
                      </td>
                      <td className="px-4 py-3 text-right font-semibold text-gray-800 dark:text-white">
                        {pct(t.taux)}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Rappel des formules */}
      <div className="rounded-2xl border border-gray-200 bg-gray-50 p-6 dark:border-gray-800 dark:bg-gray-800/30">
        <h2 className="mb-3 text-theme-md font-bold text-gray-800 dark:text-white">Formules appliquées</h2>
        <ol className="space-y-1.5 text-theme-sm text-gray-600 dark:text-gray-300">
          <li>1. CNSS salarié = base CNSS × taux CNSS salarié</li>
          <li>2. Salaire imposable = brut (ajusté IRPP) − CNSS salarié</li>
          <li>3. Abattement = salaire imposable × taux d'abattement</li>
          <li>4. Revenu net imposable = salaire imposable − abattement</li>
          <li>5. Contribution de solidarité = base de cotisation × taux CSS</li>
          <li>6. IRPP mensuel = irppAnnuel(revenu net imposable × 12) ÷ 12</li>
          <li>7. Net = brut effectif − CNSS − CSS − IRPP</li>
          <li>8. Net à payer = Net − acomptes déjà versés</li>
          <li className="pt-2">
            Charges employeur = brut × (CNSS patronale + TFP + FOPROLOS + AT) — Coût total = brut + charges employeur
          </li>
          <li className="pt-2 text-gray-500 dark:text-gray-400">
            Les contrats <strong>CIVP</strong>, <strong>Freelance</strong> et <strong>Stage</strong> sont exonérés : Net = Brut.
          </li>
        </ol>
      </div>
    </div>
  );
};

export default ParametresPaiePage;
