import { format, formatDistanceToNow, parseISO } from 'date-fns';
import { zhCN } from 'date-fns/locale';

export function formatDate(timestamp: number | null): string {
  if (!timestamp) return '';
  return format(new Date(timestamp), 'yyyy-MM-dd', { locale: zhCN });
}

export function formatRelativeTime(timestamp: number | null): string {
  if (!timestamp) return '';
  return formatDistanceToNow(new Date(timestamp), { addSuffix: true, locale: zhCN });
}

export function parseDateString(dateString: string): number | null {
  try {
    return parseISO(dateString).getTime();
  } catch {
    return null;
  }
}

export function todayString(): string {
  return format(new Date(), 'yyyy-MM-dd');
}

export function calculateUsageDays(purchaseDate: number | null): number | null {
  if (!purchaseDate) return null;
  const diff = Date.now() - purchaseDate;
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  return days >= 0 ? days : null;
}
