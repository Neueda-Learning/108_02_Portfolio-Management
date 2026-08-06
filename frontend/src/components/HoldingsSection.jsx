import { ASSET_LABELS, ASSET_ICONS, ASSET_COLORS, profitClass } from '../utils/helpers'
import { useFmt } from '../context/PreferencesContext'

// Single holding row
function HoldingCard({ item, onClick }) {
  const fmt = useFmt()
  const badge = `${ASSET_ICONS[item.assetType] || '🔷'}`
  const plClass = profitClass(item.profitLoss)

  return (
    <button
      onClick={() => onClick(item)}
      className="w-full flex items-center gap-3 p-3 rounded-xl bg-slate-800/60
                 hover:bg-slate-700/60 active:scale-[0.98] transition-all text-left"
    >
      {/* Icon */}
      <div className={`w-10 h-10 rounded-xl flex items-center justify-center text-lg flex-shrink-0
                       ${ASSET_COLORS[item.assetType] || 'bg-slate-700 text-slate-300'}`}>
        {badge}
      </div>

      {/* Name + symbol */}
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-slate-100 truncate">{item.name}</p>
        <p className="text-xs text-slate-500">{item.symbol} · {fmt.number(item.quantity)} units</p>
      </div>

      {/* Values */}
      <div className="text-right flex-shrink-0">
        <p className="text-sm font-bold text-slate-100">{fmt.currency(item.currentValue)}</p>
        <p className={`text-xs font-medium ${plClass}`}>
          {fmt.percent(item.profitLossPercentage)}
        </p>
      </div>

    </button>
  )
}

// Holdings grouped by assetType
export default function HoldingsSection({ items, onItemClick }) {
  if (!items || items.length === 0) {
    return (
      <div className="text-center text-slate-500 text-sm py-10">
        No holdings yet. Go to Assets to buy your first investment.
      </div>
    )
  }

  // Group by assetType
  const groups = items.reduce((acc, item) => {
    const type = item.assetType
    if (!acc[type]) acc[type] = []
    acc[type].push(item)
    return acc
  }, {})

  return (
    <div className="flex flex-col gap-6">
      {Object.entries(groups).map(([type, typeItems]) => (
        <section key={type}>
          {/* Group header */}
          <div className="flex items-center gap-2 mb-2 px-1">
            <span className="text-base">{ASSET_ICONS[type] || '🔷'}</span>
            <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-widest">
              {ASSET_LABELS[type] || type}
            </h3>
            <span className="ml-auto text-xs text-slate-600">{typeItems.length}</span>
          </div>

          {/* Items */}
          <div className="flex flex-col gap-2">
            {typeItems.map(item => (
              <HoldingCard key={item.id} item={item} onClick={onItemClick} />
            ))}
          </div>
        </section>
      ))}
    </div>
  )
}

