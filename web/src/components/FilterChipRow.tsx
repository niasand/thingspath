import { useRef } from 'react';

interface Props {
  tags: string[];
  selectedTags: Set<string>;
  onToggle: (tag: string) => void;
}

export default function FilterChipRow({ tags, selectedTags, onToggle }: Props) {
  const scrollRef = useRef<HTMLDivElement>(null);

  if (tags.length === 0) return null;

  return (
    <div className="relative">
      <div
        ref={scrollRef}
        className="flex gap-2 overflow-x-auto scrollbar-hide py-1"
        style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
      >
        {tags.map(tag => {
          const active = selectedTags.has(tag);
          return (
            <button
              key={tag}
              onClick={() => onToggle(tag)}
              className="shrink-0 px-3.5 py-1.5 rounded-full text-xs font-medium"
              style={{
                background: active
                  ? 'linear-gradient(135deg, var(--accent), var(--accent-light))'
                  : 'var(--bg-surface)',
                backdropFilter: active ? 'none' : 'blur(var(--blur-sm))',
                WebkitBackdropFilter: active ? 'none' : 'blur(var(--blur-sm))',
                border: active
                  ? '1px solid var(--accent-light)'
                  : '1px solid var(--border-glass)',
                color: active ? 'var(--text-inverse)' : 'var(--text-secondary)',
                boxShadow: active ? 'var(--shadow-glow)' : 'none',
                transition: 'all 0.2s ease',
              }}
              onMouseEnter={e => {
                if (!active) {
                  e.currentTarget.style.background = 'var(--accent-soft)';
                  e.currentTarget.style.color = 'var(--accent)';
                  e.currentTarget.style.borderColor = 'var(--accent-light)';
                  e.currentTarget.style.transform = 'translateY(-1px)';
                }
              }}
              onMouseLeave={e => {
                if (!active) {
                  e.currentTarget.style.background = 'var(--bg-surface)';
                  e.currentTarget.style.color = 'var(--text-secondary)';
                  e.currentTarget.style.borderColor = 'var(--border-glass)';
                  e.currentTarget.style.transform = 'translateY(0)';
                }
              }}
            >
              {tag}
            </button>
          );
        })}
      </div>
    </div>
  );
}
