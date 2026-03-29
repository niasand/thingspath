import { BarChart3, Wallet } from 'lucide-react';
import { formatPrice } from '../utils/numberUtils';

interface Props {
  totalItems: number;
  totalPrice: number;
}

export default function StatisticsHeader({ totalItems, totalPrice }: Props) {
  return (
    <div className="bg-surface rounded-2xl p-4 shadow-sm mb-4">
      <div className="flex items-center gap-2 mb-3">
        <BarChart3 size={18} className="text-primary" />
        <span className="text-sm font-semibold text-text">概览</span>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div className="bg-primary/5 rounded-xl p-3">
          <div className="flex items-center gap-1.5 mb-1">
            <BarChart3 size={14} className="text-primary/60" />
            <span className="text-xs text-text-secondary">物品总数</span>
          </div>
          <p className="text-2xl font-bold text-primary">{totalItems}</p>
        </div>
        <div className="bg-secondary/5 rounded-xl p-3">
          <div className="flex items-center gap-1.5 mb-1">
            <Wallet size={14} className="text-secondary/60" />
            <span className="text-xs text-text-secondary">总价值</span>
          </div>
          <p className="text-2xl font-bold text-secondary">{formatPrice(totalPrice)}</p>
        </div>
      </div>
    </div>
  );
}
