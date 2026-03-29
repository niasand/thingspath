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
    <div className="relative group">
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
              className={`shrink-0 px-3 py-1.5 rounded-full text-xs font-medium transition-all duration-200 border
                ${
                  active
                    ? 'bg-primary text-white border-primary shadow-sm'
                    : 'bg-surface text-text-secondary border-border/60 hover:border-primary/30 hover:text-primary'
                }`}
            >
              {tag}
            </button>
          );
        })}
      </div>
    </div>
  );
}
