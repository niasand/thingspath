import { createContext, useContext, useReducer, type ReactNode, type Dispatch } from 'react';
import { appReducer, initialState, type AppState, type AppAction } from './appReducer';
import { useSettings } from '../hooks/useSettings';
import { itemRepository } from '../db/repository';
import { useLiveQuery } from 'dexie-react-hooks';
import { useEffect } from 'react';

interface AppContextValue {
  state: AppState;
  dispatch: Dispatch<AppAction>;
  settings: ReturnType<typeof useSettings>['settings'];
  setApiKey: (key: string) => void;
  setPageSize: (size: number) => void;
}

const AppContext = createContext<AppContextValue | null>(null);

export function AppProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(appReducer, initialState);
  const { settings, setApiKey, setPageSize } = useSettings();

  const allItems = useLiveQuery(() => itemRepository.getAllItems(), []);

  useEffect(() => {
    if (allItems) {
      const allTags = [...new Set(allItems.flatMap(i => i.tags).filter(Boolean))].sort();
      dispatch({ type: 'SET_ITEMS', items: allItems });
      dispatch({ type: 'SET_ALL_TAGS', tags: allTags });
    }
  }, [allItems]);

  return (
    <AppContext.Provider value={{ state, dispatch, settings, setApiKey, setPageSize }}>
      {children}
    </AppContext.Provider>
  );
}

export function useApp() {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error('useApp must be used within AppProvider');
  return ctx;
}
