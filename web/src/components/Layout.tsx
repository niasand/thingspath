import { type ReactNode, useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { AnimatePresence } from 'framer-motion';
import { Home, BarChart3, Settings, Moon, Sun } from 'lucide-react';
import PageTransition from './PageTransition';

const NAV_ITEMS = [
  { path: '/', label: '首页', icon: Home },
  { path: '/statistics', label: '统计', icon: BarChart3 },
  { path: '/settings', label: '设置', icon: Settings },
];

function useTheme() {
  const [dark, setDark] = useState(() => {
    const stored = localStorage.getItem('thingspath-theme');
    if (stored) return stored === 'dark';
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  });

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');
    localStorage.setItem('thingspath-theme', dark ? 'dark' : 'light');
  }, [dark]);

  const toggle = () => setDark(d => !d);
  return { dark, toggle };
}

export { useTheme };

export default function Layout({ children }: { children: ReactNode }) {
  const location = useLocation();
  const navigate = useNavigate();
  const { dark, toggle: toggleTheme } = useTheme();
  const showBottomNav = !['/add', '/item/'].some(p => location.pathname.startsWith(p));

  return (
    <div className="min-h-[100dvh] flex flex-col" style={{ background: 'var(--bg-app)' }}>
      {/* Top Bar */}
      <header
        className="sticky top-0 z-50"
        style={{
          background: 'var(--bg-surface)',
          backdropFilter: 'blur(var(--blur-lg))',
          WebkitBackdropFilter: 'blur(var(--blur-lg))',
          borderBottom: '1px solid var(--border-subtle)',
        }}
      >
        <div className="container-app flex items-center justify-between h-14">
          <button onClick={() => navigate('/')} className="flex items-center gap-2.5 group">
            <div
              className="w-8 h-8 rounded-lg flex items-center justify-center font-bold text-xs tracking-tight"
              style={{
                background: 'linear-gradient(135deg, var(--accent), var(--accent-light))',
                color: 'var(--text-inverse)',
                boxShadow: '0 2px 8px rgba(124, 58, 237, 0.25)',
              }}
            >
              TP
            </div>
            <span
              className="font-semibold text-sm tracking-tight"
              style={{ color: 'var(--text-primary)' }}
            >
              ThingsPath
            </span>
          </button>

          <div className="flex items-center gap-1">
            <button
              onClick={toggleTheme}
              className="p-2 rounded-xl transition-colors duration-200"
              style={{ color: 'var(--text-secondary)' }}
              aria-label={dark ? '切换浅色模式' : '切换深色模式'}
            >
              {dark ? <Sun size={18} /> : <Moon size={18} />}
            </button>

            {/* Desktop nav */}
            <nav className="hidden md:flex items-center gap-0.5">
              {NAV_ITEMS.map(item => {
                const Icon = item.icon;
                const active = location.pathname === item.path;
                return (
                  <button
                    key={item.path}
                    onClick={() => navigate(item.path)}
                    className="px-3 py-1.5 rounded-xl text-sm font-medium transition-all duration-200"
                    style={{
                      color: active ? 'var(--accent)' : 'var(--text-secondary)',
                      background: active ? 'var(--accent-soft)' : 'transparent',
                    }}
                  >
                    <Icon size={16} className="inline mr-1 -mt-0.5" />
                    <span className="hidden lg:inline">{item.label}</span>
                  </button>
                );
              })}
            </nav>
          </div>
        </div>
      </header>

      {/* Content */}
      <main className="flex-1 pb-24 md:pb-6">
        <div className="container-app py-4">
          <AnimatePresence mode="wait">
            <PageTransition key={location.pathname}>
              {children}
            </PageTransition>
          </AnimatePresence>
        </div>
      </main>

      {/* Mobile Bottom Nav */}
      {showBottomNav && (
        <nav
          className="md:hidden fixed bottom-0 left-0 right-0 z-50 safe-bottom"
          style={{
            background: 'var(--bg-surface-solid)',
            borderTop: '1px solid var(--border-subtle)',
            boxShadow: '0 -4px 20px rgba(0,0,0,0.05)',
          }}
        >
          <div className="flex items-center justify-around px-2 pt-2 pb-1">
            {NAV_ITEMS.map(item => {
              const Icon = item.icon;
              const active = location.pathname === item.path;
              return (
                <button
                  key={item.path}
                  onClick={() => navigate(item.path)}
                  className="flex flex-col items-center gap-0.5 px-5 py-1 rounded-xl transition-all duration-200 min-w-[56px]"
                >
                  <Icon
                    size={21}
                    strokeWidth={active ? 2.2 : 1.6}
                    style={{ color: active ? 'var(--accent)' : 'var(--text-tertiary)' }}
                  />
                  <span
                    className="text-[10px] font-medium"
                    style={{ color: active ? 'var(--accent)' : 'var(--text-tertiary)' }}
                  >
                    {item.label}
                  </span>
                </button>
              );
            })}
          </div>
        </nav>
      )}
    </div>
  );
}
