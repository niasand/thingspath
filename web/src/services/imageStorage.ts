import { db } from '../db/database';
import type { ImageRecord } from '../types/item';
import { IMAGE_COMPRESS_MAX_WIDTH, IMAGE_COMPRESS_QUALITY } from '../utils/constants';

const objectUrlCache = new Map<string, string>();

export function getImageRecordId(imagePath: string): number | null {
  if (imagePath.startsWith('img:')) {
    const id = parseInt(imagePath.slice(4), 10);
    return isNaN(id) ? null : id;
  }
  return null;
}

export function isWebImage(imagePath: string): boolean {
  return imagePath.startsWith('img:');
}

export async function saveImage(itemId: number, file: File): Promise<number> {
  const compressedBlob = await compressImage(file);
  const record: ImageRecord = {
    itemId,
    blob: compressedBlob,
    mimeType: file.type || 'image/jpeg',
    createdAt: Date.now(),
  };
  const id = await db.images.add(record);
  return id as number;
}

export async function saveImageBlob(itemId: number, blob: Blob, mimeType: string): Promise<number> {
  const record: ImageRecord = { itemId, blob, mimeType, createdAt: Date.now() };
  const id = await db.images.add(record);
  return id as number;
}

export async function deleteImage(recordId: number): Promise<void> {
  const cached = objectUrlCache.get(`img:${recordId}`);
  if (cached) {
    URL.revokeObjectURL(cached);
    objectUrlCache.delete(`img:${recordId}`);
  }
  await db.images.delete(recordId);
}

export async function getImageUrl(imagePath: string): Promise<string | null> {
  const recordId = getImageRecordId(imagePath);
  if (recordId === null) return null;

  const cached = objectUrlCache.get(imagePath);
  if (cached) return cached;

  const record = await db.images.get(recordId);
  if (!record) return null;

  const url = URL.createObjectURL(record.blob);
  objectUrlCache.set(imagePath, url);
  return url;
}

export async function getImageBlob(imagePath: string): Promise<Blob | null> {
  const recordId = getImageRecordId(imagePath);
  if (recordId === null) return null;
  const record = await db.images.get(recordId);
  return record?.blob ?? null;
}

function compressImage(file: File): Promise<Blob> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => {
      const canvas = document.createElement('canvas');
      let width = img.width;
      let height = img.height;
      if (width > IMAGE_COMPRESS_MAX_WIDTH) {
        height = Math.round((height * IMAGE_COMPRESS_MAX_WIDTH) / width);
        width = IMAGE_COMPRESS_MAX_WIDTH;
      }
      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext('2d')!;
      ctx.drawImage(img, 0, 0, width, height);
      canvas.toBlob(
        blob => blob ? resolve(blob) : resolve(file),
        file.type || 'image/jpeg',
        IMAGE_COMPRESS_QUALITY,
      );
    };
    img.onerror = reject;
    img.src = URL.createObjectURL(file);
  });
}
