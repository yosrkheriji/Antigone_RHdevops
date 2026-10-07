import React, { createContext, useContext, useMemo } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AiChatClient } from '../api/client';
import { AiApiError } from '../api/errors';
import type { Capability } from '../api/types';

interface AiClientContextValue {
  client: AiChatClient;
  /** Capacite metier de l'app hote. Sert l'affichage, jamais le routage : le backend classe l'intention. */
  capability: Capability;
  isAdmin: boolean;
}

const AiClientContext = createContext<AiClientContextValue | null>(null);

/**
 * Un `QueryClient` dedie au widget plutot que celui de l'hote.
 *
 * Aucune des deux applications n'utilise TanStack Query aujourd'hui, et rien ne
 * garantit qu'un futur client hote aurait la meme politique de retry. Un client
 * propre evite d'imposer nos reglages a l'application, et reciproquement.
 */
function createWidgetQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        refetchOnWindowFocus: false,
        retry: (failureCount, error) => {
          // Un refus de perimetre ou une ressource absente ne changeront pas au
          // deuxieme essai : reessayer ne ferait que retarder le message d'erreur.
          if (error instanceof AiApiError) {
            if (
              error.code === 'FORBIDDEN' ||
              error.code === 'NOT_FOUND' ||
              error.code === 'UNAUTHENTICATED' ||
              error.code === 'AI_UNAVAILABLE' ||
              error.code === 'RATE_LIMITED'
            ) {
              return false;
            }
          }
          return failureCount < 2;
        },
      },
      mutations: { retry: false },
    },
  });
}

export interface AiClientProviderProps {
  client: AiChatClient;
  capability: Capability;
  isAdmin?: boolean;
  /** Injectable pour les tests, qui fournissent leur propre client. */
  queryClient?: QueryClient;
  children: React.ReactNode;
}

export const AiClientProvider: React.FC<AiClientProviderProps> = ({
  client,
  capability,
  isAdmin = false,
  queryClient,
  children,
}) => {
  const resolvedQueryClient = useMemo(
    () => queryClient ?? createWidgetQueryClient(),
    [queryClient],
  );
  const value = useMemo<AiClientContextValue>(
    () => ({ client, capability, isAdmin }),
    [client, capability, isAdmin],
  );

  return (
    <QueryClientProvider client={resolvedQueryClient}>
      <AiClientContext.Provider value={value}>{children}</AiClientContext.Provider>
    </QueryClientProvider>
  );
};

export function useAiClient(): AiClientContextValue {
  const context = useContext(AiClientContext);
  if (!context) {
    throw new Error('useAiClient doit être utilisé à l’intérieur de AiClientProvider');
  }
  return context;
}

/** Cles de cache centralisees : une invalidation ratee vient toujours d'une cle divergente. */
export const aiQueryKeys = {
  conversations: ['aicw', 'conversations'] as const,
  conversation: (id: number) => ['aicw', 'conversation', id] as const,
  status: ['aicw', 'status'] as const,
};
