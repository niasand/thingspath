import { db } from './database';
import type { Item } from '../types/item';

class ItemRepository {
  async getAllItems(): Promise<Item[]> {
    return db.items.orderBy('updatedAt').reverse().toArray();
  }

  async getItemById(id: number): Promise<Item | undefined> {
    return db.items.get(id);
  }

  async addItem(item: Omit<Item, 'id'>): Promise<number> {
    return db.items.add(item as Item);
  }

  async updateItem(item: Item): Promise<void> {
    await db.items.put({ ...item, updatedAt: Date.now() });
  }

  async deleteItem(id: number): Promise<void> {
    await db.transaction('rw', db.items, db.images, async () => {
      await db.items.delete(id);
      await db.images.where('itemId').equals(id).delete();
    });
  }

  async deleteItems(ids: number[]): Promise<void> {
    await db.transaction('rw', db.items, db.images, async () => {
      await db.items.bulkDelete(ids);
      await db.images.where('itemId').anyOf(ids).delete();
    });
  }

  async insertItems(items: Omit<Item, 'id'>[]): Promise<void> {
    await db.items.bulkAdd(items as Item[]);
  }

  async getAllTags(): Promise<string[]> {
    const allItems = await db.items.toArray();
    return [...new Set(allItems.flatMap(item => item.tags).filter(Boolean))].sort();
  }
}

export const itemRepository = new ItemRepository();
