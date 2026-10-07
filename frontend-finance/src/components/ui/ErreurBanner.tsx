import React from 'react';
import { HiOutlineExclamationCircle, HiOutlineRefresh } from 'react-icons/hi';

interface Props {
  message: string;
  onRetry?: () => void;
}

/** Bandeau d'erreur de chargement — évite de confondre « vide » et « échec ». */
const ErreurBanner: React.FC<Props> = ({ message, onRetry }) => (
  <div className="flex items-start gap-3 rounded-2xl border border-error-200 bg-error-50 p-4 dark:border-error-500/30 dark:bg-error-500/10">
    <HiOutlineExclamationCircle size={20} className="mt-0.5 shrink-0 text-error-500" />
    <div className="min-w-0 flex-1">
      <p className="text-theme-sm font-semibold text-error-600 dark:text-error-400">
        Échec du chargement des données
      </p>
      <p className="mt-0.5 text-theme-sm text-error-600/80 dark:text-error-400/80">{message}</p>
    </div>
    {onRetry && (
      <button
        onClick={onRetry}
        className="flex shrink-0 items-center gap-1.5 rounded-lg border border-error-300 px-3 py-1.5 text-theme-xs font-semibold text-error-600 hover:bg-error-100 dark:border-error-500/40 dark:text-error-400 dark:hover:bg-error-500/20"
      >
        <HiOutlineRefresh size={14} /> Réessayer
      </button>
    )}
  </div>
);

export default ErreurBanner;
