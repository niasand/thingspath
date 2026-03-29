import { useMemo, useState, useEffect } from 'react';
import type { Item } from '../types/item';
import type { SortField } from '../utils/constants';
import { useApp } from '../context/AppContext';

function applySorting(items: Item[], field: SortField, ascending: boolean): Item[] {
  const sorted = [...items];

  switch (field) {
    case 'purchaseDate':
      sorted.sort((a, b) => {
        const va = a.purchaseDate ?? (ascending ? Infinity : -Infinity);
        const vb = b.purchaseDate ?? (ascending ? Infinity : -Infinity);
        const cmp = va < vb ? -1 : va > vb ? 1 : 0;
        return ascending ? cmp : -cmp;
      });
      break;
    case 'name':
      sorted.sort((a, b) => {
        const cmp = a.name.toLowerCase().localeCompare(b.name.toLowerCase());
        return ascending ? cmp : -cmp;
      });
      break;
    case 'usageDays':
      sorted.sort((a, b) => {
        const va = a.usageDays ?? (ascending ? Infinity : -Infinity);
        const vb = b.usageDays ?? (ascending ? Infinity : -Infinity);
        const cmp = va < vb ? -1 : va > vb ? 1 : 0;
        return ascending ? cmp : -cmp;
      });
      break;
    case 'updatedAt':
      sorted.sort((a, b) => ascending ? a.updatedAt - b.updatedAt : b.updatedAt - a.updatedAt);
      break;
    case 'createdAt':
      sorted.sort((a, b) => ascending ? a.createdAt - b.createdAt : b.createdAt - a.createdAt);
      break;
  }

  // Secondary sort by updatedAt desc
  sorted.sort((a, b) => b.updatedAt - a.updatedAt);
  return sorted;
}

export function useItems() {
  const { state } = useApp();
  const [debouncedQuery, setDebouncedQuery] = useState(state.searchQuery);
  const pageSize = 10;

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedQuery(state.searchQuery), 300);
    return () => clearTimeout(timer);
  }, [state.searchQuery]);

  const filteredAndSorted = useMemo(() => {
    let result = state.items;

    if (debouncedQuery) {
      const q = debouncedQuery.toLowerCase();
      result = result.filter(
        i =>
          i.name.toLowerCase().includes(q) ||
          (i.note?.toLowerCase().includes(q) ?? false) ||
          (i.location?.toLowerCase().includes(q) ?? false),
      );
    }

    if (state.selectedTags.size > 0) {
      result = result.filter(i => i.tags.some(t => state.selectedTags.has(t)));
    }

    return applySorting(result, state.sortField, state.sortAscending);
  }, [state.items, debouncedQuery, state.selectedTags, state.sortField, state.sortAscending]);

  const pageCount = Math.ceil(filteredAndSorted.length / pageSize);

  return {
    items: filteredAndSorted,
    pageCount,
    pageSize,
  };
}
