import React, { useState, useRef, useEffect } from 'react';
import { motion, useMotionValue, useTransform, AnimatePresence } from 'framer-motion';

interface SlideToUnlockProps {
  children: React.ReactNode;
  onUnlock: () => void;
  sliderText?: string;
  unlockedContent: React.ReactNode;
  className?: string;
  shimmer?: boolean;
}

export const SlideToUnlock = ({
  children,
  onUnlock,
  sliderText = 'Glissez pour ouvrir votre cadeau',
  unlockedContent,
  className = '',
  shimmer = true,
}: SlideToUnlockProps) => {
  const [unlocked, setUnlocked] = useState(false);
  const [dragConstraint, setDragConstraint] = useState(0);
  const x = useMotionValue(0);

  const sliderRef = useRef<HTMLDivElement>(null);
  const handleRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const sliderWidth = sliderRef.current?.offsetWidth || 0;
    const handleWidth = handleRef.current?.offsetWidth || 0;
    setDragConstraint(sliderWidth - handleWidth);
  }, []);

  const onDragEnd = (_event: unknown, info: { offset: { x: number } }) => {
    if (info.offset.x > dragConstraint * 0.8) {
      setUnlocked(true);
      onUnlock();
    } else {
      x.set(0);
    }
  };

  const textOpacity = useTransform(x, [0, 50], [1, 0]);

  return (
    <div
      className={`relative w-full max-w-sm overflow-hidden rounded-3xl border border-gray-100 bg-white p-7 text-gray-900 shadow-2xl dark:border-gray-700 dark:bg-gray-900 dark:text-white ${className}`}
    >
      {children}

      <AnimatePresence mode="wait">
        {!unlocked ? (
          <motion.div
            key="slider"
            initial={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 20 }}
            transition={{ duration: 0.3 }}
            className="relative mt-6"
          >
            <div ref={sliderRef} className="relative h-14 w-full rounded-full bg-gray-100 dark:bg-gray-800">
              <motion.div
                ref={handleRef}
                drag="x"
                dragConstraints={{ left: 0, right: dragConstraint }}
                dragElastic={0.1}
                style={{ x }}
                onDragEnd={onDragEnd}
                className="absolute left-0 top-0 z-10 flex h-14 w-14 cursor-grab items-center justify-center rounded-full bg-gradient-to-br from-brand-400 to-brand-600 shadow-lg active:cursor-grabbing"
                whileTap={{ scale: 0.95 }}
              >
                <ChevronRightIcon className="h-6 w-6 text-white" />
              </motion.div>

              <motion.span
                style={{ opacity: textOpacity }}
                className={
                  'absolute inset-0 flex items-center justify-center pl-16 pr-4 text-sm font-medium text-gray-400' +
                  (shimmer
                    ? ' animate-shimmer bg-[linear-gradient(110deg,#9ca3af,45%,#e5e7eb,55%,#9ca3af)] bg-[length:200%_100%] bg-clip-text text-transparent'
                    : '')
                }
              >
                {sliderText}
              </motion.span>
            </div>
          </motion.div>
        ) : (
          <motion.div
            key="unlocked"
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.1 }}
          >
            {unlockedContent}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

const ChevronRightIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg
    {...props}
    xmlns="http://www.w3.org/2000/svg"
    width="24"
    height="24"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
  >
    <path d="m9 18 6-6-6-6" />
  </svg>
);
