import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { authService } from '../api/authService';
import { LightPullThemeSwitcher } from '../components/ui/LightPullThemeSwitcher';

// ── Envelope character panel ──────────────────────────────────────────────────

function EnvelopePanel({ isTyping, email, sent }: {
  isTyping: boolean;
  email: string;
  sent: boolean;
}) {
  const [blink, setBlink] = useState(false);
  const [bounce, setBounce] = useState(false);

  // Blink loop
  useEffect(() => {
    const loop = (): ReturnType<typeof setTimeout> => {
      const t = setTimeout(() => {
        setBlink(true);
        setTimeout(() => { setBlink(false); loop(); }, 150);
      }, Math.random() * 4000 + 2500);
      return t;
    };
    const t = loop();
    return () => clearTimeout(t);
  }, []);

  // Bounce when typing
  useEffect(() => {
    if (!isTyping || email.length === 0) return;
    setBounce(true);
    const t = setTimeout(() => setBounce(false), 600);
    return () => clearTimeout(t);
  }, [isTyping, email.length]);

  const flapOpen = isTyping && email.length > 0;

  return (
    <div className="relative hidden lg:flex flex-col items-center justify-center overflow-hidden">
      <div className="relative" style={{ width: '440px', height: '400px' }}>

        {/* Floating decorative dots */}
        {[
          { x: 30, y: 50, size: 10, color: '#F59E0B', delay: 0 },
          { x: 380, y: 100, size: 14, color: '#e8621a', delay: 0.4 },
          { x: 50, y: 300, size: 7, color: '#7C3AED', delay: 0.8 },
          { x: 370, y: 320, size: 11, color: '#10b981', delay: 1.2 },
          { x: 200, y: 30, size: 6, color: '#e8621a', delay: 0.6 },
        ].map((dot, i) => (
          <motion.div
            key={i}
            style={{
              position: 'absolute', left: dot.x, top: dot.y,
              width: dot.size, height: dot.size,
              borderRadius: '50%', backgroundColor: dot.color,
              opacity: 0.7,
            }}
            animate={{ y: [0, -10, 0] }}
            transition={{ duration: 2.4, repeat: Infinity, delay: dot.delay, ease: 'easeInOut' }}
          />
        ))}

        {/* Paper plane — flies away when sent */}
        <AnimatePresence>
          {sent && (
            <motion.div
              key="plane"
              initial={{ x: 160, y: 160, opacity: 1, rotate: -10, scale: 1 }}
              animate={{ x: 420, y: -80, opacity: 0, rotate: 25, scale: 0.6 }}
              transition={{ duration: 1.4, ease: [0.25, 0.46, 0.45, 0.94] }}
              style={{ position: 'absolute', zIndex: 30 }}
            >
              <svg width="56" height="56" viewBox="0 0 24 24" fill="none">
                <path d="M22 2L11 13" stroke="#e8621a" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
                <path d="M22 2L15 22L11 13L2 9L22 2Z" stroke="#e8621a" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" fill="#e8621a" fillOpacity="0.18" />
              </svg>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Envelope body */}
        <motion.div
          animate={bounce ? { y: [0, -18, 4, -8, 0] } : { y: 0 }}
          transition={{ duration: 0.55, ease: 'easeOut' }}
          style={{ position: 'absolute', left: '70px', top: '90px' }}
        >
          <div style={{ position: 'relative', width: '300px', height: '210px' }}>

            {/* Envelope background */}
            <div style={{
              width: '300px', height: '210px',
              backgroundColor: 'var(--surface, #f9f9fb)',
              borderRadius: '14px',
              border: '3px solid #7C3AED',
              position: 'relative',
              overflow: 'hidden',
              boxShadow: '0 12px 40px rgba(124,58,237,0.18)',
            }}>
              {/* Bottom-left diagonal */}
              <div style={{
                position: 'absolute', bottom: 0, left: 0,
                width: 0, height: 0, borderStyle: 'solid',
                borderWidth: '95px 0 0 150px',
                borderColor: 'transparent transparent transparent rgba(124,58,237,0.12)',
              }} />
              {/* Bottom-right diagonal */}
              <div style={{
                position: 'absolute', bottom: 0, right: 0,
                width: 0, height: 0, borderStyle: 'solid',
                borderWidth: '95px 150px 0 0',
                borderColor: 'transparent rgba(124,58,237,0.12) transparent transparent',
              }} />

              {/* Letter peeking out when typing */}
              <AnimatePresence>
                {flapOpen && (
                  <motion.div
                    key="letter"
                    initial={{ y: 80, opacity: 0 }}
                    animate={{ y: 18, opacity: 1 }}
                    exit={{ y: 80, opacity: 0 }}
                    transition={{ duration: 0.45, ease: 'easeOut' }}
                    style={{
                      position: 'absolute', left: '30px', right: '30px',
                      background: 'white',
                      borderRadius: '8px',
                      padding: '14px',
                      boxShadow: '0 4px 14px rgba(0,0,0,0.1)',
                      border: '1px solid #e9d5ff',
                      zIndex: 5,
                    }}
                  >
                    <div style={{ height: '7px', background: '#7C3AED', borderRadius: '4px', marginBottom: '7px', opacity: 0.25 }} />
                    <div style={{ height: '5px', background: '#a78bfa', borderRadius: '3px', marginBottom: '6px', width: '75%', opacity: 0.18 }} />
                    <div style={{ height: '5px', background: '#a78bfa', borderRadius: '3px', width: '55%', opacity: 0.15 }} />
                    <div style={{ marginTop: '10px', fontSize: '11px', color: '#7C3AED', fontFamily: 'monospace', fontWeight: 700, letterSpacing: '0.02em' }}>
                      {email.slice(0, 22)}
                      <motion.span
                        animate={{ opacity: [1, 0, 1] }}
                        transition={{ duration: 0.8, repeat: Infinity }}
                      >|</motion.span>
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>

              {/* Face — eyes */}
              <div style={{
                position: 'absolute',
                top: flapOpen ? '128px' : '105px',
                left: '50%', transform: 'translateX(-50%)',
                display: 'flex', gap: '22px',
                transition: 'top 0.4s ease',
                zIndex: 6,
              }}>
                {[0, 1].map(i => (
                  <div key={i} style={{
                    width: '20px',
                    height: blink ? '3px' : '20px',
                    backgroundColor: '#7C3AED',
                    borderRadius: '50%',
                    transition: 'height 0.1s ease',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                  }}>
                    {!blink && (
                      <div style={{
                        width: '8px', height: '8px',
                        backgroundColor: 'var(--bg, #ffffff)',
                        borderRadius: '50%',
                        transform: isTyping ? 'translateX(3px) translateY(-3px)' : 'translate(0,0)',
                        transition: 'transform 0.25s ease',
                      }} />
                    )}
                  </div>
                ))}
              </div>

              {/* Face — mouth */}
              <div style={{
                position: 'absolute',
                top: flapOpen ? '157px' : '133px',
                left: '50%', transform: 'translateX(-50%)',
                width: bounce ? '34px' : isTyping ? '22px' : '26px',
                height: bounce ? '16px' : isTyping ? '9px' : '7px',
                borderRadius: bounce ? '0 0 17px 17px' : isTyping ? '0 0 11px 11px' : '0 0 7px 7px',
                backgroundColor: '#7C3AED',
                transition: 'all 0.3s ease',
                zIndex: 6,
              }} />
            </div>

            {/* Envelope flap — opens when typing */}
            <motion.div
              animate={{ rotateX: flapOpen ? -150 : 0 }}
              transition={{ duration: 0.5, ease: 'easeInOut' }}
              style={{
                position: 'absolute', top: 0, left: 0,
                width: 0, height: 0, borderStyle: 'solid',
                borderWidth: '0 150px 105px 150px',
                borderColor: 'transparent transparent #7C3AED transparent',
                transformOrigin: 'top center',
                zIndex: 10,
              }}
            />
            {/* Flap inner fill */}
            <motion.div
              animate={{ rotateX: flapOpen ? -150 : 0 }}
              transition={{ duration: 0.5, ease: 'easeInOut' }}
              style={{
                position: 'absolute', top: '3px', left: '3px',
                width: 0, height: 0, borderStyle: 'solid',
                borderWidth: '0 147px 100px 147px',
                borderColor: 'transparent transparent #ede9fe transparent',
                transformOrigin: 'top center',
                zIndex: 11,
              }}
            />
          </div>
        </motion.div>

        {/* Label under envelope */}
        <motion.p
          style={{
            position: 'absolute', bottom: '20px', left: 0, right: 0,
            textAlign: 'center', fontSize: '13px',
            color: 'var(--text-2, #6e6e73)', fontWeight: 500,
          }}
          animate={{ opacity: isTyping ? 1 : 0.5 }}
          transition={{ duration: 0.3 }}
        >
          {sent ? '✈️ En route vers votre boîte mail !' : isTyping ? '📬 Prêt à envoyer…' : ''}
        </motion.p>
      </div>
    </div>
  );
}

// ── Styles (identical to LoginPage) ──────────────────────────────────────────

const labelStyle: React.CSSProperties = {
  fontSize: '13px',
  fontWeight: 500,
  color: 'var(--text-1, #1d1d1f)',
};

const inputStyle: React.CSSProperties = {
  height: '46px',
  padding: '0 20px',
  borderRadius: '9999px',
  border: '1px solid var(--border, #e0e0e5)',
  background: 'var(--surface, #f9f9fb)',
  color: 'var(--text-1, #1d1d1f)',
  fontSize: '14px',
  fontFamily: 'inherit',
  outline: 'none',
  width: '100%',
  boxSizing: 'border-box' as const,
};

const submitStyle = (disabled: boolean): React.CSSProperties => ({
  height: '46px',
  borderRadius: '9999px',
  border: 'none',
  background: '#e8621a',
  color: '#ffffff',
  fontSize: '15px',
  fontWeight: 600,
  fontFamily: 'inherit',
  cursor: disabled ? 'not-allowed' : 'pointer',
  opacity: disabled ? 0.5 : 1,
  marginTop: '4px',
  transition: 'opacity 0.15s',
  width: '100%',
  outline: 'none',
});

// ── Main component ────────────────────────────────────────────────────────────

const ForgotPasswordPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState('');
  const [isTyping, setIsTyping] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await authService.forgotPassword({ email });
      setSent(true);
    } catch {
      setError('Une erreur est survenue. Veuillez réessayer.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="h-screen grid lg:grid-cols-2 bg-[var(--bg)] transition-colors duration-300">
      {/* ── Top bar ── */}
      <div style={{
        position: 'fixed', top: 0, left: 0, right: 0, zIndex: 50,
        display: 'flex', alignItems: 'flex-start', justifyContent: 'center',
        pointerEvents: 'none',
      }}>
        <div style={{ position: 'absolute', left: '20px', top: '14px', display: 'flex', alignItems: 'center', pointerEvents: 'auto' }}>
          <img src="/antigone-icon.svg" alt="Antigone" style={{ width: '80px', height: '80px' }} />
        </div>
        <div style={{ pointerEvents: 'auto' }}>
          <LightPullThemeSwitcher />
        </div>
      </div>

      {/* Left — envelope character */}
      <EnvelopePanel isTyping={isTyping} email={email} sent={sent} />

      {/* Right — form */}
      <div className="flex items-center justify-center p-8 overflow-y-auto">
        <div className="w-full max-w-[400px]">

          {/* Mobile logo */}
          <motion.div
            className="lg:hidden flex items-center gap-3 justify-center mb-10"
            initial={{ opacity: 0, y: -16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.45, ease: 'easeOut' }}
          >
            <div className="w-9 h-9 rounded-xl flex items-center justify-center" style={{ background: '#e8621a' }}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </div>
            <span className="text-lg font-bold" style={{ color: 'var(--text-1, #1d1d1f)' }}>Antigone</span>
          </motion.div>

          <motion.div
            className="mb-8"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'easeOut', delay: 0.05 }}
          >
            <h1 style={{ fontSize: '28px', fontWeight: 700, color: 'var(--text-1, #1d1d1f)', margin: '0 0 6px' }}>
              Mot de passe oublié ?
            </h1>
            <p style={{ fontSize: '14px', color: 'var(--text-2, #6e6e73)', margin: 0 }}>
              Entrez votre email pour recevoir un lien de réinitialisation.
            </p>
          </motion.div>

          <AnimatePresence mode="wait">
            {sent ? (
              <motion.div
                key="success"
                initial={{ opacity: 0, scale: 0.95, y: 10 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.95 }}
                transition={{ duration: 0.4, ease: 'easeOut' }}
                style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}
              >
                <div style={{
                  padding: '24px 20px',
                  background: 'rgba(16,185,129,0.07)',
                  border: '1px solid rgba(16,185,129,0.22)',
                  borderRadius: '18px',
                  textAlign: 'center',
                }}>
                  <motion.div
                    initial={{ scale: 0 }}
                    animate={{ scale: 1 }}
                    transition={{ type: 'spring', stiffness: 300, damping: 18, delay: 0.1 }}
                    style={{ fontSize: '44px', marginBottom: '12px' }}
                  >
                    📧
                  </motion.div>
                  <p style={{ fontSize: '16px', fontWeight: 700, color: 'var(--text-1)', margin: '0 0 8px' }}>Email envoyé !</p>
                  <p style={{ fontSize: '13px', color: 'var(--text-2, #6e6e73)', margin: 0, lineHeight: 1.6 }}>
                    Si un compte correspond à <strong>{email}</strong>, vous recevrez un lien de réinitialisation valable 30 minutes.
                  </p>
                </div>
                <Link
                  to="/login"
                  style={{
                    height: '46px', borderRadius: '9999px',
                    background: 'var(--surface, #f9f9fb)',
                    border: '1px solid var(--border, #e0e0e5)',
                    color: 'var(--text-1)', fontSize: '15px', fontWeight: 600,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    textDecoration: 'none',
                  }}
                >
                  ← Retour à la connexion
                </Link>
              </motion.div>
            ) : (
              <motion.form
                key="form"
                onSubmit={handleSubmit}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}
              >
                <motion.div
                  style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.5, ease: 'easeOut', delay: 0.15 }}
                >
                  <label htmlFor="email" style={labelStyle}>Adresse email</label>
                  <input
                    id="email"
                    name="email"
                    type="email"
                    autoComplete="email"
                    required
                    placeholder="exemple@email.com"
                    value={email}
                    onChange={e => setEmail(e.target.value)}
                    onFocus={() => setIsTyping(true)}
                    onBlur={() => setIsTyping(false)}
                    style={inputStyle}
                  />
                </motion.div>

                {error && (
                  <motion.p
                    initial={{ opacity: 0, scale: 0.95 }}
                    animate={{ opacity: 1, scale: 1 }}
                    transition={{ duration: 0.2 }}
                    style={{
                      fontSize: '13px', color: '#ff3b30',
                      background: 'rgba(255,59,48,0.08)',
                      borderRadius: '8px', padding: '10px 12px', margin: 0,
                    }}
                  >
                    {error}
                  </motion.p>
                )}

                <motion.button
                  type="submit"
                  disabled={loading}
                  style={submitStyle(loading)}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.5, ease: 'easeOut', delay: 0.25 }}
                  whileHover={loading ? {} : { opacity: 0.88 }}
                  whileTap={loading ? {} : { scale: 0.98 }}
                >
                  {loading ? 'Envoi en cours…' : 'Envoyer le lien'}
                </motion.button>

                <motion.div
                  style={{ textAlign: 'center' }}
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ duration: 0.5, delay: 0.3 }}
                >
                  <Link to="/login" style={{ fontSize: '13px', color: '#e8621a', textDecoration: 'none', fontWeight: 500 }}>
                    ← Retour à la connexion
                  </Link>
                </motion.div>
              </motion.form>
            )}
          </AnimatePresence>

          <motion.p
            style={{ marginTop: '24px', textAlign: 'center', fontSize: '14px', color: 'var(--text-3, #9c9a94)' }}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.5, delay: 0.45 }}
          >
            © 2026 Antigone-IT. Tous droits réservés.
          </motion.p>
        </div>
      </div>
    </div>
  );
};

export default ForgotPasswordPage;
