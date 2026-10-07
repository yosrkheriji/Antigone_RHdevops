import React, { useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import {
  HiOutlineCalendar,
  HiOutlineChevronLeft,
  HiOutlineChevronRight,
  HiOutlineLightningBolt,
  HiOutlineSearch,
} from 'react-icons/hi';
import { useNavigate } from 'react-router-dom';
import { demandeService } from '../api/demandeService';
import { demandePapierService } from '../api/demandePapierService';
import { notificationService } from '../api/notificationService';
import { tacheService } from '../api/tacheService';
import { calendrierService } from '../api/calendrierService';
import { documentEmployeService } from '../api/documentEmployeService';
import { employeService } from '../api/employeService';
import { validationService } from '../api/validationService';
import { useAuth } from '../context/AuthContext';
import { GradientRhCard } from '../components/ui/GradientRhCard';
import { shouldDisplayNotification } from '../utils/notificationRules';
import {
  CalendrierJour,
  DecisionValidation,
  DemandeResponse,
  DocumentEmployeDTO,
  NotificationResponse,
  SoldeCongeInfo,
  StatutDemande,
  StatutTache,
  TacheDetail,
  Validation,
} from '../types';

type HeroStat = {
  value: string;
  label: string;
};

type FocusItem = {
  title: string;
  subtitle: string;
  dueText: string;
  badge: string;
  emoji: string;
  path: string;
  urgent?: boolean;
};

type RhCard = {
  id: string;
  title: string;
  status: string;
  emoji: string;
  meta: string;
  badges: string[];
  path: string;
  gradient: string;
};

const cardGradients = [
  'linear-gradient(135deg, #E86A2E, #F5A87A)',
  'linear-gradient(135deg, #7c3aed, #a78bfa)',
  'linear-gradient(135deg, #059669, #34d399)',
  'linear-gradient(135deg, #2563eb, #60a5fa)',
  'linear-gradient(135deg, #f59e0b, #fbbf24)',
  'linear-gradient(135deg, #0f9f77, #46d7af)',
];

const formatHeroDate = (date: Date): string => {
  const parts = new Intl.DateTimeFormat('fr-FR', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  }).format(date);

  return parts.charAt(0).toUpperCase() + parts.slice(1);
};

const formatShortDate = (value?: string | null): string => {
  if (!value) return 'Date inconnue';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Date inconnue';
  return date.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
};

const formatDemandeStatus = (statut: StatutDemande): string => {
  if (statut === StatutDemande.APPROUVEE) return 'Approuvee';
  if (statut === StatutDemande.REFUSEE) return 'Refusee';
  if (statut === StatutDemande.ANNULEE) return 'Annulee';
  return 'En attente';
};

const toDemandeEmoji = (type?: string | null): string => {
  const normalized = (type || '').toUpperCase();
  if (normalized.includes('CONGE')) return '🏖️';
  if (normalized.includes('AUTORISATION')) return '📝';
  if (normalized.includes('TELETRAVAIL')) return '💻';
  if (normalized.includes('ADMIN')) return '📂';
  return '📄';
};

