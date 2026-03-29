import { useCallback, useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApp } from '../context/AppContext';
import { useItems } from '../hooks/useItems';
import { itemRepository } from '../db/repository';
import SearchBar from '../components/SearchBar';
import FilterChipRow from '../components/FilterChipRow';
import SortDropdown from '../components/SortDropdown';
import FloatingActionButton from '../components/FloatingActionButton';
import DeleteConfirmationDialog from '../components/DeleteConfirmationDialog';
import EmptyState from '../components/EmptyState';
import StatisticsHeader from '../components/StatisticsHeader';
import AIInputDialog from '../components/AIInputDialog';
import ItemCard from '../components/ItemCard';
import SwipeableItem from '../components/SwipeableItem';
import SkeletonCard from '../components/SkeletonCard';
import { CheckSquare, XSquare, Trash2 } from 'lucide-react';

export default function HomeScreen() {
  const { state, dispatch, settings } = useApp();
  const { items: filteredItems } = useItems();
  const navigate = useNavigate();
  const [currentPage, setCurrentPage] = useState(0);
  const pageSize = 10;

  const totalPages = Math.ceil(filteredItems.length / pageSize);
  const pageItems = filteredItems.slice(currentPage * pageSize, (currentPage + 1) * pageSize);

  const handleSearch = useCallback((q: string) => dispatch({ type: 'SET_SEARCH_QUERY', query: q }), [dispatch]);
  const handleToggleTag = useCallback((tag: string) => dispatch({ type: 'TOGGLE_TAG', tag }), [dispatch]);
  const handleSort = useCallback((f: typeof state.sortField) => dispatch({ type: 'SET_SORT', field: f }), [dispatch]);

  const handleDeleteItem = useCallback(async () => {
    if (state.isSelectionMode && state.selectedItemIds.size > 0) {
      await itemRepository.deleteItems([...state.selectedItemIds]);
      dispatch({ type: 'TOGGLE_SELECTION_MODE' });
      dispatch({ type: 'DISMISS_DELETE_DIALOG' });
      dispatch({ type: 'SHOW_SNACKBAR', message: `已删除 ${state.selectedItemIds.size} 个物品`, snackbarType: 'success' });
    } else if (state.itemToDelete) {
      await itemRepository.deleteItem(state.itemToDelete.id);
      dispatch({ type: 'DISMISS_DELETE_DIALOG' });
      dispatch({ type: 'SHOW_SNACKBAR', message: '已删除', snackbarType: 'success' });
    }
  }, [state.itemToDelete, state.isSelectionMode, state.selectedItemIds, dispatch]);

  const handleAddAI = useCallback(async (text: string) => {
    dispatch({ type: 'SET_AI_PROCESSING', processing: true });
    try {
      const siliconflow = await import('../services/siliconflow');
      if (!settings.apiKey) throw new Error('请先在设置中配置 API Key');

      const extracted = await siliconflow.analyzeText(text, settings.apiKey);
      if (!extracted.length) throw new Error('无法识别任何物品');

      for (const item of extracted) {
        if (!item.name) continue;
        const purchaseDate = item.date ? new Date(item.date).getTime() : null;
        const usageDays = purchaseDate ? Math.floor((Date.now() - purchaseDate) / 86400000) : null;

        await itemRepository.addItem({
          name: item.name.trim(),
          imagePath: null,
          imagePaths: [],
          location: item.location?.trim() || null,
          purchaseDate,
          purchasePrice: item.price || 0,
          usageDays: usageDays && usageDays >= 0 ? usageDays : null,
          note: item.note?.trim() || null,
          tags: item.tags || [],
          createdAt: Date.now(),
          updatedAt: Date.now(),
        });
      }

      dispatch({
        type: 'SHOW_SNACKBAR',
        message: extracted.length === 1 ? `AI 已添加：${extracted[0].name}` : `AI 已添加 ${extracted.length} 个物品`,
        snackbarType: 'success',
      });
    } catch (e: any) {
      dispatch({ type: 'SHOW_SNACKBAR', message: e.message || 'AI 分析失败', snackbarType: 'error' });
    } finally {
      dispatch({ type: 'SET_AI_PROCESSING', processing: false });
      dispatch({ type: 'TOGGLE_AI_DIALOG' });
    }
  }, [dispatch, state]);

  // Reset page on filter change
  useEffect(() => { setCurrentPage(0); }, [state.searchQuery, state.selectedTags, state.sortField]);

  return (
    <div className="space-y-4">
      {/* Stats Header */}
      <StatisticsHeader totalItems={state.totalItemCount} totalPrice={state.totalPrice} />

      {/* Search */}
      <SearchBar value={state.searchQuery} onChange={handleSearch} />

      {/* Tag filters */}
      <FilterChipRow tags={state.allTags} selectedTags={state.selectedTags} onToggle={handleToggleTag} />

      {/* Sort + Selection controls */}
      <div className="flex items-center justify-between">
        <SortDropdown field={state.sortField} ascending={state.sortAscending} onSelect={handleSort} />
        <div className="flex items-center gap-1">
          {!state.isSelectionMode ? (
            <button
              onClick={() => dispatch({ type: 'TOGGLE_SELECTION_MODE' })}
              className="p-2 rounded-xl transition-all duration-200 hover:scale-105"
              style={{ color: 'var(--text-secondary)' }}
              title="多选模式"
            >
              <CheckSquare size={18} />
            </button>
          ) : (
            <>
              <button
                onClick={() => dispatch({ type: 'SELECT_ALL' })}
                className="px-3 py-1.5 rounded-xl text-xs font-medium transition-all duration-200 hover:scale-105"
                style={{
                  color: 'var(--color-accent)',
                  background: 'var(--glass-bg)',
                  backdropFilter: 'var(--glass-blur)',
                  WebkitBackdropFilter: 'var(--glass-blur)',
                  border: '1px solid var(--glass-border)',
                }}
              >
                全选
              </button>
              {state.selectedItemIds.size > 0 && (
                <button
                  onClick={() => dispatch({ type: 'SHOW_DELETE_DIALOG', item: null })}
                  className="p-2 rounded-xl transition-all duration-200 hover:scale-105"
                  style={{ color: 'var(--color-error)' }}
                >
                  <Trash2 size={18} />
                </button>
              )}
              <button
                onClick={() => dispatch({ type: 'TOGGLE_SELECTION_MODE' })}
                className="p-2 rounded-xl transition-all duration-200 hover:scale-105"
                style={{ color: 'var(--text-secondary)' }}
              >
                <XSquare size={18} />
              </button>
            </>
          )}
        </div>
      </div>

      {/* Selection info bar */}
      {state.isSelectionMode && state.selectedItemIds.size > 0 && (
        <div
          className="px-3 py-2 rounded-xl text-xs font-medium text-center"
          style={{
            background: 'var(--glass-bg)',
            backdropFilter: 'var(--glass-blur)',
            WebkitBackdropFilter: 'var(--glass-blur)',
            border: '1px solid var(--glass-border)',
            color: 'var(--color-accent)',
          }}
        >
          已选择 {state.selectedItemIds.size} 个物品
        </div>
      )}

      {/* Item list */}
      {state.isLoading ? (
        Array.from({ length: 5 }, (_, i) => <SkeletonCard key={`sk-${i}`} />)
      ) : filteredItems.length === 0 ? (
        <EmptyState />
      ) : (
        <>
          {pageItems.map(item => (
            <SwipeableItem
              key={item.id}
              onDelete={() => dispatch({ type: 'SHOW_DELETE_DIALOG', item })}
            >
              <ItemCard
                item={item}
                selectionMode={state.isSelectionMode}
                selected={state.selectedItemIds.has(item.id)}
                onSelect={() => dispatch({ type: 'TOGGLE_ITEM_SELECTION', id: item.id })}
              />
            </SwipeableItem>
          ))}

          {/* Pagination - compact pill style */}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 py-3">
              <button
                onClick={() => setCurrentPage(p => Math.max(0, p - 1))}
                disabled={currentPage === 0}
                className="px-3 py-1.5 rounded-full text-xs font-medium transition-all duration-200
                           disabled:opacity-40 disabled:cursor-not-allowed hover:scale-105"
                style={{
                  background: 'var(--glass-bg)',
                  backdropFilter: 'var(--glass-blur)',
                  WebkitBackdropFilter: 'var(--glass-blur)',
                  border: '1px solid var(--glass-border)',
                  color: 'var(--text-secondary)',
                }}
              >
                上一页
              </button>
              <span
                className="px-3 py-1.5 rounded-full text-xs font-medium"
                style={{
                  background: 'var(--glass-bg)',
                  backdropFilter: 'var(--glass-blur)',
                  WebkitBackdropFilter: 'var(--glass-blur)',
                  border: '1px solid var(--glass-border)',
                  color: 'var(--text-tertiary)',
                }}
              >
                {currentPage + 1} / {totalPages}
              </span>
              <button
                onClick={() => setCurrentPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={currentPage >= totalPages - 1}
                className="px-3 py-1.5 rounded-full text-xs font-medium transition-all duration-200
                           disabled:opacity-40 disabled:cursor-not-allowed hover:scale-105"
                style={{
                  background: 'var(--glass-bg)',
                  backdropFilter: 'var(--glass-blur)',
                  WebkitBackdropFilter: 'var(--glass-blur)',
                  border: '1px solid var(--glass-border)',
                  color: 'var(--text-secondary)',
                }}
              >
                下一页
              </button>
            </div>
          )}
        </>
      )}

      {/* FAB */}
      <FloatingActionButton
        onAddManual={() => navigate('/add')}
        onAddAI={() => dispatch({ type: 'TOGGLE_AI_DIALOG' })}
      />

      {/* Delete Dialog */}
      <DeleteConfirmationDialog
        open={state.showDeleteDialog}
        message={
          state.isSelectionMode && state.selectedItemIds.size > 0
            ? `确定要删除已选中的 ${state.selectedItemIds.size} 个物品吗？`
            : `确定要删除「${state.itemToDelete?.name ?? ''}」吗？`
        }
        onConfirm={handleDeleteItem}
        onCancel={() => dispatch({ type: 'DISMISS_DELETE_DIALOG' })}
      />

      {/* AI Dialog */}
      <AIInputDialog
        open={state.showAIDialog}
        loading={state.isAIProcessing}
        onSubmit={handleAddAI}
        onClose={() => dispatch({ type: 'TOGGLE_AI_DIALOG' })}
      />

      {/* Snackbar */}
      {state.snackbar && (
        <div
          className="fixed bottom-24 md:bottom-8 left-1/2 -translate-x-1/2 z-50 px-5 py-2.5 rounded-2xl
                     text-sm font-medium shadow-xl backdrop-blur-xl animate-in"
          style={{
            background: state.snackbar.type === 'error'
              ? 'var(--color-error)'
              : state.snackbar.type === 'success'
                ? 'var(--glass-bg, rgba(255,255,255,0.15))'
                : 'var(--glass-bg)',
            backdropFilter: 'blur(20px)',
            WebkitBackdropFilter: 'blur(20px)',
            border: '1px solid var(--glass-border)',
            color: state.snackbar.type === 'success'
              ? 'var(--text-primary)'
              : 'white',
            boxShadow: state.snackbar.type === 'success'
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
