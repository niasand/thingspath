import { AlertTriangle } from 'lucide-react';

interface Props {
  open: boolean;
  title?: string;
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
  loading?: boolean;
}

export default function DeleteConfirmationDialog({
  open,
  title = '确认删除',
  message,
  onConfirm,
  onCancel,
  loading,
}: Props) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div
        className="absolute inset-0"
        style={{
          background: 'rgba(0, 0, 0, 0.3)',
          backdropFilter: 'blur(var(--blur-sm))',
          WebkitBackdropFilter: 'blur(var(--blur-sm))',
        }}
        onClick={onCancel}
      />

      {/* Dialog */}
      <div
        className="relative p-6 max-w-sm w-full"
        style={{
          background: 'var(--bg-elevated)',
          backdropFilter: 'blur(var(--blur-lg))',
          WebkitBackdropFilter: 'blur(var(--blur-lg))',
          border: '1px solid var(--border-glass)',
          borderRadius: 'var(--radius-lg)',
          boxShadow: 'var(--shadow-lg)',
          animation: 'dialogIn 0.2s ease',
        }}
      >
        <style>{`
          @keyframes dialogIn {
            from { opacity: 0; transform: scale(0.95) translateY(8px); }
            to { opacity: 1; transform: scale(1) translateY(0); }
          }
        `}</style>

        <div className="flex items-center gap-3 mb-4">
          <div
            className="w-10 h-10 flex items-center justify-center shrink-0"
            style={{
              borderRadius: 'var(--radius-full)',
              background: 'var(--danger-soft)',
            }}
          >
            <AlertTriangle size={20} style={{ color: 'var(--danger)' }} />
          </div>
          <h3
            className="text-lg font-semibold"
            style={{ color: 'var(--text-primary)' }}
          >
            {title}
          </h3>
        </div>

        <p
          className="text-sm mb-6 leading-relaxed"
          style={{ color: 'var(--text-secondary)' }}
        >
          {message}
        </p>

        <div className="flex gap-3 justify-end">
          <button
            onClick={onCancel}
            disabled={loading}
            className="px-4 py-2.5 text-sm font-medium cursor-pointer"
            style={{
              borderRadius: 'var(--radius-md)',
              color: 'var(--text-secondary)',
              background: 'var(--bg-surface)',
              backdropFilter: 'blur(var(--blur-sm))',
              WebkitBackdropFilter: 'blur(var(--blur-sm))',
              border: '1px solid var(--border-glass)',
              transition: 'all 0.2s ease',
            }}
            onMouseEnter={e => {
              e.currentTarget.style.background = 'var(--bg-surface-hover)';
            }}
            onMouseLeave={e => {
              e.currentTarget.style.background = 'var(--bg-surface)';
            }}
          >
            取消
          </button>
          <button
            onClick={onConfirm}
            disabled={loading}
            className="px-4 py-2.5 text-sm font-medium cursor-pointer"
            style={{
              borderRadius: 'var(--radius-md)',
              background: 'var(--danger)',
              color: 'var(--text-inverse)',
              border: 'none',
              boxShadow: 'var(--shadow-md)',
              transition: 'all 0.2s ease',
              opacity: loading ? 0.5 : 1,
              cursor: loading ? 'not-allowed' : 'pointer',
            }}
            onMouseEnter={e => {
              if (!loading) e.currentTarget.style.opacity = '0.9';
            }}
            onMouseLeave={e => {
              if (!loading) e.currentTarget.style.opacity = '1';
            }}
          >
            {loading ? '删除中...' : '删除'}
          </button>
        </div>
      </div>
    </div>
  );
}
