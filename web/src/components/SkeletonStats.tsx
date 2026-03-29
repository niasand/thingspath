export default function SkeletonStats() {
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
      {/* Title bar */}
      <div className="flex items-center gap-2 mb-3">
        <div className="skeleton" style={{ width: 18, height: 18, borderRadius: 4 }} />
        <div className="skeleton" style={{ width: 36, height: 16, borderRadius: 4 }} />
      </div>

      {/* Two-column stat cards */}
      <div className="grid grid-cols-2 gap-3">
        {[0, 1].map(i => (
          <div
            key={i}
            className="p-3"
            style={{
              borderRadius: 'var(--radius-md)',
              background: 'linear-gradient(135deg, var(--accent-soft), rgba(124, 58, 237, 0.05))',
              border: '1px solid var(--border-glass)',
            }}
          >
            <div className="flex items-center gap-1.5 mb-1">
              <div className="skeleton" style={{ width: 14, height: 14, borderRadius: 3 }} />
              <div className="skeleton" style={{ width: 48, height: 12, borderRadius: 4 }} />
            </div>
            <div className="skeleton" style={{ width: '55%', height: 28, borderRadius: 6 }} />
          </div>
        ))}
      </div>
    </div>
  );
}

export function SkeletonChart() {
  return (
    <div
      className="p-5"
      style={{
        background: 'var(--glass-bg)',
        backdropFilter: 'var(--glass-blur)',
        WebkitBackdropFilter: 'var(--glass-blur)',
        border: '1px solid var(--glass-border)',
        borderRadius: '20px',
        boxShadow: '0 8px 32px var(--shadow-color, rgba(0,0,0,0.08))',
      }}
    >
      {/* Chart title bar */}
      <div className="flex items-center justify-between mb-4">
        <div className="skeleton" style={{ width: 64, height: 16, borderRadius: 4 }} />
        <div className="skeleton" style={{ width: 40, height: 20, borderRadius: 10 }} />
      </div>

      {/* Chart area placeholder */}
      <div
        className="skeleton flex items-center justify-center"
        style={{ height: 200, borderRadius: 'var(--radius-md)' }}
      />
    </div>
  );
}
