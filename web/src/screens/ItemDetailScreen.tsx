import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useApp } from '../context/AppContext';
import { itemRepository } from '../db/repository';
import { saveImage, deleteImage, getImageUrl, isWebImage } from '../services/imageStorage';
import TopBar from '../components/TopBar';
import MultiImageViewer from '../components/MultiImageViewer';
import TagInput from '../components/TagInput';
import DeleteConfirmationDialog from '../components/DeleteConfirmationDialog';
import { formatDate, formatRelativeTime, calculateUsageDays } from '../utils/dateUtils';
import { formatPrice, formatDailyCost, calculateDailyCost } from '../utils/numberUtils';
import { MapPin, Calendar, Hash, DollarSign, FileText, Edit3, Trash2, Save, X } from 'lucide-react';
import type { Item } from '../types/item';

export default function ItemDetailScreen() {
  const { itemId } = useParams<{ itemId: string }>();
  const navigate = useNavigate();
  const { state, dispatch } = useApp();

  const [item, setItem] = useState<Item | null>(null);
  const [editing, setEditing] = useState(false);
  const [showDelete, setShowDelete] = useState(false);
  const [loading, setLoading] = useState(true);

  // Edit fields
  const [name, setName] = useState('');
  const [location, setLocation] = useState('');
  const [purchaseDate, setPurchaseDate] = useState('');
  const [purchasePrice, setPurchasePrice] = useState('');
  const [usageDays, setUsageDays] = useState('');
  const [note, setNote] = useState('');
  const [tags, setTags] = useState<string[]>([]);
  const [imagePaths, setImagePaths] = useState<string[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (!itemId) return;
    itemRepository.getItemById(parseInt(itemId)).then(i => {
      if (i) setItem(i);
      setLoading(false);
    });
  }, [itemId]);

  useEffect(() => {
    if (item) {
      setName(item.name);
      setLocation(item.location || '');
      setPurchaseDate(item.purchaseDate ? formatDate(item.purchaseDate) : '');
      setPurchasePrice(item.purchasePrice > 0 ? String(item.purchasePrice) : '');
      setUsageDays(item.usageDays != null ? String(item.usageDays) : '');
      setNote(item.note || '');
      setTags([...item.tags]);
      setImagePaths([...item.imagePaths]);
    }
  }, [item, editing]);

  const dailyCost = item ? calculateDailyCost(item.purchasePrice, item.usageDays) : null;
  const autoUsageDays = item?.purchaseDate ? calculateUsageDays(item.purchaseDate) : null;

  const handleSave = async () => {
    if (!item) return;
    const e: Record<string, string> = {};
    if (!name.trim()) e.name = '请输入物品名称';
    setErrors(e);
    if (Object.keys(e).length > 0) return;

    const dateMs = purchaseDate ? new Date(purchaseDate).getTime() : null;
    const days = usageDays ? parseInt(usageDays, 10) : calculateUsageDays(dateMs);

    await itemRepository.updateItem({
      ...item,
      name: name.trim(),
      location: location.trim() || null,
      purchaseDate: dateMs,
      purchasePrice: parseFloat(purchasePrice) || 0,
      usageDays: days && days >= 0 ? days : null,
      note: note.trim() || null,
      tags,
      imagePath: imagePaths[0] || null,
      imagePaths,
    });

    setEditing(false);
    dispatch({ type: 'SHOW_SNACKBAR', message: '保存成功', snackbarType: 'success' });
    const updated = await itemRepository.getItemById(item.id);
    if (updated) setItem(updated);
  };

  const handleDelete = async () => {
    if (!item) return;
    await itemRepository.deleteItem(item.id);
    dispatch({ type: 'SHOW_SNACKBAR', message: '已删除', snackbarType: 'success' });
    navigate('/');
  };

  const handleAddImage = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || !item) return;
    for (const file of Array.from(files)) {
      const recordId = await saveImage(item.id, file);
      setImagePaths(prev => [...prev, `img:${recordId}`]);
    }
    e.target.value = '';
  };

  const handleRemoveImage = async (index: number) => {
    const path = imagePaths[index];
    if (isWebImage(path)) {
      const id = parseInt(path.slice(4), 10);
      await deleteImage(id);
    }
    setImagePaths(prev => prev.filter((_, i) => i !== index));
  };

  if (loading) return <div className="text-center py-10 text-sm" style={{ color: 'var(--text-secondary)' }}>加载中...</div>;
  if (!item) return <div className="text-center py-10 text-sm" style={{ color: 'var(--text-secondary)' }}>物品不存在</div>;

  const glassCard = {
    background: 'var(--glass-bg)',
    backdropFilter: 'var(--glass-blur)',
    WebkitBackdropFilter: 'var(--glass-blur)',
    border: '1px solid var(--glass-border)',
    borderRadius: '20px',
  };

  const editInputStyle = {
    width: '100%',
    padding: '10px 14px',
    borderRadius: '14px',
    fontSize: '14px',
    color: 'var(--text-primary)',
    background: 'var(--glass-input-bg, rgba(255,255,255,0.06))',
    backdropFilter: 'var(--glass-blur)',
    WebkitBackdropFilter: 'var(--glass-blur)',
    border: '1px solid var(--glass-border)',
    outline: 'none',
    transition: 'all 0.2s ease',
  };

  return (
    <div>
      <TopBar
        title={editing ? '编辑物品' : item.name}
        showBack
        actions={
          editing ? (
            <div className="flex items-center gap-2">
              <button
                onClick={() => setEditing(false)}
                className="p-2 rounded-xl transition-all duration-200 hover:scale-105"
                style={{ color: 'var(--text-secondary)' }}
              >
                <X size={18} />
              </button>
              <button
                onClick={handleSave}
                className="px-3 py-1.5 rounded-xl text-sm font-medium text-white transition-all duration-200
                           hover:scale-105 active:scale-95 flex items-center gap-1"
                style={{
                  background: 'var(--gradient-accent)',
                  boxShadow: '0 4px 16px var(--shadow-accent, rgba(124,58,237,0.3))',
                }}
              >
                <Save size={16} /> 保存
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-1">
              <button
                onClick={() => setEditing(true)}
                className="p-2 rounded-xl transition-all duration-200 hover:scale-105"
                style={{ color: 'var(--text-secondary)' }}
              >
                <Edit3 size={18} />
              </button>
              <button
                onClick={() => setShowDelete(true)}
                className="p-2 rounded-xl transition-all duration-200 hover:scale-105"
                style={{ color: 'var(--color-error)' }}
              >
                <Trash2 size={18} />
              </button>
            </div>
          )
        }
      />

      <div className="space-y-4">
        {/* Image Viewer */}
        {!editing ? (
          <MultiImageViewer imagePaths={item.imagePaths} itemName={item.name} />
        ) : (
          <div className="space-y-2">
            <label className="text-sm font-medium" style={{ color: 'var(--text-primary)' }}>
              图片 ({imagePaths.length}/5)
            </label>
            <div className="flex gap-3 flex-wrap">
              {imagePaths.map((path, i) => (
                <EditableImage key={path} path={path} name={name} onRemove={() => handleRemoveImage(i)} />
              ))}
              {imagePaths.length < 5 && (
                <label
                  className="w-20 h-20 rounded-2xl flex flex-col items-center justify-center cursor-pointer
                             transition-all duration-200 hover:scale-105"
                  style={{
                    border: '2px dashed var(--glass-border)',
                    color: 'var(--text-secondary)',
                    background: 'var(--glass-input-bg, rgba(255,255,255,0.03))',
                  }}
                >
                  <span className="text-lg">+</span>
                  <input type="file" accept="image/*" multiple className="hidden" onChange={handleAddImage} />
                </label>
              )}
            </div>
          </div>
        )}

        {/* Info fields / View mode */}
        {editing ? (
          <div className="space-y-4 p-5" style={glassCard}>
            <EditField label="物品名称" required icon={<Hash size={16} />} value={name} onChange={setName} placeholder="物品名称" error={errors.name} inputStyle={editInputStyle} />
            <EditField label="存放位置" icon={<MapPin size={16} />} value={location} onChange={setLocation} placeholder="存放位置" inputStyle={editInputStyle} />
            <EditField label="购买日期" icon={<Calendar size={16} />} value={purchaseDate} onChange={setPurchaseDate} type="date" inputStyle={editInputStyle} />
            <div className="grid grid-cols-2 gap-3">
              <EditField label="购买价格" icon={<DollarSign size={16} />} value={purchasePrice} onChange={setPurchasePrice} type="number" inputStyle={editInputStyle} />
              <EditField label="使用天数" icon={<Hash size={16} />} value={usageDays} onChange={setUsageDays} type="number" placeholder={autoUsageDays != null ? `自动: ${autoUsageDays}` : ''} inputStyle={editInputStyle} />
            </div>
            <div className="space-y-1.5">
              <label className="flex items-center gap-1.5 text-sm font-medium" style={{ color: 'var(--text-primary)' }}>
                <FileText size={14} style={{ color: 'var(--text-secondary)' }} /> 备注
              </label>
              <textarea
                value={note}
                onChange={e => setNote(e.target.value)}
                rows={3}
                className="w-full px-3.5 py-2.5 rounded-xl text-sm resize-none transition-all duration-200"
                style={{
                  color: 'var(--text-primary)',
                  background: 'var(--glass-input-bg, rgba(255,255,255,0.06))',
                  backdropFilter: 'var(--glass-blur)',
                  WebkitBackdropFilter: 'var(--glass-blur)',
                  border: '1px solid var(--glass-border)',
                  outline: 'none',
                }}
                placeholder="备注..."
              />
            </div>
            <TagInput tags={tags} onChange={setTags} />
          </div>
        ) : (
          <div style={glassCard}>
            <InfoRow icon={<MapPin size={16} />} label="存放位置" value={item.location || '\u2014'} />
            <div style={{ height: '1px', background: 'var(--glass-border)', margin: '0 16px' }} />
            <InfoRow icon={<Calendar size={16} />} label="购买日期" value={item.purchaseDate ? `${formatDate(item.purchaseDate)} (${formatRelativeTime(item.purchaseDate)})` : '\u2014'} />
            <div style={{ height: '1px', background: 'var(--glass-border)', margin: '0 16px' }} />
            <InfoRow icon={<DollarSign size={16} />} label="购买价格" value={formatPrice(item.purchasePrice) || '\u2014'} />
            <div style={{ height: '1px', background: 'var(--glass-border)', margin: '0 16px' }} />
            <InfoRow icon={<Hash size={16} />} label="使用天数" value={item.usageDays != null ? `${item.usageDays} 天` : (autoUsageDays != null ? `${autoUsageDays} 天` : '\u2014')} />
            {dailyCost !== null && (
              <>
                <div style={{ height: '1px', background: 'var(--glass-border)', margin: '0 16px' }} />
                <InfoRow icon={<DollarSign size={16} />} label="日均成本" value={formatDailyCost(dailyCost)} />
              </>
            )}
            <div style={{ height: '1px', background: 'var(--glass-border)', margin: '0 16px' }} />
            <InfoRow icon={<FileText size={16} />} label="备注" value={item.note || '\u2014'} />
            {item.tags.length > 0 && (
              <>
                <div style={{ height: '1px', background: 'var(--glass-border)', margin: '0 16px' }} />
                <div className="px-4 py-3 flex items-start gap-3">
                  <span style={{ color: 'var(--text-secondary)', marginTop: '2px' }}><Hash size={16} /></span>
                  <div>
                    <p className="text-xs mb-1" style={{ color: 'var(--text-secondary)' }}>标签</p>
                    <div className="flex gap-1.5 flex-wrap">
                      {item.tags.map(tag => (
                        <span
                          key={tag}
                          className="text-xs px-2.5 py-0.5 rounded-lg font-medium"
                          style={{
                            background: 'var(--glass-bg)',
                            backdropFilter: 'var(--glass-blur)',
                            WebkitBackdropFilter: 'var(--glass-blur)',
                            border: '1px solid var(--glass-border)',
                            color: 'var(--color-accent)',
                          }}
                        >
                          {tag}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
              </>
            )}
          </div>
        )}

        {/* Metadata */}
        {!editing && (
          <div className="text-[11px] text-center py-2" style={{ color: 'var(--text-tertiary)' }}>
            创建于 {formatDate(item.createdAt)} · 更新于 {formatRelativeTime(item.updatedAt)}
          </div>
        )}
      </div>

      <DeleteConfirmationDialog
        open={showDelete}
        message={`确定要删除「${item.name}」吗？此操作不可撤销。`}
        onConfirm={handleDelete}
        onCancel={() => setShowDelete(false)}
      />

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

function InfoRow({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="px-4 py-3 flex items-start gap-3">
      <span style={{ color: 'var(--text-secondary)', marginTop: '2px' }}>{icon}</span>
      <div>
        <p className="text-xs" style={{ color: 'var(--text-secondary)' }}>{label}</p>
        <p className="text-sm mt-0.5" style={{ color: 'var(--text-primary)' }}>{value}</p>
      </div>
    </div>
  );
}

function EditField({ label, required, icon, value, onChange, placeholder, type = 'text', error, inputStyle }: {
  label: string; required?: boolean; icon: React.ReactNode; value: string; onChange: (v: string) => void; placeholder?: string; type?: string; error?: string; inputStyle?: React.CSSProperties;
}) {
  return (
    <div className="space-y-1.5">
      <label className="flex items-center gap-1.5 text-sm font-medium" style={{ color: 'var(--text-primary)' }}>
        {icon} {label} {required && <span style={{ color: 'var(--color-error)' }}>*</span>}
      </label>
      <input
        type={type}
        value={value}
        onChange={e => onChange(e.target.value)}
        placeholder={placeholder}
        style={{
          ...inputStyle,
          borderColor: error ? 'var(--color-error)' : undefined,
        }}
      />
      {error && <p className="text-xs" style={{ color: 'var(--color-error)' }}>{error}</p>}
    </div>
  );
}

function EditableImage({ path, name, onRemove }: { path: string; name: string; onRemove: () => void }) {
  const [url, setUrl] = useState<string | null>(null);
  useEffect(() => {
    if (path.startsWith('img:')) {
      getImageUrl(path).then(u => { if (u) setUrl(u); });
    }
  }, [path]);
  return (
    <div className="relative group">
      {url ? (
        <img
          src={url}
          alt=""
          className="w-20 h-20 rounded-2xl object-cover"
          style={{ border: '1px solid var(--glass-border)' }}
        />
      ) : (
        <div
          className="w-20 h-20 rounded-2xl flex items-center justify-center text-xl font-semibold"
          style={{
            background: 'var(--glass-bg)',
            backdropFilter: 'var(--glass-blur)',
            WebkitBackdropFilter: 'var(--glass-blur)',
            border: '1px solid var(--glass-border)',
            color: 'var(--color-accent)',
          }}
        >
          {name.charAt(0).toUpperCase()}
        </div>
      )}
      <button
        onClick={onRemove}
        className="absolute -top-1.5 -right-1.5 w-5 h-5 rounded-full text-white
                   flex items-center justify-center opacity-0 group-hover:opacity-100
                   transition-all duration-200 text-xs"
        style={{ background: 'var(--color-error)' }}
      >
        x
      </button>
    </div>
  );
}
