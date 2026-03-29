import { Search, X } from 'lucide-react';
import { useState } from 'react';

interface Props {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}

export default function SearchBar({ value, onChange, placeholder = '搜索物品...' }: Props) {
  const [focused, setFocused] = useState(false);

  return (
    <div className="relative">
      <Search
        size={18}
        className="absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none"
        style={{ color: focused ? 'var(--accent)' : 'var(--text-tertiary)', transition: 'color 0.2s ease' }}
      />
      <input
        type="text"
        value={value}
        onChange={e => onChange(e.target.value)}
        onFocus={() => setFocused(true)}
        onBlur={() => setFocused(false)}
        placeholder={placeholder}
        className="w-full pl-11 pr-10 py-3 rounded-2xl text-sm outline-none"
        style={{
          background: focused ? 'var(--bg-elevated)' : 'var(--bg-input)',
          backdropFilter: focused ? 'blur(var(--blur-md))' : 'blur(var(--blur-sm))',
          WebkitBackdropFilter: focused ? 'blur(var(--blur-md))' : 'blur(var(--blur-sm))',
          border: `1px solid ${focused ? 'var(--border-focus)' : 'var(--border-glass)'}`,
          boxShadow: focused ? 'var(--shadow-md)' : 'var(--shadow-sm)',
          color: 'var(--text-primary)',
          transition: 'all 0.2s ease',
        }}
      />
      {value && (
        <button
          onClick={() => onChange('')}
          className="absolute right-3 top-1/2 -translate-y-1/2 p-1 rounded-full"
          style={{
            color: 'var(--text-tertiary)',
            background: 'var(--bg-surface-hover)',
            transition: 'all 0.2s ease',
          }}
          onMouseEnter={e => {
            e.currentTarget.style.background = 'var(--accent-soft)';
            e.currentTarget.style.color = 'var(--accent)';
          }}
          onMouseLeave={e => {
            e.currentTarget.style.background = 'var(--bg-surface-hover)';
            e.currentTarget.style.color = 'var(--text-tertiary)';
          }}
        >
          <X size={14} />
        </button>
      )}
    </div>
  );
}
