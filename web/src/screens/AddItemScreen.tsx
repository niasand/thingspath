import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApp } from '../context/AppContext';
import { itemRepository } from '../db/repository';
import { saveImage } from '../services/imageStorage';
import TopBar from '../components/TopBar';
import TagInput from '../components/TagInput';
import { todayString, calculateUsageDays } from '../utils/dateUtils';
import { MapPin, Calendar, Hash, DollarSign, FileText } from 'lucide-react';

export default function AddItemScreen() {
  const { dispatch } = useApp();
  const navigate = useNavigate();

  const [name, setName] = useState('');
  const [location, setLocation] = useState('');
  const [purchaseDate, setPurchaseDate] = useState(todayString());
  const [purchasePrice, setPurchasePrice] = useState('');
  const [usageDays, setUsageDays] = useState('');
  const [note, setNote] = useState('');
  const [tags, setTags] = useState<string[]>([]);
  const [imagePaths, setImagePaths] = useState<string[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const validate = (): boolean => {
    const e: Record<string, string> = {};
    if (!name.trim()) e.name = '请输入物品名称';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSave = async () => {
    if (!validate()) return;
    setSaving(true);

    try {
      const dateMs = purchaseDate ? new Date(purchaseDate).getTime() : null;
      const days = usageDays
        ? parseInt(usageDays, 10)
        : calculateUsageDays(dateMs);

      const now = Date.now();
      const id = await itemRepository.addItem({
        name: name.trim(),
        imagePath: imagePaths[0] || null,
        imagePaths,
        location: location.trim() || null,
        purchaseDate: dateMs,
        purchasePrice: parseFloat(purchasePrice) || 0,
        usageDays: days && days >= 0 ? days : null,
        note: note.trim() || null,
        tags,
        createdAt: now,
        updatedAt: now,
      });

      // Update image itemId references
      for (const path of imagePaths) {
        if (path.startsWith('img:')) {
          const recordId = parseInt(path.slice(4), 10);
          const { db } = await import('../db/database');
          await db.images.update(recordId, { itemId: id });
        }
      }

      dispatch({ type: 'SHOW_SNACKBAR', message: '添加成功', snackbarType: 'success' });
      navigate(-1);
    } catch (err) {
      dispatch({ type: 'SHOW_SNACKBAR', message: '保存失败', snackbarType: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleImageChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files) return;
    for (const file of Array.from(files)) {
      const recordId = await saveImage(0, file);
      setImagePaths(prev => [...prev, `img:${recordId}`]);
    }
    e.target.value = '';
  };

  const glassCard = {
    background: 'var(--glass-bg)',
    backdropFilter: 'var(--glass-blur)',
    WebkitBackdropFilter: 'var(--glass-blur)',
    border: '1px solid var(--glass-border)',
    borderRadius: '20px',
  };

  const inputStyle = {
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
        title="添加物品"
        showBack
        actions={
          <button
            onClick={handleSave}
            disabled={saving}
            className="px-5 py-1.5 rounded-xl text-sm font-medium text-white transition-all duration-200
                       disabled:opacity-50 hover:scale-105 active:scale-95"
            style={{
              background: 'var(--gradient-accent)',
              boxShadow: '0 4px 16px var(--shadow-accent, rgba(124,58,237,0.3))',
            }}
          >
            {saving ? '保存中...' : '保存'}
          </button>
        }
      />

      <div className="space-y-4">
        {/* Images */}
        <div className="space-y-2">
          <label className="block text-sm font-medium" style={{ color: 'var(--text-primary)' }}>
            图片
          </label>
          <div className="flex gap-3 flex-wrap">
            {imagePaths.map((path, i) => (
              <ImagePreview key={path} path={path} name={name} onRemove={() =>
                setImagePaths(prev => prev.filter((_, j) => j !== i))
              } />
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
                <span className="text-[10px]">添加图片</span>
                <input type="file" accept="image/*" multiple className="hidden" onChange={handleImageChange} />
              </label>
            )}
          </div>
        </div>

        {/* Form Card Container */}
        <div className="space-y-4 p-5" style={glassCard}>
          {/* Name */}
          <FormField
            label="物品名称" required
            icon={<Hash size={16} />}
            value={name}
            onChange={setName}
            placeholder="例如：索尼WH-1000XM5"
            error={errors.name}
            inputStyle={inputStyle}
          />

          {/* Location */}
          <FormField
            label="存放位置"
            icon={<MapPin size={16} />}
            value={location}
            onChange={setLocation}
            placeholder="例如：卧室床头柜"
            inputStyle={inputStyle}
          />

          {/* Purchase Date */}
          <FormField
            label="购买日期"
            icon={<Calendar size={16} />}
            value={purchaseDate}
            onChange={setPurchaseDate}
            type="date"
            inputStyle={inputStyle}
          />

          {/* Price + Usage Days */}
          <div className="grid grid-cols-2 gap-3">
            <FormField
              label="购买价格"
              icon={<DollarSign size={16} />}
              value={purchasePrice}
              onChange={setPurchasePrice}
              placeholder="0.00"
              type="number"
              inputStyle={inputStyle}
            />
            <FormField
              label="使用天数"
              icon={<Hash size={16} />}
              value={usageDays}
              onChange={setUsageDays}
              placeholder="自动计算"
              type="number"
              inputStyle={inputStyle}
            />
          </div>

          {/* Note */}
          <div className="space-y-1.5">
            <label className="flex items-center gap-1.5 text-sm font-medium" style={{ color: 'var(--text-primary)' }}>
              <FileText size={14} style={{ color: 'var(--text-secondary)' }} />
              备注
            </label>
            <textarea
              value={note}
              onChange={e => setNote(e.target.value)}
              placeholder="添加备注..."
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
            />
          </div>

          {/* Tags */}
          <TagInput tags={tags} onChange={setTags} />
        </div>
      </div>
    </div>
  );
}

function FormField({
  label, required, icon, value, onChange, placeholder, type = 'text', error, inputStyle,
}: {
  label: string; required?: boolean; icon: React.ReactNode;
  value: string; onChange: (v: string) => void; placeholder?: string; type?: string; error?: string;
  inputStyle?: React.CSSProperties;
}) {
  return (
    <div className="space-y-1.5">
      <label className="flex items-center gap-1.5 text-sm font-medium" style={{ color: 'var(--text-primary)' }}>
        {icon}
        {label}
        {required && <span style={{ color: 'var(--color-error)' }}>*</span>}
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

function ImagePreview({ path, name, onRemove }: { path: string; name: string; onRemove: () => void }) {
  const [url, setUrl] = useState<string | null>(null);
  const [err, setErr] = useState(false);

  useEffect(() => {
    if (path.startsWith('img:')) {
      import('../services/imageStorage').then(({ getImageUrl }) =>
        getImageUrl(path).then(u => { if (u) setUrl(u); }).catch(() => setErr(true)),
      );
    }
  }, [path]);

  return (
    <div className="relative group">
      {url && !err ? (
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
          {name.charAt(0).toUpperCase() || '?'}
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
