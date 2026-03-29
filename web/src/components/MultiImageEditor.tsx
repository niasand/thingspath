import { useState, useRef, useEffect } from 'react';
import { Plus, X, Image as ImageIcon } from 'lucide-react';
import { getImageUrl, isWebImage, saveImage } from '../services/imageStorage';
import { MAX_IMAGES_PER_ITEM } from '../utils/constants';
import ItemImagePlaceholder from './ItemImagePlaceholder';

interface Props {
  imagePaths: string[];
  itemId: number | null;
  itemName: string;
  onChange: (paths: string[]) => void;
}

export default function MultiImageEditor({ imagePaths, itemId, itemName, onChange }: Props) {
  const [images, setImages] = useState<Map<string, string>>(new Map());
  const [errors, setErrors] = useState<Map<string, boolean>>(new Map());
  const fileRef = useRef<HTMLInputElement>(null);

  // Load image URLs
  useEffect(() => {
    imagePaths.forEach(async (path) => {
      if (isWebImage(path)) {
        try {
          const url = await getImageUrl(path);
          if (url) setImages(prev => new Map(prev).set(path, url));
        } catch {
          setErrors(prev => new Map(prev).set(path, true));
        }
      }
    });
  }, [imagePaths]);

  const handleAdd = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files) return;

    const remaining = MAX_IMAGES_PER_ITEM - imagePaths.length;
    const toProcess = Array.from(files).slice(0, remaining);

    for (const file of toProcess) {
      const tempId = itemId ?? 0;
      const recordId = await saveImage(tempId, file);
      const newPath = `img:${recordId}`;
      const url = await getImageUrl(newPath);
      if (url) setImages(prev => new Map(prev).set(newPath, url));
      onChange([...imagePaths, newPath]);
    }

    if (fileRef.current) fileRef.current.value = '';
  };

  const handleRemove = async (index: number) => {
    const path = imagePaths[index];
    if (isWebImage(path)) {
      const recordId = parseInt(path.slice(4), 10);
      const { deleteImage } = await import('../services/imageStorage');
      await deleteImage(recordId);
    }
    const newPaths = imagePaths.filter((_, i) => i !== index);
    onChange(newPaths);
    setImages(prev => {
      const next = new Map(prev);
      next.delete(path);
      return next;
    });
  };

  return (
    <div className="space-y-2">
      <label className="block text-sm font-medium text-text">
        图片 <span className="text-text-tertiary font-normal">({imagePaths.length}/{MAX_IMAGES_PER_ITEM})</span>
      </label>
      <div className="flex gap-2 flex-wrap">
        {imagePaths.map((path, idx) => {
          const url = images.get(path);
          const hasError = errors.get(path);
          return (
            <div key={`${path}-${idx}`} className="relative group">
              {url && !hasError ? (
                <img src={url} alt="" className="w-20 h-20 rounded-xl object-cover" />
              ) : (
                <ItemImagePlaceholder name={itemName} size={80} />
              )}
              <button
                onClick={() => handleRemove(idx)}
                className="absolute -top-1.5 -right-1.5 w-5 h-5 rounded-full bg-error text-white
                           flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
              >
                <X size={12} />
              </button>
            </div>
          );
        })}
        {imagePaths.length < MAX_IMAGES_PER_ITEM && (
          <button
            onClick={() => fileRef.current?.click()}
            className="w-20 h-20 rounded-xl border-2 border-dashed border-border
                       flex flex-col items-center justify-center gap-1 text-text-secondary
                       hover:border-primary/40 hover:text-primary transition-colors"
          >
            <ImageIcon size={18} />
            <Plus size={14} />
          </button>
        )}
      </div>
      <input
        ref={fileRef}
        type="file"
        accept="image/*"
        multiple
        className="hidden"
        onChange={handleAdd}
      />
    </div>
  );
}
