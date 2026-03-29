import { useState, useRef, useEffect } from 'react';
import { Plus, Sparkles, Pencil, X } from 'lucide-react';

interface Props {
  onAddManual: () => void;
  onAddAI: () => void;
}

export default function FloatingActionButton({ onAddManual, onAddAI }: Props) {
  const [expanded, setExpanded] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setExpanded(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  return (
    <div className="fixed bottom-24 md:bottom-8 right-6 z-30" ref={ref}>
      {/* Expanded menu */}
      {expanded && (
        <div className="absolute bottom-16 right-0 flex flex-col gap-2 animate-in items-end">
          <button
            onClick={() => { onAddAI(); setExpanded(false); }}
            className="flex items-center gap-2 bg-surface rounded-xl shadow-lg border border-border/40
                       px-4 py-2.5 text-sm font-medium text-secondary hover:bg-secondary/5 transition-all"
          >
            <Sparkles size={16} />
            <span>AI 智能添加</span>
          </button>
          <button
            onClick={() => { onAddManual(); setExpanded(false); }}
            className="flex items-center gap-2 bg-surface rounded-xl shadow-lg border border-border/40
                       px-4 py-2.5 text-sm font-medium text-text hover:bg-gray-light transition-all"
          >
            <Pencil size={16} />
            <span>手动添加</span>
          </button>
        </div>
      )}

      {/* FAB */}
      <button
        onClick={() => setExpanded(!expanded)}
        className="w-14 h-14 rounded-2xl bg-primary text-white shadow-lg shadow-primary/30
                   flex items-center justify-center hover:bg-primary-dark active:scale-95
                   transition-all duration-200 backdrop-blur-sm"
      >
        {expanded ? <X size={24} /> : <Plus size={24} />}
      </button>
    </div>
  );
}
