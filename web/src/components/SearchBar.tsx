import { Search, X } from 'lucide-react';

interface Props {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}

export default function SearchBar({ value, onChange, placeholder = '搜索物品...' }: Props) {
  return (
    <div className="relative">
      <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-secondary" />
      <input
        type="text"
        value={value}
        onChange={e => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full pl-10 pr-9 py-2.5 rounded-xl bg-surface border border-border/60 text-sm
                   placeholder:text-text-tertiary focus:outline-none focus:border-primary/40 focus:ring-2
                   focus:ring-primary/10 transition-all"
      />
      {value && (
        <button
          onClick={() => onChange('')}
          className="absolute right-2.5 top-1/2 -translate-y-1/2 p-0.5 rounded-full
                     hover:bg-gray-light text-text-secondary transition-colors"
        >
          <X size={16} />
        </button>
      )}
    </div>
  );
}
