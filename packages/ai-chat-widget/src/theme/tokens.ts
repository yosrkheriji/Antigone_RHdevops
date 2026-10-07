/**
 * Contrat de theming du widget.
 *
 * Le widget ne contient aucune couleur en dur : il recoit ces tokens de
 * l'application hote et les projette en variables CSS `--aicw-*` sur son
 * conteneur racine. Rien ne fuit vers l'hote, rien ne le pollue.
 */
export interface ChatThemeTokens {
  /** Couleur de marque : bulles utilisateur, accents, etat actif. */
  colorPrimary: string;
  /** Texte pose sur `colorPrimary`. Doit atteindre 4.5:1 avec lui. */
  colorPrimaryText: string;
  /** Fond attenue de la marque : surlignages, puces d'outils. */
  colorPrimarySoft: string;

  /** Fond du panneau. */
  colorBackground: string;
  /** Surface des cartes et bulles assistant. */
  colorSurface: string;
  /** Surface secondaire : barre laterale, zone de saisie. */
  colorSurfaceAlt: string;
  colorBorder: string;

  colorTextPrimary: string;
  colorTextSecondary: string;
  /** Texte decoratif uniquement — sous le seuil WCAG AA sur la charte Antigone. */
  colorTextMuted: string;

  colorSuccess: string;
  colorWarning: string;
  colorDanger: string;

  fontFamily: string;
  radiusMd: string;
  radiusLg: string;
  shadow: string;
}

export interface ChatTheme {
  light: ChatThemeTokens;
  dark: ChatThemeTokens;
  /**
   * `auto` suit `prefers-color-scheme`. Les applications Antigone gerant deja leur
   * propre bascule, elles passent `light` ou `dark` explicitement.
   */
  mode?: 'light' | 'dark' | 'auto';
}

/**
 * Tokens par defaut, derives de `THEME_AUDIT.md`.
 *
 * Les teintes de texte pour `success` et `danger` sont volontairement plus foncees
 * que les `*-500` de la charte : ces dernieres n'atteignent pas 4.5:1 sur fond
 * blanc. Meme raison pour `colorTextMuted`, cantonne au decoratif.
 */
export const ANTIGONE_LIGHT: ChatThemeTokens = {
  colorPrimary: '#683b77',
  colorPrimaryText: '#ffffff',
  colorPrimarySoft: 'rgba(104, 59, 119, 0.10)',

  colorBackground: '#f5f4f1',
  colorSurface: '#ffffff',
  colorSurfaceAlt: '#faf9f7',
  colorBorder: '#e8e6e0',

  colorTextPrimary: '#1a1814',
  colorTextSecondary: '#5c5a55',
  colorTextMuted: '#9c9a94',

  colorSuccess: '#027a48',
  colorWarning: '#c4320a',
  colorDanger: '#b42318',

  fontFamily: 'var(--app-font, "Inter", sans-serif)',
  radiusMd: '10px',
  radiusLg: '16px',
  shadow: '0 4px 16px rgba(0, 0, 0, 0.08)',
};

export const ANTIGONE_DARK: ChatThemeTokens = {
  colorPrimary: '#ab78c3',
  colorPrimaryText: '#150b1a',
  colorPrimarySoft: 'rgba(171, 120, 195, 0.16)',

  colorBackground: '#0c111d',
  colorSurface: '#1a2231',
  colorSurfaceAlt: '#111827',
  colorBorder: '#344054',

  colorTextPrimary: '#f2f4f7',
  colorTextSecondary: '#98a2b3',
  colorTextMuted: '#667085',

  colorSuccess: '#32d583',
  colorWarning: '#fd853a',
  colorDanger: '#f97066',

  fontFamily: 'var(--app-font, "Inter", sans-serif)',
  radiusMd: '10px',
  radiusLg: '16px',
  shadow: '0 8px 28px rgba(0, 0, 0, 0.45)',
};

export const DEFAULT_THEME: ChatTheme = {
  light: ANTIGONE_LIGHT,
  dark: ANTIGONE_DARK,
  mode: 'light',
};

/** Nom de la variable CSS correspondant a un token. */
const CSS_VARIABLE: Record<keyof ChatThemeTokens, string> = {
  colorPrimary: '--aicw-primary',
  colorPrimaryText: '--aicw-primary-text',
  colorPrimarySoft: '--aicw-primary-soft',
  colorBackground: '--aicw-bg',
  colorSurface: '--aicw-surface',
  colorSurfaceAlt: '--aicw-surface-alt',
  colorBorder: '--aicw-border',
  colorTextPrimary: '--aicw-text',
  colorTextSecondary: '--aicw-text-secondary',
  colorTextMuted: '--aicw-text-muted',
  colorSuccess: '--aicw-success',
  colorWarning: '--aicw-warning',
  colorDanger: '--aicw-danger',
  fontFamily: '--aicw-font',
  radiusMd: '--aicw-radius-md',
  radiusLg: '--aicw-radius-lg',
  shadow: '--aicw-shadow',
};

/** Projette les tokens en style inline, applique sur le conteneur racine du widget. */
export function toCssVariables(tokens: ChatThemeTokens): React.CSSProperties {
  const style: Record<string, string> = {};
  for (const [key, variable] of Object.entries(CSS_VARIABLE)) {
    style[variable] = tokens[key as keyof ChatThemeTokens];
  }
  return style as React.CSSProperties;
}

/**
 * Fusionne une surcharge partielle avec les valeurs par defaut.
 *
 * Permet a une application de ne redefinir qu'un accent sans avoir a repeter les
 * dix-sept tokens.
 */
export function mergeTheme(
  override?: {
    light?: Partial<ChatThemeTokens>;
    dark?: Partial<ChatThemeTokens>;
    mode?: ChatTheme['mode'];
  },
): ChatTheme {
  return {
    light: { ...ANTIGONE_LIGHT, ...override?.light },
    dark: { ...ANTIGONE_DARK, ...override?.dark },
    mode: override?.mode ?? 'light',
  };
}
