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
  const ref = useRef<HTMLDivElement>(null);
  const currentLabel = SORT_OPTIONS.find(o => o.field === field)?.label ?? '排序';

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={() => setOpen(!open)}
        className="flex items-center gap-1 px-3 py-2 rounded-xl bg-surface border border-border/60
                   text-sm text-text-secondary hover:border-primary/30 transition-all"
      >
        <ListFilter size={16} />
        <span className="hidden sm:inline">{currentLabel}</span>
        {!ascending && <ChevronDown size={14} />}
        {ascending && <ChevronDown size={14} className="rotate-180" />}
      </button>

      {open && (
        <div className="absolute right-0 top-full mt-1 bg-surface rounded-xl shadow-lg border border-border/40 py-1 z-50 min-w-[140px]">
          {SORT_OPTIONS.map(opt => {
            const active = opt.field === field;
            return (
              <button
                key={opt.field}
                onClick={() => { onSelect(opt.field); setOpen(false); }}
                className={`w-full text-left px-3 py-2 text-sm transition-colors ${
                  active ? 'text-primary bg-primary/5 font-medium' : 'text-text hover:bg-gray-light'
                }`}
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
