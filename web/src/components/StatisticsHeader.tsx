import { BarChart3, Wallet } from 'lucide-react';
import { formatPrice } from '../utils/numberUtils';

interface Props {
  totalItems: number;
  totalPrice: number;
}

export default function StatisticsHeader({ totalItems, totalPrice }: Props) {
  return (
    <div
      className="p-4 mb-4"
      style={{
        borderRadius: 'var(--radius-lg)',
        background: 'var(--bg-surface)',
        backdropFilter: 'blur(var(--blur-md))',
        WebkitBackdropFilter: 'blur(var(--blur-md))',
        border: '1px solid var(--border-glass)',
        boxShadow: 'var(--shadow-sm)',
      }}
    >
      <div className="flex items-center gap-2 mb-3">
        <BarChart3 size={18} style={{ color: 'var(--accent)' }} />
        <span
          className="text-sm font-semibold"
          style={{ color: 'var(--text-primary)' }}
        >
          概览
        </span>
      </div>
      <div className="grid grid-cols-2 gap-3">
        {/* Items count card */}
        <div
          className="p-3"
          style={{
            borderRadius: 'var(--radius-md)',
            background: 'linear-gradient(135deg, var(--accent-soft), rgba(124, 58, 237, 0.05))',
            border: '1px solid var(--border-glass)',
            transition: 'all 0.2s ease',
          }}
        >
          <div className="flex items-center gap-1.5 mb-1">
            <BarChart3 size={14} style={{ color: 'var(--accent)', opacity: 0.6 }} />
            <span className="text-xs" style={{ color: 'var(--text-secondary)' }}>
              物品总数
            </span>
          </div>
          <p
            className="text-2xl font-bold"
            style={{
              color: 'var(--accent)',
              background: 'linear-gradient(135deg, var(--accent), var(--accent-light))',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
            }}
          >
            {totalItems}
          </p>
        </div>

        {/* Total price card */}
        <div
          className="p-3"
          style={{
            borderRadius: 'var(--radius-md)',
            background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.08), rgba(16, 185, 129, 0.03))',
            border: '1px solid var(--border-glass)',
            transition: 'all 0.2s ease',
          }}
        >
          <div className="flex items-center gap-1.5 mb-1">
            <Wallet size={14} style={{ color: 'var(--success)', opacity: 0.6 }} />
            <span className="text-xs" style={{ color: 'var(--text-secondary)' }}>
              总价值
            </span>
          </div>
          <p
            className="text-2xl font-bold"
            style={{
              color: 'var(--success)',
            }}
          >
            {formatPrice(totalPrice)}
          </p>
        </div>
      </div>
    </div>
  );
}
