import React, { useEffect, useMemo, useState } from 'react';
import {
  HiOutlineCalculator,
  HiOutlineCash,
  HiOutlineCheckCircle,
  HiOutlineChevronLeft,
  HiOutlineChevronRight,
  HiOutlineCurrencyDollar,
  HiOutlineX,
} from 'react-icons/hi';
import { payrollService } from '../api/payrollService';
import { employeService } from '../api/employeService';
import { referentielService } from '../api/referentielService';
import ErreurBanner from '../components/ui/ErreurBanner';
import { messageErreur } from '../utils/apiError';
import { BulletinPaie, Employe, TotauxPaie } from '../types';

const fmt = (n: number | null | undefined) =>
  (n ?? 0).toLocaleString('fr-TN', { minimumFractionDigits: 3, maximumFractionDigits: 3 });

const currentMois = () => new Date().toISOString().slice(0, 7);

const statutBadge = (statut: string) => {
  switch (statut) {
    case 'PAYE':
      return 'bg-success-50 text-success-600 dark:bg-success-500/10 dark:text-success-400';
    case 'PARTIEL':
      return 'bg-warning-50 text-warning-600 dark:bg-warning-500/10 dark:text-warning-400';
    default:
      return 'bg-error-50 text-error-600 dark:bg-error-500/10 dark:text-error-400';
  }
};

