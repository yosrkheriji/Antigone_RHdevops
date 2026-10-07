import React, { useEffect, useMemo, useState } from 'react';
import {
  HiOutlineArrowSmDown,
  HiOutlineArrowSmUp,
  HiOutlineCash,
  HiOutlineChevronLeft,
  HiOutlineChevronRight,
  HiOutlineClock,
  HiOutlineCreditCard,
  HiOutlineScale,
  HiOutlineTrendingDown,
  HiOutlineTrendingUp,
} from 'react-icons/hi';
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import type { DefaultLegendContentProps, TooltipContentProps } from 'recharts';
import { dashboardService } from '../api/dashboardService';
import ErreurBanner from '../components/ui/ErreurBanner';
import { useTheme } from '../hooks/useTheme';
import { messageErreur } from '../utils/apiError';
import { DeclarationTva, ResultatNet, VueDecaissements, VueEncaissements } from '../types';

const fmt = (n: number | null | undefined) =>
  (n ?? 0).toLocaleString('fr-TN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const axisFmt = (n: number) => {
  const abs = Math.abs(n);
  if (abs >= 1000) return `${(n / 1000).toLocaleString('fr-TN', { maximumFractionDigits: 1 })}k`;
  return `${n}`;
};

const currentMois = () => new Date().toISOString().slice(0, 7);

const moisOffset = (mois: string, delta: number) => {
  const [y, m] = mois.split('-').map(Number);
  const d = new Date(y, m - 1 + delta, 1);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
};

const moisLabel = (mois: string) => {
  const [y, m] = mois.split('-').map(Number);
  const label = new Date(y, m - 1, 1).toLocaleDateString('fr-FR', { month: 'short' });
  return (label.charAt(0).toUpperCase() + label.slice(1)).replace(/\.$/, '');
};

// Palette catégorielle validée CVD (voir skill dataviz) : violet marque / bleu / orange.
const PALETTE = {
  light: {
    purple: '#683B77',
    blue: '#3B82F6',
    orange: '#FB6514',
    grid: '#E4E7EC',
    axis: '#98A2B3',
    surface: '#FFFFFF',
  },
  dark: {
    purple: '#AB78C3',
    blue: '#3B82F6',
    orange: '#EC4A0A',
    grid: 'rgba(255,255,255,0.08)',
    axis: '#8B93A1',
    surface: '#1A2231',
  },
};

const Ligne: React.FC<{ label: string; value: number; accent?: 'error' | 'success' | 'none'; bold?: boolean }> = ({
  label,
  value,
  accent = 'none',
  bold,
}) => (
  <div className={`flex justify-between py-1 ${bold ? 'border-t border-gray-200 pt-2 dark:border-gray-700' : ''}`}>
    <span className={bold ? 'font-semibold text-gray-700 dark:text-gray-200' : 'text-gray-500 dark:text-gray-400'}>
      {label}
    </span>
    <span
      className={`${bold ? 'font-bold' : ''} ${
        accent === 'error'
          ? 'text-error-500'
          : accent === 'success'
            ? 'text-success-500'
            : 'text-gray-800 dark:text-white'
      }`}
    >
      {fmt(value)} DT
    </span>
  </div>
);

const DeltaBadge: React.FC<{ percent: number; invert?: boolean }> = ({ percent, invert }) => {
  const up = percent >= 0;
  const good = invert ? !up : up;
  return (
    <span
      className={`inline-flex items-center gap-0.5 rounded-full px-2 py-0.5 text-theme-xs font-semibold ${
        good
          ? 'bg-success-50 text-success-600 dark:bg-success-500/10 dark:text-success-400'
          : 'bg-error-50 text-error-600 dark:bg-error-500/10 dark:text-error-400'
      }`}
    >
      {up ? <HiOutlineArrowSmUp size={14} /> : <HiOutlineArrowSmDown size={14} />}
      {Math.abs(percent).toFixed(1)}%
    </span>
  );
};

