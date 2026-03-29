import { PackageOpen } from 'lucide-react';

export default function EmptyState() {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center">
      <div
        className="w-20 h-20 flex items-center justify-center mb-5"
        style={{
          borderRadius: 'var(--radius-xl)',
          background: 'var(--accent-soft)',
          border: '1px solid var(--border-glass)',
          backdropFilter: 'blur(var(--blur-sm))',
          WebkitBackdropFilter: 'blur(var(--blur-sm))',
        }}
      >
        <PackageOpen size={36} style={{ color: 'var(--accent)' }} />
      </div>
      <h3
        className="text-lg font-semibold mb-1.5"
        style={{ color: 'var(--text-primary)' }}
      >
        还没有物品
      </h3>
      <p
        className="text-sm max-w-[240px] leading-relaxed"
        style={{ color: 'var(--text-secondary)' }}
      >
        点击右下角 + 按钮添加你的第一个物品
      </p>
    </div>
  );
}
