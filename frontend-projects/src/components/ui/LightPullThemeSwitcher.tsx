import { motion } from 'framer-motion';
import { useRef, useCallback } from 'react';
import { flushSync } from 'react-dom';
import { useTheme, setSharedCookie } from '../../hooks/useTheme';

export function LightPullThemeSwitcher() {
  const { theme, setTheme } = useTheme();
  const ballRef = useRef<HTMLDivElement>(null);

  const toggle = useCallback(async () => {
    const newTheme = theme === 'dark' ? 'light' : 'dark';

    const applyTheme = () => {
      flushSync(() => {
        document.documentElement.classList.toggle('dark', newTheme === 'dark');
        localStorage.setItem('theme', newTheme);
        setSharedCookie('theme', newTheme);
        setTheme(newTheme);
      });
    };

    if (!document.startViewTransition) {
      applyTheme();
      return;
    }

    const el = ballRef.current;
    const rect = el?.getBoundingClientRect();
    const centerX = rect ? rect.left + rect.width / 2 : window.innerWidth / 2;
    const centerY = rect ? rect.top + rect.height / 2 : 0;
    const maxDistance = Math.hypot(
      Math.max(centerX, window.innerWidth - centerX),
      Math.max(centerY, window.innerHeight - centerY)
    );

    await document.startViewTransition(applyTheme).ready;

    document.documentElement.animate(
      {
        clipPath: [
          `circle(0px at ${centerX}px ${centerY}px)`,
          `circle(${maxDistance}px at ${centerX}px ${centerY}px)`,
        ],
      },
      {
        duration: 700,
        easing: 'ease-in-out',
        pseudoElement: '::view-transition-new(root)',
      }
    );
  }, [theme, setTheme]);

  const isDark = theme === 'dark';

  return (
    <div
      style={{
        position: 'relative',
        paddingTop: '64px',
        paddingBottom: '12px',
        paddingLeft: '24px',
        paddingRight: '24px',
        overflow: 'hidden',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
      }}
    >
      <motion.div
        ref={ballRef}
        drag="y"
        dragDirectionLock
        onDragEnd={(_e, info) => {
          if (info.offset.y > 0) toggle();
        }}
        dragConstraints={{ top: 0, right: 0, bottom: 0, left: 0 }}
        dragTransition={{ bounceStiffness: 500, bounceDamping: 15 }}
        dragElastic={0.075}
        whileDrag={{ cursor: 'grabbing' }}
        onClick={toggle}
        style={{
          position: 'relative',
          width: '32px',
          height: '32px',
          borderRadius: '9999px',
          cursor: 'grab',
          flexShrink: 0,
          background: isDark
            ? 'radial-gradient(circle at center, #4b5563, #1f2937, #000)'
            : 'radial-gradient(circle at center, #facc15, #fcd34d, #fef9c3)',
          boxShadow: isDark
            ? '0 0 20px 6px rgba(31,41,55,0.7)'
            : '0 0 20px 8px rgba(250,204,21,0.5)',
        }}
      >
        {/* Cord going up */}
        <div
          style={{
            position: 'absolute',
            bottom: '100%',
            left: '50%',
            transform: 'translateX(-50%)',
            width: '2px',
            height: '9999px',
            background: isDark ? '#525252' : '#d4d4d4',
          }}
        />
      </motion.div>
    </div>
  );
}
