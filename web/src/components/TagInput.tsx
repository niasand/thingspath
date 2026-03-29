import { useState, useRef } from 'react';
import { X, Plus } from 'lucide-react';

interface Props {
  tags: string[];
  onChange: (tags: string[]) => void;
  maxTags?: number;
}

export default function TagInput({ tags, onChange, maxTags = 5 }: Props) {
  const [input, setInput] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);

  const addTag = () => {
    const tag = input.trim();
    if (!tag || tags.includes(tag) || tags.length >= maxTags) return;
    onChange([...tags, tag]);
    setInput('');
  };

  const removeTag = (tag: string) => {
    onChange(tags.filter(t => t !== tag));
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      addTag();
    }
  };

  return (
    <div className="space-y-2">
      <label
        className="block text-sm font-medium"
        style={{ color: 'var(--text-primary)' }}
      >
        标签
      </label>
      <div
        className="flex flex-wrap gap-2 p-2.5 min-h-[44px] items-center"
        style={{
          borderRadius: 'var(--radius-md)',
          background: 'var(--bg-surface)',
          backdropFilter: 'blur(var(--blur-sm))',
          WebkitBackdropFilter: 'blur(var(--blur-sm))',
          border: '1px solid var(--border-glass)',
          transition: 'all 0.2s ease',
        }}
      >
        {tags.map(tag => (
          <span
            key={tag}
            className="flex items-center gap-1 px-2.5 py-1 text-xs font-medium"
            style={{
              borderRadius: 'var(--radius-full)',
              background: 'var(--accent-soft)',
              color: 'var(--accent)',
              border: '1px solid var(--accent-light)',
              transition: 'all 0.2s ease',
            }}
          >
            {tag}
            <button
              onClick={() => removeTag(tag)}
              className="cursor-pointer"
              style={{
                borderRadius: 'var(--radius-full)',
                padding: '1px',
                transition: 'all 0.15s ease',
              }}
              onMouseEnter={e => {
                e.currentTarget.style.background = 'var(--accent-soft-hover)';
              }}
              onMouseLeave={e => {
                e.currentTarget.style.background = 'transparent';
              }}
            >
              <X size={12} />
            </button>
          </span>
        ))}
        {tags.length < maxTags && (
          <div className="flex items-center gap-1 flex-1 min-w-[100px]">
            <input
              ref={inputRef}
              type="text"
              value={input}
              onChange={e => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder={tags.length === 0 ? '输入标签后回车...' : ''}
              className="flex-1 min-w-[80px] text-sm outline-none bg-transparent"
              style={{
                color: 'var(--text-primary)',
                background: 'transparent',
              }}
            />
            {input.trim() && (
              <button
                onClick={addTag}
                className="cursor-pointer"
                style={{
                  borderRadius: 'var(--radius-sm)',
                  padding: '4px',
                  background: 'var(--accent-soft)',
                  color: 'var(--accent)',
                  border: '1px solid var(--border-glass)',
                  transition: 'all 0.2s ease',
                }}
                onMouseEnter={e => {
                  e.currentTarget.style.background = 'var(--accent-soft-hover)';
                  e.currentTarget.style.transform = 'scale(1.05)';
                }}
                onMouseLeave={e => {
                  e.currentTarget.style.background = 'var(--accent-soft)';
                  e.currentTarget.style.transform = 'scale(1)';
                }}
              >
                <Plus size={14} />
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
