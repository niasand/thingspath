export default function ItemImagePlaceholder({ name, size = 64 }: { name: string; size?: number }) {
  const initial = name.charAt(0).toUpperCase();

  const gradients = [
    'linear-gradient(135deg, rgba(124, 58, 237, 0.12), rgba(167, 139, 250, 0.08))',
    'linear-gradient(135deg, rgba(236, 72, 153, 0.12), rgba(244, 114, 182, 0.08))',
    'linear-gradient(135deg, rgba(245, 158, 11, 0.12), rgba(251, 191, 36, 0.08))',
    'linear-gradient(135deg, rgba(59, 130, 246, 0.12), rgba(96, 165, 250, 0.08))',
    'linear-gradient(135deg, rgba(16, 185, 129, 0.12), rgba(52, 211, 153, 0.08))',
    'linear-gradient(135deg, rgba(239, 68, 68, 0.12), rgba(248, 113, 113, 0.08))',
    'linear-gradient(135deg, rgba(168, 85, 247, 0.12), rgba(192, 132, 252, 0.08))',
    'linear-gradient(135deg, rgba(20, 184, 166, 0.12), rgba(45, 212, 191, 0.08))',
  ];

  const textColors = [
    'var(--accent)',
    'var(--danger)',
    'var(--warning)',
    'var(--accent)',
    'var(--success)',
    'var(--danger)',
    'var(--accent)',
    'var(--success)',
  ];

  const idx = name.charCodeAt(0) % gradients.length;

  return (
    <div
      className="flex items-center justify-center shrink-0 select-none"
      style={{
        width: size,
        height: size,
        borderRadius: 'var(--radius-lg)',
        background: gradients[idx],
        backdropFilter: 'blur(var(--blur-sm))',
        WebkitBackdropFilter: 'blur(var(--blur-sm))',
        border: '1px solid var(--border-glass)',
        fontSize: size * 0.35,
        color: textColors[idx],
        fontWeight: 600,
      }}
    >
      {initial}
    </div>
  );
}
