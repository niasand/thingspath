export function formatPrice(price: number): string {
  return price > 0 ? `¥${price.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}` : '';
}

export function calculateDailyCost(price: number, usageDays: number | null): number | null {
  if (price <= 0 || !usageDays || usageDays <= 0) return null;
  return price / usageDays;
}

export function formatDailyCost(dailyCost: number | null): string {
  if (dailyCost === null) return '';
  return `¥${dailyCost.toFixed(2)}/天`;
}