const StatTile: React.FC<{
  icon: React.ReactNode;
  label: string;
  value: string;
  accentClass: string;
  deltaPercent?: number | null;
  invert?: boolean;
}> = ({ icon, label, value, accentClass, deltaPercent, invert }) => (
  <div className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-dark">
    <div className="flex items-center justify-between">
      <div className={`flex h-10 w-10 items-center justify-center rounded-xl ${accentClass}`}>{icon}</div>
      {deltaPercent != null && <DeltaBadge percent={deltaPercent} invert={invert} />}
    </div>
    <p className="mt-3 text-theme-xs text-gray-400">{label}</p>
    <p className="mt-1 text-xl font-bold text-gray-800 dark:text-white">{value}</p>
  </div>
);

const MeterTile: React.FC<{ percent: number; deltaPoints?: number | null }> = ({ percent, deltaPoints }) => {
  const positive = percent >= 0;
  const width = Math.min(100, Math.abs(percent));
  return (
    <div className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-dark">
      <div className="flex items-center justify-between">
        <div
          className={`flex h-10 w-10 items-center justify-center rounded-xl ${
            positive
              ? 'bg-brand-50 text-brand-500 dark:bg-brand-500/10 dark:text-brand-300'
              : 'bg-error-50 text-error-500 dark:bg-error-500/10'
          }`}
        >
          <HiOutlineScale size={20} />
        </div>
        {deltaPoints != null && <DeltaBadge percent={deltaPoints} />}
      </div>
      <p className="mt-3 text-theme-xs text-gray-400">Marge</p>
      <p className="mt-1 text-xl font-bold text-gray-800 dark:text-white">{percent.toFixed(1)}%</p>
      <div className="mt-2.5 h-2 w-full overflow-hidden rounded-full bg-brand-50 dark:bg-brand-500/10">
        <div
          className={`h-full rounded-full transition-all ${positive ? 'bg-brand-500' : 'bg-error-500'}`}
          style={{ width: `${width}%` }}
        />
      </div>
    </div>
  );
};

interface Segment {
  label: string;
  value: number;
  barClass: string;
  dotClass: string;
}

const CompositionBar: React.FC<{ segments: Segment[]; total: number }> = ({ segments, total }) => (
  <div>
    <div className="flex h-3 w-full gap-0.5 overflow-hidden rounded-full bg-gray-100 dark:bg-gray-800">
      {segments.map((s) => {
        const pct = total > 0 ? (s.value / total) * 100 : 0;
        if (pct <= 0) return null;
        return (
          <div
            key={s.label}
            className={`h-full rounded-full ${s.barClass}`}
            style={{ width: `${pct}%` }}
            title={`${s.label} — ${pct.toFixed(1)}%`}
          />
        );
      })}
    </div>
    <div className="mt-3 flex flex-wrap gap-x-5 gap-y-1.5">
      {segments.map((s) => (
        <div key={s.label} className="flex items-center gap-1.5 text-theme-xs">
          <span className={`h-2 w-2 rounded-full ${s.dotClass}`} />
          <span className="text-gray-500 dark:text-gray-400">{s.label}</span>
          <span className="font-semibold text-gray-700 dark:text-gray-200">
            {total > 0 ? ((s.value / total) * 100).toFixed(0) : 0}%
          </span>
        </div>
      ))}
    </div>
  </div>
);

interface TrendPoint {
  mois: string;
  moisLabel: string;
  revenus: number;
  depenses: number;
  marge: number;
}

const ChartTooltip: React.FC<TooltipContentProps<number, string>> = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-xl border border-gray-200 bg-white px-3 py-2 shadow-theme-lg dark:border-gray-700 dark:bg-gray-dark">
      <p className="mb-1.5 text-theme-xs font-semibold text-gray-500 dark:text-gray-400">{label}</p>
      <div className="space-y-1">
        {payload.map((p) => (
          <div key={p.dataKey as string} className="flex items-center justify-between gap-5 text-theme-xs">
            <span className="flex items-center gap-1.5 text-gray-500 dark:text-gray-400">
              <span className="h-2 w-2 rounded-full" style={{ backgroundColor: p.color }} />
              {p.name}
            </span>
            <span className="font-semibold text-gray-800 dark:text-white">{fmt(p.value as number)} DT</span>
          </div>
        ))}
      </div>
    </div>
  );
};

