export const CHART_COLORS = [
  '#6650a4', '#7D5260', '#D0BCFF', '#EFB8C8',
  '#4CAF50', '#FF9800', '#2196F3', '#E91E63',
  '#9C27B0', '#00BCD4', '#FF5722', '#607D8B',
];

export const PRICE_RANGE_COLORS = ['#4CAF50', '#FF9800', '#E91E63'];

export type SortField = 'purchaseDate' | 'name' | 'usageDays' | 'updatedAt' | 'createdAt';

export const SORT_OPTIONS: { field: SortField; label: string }[] = [
  { field: 'updatedAt', label: '最近更新' },
  { field: 'purchaseDate', label: '购买日期' },
  { field: 'name', label: '物品名称' },
  { field: 'usageDays', label: '使用天数' },
  { field: 'createdAt', label: '创建时间' },
];

export const MAX_IMAGES_PER_ITEM = 5;

export const IMAGE_COMPRESS_MAX_WIDTH = 1200;
export const IMAGE_COMPRESS_QUALITY = 0.8;
