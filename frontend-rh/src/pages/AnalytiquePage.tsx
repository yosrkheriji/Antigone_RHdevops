import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  HiOutlineClock,
  HiOutlineExclamationCircle,
  HiOutlineHome,
  HiOutlineRefresh,
  HiOutlineUserGroup,
  HiOutlineUsers,
} from 'react-icons/hi';
import {
  Area,
  AreaChart,
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
import { analyticsService, EtatEntrepot, PresenceDashboard } from '../api/analyticsService';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../hooks/useTheme';

/**
 * Tableau de bord decisionnel « Presence et productivite ».
 *
 * Les donnees viennent du schema `dwh` (entrepot en etoile) via /api/analytics,
 * pas des tables de saisie : les memes chiffres sont restituees par Power BI
 * branche sur ce meme entrepot.
 */

/* ── Palette categorielle ────────────────────────────────────────────────
 * Ordre fixe, jamais recycle. Validee (bandes de clarte, plancher de chroma,
 * separation deuteranope/protanope/tritanope) contre les deux surfaces de
 * l'application. Les series 4 a 6 passent sous 3:1 en mode clair : elles sont
 * donc toujours accompagnees d'une legende et d'un tableau de valeurs.
 */
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

const nb = (n: number | null | undefined, decimales = 0) =>
  (n ?? 0).toLocaleString('fr-TN', { minimumFractionDigits: decimales, maximumFractionDigits: decimales });

const premierJourIlYAUnAn = () => {
  const d = new Date();
  d.setMonth(d.getMonth() - 11, 1);
  return d.toISOString().slice(0, 10);
};

const aujourdhui = () => new Date().toISOString().slice(0, 10);

/* ── Briques d'interface ─────────────────────────────────────────────────── */

const Carte: React.FC<{ titre: string; sousTitre?: string; children: React.ReactNode; className?: string }> = ({
  titre, sousTitre, children, className = '',
}) => (
  <div className={`rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-dark ${className}`}>
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

/** Tableau de valeurs : sert de relais quand une teinte passe sous 3:1 de contraste. */
const TableauValeurs: React.FC<{ entetes: string[]; lignes: (string | number)[][] }> = ({ entetes, lignes }) => (
  <div className="mt-4 max-h-56 overflow-auto">
    <table className="w-full text-theme-xs">
      <thead className="sticky top-0 bg-white dark:bg-gray-dark">
        <tr className="text-left text-gray-400">
          {entetes.map((e, i) => (
            <th key={e} className={`pb-2 font-medium ${i > 0 ? 'text-right' : ''}`}>{e}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {lignes.map((ligne, i) => (
          <tr key={i} className="border-t border-gray-100 dark:border-gray-800">
            {ligne.map((cell, j) => (
              <td key={j} className={`py-1.5 ${j > 0 ? 'text-right tabular-nums' : ''} text-gray-700 dark:text-gray-300`}>
                {cell}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
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
  const [departement, setDepartement] = useState('');
  const [departements, setDepartements] = useState<string[]>([]);
  const [data, setData] = useState<PresenceDashboard | null>(null);
  const [etat, setEtat] = useState<EtatEntrepot | null>(null);
  const [chargement, setChargement] = useState(true);
  const [rechargement, setRechargement] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);

  const charger = useCallback(async () => {
    setChargement(true);
    setErreur(null);
    try {
      const [dash, et] = await Promise.all([
        analyticsService.getPresence(debut, fin, departement || undefined),
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
  }, [debut, fin, departement]);

  useEffect(() => { charger(); }, [charger]);

  useEffect(() => {
    analyticsService.getDepartements()
      .then((r) => setDepartements(r.data.data))
      .catch(() => setDepartements([]));
  }, []);

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

  const couleursStatut = useMemo(() => [c.s1, c.s2, c.s3, c.s4, c.s5, c.s6], [c]);

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

  return (
    <div className="space-y-6">
      {/* En-tete */}
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-gray-800 dark:text-white">Analytique — Presence et productivite</h1>
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

      {/* Filtres — une seule rangee, au-dessus des visuels */}
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
        <label className="flex flex-col gap-1">
          <span className="text-theme-xs text-gray-400">Departement</span>
          <select
            value={departement} onChange={(e) => setDepartement(e.target.value)}
            className="rounded-lg border border-gray-300 bg-transparent px-3 py-1.5 text-theme-sm text-gray-800 dark:border-gray-700 dark:bg-gray-dark dark:text-white"
          >
            <option value="">Tous</option>
            {departements.map((d) => <option key={d} value={d}>{d}</option>)}
          </select>
        </label>
      </div>

      {erreur && <BandeauErreur message={erreur} onRetry={charger} />}

      {chargement && !data && (
        <p className="py-16 text-center text-theme-sm text-gray-400">Chargement des indicateurs...</p>
      )}

      {data && (
        <>
          {/* KPI */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <Kpi
              icone={<HiOutlineUsers size={20} className="text-white" />}
              libelle="Taux de presence"
              valeur={`${nb(data.kpis.tauxPresence, 1)} %`}
              accent="bg-brand-500"
            />
            <Kpi
              icone={<HiOutlineUserGroup size={20} className="text-white" />}
              libelle="Taux d'absenteisme"
              valeur={`${nb(data.kpis.tauxAbsenteisme, 1)} %`}
              accent="bg-error-500"
            />
            <Kpi
              icone={<HiOutlineClock size={20} className="text-white" />}
              libelle="Heures travaillees"
              valeur={`${nb(data.kpis.heuresTravaillees)} h`}
              accent="bg-success-500"
            />
            <Kpi
              icone={<HiOutlineHome size={20} className="text-white" />}
              libelle="Part de teletravail"
              valeur={`${nb(data.kpis.tauxTeletravail, 1)} %`}
              accent="bg-warning-500"
            />
          </div>

          <p className="text-theme-xs text-gray-400">
            {nb(data.kpis.joursSuivis)} journees suivies · {nb(data.kpis.effectifSuivi)} collaborateurs ·
            {' '}{nb(data.kpis.joursRetard)} jours avec retard · retard moyen {nb(data.kpis.retardMoyenMinutes, 1)} min
          </p>

          <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
            {/* Taux mensuels — deux series, meme unite, un seul axe */}
            <Carte titre="Presence et absenteisme par mois" sousTitre="En pourcentage des journees suivies">
              <ResponsiveContainer width="100%" height={260}>
                <LineChart data={data.evolutionMensuelle} margin={{ top: 4, right: 8, left: -20, bottom: 0 }}>
                  <CartesianGrid stroke={c.grid} vertical={false} />
                  <XAxis dataKey="libelle" {...axe} />
                  <YAxis {...axe} unit="%" />
                  <Tooltip {...infoBulle} formatter={(v: number) => `${nb(v, 1)} %`} />
                  <Legend iconType="plainline" wrapperStyle={{ fontSize: 12, color: c.axis }} />
                  <Line type="monotone" dataKey="tauxPresence" name="Presence" stroke={c.s1} strokeWidth={2} dot={{ r: 3 }} />
                  <Line type="monotone" dataKey="tauxAbsenteisme" name="Absenteisme" stroke={c.s2} strokeWidth={2} dot={{ r: 3 }} />
                </LineChart>
              </ResponsiveContainer>
            </Carte>

            {/* Heures : unite differente des taux — donc graphique separe, jamais un second axe */}
            <Carte titre="Heures travaillees par mois" sousTitre="Somme des heures pointees">
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={data.evolutionMensuelle} margin={{ top: 4, right: 8, left: -20, bottom: 0 }}>
                  <CartesianGrid stroke={c.grid} vertical={false} />
                  <XAxis dataKey="libelle" {...axe} />
                  <YAxis {...axe} />
                  <Tooltip {...infoBulle} cursor={{ fill: c.grid }} formatter={(v: number) => `${nb(v)} h`} />
                  <Bar dataKey="heures" name="Heures" fill={c.s3} radius={[4, 4, 0, 0]} maxBarSize={38} />
                </BarChart>
              </ResponsiveContainer>
            </Carte>

            {/* Repartition par statut — legende + tableau (teintes 4 a 6 sous 3:1 en clair) */}
            <Carte titre="Repartition des journees par statut" sousTitre="Nombre de journees">
              <ResponsiveContainer width="100%" height={240}>
                <PieChart>
                  <Pie
                    data={data.repartitionStatuts}
                    dataKey="jours"
                    nameKey="statut"
                    innerRadius={55}
                    outerRadius={95}
                    paddingAngle={2}
                    stroke={c.surface}
                    strokeWidth={2}
                  >
                    {data.repartitionStatuts.map((_, i) => (
                      <Cell key={i} fill={couleursStatut[i % couleursStatut.length]} />
                    ))}
                  </Pie>
                  <Tooltip {...infoBulle} formatter={(v: number) => `${nb(v)} jours`} />
                  <Legend wrapperStyle={{ fontSize: 12, color: c.axis }} />
                </PieChart>
              </ResponsiveContainer>
              <TableauValeurs
                entetes={['Statut', 'Journees']}
                lignes={data.repartitionStatuts.map((s) => [s.statut, nb(s.jours)])}
              />
            </Carte>

            <Carte titre="Taux de presence par departement" sousTitre="Toutes equipes, filtre departement ignore">
              <ResponsiveContainer width="100%" height={240}>
                <BarChart
                  data={data.parDepartement}
                  layout="vertical"
                  margin={{ top: 4, right: 24, left: 8, bottom: 0 }}
                >
                  <CartesianGrid stroke={c.grid} horizontal={false} />
                  <XAxis type="number" unit="%" domain={[0, 100]} {...axe} />
                  <YAxis type="category" dataKey="departement" width={120} {...axe} />
                  <Tooltip {...infoBulle} cursor={{ fill: c.grid }} formatter={(v: number) => `${nb(v, 1)} %`} />
                  <Bar dataKey="tauxPresence" name="Presence" fill={c.s1} radius={[0, 4, 4, 0]} maxBarSize={22} />
                </BarChart>
              </ResponsiveContainer>
            </Carte>

            <Carte titre="Inactivite excedentaire par semaine" sousTitre="Minutes au-dela de la tolerance (agent poste de travail)">
              {data.inactivite.length === 0 ? (
                <p className="py-16 text-center text-theme-sm text-gray-400">Aucun rapport d'inactivite sur la periode.</p>
              ) : (
                <ResponsiveContainer width="100%" height={240}>
                  <AreaChart data={data.inactivite} margin={{ top: 4, right: 8, left: -20, bottom: 0 }}>
                    <defs>
                      <linearGradient id="degradeInactivite" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor={c.s2} stopOpacity={0.35} />
                        <stop offset="100%" stopColor={c.s2} stopOpacity={0.02} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid stroke={c.grid} vertical={false} />
                    <XAxis dataKey="semaine" {...axe} />
                    <YAxis {...axe} />
                    <Tooltip {...infoBulle} formatter={(v: number) => `${nb(v)} min`} />
                    <Area
                      type="monotone" dataKey="minutesExcedentaires" name="Minutes excedentaires"
                      stroke={c.s2} strokeWidth={2} fill="url(#degradeInactivite)"
                    />
                  </AreaChart>
                </ResponsiveContainer>
              )}
            </Carte>

            <Carte titre="Jours de conges approuves par type" sousTitre="Somme des jours sur la periode">
              {data.conges.length === 0 ? (
                <p className="py-16 text-center text-theme-sm text-gray-400">Aucun conge approuve sur la periode.</p>
              ) : (
                <ResponsiveContainer width="100%" height={240}>
                  <BarChart data={data.conges} layout="vertical" margin={{ top: 4, right: 24, left: 8, bottom: 0 }}>
                    <CartesianGrid stroke={c.grid} horizontal={false} />
                    <XAxis type="number" {...axe} />
                    <YAxis type="category" dataKey="typeConge" width={150} {...axe} />
                    <Tooltip {...infoBulle} cursor={{ fill: c.grid }} formatter={(v: number) => `${nb(v)} jours`} />
                    <Bar dataKey="jours" name="Jours" fill={c.s4} radius={[0, 4, 4, 0]} maxBarSize={22} />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </Carte>
          </div>

          <Carte titre="Retards cumules — 10 premiers collaborateurs" sousTitre="Sur la periode selectionnee">
            {data.topRetards.length === 0 ? (
              <p className="py-10 text-center text-theme-sm text-gray-400">Aucun retard enregistre sur la periode.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-theme-sm">
                  <thead>
                    <tr className="text-left text-gray-400">
                      <th className="pb-2 font-medium">Collaborateur</th>
                      <th className="pb-2 font-medium">Departement</th>
                      <th className="pb-2 text-right font-medium">Retard cumule</th>
                      <th className="pb-2 text-right font-medium">Jours en retard</th>
                      <th className="pb-2 text-right font-medium">Heures travaillees</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.topRetards.map((l) => (
                      <tr key={l.employe} className="border-t border-gray-100 dark:border-gray-800">
                        <td className="py-2 text-gray-800 dark:text-white">{l.employe}</td>
                        <td className="py-2 text-gray-500 dark:text-gray-400">{l.departement}</td>
                        <td className="py-2 text-right tabular-nums text-gray-800 dark:text-white">
                          {nb(Math.floor(l.retardTotalMinutes / 60))} h {nb(l.retardTotalMinutes % 60)} min
                        </td>
                        <td className="py-2 text-right tabular-nums text-gray-800 dark:text-white">{nb(l.joursRetard)}</td>
                        <td className="py-2 text-right tabular-nums text-gray-500 dark:text-gray-400">{nb(l.heures)} h</td>
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
