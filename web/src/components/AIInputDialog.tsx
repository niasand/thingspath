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
      <div className="absolute inset-0 bg-black/30 backdrop-blur-sm" onClick={handleClose} />
      <div className="relative bg-surface rounded-2xl shadow-xl w-full max-w-md animate-in">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-border/40">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-xl bg-secondary/10 flex items-center justify-center">
              <Sparkles size={16} className="text-secondary" />
            </div>
            <div>
              <h3 className="font-semibold text-text text-sm">AI 智能添加</h3>
              <p className="text-[11px] text-text-secondary">描述物品，AI 自动提取信息</p>
            </div>
          </div>
          <button onClick={handleClose} className="p-1.5 rounded-lg hover:bg-gray-light text-text-secondary">
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
            className="w-full p-3 rounded-xl border border-border/60 bg-bg-page text-sm resize-none
                       placeholder:text-text-tertiary focus:outline-none focus:border-primary/40 focus:ring-2
                       focus:ring-primary/10 transition-all"
            disabled={loading}
          />
          <p className="text-[11px] text-text-tertiary leading-relaxed">
            支持识别：物品名称、价格、购买日期、存放位置、分类标签、备注信息。支持多个物品同时识别。
          </p>
        </div>

        {/* Footer */}
        <div className="p-4 pt-0">
          <button
            onClick={handleSubmit}
            disabled={!text.trim() || loading}
            className="w-full py-2.5 rounded-xl text-sm font-medium bg-primary text-white
                       hover:bg-primary-dark transition-colors disabled:opacity-50 disabled:cursor-not-allowed
                       flex items-center justify-center gap-2"
          >
            {loading ? (
              <>
                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
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
