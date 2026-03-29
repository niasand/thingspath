import { useState, useRef } from 'react';
import { useApp } from '../context/AppContext';
import { downloadJson, importFromJson, readJsonFile } from '../services/dataTransfer';
import TopBar from '../components/TopBar';
import { Download, Upload, Key, Info, ExternalLink } from 'lucide-react';

export default function SettingsScreen() {
  const { state, dispatch, settings, setApiKey } = useApp();
  const [importStatus, setImportStatus] = useState<string | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  const handleExport = () => {
    try {
      downloadJson(state.items);
      dispatch({ type: 'SHOW_SNACKBAR', message: '导出成功', snackbarType: 'success' });
    } catch {
      dispatch({ type: 'SHOW_SNACKBAR', message: '导出失败', snackbarType: 'error' });
    }
  };

  const handleImport = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      setImportStatus('导入中...');
      const json = await readJsonFile(file);
      const count = await importFromJson(json);
      dispatch({ type: 'SHOW_SNACKBAR', message: `成功导入 ${count} 个物品`, snackbarType: 'success' });
    } catch (err: any) {
      dispatch({ type: 'SHOW_SNACKBAR', message: err.message || '导入失败', snackbarType: 'error' });
    } finally {
      setImportStatus(null);
      if (fileRef.current) fileRef.current.value = '';
    }
  };

  return (
    <div>
      <TopBar title="设置" />

      <div className="space-y-3">
        {/* API Key */}
        <div className="bg-surface rounded-2xl p-4 shadow-sm">
          <div className="flex items-center gap-2 mb-3">
            <Key size={16} className="text-primary" />
            <h3 className="text-sm font-semibold text-text">AI API Key</h3>
          </div>
          <input
            type="password"
            value={settings.apiKey}
            onChange={e => setApiKey(e.target.value)}
            placeholder="输入 SiliconFlow API Key"
            className="w-full px-3 py-2.5 rounded-xl border border-border/60 bg-bg-page text-sm
                       placeholder:text-text-tertiary focus:outline-none focus:border-primary/40 focus:ring-2
                       focus:ring-primary/10 transition-all mb-2"
          />
          <div className="flex items-start gap-1.5 text-[11px] text-text-tertiary leading-relaxed">
            <Info size={12} className="shrink-0 mt-0.5" />
            <span>
              用于 AI 智能添加功能。获取 API Key 请访问{' '}
              <a href="https://cloud.siliconflow.cn" target="_blank" rel="noopener noreferrer"
                 className="text-primary hover:underline inline-flex items-center gap-0.5">
                SiliconFlow <ExternalLink size={10} />
              </a>
            </span>
          </div>
        </div>

        {/* Export */}
        <div className="bg-surface rounded-2xl p-4 shadow-sm">
          <div className="flex items-center gap-2 mb-3">
            <Download size={16} className="text-primary" />
            <h3 className="text-sm font-semibold text-text">数据导出</h3>
          </div>
          <p className="text-xs text-text-secondary mb-3">
            将所有物品数据导出为 JSON 文件，可与 Android APP 互通。
          </p>
          <button
            onClick={handleExport}
            disabled={state.items.length === 0}
            className="px-4 py-2 rounded-xl text-sm font-medium bg-primary/10 text-primary
                       hover:bg-primary/20 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
          >
            导出 JSON 文件 ({state.items.length} 个物品)
          </button>
        </div>

        {/* Import */}
        <div className="bg-surface rounded-2xl p-4 shadow-sm">
          <div className="flex items-center gap-2 mb-3">
            <Upload size={16} className="text-primary" />
            <h3 className="text-sm font-semibold text-text">数据导入</h3>
          </div>
          <p className="text-xs text-text-secondary mb-3">
            从 JSON 文件导入物品数据。支持从 Android APP 导出的数据。
          </p>
          <button
            onClick={() => fileRef.current?.click()}
            disabled={!!importStatus}
            className="px-4 py-2 rounded-xl text-sm font-medium bg-primary/10 text-primary
                       hover:bg-primary/20 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
          >
            {importStatus || '选择 JSON 文件导入'}
          </button>
          <input ref={fileRef} type="file" accept=".json,application/json" className="hidden" onChange={handleImport} />
        </div>

        {/* About */}
        <div className="bg-surface rounded-2xl p-4 shadow-sm">
          <h3 className="text-sm font-semibold text-text mb-2">关于</h3>
          <div className="text-xs text-text-secondary space-y-1">
            <p>ThingsPath Web v1.0.0</p>
            <p>家庭物品管理工具</p>
            <p>数据存储在浏览器本地 (IndexedDB)</p>
          </div>
        </div>
      </div>

      {/* Snackbar */}
      {state.snackbar && (
        <div className={`fixed bottom-8 left-1/2 -translate-x-1/2 z-50 px-4 py-2.5 rounded-xl text-sm font-medium shadow-lg ${state.snackbar.type === 'error' ? 'bg-error text-white' : 'bg-primary text-white'}`}>
          {state.snackbar.message}
        </div>
      )}
    </div>
  );
}
