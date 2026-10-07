import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  HiOutlineCash,
  HiOutlineClock,
  HiOutlineRefresh,
  HiOutlineScale,
  HiOutlineTrendingUp,
} from 'react-icons/hi';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ComposedChart,
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
import { analyticsService, EtatEntrepot, FinanceDashboard } from '../api/analyticsService';
import ErreurBanner from '../components/ui/ErreurBanner';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../hooks/useTheme';
import { messageErreur } from '../utils/apiError';

/**
 * Tableau de bord decisionnel « Finance et tresorerie ».
 *
 * Alimente par /api/analytics/finance, lui-meme branche sur le schema `dwh`
 * (entrepot en etoile) — et non sur les tables de saisie.
 */

/* Palette categorielle a ordre fixe, validee (clarte, chroma, separation CVD)
 * contre les surfaces claire (#FFFFFF) et sombre (#1A2231) de l'application.
 * Les series 4 a 6 passent sous 3:1 en mode clair : legende et tableau de
 * valeurs obligatoires partout ou elles servent. */
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

const LIBELLE_NATURE: Record<string, string> = {
  SALAIRE: 'Salaires',
  CHARGE_FIXE: 'Charges fixes',
  CHARGE_VARIABLE: 'Charges variables',
};

const dt = (n: number | null | undefined) =>
  (n ?? 0).toLocaleString('fr-TN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const axeCourt = (n: number) => {
  const abs = Math.abs(n);
  if (abs >= 1000) return `${(n / 1000).toLocaleString('fr-TN', { maximumFractionDigits: 1 })}k`;
  return `${n}`;
};

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

/* ── Page ────────────────────────────────────────────────────────────────── */

