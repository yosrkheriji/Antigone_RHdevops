import React from 'react';

/**
 * Icones en SVG inline.
 *
 * Pas de dependance sur une librairie d'icones : les deux applications hotes
 * embarquent react-icons, mais un package partage ne peut pas presumer de ce que
 * la prochaine embarquera. `currentColor` partout, donc elles suivent
 * automatiquement le theme sans reglage.
 */
type IconProps = {
  size?: number;
  className?: string;
  'aria-hidden'?: boolean;
};

const base = (size: number) => ({
  width: size,
  height: size,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 2,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
  'aria-hidden': true,
  focusable: false,
});

export const SparkleIcon: React.FC<IconProps> = ({ size = 16, className }) => (
  <svg {...base(size)} className={className}>
    <path d="M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9L12 3z" />
    <path d="M18 15l.8 2.2L21 18l-2.2.8L18 21l-.8-2.2L15 18l2.2-.8L18 15z" />
  </svg>
);

export const SendIcon: React.FC<IconProps> = ({ size = 16, className }) => (
  <svg {...base(size)} className={className}>
    <path d="M4 12l16-8-6 16-2.5-6.5L4 12z" />
  </svg>
);

export const StopIcon: React.FC<IconProps> = ({ size = 14, className }) => (
  <svg {...base(size)} className={className}>
    <rect x="6" y="6" width="12" height="12" rx="2" fill="currentColor" stroke="none" />
  </svg>
);

export const CloseIcon: React.FC<IconProps> = ({ size = 16, className }) => (
  <svg {...base(size)} className={className}>
    <path d="M6 6l12 12M18 6L6 18" />
  </svg>
);

export const ExpandIcon: React.FC<IconProps> = ({ size = 16, className }) => (
  <svg {...base(size)} className={className}>
    <path d="M4 10V4h6M20 14v6h-6M4 4l7 7M20 20l-7-7" />
  </svg>
);

export const CollapseIcon: React.FC<IconProps> = ({ size = 16, className }) => (
  <svg {...base(size)} className={className}>
    <path d="M10 4v6H4M14 20v-6h6M4 10l7-7M20 14l-7 7" />
  </svg>
);

export const HistoryIcon: React.FC<IconProps> = ({ size = 16, className }) => (
  <svg {...base(size)} className={className}>
    <path d="M3 12a9 9 0 1 0 3-6.7L3 8" />
    <path d="M3 4v4h4M12 7v5l3 2" />
  </svg>
);

export const PlusIcon: React.FC<IconProps> = ({ size = 14, className }) => (
  <svg {...base(size)} className={className}>
    <path d="M12 5v14M5 12h14" />
  </svg>
);

export const PinIcon: React.FC<IconProps> = ({ size = 13, className }) => (
  <svg {...base(size)} className={className}>
    <path d="M9 3h6l-1 6 4 4H6l4-4-1-6z" />
    <path d="M12 13v8" />
  </svg>
);

export const PencilIcon: React.FC<IconProps> = ({ size = 13, className }) => (
  <svg {...base(size)} className={className}>
    <path d="M4 20h4L19 9a2.8 2.8 0 0 0-4-4L4 16v4z" />
  </svg>
);

export const TrashIcon: React.FC<IconProps> = ({ size = 13, className }) => (
  <svg {...base(size)} className={className}>
    <path d="M4 7h16M9 7V5h6v2M6 7l1 13h10l1-13" />
  </svg>
);

export const CheckIcon: React.FC<IconProps> = ({ size = 12, className }) => (
  <svg {...base(size)} className={className}>
    <path d="M5 13l4 4L19 7" />
  </svg>
);

export const AlertIcon: React.FC<IconProps> = ({ size = 12, className }) => (
  <svg {...base(size)} className={className}>
    <path d="M12 8v5M12 17h.01" />
    <circle cx="12" cy="12" r="9" />
  </svg>
);

export const CopyIcon: React.FC<IconProps> = ({ size = 13, className }) => (
  <svg {...base(size)} className={className}>
    <rect x="9" y="9" width="11" height="11" rx="2" />
    <path d="M5 15V5a2 2 0 0 1 2-2h8" />
  </svg>
);

export const MailIcon: React.FC<IconProps> = ({ size = 13, className }) => (
  <svg {...base(size)} className={className}>
    <rect x="3" y="5" width="18" height="14" rx="2" />
    <path d="M3 7l9 6 9-6" />
  </svg>
);

export const ArrowDownIcon: React.FC<IconProps> = ({ size = 13, className }) => (
  <svg {...base(size)} className={className}>
    <path d="M12 5v14M6 13l6 6 6-6" />
  </svg>
);

export const DocumentIcon: React.FC<IconProps> = ({ size = 15, className }) => (
  <svg {...base(size)} className={className}>
    <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8l-5-5z" />
    <path d="M14 3v5h5M9 13h6M9 17h4" />
  </svg>
);

export const ShareIcon: React.FC<IconProps> = ({ size = 13, className }) => (
  <svg {...base(size)} className={className}>
    <circle cx="18" cy="5" r="3" />
    <circle cx="6" cy="12" r="3" />
    <circle cx="18" cy="19" r="3" />
    <path d="M8.6 10.5l6.8-3.9M8.6 13.5l6.8 3.9" />
  </svg>
);

export const CalendarIcon: React.FC<IconProps> = ({ size = 13, className }) => (
  <svg {...base(size)} className={className}>
    <rect x="3" y="5" width="18" height="16" rx="2" />
    <path d="M3 10h18M8 3v4M16 3v4" />
  </svg>
);

export const ClockIcon: React.FC<IconProps> = ({ size = 13, className }) => (
  <svg {...base(size)} className={className}>
    <circle cx="12" cy="12" r="9" />
    <path d="M12 7v5l3.5 2" />
  </svg>
);
