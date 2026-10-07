import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { aiQueryKeys, useAiClient } from './useAiClient';
import type { ConversationDetail, ConversationSummary, Page } from '../api/types';

/**
 * CRUD des conversations.
 *
 * Les mutations sont optimistes : renommer ou epingler doit etre instantane a
 * l'ecran, un aller-retour serveur sur une action aussi anodine se voit. Chaque
 * mutation restaure l'etat precedent en cas d'echec, pour ne jamais laisser l'UI
 * affirmer quelque chose que le serveur a refuse.
 */

export function useConversations() {
  const { client } = useAiClient();

  return useQuery({
    queryKey: aiQueryKeys.conversations,
    queryFn: () => client.listConversations({ size: 50, sort: 'updatedAt', direction: 'DESC' }),
    select: (page: Page<ConversationSummary>) => sortConversations(page.content),
  });
}

/** Epinglees d'abord, puis les plus recemment mises a jour. */
function sortConversations(items: ConversationSummary[]): ConversationSummary[] {
  return [...items].sort((a, b) => {
    if (a.pinned !== b.pinned) return a.pinned ? -1 : 1;
    return Date.parse(b.updatedAt) - Date.parse(a.updatedAt);
  });
}

export function useConversation(id: number | null) {
  const { client } = useAiClient();

  return useQuery({
    queryKey: id ? aiQueryKeys.conversation(id) : ['aicw', 'conversation', 'none'],
    queryFn: () => client.getConversation(id as number),
    enabled: id !== null,
  });
}

export function useCreateConversation() {
  const { client } = useAiClient();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (title?: string) => client.createConversation(title),
    onSuccess: (created) => {
      queryClient.setQueryData<Page<ConversationSummary>>(aiQueryKeys.conversations, (previous) =>
        previous
          ? { ...previous, content: [created, ...previous.content], totalElements: previous.totalElements + 1 }
          : { content: [created], totalElements: 1, totalPages: 1, number: 0, size: 50 },
      );
    },
  });
}

export function useUpdateConversation() {
  const { client } = useAiClient();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, changes }: { id: number; changes: { title?: string; pinned?: boolean } }) =>
      client.updateConversation(id, changes),

    onMutate: async ({ id, changes }) => {
      await queryClient.cancelQueries({ queryKey: aiQueryKeys.conversations });
      const snapshot = queryClient.getQueryData<Page<ConversationSummary>>(aiQueryKeys.conversations);

      queryClient.setQueryData<Page<ConversationSummary>>(aiQueryKeys.conversations, (previous) =>
        previous
          ? {
              ...previous,
              content: previous.content.map((item) =>
                item.id === id ? { ...item, ...changes } : item,
              ),
            }
          : previous,
      );
      // Le detail porte aussi le titre : le laisser obsolete ferait clignoter
      // l'en-tete de la conversation ouverte.
      queryClient.setQueryData<ConversationDetail>(aiQueryKeys.conversation(id), (previous) =>
        previous ? { ...previous, ...changes } : previous,
      );

      return { snapshot };
    },

    onError: (_error, _variables, context) => {
      if (context?.snapshot) {
        queryClient.setQueryData(aiQueryKeys.conversations, context.snapshot);
      }
    },

    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: aiQueryKeys.conversations });
    },
  });
}

export function useDeleteConversation() {
  const { client } = useAiClient();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => client.deleteConversation(id),

    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: aiQueryKeys.conversations });
      const snapshot = queryClient.getQueryData<Page<ConversationSummary>>(aiQueryKeys.conversations);

      // Suppression logique cote serveur, mais elle doit disparaitre de l'ecran
      // immediatement : l'utilisateur vient de confirmer, il ne doit plus la voir.
      queryClient.setQueryData<Page<ConversationSummary>>(aiQueryKeys.conversations, (previous) =>
        previous
          ? {
              ...previous,
              content: previous.content.filter((item) => item.id !== id),
              totalElements: Math.max(0, previous.totalElements - 1),
            }
          : previous,
      );

      return { snapshot };
    },

    onError: (_error, _id, context) => {
      if (context?.snapshot) {
        queryClient.setQueryData(aiQueryKeys.conversations, context.snapshot);
      }
    },

    onSuccess: (_data, id) => {
      queryClient.removeQueries({ queryKey: aiQueryKeys.conversation(id) });
    },
  });
}
