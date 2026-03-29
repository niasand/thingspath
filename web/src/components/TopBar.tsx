import { useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';

interface Props {
  title: string;
  showBack?: boolean;
  actions?: React.ReactNode;
}

export default function TopBar({ title, showBack, actions }: Props) {
  const navigate = useNavigate();

  return (
    <div className="flex items-center gap-3 mb-4">
      {showBack && (
        <button
          onClick={() => navigate(-1)}
          className="p-2 -ml-2 cursor-pointer"
          style={{
            borderRadius: 'var(--radius-sm)',
            color: 'var(--text-secondary)',
            background: 'transparent',
            border: 'none',
            transition: 'all 0.2s ease',
          }}
          onMouseEnter={e => {
            e.currentTarget.style.background = 'var(--bg-surface-hover)';
            e.currentTarget.style.color = 'var(--text-primary)';
          }}
          onMouseLeave={e => {
            e.currentTarget.style.background = 'transparent';
            e.currentTarget.style.color = 'var(--text-secondary)';
          }}
        >
          <ArrowLeft size={20} />
        </button>
      )}
      <h1
        className="flex-1 text-xl font-bold truncate"
        style={{ color: 'var(--text-primary)' }}
      >
        {title}
      </h1>
      {actions}
    </div>
  );
}
