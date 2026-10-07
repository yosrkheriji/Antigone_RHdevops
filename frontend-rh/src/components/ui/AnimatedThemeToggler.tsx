import { useRef, useCallback } from "react";
import { flushSync } from "react-dom";
import { motion, AnimatePresence } from "framer-motion";
import { HiOutlineSun, HiOutlineMoon } from "react-icons/hi";
import { useTheme, setSharedCookie } from "../../hooks/useTheme";

export const AnimatedThemeToggler = ({ className = '' }: { className?: string }) => {
  const buttonRef = useRef<HTMLButtonElement>(null);
  const { theme, setTheme } = useTheme();
  const darkMode = theme === 'dark';

  const onToggle = useCallback(async () => {
    if (!buttonRef.current) return;

    const toggled = !darkMode;

    const applyTheme = () => {
      flushSync(() => {
        const newTheme = toggled ? 'dark' : 'light';
        // Manipuler le DOM directement pour que startViewTransition capture le changement
        document.documentElement.classList.toggle('dark', toggled);
        localStorage.setItem('theme', newTheme);
        setSharedCookie('theme', newTheme); // sync cross-port (RH ↔ Projects)
        setTheme(newTheme);
      });
    };

    if (!document.startViewTransition) {
      applyTheme();
      return;
    }

    await document.startViewTransition(applyTheme).ready;

    const { left, top, width, height } = buttonRef.current.getBoundingClientRect();
    const centerX = left + width / 2;
    const centerY = top + height / 2;
    const maxDistance = Math.hypot(
      Math.max(centerX, window.innerWidth - centerX),
      Math.max(centerY, window.innerHeight - centerY)
    );

    document.documentElement.animate(
      {
        clipPath: [
          `circle(0px at ${centerX}px ${centerY}px)`,
          `circle(${maxDistance}px at ${centerX}px ${centerY}px)`,
        ],
      },
      {
        duration: 700,
        easing: "ease-in-out",
        pseudoElement: "::view-transition-new(root)",
      }
    );
  }, [darkMode, setTheme]);

  return (
    <button
      ref={buttonRef}
      onClick={onToggle}
      aria-label="Changer le thème"
      className={`flex items-center justify-center p-2 rounded-full outline-none focus:outline-none active:outline-none focus:ring-0 cursor-pointer text-gray-500 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-800 transition-colors ${className}`}
      type="button"
    >
      <AnimatePresence mode="wait" initial={false}>
        {darkMode ? (
          <motion.span
            key="sun-icon"
            initial={{ opacity: 0, scale: 0.55, rotate: 25 }}
            animate={{ opacity: 1, scale: 1, rotate: 0 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.33 }}
          >
            <HiOutlineSun size={20} />
          </motion.span>
        ) : (
          <motion.span
            key="moon-icon"
            initial={{ opacity: 0, scale: 0.55, rotate: -25 }}
            animate={{ opacity: 1, scale: 1, rotate: 0 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.33 }}
          >
            <HiOutlineMoon size={20} />
          </motion.span>
        )}
      </AnimatePresence>
    </button>
  );
};

export default AnimatedThemeToggler;