const AnalytiquePage: React.FC = () => {
  const { theme } = useTheme();
  const { user } = useAuth();
  const c = theme === 'dark' ? PALETTE.dark : PALETTE.light;
  const estAdmin = !!user?.roles?.includes('ADMIN');

  const [debut, setDebut] = useState(premierJourIlYAUnAn);
  const [fin, setFin] = useState(aujourdhui);
  const [data, setData] = useState<FinanceDashboard | null>(null);
  const [etat, setEtat] = useState<EtatEntrepot | null>(null);
  const [chargement, setChargement] = useState(true);
  const [rechargement, setRechargement] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);

  const charger = useCallback(async () => {
    setChargement(true);
    setErreur(null);
    try {
      const [dash, et] = await Promise.all([
        analyticsService.getFinance(debut, fin),
        analyticsService.getEtat(),
      ]);
      setData(dash.data.data);
      setEtat(et.data.data);
    } catch (e) {
      setErreur(messageErreur(e));
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
      setErreur(messageErreur(e));
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

  const charges = data?.repartitionCharges.map((r) => ({
    ...r,
    libelle: LIBELLE_NATURE[r.nature] ?? r.nature,
  })) ?? [];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-gray-800 dark:text-white">Analytique — Finance et tresorerie</h1>
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

      {erreur && <ErreurBanner message={erreur} onRetry={charger} />}

      {chargement && !data && (
        <p className="py-16 text-center text-theme-sm text-gray-400">Chargement des indicateurs...</p>
      )}

      {data && (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <Kpi
              icone={<HiOutlineTrendingUp size={20} className="text-white" />}
              libelle="Produits HT"
              valeur={`${dt(data.kpis.produitsHt)} DT`}
              accent="bg-success-500"
            />
            <Kpi
              icone={<HiOutlineCash size={20} className="text-white" />}
              libelle="Charges (dont salaires)"
              valeur={`${dt(data.kpis.chargesTotales)} DT`}
              accent="bg-error-500"
            />
            <Kpi
              icone={<HiOutlineScale size={20} className="text-white" />}
              libelle={`Resultat — marge ${dt(data.kpis.margePourcent)} %`}
              valeur={`${dt(data.kpis.resultat)} DT`}
              accent="bg-brand-500"
            />
            <Kpi
              icone={<HiOutlineClock size={20} className="text-white" />}
              libelle="Creances en retard"
              valeur={`${dt(data.kpis.creancesEnRetard)} DT`}
              accent="bg-warning-500"
            />
          </div>

          <p className="text-theme-xs text-gray-400">
            Masse salariale {dt(data.kpis.masseSalariale)} DT · encaisse {dt(data.kpis.encaissements)} DT ·
            {' '}decaisse {dt(data.kpis.decaissements)} DT · tresorerie nette {dt(data.kpis.tresorerieNette)} DT ·
            {' '}creances en cours {dt(data.kpis.creancesEnCours)} DT
          </p>

          <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
            {/* Produits, charges et resultat : meme unite (DT), un seul axe */}
            <Carte titre="Produits, charges et resultat par mois" sousTitre="Base engagement, en dinars HT">
              <ResponsiveContainer width="100%" height={280}>
                <ComposedChart data={data.evolutionMensuelle} margin={{ top: 4, right: 8, left: -12, bottom: 0 }}>
                  <CartesianGrid stroke={c.grid} vertical={false} />
                  <XAxis dataKey="libelle" {...axe} />
                  <YAxis tickFormatter={axeCourt} {...axe} />
                  <Tooltip {...infoBulle} cursor={{ fill: c.grid }} formatter={(v: number) => `${dt(v)} DT`} />
                  <Legend wrapperStyle={{ fontSize: 12, color: c.axis }} />
                  <Bar dataKey="produits" name="Produits" fill={c.s4} radius={[4, 4, 0, 0]} maxBarSize={22} />
                  <Bar dataKey="charges" name="Charges" fill={c.s2} radius={[4, 4, 0, 0]} maxBarSize={22} />
                  <Line type="monotone" dataKey="resultat" name="Resultat" stroke={c.s1} strokeWidth={2} dot={{ r: 3 }} />
                </ComposedChart>
              </ResponsiveContainer>
            </Carte>

            <Carte titre="Encaissements et decaissements par mois" sousTitre="Base tresorerie, en dinars TTC">
              <ResponsiveContainer width="100%" height={280}>
                <LineChart data={data.evolutionMensuelle} margin={{ top: 4, right: 8, left: -12, bottom: 0 }}>
                  <CartesianGrid stroke={c.grid} vertical={false} />
                  <XAxis dataKey="libelle" {...axe} />
                  <YAxis tickFormatter={axeCourt} {...axe} />
                  <Tooltip {...infoBulle} formatter={(v: number) => `${dt(v)} DT`} />
                  <Legend iconType="plainline" wrapperStyle={{ fontSize: 12, color: c.axis }} />
                  <Line type="monotone" dataKey="encaissements" name="Encaisse" stroke={c.s3} strokeWidth={2} dot={{ r: 3 }} />
                  <Line type="monotone" dataKey="decaissements" name="Decaisse" stroke={c.s2} strokeWidth={2} dot={{ r: 3 }} />
                </LineChart>
              </ResponsiveContainer>
            </Carte>

            <Carte titre="Structure des charges" sousTitre="Base engagement, en dinars HT">
              {charges.length === 0 ? (
                <p className="py-16 text-center text-theme-sm text-gray-400">Aucune charge sur la periode.</p>
              ) : (
                <>
                  <ResponsiveContainer width="100%" height={240}>
                    <PieChart>
                      <Pie
                        data={charges} dataKey="montant" nameKey="libelle"
                        innerRadius={55} outerRadius={95} paddingAngle={2}
                        stroke={c.surface} strokeWidth={2}
                      >
                        {charges.map((_, i) => <Cell key={i} fill={couleurs[i % couleurs.length]} />)}
                      </Pie>
                      <Tooltip {...infoBulle} formatter={(v: number) => `${dt(v)} DT`} />
                      <Legend wrapperStyle={{ fontSize: 12, color: c.axis }} />
                    </PieChart>
                  </ResponsiveContainer>
                  <table className="mt-4 w-full text-theme-xs">
                    <tbody>
                      {charges.map((r) => (
                        <tr key={r.nature} className="border-t border-gray-100 dark:border-gray-800">
                          <td className="py-1.5 text-gray-700 dark:text-gray-300">{r.libelle}</td>
                          <td className="py-1.5 text-right tabular-nums text-gray-700 dark:text-gray-300">{dt(r.montant)} DT</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </>
              )}
            </Carte>

            <Carte titre="Balance agee des creances" sousTitre="Reste du par anciennete de l'echeance">
              {data.balanceAgee.length === 0 ? (
                <p className="py-16 text-center text-theme-sm text-gray-400">Aucune creance ouverte sur la periode.</p>
              ) : (
                <ResponsiveContainer width="100%" height={240}>
                  <BarChart data={data.balanceAgee} margin={{ top: 4, right: 8, left: -12, bottom: 0 }}>
                    <CartesianGrid stroke={c.grid} vertical={false} />
                    <XAxis dataKey="tranche" {...axe} />
                    <YAxis tickFormatter={axeCourt} {...axe} />
                    <Tooltip {...infoBulle} cursor={{ fill: c.grid }} formatter={(v: number) => `${dt(v)} DT`} />
                    <Bar dataKey="montant" name="Reste du" fill={c.s2} radius={[4, 4, 0, 0]} maxBarSize={48} />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </Carte>
          </div>

          <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
            <Carte titre="Chiffre d'affaires par client" sousTitre="10 premiers clients, en dinars HT">
              {data.topClients.length === 0 ? (
                <p className="py-10 text-center text-theme-sm text-gray-400">Aucune facture sur la periode.</p>
              ) : (
                <ResponsiveContainer width="100%" height={300}>
                  <BarChart data={data.topClients} layout="vertical" margin={{ top: 4, right: 24, left: 8, bottom: 0 }}>
                    <CartesianGrid stroke={c.grid} horizontal={false} />
                    <XAxis type="number" tickFormatter={axeCourt} {...axe} />
                    <YAxis type="category" dataKey="client" width={130} {...axe} />
                    <Tooltip {...infoBulle} cursor={{ fill: c.grid }} formatter={(v: number) => `${dt(v)} DT`} />
                    <Bar dataKey="caHt" name="CA HT" fill={c.s1} radius={[0, 4, 4, 0]} maxBarSize={20} />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </Carte>

            <Carte titre="Factures en retard de paiement" sousTitre="15 plus anciennes">
              {data.facturesEnRetard.length === 0 ? (
                <p className="py-10 text-center text-theme-sm text-gray-400">Aucune facture en retard.</p>
              ) : (
                <div className="max-h-[300px] overflow-auto">
                  <table className="w-full text-theme-sm">
                    <thead className="sticky top-0 bg-white dark:bg-gray-dark">
                      <tr className="text-left text-gray-400">
                        <th className="pb-2 font-medium">Numero</th>
                        <th className="pb-2 font-medium">Client</th>
                        <th className="pb-2 font-medium">Echeance</th>
                        <th className="pb-2 text-right font-medium">Reste du</th>
                        <th className="pb-2 text-right font-medium">Retard</th>
                      </tr>
                    </thead>
                    <tbody>
                      {data.facturesEnRetard.map((f) => (
                        <tr key={f.numero} className="border-t border-gray-100 dark:border-gray-800">
                          <td className="py-2 text-gray-800 dark:text-white">{f.numero}</td>
                          <td className="py-2 text-gray-500 dark:text-gray-400">{f.client}</td>
                          <td className="py-2 text-gray-500 dark:text-gray-400">{f.dateEcheance ?? '—'}</td>
                          <td className="py-2 text-right tabular-nums text-gray-800 dark:text-white">{dt(f.resteDu)} DT</td>
                          <td className="py-2 text-right tabular-nums text-error-500">{f.joursRetard} j</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </Carte>
          </div>
        </>
      )}
    </div>
  );
};

export default AnalytiquePage;
