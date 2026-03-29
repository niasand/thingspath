import Dexie, { type Table } from 'dexie';
import type { Item, ImageRecord } from '../types/item';

class ThingsPathDB extends Dexie {
  items!: Table<Item>;
  images!: Table<ImageRecord>;

  constructor() {
    super('ThingsPathDB');
    this.version(1).stores({
      items: '++id, name, location, *tags, purchaseDate, createdAt, updatedAt',
      images: '++id, itemId',
    });
  }
}

export const db = new ThingsPathDB();
