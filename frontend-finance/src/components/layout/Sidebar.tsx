import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { flushSync } from 'react-dom';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import {
  HiChevronLeft,
  HiChevronRight,
  HiOutlineCash,
  HiOutlineCog,
  HiOutlineDocumentText,
  HiOutlineHome,
  HiOutlineLibrary,
  HiOutlineLogout,
  HiOutlineMoon,
  HiOutlinePresentationChartLine,
  HiOutlineReceiptTax,
  HiOutlineSparkles,
  HiOutlineSun,
  HiOutlineTrendingDown,
  HiOutlineTrendingUp,
} from 'react-icons/hi';
import { API_BASE } from '../../api/axios';
import { useSidebar } from '../../hooks/useSidebar';
import { useAuth } from '../../context/AuthContext';
import { useTheme, AppFont, AppFontSize, setSharedCookie } from '../../hooks/useTheme';
import { useAiAssistant } from '../ai/AiAssistantContext';
import './SidebarCanva.css';

interface NavItemDef {
  key: string;
  label: string;
  path: string;
  icon: React.ReactNode;
}

const panelGroups: Array<{ title: string; items: NavItemDef[] }> = [
  {
    title: 'ENCAISSEMENTS',
    items: [
      { key: 'factures', label: 'Factures & devis', path: '/factures', icon: <HiOutlineDocumentText size={18} /> },
      { key: 'revenus', label: 'Autres revenus', path: '/revenus', icon: <HiOutlineTrendingUp size={18} /> },
    ],
  },
  {
    title: 'DÉCAISSEMENTS',
    items: [
      { key: 'salaires', label: 'Salaires', path: '/salaires', icon: <HiOutlineCash size={18} /> },
      { key: 'charges', label: 'Charges', path: '/charges', icon: <HiOutlineReceiptTax size={18} /> },
      { key: 'dettes', label: 'Dettes', path: '/dettes', icon: <HiOutlineTrendingDown size={18} /> },
    ],
  },
  {
    title: 'ÉTAT & PARAMÈTRES',
    items: [
      { key: 'analytique', label: 'Analytique (BI)', path: '/analytique', icon: <HiOutlinePresentationChartLine size={18} /> },
      { key: 'cnss', label: 'CNSS trimestriel', path: '/cnss', icon: <HiOutlineLibrary size={18} /> },
      { key: 'parametres', label: 'Paramètres de paie', path: '/parametres', icon: <HiOutlineCog size={18} /> },
    ],
  },
];

const railItems = [
  { key: 'dashboard', label: 'Tableau de bord', icon: <HiOutlineHome size={20} />, path: '/dashboard', prefixes: ['/dashboard'] },
  { key: 'factures', label: 'Factures', icon: <HiOutlineDocumentText size={20} />, path: '/factures', prefixes: ['/factures', '/revenus'] },
  { key: 'salaires', label: 'Salaires', icon: <HiOutlineCash size={20} />, path: '/salaires', prefixes: ['/salaires'] },
  { key: 'charges', label: 'Charges', icon: <HiOutlineReceiptTax size={20} />, path: '/charges', prefixes: ['/charges', '/dettes'] },
  { key: 'cnss', label: 'CNSS', icon: <HiOutlineLibrary size={20} />, path: '/cnss', prefixes: ['/cnss'] },
  { key: 'analytique', label: 'Analytique', icon: <HiOutlinePresentationChartLine size={20} />, path: '/analytique', prefixes: ['/analytique'] },
  { key: 'parametres', label: 'Paramètres', icon: <HiOutlineCog size={20} />, path: '/parametres', prefixes: ['/parametres'] },
];

const pathMatches = (currentPath: string, prefix: string) =>
  currentPath === prefix || currentPath.startsWith(`${prefix}/`);

/** Bouton de thème avec animation View Transition (cercle qui s'expanse). */
const ThemeAnimatedBtn: React.FC<{
  targetTheme: 'light' | 'dark';
  current: string;
  setTheme: (t: 'light' | 'dark') => void;
  children: React.ReactNode;
}> = ({ targetTheme, current, setTheme, children }) => {
  const btnRef = useRef<HTMLButtonElement>(null);

  const handleClick = useCallback(async () => {
    if (current === targetTheme || !btnRef.current) return;

    const applyTheme = () => {
      flushSync(() => {
        document.documentElement.classList.toggle('dark', targetTheme === 'dark');
        localStorage.setItem('theme', targetTheme);
        setSharedCookie('theme', targetTheme);
        setTheme(targetTheme);
      });
    };

    if (!document.startViewTransition) {
      applyTheme();
      return;
    }

    await document.startViewTransition(applyTheme).ready;

    const { left, top, width, height } = btnRef.current.getBoundingClientRect();
    const centerX = left + width / 2;
    const centerY = top + height / 2;
    const maxDistance = Math.hypot(
      Math.max(centerX, window.innerWidth - centerX),
      Math.max(centerY, window.innerHeight - centerY),
    );

    document.documentElement.animate(
      {
        clipPath: [
          `circle(0px at ${centerX}px ${centerY}px)`,
          `circle(${maxDistance}px at ${centerX}px ${centerY}px)`,
        ],
      },
      { duration: 700, easing: 'ease-in-out', pseudoElement: '::view-transition-new(root)' },
    );
  }, [current, targetTheme, setTheme]);

  return (
    <button
      ref={btnRef}
      onClick={handleClick}
      className={`pc-account-popup-theme-btn ${current === targetTheme ? 'active' : ''}`}
      title={targetTheme === 'light' ? 'Mode clair' : 'Mode sombre'}
    >
      {children}
    </button>
  );
};