const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const employeId = user?.employeId;

  const [demandes, setDemandes] = useState<DemandeResponse[]>([]);
  const [demandesPapier, setDemandesPapier] = useState<DemandeResponse[]>([]);
  const [taches, setTaches] = useState<TacheDetail[]>([]);
  const [notifications, setNotifications] = useState<NotificationResponse[]>([]);
  const [documents, setDocuments] = useState<DocumentEmployeDTO[]>([]);
  const [validations, setValidations] = useState<Validation[]>([]);
  const [joursFeries, setJoursFeries] = useState<CalendrierJour[]>([]);
  const [soldeInfo, setSoldeInfo] = useState<SoldeCongeInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [calMonth, setCalMonth] = useState<Date>(new Date());

  const displayName = user?.prenom?.trim() || 'Utilisateur';
  const heroDateText = useMemo(() => `${formatHeroDate(new Date())} · Espace RH`, []);

  const fetchData = async () => {
    if (!employeId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    try {
      const [
        demandesRes,
        demandesPapierRes,
        tachesRes,
        notificationsRes,
        documentsRes,
        validationsRes,
        feriesRes,
        soldeRes,
      ] = await Promise.all([
        demandeService.getByEmploye(employeId).catch(() => ({ data: { data: [] } })),
        demandePapierService.getAll().catch(() => ({ data: { data: [] } })),
        tacheService.getByAssignee(employeId).catch(() => ({ data: { data: [] } })),
        notificationService.getByEmploye(employeId).catch(() => ({ data: { data: [] } })),
        documentEmployeService.getByEmploye(employeId).catch(() => ({ data: { data: [] } })),
        validationService.getPendingByValidateur(employeId).catch(() => ({ data: { data: [] } })),
        calendrierService.getFeries(new Date().getFullYear()).catch(() => ({ data: { data: [] } })),
        employeService.getSoldeInfo(employeId).catch(() => ({ data: { data: null } })),
      ]);

      const allPapier = demandesPapierRes.data.data || [];
      setDemandes(demandesRes.data.data || []);
      setDemandesPapier(allPapier.filter((d: DemandeResponse) => d.employeId === employeId));
      setTaches(tachesRes.data.data || []);
      const rawNotifications = notificationsRes.data.data || [];
      setNotifications(rawNotifications.filter(shouldDisplayNotification));
      setDocuments(documentsRes.data.data || []);
      setValidations(validationsRes.data.data || []);
      setJoursFeries(feriesRes.data.data || []);
      setSoldeInfo(soldeRes.data.data || null);
    } catch (error) {
      console.error('Erreur chargement dashboard RH:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void fetchData();
  }, [employeId]);

  const now = new Date();
  const dans30j = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000);

  const demandesEnAttente = demandes.filter((d) => d.statut === StatutDemande.EN_ATTENTE).length;
  const notifsNonLues = notifications.filter((n) => !n.lu).length;

  const tachesOuvertes = taches.filter((t) => t.statut !== StatutTache.DONE);
  const docsExpirant = documents.filter(
    (d) => d.dateExpiration && new Date(d.dateExpiration) >= now && new Date(d.dateExpiration) <= dans30j,
  );

  const prochainsFeries = [...joursFeries]
    .filter((j) => new Date(j.dateJour) >= now)
    .sort((a, b) => new Date(a.dateJour).getTime() - new Date(b.dateJour).getTime())
    .slice(0, 4);

  const heroStats = useMemo<HeroStat[]>(
    () => [
      { value: String(demandesEnAttente), label: 'Demandes en attente' },
      { value: String(notifsNonLues), label: 'Notifications non lues' },
      { value: `${soldeInfo?.soldeDisponible?.toFixed(1) ?? '0'}j`, label: 'Solde conge disponible' },
    ],
    [demandesEnAttente, notifsNonLues, soldeInfo],
  );

  const focusItem = useMemo<FocusItem | null>(() => {
    const topTask = [...tachesOuvertes].sort((a, b) => {
      const urgentWeight = Number(Boolean(b.urgente)) - Number(Boolean(a.urgente));
      if (urgentWeight !== 0) return urgentWeight;
      const aDate = new Date(a.dateEcheance || '2999-12-31').getTime();
      const bDate = new Date(b.dateEcheance || '2999-12-31').getTime();
      return aDate - bDate;
    })[0];

    if (topTask) {
      return {
        title: topTask.titre,
        subtitle: `Priorite du jour · ${topTask.projetNom || 'Tache personnelle'}`,
        dueText: formatShortDate(topTask.dateEcheance),
        badge: topTask.urgente ? 'Urgent' : 'A planifier',
        emoji: topTask.urgente ? '⚡' : '🎯',
        path: '/mon-calendrier',
        urgent: Boolean(topTask.urgente),
      };
    }

    const pendingDemande = [...demandes]
      .filter((d) => d.statut === StatutDemande.EN_ATTENTE)
      .sort((a, b) => new Date(a.dateCreation).getTime() - new Date(b.dateCreation).getTime())[0];

    if (pendingDemande) {
      return {
        title: pendingDemande.typeCongeLabel || pendingDemande.type || 'Demande RH',
        subtitle: 'Action RH en attente de traitement',
        dueText: formatShortDate(pendingDemande.dateCreation),
        badge: 'En attente',
        emoji: toDemandeEmoji(pendingDemande.type),
        path: '/mes-demandes',
      };
    }

    const expiringDoc = [...docsExpirant].sort(
      (a, b) => new Date(a.dateExpiration || '2999-12-31').getTime() - new Date(b.dateExpiration || '2999-12-31').getTime(),
    )[0];

    if (expiringDoc) {
      return {
        title: expiringDoc.nom,
        subtitle: 'Document RH a renouveler prochainement',
        dueText: formatShortDate(expiringDoc.dateExpiration),
        badge: 'Expire bientot',
        emoji: '📄',
        path: '/mon-profil',
      };
    }

    return null;
  }, [demandes, docsExpirant, tachesOuvertes]);

  const recentRhCards = useMemo<RhCard[]>(() => {
    const demandeCards = [...demandes]
      .sort((a, b) => new Date(b.dateCreation).getTime() - new Date(a.dateCreation).getTime())
      .slice(0, 4)
      .map((demande, index) => ({
        id: `demande-${demande.id}`,
        title: demande.typeCongeLabel || demande.type || 'Demande RH',
        status: formatDemandeStatus(demande.statut),
        emoji: toDemandeEmoji(demande.type),
        meta: `Soumise le ${formatShortDate(demande.dateCreation)}`,
        badges: [demande.joursOuvrables ? `${demande.joursOuvrables}j` : 'RH', 'Workflow'],
        path: '/mes-demandes',
        gradient: cardGradients[index % cardGradients.length],
      }));

    const docCards = [...documents]
      .sort((a, b) => new Date(b.dateExpiration || 0).getTime() - new Date(a.dateExpiration || 0).getTime())
      .slice(0, 2)
      .map((doc, index) => {
        const isExpired = doc.dateExpiration && new Date(doc.dateExpiration) < now;
        const isExpiring = doc.dateExpiration && new Date(doc.dateExpiration) <= dans30j;
        const status = isExpired ? 'Expire' : isExpiring ? 'Expire bientot' : 'A jour';

        return {
          id: `doc-${doc.id}`,
          title: doc.nom,
          status,
          emoji: '📄',
          meta: `${doc.type || 'Document'} · ${formatShortDate(doc.dateExpiration)}`,
          badges: ['Document', isExpired ? 'A renouveler' : 'Suivi'],
          path: '/mon-profil',
          gradient: cardGradients[(index + demandeCards.length) % cardGradients.length],
        };
      });

    const ferieCards = prochainsFeries.slice(0, 1).map((jour, index) => ({
      id: `ferie-${jour.id}`,
      title: jour.nomJour,
      status: 'Jour ferie',
      emoji: '📅',
      meta: `Le ${formatShortDate(jour.dateJour)}`,
      badges: ['Calendrier', 'Entreprise'],
      path: '/mon-calendrier',
      gradient: cardGradients[(index + demandeCards.length + docCards.length) % cardGradients.length],
    }));

    return [...demandeCards, ...docCards, ...ferieCards].slice(0, 6);
  }, [demandes, documents, now, dans30j, prochainsFeries]);

  const calendarData = useMemo(() => {
    const congeDays = new Set<string>();
    demandes
      .filter((d) => d.statut === StatutDemande.APPROUVEE && d.dateDebut)
      .forEach((d) => {
        const start = new Date(d.dateDebut! + 'T00:00:00');
        const end = d.dateFin ? new Date(d.dateFin + 'T00:00:00') : new Date(start);
        for (const dt = new Date(start); dt <= end; dt.setDate(dt.getDate() + 1)) {
          congeDays.add(`${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, '0')}-${String(dt.getDate()).padStart(2, '0')}`);
        }
      });
    const feriesDays = new Set<string>(joursFeries.map((j) => j.dateJour.slice(0, 10)));
    const feriesLabels = new Map<string, string>(joursFeries.map((j) => [j.dateJour.slice(0, 10), j.nomJour]));
    return { congeDays, feriesDays, feriesLabels };
  }, [demandes, joursFeries]);

  if (loading) {
    return (
      <div className="flex h-96 items-center justify-center">
        <div className="flex flex-col items-center gap-3">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-brand-200 border-t-brand-500" />
          <p className="text-gray-500 dark:text-gray-400">Chargement de votre dashboard RH...</p>
        </div>
      </div>
    );
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', gap: '28px', paddingBottom: '48px' }}>
      <motion.section
        initial={{ opacity: 0, y: 22 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.52, ease: 'easeOut' }}
        aria-label="Banniere d'accueil RH"
        className="dashboard-hero-card"
        style={{ position: 'relative', overflow: 'hidden', margin: '24px 24px 0', borderRadius: '24px', padding: 'clamp(36px, 6vw, 56px) clamp(28px, 5vw, 56px)' }}
      >
        {/* Glow spot haut-droite */}
        <div style={{ position: 'absolute', right: '-80px', top: '-80px', width: '400px', height: '400px', borderRadius: '50%', background: 'rgba(104,59,119,0.12)', filter: 'blur(80px)', pointerEvents: 'none' }} />
        {/* Glow spot bas-gauche */}
        <div style={{ position: 'absolute', left: '-60px', bottom: '-60px', width: '300px', height: '300px', borderRadius: '50%', background: 'rgba(104,59,119,0.07)', filter: 'blur(70px)', pointerEvents: 'none' }} />

        <div style={{ position: 'relative', zIndex: 1, display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) auto', gap: '32px', alignItems: 'end' }}>
          <div>
            <p style={{ margin: '0 0 14px', color: '#683b77', fontSize: '13px', fontWeight: 600, letterSpacing: '0.1em', textTransform: 'uppercase', display: 'flex', alignItems: 'center', gap: '6px' }}>
              <HiOutlineLightningBolt size={15} />
              {heroDateText}
            </p>
            <h1 className="dashboard-hero-title" style={{ margin: 0, fontSize: 'clamp(26px, 4vw, 44px)', fontWeight: 800, letterSpacing: '-0.025em', lineHeight: 1.12 }}>
              Bonjour,{' '}
              <span style={{ background: 'linear-gradient(90deg, #683b77, #ab78c3)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', backgroundClip: 'text' }}>{displayName}</span>
              {' '}👋
            </h1>
            <p className="dashboard-hero-subtitle" style={{ margin: '14px 0 0', fontSize: '15px', lineHeight: 1.65 }}>
              Bienvenue sur votre espace RH. Suivez vos demandes, documents et actions prioritaires d'un seul coup d'oeil.
            </p>

            <div style={{ position: 'relative', marginTop: '28px', maxWidth: '440px' }}>
              <HiOutlineSearch style={{ position: 'absolute', left: '18px', top: '50%', transform: 'translateY(-50%)', color: '#9ca3af', pointerEvents: 'none', zIndex: 1 }} size={18} />
              <input
                type="text"
                placeholder="Rechercher une demande, un document..."
                className="dashboard-hero-search"
                style={{ width: '100%', height: '50px', borderRadius: '9999px', fontSize: '14px', padding: '0 24px 0 48px', outline: 'none', boxSizing: 'border-box', fontFamily: 'inherit', transition: 'all 0.2s' }}
                aria-label="Recherche principale RH"
                onFocus={e => { e.target.style.boxShadow = '0 0 0 3px rgba(104,59,119,0.25)'; }}
                onBlur={e => { e.target.style.boxShadow = 'none'; }}
              />
            </div>
          </div>

          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '12px', alignSelf: 'end' }}>
            {heroStats.map((stat, index) => (
              <motion.div
                key={stat.label}
                initial={{ opacity: 0, y: 16 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.45, delay: 0.15 + index * 0.08 }}
                className="dashboard-hero-stat"
                style={{ width: '150px', borderRadius: '16px', padding: '18px 22px' }}
              >
                <strong className="dashboard-hero-stat-value" style={{ display: 'block', fontSize: '26px', fontWeight: 800, lineHeight: 1 }}>{stat.value}</strong>
                <span className="dashboard-hero-stat-label" style={{ display: 'block', marginTop: '7px', fontSize: '11px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em', wordBreak: 'break-word' }}>{stat.label}</span>
              </motion.div>
            ))}
          </div>
        </div>
      </motion.section>

      <section className="animate-fade-in-up space-y-5" style={{ animationDelay: '0.15s', padding: '0 24px' }}>
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-bold tracking-tight text-gray-800 dark:text-white">Focus du jour</h2>
        </div>

        {focusItem ? (
          <div
            onClick={() => navigate(focusItem.path)}
            className="group relative cursor-pointer overflow-hidden rounded-3xl border-2 border-brand-500/20 bg-white p-6 transition-all duration-300 hover:border-brand-500/50 hover:shadow-2xl hover:shadow-brand-500/10 dark:border-brand-400/20 dark:bg-gray-800 lg:p-8"
          >
            <div className="pointer-events-none absolute right-0 top-0 -mr-10 -mt-10 h-40 w-40 rounded-full bg-brand-500/10 blur-[40px] transition-colors duration-500 group-hover:bg-brand-500/20" />
            <div className="pointer-events-none absolute bottom-0 left-0 -mb-10 -ml-10 h-40 w-40 rounded-full bg-indigo-500/10 blur-[40px] transition-colors duration-500 group-hover:bg-indigo-500/20" />

            <div className="relative z-10 flex flex-col justify-between gap-6 md:flex-row md:items-center">
              <div className="flex items-start gap-5 md:items-center">
                <div className="flex h-14 w-14 flex-shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-400 to-brand-600 text-2xl text-white shadow-lg">
                  {focusItem.emoji}
                </div>
                <div>
                  <div className="mb-1 flex items-center gap-3">
                    <h3 className="text-xl font-bold text-gray-900 transition-colors group-hover:text-brand-500 dark:text-white">
                      {focusItem.title}
                    </h3>
                    {focusItem.urgent && (
                      <span className="rounded-full bg-red-100 px-2.5 py-0.5 text-[10px] font-extrabold uppercase tracking-widest text-red-600 dark:bg-red-500/10 dark:text-red-400">
                        Urgent
                      </span>
                    )}
                  </div>
                  <p className="text-sm font-medium text-gray-500 dark:text-gray-400">{focusItem.subtitle}</p>
                </div>
              </div>

              <div className="flex flex-col gap-2 md:items-end md:gap-1">
                <p className="text-xs font-semibold uppercase tracking-widest text-gray-400 dark:text-gray-500">Echeance</p>
                <div className="flex items-center gap-2 font-bold text-gray-900 dark:text-white">
                  <HiOutlineCalendar size={18} className="text-brand-500" />
                  {focusItem.dueText}
                </div>
                <span className="rounded-full bg-gray-100 px-2 py-0.5 text-[11px] font-medium text-gray-600 dark:bg-gray-700 dark:text-gray-300">
                  {focusItem.badge}
                </span>
              </div>
            </div>
          </div>
        ) : (
          <div className="flex items-center gap-5 rounded-3xl border border-emerald-100 bg-gradient-to-r from-emerald-50 to-teal-50 p-6 dark:border-emerald-500/20 dark:from-emerald-900/10 dark:to-teal-900/10">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-emerald-100 text-2xl text-emerald-600 dark:bg-emerald-500/20 dark:text-emerald-400">
              🎉
            </div>
            <div>
              <h3 className="text-lg font-bold text-emerald-800 dark:text-emerald-300">Tout est sous controle</h3>
              <p className="text-sm text-emerald-600/90 dark:text-emerald-400/80">
                Aucune priorite critique detectee. Votre dashboard RH est a jour.
              </p>
            </div>
          </div>
        )}
      </section>

      <div className="grid grid-cols-1 gap-8 xl:grid-cols-3" style={{ padding: '0 24px' }}>
        <section className="animate-fade-in-up space-y-5 xl:col-span-2" style={{ animationDelay: '0.2s' }}>
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-bold tracking-tight text-gray-800 dark:text-white">Mes dossiers RH recents</h2>
            <button
              type="button"
              className="group flex items-center gap-1 text-sm font-bold text-brand-500 transition-colors hover:text-brand-600 dark:text-brand-400"
              onClick={() => navigate('/mes-demandes')}
            >
              Voir tout <span className="transform transition-transform group-hover:translate-x-1">→</span>
            </button>
          </div>

          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
            {!loading && recentRhCards.length === 0 ? (
              <div className="col-span-full flex flex-col items-center justify-center rounded-3xl p-12 text-center"
                style={{ background: '#0e131f', border: '1px solid rgba(171,120,195,0.15)' }}>
                <h3 className="mb-2 text-lg font-bold text-white">Aucun element recent</h3>
                <p className="max-w-xs text-sm" style={{ color: 'rgba(255,255,255,0.45)' }}>
                  Vos demandes, documents et jalons RH s'afficheront ici des qu'ils seront disponibles.
                </p>
              </div>
            ) : (
              recentRhCards.map((card, index) => (
                <GradientRhCard
                  key={card.id}
                  title={card.title}
                  status={card.status}
                  meta={card.meta}
                  badges={card.badges}
                  onClick={() => navigate(card.path)}
                  variant={
                    card.id.startsWith('ferie-')
                      ? 'ferie'
                      : card.id.startsWith('doc-')
                      ? 'document'
                      : 'default'
                  }
                />
              ))
            )}
          </div>
        </section>

        <section className="animate-fade-in-up space-y-5" style={{ animationDelay: '0.3s' }}>
          <h2 className="text-xl font-bold tracking-tight text-gray-800 dark:text-white">Mon calendrier RH</h2>

          <div className="relative overflow-hidden rounded-3xl border border-white/30 bg-white/70 p-6 shadow-xl backdrop-blur-xl dark:border-white/10 dark:bg-gray-800/60">
            {/* Blobs décoratifs */}
            <div className="pointer-events-none absolute -right-10 -top-10 h-40 w-40 rounded-full bg-brand-400/20 blur-[60px]" />
            <div className="pointer-events-none absolute -bottom-10 -left-10 h-40 w-40 rounded-full bg-indigo-400/15 blur-[60px]" />

            {/* Navigation mois */}
            <div className="relative z-10 mb-5 flex items-center justify-between">
              <button
                onClick={() => setCalMonth((m) => new Date(m.getFullYear(), m.getMonth() - 1, 1))}
                className="flex h-9 w-9 items-center justify-center rounded-full bg-white/50 text-gray-600 transition hover:bg-brand-50 hover:text-brand-500 dark:bg-white/10 dark:text-gray-300 dark:hover:bg-brand-500/20 dark:hover:text-brand-300"
                aria-label="Mois précédent"
              >
                <HiOutlineChevronLeft size={18} />
              </button>
              <span className="text-base font-bold capitalize text-gray-800 dark:text-white">
                {calMonth.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' })}
              </span>
              <button
                onClick={() => setCalMonth((m) => new Date(m.getFullYear(), m.getMonth() + 1, 1))}
                className="flex h-9 w-9 items-center justify-center rounded-full bg-white/50 text-gray-600 transition hover:bg-brand-50 hover:text-brand-500 dark:bg-white/10 dark:text-gray-300 dark:hover:bg-brand-500/20 dark:hover:text-brand-300"
                aria-label="Mois suivant"
              >
                <HiOutlineChevronRight size={18} />
              </button>
            </div>

            {/* En-têtes jours */}
            <div className="relative z-10 mb-2 grid grid-cols-7 text-center">
              {['L', 'M', 'M', 'J', 'V', 'S', 'D'].map((d, i) => (
                <div
                  key={i}
                  className={`pb-2 text-[11px] font-bold uppercase tracking-wider ${
                    i >= 5 ? 'text-gray-300 dark:text-gray-600' : 'text-gray-400 dark:text-gray-500'
                  }`}
                >
                  {d}
                </div>
              ))}
            </div>

            {/* Grille des jours */}
            <div className="relative z-10 grid grid-cols-7 gap-1">
              {(() => {
                const year = calMonth.getFullYear();
                const month = calMonth.getMonth();
                const firstDay = new Date(year, month, 1);
                let startOffset = firstDay.getDay() - 1;
                if (startOffset < 0) startOffset = 6;
                const daysInMonth = new Date(year, month + 1, 0).getDate();
                const todayStr = new Date().toISOString().slice(0, 10);
                const cells: React.ReactNode[] = [];

                for (let i = 0; i < startOffset; i++) {
                  cells.push(<div key={`empty-${i}`} />);
                }

                for (let day = 1; day <= daysInMonth; day++) {
                  const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
                  const isToday = dateStr === todayStr;
                  const isFerie = calendarData.feriesDays.has(dateStr);
                  const isConge = calendarData.congeDays.has(dateStr);
                  const dayOfWeek = new Date(year, month, day).getDay();
                  const isWeekend = dayOfWeek === 0 || dayOfWeek === 6;

                  let cellClass = 'flex h-9 w-full items-center justify-center rounded-xl text-sm font-medium transition-all duration-150 ';
                  if (isToday && isConge) {
                    cellClass += 'bg-brand-500 text-white shadow-lg ring-2 ring-brand-300 dark:ring-brand-400';
                  } else if (isToday && isFerie) {
                    cellClass += 'bg-amber-500 text-white shadow-lg ring-2 ring-amber-300';
                  } else if (isToday) {
                    cellClass += 'bg-brand-500 text-white shadow-lg';
                  } else if (isConge) {
                    cellClass += 'bg-brand-100/80 text-brand-700 dark:bg-brand-500/20 dark:text-brand-300 font-bold';
                  } else if (isFerie) {
                    cellClass += 'bg-amber-100/80 text-amber-700 dark:bg-amber-500/20 dark:text-amber-300 font-bold';
                  } else if (isWeekend) {
                    cellClass += 'text-gray-300 dark:text-gray-600';
                  } else {
                    cellClass += 'text-gray-700 hover:bg-white/60 dark:text-gray-300 dark:hover:bg-white/10';
                  }

                  cells.push(
                    <div
                      key={dateStr}
                      className={cellClass}
                      title={
                        isFerie
                          ? calendarData.feriesLabels.get(dateStr)
                          : isConge
                            ? 'Congé approuvé'
                            : undefined
                      }
                    >
                      {day}
                    </div>,
                  );
                }
                return cells;
              })()}
            </div>

            {/* Légende */}
            <div className="relative z-10 mt-5 flex flex-wrap items-center gap-4 border-t border-gray-100/60 pt-4 dark:border-white/10">
              <div className="flex items-center gap-1.5">
                <div className="h-3 w-3 rounded-full bg-brand-500" />
                <span className="text-xs text-gray-500 dark:text-gray-400">Aujourd'hui</span>
              </div>
              <div className="flex items-center gap-1.5">
                <div className="h-3 w-3 rounded-full bg-brand-300" />
                <span className="text-xs text-gray-500 dark:text-gray-400">Congé approuvé</span>
              </div>
              <div className="flex items-center gap-1.5">
                <div className="h-3 w-3 rounded-full bg-amber-400" />
                <span className="text-xs text-gray-500 dark:text-gray-400">Jour férié</span>
              </div>
            </div>
          </div>
        </section>
      </div>

      <style>{`
        /* ── Dashboard hero card ── */
        .dashboard-hero-card {
          background: linear-gradient(135deg, #ffffff 0%, rgba(250,245,255,0.5) 50%, rgba(237,225,255,0.65) 100%);
          box-shadow: 0 8px 32px rgba(0,0,0,0.07), 0 2px 8px rgba(0,0,0,0.04);
        }
        .dark .dashboard-hero-card {
          background: linear-gradient(135deg, #030712 0%, #111827 55%, #0d1117 100%) !important;
          box-shadow: 0 20px 60px rgba(0,0,0,0.45) !important;
        }

        /* ── Hero text ── */
        .dashboard-hero-title { color: #1a1814; }
        .dark .dashboard-hero-title { color: #f9fafb; }
        .dashboard-hero-subtitle { color: #6b7280; }
        .dark .dashboard-hero-subtitle { color: #9ca3af; }

        /* ── Hero search ── */
        .dashboard-hero-search {
          background: rgba(255,255,255,0.72);
          border: 1px solid #e5e7eb;
          color: #1a1814;
          backdrop-filter: blur(8px);
        }
        .dashboard-hero-search::placeholder { color: #9ca3af; }
        .dark .dashboard-hero-search {
          background: rgba(255,255,255,0.06) !important;
          border-color: rgba(255,255,255,0.12) !important;
          color: #f9fafb !important;
        }
        .dark .dashboard-hero-search::placeholder { color: #6b7280; }

        /* ── Hero stat cards ── */
        .dashboard-hero-stat {
          background: rgba(255,255,255,0.72);
          border: 1px solid #e5e7eb;
        }
        .dashboard-hero-stat:hover { background: rgba(255,255,255,0.92); }
        .dark .dashboard-hero-stat {
          background: rgba(255,255,255,0.05) !important;
          border-color: rgba(255,255,255,0.1) !important;
        }
        .dark .dashboard-hero-stat:hover { background: rgba(255,255,255,0.09) !important; }

        .dashboard-hero-stat-value { color: #111827; }
        .dark .dashboard-hero-stat-value { color: #f9fafb; }
        .dashboard-hero-stat-label { color: #6b7280; }
        .dark .dashboard-hero-stat-label { color: #9ca3af; }

        /* ── Animations ── */
        @keyframes fadeUp {
          from { opacity: 0; transform: translateY(15px); }
          to { opacity: 1; transform: translateY(0); }
        }
        .animate-fade-in-up {
          opacity: 0;
          animation: fadeUp 0.6s ease-out forwards;
        }
        @media (max-width: 700px) {
          .dashboard-hero-card > div > div:first-child { grid-template-columns: 1fr !important; }
        }
      `}</style>
    </div>
  );
};

export default DashboardPage;
