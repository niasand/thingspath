import { useState, useEffect, useRef } from 'react';
import { ChevronLeft, ChevronRight, X, Expand } from 'lucide-react';
import { getImageUrl, isWebImage } from '../services/imageStorage';
import ItemImagePlaceholder from './ItemImagePlaceholder';

interface Props {
  imagePaths: string[];
  itemName: string;
}

export default function MultiImageViewer({ imagePaths, itemName }: Props) {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [urls, setUrls] = useState<Map<string, string>>(new Map());
  const [errors, setErrors] = useState<Set<string>>(new Set());
  const [fullscreen, setFullscreen] = useState(false);
  const touchStartX = useRef(0);

  useEffect(() => {
    imagePaths.forEach(async (path) => {
      if (isWebImage(path) && !urls.has(path)) {
        try {
          const url = await getImageUrl(path);
          if (url) setUrls(prev => new Map(prev).set(path, url));
        } catch {
          setErrors(prev => new Set(prev).add(path));
        }
      }
    });
  }, [imagePaths]);

  if (imagePaths.length === 0) return null;

  const currentPath = imagePaths[currentIndex];
  const currentUrl = urls.get(currentPath);
  const hasError = errors.has(currentPath);

  const goPrev = () => setCurrentIndex(i => Math.max(0, i - 1));
  const goNext = () => setCurrentIndex(i => Math.min(imagePaths.length - 1, i + 1));

  const handleTouchStart = (e: React.TouchEvent) => { touchStartX.current = e.touches[0].clientX; };
  const handleTouchEnd = (e: React.TouchEvent) => {
    const diff = touchStartX.current - e.changedTouches[0].clientX;
    if (diff > 50) goNext();
    else if (diff < -50) goPrev();
  };

  const content = (
    <div className="relative">
      <div
        className="flex items-center justify-center bg-gray-light rounded-2xl overflow-hidden"
        onTouchStart={handleTouchStart}
        onTouchEnd={handleTouchEnd}
      >
        {currentUrl && !hasError ? (
          <img
            src={currentUrl}
            alt={`${itemName} ${currentIndex + 1}`}
            className={`object-contain ${fullscreen ? 'max-h-[80vh] max-w-full' : 'max-h-64 w-full'}`}
          />
        ) : (
          <ItemImagePlaceholder name={itemName} size={fullscreen ? 200 : 120} />
        )}
      </div>

      {/* Navigation arrows */}
      {imagePaths.length > 1 && (
        <>
          {currentIndex > 0 && (
            <button
              onClick={goPrev}
              className="absolute left-2 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-black/20
                         backdrop-blur-sm text-white flex items-center justify-center hover:bg-black/30 transition-colors"
            >
              <ChevronLeft size={18} />
            </button>
          )}
          {currentIndex < imagePaths.length - 1 && (
            <button
              onClick={goNext}
              className="absolute right-2 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-black/20
                         backdrop-blur-sm text-white flex items-center justify-center hover:bg-black/30 transition-colors"
            >
              <ChevronRight size={18} />
            </button>
          )}
        </>
      )}

      {/* Dots */}
      {imagePaths.length > 1 && (
        <div className="flex justify-center gap-1.5 mt-3">
          {imagePaths.map((_, i) => (
            <button
              key={i}
              onClick={() => setCurrentIndex(i)}
              className={`w-2 h-2 rounded-full transition-all ${
                i === currentIndex ? 'bg-primary w-5' : 'bg-border'
              }`}
            />
          ))}
        </div>
      )}

      {/* Counter + fullscreen toggle */}
      <div className="absolute top-2 right-2 flex gap-2">
        {!fullscreen && imagePaths.length > 0 && (
          <button
            onClick={() => setFullscreen(true)}
            className="p-1.5 rounded-lg bg-black/20 backdrop-blur-sm text-white hover:bg-black/30 transition-colors"
          >
            <Expand size={16} />
          </button>
        )}
      </div>
    </div>
  );

  if (fullscreen) {
    return (
      <div className="fixed inset-0 z-50 bg-black/90 flex items-center justify-center p-4">
        <button
          onClick={() => setFullscreen(false)}
          className="absolute top-4 right-4 p-2 rounded-full bg-white/10 text-white hover:bg-white/20 transition-colors"
        >
          <X size={24} />
        </button>
        <div className="absolute top-4 left-4 text-white/70 text-sm">
          {currentIndex + 1} / {imagePaths.length}
        </div>
        {content}
      </div>
    );
  }

  return content;
}
