import React, { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { HiOutlineClock, HiOutlineChevronDown } from 'react-icons/hi';

interface TimePickerProps {
  value: string; // "HH:MM"
  onChange: (val: string) => void;
  placeholder?: string;
}

const HOURS = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0'));
const MINUTES = Array.from({ length: 60 }, (_, i) => String(i).padStart(2, '0'));

const TimePicker: React.FC<TimePickerProps> = ({ value, onChange, placeholder = '--:--' }) => {
  const [open, setOpen] = useState(false);
  const [dropdownStyle, setDropdownStyle] = useState<React.CSSProperties>({});
  const ref = useRef<HTMLDivElement>(null);
  const btnRef = useRef<HTMLButtonElement>(null);
  const hourRef = useRef<HTMLDivElement>(null);
  const minRef = useRef<HTMLDivElement>(null);

  const [h, m] = value ? value.split(':') : ['', ''];

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  // Position the portal dropdown under the button
  useEffect(() => {
    if (!open || !btnRef.current) return;
    const rect = btnRef.current.getBoundingClientRect();
    setDropdownStyle({
      position: 'fixed',
      top: rect.bottom + 4,
      left: rect.left,
      width: rect.width,
      zIndex: 9999,
    });
    setTimeout(() => {
      if (h && hourRef.current) {
        const el = hourRef.current.querySelector(`[data-h="${h}"]`) as HTMLElement | null;
        el?.scrollIntoView({ block: 'center' });
      }
      if (m && minRef.current) {
        const el = minRef.current.querySelector(`[data-m="${m}"]`) as HTMLElement | null;
        el?.scrollIntoView({ block: 'center' });
      }
    }, 10);
  }, [open, h, m]);

  const selectHour = (hh: string) => onChange(`${hh}:${m || '00'}`);
  const selectMin = (mm: string) => onChange(`${h || '00'}:${mm}`);

  const dropdown = open ? (
    <div style={dropdownStyle} className="rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 shadow-xl overflow-hidden">
      <div className="flex">
        {/* Hours column */}
        <div ref={hourRef} className="flex-1 max-h-52 overflow-y-auto border-r border-gray-100 dark:border-gray-800 py-1">
          <p className="sticky top-0 bg-white dark:bg-gray-900 text-center text-[10px] font-bold text-gray-400 uppercase tracking-widest py-1 border-b border-gray-100 dark:border-gray-800">H</p>
          {HOURS.map((hh) => (
            <button
              key={hh}
              type="button"
              data-h={hh}
              onMouseDown={(e) => { e.preventDefault(); selectHour(hh); }}
              className={`w-full text-center px-2 py-2 text-theme-sm transition-colors ${
                h === hh
                  ? 'bg-brand-50 dark:bg-brand-500/10 text-brand-600 dark:text-brand-400 font-semibold'
                  : 'text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-800'
              }`}
            >
              {hh}
            </button>
          ))}
        </div>

        {/* Minutes column */}
        <div ref={minRef} className="flex-1 max-h-52 overflow-y-auto py-1">
          <p className="sticky top-0 bg-white dark:bg-gray-900 text-center text-[10px] font-bold text-gray-400 uppercase tracking-widest py-1 border-b border-gray-100 dark:border-gray-800">Min</p>
          {MINUTES.map((mm) => (
            <button
              key={mm}
              type="button"
              data-m={mm}
              onMouseDown={(e) => { e.preventDefault(); selectMin(mm); }}
              className={`w-full text-center px-2 py-2 text-theme-sm transition-colors ${
                m === mm
                  ? 'bg-brand-50 dark:bg-brand-500/10 text-brand-600 dark:text-brand-400 font-semibold'
                  : 'text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-800'
              }`}
            >
              {mm}
            </button>
          ))}
        </div>
      </div>
    </div>
  ) : null;

  return (
    <div ref={ref} className="relative">
      <button
        ref={btnRef}
        type="button"
        onClick={() => setOpen((o) => !o)}
        className="h-11 w-full flex items-center justify-between gap-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 px-4 text-theme-sm hover:border-gray-400 dark:hover:border-gray-500 focus:border-brand-300 focus:outline-none focus:ring focus:ring-brand-500/10 transition-all"
      >
        <span className={`flex items-center gap-2 ${value ? 'text-gray-700 dark:text-gray-200' : 'text-gray-400 dark:text-gray-500'}`}>
          <HiOutlineClock size={15} className="text-gray-400 flex-shrink-0" />
          {value || placeholder}
        </span>
        <HiOutlineChevronDown size={16} className={`text-gray-400 transition-transform flex-shrink-0 ${open ? 'rotate-180' : ''}`} />
      </button>

      {createPortal(dropdown, document.body)}
    </div>
  );
};

export default TimePicker;
