import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  HiOutlineCheckCircle,
  HiOutlineClock,
  HiOutlineExclamation,
  HiOutlineExclamationCircle,
  HiOutlineFolderOpen,
  HiOutlineRefresh,
} from 'react-icons/hi';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { analyticsService, EtatEntrepot, ProjetsDashboard } from '../api/analyticsService';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../hooks/useTheme';

/**
 * Tableau de bord decisionnel « Projets ».
 *
 * Alimente par /api/analytics/projets, branche sur le schema `dwh`
 * (entrepot en etoile) et non sur les tables de saisie.
 */

/* Palette categorielle a ordre fixe, validee (clarte, chroma, separation CVD)
 * contre les surfaces claire (#FFFFFF) et sombre (#1A2231) de l'application. */
const PALETTE = {
  light: {
    s1: '#683B77', s2: '#eb6834', s3: '#2a78d6', s4: '#1baf7a', s5: '#eda100', s6: '#e87ba4',
    grid: '#E4E7EC', axis: '#98A2B3', surface: '#FFFFFF', ink: '#101828',
  },
  dark: {
    s1: '#AB78C3', s2: '#d95926', s3: '#3987e5', s4: '#199e70', s5: '#c98500', s6: '#d55181',
    grid: 'rgba(255,255,255,0.08)', axis: '#8B93A1', surface: '#1A2231', ink: '#F2F4F7',
  },
};

const LIBELLE_STATUT: Record<string, string> = {
  PLANIFIE: 'Planifie',
  EN_COURS: 'En cours',
  CLOTURE: 'Cloture',
  CLOTURE_INCOMPLET: 'Cloture incomplet',
  ANNULE: 'Annule',
  INCONNU: 'Inconnu',
};

const nb = (n: number | null | undefined, decimales = 0) =>
  (n ?? 0).toLocaleString('fr-TN', { minimumFractionDigits: decimales, maximumFractionDigits: decimales });

const premierJourIlYAUnAn = () => {
  const d = new Date();
  d.setMonth(d.getMonth() - 11, 1);
  return d.toISOString().slice(0, 10);
};

const aujourdhui = () => new Date().toISOString().slice(0, 10);

/* ── Briques d'interface ─────────────────────────────────────────────────── */

const Carte: React.FC<{ titre: string; sousTitre?: string; children: React.ReactNode }> = ({
  titre, sousTitre, children,
}) => (
  <div className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-dark">
    <h3 className="text-theme-sm font-semibold text-gray-800 dark:text-white">{titre}</h3>
    {sousTitre && <p className="mt-0.5 text-theme-xs text-gray-400">{sousTitre}</p>}
    <div className="mt-4">{children}</div>
  </div>
);

const Kpi: React.FC<{ icone: React.ReactNode; libelle: string; valeur: string; accent: string }> = ({
  icone, libelle, valeur, accent,
}) => (
  <div className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-dark">
    <div className={`flex h-10 w-10 items-center justify-center rounded-xl ${accent}`}>{icone}</div>
    <p className="mt-3 text-theme-xs text-gray-400">{libelle}</p>
    <p className="mt-1 text-xl font-bold text-gray-800 dark:text-white">{valeur}</p>
  </div>
);

const BandeauErreur: React.FC<{ message: string; onRetry: () => void }> = ({ message, onRetry }) => (
  <div className="flex items-start gap-3 rounded-2xl border border-error-200 bg-error-50 p-4 dark:border-error-500/30 dark:bg-error-500/10">
    <HiOutlineExclamationCircle size={20} className="mt-0.5 shrink-0 text-error-500" />
    <div className="min-w-0 flex-1">
      <p className="text-theme-sm font-semibold text-error-600 dark:text-error-400">Chargement impossible</p>
      <p className="mt-0.5 text-theme-sm text-error-600/80 dark:text-error-400/80">{message}</p>
    </div>
    <button
      onClick={onRetry}
      className="flex shrink-0 items-center gap-1.5 rounded-lg border border-error-300 px-3 py-1.5 text-theme-xs font-semibold text-error-600 hover:bg-error-100 dark:border-error-500/40 dark:text-error-400"
    >
      <HiOutlineRefresh size={14} /> Reessayer
    </button>
  </div>
);

