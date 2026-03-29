import { useState, useCallback, useEffect } from 'react';

interface Settings {
  apiKey: string;
  pageSize: number;
  infiniteScroll: boolean;
}

const STORAGE_KEY = 'thingspath_settings';
const DEFAULT_SETTINGS: Settings = {
  apiKey: '',
  pageSize: 10,
  infiniteScroll: false,
};

function loadSettings(): Settings {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return { ...DEFAULT_SETTINGS, ...JSON.parse(raw) };
  } catch {}
  return DEFAULT_SETTINGS;
}

function saveSettings(settings: Settings) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
}

export function useSettings() {
  const [settings, setSettings] = useState<Settings>(loadSettings);

  useEffect(() => {
    saveSettings(settings);
  }, [settings]);

  const setApiKey = useCallback((apiKey: string) => {
    setSettings(s => ({ ...s, apiKey }));
  }, []);

  const setPageSize = useCallback((pageSize: number) => {
    setSettings(s => ({ ...s, pageSize }));
  }, []);

  const setInfiniteScroll = useCallback((infiniteScroll: boolean) => {
    setSettings(s => ({ ...s, infiniteScroll }));
  }, []);

  return { settings, setApiKey, setPageSize, setInfiniteScroll };
}
