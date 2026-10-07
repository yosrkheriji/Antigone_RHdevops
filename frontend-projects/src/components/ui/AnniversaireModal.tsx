import React, { useEffect, useRef, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { createPortal } from 'react-dom';
import { SlideToUnlock } from './reward-card';

interface AnniversaireModalProps {
  prenom: string;
  annees: number;
  onClose: () => void;
}

const CONFETTI_COLORS = ['#E86A2E', '#7c3aed', '#059669', '#2563eb', '#f59e0b', '#e11d48', '#06b6d4'];

interface Particle {
  id: number;
  x: number;
  color: string;
  delay: number;
  duration: number;
  rotate: number;
  size: number;
}

const Confetti: React.FC<{ active: boolean }> = ({ active }) => {
  const particles: Particle[] = Array.from({ length: 60 }, (_, i) => ({
    id: i,
    x: Math.random() * 100,
    color: CONFETTI_COLORS[i % CONFETTI_COLORS.length],
    delay: Math.random() * 0.6,
    duration: 1.8 + Math.random() * 1.2,
    rotate: Math.random() * 720 - 360,
    size: 6 + Math.random() * 8,
  }));

  if (!active) return null;
  return (
    <div className="pointer-events-none fixed inset-0 z-[200] overflow-hidden">
      {particles.map((p) => (
        <motion.div
          key={p.id}
          className="absolute top-0 rounded-sm"
          style={{ left: `${p.x}%`, width: p.size, height: p.size, backgroundColor: p.color }}
          initial={{ y: -20, opacity: 1, rotate: 0 }}
          animate={{ y: '110vh', opacity: 0, rotate: p.rotate }}
          transition={{ duration: p.duration, delay: p.delay, ease: 'easeIn' }}
        />
      ))}
    </div>
  );
};

function getAnniversaireTexte(prenom: string, annees: number): { titre: string; corps: string } {
  if (annees === 1) {
    return {
      titre: `🎉 Joyeux 1er anniversaire, ${prenom} !`,
      corps:
        'Un an déjà ! Vous avez rejoint notre équipe il y a tout juste un an, et quelle belle année ce fut. ' +
        "Votre fraîcheur, votre investissement et votre bonne humeur ont laissé une vraie empreinte parmi nous. " +
        "Bienvenue dans le club des anciens — il n'en faut pas beaucoup plus pour mériter le titre ! 😄",
    };
  }
  if (annees < 5) {
    return {
      titre: `🎉 ${annees} ans ensemble, ${prenom} !`,
      corps:
        `Voilà ${annees} ans que vous faites partie intégrante de notre équipe. ` +
        "Chaque jour, vous apportez votre talent, votre sérieux et cette énergie qui rend notre travail plus agréable. " +
        "Merci d'être là, et voici quelques années de plus à écrire ensemble ! ✨",
    };
  }
  if (annees < 10) {
    return {
      titre: `🏆 ${annees} ans de fidélité, ${prenom} !`,
      corps:
        `${annees} années de présence, d'engagement et de passion partagée — c'est exceptionnel. ` +
        "Vous êtes un pilier de cette équipe, une référence pour les plus jeunes et une source de confiance pour tous. " +
        "Aujourd'hui, on célèbre non seulement vos années de service, mais surtout ce que vous êtes. 🌟",
    };
  }
  return {
    titre: `🥇 ${annees} ans — Une légende parmi nous, ${prenom} !`,
    corps:
      `${annees} ans ! Vous avez traversé les saisons, les projets, les défis, et vous êtes toujours là, solide comme un roc. ` +
      "Peu de personnes peuvent se targuer d'une telle fidélité et d'un tel engagement. " +
      "Vous n'êtes pas seulement un collaborateur précieux — vous faites partie de l'histoire de cette entreprise. 👑",
  };
}

const AnniversaireModal: React.FC<AnniversaireModalProps> = ({ prenom, annees, onClose }) => {
  const { titre, corps } = getAnniversaireTexte(prenom, annees);
  const [confetti, setConfetti] = useState(false);
  const [unlocked, setUnlocked] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    timerRef.current = setTimeout(() => setConfetti(true), 300);
    return () => { if (timerRef.current) clearTimeout(timerRef.current); };
  }, []);

  const handleUnlock = () => {
    setUnlocked(true);
    setConfetti(true);
    timerRef.current = setTimeout(onClose, 6000);
  };

  const UnlockedContent = () => (
    <div className="mt-2 flex flex-col items-center gap-3 text-center">
      <motion.div
        initial={{ scale: 0 }}
        animate={{ scale: 1 }}
        transition={{ type: 'spring', stiffness: 300, damping: 15 }}
        className="text-6xl"
      >
        🎊
      </motion.div>
      <div className="rounded-2xl bg-gradient-to-br from-brand-50 to-purple-50 px-5 py-4 dark:from-brand-900/20 dark:to-purple-900/20">
        <p className="text-lg font-bold text-brand-600 dark:text-brand-400">
          {annees} {annees === 1 ? 'an' : 'ans'} ensemble — Merci !
        </p>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          La fenêtre se fermera dans quelques secondes…
        </p>
      </div>
      <button
        onClick={onClose}
        className="mt-2 rounded-full bg-brand-500 px-6 py-2.5 text-sm font-bold text-white shadow-md transition-all hover:bg-brand-600 hover:shadow-lg active:scale-95"
      >
        Merci ! 🙏
      </button>
    </div>
  );

  return createPortal(
    <>
      <Confetti active={confetti} />
      <AnimatePresence>
        <motion.div
          className="fixed inset-0 z-[150] flex items-center justify-center p-4"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <motion.div
            className="absolute inset-0 bg-gray-900/60 backdrop-blur-sm"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            onClick={!unlocked ? onClose : undefined}
          />
          <motion.div
            initial={{ scale: 0.85, opacity: 0, y: 30 }}
            animate={{ scale: 1, opacity: 1, y: 0 }}
            transition={{ type: 'spring', stiffness: 280, damping: 22, delay: 0.1 }}
            className="relative z-10 flex flex-col items-center"
          >
            <SlideToUnlock
              onUnlock={handleUnlock}
              sliderText="✨ Glissez pour ouvrir votre cadeau"
              shimmer={true}
              unlockedContent={<UnlockedContent />}
              className="max-w-sm w-full"
            >
              <div className="flex flex-col items-center gap-4 pt-3 text-center">
                <motion.div
                  animate={{ rotate: [0, -8, 8, -8, 8, 0] }}
                  transition={{ duration: 1.2, delay: 0.8, repeat: Infinity, repeatDelay: 3 }}
                  className="flex h-20 w-20 items-center justify-center rounded-full bg-gradient-to-br from-brand-100 to-purple-100 text-4xl shadow-inner dark:from-brand-900/30 dark:to-purple-900/30"
                >
                  🎂
                </motion.div>
                <div>
                  <h2 className="text-xl font-extrabold leading-snug tracking-tight text-gray-900 dark:text-white">
                    {titre}
                  </h2>
                  <span className="mt-2 inline-block rounded-full bg-gradient-to-r from-brand-500 to-purple-500 px-4 py-1 text-xs font-bold tracking-wider text-white shadow">
                    {annees} {annees === 1 ? 'année' : 'années'} de collaboration
                  </span>
                </div>
                <p className="text-sm leading-relaxed text-gray-500 dark:text-gray-400">{corps}</p>
              </div>
            </SlideToUnlock>
            {!unlocked && (
              <button
                onClick={onClose}
                className="mt-4 text-xs text-white/60 transition-colors hover:text-white/90"
              >
                Ignorer pour aujourd'hui
              </button>
            )}
          </motion.div>
        </motion.div>
      </AnimatePresence>
    </>,
    document.body
  );
};

export default AnniversaireModal;