const SalairesPage: React.FC = () => {
  const [mois, setMois] = useState(currentMois());
  const [employes, setEmployes] = useState<Employe[]>([]);
  const [bulletins, setBulletins] = useState<BulletinPaie[]>([]);
  const [totaux, setTotaux] = useState<TotauxPaie | null>(null);
  const [loading, setLoading] = useState(true);
  const [erreur, setErreur] = useState<string | null>(null);
  const [generating, setGenerating] = useState(false);
  const [acompteTarget, setAcompteTarget] = useState<Employe | null>(null);
  const [acompteMontant, setAcompteMontant] = useState('');
  const [acompteNote, setAcompteNote] = useState('');
  const [busyRow, setBusyRow] = useState<number | null>(null);
  const [detailId, setDetailId] = useState<number | null>(null);

  const loadData = async () => {
    setLoading(true);
    setErreur(null);
    try {
      const [empRes, bullRes, totRes] = await Promise.all([
        referentielService.getEmployesDuMois(mois),
        payrollService.getBulletins(mois),
        payrollService.getTotaux(mois),
      ]);
      setEmployes(empRes.data.data || []);
      setBulletins(bullRes.data.data || []);
      setTotaux(totRes.data.data || null);
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

  const bulletinByEmploye = useMemo(() => {
    const map = new Map<number, BulletinPaie>();
    bulletins.forEach((b) => map.set(b.employeId, b));
    return map;
  }, [bulletins]);

  const changeMois = (delta: number) => {
    const [y, m] = mois.split('-').map(Number);
    const d = new Date(y, m - 1 + delta, 1);
    setMois(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
  };

  const handleGenererTout = async () => {
    setGenerating(true);
    try {
      await payrollService.genererTout(mois);
      await loadData();
    } catch (e: any) {
      alert(e?.response?.data?.message || 'Erreur lors de la génération des bulletins');
    } finally {
      setGenerating(false);
    }
  };

  const handleGenererUn = async (employeId: number) => {
    setBusyRow(employeId);
    try {
      await payrollService.genererBulletin(employeId, mois);
      await loadData();
    } catch (e: any) {
      alert(e?.response?.data?.message || 'Erreur lors du calcul du bulletin');
    } finally {
      setBusyRow(null);
    }
  };

  const handleMarquerPaye = async (bulletinId: number) => {
    setBusyRow(bulletinId);
    try {
      await payrollService.marquerPaye(bulletinId);
      await loadData();
    } catch (e: any) {
      alert(e?.response?.data?.message || 'Erreur');
    } finally {
      setBusyRow(null);
    }
  };

  const handleToggleMode = async (employe: Employe) => {
    const nouveau = employe.modeSalaire === 'NET' ? 'BRUT' : 'NET';
    try {
      await employeService.updateModeSalaire(employe.id, nouveau);
      setEmployes((prev) => prev.map((e) => (e.id === employe.id ? { ...e, modeSalaire: nouveau } : e)));
    } catch (e: any) {
      alert(e?.response?.data?.message || 'Erreur');
    }
  };

  const submitAcompte = async () => {
    if (!acompteTarget) return;
    const montant = parseFloat(acompteMontant);
    if (!montant || montant <= 0) return;
    try {
      await payrollService.enregistrerAcompte(acompteTarget.id, mois, montant, undefined, acompteNote || undefined);
      setAcompteTarget(null);
      setAcompteMontant('');
      setAcompteNote('');
      await loadData();
    } catch (e: any) {
      alert(e?.response?.data?.message || "Erreur lors de l'enregistrement de l'acompte");
    }
  };

  const detail = detailId != null ? employes.find((e) => e.id === detailId) : null;
  const detailBulletin = detailId != null ? bulletinByEmploye.get(detailId) : null;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-title-sm font-extrabold tracking-tight text-transparent bg-clip-text bg-gradient-to-r from-gray-900 to-[#683b77] dark:from-white dark:to-[#ab78c3]">
            Salaires
          </h1>
          <p className="text-theme-sm text-gray-500 dark:text-gray-400 mt-1">
            {employes.length} employé{employes.length !== 1 ? 's' : ''} sous contrat sur {mois}
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
            onClick={handleGenererTout}
            disabled={generating || employes.length === 0}
            className="ml-2 flex items-center gap-2 rounded-lg bg-brand-500 px-4 py-2.5 text-theme-sm font-semibold text-white hover:bg-brand-600 disabled:opacity-50"
          >
            <HiOutlineCalculator size={16} />
            {generating ? 'Génération...' : 'Générer tous les bulletins'}
          </button>
        </div>
      </div>

      {erreur && <ErreurBanner message={erreur} onRetry={loadData} />}

      {totaux && (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
          {[
            { label: 'Masse brute', value: totaux.masseBrute },
            { label: 'Masse nette', value: totaux.masseNette },
            { label: 'CNSS salarié', value: totaux.cnssSalarie },
            { label: 'CNSS patronale', value: totaux.cnssEmployeur },
            { label: 'IRPP', value: totaux.irppTotal },
            { label: 'Coût total', value: totaux.coutTotal },
          ].map((item) => (
            <div key={item.label} className="rounded-2xl border border-gray-200 bg-white p-4 dark:border-gray-800 dark:bg-gray-dark">
              <p className="text-theme-xs text-gray-400">{item.label}</p>
              <p className="mt-1 text-theme-sm font-bold text-gray-800 dark:text-white">{fmt(item.value)} DT</p>
            </div>
          ))}
        </div>
      )}

      <div className="overflow-x-auto rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-gray-dark">
        <table className="w-full text-left text-theme-sm">
          <thead className="bg-gray-50 dark:bg-gray-800/50 text-theme-xs text-gray-500 dark:text-gray-400">
            <tr>
              <th className="px-4 py-3 font-semibold">Employé</th>
              <th className="px-4 py-3 font-semibold">Poste / Département</th>
              <th className="px-4 py-3 font-semibold">Contrat</th>
              <th className="px-4 py-3 font-semibold">Mode</th>
              <th className="px-4 py-3 font-semibold text-right">Salaire saisi</th>
              <th className="px-4 py-3 font-semibold text-right">Brut</th>
              <th className="px-4 py-3 font-semibold text-right">Retenues</th>
              <th className="px-4 py-3 font-semibold text-right">Net</th>
              <th className="px-4 py-3 font-semibold text-right">Net à payer</th>
              <th className="px-4 py-3 font-semibold">Statut</th>
              <th className="px-4 py-3 font-semibold text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={11} className="px-4 py-10 text-center text-gray-400">Chargement...</td></tr>
            ) : employes.length === 0 ? (
              <tr>
                <td colSpan={11} className="px-4 py-10 text-center text-gray-400">
                  Aucun employé sous contrat sur {mois}
                </td>
              </tr>
            ) : (
              employes.map((emp) => {
                const b = bulletinByEmploye.get(emp.id);
                const retenues = b ? (b.cnssSalarie || 0) + (b.solidariteSalarie || 0) + (b.irppMensuel || 0) : 0;
                return (
                  <tr key={emp.id} className="border-t border-gray-100 dark:border-gray-800">
                    <td className="px-4 py-3">
                      <button
                        onClick={() => setDetailId(emp.id)}
                        className="text-left font-semibold text-gray-800 hover:text-brand-500 dark:text-white"
                      >
                        {emp.prenom} {emp.nom}
                      </button>
                      <p className="text-theme-xs text-gray-400">{emp.matricule || '—'}</p>
                      {emp.archived && (
                        <span className="mt-1 inline-block rounded-full bg-gray-100 px-2 py-0.5 text-[10px] font-semibold text-gray-500 dark:bg-gray-700 dark:text-gray-400">
                          Archivé{emp.dateArchivage ? ` le ${emp.dateArchivage}` : ''}
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <p className="text-gray-700 dark:text-gray-200">{emp.poste || '—'}</p>
                      <p className="text-theme-xs text-gray-400">{emp.departement || '—'}</p>
                    </td>
                    <td className="px-4 py-3">
                      <p className="text-gray-700 dark:text-gray-200">{emp.typeContrat || '—'}</p>
                      {emp.exonere && (
                        <span className="text-theme-xs font-semibold text-warning-500">Exonéré</span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => handleToggleMode(emp)}
                        disabled={emp.exonere}
                        className="rounded-full border border-gray-200 px-2.5 py-1 text-theme-xs font-semibold text-gray-600 hover:bg-gray-50 disabled:opacity-40 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-800"
                        title={emp.exonere ? 'Sans objet pour un contrat exonéré' : 'Basculer BRUT / NET'}
                      >
                        {emp.modeSalaire}
                      </button>
                    </td>
                    <td className="px-4 py-3 text-right text-gray-500 dark:text-gray-400">{fmt(emp.salaire)}</td>
                    {b ? (
                      <>
                        <td className="px-4 py-3 text-right">{fmt(b.brutEffectif)}</td>
                        <td className="px-4 py-3 text-right text-gray-500 dark:text-gray-400">{fmt(retenues)}</td>
                        <td className="px-4 py-3 text-right font-semibold">{fmt(b.net)}</td>
                        <td className="px-4 py-3 text-right">{fmt(b.netAPayer)}</td>
                        <td className="px-4 py-3">
                          <span className={`rounded-full px-2.5 py-1 text-theme-xs font-semibold ${statutBadge(b.statut)}`}>
                            {b.statut}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex justify-end gap-2">
                            <button
                              onClick={() => { setAcompteTarget(emp); setAcompteMontant(String(b.netAPayer)); }}
                              className="rounded-lg border border-gray-200 dark:border-gray-700 p-2 text-gray-500 hover:bg-gray-50 dark:hover:bg-gray-800"
                              title="Enregistrer un acompte"
                            >
                              <HiOutlineCash size={16} />
                            </button>
                            {b.statut !== 'PAYE' && (
                              <button
                                onClick={() => handleMarquerPaye(b.id)}
                                disabled={busyRow === b.id}
                                className="rounded-lg bg-success-500 p-2 text-white hover:bg-success-600 disabled:opacity-50"
                                title="Marquer payé"
                              >
                                <HiOutlineCheckCircle size={16} />
                              </button>
                            )}
                          </div>
                        </td>
                      </>
                    ) : (
                      <td colSpan={6} className="px-4 py-3 text-right">
                        <button
                          onClick={() => handleGenererUn(emp.id)}
                          disabled={busyRow === emp.id}
                          className="rounded-lg border border-brand-200 bg-brand-50 px-3 py-1.5 text-theme-xs font-semibold text-brand-600 hover:bg-brand-100 disabled:opacity-50 dark:border-brand-500/30 dark:bg-brand-500/10 dark:text-brand-400"
                        >
                          {busyRow === emp.id ? 'Calcul...' : 'Calculer le bulletin'}
                        </button>
                      </td>
                    )}
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Fiche employé + détail du bulletin */}
      {detail && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/40 backdrop-blur-sm p-4 overflow-y-auto">
          <div className="my-8 w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl dark:bg-gray-800">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="text-base font-bold text-gray-900 dark:text-white">
                {detail.prenom} {detail.nom}
              </h3>
              <button onClick={() => setDetailId(null)} className="text-gray-400 hover:text-gray-600">
                <HiOutlineX size={18} />
              </button>
            </div>

            <div className="grid grid-cols-2 gap-x-4 gap-y-2 text-theme-sm">
              {[
                ['Matricule', detail.matricule],
                ['CIN', detail.cin],
                ['N° CNSS', detail.cnss],
                ['RIB', detail.ribBancaire],
                ['Email', detail.email],
                ['Poste', detail.poste],
                ['Département', detail.departement],
                ['Type de contrat', detail.typeContrat],
                ['Date d\'embauche', detail.dateEmbauche],
                ['Fin de contrat', detail.dateFinContrat],
                ['Mode de salaire', detail.modeSalaire],
                ['Salaire saisi', detail.salaire != null ? `${fmt(detail.salaire)} DT` : null],
              ].map(([label, value]) => (
                <div key={label as string}>
                  <p className="text-theme-xs text-gray-400">{label}</p>
                  <p className="text-gray-800 dark:text-gray-200">{value || '—'}</p>
                </div>
              ))}
            </div>

            {detailBulletin ? (
              <div className="mt-5 rounded-xl bg-gray-50 p-4 dark:bg-gray-700/40">
                <p className="mb-2 text-theme-xs font-semibold uppercase tracking-wide text-gray-400">
                  Bulletin {detailBulletin.mois}
                </p>
                <div className="space-y-1 text-theme-sm">
                  {[
                    ['Salaire brut', detailBulletin.salaireBrut],
                    ['Brut effectif', detailBulletin.brutEffectif],
                    ['CNSS salarié', -detailBulletin.cnssSalarie],
                    ['Contribution solidarité', -detailBulletin.solidariteSalarie],
                    ['Salaire imposable', detailBulletin.salaireImposable],
                    ['Abattement', -detailBulletin.abattementMontant],
                    ['Revenu net imposable', detailBulletin.revenuNetImposable],
                    ['IRPP mensuel', -detailBulletin.irppMensuel],
                  ].map(([label, value]) => (
                    <div key={label as string} className="flex justify-between">
                      <span className="text-gray-500 dark:text-gray-400">{label}</span>
                      <span className="text-gray-800 dark:text-gray-200">{fmt(value as number)} DT</span>
                    </div>
                  ))}
                  <div className="flex justify-between border-t border-gray-200 pt-1.5 font-bold text-gray-800 dark:border-gray-600 dark:text-white">
                    <span>Net</span><span>{fmt(detailBulletin.net)} DT</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-500 dark:text-gray-400">Acomptes versés</span>
                    <span className="text-gray-800 dark:text-gray-200">{fmt(detailBulletin.acompte)} DT</span>
                  </div>
                  <div className="flex justify-between font-semibold text-gray-800 dark:text-white">
                    <span>Net à payer</span><span>{fmt(detailBulletin.netAPayer)} DT</span>
                  </div>

                  <p className="mb-1 mt-3 text-theme-xs font-semibold uppercase tracking-wide text-gray-400">
                    Charges employeur
                  </p>
                  {[
                    ['CNSS patronale', detailBulletin.cnssEmployeurDetail],
                    ['TFP', detailBulletin.tfpDetail],
                    ['FOPROLOS', detailBulletin.foprolosDetail],
                    ['Accidents du travail', detailBulletin.atDetail],
                  ].map(([label, value]) => (
                    <div key={label as string} className="flex justify-between">
                      <span className="text-gray-500 dark:text-gray-400">{label}</span>
                      <span className="text-gray-800 dark:text-gray-200">{fmt(value as number)} DT</span>
                    </div>
                  ))}
                  <div className="flex justify-between border-t border-gray-200 pt-1.5 font-bold text-gray-800 dark:border-gray-600 dark:text-white">
                    <span>Coût total employeur</span><span>{fmt(detailBulletin.coutTotal)} DT</span>
                  </div>
                </div>
              </div>
            ) : (
              <p className="mt-5 rounded-xl bg-gray-50 p-4 text-center text-theme-sm text-gray-400 dark:bg-gray-700/40">
                Aucun bulletin calculé pour {mois}
              </p>
            )}
          </div>
        </div>
      )}

      {/* Modal acompte */}
      {acompteTarget && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
          <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-2xl dark:bg-gray-800">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="text-base font-bold text-gray-900 dark:text-white flex items-center gap-2">
                <HiOutlineCurrencyDollar size={18} /> Acompte — {acompteTarget.prenom} {acompteTarget.nom}
              </h3>
              <button onClick={() => setAcompteTarget(null)} className="text-gray-400 hover:text-gray-600">
                <HiOutlineX size={18} />
              </button>
            </div>
            <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">Montant (DT)</label>
            <input
              type="number"
              value={acompteMontant}
              onChange={(e) => setAcompteMontant(e.target.value)}
              className="mb-3 h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-theme-sm dark:border-gray-600 dark:text-gray-200"
              autoFocus
            />
            <label className="mb-1 block text-theme-xs font-semibold text-gray-500 dark:text-gray-400">Note (optionnel)</label>
            <input
              type="text"
              value={acompteNote}
              onChange={(e) => setAcompteNote(e.target.value)}
              className="mb-4 h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-theme-sm dark:border-gray-600 dark:text-gray-200"
            />
            <div className="flex gap-2">
              <button
                onClick={() => setAcompteTarget(null)}
                className="flex-1 rounded-xl border border-gray-200 px-4 py-2.5 text-theme-sm font-medium text-gray-600 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-700"
              >
                Annuler
              </button>
              <button
                onClick={submitAcompte}
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

export default SalairesPage;
