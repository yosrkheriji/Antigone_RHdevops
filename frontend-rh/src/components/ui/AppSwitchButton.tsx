import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { HiChevronLeft, HiChevronRight } from 'react-icons/hi';
import { useAuth } from '../../context/AuthContext';
import { relayAuthSnapshotForSwitch } from '../../utils/authStorage';

type AppKind = 'projects' | 'rh' | 'finance';

const APPS: Array<{ kind: AppKind; label: string; emoji: string; envKey: string; fallbackPath: string }> = [
  { kind: 'projects', label: 'Projets', emoji: '💼', envKey: 'VITE_PROJECTS_APP_URL', fallbackPath: '/dashboard' },
  { kind: 'rh', label: 'RH', emoji: '👥', envKey: 'VITE_RH_APP_URL', fallbackPath: '/dashboard-rh' },
  { kind: 'finance', label: 'Finance', emoji: '💰', envKey: 'VITE_FINANCE_APP_URL', fallbackPath: '/salaires' },
];

const resolveAppKind = (): AppKind => {
  const appKind = (import.meta.env.VITE_APP_KIND as string | undefined)?.trim().toLowerCase();
  if (appKind === 'rh') return 'rh';
  if (appKind === 'finance') return 'finance';
  return 'projects';
};

const AppSwitchButton: React.FC = () => {
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuth();
  const [hoveredOption, setHoveredOption] = useState<AppKind | null>(null);
  const [collapsed, setCollapsed] = useState(false);

  if (!isAuthenticated) return null;

  const appKind = resolveAppKind();

  // L'espace Finance n'est proposé qu'aux comptes qui en ont la permission.
  const canViewFinance = user?.permissions?.includes('VIEW_FINANCE') || user?.roles?.includes('ADMIN');
  const visibleApps = APPS.filter((app) => app.kind !== 'finance' || canViewFinance);

  const switchTo = (target: AppKind) => {
    if (target === appKind) return;

    const app = APPS.find((a) => a.kind === target)!;
    const targetUrl = (import.meta.env[app.envKey as keyof ImportMetaEnv] as string | undefined)?.trim();

    if (targetUrl) {
      relayAuthSnapshotForSwitch();
      window.location.href = targetUrl;
      return;
    }

    navigate(app.fallbackPath);
  };

  if (collapsed) {
    return (
      <div className="fixed right-0 bottom-6 z-[10000]">
        <button
          type="button"
          onClick={() => setCollapsed(false)}
          title="Afficher le sélecteur d'application"
          className="flex items-center justify-center w-8 h-10 rounded-l-full bg-white/90 dark:bg-gray-800/90 border border-r-0 border-gray-200/90 dark:border-gray-700/90 shadow-2xl shadow-black/10 dark:shadow-black/30 backdrop-blur-xl text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white transition-colors"
        >
          <HiChevronLeft size={16} />
        </button>
      </div>
    );
  }

  const hoveredLabel = APPS.find((a) => a.kind === hoveredOption)?.label;

  return (
    <div className="fixed right-6 bottom-6 z-[10000] flex items-center gap-1.5 p-1.5 rounded-full bg-white/90 dark:bg-gray-800/90 border border-gray-200/90 dark:border-gray-700/90 shadow-2xl shadow-black/10 dark:shadow-black/30 backdrop-blur-xl transition-all">
      {visibleApps.map((app, index) => (
        <React.Fragment key={app.kind}>
          {index > 0 && <span aria-hidden="true" className="w-[1px] h-5 bg-gray-200 dark:bg-gray-700 shrink-0" />}
          <button
            type="button"
            onClick={() => switchTo(app.kind)}
            onMouseEnter={() => setHoveredOption(app.kind)}
            onMouseLeave={() => setHoveredOption(null)}
            disabled={appKind === app.kind}
            title={`Aller vers ${app.label}`}
            className={`min-w-[100px] h-11 rounded-full px-4 inline-flex items-center justify-center gap-2 text-[13px] font-bold tracking-wide transition-all duration-200 whitespace-nowrap ${
              appKind === app.kind
                ? 'bg-gradient-to-r from-brand-600 to-brand-500 dark:from-brand-500 dark:to-brand-400 text-white shadow-lg shadow-brand-500/30 -translate-y-[1px] cursor-default'
                : 'bg-transparent text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 hover:text-gray-900 dark:hover:text-white cursor-pointer'
            }`}
          >
            <span aria-hidden="true">{app.emoji}</span>
            <span>{app.label}</span>
          </button>
        </React.Fragment>
      ))}

      <span aria-hidden="true" className="w-[1px] h-5 bg-gray-200 dark:bg-gray-700 shrink-0" />

      <button
        type="button"
        onClick={() => setCollapsed(true)}
        title="Réduire"
        className="w-8 h-8 rounded-full inline-flex items-center justify-center text-gray-400 dark:text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700 hover:text-gray-700 dark:hover:text-gray-300 transition-colors"
      >
        <HiChevronRight size={16} />
      </button>

      <div
        className={`absolute right-0 bottom-[62px] translate-y-1 bg-gray-900 dark:bg-gray-100 text-white dark:text-gray-900 text-[11px] font-semibold px-3 py-1.5 rounded-lg whitespace-nowrap pointer-events-none transition-all duration-150 shadow-lg ${hoveredOption ? 'opacity-100' : 'opacity-0'}`}
      >
        {hoveredLabel ? `Aller vers ${hoveredLabel}` : ''}
      </div>
    </div>
  );
};

export default AppSwitchButton;
