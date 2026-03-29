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
    <div className="fixed bottom-24 md:bottom-8 right-6 md:right-[calc(50%-336px+1rem)] z-30" ref={ref}>
      {/* Expanded menu */}
      <div
        className="absolute bottom-16 right-0 flex flex-col gap-2 items-end"
        style={{
          opacity: expanded ? 1 : 0,
          transform: expanded ? 'translateY(0) scale(1)' : 'translateY(8px) scale(0.95)',
          pointerEvents: expanded ? 'auto' : 'none',
          transition: 'all 0.2s ease',
        }}
      >
        <button
          onClick={() => { onAddAI(); setExpanded(false); }}
          className="flex items-center gap-2 px-4 py-2.5 text-sm font-medium cursor-pointer"
          style={{
            background: 'var(--bg-elevated)',
            backdropFilter: 'blur(var(--blur-md))',
            WebkitBackdropFilter: 'blur(var(--blur-md))',
            border: '1px solid var(--border-glass)',
            borderRadius: 'var(--radius-md)',
            boxShadow: 'var(--shadow-md)',
            color: 'var(--accent)',
            transition: 'all 0.2s ease',
          }}
          onMouseEnter={e => {
            e.currentTarget.style.transform = 'translateY(-1px)';
            e.currentTarget.style.boxShadow = 'var(--shadow-lg)';
          }}
          onMouseLeave={e => {
            e.currentTarget.style.transform = 'translateY(0)';
            e.currentTarget.style.boxShadow = 'var(--shadow-md)';
          }}
        >
          <Sparkles size={16} />
          <span>AI 智能添加</span>
        </button>
        <button
          onClick={() => { onAddManual(); setExpanded(false); }}
          className="flex items-center gap-2 px-4 py-2.5 text-sm font-medium cursor-pointer"
          style={{
            background: 'var(--bg-elevated)',
            backdropFilter: 'blur(var(--blur-md))',
            WebkitBackdropFilter: 'blur(var(--blur-md))',
            border: '1px solid var(--border-glass)',
            borderRadius: 'var(--radius-md)',
            boxShadow: 'var(--shadow-md)',
            color: 'var(--text-primary)',
            transition: 'all 0.2s ease',
          }}
          onMouseEnter={e => {
            e.currentTarget.style.transform = 'translateY(-1px)';
            e.currentTarget.style.boxShadow = 'var(--shadow-lg)';
          }}
          onMouseLeave={e => {
            e.currentTarget.style.transform = 'translateY(0)';
            e.currentTarget.style.boxShadow = 'var(--shadow-md)';
          }}
        >
          <Pencil size={16} />
          <span>手动添加</span>
        </button>
      </div>

      {/* FAB */}
      <button
        onClick={() => setExpanded(!expanded)}
        className="w-14 h-14 flex items-center justify-center cursor-pointer"
        style={{
          borderRadius: 'var(--radius-lg)',
          background: expanded ? 'var(--accent-dark)' : 'linear-gradient(135deg, var(--accent), var(--accent-light))',
          color: 'var(--text-inverse)',
          border: 'none',
          boxShadow: expanded ? 'var(--shadow-md)' : 'var(--shadow-glow), var(--shadow-md)',
          backdropFilter: 'blur(var(--blur-sm))',
          WebkitBackdropFilter: 'blur(var(--blur-sm))',
          transition: 'all 0.2s ease',
          transform: expanded ? 'rotate(0deg)' : 'rotate(0deg)',
        }}
      >
        {expanded ? <X size={24} /> : <Plus size={24} />}
      </button>
    </div>
  );
}
