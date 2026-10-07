import React, { useState, useRef, useEffect, useCallback } from 'react';
import { HiOutlineChevronLeft, HiOutlineChevronRight, HiOutlineCalendar } from 'react-icons/hi';

interface DatePickerInputProps {
  value: string;          // YYYY-MM-DD
  onChange: (v: string) => void;
  min?: string;           // YYYY-MM-DD
  max?: string;
  placeholder?: string;
  className?: string;
}

const MONTHS = ['Janvier','Février','Mars','Avril','Mai','Juin','Juillet','Août','Septembre','Octobre','Novembre','Décembre'];
const DAYS   = ['Lu','Ma','Me','Je','Ve','Sa','Di'];

function parseYMD(s: string): Date | null {
  if (!s) return null;
  const [y, m, d] = s.split('-').map(Number);
  if (!y || !m || !d) return null;
  return new Date(y, m - 1, d);
}

function toYMD(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function formatDisplay(s: string): string {
  if (!s) return '';
  const [y, m, d] = s.split('-');
  return `${d}/${m}/${y}`;
}

export const DatePickerInput: React.FC<DatePickerInputProps> = ({
  value, onChange, min, max, placeholder = 'JJ/MM/AAAA', className = '',
}) => {
  const today = new Date();
  const selected = parseYMD(value);
  const minDate = parseYMD(min || '') || null;
  const maxDate = parseYMD(max || '') || null;

  const initMonth = selected || today;
  const [viewYear, setViewYear]   = useState(initMonth.getFullYear());
  const [viewMonth, setViewMonth] = useState(initMonth.getMonth());
  const [open, setOpen]           = useState(false);
  const containerRef              = useRef<HTMLDivElement>(null);

  // Sync view when value changes externally
  useEffect(() => {
    const d = parseYMD(value);
    if (d) { setViewYear(d.getFullYear()); setViewMonth(d.getMonth()); }
  }, [value]);

  // Close on outside click
  useEffect(() => {
    if (!open) return;
    const handle = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', handle);
    return () => document.removeEventListener('mousedown', handle);
  }, [open]);

  const prevMonth = useCallback(() => {
    if (viewMonth === 0) { setViewMonth(11); setViewYear(y => y - 1); }
    else setViewMonth(m => m - 1);
  }, [viewMonth]);

  const nextMonth = useCallback(() => {
    if (viewMonth === 11) { setViewMonth(0); setViewYear(y => y + 1); }
    else setViewMonth(m => m + 1);
  }, [viewMonth]);

  // Build calendar grid (Mon-first)
  const cells = (() => {
    const first = new Date(viewYear, viewMonth, 1);
    // Monday-first: 0=Mon..6=Sun
    let offset = first.getDay() - 1;
    if (offset < 0) offset = 6;
    const total = new Date(viewYear, viewMonth + 1, 0).getDate();
    const days: (number | null)[] = Array(offset).fill(null);
    for (let i = 1; i <= total; i++) days.push(i);
    // pad to complete last row
    while (days.length % 7 !== 0) days.push(null);
    return days;
  })();

  const isDisabled = (day: number) => {
    const d = new Date(viewYear, viewMonth, day);
    if (minDate && d < minDate) return true;
    if (maxDate && d > maxDate) return true;
    return false;
  };

  const isToday = (day: number) => {
    return today.getFullYear() === viewYear && today.getMonth() === viewMonth && today.getDate() === day;
  };

  const isSelected = (day: number) => {
    if (!selected) return false;
    return selected.getFullYear() === viewYear && selected.getMonth() === viewMonth && selected.getDate() === day;
  };

  const pick = (day: number) => {
    if (isDisabled(day)) return;
    onChange(toYMD(new Date(viewYear, viewMonth, day)));
    setOpen(false);
  };

  return (
    <div ref={containerRef} style={{ position: 'relative', width: '100%' }}>
      {/* Trigger */}
      <button
        type="button"
        onClick={() => setOpen(o => !o)}
        className={className}
        style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          width: '100%', cursor: 'pointer', userSelect: 'none', textAlign: 'left',
        }}
      >
        <span style={{ color: value ? 'inherit' : 'var(--text-3)', fontSize: 'inherit' }}>
          {value ? formatDisplay(value) : placeholder}
        </span>
        <HiOutlineCalendar size={16} style={{ color: 'var(--brand)', flexShrink: 0, marginLeft: '6px' }} />
      </button>

      {/* Popup */}
      {open && (
        <div
          style={{
            position: 'absolute',
            top: 'calc(100% + 6px)',
            left: 0,
            zIndex: 9999,
            width: '280px',
            borderRadius: '16px',
            background: 'rgba(255,255,255,0.92)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(243,105,4,0.2)',
            boxShadow: '0 8px 32px rgba(243,105,4,0.12), 0 2px 8px rgba(0,0,0,0.08)',
            padding: '14px',
            animation: 'dpFadeUp 0.18s ease-out',
          }}
          className="dark:bg-gray-900/95 dark:border-orange-500/20"
        >
          <style>{`
            @keyframes dpFadeUp {
              from { opacity:0; transform:translateY(8px); }
              to   { opacity:1; transform:translateY(0); }
            }
          `}</style>

          {/* Month nav */}
          <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:'10px' }}>
            <button type="button" onClick={prevMonth}
              style={{ width:28, height:28, borderRadius:'8px', border:'none', background:'transparent', cursor:'pointer', display:'flex', alignItems:'center', justifyContent:'center', color:'var(--text-2)' }}
              className="hover:bg-orange-50 dark:hover:bg-orange-500/10"
            >
              <HiOutlineChevronLeft size={16} />
            </button>
            <span style={{ fontWeight:700, fontSize:'13px', color:'var(--text-1)' }} className="dark:text-gray-100">
              {MONTHS[viewMonth]} {viewYear}
            </span>
            <button type="button" onClick={nextMonth}
              style={{ width:28, height:28, borderRadius:'8px', border:'none', background:'transparent', cursor:'pointer', display:'flex', alignItems:'center', justifyContent:'center', color:'var(--text-2)' }}
              className="hover:bg-orange-50 dark:hover:bg-orange-500/10"
            >
              <HiOutlineChevronRight size={16} />
            </button>
          </div>

          {/* Day headers */}
          <div style={{ display:'grid', gridTemplateColumns:'repeat(7,1fr)', marginBottom:'4px' }}>
            {DAYS.map(d => (
              <div key={d} style={{ textAlign:'center', fontSize:'10px', fontWeight:700, color:'var(--text-3)', padding:'2px 0', textTransform:'uppercase', letterSpacing:'0.05em' }}>
                {d}
              </div>
            ))}
          </div>

          {/* Day grid */}
          <div style={{ display:'grid', gridTemplateColumns:'repeat(7,1fr)', gap:'2px' }}>
            {cells.map((day, i) => {
              if (day === null) return <div key={`e-${i}`} />;
              const sel  = isSelected(day);
              const tod  = isToday(day);
              const dis  = isDisabled(day);
              return (
                <button
                  key={day}
                  type="button"
                  onClick={() => pick(day)}
                  disabled={dis}
                  style={{
                    width:'100%', aspectRatio:'1', borderRadius:'50%', border:'none',
                    fontSize:'12px', fontWeight: sel || tod ? 700 : 400,
                    cursor: dis ? 'not-allowed' : 'pointer',
                    transition: 'all 0.12s',
                    background: sel ? 'var(--brand)' : tod ? 'rgba(243,105,4,0.12)' : 'transparent',
                    color: sel ? '#fff' : dis ? 'var(--text-3)' : tod ? 'var(--brand)' : 'var(--text-1)',
                    opacity: dis ? 0.4 : 1,
                    outline: 'none',
                  }}
                  onMouseEnter={e => { if (!sel && !dis) (e.currentTarget as HTMLButtonElement).style.background = 'rgba(243,105,4,0.1)'; }}
                  onMouseLeave={e => { if (!sel && !dis) (e.currentTarget as HTMLButtonElement).style.background = 'transparent'; }}
                >
                  {day}
                </button>
              );
            })}
          </div>

          {/* Today shortcut */}
          <div style={{ marginTop:'10px', borderTop:'1px solid rgba(0,0,0,0.06)', paddingTop:'8px', textAlign:'center' }}>
            <button
              type="button"
              onClick={() => {
                if (!isDisabled(today.getDate()) || (today.getFullYear() === viewYear && today.getMonth() === viewMonth)) {
                  const d = new Date(today.getFullYear(), today.getMonth(), today.getDate());
                  if (!minDate || d >= minDate) { onChange(toYMD(d)); setOpen(false); }
                }
                setViewYear(today.getFullYear()); setViewMonth(today.getMonth());
              }}
              style={{ fontSize:'11px', fontWeight:600, color:'var(--brand)', background:'none', border:'none', cursor:'pointer', padding:'2px 8px', borderRadius:'6px' }}
              className="hover:bg-orange-50 dark:hover:bg-orange-500/10"
            >
              Aujourd'hui
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default DatePickerInput;
