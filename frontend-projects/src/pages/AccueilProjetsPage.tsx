import React, { useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import AnniversaireModal from '../components/ui/AnniversaireModal';
import { employeService } from '../api/employeService';
import {
  HiOutlineBriefcase,
  HiOutlineCalendar,
  HiOutlineChartBar,
  HiOutlineClipboardList,
  HiOutlineDocumentText,
  HiOutlineLightningBolt,
  HiOutlineBell,
  HiOutlineSearch,
  HiOutlineUserGroup,
} from 'react-icons/hi';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { shouldDisplayNotification } from '../utils/notificationRules';
import { notificationService } from '../api/notificationService';
import { projetService } from '../api/projetService';
import { tacheService } from '../api/tacheService';
import { NotificationResponse, Projet, StatutProjet, StatutTache, TacheDetail } from '../types';

type Shortcut = {
  key: string;
  label: string;
  path: string;
  gradient: string;
  icon: React.ReactNode;
  permission?: string;
  permissions?: string[];
};

type RecentProject = {
  id: number;
  name: string;
  gradient: string;
  emoji: string;
  status: string;
  createdText: string;
  managers: string[];
  taskPath: string;
};

type ActivityItem = {
  id: string;
  icon: string;
  title: string;
  subtitle: string;
  time: string;
  path: string;
};

const shortcutsConfig: Shortcut[] = [
  {
    key: 'new-project',
    label: 'Nouveau projet',
    path: '/projets',
    gradient: 'linear-gradient(135deg, #e86a2e 0%, #f5a87a 100%)',
    icon: <HiOutlineBriefcase size={24} />,
    permissions: ['MANAGE_ALL_PROJETS', 'CREATE_PROJET'],
  },
  {
    key: 'new-task',
    label: 'Nouvelle tache',
    path: '/mes-taches',
    gradient: 'linear-gradient(135deg, #5963f3 0%, #9ea6ff 100%)',
    icon: <HiOutlineClipboardList size={24} />,
    permissions: ['VIEW_PROJETS_CREATE_TACHES', 'MANAGE_ALL_PROJETS', 'CREATE_TACHE'],
  },
  {
    key: 'media-plan',
    label: 'Media Plan',
    path: '/media-plan',
    gradient: 'linear-gradient(135deg, #0f9f77 0%, #46d7af 100%)',
    icon: <HiOutlineChartBar size={24} />,
    permission: 'VIEW_MEDIA_PLAN',
  },
  {
    key: 'calendar',
    label: 'Calendrier',
    path: '/admin/calendrier-projets',
    gradient: 'linear-gradient(135deg, #7c3aed 0%, #b28cff 100%)',
    icon: <HiOutlineCalendar size={24} />,
    permissions: ['VIEW_CALENDRIER_PROJETS', 'VIEW_DEADLINES', 'VIEW_REUNIONS'],
  },

];

const projectGradients = [
  'linear-gradient(135deg, #E86A2E, #F5A87A)',
  'linear-gradient(135deg, #7c3aed, #a78bfa)',
  'linear-gradient(135deg, #059669, #34d399)',
  'linear-gradient(135deg, #2563eb, #60a5fa)',
  'linear-gradient(135deg, #f59e0b, #fbbf24)',
  'linear-gradient(135deg, #0f9f77, #46d7af)',
];

const statusLabel: Record<string, string> = {
  PLANIFIE: 'Planifie',
  EN_COURS: 'En cours',
  CLOTURE: 'Cloture',
  CLOTURE_INCOMPLET: 'Cloture incomplet',
  ANNULE: 'Annule',
};

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

const formatRelativeTime = (value?: string | null): string => {
  if (!value) return 'Date inconnue';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Date inconnue';

  const diffMs = Date.now() - date.getTime();
  const diffMin = Math.max(1, Math.round(diffMs / 60000));
  if (diffMin < 60) return `Il y a ${diffMin} min`;
  const diffHours = Math.round(diffMin / 60);
  if (diffHours < 24) return `Il y a ${diffHours} h`;
  const diffDays = Math.round(diffHours / 24);
  return `Il y a ${diffDays} j`;
};

const buildInitials = (fullName?: string | null): string => {
  if (!fullName) return 'NA';
  const parts = fullName.trim().split(/\s+/).filter(Boolean);
  if (!parts.length) return 'NA';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
};

const hasPermission = (shortcut: Shortcut, userPermissions: string[]): boolean => {
  if (shortcut.permissions?.length) {
    return shortcut.permissions.some((permission) => userPermissions.includes(permission));
  }
  if (shortcut.permission) {
    return userPermissions.includes(shortcut.permission);
  }
  return true;
};

const toActivityIcon = (title: string): string => {
  const normalized = title.toLowerCase();
  if (normalized.includes('tache')) return '🧩';
  if (normalized.includes('projet')) return '🚀';
  if (normalized.includes('reunion')) return '📅';
  if (normalized.includes('validation')) return '✅';
  if (normalized.includes('retard')) return '⏰';
  return '🔔';
};

const AccueilProjetsPage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const employeId = user?.employeId;
  const userPermissions = user?.permissions || [];

  const [projets, setProjets] = useState<Projet[]>([]);
  const [taches, setTaches] = useState<TacheDetail[]>([]);
  const [notifications, setNotifications] = useState<NotificationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [anniversaire, setAnniversaire] = useState<{ annees: number } | null>(null);

  const displayName = user?.prenom?.trim() || 'System Admin';
  const heroDateText = useMemo(() => `${formatHeroDate(new Date())} · Antigone`, []);

  useEffect(() => {
    const fetchAccueilData = async () => {
      if (!employeId) {
        setLoading(false);
        return;
      }

      setLoading(true);
      try {
        const [projetsRes, tachesRes, notificationsRes] = await Promise.all([
          projetService.getByEmploye(employeId).catch(() => ({ data: { data: [] } })),
          tacheService.getByAssignee(employeId).catch(() => ({ data: { data: [] } })),
          notificationService.getByEmploye(employeId).catch(() => ({ data: { data: [] } })),
        ]);

        const allProjets: Projet[] = projetsRes.data.data || [];
        const now = new Date();
        const todayAtMidnight = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();

        const activeProjets = allProjets.filter(p => {
          if (!p.dateFin) return true;
          const df = new Date(p.dateFin).getTime();
          if (Number.isNaN(df)) return true;
          
          const diffDays = (todayAtMidnight - df) / (1000 * 3600 * 24);
          return diffDays <= 2; // Keep projects that are not older than 2 days
        });

        const allTaches: TacheDetail[] = tachesRes.data.data || [];
        const activeTaches = allTaches.filter(t => {
          if (!t.dateEcheance) return true;
          const df = new Date(t.dateEcheance).getTime();
          if (Number.isNaN(df)) return true;
          
          const diffDays = (todayAtMidnight - df) / (1000 * 3600 * 24);
          return diffDays <= 2; // Keep tasks that are not older than 2 days
        });

        setProjets(activeProjets);
        setTaches(activeTaches);
        const rawNotifications = notificationsRes.data.data || [];
        setNotifications(rawNotifications.filter(shouldDisplayNotification));
      } catch (error) {
        console.error('Erreur chargement accueil projets:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchAccueilData();
  }, [employeId]);

  /* ── Détection anniversaire d'embauche ── */
  useEffect(() => {
    if (!employeId) return;
    const sessionKey = `anniversaire_shown_${new Date().getFullYear()}_${employeId}`;
    if (sessionStorage.getItem(sessionKey)) return;
    employeService.getById(employeId)
      .then(res => {
        const employe = res.data.data;
        if (!employe?.dateEmbauche) return;
        const embauche = new Date(employe.dateEmbauche);
        const today = new Date();
        const anneesEcoules = today.getFullYear() - embauche.getFullYear();
        const isAnniversaire =
          anneesEcoules >= 1 &&
          embauche.getMonth() === today.getMonth() &&
          embauche.getDate() === today.getDate();
        if (isAnniversaire) {
          sessionStorage.setItem(sessionKey, '1');
          setAnniversaire({ annees: anneesEcoules });
        }
      })
      .catch(() => { /* silencieux */ });
  }, [employeId]);

  const shortcuts = useMemo(
    () => shortcutsConfig.filter((shortcut) => hasPermission(shortcut, userPermissions)),
    [userPermissions],
  );

  const canOpenProjectDetails = userPermissions.includes('VIEW_PROJETS') || userPermissions.includes('VIEW_TOUS_PROJETS');

  const recentProjects = useMemo<RecentProject[]>(() => {
    return [...projets]
      .sort((a, b) => new Date(b.dateDebut || 0).getTime() - new Date(a.dateDebut || 0).getTime())
      .slice(0, 6)
      .map((project, index) => {
        const managers = project.chefsDeProjet?.length
          ? project.chefsDeProjet.map((m) => buildInitials(`${m.prenom || ''} ${m.nom || ''}`))
          : project.chefDeProjet
            ? [buildInitials(`${project.chefDeProjet.prenom || ''} ${project.chefDeProjet.nom || ''}`)]
            : [buildInitials(project.createurNom || '')];

        const status = statusLabel[project.statut] || project.statut;
        const statusEmoji =
          project.statut === StatutProjet.EN_COURS ? '🚀' :
            project.statut === StatutProjet.PLANIFIE ? '📅' :
              project.statut === StatutProjet.CLOTURE ? '✅' :
                project.statut === StatutProjet.ANNULE ? '⛔' : '📁';

        return {
          id: project.id,
          name: project.nom,
          gradient: projectGradients[index % projectGradients.length],
          emoji: statusEmoji,
          status,
          createdText: `Cree par ${project.createurNom || 'System'} · ${formatShortDate(project.dateDebut)}`,
          managers,
          taskPath: canOpenProjectDetails
            ? (userPermissions.includes('VIEW_TOUS_PROJETS') && !userPermissions.includes('VIEW_PROJETS')
              ? `/admin/projets/${project.id}/taches`
              : `/projets/${project.id}/taches`)
            : '/mes-taches',
        };
      });
  }, [projets, canOpenProjectDetails, userPermissions]);

  const activities = useMemo<ActivityItem[]>(() => {
    const notificationActivities = [...notifications]
      .sort((a, b) => new Date(b.dateCreation).getTime() - new Date(a.dateCreation).getTime())
      .slice(0, 8)
      .map((notification) => ({
        id: `notif-${notification.id}`,
        icon: toActivityIcon(notification.titre || ''),
        title: notification.titre || 'Nouvelle notification',
        subtitle: notification.message || 'Mise a jour disponible',
        time: formatRelativeTime(notification.dateCreation),
        path: '/accueil#activite-recente',
      }));

    if (notificationActivities.length > 0) {
      return notificationActivities;
    }

    return [...taches]
      .filter((task) => !!task.dateEcheance)
      .sort((a, b) => new Date(a.dateEcheance || 0).getTime() - new Date(b.dateEcheance || 0).getTime())
      .slice(0, 8)
      .map((task) => ({
        id: `task-${task.id}`,
        icon: task.statut === StatutTache.DONE ? '✅' : task.urgente ? '⚡' : '🧩',
        title: task.titre,
        subtitle: task.projetNom ? `Projet: ${task.projetNom}` : 'Tache personnelle',
        time: formatRelativeTime(task.dateEcheance),
        path: '/mes-taches',
      }));
  }, [notifications, taches]);

  const heroStats = useMemo(() => {
    const projetsActifs = projets.filter((project) =>
      project.statut === StatutProjet.EN_COURS || project.statut === StatutProjet.PLANIFIE,
    ).length;
    const notifsNonLues = notifications.filter((notification) => !notification.lu).length;
    const tachesOuvertes = taches.filter((task) => task.statut !== StatutTache.DONE).length;

    return [
      { value: projetsActifs.toString(), label: 'Projets actifs' },
      { value: notifsNonLues.toString(), label: 'Notifications non lues' },
      { value: tachesOuvertes.toString(), label: 'Taches ouvertes' },
    ];
  }, [projets, notifications, taches]);

  const priorityTask = useMemo(() => {
    const openTasks = taches.filter((task) => task.statut !== StatutTache.DONE);
    const urgents = openTasks.filter(t => t.urgente);
    if (urgents.length > 0) {
      return urgents.sort((a, b) => new Date(a.dateEcheance || 0).getTime() - new Date(b.dateEcheance || 0).getTime())[0];
    }
    return openTasks.sort((a, b) => new Date(a.dateEcheance || 0).getTime() - new Date(b.dateEcheance || 0).getTime())[0] || null;
  }, [taches]);

  const taskStats = useMemo(() => {
    const total = taches.length;
    if (total === 0) return { aFaire: 0, enCours: 0, enRevision: 0, termine: 0, total: 0 };

    const aFaire = taches.filter(t => t.statut === StatutTache.TODO || !t.statut).length;
    const enCours = taches.filter(t => t.statut === StatutTache.IN_PROGRESS).length;
    const termine = taches.filter(t => t.statut === StatutTache.DONE).length;

    // Any other status falls back to "enRevision" or we just calculate it directly
    const enRevision = total - (aFaire + enCours + termine);

    return { aFaire, enCours, enRevision: Math.max(0, enRevision), termine, total };
  }, [taches]);

  // ── Framer-motion variants ────────────────────────────────────
  // @ts-ignore - ease: string is valid in framer-motion at runtime
  const fadeUp = (delay = 0) => ({
    initial: { opacity: 0, y: 22 },
    animate: { opacity: 1, y: 0 },
    transition: { duration: 0.52, ease: 'easeOut', delay },
  });

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg)', display: 'flex', flexDirection: 'column', gap: '28px', paddingBottom: '48px' }}>

      {/* ── HERO ────────────────────────────────────────────────────── */}
      {/* @ts-ignore */}
      <motion.section
        {...fadeUp(0)}
        aria-label="Bannière d'accueil"
        className="accueil-hero-card"
        style={{
          position: 'relative',
          overflow: 'hidden',
          margin: '24px 24px 0',
          borderRadius: '24px',
          padding: 'clamp(36px, 6vw, 56px) clamp(28px, 5vw, 56px)',
        }}
      >
        {/* Glow spot haut-droite */}
        <div style={{ position: 'absolute', right: '-80px', top: '-80px', width: '400px', height: '400px', borderRadius: '50%', background: 'rgba(232,106,46,0.12)', filter: 'blur(80px)', pointerEvents: 'none' }} />
        {/* Glow spot bas-gauche */}
        <div style={{ position: 'absolute', left: '-60px', bottom: '-60px', width: '300px', height: '300px', borderRadius: '50%', background: 'rgba(232,106,46,0.07)', filter: 'blur(70px)', pointerEvents: 'none' }} />

        {/* Content */}
        <div style={{ position: 'relative', zIndex: 1, display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) auto', gap: '32px', alignItems: 'end' }}>
          <div>
            {/* Label date — style RH */}
            <p style={{ margin: '0 0 14px', color: '#e86a2e', fontSize: '13px', fontWeight: 600, letterSpacing: '0.1em', textTransform: 'uppercase', display: 'flex', alignItems: 'center', gap: '6px' }}>
              <HiOutlineLightningBolt size={15} />
              {heroDateText}
            </p>

            <h1 className="accueil-hero-title" style={{ margin: 0, fontSize: 'clamp(26px, 4vw, 44px)', fontWeight: 800, letterSpacing: '-0.025em', lineHeight: 1.12 }}>
              Bonjour,{' '}
              <span style={{ background: 'linear-gradient(90deg, #e86a2e, #f5a87a)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', backgroundClip: 'text' }}>{displayName}</span>
              {' '}👋
            </h1>
            <p className="accueil-hero-subtitle" style={{ margin: '14px 0 0', fontSize: '15px', lineHeight: 1.65 }}>
              Prêt(e) à accomplir de grandes choses aujourd'hui&nbsp;? Voici votre tableau de bord.
            </p>

            <div className="group" style={{ position: 'relative', marginTop: '28px', maxWidth: '440px' }}>
              <HiOutlineSearch style={{ position: 'absolute', left: '18px', top: '50%', transform: 'translateY(-50%)', color: '#9ca3af', pointerEvents: 'none', zIndex: 1 }} size={18} />
              <input
                type="text"
                placeholder="Rechercher un projet, une tâche..."
                className="accueil-hero-search"
                style={{ width: '100%', height: '50px', borderRadius: '9999px', fontSize: '14px', padding: '0 24px 0 48px', outline: 'none', boxSizing: 'border-box', fontFamily: 'inherit', transition: 'all 0.2s' }}
                aria-label="Recherche principale"
                onFocus={e => { e.target.style.outline = 'none'; e.target.style.boxShadow = '0 0 0 3px rgba(232,106,46,0.25)'; }}
                onBlur={e => { e.target.style.boxShadow = 'none'; }}
              />
            </div>
          </div>

          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '12px', alignSelf: 'end' }}>
            {heroStats.map((stat, i) => (
              <motion.div
                key={stat.label}
                initial={{ opacity: 0, y: 16 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.45, delay: 0.15 + i * 0.08 }}
                className="accueil-hero-stat"
                style={{ width: '150px', borderRadius: '16px', padding: '18px 22px' }}
              >
                <strong className="accueil-hero-stat-value" style={{ display: 'block', fontSize: '26px', fontWeight: 800, lineHeight: 1 }}>{stat.value}</strong>
                <span className="accueil-hero-stat-label" style={{ display: 'block', marginTop: '7px', fontSize: '11px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em', wordBreak: 'break-word' }}>{stat.label}</span>
              </motion.div>
            ))}
          </div>
        </div>
      </motion.section>

      {/* ── FOCUS DU JOUR ─────────────────────────────────────────── */}
      {/* @ts-ignore */}
      <motion.section {...fadeUp(0.15)} style={{ padding: '0 24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
          <h2 style={{ margin: 0, color: 'var(--text-1)', fontSize: '17px', fontWeight: 700, letterSpacing: '-0.01em' }}>Focus du jour</h2>
        </div>

        {priorityTask ? (
          <div
            role="button"
            tabIndex={0}
            onClick={() => navigate('/mes-taches')}
            onKeyDown={e => e.key === 'Enter' && navigate('/mes-taches')}
            style={{ position: 'relative', overflow: 'hidden', background: 'var(--surface)', border: '1.5px solid rgba(232,106,46,0.2)', borderRadius: '20px', padding: '26px 30px', cursor: 'pointer', transition: 'border-color 0.2s, box-shadow 0.2s' }}
            onMouseEnter={e => { (e.currentTarget as HTMLDivElement).style.borderColor = 'rgba(232,106,46,0.45)'; (e.currentTarget as HTMLDivElement).style.boxShadow = '0 8px 40px rgba(232,106,46,0.1)'; }}
            onMouseLeave={e => { (e.currentTarget as HTMLDivElement).style.borderColor = 'rgba(232,106,46,0.2)'; (e.currentTarget as HTMLDivElement).style.boxShadow = 'none'; }}
          >
            <div style={{ position: 'absolute', top: '-40px', right: '-40px', width: '160px', height: '160px', borderRadius: '50%', background: 'radial-gradient(circle, rgba(232,106,46,0.08) 0%, transparent 70%)', pointerEvents: 'none' }} />
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '20px', flexWrap: 'wrap' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '18px' }}>
                <div style={{ flexShrink: 0, width: '52px', height: '52px', borderRadius: '16px', background: 'linear-gradient(135deg, #e86a2e, #f5a87a)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '22px', boxShadow: '0 4px 16px rgba(232,106,46,0.28)' }}>
                  {priorityTask.urgente ? '⚡' : '🎯'}
                </div>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '5px' }}>
                    <h3 style={{ margin: 0, color: 'var(--text-1)', fontSize: '16px', fontWeight: 700 }}>{priorityTask.titre}</h3>
                    {priorityTask.urgente && (
                      <span style={{ padding: '2px 9px', background: 'rgba(239,68,68,0.1)', color: '#ef4444', borderRadius: '9999px', fontSize: '10px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.06em' }}>Urgent</span>
                    )}
                  </div>
                  <p style={{ margin: 0, color: 'var(--text-2)', fontSize: '13px' }}>
                    Projet&nbsp;: <strong style={{ color: 'var(--text-1)', fontWeight: 600 }}>{priorityTask.projetNom || 'Personnel'}</strong>
                  </p>
                </div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <p style={{ margin: '0 0 4px', color: 'var(--text-2)', fontSize: '11px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.08em' }}>Échéance</p>
                <div style={{ display: 'flex', alignItems: 'center', gap: '7px', color: 'var(--text-1)', fontWeight: 700, fontSize: '14px' }}>
                  <HiOutlineCalendar size={15} style={{ color: 'var(--brand)' }} />
                  {formatShortDate(priorityTask.dateEcheance)}
                </div>
              </div>
            </div>
          </div>
        ) : (
          <div style={{ display: 'flex', alignItems: 'center', gap: '18px', padding: '22px 26px', background: 'rgba(16,185,129,0.05)', border: '1px solid rgba(16,185,129,0.15)', borderRadius: '20px' }}>
            <div style={{ flexShrink: 0, width: '46px', height: '46px', borderRadius: '50%', background: 'rgba(16,185,129,0.12)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '22px' }}>🎉</div>
            <div>
              <h3 style={{ margin: '0 0 4px', color: '#059669', fontSize: '15px', fontWeight: 700 }}>Vous êtes à jour !</h3>
              <p style={{ margin: 0, color: 'rgba(5,150,105,0.7)', fontSize: '13px' }}>Aucune tâche ouverte pour le moment. Profitez-en !</p>
            </div>
          </div>
        )}
      </motion.section>

      {/* ── GRID : PROJETS + TÂCHES ─────────────────────────────── */}
      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0,2fr) minmax(0,1fr)', gap: '24px', padding: '0 24px' }} className="accueil-main-grid">

        {/* MES PROJETS RÉCENTS */}
        {/* @ts-ignore */}
        <motion.section {...fadeUp(0.22)}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
            <h2 style={{ margin: 0, color: 'var(--text-1)', fontSize: '17px', fontWeight: 700, letterSpacing: '-0.01em' }}>Mes projets récents</h2>
            <button
              type="button"
              style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--brand)', fontSize: '13px', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '4px', fontFamily: 'inherit' }}
              onClick={() => navigate('/projets')}
            >
              Voir tout →
            </button>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: '16px' }}>
            {!loading && recentProjects.length === 0 ? (
              <div style={{ gridColumn: '1/-1', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '56px 24px', background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '20px', textAlign: 'center' }}>
                <span style={{ fontSize: '40px', opacity: 0.4, marginBottom: '12px' }}>📁</span>
                <h3 style={{ margin: '0 0 6px', color: 'var(--text-1)', fontSize: '15px', fontWeight: 700 }}>Aucun projet récent</h3>
                <p style={{ margin: 0, color: 'var(--text-2)', fontSize: '13px', maxWidth: '260px' }}>Vous n'êtes affecté(e) à aucun projet récent pour le moment.</p>
              </div>
            ) : (
              recentProjects.map((project, index) => (
                <motion.article
                  key={project.id}
                  initial={{ opacity: 0, y: 16 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.4, delay: 0.28 + index * 0.07 }}
                  style={{ position: 'relative', display: 'flex', flexDirection: 'column', background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '20px', padding: '22px', cursor: 'pointer', transition: 'transform 0.2s, box-shadow 0.2s', overflow: 'hidden' }}
                  onMouseEnter={e => { (e.currentTarget as HTMLElement).style.transform = 'translateY(-3px)'; (e.currentTarget as HTMLElement).style.boxShadow = '0 12px 40px rgba(0,0,0,0.1)'; }}
                  onMouseLeave={e => { (e.currentTarget as HTMLElement).style.transform = 'translateY(0)'; (e.currentTarget as HTMLElement).style.boxShadow = 'none'; }}
                  onClick={() => navigate(project.taskPath)}
                >
                  {/* Colour accent top bar */}
                  <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '3px', background: project.gradient, borderRadius: '20px 20px 0 0' }} />

                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px', marginTop: '4px' }}>
                    <span style={{ padding: '3px 10px', background: 'var(--bg)', border: '1px solid var(--border)', borderRadius: '9999px', fontSize: '10px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--text-2)' }}>
                      {project.status}
                    </span>
                    <span style={{ fontSize: '20px' }} aria-hidden="true">{project.emoji}</span>
                  </div>

                  <div style={{ flex: 1 }}>
                    <h3 style={{ margin: '0 0 6px', color: 'var(--text-1)', fontSize: '15px', fontWeight: 700, lineHeight: 1.3 }}>{project.name}</h3>
                    <p style={{ margin: 0, color: 'var(--text-2)', fontSize: '11px', fontWeight: 500 }}>{project.createdText}</p>
                  </div>

                  <div style={{ marginTop: '18px', paddingTop: '14px', borderTop: '1px solid var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <div style={{ display: 'flex' }} aria-label="Managers">
                      {project.managers.map((manager, idx) => (
                        <div
                          key={`${project.id}-${manager}-${idx}`}
                          style={{ width: '28px', height: '28px', borderRadius: '50%', border: '2px solid var(--surface)', background: 'var(--bg)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '9px', fontWeight: 700, color: 'var(--text-2)', marginLeft: idx > 0 ? '-8px' : 0 }}
                        >
                          {manager}
                        </div>
                      ))}
                    </div>
                    <span style={{ fontSize: '11px', fontWeight: 700, color: 'var(--brand)' }}>Voir tâches →</span>
                  </div>
                </motion.article>
              ))
            )}
          </div>
        </motion.section>

        {/* MES TÂCHES */}
        {/* @ts-ignore */}
        <motion.section {...fadeUp(0.3)}>
          <div style={{ marginBottom: '16px' }}>
            <h2 style={{ margin: 0, color: 'var(--text-1)', fontSize: '17px', fontWeight: 700, letterSpacing: '-0.01em' }}>Mes tâches</h2>
          </div>

          <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '20px', padding: '24px' }}>
            {!loading && taskStats.total === 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '32px 0', textAlign: 'center' }}>
                <div style={{ width: '52px', height: '52px', borderRadius: '50%', background: 'var(--bg)', display: 'flex', alignItems: 'center', justifyContent: 'center', opacity: 0.5, marginBottom: '12px' }}>
                  <HiOutlineClipboardList size={26} color="var(--text-2)" />
                </div>
                <h3 style={{ margin: '0 0 4px', color: 'var(--text-1)', fontSize: '14px', fontWeight: 700 }}>Aucune tâche</h3>
                <p style={{ margin: 0, color: 'var(--text-2)', fontSize: '12px' }}>Votre progression apparaîtra ici.</p>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                {/* Progress */}
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                    <span style={{ color: 'var(--text-1)', fontSize: '13px', fontWeight: 600 }}>Avancement global</span>
                    <span style={{ color: 'var(--brand)', fontSize: '16px', fontWeight: 800 }}>
                      {taskStats.total > 0 ? Math.round((taskStats.termine / taskStats.total) * 100) : 0}%
                    </span>
                  </div>
                  <div style={{ width: '100%', height: '6px', background: 'var(--bg)', borderRadius: '9999px', overflow: 'hidden' }}>
                    <motion.div
                      initial={{ width: 0 }}
                      animate={{ width: `${taskStats.total > 0 ? (taskStats.termine / taskStats.total) * 100 : 0}%` }}
                      transition={{ duration: 1, delay: 0.5, ease: 'easeOut' }}
                      style={{ height: '100%', background: 'linear-gradient(90deg, #e86a2e, #f5a87a)', borderRadius: '9999px' }}
                    />
                  </div>
                </div>

                {/* Stats */}
                <div style={{ paddingTop: '8px', borderTop: '1px solid var(--border)', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  {[
                    { label: 'À faire', count: taskStats.aFaire, color: 'var(--border)' },
                    { label: 'En cours', count: taskStats.enCours, color: '#60a5fa' },
                    { label: 'En révision', count: taskStats.enRevision, color: '#c084fc' },
                    { label: 'Terminées', count: taskStats.termine, color: '#34d399' },
                  ].map(row => (
                    <div key={row.label} style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }} onClick={() => navigate('/mes-taches')}>
                      <div style={{ width: '8px', height: '8px', borderRadius: '50%', background: row.color, marginRight: '10px', flexShrink: 0 }} />
                      <div style={{ flex: 1, color: 'var(--text-2)', fontSize: '13px', fontWeight: 500 }}>{row.label}</div>
                      <span style={{ color: 'var(--text-1)', fontSize: '13px', fontWeight: 700 }}>{row.count}</span>
                    </div>
                  ))}
                </div>

                <button
                  onClick={() => navigate('/mes-taches')}
                  style={{ marginTop: '4px', width: '100%', height: '40px', borderRadius: '12px', border: '1px solid var(--border)', background: 'var(--bg)', color: 'var(--text-2)', fontSize: '13px', fontWeight: 700, cursor: 'pointer', transition: 'border-color 0.2s, color 0.2s', fontFamily: 'inherit' }}
                  onMouseEnter={e => { (e.currentTarget as HTMLButtonElement).style.borderColor = 'var(--brand)'; (e.currentTarget as HTMLButtonElement).style.color = 'var(--brand)'; }}
                  onMouseLeave={e => { (e.currentTarget as HTMLButtonElement).style.borderColor = 'var(--border)'; (e.currentTarget as HTMLButtonElement).style.color = 'var(--text-2)'; }}
                >
                  Accéder au Kanban
                </button>
              </div>
            )}
          </div>
        </motion.section>
      </div>

      <style>{`
        /* ── Hero card ── */
        .accueil-hero-card {
          background: linear-gradient(135deg, #ffffff 0%, rgba(255,247,237,0.5) 50%, rgba(255,237,213,0.65) 100%);
          box-shadow: 0 8px 32px rgba(0,0,0,0.07), 0 2px 8px rgba(0,0,0,0.04);
        }
        .dark .accueil-hero-card {
          background: linear-gradient(135deg, #030712 0%, #111827 55%, #0d1117 100%) !important;
          box-shadow: 0 20px 60px rgba(0,0,0,0.45) !important;
        }

        /* ── Hero text ── */
        .accueil-hero-title { color: #1a1814; }
        .dark .accueil-hero-title { color: #f9fafb; }
        .accueil-hero-subtitle { color: #6b7280; }
        .dark .accueil-hero-subtitle { color: #9ca3af; }

        /* ── Hero search ── */
        .accueil-hero-search {
          background: rgba(255,255,255,0.72);
          border: 1px solid #e5e7eb;
          color: #1a1814;
          backdrop-filter: blur(8px);
        }
        .accueil-hero-search::placeholder { color: #9ca3af; }
        .dark .accueil-hero-search {
          background: rgba(255,255,255,0.06) !important;
          border-color: rgba(255,255,255,0.12) !important;
          color: #f9fafb !important;
        }
        .dark .accueil-hero-search::placeholder { color: #6b7280; }

        /* ── Hero stat cards ── */
        .accueil-hero-stat {
          background: rgba(255,255,255,0.72);
          border: 1px solid #e5e7eb;
          backdrop-filter: blur(8px);
          transition: background 0.2s;
        }
        .accueil-hero-stat:hover { background: rgba(255,255,255,0.92); }
        .dark .accueil-hero-stat {
          background: rgba(255,255,255,0.05) !important;
          border-color: rgba(255,255,255,0.1) !important;
        }
        .dark .accueil-hero-stat:hover { background: rgba(255,255,255,0.09) !important; }

        .accueil-hero-stat-value { color: #111827; }
        .dark .accueil-hero-stat-value { color: #f9fafb; }
        .accueil-hero-stat-label { color: #6b7280; }
        .dark .accueil-hero-stat-label { color: #9ca3af; }

        /* ── Responsive grid ── */
        @media (max-width: 900px) {
          .accueil-main-grid { grid-template-columns: 1fr !important; }
        }
        @media (max-width: 700px) {
          .accueil-hero-card > div > div:first-child { grid-template-columns: 1fr !important; }
        }
      `}</style>

      {anniversaire && (
        <AnniversaireModal
          prenom={displayName}
          annees={anniversaire.annees}
          onClose={() => setAnniversaire(null)}
        />
      )}
    </div>
  );
};

export default AccueilProjetsPage;
