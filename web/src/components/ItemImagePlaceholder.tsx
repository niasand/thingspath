export default function ItemImagePlaceholder({ name, size = 64 }: { name: string; size?: number }) {
  const initial = name.charAt(0).toUpperCase();
  const bgColors = [
    '#E8DEF8', '#FFD8E4', '#FFDDB3', '#D0BCFF',
    '#CCC2DC', '#BBD0FF', '#C8FACD', '#FFDBC5',
  ];
  const bgColor = bgColors[name.charCodeAt(0) % bgColors.length];

  return (
    <div
      className="rounded-2xl flex items-center justify-center shrink-0 select-none"
      style={{
        width: size,
        height: size,
        backgroundColor: bgColor,
        fontSize: size * 0.35,
        color: '#6650a4',
        fontWeight: 600,
      }}
    >
      {initial}
    </div>
  );
}
