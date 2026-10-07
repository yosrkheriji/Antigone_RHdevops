import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import MainLayout from './components/layout/MainLayout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import SalairesPage from './pages/SalairesPage';
import ChargesPage from './pages/ChargesPage';
import CnssPage from './pages/CnssPage';
import FacturesPage from './pages/FacturesPage';
import RevenusPage from './pages/RevenusPage';
import DettesPage from './pages/DettesPage';
import ParametresPaiePage from './pages/ParametresPaiePage';
import AnalytiquePage from './pages/AnalytiquePage';

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) {
    const projectsAppUrl = (import.meta.env.VITE_PROJECTS_APP_URL as string | undefined)?.trim();
    if (projectsAppUrl) {
      window.location.replace(`${projectsAppUrl}/login`);
      return (
        <div className="min-h-screen flex items-center justify-center bg-gray-900">
          <p className="text-gray-400 text-sm">Redirection vers la page de connexion...</p>
        </div>
      );
    }
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
};

/** Toute l'app Finance requiert VIEW_FINANCE — sinon on renvoie vers l'app RH. */
const FinanceGuard: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user } = useAuth();
  const allowed = user?.permissions?.includes('VIEW_FINANCE') || user?.roles?.includes('ADMIN');

  if (!allowed) {
    const rhAppUrl = (import.meta.env.VITE_RH_APP_URL as string | undefined)?.trim();
    return (
      <div className="min-h-screen flex flex-col items-center justify-center gap-4 bg-gray-50 dark:bg-gray-950 p-6 text-center">
        <p className="text-lg font-semibold text-gray-800 dark:text-white">Accès refusé</p>
        <p className="max-w-md text-sm text-gray-500 dark:text-gray-400">
          Votre compte ne dispose pas de la permission <strong>VIEW_FINANCE</strong> nécessaire pour accéder à l'espace Finance.
        </p>
        {rhAppUrl && (
          <a
            href={rhAppUrl}
            className="rounded-lg bg-brand-500 px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-600"
          >
            Retourner à l'espace RH
          </a>
        )}
      </div>
    );
  }
  return <>{children}</>;
};

const App: React.FC = () => {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <FinanceGuard>
              <MainLayout />
            </FinanceGuard>
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="factures" element={<FacturesPage />} />
        <Route path="revenus" element={<RevenusPage />} />
        <Route path="salaires" element={<SalairesPage />} />
        <Route path="charges" element={<ChargesPage />} />
        <Route path="dettes" element={<DettesPage />} />
        <Route path="cnss" element={<CnssPage />} />
        <Route path="analytique" element={<AnalytiquePage />} />
        <Route path="parametres" element={<ParametresPaiePage />} />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
};

export default App;