const Sidebar: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const { theme, setTheme, font, setFont, fontSize, setFontSize } = useTheme();
  const { isExpanded, isMobileOpen, isRailVisible, toggleSidebar, toggleMobileSidebar, toggleRail } = useSidebar();
  const assistant = useAiAssistant();
  const canUseAssistant = useMemo(
    () => !!user?.roles?.includes('ADMIN') || !!user?.permissions?.includes('VIEW_FINANCE'),
    [user],
  );

  const [showAccountMenu, setShowAccountMenu] = useState(false);
  const accountMenuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleOutsideClick = (event: MouseEvent) => {
      if (accountMenuRef.current && !accountMenuRef.current.contains(event.target as Node)) {
        setShowAccountMenu(false);
      }
    };
    const handleEscapeKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setShowAccountMenu(false);
    };

    document.addEventListener('mousedown', handleOutsideClick);
    document.addEventListener('keydown', handleEscapeKey);
    return () => {
      document.removeEventListener('mousedown', handleOutsideClick);
      document.removeEventListener('keydown', handleEscapeKey);
    };
  }, []);

  const userInitials = `${user?.prenom?.[0] ?? ''}${user?.nom?.[0] ?? ''}`.toUpperCase() || 'FI';

  const activeRailKey = useMemo(
    () => railItems.find((item) => item.prefixes.some((p) => pathMatches(location.pathname, p)))?.key,
    [location.pathname],
  );

  const closeAllPanels = () => {
    if (isExpanded) toggleSidebar();
    if (isMobileOpen) toggleMobileSidebar();
    setShowAccountMenu(false);
  };

  return (
    <>
      <aside className={`pc-icon-rail-shell ${isMobileOpen ? 'is-mobile-open' : ''} ${!isRailVisible ? 'rail-hidden' : ''}`}>
        <button
          type="button"
          className="pc-logo-btn"
          onClick={() => {
            closeAllPanels();
            navigate('/salaires');
          }}
          aria-label="Accueil Finance"
        >
          <img src="/antigone-icon.svg" alt="Antigone" className="h-8 w-8 object-contain" />
        </button>

        <div className="pc-rail-divider" />

        <div className="pc-rail-list">
          {railItems.map((item) => (
            <button
              key={item.key}
              type="button"
              className={`pc-icon-btn ${activeRailKey === item.key ? 'active' : ''}`}
              onClick={() => {
                closeAllPanels();
                navigate(item.path);
              }}
              aria-label={item.label}
            >
              <span className="pc-icon-wrapper">{item.icon}</span>
              <span className="iconLabel">{item.label}</span>
            </button>
          ))}
        </div>

        {assistant && canUseAssistant && (
          <>
            <div className="pc-rail-divider" />
            <button
              type="button"
              className="pc-ai-assistant-btn"
              onClick={() => {
                closeAllPanels();
                assistant.setOpen(true);
              }}
              aria-label="Ouvrir l'assistant relances"
              title="Assistant relances"
            >
              <span className="pc-icon-wrapper">
                <HiOutlineSparkles size={20} />
              </span>
              <span className="iconLabel">Assistant</span>
            </button>
          </>
        )}

        <div className="pc-rail-spacer" />

        <div className="pc-account-menu-wrap" ref={accountMenuRef}>
          <button
            type="button"
            className="pc-avatar-btn"
            title={`${user?.prenom ?? ''} ${user?.nom ?? ''}`}
            onClick={() => setShowAccountMenu((prev) => !prev)}
            aria-label="Compte utilisateur"
          >
            <div className="pc-avatar-chip">{userInitials}</div>
          </button>

          {showAccountMenu && (
            <div className="pc-account-popup">
              <div className="pc-account-popup-header">
                <div className="flex flex-1 items-center gap-3">
                  {user?.imageUrl ? (
                    <img src={`${API_BASE}${user.imageUrl}`} alt="" className="h-11 w-11 shrink-0 rounded-full object-cover" />
                  ) : (
                    <div
                      className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full text-sm font-bold text-white"
                      style={{ background: 'linear-gradient(135deg, #683b77, #ab78c3)' }}
                    >
                      {userInitials}
                    </div>
                  )}
                  <div className="min-w-0 flex-1">
                    <p className="pc-account-name">{user?.prenom} {user?.nom}</p>
                    <p className="pc-account-email">{user?.email}</p>
                    <span className="pc-account-role-badge">{user?.roles?.[0] || 'Finance'}</span>
                  </div>
                </div>
              </div>

              <div className="pc-account-popup-divider" />
              <div className="pc-account-popup-section-label">Theme</div>
              <div className="pc-account-popup-theme-options">
                <ThemeAnimatedBtn targetTheme={theme === 'dark' ? 'light' : 'dark'} current={theme} setTheme={setTheme}>
                  {theme === 'dark' ? <HiOutlineSun size={18} /> : <HiOutlineMoon size={18} />}
                  <span>{theme === 'dark' ? 'Léger' : 'Sombre'}</span>
                </ThemeAnimatedBtn>
              </div>

              <div className="pc-account-popup-divider" />
              <div className="pc-account-popup-section-label">Police d'écriture</div>
              <div className="pc-account-popup-theme-options" style={{ flexWrap: 'wrap', gap: '8px', padding: '8px 12px' }}>
                {(['Inter', 'Poppins'] as AppFont[]).map((f) => (
                  <button
                    key={f}
                    onClick={() => setFont(f)}
                    className={`pc-account-popup-theme-btn ${font === f ? 'active' : ''}`}
                    title={f}
                    style={{ fontFamily: f, fontSize: '11px', padding: '4px 12px', height: 'auto', minHeight: 'unset' }}
                  >
                    {f}
                  </button>
                ))}
              </div>

              <div className="pc-account-popup-section-label" style={{ marginTop: '4px' }}>Taille du texte</div>
              <div style={{ padding: '4px 12px 10px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  {(['13px', '14px', '15px', '16px'] as AppFontSize[]).map((s) => (
                    <button
                      key={s}
                      onClick={() => setFontSize(s)}
                      style={{
                        fontSize: '10px',
                        padding: '2px 6px',
                        borderRadius: '6px',
                        border: fontSize === s ? '1px solid var(--brand)' : '1px solid transparent',
                        background: fontSize === s ? 'var(--brand-light)' : 'transparent',
                        color: fontSize === s ? 'var(--brand)' : 'var(--text-3)',
                        cursor: 'pointer',
                        fontWeight: fontSize === s ? 600 : 400,
                        transition: 'all 0.15s',
                      }}
                    >
                      {s}
                    </button>
                  ))}
                </div>
              </div>

              <div className="pc-account-popup-divider" />
              <button
                onClick={() => {
                  setShowAccountMenu(false);
                  logout();
                }}
                className="pc-account-popup-item pc-account-popup-item-logout"
              >
                <HiOutlineLogout size={18} />
                <span>Se deconnecter</span>
              </button>
            </div>
          )}
        </div>
      </aside>

      <button
        type="button"
        className={`pc-rail-toggle-btn ${!isRailVisible ? 'rail-hidden' : ''}`}
        onClick={toggleRail}
        aria-label={isRailVisible ? 'Fermer la barre' : 'Ouvrir la barre'}
      >
        {isRailVisible ? <HiChevronLeft size={12} /> : <HiChevronRight size={12} />}
      </button>

      {isExpanded && isRailVisible && (
        <button type="button" className="pc-panel-overlay" onClick={closeAllPanels} aria-label="Fermer le panel" />
      )}

      <section className={`pc-secondary-panel ${isExpanded && isRailVisible ? 'open' : ''}`} aria-hidden={!isExpanded || !isRailVisible}>
        <header className="pc-secondary-header">
          <div>
            <p className="pc-secondary-title">Antigone Finance</p>
          </div>
        </header>

        <div className="pc-secondary-content custom-scrollbar">
          {panelGroups.map((group) => (
            <div key={group.title} className="pc-panel-group">
              <p className="pc-panel-group-title">{group.title}</p>
              <ul className="pc-panel-list">
                {group.items.map((item) => (
                  <li key={item.key}>
                    <NavLink
                      to={item.path}
                      onClick={(event) => { event.stopPropagation(); closeAllPanels(); }}
                      className={({ isActive }) => `pc-panel-item ${isActive ? 'is-active' : ''}`}
                    >
                      <span className="pc-panel-item-icon">{item.icon}</span>
                      <span className="pc-panel-item-label">{item.label}</span>
                    </NavLink>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </section>
    </>
  );
};

export default Sidebar;
