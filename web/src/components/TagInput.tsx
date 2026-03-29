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
      <label className="block text-sm font-medium text-text">标签</label>
      <div className="flex flex-wrap gap-2 p-2.5 rounded-xl bg-surface border border-border/60 min-h-[44px] items-center">
        {tags.map(tag => (
          <span
            key={tag}
            className="flex items-center gap-1 px-2 py-1 rounded-lg bg-primary/10 text-primary text-xs font-medium"
          >
            {tag}
            <button onClick={() => removeTag(tag)} className="hover:bg-primary/20 rounded-full p-0.5">
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
              className="flex-1 min-w-[80px] text-sm outline-none bg-transparent placeholder:text-text-tertiary"
            />
            {input.trim() && (
              <button
                onClick={addTag}
                className="p-1 rounded-lg bg-primary/10 text-primary hover:bg-primary/20 transition-colors"
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
