import { type ReactNode } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Home, BarChart3, Settings } from 'lucide-react';

const NAV_ITEMS = [
  { path: '/', label: '物品', icon: Home },
  { path: '/statistics', label: '统计', icon: BarChart3 },
  { path: '/settings', label: '设置', icon: Settings },
];

export default function Layout({ children }: { children: ReactNode }) {
  const location = useLocation();
  const navigate = useNavigate();

  const showBottomNav = !['/add', '/item/'].some(p => location.pathname.startsWith(p));

  return (
    <div className="min-h-screen flex flex-col">
      {/* Top Bar */}
      <header className="sticky top-0 z-40 backdrop-blur-xl bg-bg-page/85 border-b border-border/50">
        <div className="max-w-2xl mx-auto flex items-center justify-between px-4 h-14">
          <button
            onClick={() => navigate('/')}
            className="flex items-center gap-2 group"
          >
            <div className="w-9 h-9 rounded-xl bg-primary flex items-center justify-center text-white font-bold text-sm">
              TP
            </div>
            <span className="font-semibold text-text group-hover:text-primary transition-colors">
              ThingsPath
            </span>
          </button>

          {/* Desktop nav icons */}
          <nav className="hidden md:flex items-center gap-1">
            {NAV_ITEMS.map(item => {
              const Icon = item.icon;
              const active = location.pathname === item.path;
              return (
                <button
                  key={item.path}
                  onClick={() => navigate(item.path)}
                  className={`p-2.5 rounded-xl transition-all duration-200 ${
                    active
                      ? 'bg-primary/10 text-primary'
                      : 'text-text-secondary hover:bg-gray-light hover:text-text'
                  }`}
                >
                  <Icon size={20} />
                </button>
              );
            })}
          </nav>
        </div>
      </header>

      {/* Content */}
      <main className="flex-1 pb-20 md:pb-4">
        <div className="max-w-2xl mx-auto px-4 py-4">{children}</div>
      </main>

      {/* Mobile Bottom Nav */}
      {showBottomNav && (
        <nav className="md:hidden fixed bottom-0 left-0 right-0 z-40 backdrop-blur-xl bg-bg-page/90 border-t border-border/50">
          <div className="flex items-center justify-around py-2 px-4 safe-area-bottom">
            {NAV_ITEMS.map(item => {
              const Icon = item.icon;
              const active = location.pathname === item.path;
              return (
                <button
                  key={item.path}
                  onClick={() => navigate(item.path)}
                  className={`flex flex-col items-center gap-0.5 px-4 py-1.5 rounded-xl transition-all duration-200 ${
                    active ? 'text-primary' : 'text-text-secondary'
                  }`}
                >
                  <Icon size={22} strokeWidth={active ? 2.2 : 1.8} />
                  <span className={`text-[10px] ${active ? 'font-semibold' : ''}`}>
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
