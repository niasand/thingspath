import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { MapPin, Calendar } from 'lucide-react';
import type { Item } from '../types/item';
import { formatRelativeTime } from '../utils/dateUtils';
import { formatPrice, formatDailyCost, calculateDailyCost } from '../utils/numberUtils';
import { getImageUrl, isWebImage } from '../services/imageStorage';
import ItemImagePlaceholder from './ItemImagePlaceholder';

interface Props {
  item: Item;
  selectionMode?: boolean;
  selected?: boolean;
  onSelect?: () => void;
  onDelete?: () => void;
}

export default function ItemCard({ item, selectionMode, selected, onSelect }: Props) {
  const navigate = useNavigate();
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [imageError, setImageError] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const primaryImage = item.imagePaths?.[0] || item.imagePath;
    if (primaryImage && isWebImage(primaryImage)) {
      getImageUrl(primaryImage).then(url => {
        if (!cancelled && url) setImageUrl(url);
      });
    }
    return () => { cancelled = true; };
  }, [item.imagePaths, item.imagePath]);

  const dailyCost = calculateDailyCost(item.purchasePrice, item.usageDays);

  const handleClick = () => {
    if (selectionMode && onSelect) {
      onSelect();
    } else {
      navigate(`/item/${item.id}`);
    }
  };

  return (
    <div
      className={`relative bg-surface rounded-2xl shadow-sm hover:shadow-md transition-all duration-200
        ${selected ? 'ring-2 ring-primary' : ''}
        cursor-pointer hover:border-l-[3px] hover:border-l-primary`}
      onClick={handleClick}
    >
      <div className="flex p-4 gap-3.5">
          {/* Image */}
          <div className="shrink-0">
            {imageUrl && !imageError ? (
              <img
                src={imageUrl}
                alt={item.name}
                className="w-16 h-16 rounded-2xl object-cover"
                onError={() => setImageError(true)}
              />
            ) : (
              <ItemImagePlaceholder name={item.name} size={64} />
            )}
          </div>

          {/* Content */}
          <div className="flex-1 min-w-0">
            <div className="flex items-start justify-between gap-2 mb-1">
              <h3 className="font-semibold text-[15px] text-text truncate">{item.name}</h3>
              {item.purchasePrice > 0 && (
                <span className="shrink-0 text-xs font-semibold text-primary bg-primary/8 px-2 py-0.5 rounded-full">
                  {formatPrice(item.purchasePrice)}
                </span>
              )}
            </div>

            <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-text-secondary mb-1.5">
              {item.location && (
                <span className="flex items-center gap-1">
                  <MapPin size={12} />
                  <span className="truncate max-w-[120px]">{item.location}</span>
                </span>
              )}
              {item.purchaseDate && (
                <span className="flex items-center gap-1">
                  <Calendar size={12} />
                  {formatRelativeTime(item.purchaseDate)}
                </span>
              )}
            </div>

            <div className="flex items-center gap-2 flex-wrap">
              {dailyCost !== null && (
                <span className="text-[11px] font-medium text-secondary bg-secondary-light/40 px-1.5 py-0.5 rounded">
                  {formatDailyCost(dailyCost)}
                </span>
              )}
              {item.usageDays !== null && item.usageDays > 0 && (
                <span className="text-[11px] text-text-tertiary">
                  {item.usageDays} 天
                </span>
              )}
            </div>

            {item.tags.length > 0 && (
              <div className="flex gap-1 mt-1.5 flex-wrap">
                {item.tags.slice(0, 3).map(tag => (
                  <span key={tag} className="text-[10px] px-1.5 py-0.5 rounded bg-gray-light text-text-secondary">
                    {tag}
                  </span>
                ))}
                {item.tags.length > 3 && (
                  <span className="text-[10px] px-1.5 py-0.5 rounded bg-gray-light text-text-tertiary">
                    +{item.tags.length - 3}
                  </span>
                )}
              </div>
            )}
          </div>

          {/* Checkbox in selection mode */}
          {selectionMode && (
            <div className="flex items-center">
              <div
                className={`w-5 h-5 rounded-md border-2 flex items-center justify-center transition-all ${
                  selected
                    ? 'bg-primary border-primary'
                    : 'border-gray-300 group-hover:border-primary/50'
                }`}
              >
                {selected && (
                  <svg className="w-3 h-3 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                  </svg>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
  );
}
