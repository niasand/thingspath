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

  return (
    <div>
      <TopBar
        title="添加物品"
        showBack
        actions={
          <button
            onClick={handleSave}
            disabled={saving}
            className="px-4 py-1.5 rounded-xl text-sm font-medium bg-primary text-white
                       hover:bg-primary-dark transition-colors disabled:opacity-50"
          >
            {saving ? '保存中...' : '保存'}
          </button>
        }
      />

      <div className="space-y-4">
        {/* Images */}
        <div className="space-y-2">
          <label className="block text-sm font-medium text-text">图片</label>
          <div className="flex gap-2 flex-wrap">
            {imagePaths.map((path, i) => (
              <ImagePreview key={path} path={path} name={name} onRemove={() =>
                setImagePaths(prev => prev.filter((_, j) => j !== i))
              } />
            ))}
            {imagePaths.length < 5 && (
              <label className="w-20 h-20 rounded-xl border-2 border-dashed border-border
                               flex flex-col items-center justify-center cursor-pointer
                               hover:border-primary/40 hover:text-primary text-text-secondary transition-colors">
                <span className="text-lg">+</span>
                <span className="text-[10px]">添加图片</span>
                <input type="file" accept="image/*" multiple className="hidden" onChange={handleImageChange} />
              </label>
            )}
          </div>
        </div>

        {/* Name */}
        <FormField
          label="物品名称" required
          icon={<Hash size={16} />}
          value={name}
          onChange={setName}
          placeholder="例如：索尼WH-1000XM5"
          error={errors.name}
        />

        {/* Location */}
        <FormField
          label="存放位置"
          icon={<MapPin size={16} />}
          value={location}
          onChange={setLocation}
          placeholder="例如：卧室床头柜"
        />

        {/* Purchase Date */}
        <FormField
          label="购买日期"
          icon={<Calendar size={16} />}
          value={purchaseDate}
          onChange={setPurchaseDate}
          type="date"
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
          />
          <FormField
            label="使用天数"
            icon={<Hash size={16} />}
            value={usageDays}
            onChange={setUsageDays}
            placeholder="自动计算"
            type="number"
          />
        </div>

        {/* Note */}
        <div className="space-y-1.5">
          <label className="flex items-center gap-1.5 text-sm font-medium text-text">
            <FileText size={14} className="text-text-secondary" />
            备注
          </label>
          <textarea
            value={note}
            onChange={e => setNote(e.target.value)}
            placeholder="添加备注..."
            rows={3}
            className="w-full px-3 py-2.5 rounded-xl border border-border/60 bg-surface text-sm
                       placeholder:text-text-tertiary focus:outline-none focus:border-primary/40 focus:ring-2
                       focus:ring-primary/10 resize-none transition-all"
          />
        </div>

        {/* Tags */}
        <TagInput tags={tags} onChange={setTags} />
      </div>
    </div>
  );
}

function FormField({
  label, required, icon, value, onChange, placeholder, type = 'text', error,
}: {
  label: string; required?: boolean; icon: React.ReactNode;
  value: string; onChange: (v: string) => void; placeholder?: string; type?: string; error?: string;
}) {
  return (
    <div className="space-y-1.5">
      <label className="flex items-center gap-1.5 text-sm font-medium text-text">
        {icon}
        {label}
        {required && <span className="text-error">*</span>}
      </label>
      <input
        type={type}
        value={value}
        onChange={e => onChange(e.target.value)}
        placeholder={placeholder}
        className={`w-full px-3 py-2.5 rounded-xl border text-sm bg-surface
                   placeholder:text-text-tertiary focus:outline-none focus:ring-2 transition-all
                   ${error ? 'border-error focus:border-error focus:ring-error/10' : 'border-border/60 focus:border-primary/40 focus:ring-primary/10'}`}
      />
      {error && <p className="text-xs text-error">{error}</p>}
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
        <img src={url} alt="" className="w-20 h-20 rounded-xl object-cover" />
      ) : (
        <div className="w-20 h-20 rounded-xl flex items-center justify-center text-xl font-semibold"
             style={{ backgroundColor: '#E8DEF8', color: '#6650a4' }}>
          {name.charAt(0).toUpperCase() || '?'}
        </div>
      )}
      <button
        onClick={onRemove}
        className="absolute -top-1.5 -right-1.5 w-5 h-5 rounded-full bg-error text-white
                   flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity text-xs"
      >
        x
      </button>
    </div>
  );
}
