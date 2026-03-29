import { useState } from 'react';
import { Sparkles, X } from 'lucide-react';

interface Props {
  open: boolean;
  loading: boolean;
  onSubmit: (text: string) => void;
  onClose: () => void;
}

export default function AIInputDialog({ open, loading, onSubmit, onClose }: Props) {
  const [text, setText] = useState('');

  if (!open) return null;

  const handleSubmit = () => {
    const trimmed = text.trim();
    if (!trimmed || loading) return;
    onSubmit(trimmed);
    setText('');
  };

  const handleClose = () => {
    if (!loading) {
      setText('');
      onClose();
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end md:items-center justify-center p-4">
      {/* Backdrop */}
      <div
        className="absolute inset-0"
        style={{
          background: 'rgba(0, 0, 0, 0.3)',
          backdropFilter: 'blur(var(--blur-sm))',
          WebkitBackdropFilter: 'blur(var(--blur-sm))',
        }}
        onClick={handleClose}
      />

      {/* Bottom sheet on mobile, centered dialog on desktop */}
      <div
        className="relative w-full max-w-md"
        style={{
          background: 'var(--bg-elevated)',
          backdropFilter: 'blur(var(--blur-lg))',
          WebkitBackdropFilter: 'blur(var(--blur-lg))',
          border: '1px solid var(--border-glass)',
          borderRadius: 'var(--radius-lg)',
          boxShadow: 'var(--shadow-lg)',
          animation: 'bottomSheetIn 0.3s ease',
        }}
      >
        <style>{`
          @keyframes bottomSheetIn {
            from { opacity: 0; transform: translateY(20px) scale(0.98); }
            to { opacity: 1; transform: translateY(0) scale(1); }
          }
          @media (min-width: 768px) {
            @keyframes bottomSheetIn {
              from { opacity: 0; transform: scale(0.95) translateY(8px); }
              to { opacity: 1; transform: scale(1) translateY(0); }
            }
          }
        `}</style>

        {/* Header */}
        <div
          className="flex items-center justify-between p-4"
          style={{ borderBottom: '1px solid var(--border-glass)' }}
        >
          <div className="flex items-center gap-2.5">
            <div
              className="w-8 h-8 flex items-center justify-center"
              style={{
                borderRadius: 'var(--radius-sm)',
                background: 'var(--accent-soft)',
                border: '1px solid var(--border-glass)',
              }}
            >
              <Sparkles size={16} style={{ color: 'var(--accent)' }} />
            </div>
            <div>
              <h3
                className="font-semibold text-sm"
                style={{ color: 'var(--text-primary)' }}
              >
                AI 智能添加
              </h3>
              <p
                className="text-[11px]"
                style={{ color: 'var(--text-secondary)' }}
              >
                描述物品，AI 自动提取信息
              </p>
            </div>
          </div>
          <button
            onClick={handleClose}
            className="cursor-pointer"
            style={{
              borderRadius: 'var(--radius-sm)',
              padding: '6px',
              color: 'var(--text-secondary)',
              background: 'transparent',
              border: 'none',
              transition: 'all 0.2s ease',
            }}
            onMouseEnter={e => {
              e.currentTarget.style.background = 'var(--bg-surface-hover)';
            }}
            onMouseLeave={e => {
              e.currentTarget.style.background = 'transparent';
            }}
          >
            <X size={18} />
          </button>
        </div>

        {/* Body */}
        <div className="p-4 space-y-3">
          <textarea
            value={text}
            onChange={e => setText(e.target.value)}
            placeholder="例如：昨天在京东买了一个索尼WH-1000XM5降噪耳机，花了2299元，放在卧室的床头柜上"
            rows={4}
            className="w-full p-3 rounded-xl text-sm resize-none outline-none"
            style={{
              background: 'var(--bg-input)',
              border: '1px solid var(--border-glass)',
              color: 'var(--text-primary)',
              backdropFilter: 'blur(var(--blur-sm))',
              WebkitBackdropFilter: 'blur(var(--blur-sm))',
              borderRadius: 'var(--radius-md)',
              transition: 'all 0.2s ease',
            }}
            onFocus={e => {
              e.currentTarget.style.borderColor = 'var(--border-focus)';
              e.currentTarget.style.boxShadow = 'var(--shadow-md)';
              e.currentTarget.style.background = 'var(--bg-elevated)';
            }}
            onBlur={e => {
              e.currentTarget.style.borderColor = 'var(--border-glass)';
              e.currentTarget.style.boxShadow = 'none';
              e.currentTarget.style.background = 'var(--bg-input)';
            }}
            disabled={loading}
          />
          <p
            className="text-[11px] leading-relaxed"
            style={{ color: 'var(--text-tertiary)' }}
          >
            支持识别：物品名称、价格、购买日期、存放位置、分类标签、备注信息。支持多个物品同时识别。
          </p>
        </div>

        {/* Footer */}
        <div className="p-4 pt-0">
          <button
            onClick={handleSubmit}
            disabled={!text.trim() || loading}
            className="w-full py-2.5 text-sm font-medium flex items-center justify-center gap-2 cursor-pointer"
            style={{
              borderRadius: 'var(--radius-md)',
              background: !text.trim() || loading
                ? 'var(--accent-soft)'
                : 'linear-gradient(135deg, var(--accent), var(--accent-light))',
              color: 'var(--text-inverse)',
              border: 'none',
              boxShadow: !text.trim() || loading ? 'none' : 'var(--shadow-glow)',
              transition: 'all 0.2s ease',
              opacity: !text.trim() || loading ? 0.5 : 1,
              cursor: !text.trim() || loading ? 'not-allowed' : 'pointer',
            }}
          >
            {loading ? (
              <>
                <div
                  className="w-4 h-4 rounded-full animate-spin"
                  style={{
                    border: '2px solid rgba(255,255,255,0.3)',
                    borderTopColor: 'white',
                  }}
                />
                AI 分析中...
              </>
            ) : (
              <>
                <Sparkles size={16} />
                开始识别
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
