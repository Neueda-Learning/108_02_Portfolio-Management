import { useState } from 'react'
import { useFmt } from '../context/PreferencesContext'

export default function BuySellModal({ mode, item, onClose, onConfirm }) {
  const fmt = useFmt()
  const [qty, setQty]       = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError]   = useState('')

  const isBuy  = mode === 'buy'
  const price  = parseFloat(item.currentPrice || item.purchasePrice || 0)
  const total  = price * (parseFloat(qty) || 0)
  const maxSell = parseFloat(item.quantity)

  const validate = () => {
    const n = parseFloat(qty)
    if (!qty || isNaN(n) || n <= 0) return 'Enter a valid quantity'
    if (n < 0.0001)                  return 'Minimum quantity is 0.0001'
    if (!isBuy && n > maxSell)       return `You only own ${fmt.number(maxSell)} units`
    return ''
  }

  const handleConfirm = async () => {
    const err = validate()
    if (err) { setError(err); return }
    setLoading(true)
    try {
      await onConfirm(parseFloat(qty))
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="bg-[#1e293b] rounded-t-3xl w-full max-w-md p-6 pb-8 border-t border-slate-700"
        onClick={e => e.stopPropagation()}
      >
        <div className="w-10 h-1 bg-slate-600 rounded-full mx-auto mb-5" />

        <h2 className="text-lg font-bold text-white mb-1">
          {isBuy ? 'Buy' : 'Sell'} {item.symbol}
        </h2>
        <p className="text-sm text-slate-400 mb-6">
          Current price: <span className="text-white font-semibold">{fmt.currency(price)}</span>
          {!isBuy && (
            <span className="ml-2 text-slate-500">· You own {fmt.number(maxSell)}</span>
          )}
        </p>

        {/* Quantity input */}
        <label className="block text-xs text-slate-400 mb-2">Quantity</label>
        <input
          type="number"
          min="0.0001"
          step="0.0001"
          placeholder="0.00"
          value={qty}
          onChange={e => { setQty(e.target.value); setError('') }}
          className="w-full bg-slate-700/50 border border-slate-600 rounded-xl
                     px-4 py-3 text-white text-base placeholder-slate-600
                     focus:outline-none focus:border-blue-500 transition-colors mb-2"
          autoFocus
        />

        {/* Total */}
        {parseFloat(qty) > 0 && (
          <p className="text-sm text-slate-400 mb-4">
            Total:{' '}
            <span className="text-white font-bold">{fmt.currency(total)}</span>
          </p>
        )}

        {/* Error */}
        {error && <p className="text-red-400 text-sm mb-4">{error}</p>}

        {/* Actions */}
        <div className="flex gap-3">
          <button
            onClick={onClose}
            className="flex-1 py-3 rounded-xl border border-slate-600
                       text-slate-400 font-semibold hover:bg-slate-700 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleConfirm}
            disabled={loading}
            className={`flex-1 py-3 rounded-xl font-semibold transition-colors
              ${isBuy
                ? 'bg-blue-600 hover:bg-blue-500 text-white'
                : 'bg-red-600 hover:bg-red-500 text-white'}
              ${loading ? 'opacity-50 cursor-not-allowed' : ''}`}
          >
            {loading ? 'Processing…' : `Confirm ${isBuy ? 'Buy' : 'Sell'}`}
          </button>
        </div>
      </div>
    </div>
  )
}