const ChartLegend: React.FC<DefaultLegendContentProps> = ({ payload }) => (
  <div className="mb-1 flex items-center justify-end gap-4">
    {payload?.map((entry) => (
      <div key={entry.value} className="flex items-center gap-1.5 text-theme-xs text-gray-500 dark:text-gray-400">
        <span className="h-2 w-2 rounded-full" style={{ backgroundColor: entry.color }} />
        {entry.value}
      </div>
    ))}
  </div>
);

const DashboardPage: React.FC = () => {
  const { theme } = useTheme();
  const c = PALETTE[theme];

  const [mois, setMois] = useState(currentMois());
  const [encaissements, setEncaissements] = useState<VueEncaissements | null>(null);
  const [decaissements, setDecaissements] = useState<VueDecaissements | null>(null);
  const [resultat, setResultat] = useState<ResultatNet | null>(null);
  const [tva, setTva] = useState<DeclarationTva | null>(null);
  const [trend, setTrend] = useState<TrendPoint[]>([]);
  const [loading, setLoading] = useState(true);
  const [erreur, setErreur] = useState<string | null>(null);

  const loadData = async () => {
    setLoading(true);
    setErreur(null);
    try {
      const moisPrecedents = [-5, -4, -3, -2, -1].map((delta) => moisOffset(mois, delta));
      const [enc, dec, res, tv, ...precedents] = await Promise.all([
        dashboardService.getEncaissements(mois),
        dashboardService.getDecaissements(mois),
        dashboardService.getResultatNet(mois),
        dashboardService.getTva(mois),
        ...moisPrecedents.map((m) => dashboardService.getResultatNet(m)),
      ]);
      setEncaissements(enc.data.data);
      setDecaissements(dec.data.data);
      setResultat(res.data.data);
      setTva(tv.data.data);

      const points = [...precedents.map((p) => p.data.data), res.data.data].map((r) => ({
        mois: r.mois,
        moisLabel: moisLabel(r.mois),
        revenus: r.totalRevenus,
        depenses: r.totalDepenses,
        marge: r.margePourcent,
      }));
      setTrend(points);
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

  const changeMois = (delta: number) => setMois(moisOffset(mois, delta));

  const beneficiaire = (resultat?.resultatNet ?? 0) >= 0;

  const deltas = useMemo(() => {
    if (trend.length < 2) return { revenus: null, depenses: null, marge: null };
    const cur = trend[trend.length - 1];
    const prev = trend[trend.length - 2];
    const pct = (a: number, b: number) => (b !== 0 ? ((a - b) / Math.abs(b)) * 100 : null);
    return {
      revenus: pct(cur.revenus, prev.revenus),
      depenses: pct(cur.depenses, prev.depenses),
      marge: cur.marge - prev.marge,
    };
  }, [trend]);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-title-sm font-extrabold tracking-tight text-transparent bg-clip-text bg-gradient-to-r from-gray-900 to-[#683b77] dark:from-white dark:to-[#ab78c3]">
            Tableau de bord
          </h1>
          <p className="text-theme-sm text-gray-500 dark:text-gray-400 mt-1">
            Encaissements, décaissements et résultat net du mois
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
        </div>
      </div>

      {erreur && <ErreurBanner message={erreur} onRetry={loadData} />}

      {loading ? (
        <div className="py-20 text-center text-gray-400">Chargement...</div>
      ) : (
        <>
          {/* Résultat net + KPIs */}
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
            {resultat && (
              <div className="lg:col-span-5 relative overflow-hidden rounded-2xl border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-dark">
                <div
                  className={`absolute -right-10 -top-10 h-40 w-40 rounded-full blur-[60px] ${
                    beneficiaire ? 'bg-success-500/10' : 'bg-error-500/10'
                  }`}
                />
                <div className="relative flex items-center justify-between flex-wrap gap-4">
                  <div>
                    <p className="text-theme-xs text-gray-400">Résultat net du mois</p>
                    <p className={`mt-1 text-3xl font-extrabold ${beneficiaire ? 'text-success-500' : 'text-error-500'}`}>
                      {fmt(resultat.resultatNet)} DT
                    </p>
                    <span
                      className={`mt-2 inline-flex items-center rounded-full px-2.5 py-0.5 text-theme-xs font-semibold ${
                        beneficiaire
                          ? 'bg-success-50 text-success-600 dark:bg-success-500/10 dark:text-success-400'
                          : 'bg-error-50 text-error-600 dark:bg-error-500/10 dark:text-error-400'
                      }`}
                    >
                      {beneficiaire ? 'Bénéficiaire' : 'Déficitaire'} · marge {resultat.margePourcent}%
                    </span>
                  </div>
                  <div
                    className={`flex h-14 w-14 items-center justify-center rounded-full ${
                      beneficiaire ? 'bg-success-50 text-success-500 dark:bg-success-500/10' : 'bg-error-50 text-error-500 dark:bg-error-500/10'
                    }`}
                  >
                    {beneficiaire ? <HiOutlineTrendingUp size={26} /> : <HiOutlineTrendingDown size={26} />}
                  </div>
                </div>
                <div className="relative mt-5 grid grid-cols-2 gap-4">
                  <div className="rounded-xl bg-gray-50 p-3 dark:bg-gray-700/40">
                    <p className="text-theme-xs text-gray-400">Revenus</p>
                    <p className="mt-0.5 font-bold text-gray-800 dark:text-white">{fmt(resultat.totalRevenus)} DT</p>
                  </div>
                  <div className="rounded-xl bg-gray-50 p-3 dark:bg-gray-700/40">
                    <p className="text-theme-xs text-gray-400">Dépenses</p>
                    <p className="mt-0.5 font-bold text-gray-800 dark:text-white">{fmt(resultat.totalDepenses)} DT</p>
                  </div>
                </div>
              </div>
            )}

            {resultat && encaissements && (
              <div className="lg:col-span-7 grid grid-cols-1 sm:grid-cols-2 gap-4">
                <StatTile
                  icon={<HiOutlineCash size={20} />}
                  label="Revenus encaissés"
                  value={`${fmt(resultat.totalRevenus)} DT`}
                  accentClass="bg-success-50 text-success-500 dark:bg-success-500/10 dark:text-success-400"
                  deltaPercent={deltas.revenus}
                />
                <StatTile
                  icon={<HiOutlineCreditCard size={20} />}
                  label="Dépenses totales"
                  value={`${fmt(resultat.totalDepenses)} DT`}
                  accentClass="bg-blue-50 text-blue-500 dark:bg-blue-500/10 dark:text-blue-400"
                  deltaPercent={deltas.depenses}
                  invert
                />
                <StatTile
                  icon={<HiOutlineClock size={20} />}
                  label="Reste à encaisser"
                  value={`${fmt(encaissements.totalRemaining)} DT`}
                  accentClass="bg-warning-50 text-warning-500 dark:bg-warning-500/10 dark:text-warning-400"
                />
                <MeterTile percent={resultat.margePourcent} deltaPoints={deltas.marge} />
              </div>
            )}
          </div>

          {/* Tendance 6 mois */}
          {trend.length > 0 && (
            <div className="rounded-2xl border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-dark">
              <div className="mb-2">
                <h2 className="text-theme-md font-bold text-gray-800 dark:text-white">Revenus vs Dépenses</h2>
                <p className="text-theme-xs text-gray-400">Évolution sur les 6 derniers mois</p>
              </div>
              <ResponsiveContainer width="100%" height={280}>
                <LineChart data={trend} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                  <CartesianGrid stroke={c.grid} vertical={false} />
                  <XAxis
                    dataKey="moisLabel"
                    tick={{ fill: c.axis, fontSize: 12 }}
                    axisLine={{ stroke: c.grid }}
                    tickLine={false}
                  />
                  <YAxis
                    tick={{ fill: c.axis, fontSize: 12 }}
                    axisLine={false}
                    tickLine={false}
                    width={48}
                    tickFormatter={axisFmt}
                  />
                  <Tooltip content={(props) => <ChartTooltip {...props} />} cursor={{ stroke: c.grid, strokeWidth: 1 }} />
                  <Legend content={(props) => <ChartLegend {...props} />} verticalAlign="top" />
                  <Line
                    type="monotone"
                    dataKey="revenus"
                    name="Revenus"
                    stroke={c.purple}
                    strokeWidth={2}
                    dot={{ r: 4, fill: c.purple, stroke: c.surface, strokeWidth: 2 }}
                    activeDot={{ r: 5, stroke: c.surface, strokeWidth: 2 }}
                  />
                  <Line
                    type="monotone"
                    dataKey="depenses"
                    name="Dépenses"
                    stroke={c.blue}
                    strokeWidth={2}
                    dot={{ r: 4, fill: c.blue, stroke: c.surface, strokeWidth: 2 }}
                    activeDot={{ r: 5, stroke: c.surface, strokeWidth: 2 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            {/* Encaissements */}
            {encaissements && (
              <div className="rounded-2xl border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-dark">
                <h2 className="mb-1 text-theme-md font-bold text-gray-800 dark:text-white">Encaissements</h2>
                <p className="mb-4 text-theme-xs text-gray-400">Composition des revenus encaissés</p>
                <CompositionBar
                  total={encaissements.grandTotal}
                  segments={[
                    {
                      label: 'Factures encaissées',
                      value: encaissements.totalEncaisse,
                      barClass: 'bg-success-500',
                      dotClass: 'bg-success-500',
                    },
                    {
                      label: 'Autres revenus',
                      value: encaissements.totalAutresRevenus,
                      barClass: 'bg-blue-500',
                      dotClass: 'bg-blue-500',
                    },
                  ]}
                />
                <div className="mt-5 text-theme-sm">
                  <Ligne label="Total facturé" value={encaissements.totalFacture} />
                  <Ligne label="Encaissé" value={encaissements.totalEncaisse} accent="success" />
                  <Ligne label="En attente" value={encaissements.totalPending} accent="error" />
                  <Ligne label="Reste à encaisser" value={encaissements.totalRemaining} accent="error" />
                  <Ligne label="Autres revenus" value={encaissements.totalAutresRevenus} />
                  <Ligne label="Total encaissé" value={encaissements.grandTotal} bold />
                </div>
              </div>
            )}

            {/* Décaissements */}
            {decaissements && (
              <div className="rounded-2xl border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-dark">
                <h2 className="mb-1 text-theme-md font-bold text-gray-800 dark:text-white">Décaissements</h2>
                <p className="mb-4 text-theme-xs text-gray-400">Composition des dépenses du mois</p>
                <CompositionBar
                  total={decaissements.totalDecaissements}
                  segments={[
                    {
                      label: 'Masse salariale',
                      value: decaissements.coutTotalSalaires,
                      barClass: 'bg-brand-500 dark:bg-brand-300',
                      dotClass: 'bg-brand-500 dark:bg-brand-300',
                    },
                    {
                      label: 'Charges fixes',
                      value: decaissements.chargesFixesDues,
                      barClass: 'bg-blue-500',
                      dotClass: 'bg-blue-500',
                    },
                    {
                      label: 'Charges variables',
                      value: decaissements.chargesVariables,
                      barClass: 'bg-orange-500 dark:bg-orange-600',
                      dotClass: 'bg-orange-500 dark:bg-orange-600',
                    },
                  ]}
                />

                <div className="mt-5 grid grid-cols-1 sm:grid-cols-2 gap-x-6 text-theme-sm">
                  <div>
                    <p className="mb-1 text-theme-xs font-semibold uppercase tracking-wide text-gray-400">Masse salariale</p>
                    <Ligne label="Masse brute" value={decaissements.masseBrute} />
                    <Ligne label="Masse nette" value={decaissements.masseNette} />
                    <Ligne label="Charges patronales" value={decaissements.chargesPatronales} />
                    <Ligne label="Coût total salaires" value={decaissements.coutTotalSalaires} />
                    <Ligne label="Net restant à payer" value={decaissements.netRestantAPayer} accent="error" />
                    <Ligne label="Net reporté (mois antérieurs)" value={decaissements.netReporte} accent="error" />
                  </div>
                  <div>
                    <p className="mb-1 text-theme-xs font-semibold uppercase tracking-wide text-gray-400">Charges</p>
                    <Ligne label="Fixes dues" value={decaissements.chargesFixesDues} />
                    <Ligne label="Fixes payées" value={decaissements.chargesFixesPayees} accent="success" />
                    <Ligne label="Fixes restantes" value={decaissements.chargesFixesRestantes} accent="error" />
                    <Ligne label="Variables" value={decaissements.chargesVariables} />

                    <p className="mb-1 mt-3 text-theme-xs font-semibold uppercase tracking-wide text-gray-400">Taxes & dettes</p>
                    <Ligne label="IRPP" value={decaissements.irpp} />
                    <Ligne label="TFP" value={decaissements.tfp} />
                    <Ligne label="FOPROLOS" value={decaissements.foprolos} />
                    <Ligne label="Total taxes dues" value={decaissements.totalTaxesDues} />
                    <Ligne label="Dettes — solde restant" value={decaissements.dettesSoldeRestant} accent="error" />
                  </div>
                </div>
                <div className="mt-3 text-theme-sm">
                  <Ligne label="Total décaissements" value={decaissements.totalDecaissements} bold />
                </div>
              </div>
            )}
          </div>

          {/* TVA */}
          {tva && (
            <div className="rounded-2xl border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-dark">
              <div className="mb-3 flex items-center justify-between flex-wrap gap-3">
                <h2 className="text-theme-md font-bold text-gray-800 dark:text-white">
                  Déclaration TVA — {tva.mois}
                </h2>
                <span
                  className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-theme-xs font-semibold ${
                    tva.aReverser
                      ? 'bg-error-50 text-error-600 dark:bg-error-500/10 dark:text-error-400'
                      : 'bg-success-50 text-success-600 dark:bg-success-500/10 dark:text-success-400'
                  }`}
                >
                  {tva.aReverser ? 'À reverser' : 'Crédit'} · {fmt(Math.abs(tva.tvaNette))} DT
                </span>
              </div>
              <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
                <div className="text-theme-sm">
                  <Ligne label="TVA collectée sur factures" value={tva.tvaCollecteeFactures} />
                  <Ligne label="TVA sur autres revenus" value={tva.tvaCollecteeAutresRevenus} />
                  <Ligne label="Total collectée" value={tva.tvaCollectee} bold />
                </div>
                <div className="text-theme-sm">
                  <Ligne label="TVA déductible (charges)" value={tva.tvaDeductible} />
                  <Ligne
                    label={tva.aReverser ? 'TVA à reverser à l’État' : 'Crédit de TVA en votre faveur'}
                    value={Math.abs(tva.tvaNette)}
                    accent={tva.aReverser ? 'error' : 'success'}
                    bold
                  />
                </div>
              </div>
              <p className="mt-3 text-theme-xs text-gray-400">
                La TVA collectée sur factures est calculée au prorata du montant réellement encaissé.
              </p>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default DashboardPage;
