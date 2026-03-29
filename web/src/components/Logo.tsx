export default function Logo({ size = 40 }: { size?: number }) {
  return (
    <div
      className="rounded-2xl bg-primary flex items-center justify-center text-white font-bold shrink-0"
      style={{ width: size, height: size, fontSize: size * 0.35 }}
    >
      TP
    </div>
  );
}
