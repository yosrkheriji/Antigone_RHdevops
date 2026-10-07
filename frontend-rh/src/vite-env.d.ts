/// <reference types="vite/client" />

interface ImportMetaEnv {
	readonly VITE_API_URL?: string;
	readonly VITE_APP_KIND?: 'projects' | 'rh' | 'finance';
	readonly VITE_RH_APP_URL?: string;
	readonly VITE_PROJECTS_APP_URL?: string;
	readonly VITE_FINANCE_APP_URL?: string;
	readonly VITE_POWERBI_PRESENCE_URL?: string;
	readonly VITE_POWERBI_FINANCE_URL?: string;
	readonly VITE_POWERBI_PROJETS_URL?: string;
}

interface ImportMeta {
	readonly env: ImportMetaEnv;
}
