import React from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import AppSwitchButton from '../ui/AppSwitchButton';
import AiAssistant from '../ai/AiAssistant';
import { AiAssistantProvider } from '../ai/AiAssistantContext';
import { useSidebar } from '../../hooks/useSidebar';

const MainLayout: React.FC = () => {
  const { isRailVisible } = useSidebar();
  return (
    // Le fournisseur englobe l'Outlet : c'est ce qui permet a une page (fiche
    // facture) d'ouvrir l'assistant avec une demande deja formulee.
    <AiAssistantProvider>
      <div className="min-h-screen bg-[var(--bg)] dark:bg-gray-950">
        <Sidebar />
        <div className={`transition-all duration-300 ease-in-out ${isRailVisible ? 'lg:ml-[80px]' : 'lg:ml-0'}`}>
          <main className="mx-auto max-w-(--breakpoint-2xl) p-4 md:p-6">
            <Outlet />
          </main>
        </div>
        <AppSwitchButton />
        <AiAssistant />
      </div>
    </AiAssistantProvider>
  );
};

export default MainLayout;
