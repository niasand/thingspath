export default function SkeletonCard() {
  return (
    <div
      className="rounded-2xl mb-3 shadow-sm"
      style={{
        background: 'var(--bg-surface)',
        backdropFilter: 'blur(var(--blur-md))',
        WebkitBackdropFilter: 'blur(var(--blur-md))',
        border: '1px solid var(--border-glass)',
      }}
    >
      <div className="flex p-4 gap-3.5">
        {/* Thumbnail skeleton */}
        <div className="shrink-0">
          <div
            className="skeleton"
            style={{ width: 64, height: 64, borderRadius: 'var(--radius-lg, 16px)' }}
          />
        </div>

        {/* Content skeleton */}
        <div className="flex-1 min-w-0 space-y-2.5">
          {/* Title line */}
          <div
            className="skeleton"
            style={{ width: '60%', height: 15, borderRadius: 6 }}
          />

          {/* Meta line (location + date) */}
          <div className="flex items-center gap-3">
            <div
              className="skeleton"
              style={{ width: 72, height: 12, borderRadius: 4 }}
            />
            <div
              className="skeleton"
              style={{ width: 56, height: 12, borderRadius: 4 }}
            />
          </div>

          {/* Tag row */}
          <div className="flex items-center gap-1.5">
            <div
              className="skeleton"
              style={{ width: 40, height: 18, borderRadius: 6 }}
            />
            <div
              className="skeleton"
              style={{ width: 52, height: 18, borderRadius: 6 }}
            />
          </div>
        </div>
      </div>
    </div>
  );
}
