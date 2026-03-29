import { useState, useRef } from 'react';
import { useApp } from '../context/AppContext';
import { downloadJson, importFromJson, readJsonFile } from '../services/dataTransfer';
import TopBar from '../components/TopBar';
import { Download, Upload, Key, Info, ExternalLink, Eye, EyeOff } from 'lucide-react';

export default function SettingsScreen() {
  const { state, dispatch, settings, setApiKey } = useApp();
  const [importStatus, setImportStatus] = useState<string | null>(null);
  const [showApiKey, setShowApiKey] = useState(false);
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

  const glassCard = {
    background: 'var(--glass-bg)',
    backdropFilter: 'var(--glass-blur)',
    WebkitBackdropFilter: 'var(--glass-blur)',
    border: '1px solid var(--glass-border)',
    borderRadius: '20px',
    boxShadow: '0 8px 32px var(--shadow-color, rgba(0,0,0,0.06))',
  };

  const buttonStyle = (disabled: boolean) => ({
    display: 'inline-flex',
    alignItems: 'center',
    gap: '8px',
    padding: '8px 16px',
    borderRadius: '14px',
    fontSize: '14px',
    fontWeight: 500,
    cursor: disabled ? 'not-allowed' : 'pointer',
    opacity: disabled ? 0.4 : 1,
    transition: 'all 0.2s ease',
    background: 'var(--glass-bg)',
    backdropFilter: 'var(--glass-blur)',
    WebkitBackdropFilter: 'var(--glass-blur)',
    border: '1px solid var(--glass-border)',
    color: 'var(--color-accent)',
  });

  return (
    <div>
      <TopBar title="设置" />

      <div className="space-y-4">
        {/* API Key */}
        <div className="p-5" style={glassCard}>
          <div className="flex items-center gap-2 mb-3">
            <Key size={16} style={{ color: 'var(--color-accent)' }} />
            <h3 className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>AI API Key</h3>
          </div>
          <div className="relative mb-2">
            <input
              type={showApiKey ? 'text' : 'password'}
              value={settings.apiKey}
              onChange={e => setApiKey(e.target.value)}
              placeholder="输入 SiliconFlow API Key"
              className="w-full px-3.5 pr-10 py-2.5 rounded-xl text-sm transition-all duration-200"
              style={{
                color: 'var(--text-primary)',
                background: 'var(--glass-input-bg, rgba(255,255,255,0.06))',
                backdropFilter: 'var(--glass-blur)',
                WebkitBackdropFilter: 'var(--glass-blur)',
                border: '1px solid var(--glass-border)',
                outline: 'none',
              }}
            />
            <button
              type="button"
              onClick={() => setShowApiKey(v => !v)}
              className="absolute right-3 top-1/2 -translate-y-1/2 p-0.5 transition-all duration-200 hover:scale-110"
              style={{ color: 'var(--text-tertiary)' }}
            >
              {showApiKey ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          </div>
          <div className="flex items-start gap-1.5 text-[11px] leading-relaxed" style={{ color: 'var(--text-tertiary)' }}>
            <Info size={12} className="shrink-0 mt-0.5" />
            <span>
              用于 AI 智能添加功能。获取 API Key 请访问{' '}
              <a
                href="https://cloud.siliconflow.cn"
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-0.5 hover:underline"
                style={{ color: 'var(--color-accent)' }}
              >
                SiliconFlow <ExternalLink size={10} />
              </a>
            </span>
          </div>
        </div>

        {/* Export */}
        <div className="p-5" style={glassCard}>
          <div className="flex items-center gap-2 mb-3">
            <Download size={16} style={{ color: 'var(--color-accent)' }} />
            <h3 className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>数据导出</h3>
          </div>
          <p className="text-xs mb-3" style={{ color: 'var(--text-secondary)' }}>
            将所有物品数据导出为 JSON 文件，可与 Android APP 互通。
          </p>
          <button
            onClick={handleExport}
            disabled={state.items.length === 0}
            style={buttonStyle(state.items.length === 0)}
            className="hover:scale-[1.02] active:scale-[0.98]"
          >
            导出 JSON 文件 ({state.items.length} 个物品)
          </button>
        </div>

        {/* Import */}
        <div className="p-5" style={glassCard}>
          <div className="flex items-center gap-2 mb-3">
            <Upload size={16} style={{ color: 'var(--color-accent)' }} />
            <h3 className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>数据导入</h3>
          </div>
          <p className="text-xs mb-3" style={{ color: 'var(--text-secondary)' }}>
            从 JSON 文件导入物品数据。支持从 Android APP 导出的数据。
          </p>
          <button
            onClick={() => fileRef.current?.click()}
            disabled={!!importStatus}
            style={buttonStyle(!!importStatus)}
            className="hover:scale-[1.02] active:scale-[0.98]"
          >
            {importStatus || '选择 JSON 文件导入'}
          </button>
          <input ref={fileRef} type="file" accept=".json,application/json" className="hidden" onChange={handleImport} />
        </div>

        {/* About */}
        <div className="p-5" style={glassCard}>
          <h3 className="text-sm font-semibold mb-3" style={{ color: 'var(--text-primary)' }}>关于</h3>
          <div className="text-xs space-y-1.5" style={{ color: 'var(--text-secondary)' }}>
            <p>ThingsPath Web v1.0.0</p>
            <p>家庭物品管理工具</p>
            <p>数据存储在浏览器本地 (IndexedDB)</p>
          </div>
        </div>
      </div>

      {/* Snackbar */}
      {state.snackbar && (
        <div
          className="fixed bottom-8 left-1/2 -translate-x-1/2 z-50 px-5 py-2.5 rounded-2xl
                     text-sm font-medium shadow-xl backdrop-blur-xl"
          style={{
            background: state.snackbar.type === 'error'
              ? 'var(--color-error)'
              : 'var(--glass-bg)',
            backdropFilter: 'blur(20px)',
            WebkitBackdropFilter: 'blur(20px)',
            border: '1px solid var(--glass-border)',
            color: state.snackbar.type === 'error' ? 'white' : 'var(--text-primary)',
            boxShadow: state.snackbar.type !== 'error'
              ? '0 8px 32px var(--shadow-color, rgba(0,0,0,0.12))'
              : undefined,
          }}
        >
          {state.snackbar.message}
        </div>
      )}
    </div>
  );
}
