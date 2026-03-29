import { useRef, useState, useCallback, useEffect, type ReactNode } from 'react';

interface SwipeableItemProps {
  children: ReactNode;
  onDelete: () => void;
  /** Width of the revealed delete action in px (default 80) */
  actionWidth?: number;
  /** Minimum swipe distance to reveal the action (default 60) */
  threshold?: number;
}

export default function SwipeableItem({
  children,
  onDelete,
  actionWidth = 80,
  threshold = 60,
}: SwipeableItemProps) {
  const [offsetX, setOffsetX] = useState(0);
  const [isRevealed, setIsRevealed] = useState(false);
  const [isDragging, setIsDragging] = useState(false);

  const startX = useRef(0);
  const currentX = useRef(0);
  const containerRef = useRef<HTMLDivElement>(null);

  // Track whether this is a touch device
  const isTouchDevice = useRef(false);

  // Close revealed action when tapping elsewhere
  useEffect(() => {
    if (!isRevealed) return;
    const handler = (e: TouchEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsRevealed(false);
        setOffsetX(0);
      }
    };
    document.addEventListener('touchstart', handler, { passive: true });
    return () => document.removeEventListener('touchstart', handler);
  }, [isRevealed]);

  const clamp = useCallback(
    (x: number) => {
      // Allow dragging from -actionWidth to a small overshoot on the right (20px)
      const min = -actionWidth;
      const max = 20;
      return Math.max(min, Math.min(max, x));
    },
    [actionWidth],
  );

  const onTouchStart = useCallback((e: React.TouchEvent) => {
    isTouchDevice.current = true;
    startX.current = e.touches[0].clientX;
    currentX.current = 0;
    // If already revealed and swiping right (closing), use revealed state as base
  }, []);

  const onTouchMove = useCallback(
    (e: React.TouchEvent) => {
      const dx = e.touches[0].clientX - startX.current;
      currentX.current = dx;
      setIsDragging(true);

      if (isRevealed) {
        // Base offset is -actionWidth, user drags relative to that
        const total = -actionWidth + dx;
        setOffsetX(clamp(total));
      } else {
        setOffsetX(clamp(dx));
      }
    },
    [isRevealed, actionWidth, clamp],
  );

  const onTouchEnd = useCallback(() => {
    setIsDragging(false);

    if (isRevealed) {
      // Already revealed — check if user swiped enough to close
      if (currentX.current > threshold * 0.5) {
        setIsRevealed(false);
        setOffsetX(0);
      } else {
        // Snap back to revealed
        setOffsetX(-actionWidth);
      }
    } else {
      // Not revealed — check if user swiped enough to reveal
      if (currentX.current < -threshold) {
        setIsRevealed(true);
        setOffsetX(-actionWidth);
      } else {
        setOffsetX(0);
      }
    }

    currentX.current = 0;
  }, [isRevealed, actionWidth, threshold]);

  const handleDelete = useCallback(() => {
    onDelete();
    // Reset after delete
    setIsRevealed(false);
    setOffsetX(0);
  }, [onDelete]);

  // Compute transform: the visible offset plus the revealed offset
  const transformX = isDragging ? offsetX : isRevealed ? -actionWidth : 0;
  const shouldTransition = !isDragging;

  return (
    <div
      ref={containerRef}
      className="relative overflow-hidden rounded-2xl"
      style={{ touchAction: 'pan-y' }}
    >
      {/* Delete action behind the card */}
      <div
        className="absolute inset-y-0 right-0 flex items-center justify-end pr-4 rounded-r-2xl overflow-hidden"
        style={{
          width: `${actionWidth}px`,
          background: 'var(--danger)',
          opacity: transformX < -20 ? 1 : 0,
          transition: shouldTransition ? 'opacity 0.2s ease' : 'none',
        }}
      >
        <button
          onClick={handleDelete}
          className="text-white font-medium text-sm flex items-center gap-1.5 py-2 px-3
                     rounded-xl transition-all duration-150 active:scale-95"
          style={{
            background: 'rgba(255,255,255,0.15)',
            backdropFilter: 'blur(8px)',
            WebkitBackdropFilter: 'blur(8px)',
          }}
        >
          删除
        </button>
      </div>

      {/* Foreground card */}
      <div
        style={{
          transform: `translateX(${transformX}px)`,
          transition: shouldTransition
            ? 'transform 0.3s cubic-bezier(0.32, 0.72, 0, 1)'
            : 'none',
          willChange: isDragging ? 'transform' : 'auto',
        }}
        onTouchStart={onTouchStart}
        onTouchMove={onTouchMove}
        onTouchEnd={onTouchEnd}
      >
        {children}
      </div>
    </div>
  );
}
