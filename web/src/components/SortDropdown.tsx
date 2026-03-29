import { useState, useRef, useEffect } from 'react';
import { ListFilter, ChevronDown } from 'lucide-react';
import type { SortField } from '../utils/constants';
import { SORT_OPTIONS } from '../utils/constants';

interface Props {
  field: SortField;
  ascending: boolean;
  onSelect: (field: SortField) => void;
}

export default function SortDropdown({ field, ascending, onSelect }: Props) {
  const [open, setOpen] = useState(false);
  const [animating, setAnimating] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const currentLabel = SORT_OPTIONS.find(o => o.field === field)?.label ?? '排序';

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  useEffect(() => {
    if (open) {
      setAnimating(true);
    } else if (animating) {
      const timer = setTimeout(() => setAnimating(false), 200);
      return () => clearTimeout(timer);
    }
  }, [open, animating]);

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={() => setOpen(!open)}
        className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-sm cursor-pointer"
        style={{
          background: 'var(--bg-surface)',
          backdropFilter: 'blur(var(--blur-sm))',
          WebkitBackdropFilter: 'blur(var(--blur-sm))',
          border: '1px solid var(--border-glass)',
          color: open ? 'var(--accent)' : 'var(--text-secondary)',
          boxShadow: open ? 'var(--shadow-md)' : 'var(--shadow-sm)',
          transition: 'all 0.2s ease',
        }}
      >
        <ListFilter size={16} />
        <span className="hidden sm:inline">{currentLabel}</span>
        <ChevronDown
          size={14}
          className="transition-transform duration-200"
          style={{ transform: ascending ? 'rotate(180deg)' : 'rotate(0deg)' }}
        />
      </button>

      {(open || animating) && (
        <div
          className="absolute right-0 top-full mt-1.5 py-1 z-50 min-w-[140px] rounded-xl"
          style={{
            background: 'var(--bg-elevated)',
            backdropFilter: 'blur(var(--blur-lg))',
            WebkitBackdropFilter: 'blur(var(--blur-lg))',
            border: '1px solid var(--border-glass)',
            boxShadow: 'var(--shadow-lg)',
            opacity: open ? 1 : 0,
            transform: open ? 'translateY(0) scale(1)' : 'translateY(-4px) scale(0.98)',
            transition: 'all 0.2s ease',
            transformOrigin: 'top right',
          }}
        >
          {SORT_OPTIONS.map(opt => {
            const active = opt.field === field;
            return (
              <button
                key={opt.field}
                onClick={() => { onSelect(opt.field); setOpen(false); }}
                className="w-full text-left px-3.5 py-2 text-sm cursor-pointer"
                style={{
                  background: active ? 'var(--accent-soft)' : 'transparent',
                  color: active ? 'var(--accent)' : 'var(--text-primary)',
                  fontWeight: active ? 600 : 400,
                  transition: 'all 0.15s ease',
                }}
                onMouseEnter={e => {
                  if (!active) {
                    e.currentTarget.style.background = 'var(--bg-surface-hover)';
                  }
                }}
                onMouseLeave={e => {
                  if (!active) {
                    e.currentTarget.style.background = 'transparent';
                  }
                }}
              >
                {opt.label}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
