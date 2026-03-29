import type { Item } from '../types/item';
import { itemRepository } from '../db/repository';
import { isWebImage } from './imageStorage';

export function exportToJson(items: Item[]): string {
  return JSON.stringify(items);
}

export function downloadJson(items: Item[], filename?: string): void {
  const json = exportToJson(items);
  const blob = new Blob([json], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename || `thingspath_backup_${new Date().toISOString().slice(0, 10)}.json`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

export async function importFromJson(jsonString: string): Promise<number> {
  const parsed = JSON.parse(jsonString);
  const items: Item[] = Array.isArray(parsed) ? parsed : [parsed];

  if (items.length === 0) throw new Error('导入数据为空');

  const itemsToInsert = items.map(item => ({
    ...item,
    id: 0,
    imagePaths: (item.imagePaths || []).map((p: string) =>
      isWebImage(p) ? p : p,
    ),
  }));

  await itemRepository.insertItems(itemsToInsert);
  return itemsToInsert.length;
}

export function readJsonFile(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = () => reject(new Error('读取文件失败'));
    reader.readAsText(file);
  });
}