/* ── Page ────────────────────────────────────────────────────────────────── */

const AnalytiquePage: React.FC = () => {
  const { theme } = useTheme();
  const { user } = useAuth();
  const c = theme === 'dark' ? PALETTE.dark : PALETTE.light;
  const estAdmin = !!user?.roles?.includes('ADMIN');

  const [debut, setDebut] = useState(premierJourIlYAUnAn);
  const [fin, setFin] = useState(aujourdhui);
  const [data, setData] = useState<ProjetsDashboard | null>(null);
  const [etat, setEtat] = useState<EtatEntrepot | null>(null);
  const [chargement, setChargement] = useState(true);
  const [rechargement, setRechargement] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);

  const charger = useCallback(async () => {
    setChargement(true);
    setErreur(null);
    try {
      const [dash, et] = await Promise.all([
        analyticsService.getProjets(debut, fin),
        analyticsService.getEtat(),
      ]);
      setData(dash.data.data);
      setEtat(et.data.data);
    } catch (e) {
      const err = e as { response?: { data?: { message?: string } }; message?: string };
      setErreur(err.response?.data?.message ?? err.message ?? 'Erreur inconnue');
    } finally {
      setChargement(false);
    }
  }, [debut, fin]);

  useEffect(() => { charger(); }, [charger]);

  const recharger = async () => {
    setRechargement(true);
    try {
      await analyticsService.refresh();
      await charger();
    } catch (e) {
      const err = e as { response?: { data?: { message?: string } } };
      setErreur(err.response?.data?.message ?? "Rechargement de l'entrepot impossible.");
    } finally {
      setRechargement(false);
    }
  };

  const couleurs = useMemo(() => [c.s1, c.s2, c.s3, c.s4, c.s5, c.s6], [c]);

  const infoBulle = useMemo(() => ({
    contentStyle: {
      background: c.surface,
      border: `1px solid ${theme === 'dark' ? 'rgba(255,255,255,0.12)' : '#E4E7EC'}`,
      borderRadius: 12,
      fontSize: 12,
      color: c.ink,
    },
    labelStyle: { color: c.ink, fontWeight: 600 },
  }), [c, theme]);

  const axe = { stroke: c.axis, fontSize: 11, tickLine: false, axisLine: false };

  const repartition = data?.repartitionProjets.map((r) => ({
    ...r,
    libelle: LIBELLE_STATUT[r.statut] ?? r.statut,
  })) ?? [];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-gray-800 dark:text-white">Analytique — Projets</h1>
          <p className="mt-1 text-theme-sm text-gray-500 dark:text-gray-400">
            Indicateurs issus de l'entrepot decisionnel (schema <code>dwh</code>).
            {etat?.dernierChargement && ` Donnees arretees au ${etat.dernierChargement}.`}
            {etat?.statut === 'JAMAIS_CHARGE' && " L'entrepot n'a jamais ete charge."}
          </p>
        </div>
        {estAdmin && (
          <button
            onClick={recharger}
            disabled={rechargement}
            className="flex items-center gap-2 rounded-lg bg-brand-500 px-4 py-2 text-theme-sm font-semibold text-white hover:bg-brand-600 disabled:opacity-60"
          >
            <HiOutlineRefresh size={16} className={rechargement ? 'animate-spin' : ''} />
            {rechargement ? 'Rechargement...' : "Recharger l'entrepot"}
          </button>
        )}
      </div>

      <div className="flex flex-wrap items-end gap-3 rounded-2xl border border-gray-200 bg-white p-4 dark:border-gray-800 dark:bg-gray-dark">
        <label className="flex flex-col gap-1">
          <span className="text-theme-xs text-gray-400">Du</span>
          <input
            type="date" value={debut} max={fin} onChange={(e) => setDebut(e.target.value)}
            className="rounded-lg border border-gray-300 bg-transparent px-3 py-1.5 text-theme-sm text-gray-800 dark:border-gray-700 dark:text-white"
          />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-theme-xs text-gray-400">Au</span>
          <input
            type="date" value={fin} min={debut} onChange={(e) => setFin(e.target.value)}
            className="rounded-lg border border-gray-300 bg-transparent px-3 py-1.5 text-theme-sm text-gray-800 dark:border-gray-700 dark:text-white"
          />
        </label>
      </div>

      {erreur && <BandeauErreur message={erreur} onRetry={charger} />}

      {chargement && !data && (
        <p className="py-16 text-center text-theme-sm text-gray-400">Chargement des indicateurs...</p>
      )}

      {data && (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <Kpi
              icone={<HiOutlineFolderOpen size={20} className="text-white" />}
              libelle="Projets actifs sur la periode"
              valeur={nb(data.kpis.projetsTotal)}
              accent="bg-brand-500"
            />
            <Kpi
              icone={<HiOutlineCheckCircle size={20} className="text-white" />}
              libelle="Taux de completion des taches"
              valeur={`${nb(data.kpis.tauxCompletion, 1)} %`}
              accent="bg-success-500"
            />
            <Kpi
              icone={<HiOutlineExclamation size={20} className="text-white" />}
              libelle="Taches en retard"
              valeur={nb(data.kpis.tachesEnRetard)}
              accent="bg-error-500"
            />
            <Kpi
              icone={<HiOutlineClock size={20} className="text-white" />}
              libelle="Delai moyen d'une tache"
              valeur={`${nb(data.kpis.delaiMoyenTacheJours, 1)} j`}
              accent="bg-warning-500"
            />
          </div>

          <p className="text-theme-xs text-gray-400">
            {nb(data.kpis.projetsEnCours)} projets en cours · {nb(data.kpis.projetsClotures)} clotures ·
            {' '}{nb(data.kpis.tachesTerminees)} taches terminees sur {nb(data.kpis.tachesTotal)} ·
            {' '}duree moyenne d'un projet {nb(data.kpis.dureeMoyenneProjetJours, 0)} jours
          </p>

          <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
            {/* Trois series de meme unite (nombre de taches) : un seul axe */}
            <Carte titre="Activite des taches par mois" sousTitre="Nombre de taches">
              <ResponsiveContainer width="100%" height={280}>
                <LineChart data={data.evolutionMensuelle} margin={{ top: 4, right: 8, left: -20, bottom: 0 }}>
                  <CartesianGrid stroke={c.grid} vertical={false} />
                  <XAxis dataKey="libelle" {...axe} />
                  <YAxis {...axe} />
                  <Tooltip {...infoBulle} />
                  <Legend iconType="plainline" wrapperStyle={{ fontSize: 12, color: c.axis }} />
                  <Line type="monotone" dataKey="tachesCreees" name="Creees" stroke={c.s1} strokeWidth={2} dot={{ r: 3 }} />
                  <Line type="monotone" dataKey="tachesTerminees" name="Terminees" stroke={c.s4} strokeWidth={2} dot={{ r: 3 }} />
                  <Line type="monotone" dataKey="tachesEnRetard" name="En retard" stroke={c.s2} strokeWidth={2} dot={{ r: 3 }} />
                </LineChart>
              </ResponsiveContainer>
            </Carte>

            <Carte titre="Repartition des projets par statut" sousTitre="Projets actifs sur la periode">
              {repartition.length === 0 ? (
                <p className="py-16 text-center text-theme-sm text-gray-400">Aucun projet sur la periode.</p>
              ) : (
                <>
                  <ResponsiveContainer width="100%" height={240}>
                    <PieChart>
                      <Pie
                        data={repartition} dataKey="nombre" nameKey="libelle"
                        innerRadius={55} outerRadius={95} paddingAngle={2}
                        stroke={c.surface} strokeWidth={2}
                      >
                        {repartition.map((_, i) => <Cell key={i} fill={couleurs[i % couleurs.length]} />)}
                      </Pie>
                      <Tooltip {...infoBulle} formatter={(v: number) => `${nb(v)} projets`} />
                      <Legend wrapperStyle={{ fontSize: 12, color: c.axis }} />
                    </PieChart>
                  </ResponsiveContainer>
                  <table className="mt-4 w-full text-theme-xs">
                    <tbody>
                      {repartition.map((r) => (
                        <tr key={r.statut} className="border-t border-gray-100 dark:border-gray-800">
                          <td className="py-1.5 text-gray-700 dark:text-gray-300">{r.libelle}</td>
                          <td className="py-1.5 text-right tabular-nums text-gray-700 dark:text-gray-300">{nb(r.nombre)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </>
              )}
            </Carte>

            <Carte titre="Charge par collaborateur" sousTitre="Taches assignees, 15 premiers">
              {data.chargeParCollaborateur.length === 0 ? (
                <p className="py-16 text-center text-theme-sm text-gray-400">Aucune tache assignee sur la periode.</p>
              ) : (
                <ResponsiveContainer width="100%" height={340}>
                  <BarChart
                    data={data.chargeParCollaborateur}
                    layout="vertical"
                    margin={{ top: 4, right: 24, left: 8, bottom: 0 }}
                  >
                    <CartesianGrid stroke={c.grid} horizontal={false} />
                    <XAxis type="number" {...axe} />
                    <YAxis type="category" dataKey="employe" width={130} {...axe} />
                    <Tooltip {...infoBulle} cursor={{ fill: c.grid }} />
                    <Legend wrapperStyle={{ fontSize: 12, color: c.axis }} />
                    <Bar dataKey="tachesTerminees" name="Terminees" fill={c.s4} radius={[0, 4, 4, 0]} maxBarSize={18} />
                    <Bar dataKey="tachesEnRetard" name="En retard" fill={c.s2} radius={[0, 4, 4, 0]} maxBarSize={18} />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </Carte>

            <Carte titre="Delai moyen de realisation par projet" sousTitre="Jours entre le debut et la fin d'execution">
              {data.delaiParProjet.length === 0 ? (
                <p className="py-16 text-center text-theme-sm text-gray-400">Aucune tache terminee sur la periode.</p>
              ) : (
                <ResponsiveContainer width="100%" height={340}>
                  <BarChart
                    data={data.delaiParProjet}
                    layout="vertical"
                    margin={{ top: 4, right: 24, left: 8, bottom: 0 }}
                  >
                    <CartesianGrid stroke={c.grid} horizontal={false} />
                    <XAxis type="number" unit=" j" {...axe} />
                    <YAxis type="category" dataKey="projet" width={130} {...axe} />
                    <Tooltip {...infoBulle} formatter={(v: number) => `${nb(v, 1)} jours`} />
                    <Bar dataKey="delaiMoyenJours" name="Delai moyen" fill={c.s3} radius={[0, 4, 4, 0]} maxBarSize={18} />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </Carte>
          </div>

          <Carte titre="Projets a risque" sousTitre="Projets ouverts comportant au moins une tache en retard">
            {data.projetsARisque.length === 0 ? (
              <p className="py-10 text-center text-theme-sm text-gray-400">Aucun projet a risque — aucune tache en retard.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-theme-sm">
                  <thead>
                    <tr className="text-left text-gray-400">
                      <th className="pb-2 font-medium">Projet</th>
                      <th className="pb-2 font-medium">Client</th>
                      <th className="pb-2 font-medium">Statut</th>
                      <th className="pb-2 font-medium">Echeance</th>
                      <th className="pb-2 text-right font-medium">Taches</th>
                      <th className="pb-2 text-right font-medium">En retard</th>
                      <th className="pb-2 text-right font-medium">Completion</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.projetsARisque.map((p) => (
                      <tr key={p.projet} className="border-t border-gray-100 dark:border-gray-800">
                        <td className="py-2 text-gray-800 dark:text-white">{p.projet}</td>
                        <td className="py-2 text-gray-500 dark:text-gray-400">{p.client}</td>
                        <td className="py-2 text-gray-500 dark:text-gray-400">{LIBELLE_STATUT[p.statut] ?? p.statut}</td>
                        <td className="py-2 text-gray-500 dark:text-gray-400">{p.dateFin ?? '—'}</td>
                        <td className="py-2 text-right tabular-nums text-gray-800 dark:text-white">{nb(p.tachesTotal)}</td>
                        <td className="py-2 text-right tabular-nums text-error-500">{nb(p.tachesEnRetard)}</td>
                        <td className="py-2 text-right tabular-nums text-gray-800 dark:text-white">{nb(p.tauxCompletion, 1)} %</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Carte>
        </>
      )}
    </div>
  );
};

export default AnalytiquePage;
