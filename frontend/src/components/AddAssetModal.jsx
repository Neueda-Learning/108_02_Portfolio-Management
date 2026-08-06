import { useMemo, useState } from 'react'
import { ASSET_TYPES, ASSET_LABELS, ASSET_ICONS } from '../utils/helpers'
import { ASSET_CATALOG } from '../utils/assetCatalog'

// Used on Assets page to buy a brand-new asset (not yet owned)
export default function AddAssetModal({ onClose, onAdd }) {
  const [form, setForm] = useState({
    assetType: 'STOCK',
    symbol: '',
    name: '',
    quantity: '',
    purchasePrice: '',
    notes: ''
  })
  const [catalogSearch, setCatalogSearch] = useState('')
  const [loading, setLoading] = useState(false)
  const [errors, setErrors] = useState({})

  const set = (key, val) => setForm(f => ({ ...f, [key]: val }))

  const filteredCatalog = useMemo(() => {
    const query = catalogSearch.trim().toLowerCase()
    return ASSET_CATALOG
      .filter(a => a.assetType === form.assetType)
      .filter(a => !query || a.symbol.toLowerCase().includes(query) || a.name.toLowerCase().includes(query))
      .slice(0, 25)
  }, [catalogSearch, form.assetType])

  const pickFromCatalog = (asset) => {
    setForm(f => ({
      ...f,
      assetType: asset.assetType,
      symbol: asset.symbol,
      name: asset.name
    }))
    setErrors(prev => ({ ...prev, symbol: '', name: '' }))
  }

  const validate = () => {
    const e = {}
    if (!form.symbol.trim()) e.symbol = 'Symbol is required'
    if (!form.name.trim()) e.name = 'Name is required'
    const qty = parseFloat(form.quantity)
    if (!form.quantity || isNaN(qty) || qty < 0.0001) e.quantity = 'Quantity must be >= 0.0001'
    const price = parseFloat(form.purchasePrice)
    if (!form.purchasePrice || isNaN(price) || price < 0.01) e.purchasePrice = 'Price must be >= 0.01'
    return e
  }

  const handleSubmit = async () => {
    const e = validate()
    if (Object.keys(e).length) { setErrors(e); return }
    setLoading(true)
    try {
      await onAdd({
        assetType: form.assetType,
        symbol: form.symbol.toUpperCase(),
        name: form.name,
        quantity: parseFloat(form.quantity),
        purchasePrice: parseFloat(form.purchasePrice),
        notes: form.notes
      })
      onClose()
    } catch (err) {
      setErrors({ submit: err.message })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="bg-[#1e293b] rounded-t-3xl w-full max-w-md p-6 pb-8
                   border-t border-slate-700 max-h-[90vh] overflow-y-auto"
        onClick={e => e.stopPropagation()}
      >
        <div className="w-10 h-1 bg-slate-600 rounded-full mx-auto mb-5" />
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-lg font-bold text-white">Add New Asset</h2>
          <button onClick={onClose} className="text-slate-500 hover:text-white">✕</button>
        </div>

        {/* Asset type */}
        <label className="block text-xs text-slate-400 mb-2">Asset Type</label>
        <div className="grid grid-cols-4 gap-2 mb-4">
          {ASSET_TYPES.filter(t => t !== 'CASH').map(type => (
            <button
              key={type}
              onClick={() => set('assetType', type)}
              className={`flex flex-col items-center gap-1 p-2 rounded-xl border text-xs
                font-medium transition-colors
                ${form.assetType === type
                  ? 'border-blue-500 bg-blue-500/20 text-blue-300'
                  : 'border-slate-700 bg-slate-700/30 text-slate-400 hover:border-slate-600'}`}
            >
              <span className="text-base">{ASSET_ICONS[type]}</span>
              <span className="truncate w-full text-center">{ASSET_LABELS[type]}</span>
            </button>
          ))}
        </div>

        {/* Discover assets */}
        <div className="mb-4">
          <label className="block text-xs text-slate-400 mb-1">Browse Popular {ASSET_LABELS[form.assetType]}</label>
          <input
            type="text"
            value={catalogSearch}
            onChange={e => setCatalogSearch(e.target.value)}
            placeholder="Search by symbol or name"
            className="w-full bg-slate-700/50 border border-slate-600 rounded-xl px-4 py-2.5 text-white text-sm
                       placeholder-slate-600 focus:outline-none focus:border-blue-500 mb-2"
          />
          <div className="max-h-40 overflow-y-auto rounded-xl border border-slate-700 bg-slate-800/40">
            {filteredCatalog.length === 0 ? (
              <p className="text-xs text-slate-500 px-3 py-2">No assets found for this filter.</p>
            ) : (
              filteredCatalog.map(asset => (
                <button
                  key={`${asset.assetType}-${asset.symbol}`}
                  onClick={() => pickFromCatalog(asset)}
                  className="w-full px-3 py-2 text-left hover:bg-slate-700/50 border-b border-slate-700 last:border-b-0"
                >
                  <p className="text-sm text-white font-semibold">{asset.symbol}</p>
                  <p className="text-xs text-slate-400 truncate">{asset.name}</p>
                </button>
              ))
            )}
          </div>
        </div>

        {/* Fields */}
        {[
          { key: 'symbol', label: 'Ticker / Symbol', placeholder: 'e.g. AAPL', type: 'text' },
          { key: 'name', label: 'Asset Name', placeholder: 'e.g. Apple Inc.', type: 'text' },
          { key: 'quantity', label: 'Quantity', placeholder: '0.0001', type: 'number' },
          { key: 'purchasePrice', label: 'Buy Price (USD)', placeholder: '0.01', type: 'number' },
          { key: 'notes', label: 'Notes (optional)', placeholder: '', type: 'text' }
        ].map(f => (
          <div key={f.key} className="mb-4">
            <label className="block text-xs text-slate-400 mb-1">{f.label}</label>
            <input
              type={f.type}
              placeholder={f.placeholder}
              value={form[f.key]}
              onChange={e => { set(f.key, e.target.value); setErrors(prev => ({ ...prev, [f.key]: '' })) }}
              className={`w-full bg-slate-700/50 border rounded-xl px-4 py-3 text-white text-sm
                placeholder-slate-600 focus:outline-none transition-colors
                ${errors[f.key] ? 'border-red-500' : 'border-slate-600 focus:border-blue-500'}`}
            />
            {errors[f.key] && <p className="text-red-400 text-xs mt-1">{errors[f.key]}</p>}
          </div>
        ))}

        {errors.submit && <p className="text-red-400 text-sm mb-4">{errors.submit}</p>}

        <div className="flex gap-3">
          <button
            onClick={onClose}
            className="flex-1 py-3 rounded-xl border border-slate-600
                       text-slate-400 font-semibold hover:bg-slate-700 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleSubmit}
            disabled={loading}
            className={`flex-1 py-3 rounded-xl bg-blue-600 hover:bg-blue-500
                        text-white font-semibold transition-colors
                        ${loading ? 'opacity-50 cursor-not-allowed' : ''}`}
          >
            {loading ? 'Adding…' : 'Add & Buy'}
          </button>
        </div>
      </div>
    </div>
  )
}
