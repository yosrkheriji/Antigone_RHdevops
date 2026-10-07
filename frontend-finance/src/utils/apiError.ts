/**
 * Traduit une erreur axios en message lisible.
 *
 * Sans ça, un 403 ou un 404 se transforme en liste vide et l'écran affiche
 * « aucune donnée » alors que le problème est ailleurs.
 */
export const messageErreur = (e: unknown): string => {
  const err = e as {
    response?: { status?: number; data?: { message?: string } };
    code?: string;
    message?: string;
  };

  const statut = err?.response?.status;
  const messageServeur = err?.response?.data?.message;

  if (messageServeur) return messageServeur;

  switch (statut) {
    case 400:
      // Le backend renvoie 400 pour toute RuntimeException. Sans message, c'est
      // typiquement une exception non maîtrisée (NPE) : la stack est dans les logs.
      return 'Requête rejetée par le serveur (400) — exception côté backend, consultez ses logs.';
    case 401:
      return 'Session expirée — reconnectez-vous.';
    case 403:
      return "Accès refusé : votre compte n'a pas la permission VIEW_FINANCE.";
    case 404:
      return "Endpoint introuvable — le backend n'expose pas encore cette route. Redémarrez-le.";
    case 500:
      return 'Erreur serveur — consultez les logs du backend.';
    default:
      break;
  }

  // Un blocage CORS n'expose aucun statut HTTP au navigateur : il arrive ici,
  // indistinguable d'un backend éteint. On cite donc les deux causes.
  if (err?.code === 'ERR_NETWORK') {
    return "Requête bloquée : soit le backend est arrêté, soit l'origine de cette app "
      + "n'est pas autorisée par CORS (app.frontend-url doit contenir http://localhost:3002).";
  }

  return err?.message || 'Erreur inconnue';
};
