import { PackageOpen } from 'lucide-react';

export default function EmptyState() {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center">
      <div className="w-20 h-20 rounded-3xl bg-primary/5 flex items-center justify-center mb-4">
        <PackageOpen size={36} className="text-primary/40" />
      </div>
      <h3 className="text-lg font-semibold text-text mb-1">还没有物品</h3>
      <p className="text-sm text-text-secondary">点击右下角 + 按钮添加你的第一个物品</p>
    </div>
  );
}
