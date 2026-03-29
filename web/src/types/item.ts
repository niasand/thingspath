/** Mirrors Android Item.kt exactly for JSON compatibility */
export interface Item {
  id: number;
  name: string;
  imagePath: string | null;
  imagePaths: string[];
  location: string | null;
  purchaseDate: number | null;
  purchasePrice: number;
  usageDays: number | null;
  note: string | null;
  tags: string[];
  createdAt: number;
  updatedAt: number;
}

export interface ImageRecord {
  id?: number;
  itemId: number;
  blob: Blob;
  mimeType: string;
  createdAt: number;
}
