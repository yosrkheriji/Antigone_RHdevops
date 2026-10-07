import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { DEFAULT_THEME, toCssVariables, type ChatTheme, type ChatThemeTokens } from './tokens';

interface ChatThemeContextValue {
  tokens: ChatThemeTokens;
  resolvedMode: 'light' | 'dark';
}

const ChatThemeContext = createContext<ChatThemeContextValue | null>(null);

/**
 * Resout `auto` en suivant la preference systeme, et reste synchronise si
 * l'utilisateur la change pendant la session.
 *
 * Les applications Antigone passent `light`/`dark` explicitement — elles gerent
 * deja leur propre bascule et la partagent entre ports par cookie. Le mode `auto`
 * n'existe que pour un hote qui n'aurait pas ce mecanisme.
 */
function useResolvedMode(mode: ChatTheme['mode']): 'light' | 'dark' {
  const [systemMode, setSystemMode] = useState<'light' | 'dark'>(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return 'light';
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  });

  useEffect(() => {
    if (mode !== 'auto' || typeof window === 'undefined' || !window.matchMedia) return;
    const query = window.matchMedia('(prefers-color-scheme: dark)');
    const onChange = (event: MediaQueryListEvent) => setSystemMode(event.matches ? 'dark' : 'light');
    query.addEventListener('change', onChange);
    return () => query.removeEventListener('change', onChange);
  }, [mode]);

  if (mode === 'auto') return systemMode;
  return mode ?? 'light';
}

export interface ChatThemeProviderProps {
  theme?: ChatTheme;
  className?: string;
  style?: React.CSSProperties;
  children: React.ReactNode;
}

/**
 * Pose les variables CSS du widget sur son conteneur racine.
 *
 * Scopees sur ce conteneur et non sur `:root` : c'est ce qui garantit qu'aucune
 * variable du widget ne fuit vers l'application, et qu'aucune variable de
 * l'application ne le redefinisse par accident.
 */
export const ChatThemeProvider: React.FC<ChatThemeProviderProps> = ({
  theme = DEFAULT_THEME,
  className,
  style,
  children,
}) => {
  const resolvedMode = useResolvedMode(theme.mode);
  const tokens = resolvedMode === 'dark' ? theme.dark : theme.light;

  const value = useMemo<ChatThemeContextValue>(
    () => ({ tokens, resolvedMode }),
    [tokens, resolvedMode],
  );

  return (
    <ChatThemeContext.Provider value={value}>
      <div
        className={['aicw-root', `aicw-mode-${resolvedMode}`, className].filter(Boolean).join(' ')}
        style={{ ...toCssVariables(tokens), ...style }}
      >
        {children}
      </div>
    </ChatThemeContext.Provider>
  );
};

export function useChatTheme(): ChatThemeContextValue {
  const context = useContext(ChatThemeContext);
  if (!context) {
    throw new Error('useChatTheme doit être utilisé à l’intérieur de ChatThemeProvider');
  }
  return context;
}
