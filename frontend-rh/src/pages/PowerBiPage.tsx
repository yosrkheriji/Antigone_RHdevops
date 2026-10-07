import React from 'react';
import { HiOutlineChartBar, HiOutlineExclamationCircle, HiOutlineExternalLink } from 'react-icons/hi';

/**
 * Rapport Power BI embarque (lien « reportEmbed »).
 *
 * Le rapport lit le meme entrepot `dwh` que la page Analytique ; la navigation
 * entre ses trois pages (RH, Projets, Finance) se fait dans le rapport lui-meme.
 * L'affichage exige une session Power BI ouverte dans le navigateur.
 */

const URL_RAPPORT = import.meta.env.VITE_POWERBI_PRESENCE_URL?.trim();

const PowerBiPage: React.FC = () => (
  <div className="space-y-4">
    <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-gray-200 bg-white px-5 py-4 dark:border-gray-800 dark:bg-gray-dark">
      <div className="flex min-w-0 items-center gap-4">
        <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-500 dark:bg-brand-500/15 dark:text-brand-400">
          <HiOutlineChartBar size={22} />
        </div>
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-lg font-bold text-gray-800 dark:text-white">Analytics BI</h1>
            <span className="rounded-full bg-warning-50 px-2.5 py-0.5 text-theme-xs font-semibold text-warning-600 dark:bg-warning-500/15 dark:text-warning-400">
              Power BI
            </span>
          </div>
          <p className="mt-0.5 text-theme-sm text-gray-500 dark:text-gray-400">
            Présence, projets et finance — données issues de l'entrepôt décisionnel.
          </p>
        </div>
      </div>
      {URL_RAPPORT && (
        <a
          href={URL_RAPPORT}
          target="_blank"
          rel="noopener noreferrer"
          className="flex items-center gap-2 rounded-lg bg-brand-500 px-4 py-2 text-theme-sm font-semibold text-white shadow-theme-xs transition-colors hover:bg-brand-600"
        >
          <HiOutlineExternalLink size={16} /> Plein écran
        </a>
      )}
    </div>

    {URL_RAPPORT ? (
      <div className="overflow-hidden rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-gray-dark">
        <iframe
          title="Power BI — DashboardsPFE"
          src={URL_RAPPORT}
          className="block h-[calc(100vh-200px)] min-h-140 w-full border-0"
          allowFullScreen
        />
      </div>
    ) : (
      <div className="flex items-start gap-3 rounded-2xl border border-warning-200 bg-warning-50 p-4 dark:border-warning-500/30 dark:bg-warning-500/10">
        <HiOutlineExclamationCircle size={20} className="mt-0.5 shrink-0 text-warning-500" />
        <p className="text-theme-sm text-warning-700 dark:text-warning-400">
          Aucune URL Power BI configuree. Renseigner la variable <code>VITE_POWERBI_PRESENCE_URL</code>{' '}
          avec le lien d'integration du rapport, puis relancer le build.
        </p>
      </div>
    )}
  </div>
);

export default PowerBiPage;
