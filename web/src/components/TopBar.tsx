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
          className="p-2 -ml-2 rounded-xl hover:bg-gray-light text-text-secondary transition-colors"
        >
          <ArrowLeft size={20} />
        </button>
      )}
      <h1 className="flex-1 text-xl font-bold text-text truncate">{title}</h1>
      {actions}
    </div>
  );
}
