import React, { useEffect } from 'react';

/** L'authentification est centralisée dans l'app Projets — on y redirige. */
const LoginPage: React.FC = () => {
  const projectsAppUrl = (import.meta.env.VITE_PROJECTS_APP_URL as string | undefined)?.trim();

  useEffect(() => {
    if (projectsAppUrl) {
      window.location.replace(`${projectsAppUrl}/login`);
    }
  }, [projectsAppUrl]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-900">
      <p className="text-gray-400 text-sm">Redirection vers la page de connexion...</p>
    </div>
  );
};

export default LoginPage;
