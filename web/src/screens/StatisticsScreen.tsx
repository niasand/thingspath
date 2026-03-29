import { useMemo } from 'react';
import { useApp } from '../context/AppContext';
import StatisticsHeader from '../components/StatisticsHeader';
import TopBar from '../components/TopBar';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import type { TagStat, PriceRangeStat, LocationStat } from '../types/statistics';

const GLASS_CHART_COLORS = ['#7C3AED', '#A855F7', '#EC4899', '#F59E0B', '#10B981', '#3B82F6', '#6366F1', '#8B5CF6'];

export default function StatisticsScreen() {
  const { state } = useApp();

  const tagStats = useMemo<TagStat[]>(() => {
    const map = new Map<string, number>();
    state.items.forEach(item => item.tags.forEach(tag => map.set(tag, (map.get(tag) || 0) + 1)));
    return [...map.entries()]
      .sort((a, b) => b[1] - a[1])
      .slice(0, 8)
      .map(([name, value], i) => ({ name, value, color: GLASS_CHART_COLORS[i % GLASS_CHART_COLORS.length] }));
  }, [state.items]);

  const priceStats = useMemo<PriceRangeStat[]>(() => {
    const ranges: PriceRangeStat[] = [
      { range: '< \u00a51,000', min: 0, max: 1000, count: 0, color: GLASS_CHART_COLORS[3] },
      { range: '\u00a51,000 - 3,000', min: 1000, max: 3000, count: 0, color: GLASS_CHART_COLORS[2] },
      { range: '> \u00a53,000', min: 3000, max: Infinity, count: 0, color: GLASS_CHART_COLORS[0] },
    ];
    state.items.forEach(item => {
      if (item.purchasePrice <= 0) return;
      for (const r of ranges) {
        if (item.purchasePrice >= r.min && item.purchasePrice < r.max) { r.count++; break; }
      }
    });
    return ranges.filter(r => r.count > 0);
  }, [state.items]);

  const locationStats = useMemo<LocationStat[]>(() => {
    const map = new Map<string, number>();
    state.items.forEach(item => {
      if (item.location) map.set(item.location, (map.get(item.location) || 0) + 1);
    });
    return [...map.entries()]
      .sort((a, b) => b[1] - a[1])
      .slice(0, 6)
      .map(([name, value], i) => ({ name, value, color: GLASS_CHART_COLORS[(i + 5) % GLASS_CHART_COLORS.length] }));
  }, [state.items]);

  const hasData = state.items.length > 0;

  return (
    <div>
      <TopBar title="统计" />

      <StatisticsHeader totalItems={state.totalItemCount} totalPrice={state.totalPrice} />

      {!hasData ? (
        <div className="text-center py-12 text-sm" style={{ color: 'var(--text-secondary)' }}>暂无数据</div>
      ) : (
        <div className="space-y-4">
          {tagStats.length > 0 && (
            <ChartCard title="标签分布" total={state.items.length}>
              <DonutChart data={tagStats} />
            </ChartCard>
          )}

          {priceStats.length > 0 && (
            <ChartCard title="价格区间" total={state.items.filter(i => i.purchasePrice > 0).length}>
              <DonutChart data={priceStats.map(r => ({ name: r.range, value: r.count, color: r.color }))} />
            </ChartCard>
          )}

          {locationStats.length > 0 && (
            <ChartCard title="位置分布" total={state.items.filter(i => i.location).length}>
              <DonutChart data={locationStats} />
            </ChartCard>
          )}
        </div>
      )}
    </div>
  );
}

function ChartCard({ title, total, children }: { title: string; total: number; children: React.ReactNode }) {
  return (
    <div
      className="p-5 shadow-lg"
      style={{
        background: 'var(--glass-bg)',
        backdropFilter: 'var(--glass-blur)',
        WebkitBackdropFilter: 'var(--glass-blur)',
        border: '1px solid var(--glass-border)',
        borderRadius: '20px',
        boxShadow: '0 8px 32px var(--shadow-color, rgba(0,0,0,0.08))',
      }}
    >
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>{title}</h3>
        <span
          className="text-xs px-2.5 py-0.5 rounded-full"
          style={{
            background: 'var(--glass-bg)',
            border: '1px solid var(--glass-border)',
            color: 'var(--text-secondary)',
          }}
        >
          {total} 项
        </span>
      </div>
      {children}
    </div>
  );
}

function DonutChart({ data }: { data: { name: string; value: number; color: string }[] }) {
  return (
    <div className="flex flex-col sm:flex-row items-center gap-5">
      <div className="w-48 h-48 shrink-0">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={data}
              cx="50%"
              cy="50%"
              innerRadius={50}
              outerRadius={80}
              paddingAngle={3}
              dataKey="value"
              stroke="none"
              strokeWidth={0}
            >
              {data.map((entry, i) => (
                <Cell key={i} fill={entry.color} />
              ))}
            </Pie>
            <Tooltip
              formatter={(value: number, name: string) => [value, name]}
              contentStyle={{
                borderRadius: '16px',
                border: '1px solid var(--glass-border)',
                fontSize: '13px',
                background: 'var(--glass-bg)',
                backdropFilter: 'blur(20px)',
                WebkitBackdropFilter: 'blur(20px)',
                color: 'var(--text-primary)',
                boxShadow: '0 8px 32px var(--shadow-color, rgba(0,0,0,0.12))',
              }}
            />
          </PieChart>
        </ResponsiveContainer>
      </div>
      <div className="flex flex-wrap gap-x-4 gap-y-2 justify-center sm:justify-start">
        {data.map((item, i) => (
          <div key={i} className="flex items-center gap-2">
            <span
              className="w-2.5 h-2.5 rounded-full shrink-0"
              style={{
                backgroundColor: item.color,
                boxShadow: `0 0 8px ${item.color}40`,
              }}
            />
            <span className="text-xs truncate max-w-[100px]" style={{ color: 'var(--text-secondary)' }}>{item.name}</span>
            <span className="text-xs font-semibold" style={{ color: 'var(--text-primary)' }}>{item.value}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
