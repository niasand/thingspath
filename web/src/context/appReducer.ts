import type { Item } from '../types/item';
import type { SortField } from '../utils/constants';

export interface AppState {
  items: Item[];
  allTags: string[];
  searchQuery: string;
  selectedTags: Set<string>;
  sortField: SortField;
  sortAscending: boolean;
  isSelectionMode: boolean;
  selectedItemIds: Set<number>;
  showDeleteDialog: boolean;
  itemToDelete: Item | null;
  isLoading: boolean;
  totalItemCount: number;
  totalPrice: number;
  snackbar: { message: string; type: 'success' | 'error' | 'info' } | null;
  isAIProcessing: boolean;
  showAIDialog: boolean;
  isExporting: boolean;
  isImporting: boolean;
}

const initialState: AppState = {
  items: [],
  allTags: [],
  searchQuery: '',
  selectedTags: new Set(),
  sortField: 'updatedAt',
  sortAscending: false,
  isSelectionMode: false,
  selectedItemIds: new Set(),
  showDeleteDialog: false,
  itemToDelete: null,
  isLoading: true,
  totalItemCount: 0,
  totalPrice: 0,
  snackbar: null,
  isAIProcessing: false,
  showAIDialog: false,
  isExporting: false,
  isImporting: false,
};

export type AppAction =
  | { type: 'SET_ITEMS'; items: Item[] }
  | { type: 'SET_ALL_TAGS'; tags: string[] }
  | { type: 'SET_SEARCH_QUERY'; query: string }
  | { type: 'TOGGLE_TAG'; tag: string }
  | { type: 'SET_SORT'; field: SortField }
  | { type: 'TOGGLE_SELECTION_MODE' }
  | { type: 'TOGGLE_ITEM_SELECTION'; id: number }
  | { type: 'SELECT_ALL' }
  | { type: 'SHOW_DELETE_DIALOG'; item: Item | null }
  | { type: 'DISMISS_DELETE_DIALOG' }
  | { type: 'SET_LOADING'; loading: boolean }
  | { type: 'SHOW_SNACKBAR'; message: string; snackbarType: 'success' | 'error' | 'info' }
  | { type: 'DISMISS_SNACKBAR' }
  | { type: 'SET_AI_PROCESSING'; processing: boolean }
  | { type: 'TOGGLE_AI_DIALOG' }
  | { type: 'SET_EXPORTING'; exporting: boolean }
  | { type: 'SET_IMPORTING'; importing: boolean }
  | { type: 'ITEMS_UPDATED' };

export function appReducer(state: AppState, action: AppAction): AppState {
  switch (action.type) {
    case 'SET_ITEMS':
      return {
        ...state,
        items: action.items,
        totalItemCount: action.items.length,
        totalPrice: action.items.reduce((sum, i) => sum + i.purchasePrice, 0),
        isLoading: false,
      };
    case 'SET_ALL_TAGS':
      return { ...state, allTags: action.tags };
    case 'SET_SEARCH_QUERY':
      return { ...state, searchQuery: action.query };
    case 'TOGGLE_TAG': {
      const newTags = new Set(state.selectedTags);
      if (newTags.has(action.tag)) newTags.delete(action.tag);
      else newTags.add(action.tag);
      return { ...state, selectedTags: newTags };
    }
    case 'SET_SORT': {
      const nextAsc = state.sortField === action.field ? !state.sortAscending : false;
      return { ...state, sortField: action.field, sortAscending: nextAsc };
    }
    case 'TOGGLE_SELECTION_MODE':
      return { ...state, isSelectionMode: !state.isSelectionMode, selectedItemIds: new Set() };
    case 'TOGGLE_ITEM_SELECTION': {
      const newSelection = new Set(state.selectedItemIds);
      if (newSelection.has(action.id)) newSelection.delete(action.id);
      else newSelection.add(action.id);
      return { ...state, selectedItemIds: newSelection };
    }
    case 'SELECT_ALL':
      return { ...state, selectedItemIds: new Set(state.items.map(i => i.id)) };
    case 'SHOW_DELETE_DIALOG':
      return { ...state, showDeleteDialog: true, itemToDelete: action.item };
    case 'DISMISS_DELETE_DIALOG':
      return { ...state, showDeleteDialog: false, itemToDelete: null };
    case 'SET_LOADING':
      return { ...state, isLoading: action.loading };
    case 'SHOW_SNACKBAR':
      return { ...state, snackbar: { message: action.message, type: action.snackbarType } };
    case 'DISMISS_SNACKBAR':
      return { ...state, snackbar: null };
    case 'SET_AI_PROCESSING':
      return { ...state, isAIProcessing: action.processing };
    case 'TOGGLE_AI_DIALOG':
      return { ...state, showAIDialog: !state.showAIDialog };
    case 'SET_EXPORTING':
      return { ...state, isExporting: action.exporting };
    case 'SET_IMPORTING':
      return { ...state, isImporting: action.importing };
    case 'ITEMS_UPDATED':
      return state;
    default:
      return state;
  }
}

export { initialState };
