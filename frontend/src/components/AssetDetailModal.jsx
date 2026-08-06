import { useState } from 'react'
import { profitClass, ASSET_ICONS, ASSET_LABELS, ASSET_COLORS } from '../utils/helpers'
import { useFmt } from '../context/PreferencesContext'
import BuySellModal from './BuySellModal'

export default function AssetDetailModal({ item, onClose, onBuy, onSell }) {
  const [mode, setMode] = useState(null) // 'buy' | 'sell' | null
  const fmt = useFmt()

  const plClass = profitClass(item.profitLoss)
  const badge = ASSET_ICONS[item.assetType] || '🔷'

  return (
    <>
      {/* Backdrop */}
      <div className="modal-backdrop" onClick={onClose}>
        {/* Sheet */}
        <div
          className="bg-[#1e293b] rounded-t-3xl w-full max-w-md p-6 pb-8
                     border-t border-slate-700"
          onClick={e => e.stopPropagation()}
        >
          {/* Drag handle */}
          <div className="w-10 h-1 bg-slate-600 rounded-full mx-auto mb-5" />

          {/* Header */}
          <div className="flex items-center gap-3 mb-5">
            <div className={`w-12 h-12 rounded-2xl flex items-center justify-center text-2xl
                            ${ASSET_COLORS[item.assetType] || 'bg-slate-700'}`}>
              {badge}
            </div>
            <div className="flex-1 min-w-0">
              <h2 className="text-lg font-bold text-white truncate">{item.name}</h2>
              <p className="text-sm text-slate-400">
                {item.symbol} · {ASSET_LABELS[item.assetType] || item.assetType}
              </p>
            </div>
            <button onClick={onClose} className="text-slate-500 hover:text-white transition-colors">
              ✕
            </button>
          </div>

          {/* Stats grid */}
          <div className="grid grid-cols-2 gap-3 mb-6">
            <StatCard label="Current Price"    value={fmt.currency(item.currentPrice)} />
            <StatCard label="Quantity"         value={fmt.number(item.quantity)} />
            <StatCard label="Current Value"    value={fmt.currency(item.currentValue)} />
            <StatCard label="Total Invested"   value={fmt.currency(item.totalInvestment)} />
            <StatCard
              label="Profit / Loss"
              value={fmt.currency(item.profitLoss)}
              valueClass={plClass}
            />
            <StatCard
              label="Return %"
              value={fmt.percent(item.profitLossPercentage)}
              valueClass={plClass}
            />
          </div>

          {/* Buy / Sell buttons */}
          <div className="flex gap-3">
            <button
              onClick={() => setMode('buy')}
              className="flex-1 py-3 rounded-xl bg-blue-600 hover:bg-blue-500
                         text-white font-semibold transition-colors"
            >
              Buy More
            </button>
            <button
              onClick={() => setMode('sell')}
              className="flex-1 py-3 rounded-xl bg-red-600/20 hover:bg-red-600/30
                         text-red-400 font-semibold border border-red-600/30 transition-colors"
            >
              Sell
            </button>
          </div>
        </div>
      </div>

      {/* Buy / Sell quantity modal */}
      {mode && (
        <BuySellModal
          mode={mode}
          item={item}
          onClose={() => setMode(null)}
          onConfirm={async (qty) => {
            if (mode === 'buy') {
              await onBuy(item, qty)
            } else {
              await onSell(item, qty)
            }
            setMode(null)
            onClose()
          }}
        />
      )}
    </>
  )
}

function StatCard({ label, value, valueClass = 'text-slate-100' }) {
  return (
    <div className="bg-slate-700/40 rounded-xl p-3">
      <p className="text-xs text-slate-500 mb-1">{label}</p>
      <p className={`text-sm font-bold ${valueClass}`}>{value}</p>
    </div>
  )
}

