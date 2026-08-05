import { useState, useMemo } from 'react'
import { usePortfolio } from '../context/PortfolioContext'
import { useFmt } from '../context/PreferencesContext'
import PortfolioValueGraph from '../components/PortfolioValueGraph'
import AssetDetailModal from '../components/AssetDetailModal'
import BuySellModal from '../components/BuySellModal'
import { profitClass, ASSET_ICONS, ASSET_LABELS, ASSET_COLORS } from '../utils/helpers'
import {
  PieChart, Pie, Cell, Tooltip as PieTooltip, ResponsiveContainer, Sector
} from 'recharts'

// Distinct colours per asset type for the pie
const PIE_COLORS = {
  STOCK:       '#3b82f6',
  ETF:         '#a855f7',
  CRYPTO:      '#06b6d4',
  BOND:        '#eab308',
  MUTUAL_FUND: '#f97316',
  CASH:        '#22c55e',
  OTHER:       '#64748b',
}

function AllocationPie({ items }) {
  const [activeIndex, setActiveIndex] = useState(null)
  const fmt = useFmt()

  const data = useMemo(() => {
    const totals = {}
    items.forEach(item => {
      const type = item.assetType
      totals[type] = (totals[type] || 0) + Number(item.currentValue || 0)
    })
    return Object.entries(totals)
      .filter(([, v]) => v > 0)
      .map(([type, value]) => ({ name: ASSET_LABELS[type] || type, value, type }))
  }, [items])

  if (data.length === 0) return (
    <div className="h-full flex items-center justify-center text-slate-500 text-sm">
      No holdings yet.
    </div>
  )

  const totalValue = data.reduce((sum, entry) => sum + Number(entry.value || 0), 0)

  const renderActiveShape = (props) => {
    const {
      cx, cy, innerRadius, outerRadius, startAngle, endAngle,
      fill, payload, percent, value
    } = props

    return (
      <g>
        <Sector
          cx={cx}
          cy={cy}
          innerRadius={innerRadius}
          outerRadius={outerRadius + 8}
          startAngle={startAngle}
          endAngle={endAngle}
          fill={fill}
          opacity={1}
        />
        <text
          x={cx}
          y={cy - 10}
          textAnchor="middle"
          fill="#e2e8f0"
          fontSize={12}
          fontWeight="700"
        >
          {payload.name}
        </text>
        <text
          x={cx}
          y={cy + 10}
          textAnchor="middle"
          fill="#94a3b8"
          fontSize={11}
        >
          {fmt.currency(value)} ({(percent * 100).toFixed(1)}%)
        </text>
      </g>
    )
  }

  const customLabel = ({ cx, cy, midAngle, innerRadius, outerRadius, percent }) => {
    if (percent < 0.05) return null
    const RADIAN = Math.PI / 180
    const r = innerRadius + (outerRadius - innerRadius) * 0.55
    const x = cx + r * Math.cos(-midAngle * RADIAN)
    const y = cy + r * Math.sin(-midAngle * RADIAN)
    return (
      <text x={x} y={y} fill="white" textAnchor="middle" dominantBaseline="central" fontSize={11} fontWeight="700">
        {(percent * 100).toFixed(0)}%
      </text>
    )
  }

  return (
    <div className="h-full flex flex-col gap-3">
      <div className="relative flex-1 min-h-[210px] rounded-xl bg-slate-900/40 border border-slate-700/40 overflow-hidden">
        <div className="absolute inset-0 pointer-events-none bg-[radial-gradient(circle_at_50%_45%,rgba(59,130,246,0.16),transparent_60%)]" />

        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={data}
              activeIndex={activeIndex}
              activeShape={renderActiveShape}
              onMouseEnter={(_, index) => setActiveIndex(index)}
              onMouseLeave={() => setActiveIndex(null)}
              cx="50%"
              cy="50%"
              labelLine={false}
              label={customLabel}
              outerRadius="76%"
              innerRadius="38%"
              dataKey="value"
              paddingAngle={2}
              cornerRadius={4}
            >
              {data.map((entry, index) => (
                <Cell
                  key={entry.type}
                  fill={PIE_COLORS[entry.type] || '#64748b'}
                  stroke="#0f172a"
                  strokeWidth={activeIndex === index ? 2 : 1}
                  opacity={activeIndex === null || activeIndex === index ? 1 : 0.35}
                  style={{ cursor: 'pointer' }}
                />
              ))}
            </Pie>
            <PieTooltip
              formatter={(value, name) => {
                const pct = totalValue > 0 ? (Number(value) / totalValue) * 100 : 0
                return [`${fmt.currency(value)}  (${pct.toFixed(1)}%)`, name]
              }}
              contentStyle={{
                background: '#0f172a',
                border: '1px solid #334155',
                borderRadius: '10px',
                fontSize: '12px',
                color: '#e2e8f0'
              }}
              itemStyle={{ color: '#e2e8f0' }}
              labelStyle={{ color: '#94a3b8' }}
            />
          </PieChart>
        </ResponsiveContainer>

        <div className="absolute inset-0 pointer-events-none flex flex-col items-center justify-center">
          <p className="text-[10px] uppercase tracking-widest text-slate-500">Total</p>
          <p className="text-base font-bold text-slate-100">{fmt.currency(totalValue)}</p>
          <p className="text-[10px] text-slate-500">{data.length} types</p>
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        {data.map(entry => (
          <button
            key={entry.type}
            onMouseEnter={() => setActiveIndex(data.findIndex(d => d.type === entry.type))}
            onMouseLeave={() => setActiveIndex(null)}
            className="px-2.5 py-1.5 rounded-lg bg-slate-800/60 border border-slate-700/60 text-[11px] text-slate-300 flex items-center gap-1.5"
          >
            <span className="w-2.5 h-2.5 rounded-full" style={{ background: PIE_COLORS[entry.type] || '#64748b' }} />
            <span>{entry.name}</span>
          </button>
        ))}
      </div>
    </div>
  )
}

export default function WebDashboard() {
  const { portfolio, summary, graphData, loading, buyAsset, sellAsset, triggerPriceRefresh } = usePortfolio()
  const fmt = useFmt()
  const [selectedItem, setSelectedItem] = useState(null)
  const [refreshing, setRefreshing] = useState(false)
  const [buySellTarget, setBuySellTarget] = useState(null)
  const [filterType, setFilterType] = useState(null)  // holdings filter

  const handleRefresh = async () => {
    setRefreshing(true)
    await triggerPriceRefresh()
    setRefreshing(false)
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex flex-col items-center gap-3">
          <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
          <p className="text-slate-400 text-sm">Loading portfolio…</p>
        </div>
      </div>
    )
  }

  const items = portfolio?.items || []
  const plClass = profitClass(summary?.totalProfitLoss ?? 0)
  const ownedTypes = [...new Set(items.map(i => i.assetType))]
  const filteredItems = filterType ? items.filter(i => i.assetType === filterType) : items

  return (
    <div className="flex flex-col gap-6">

      {/* ── Page header ──────────────────────────────────────────────────── */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-white">Dashboard</h2>
          <p className="text-slate-400 text-sm mt-0.5">{portfolio?.name || 'My Portfolio'}</p>
        </div>
        <button
          onClick={handleRefresh}
          disabled={refreshing}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl border border-slate-700
                      text-slate-400 hover:text-white hover:border-slate-500 transition-colors text-sm
                      ${refreshing ? 'animate-pulse' : ''}`}
        >
          <span className={refreshing ? 'animate-spin inline-block' : ''}>↻</span>
          Refresh prices
        </button>
      </div>

      {/* ── KPI cards ────────────────────────────────────────────────────── */}
      <div className="grid grid-cols-4 gap-4">
        <KpiCard label="Total Value"    value={summary ? fmt.currency(summary.currentValue)    : '—'} />
        <KpiCard label="Total Invested" value={summary ? fmt.currency(summary.totalInvestment) : '—'} />
        <KpiCard
          label="Profit / Loss"
          value={summary ? fmt.currency(summary.totalProfitLoss) : '—'}
          sub={summary ? fmt.percent(summary.totalProfitLossPercentage) : null}
          highlight={summary ? (Number(summary.totalProfitLoss) >= 0 ? 'green' : 'red') : null}
        />
        <KpiCard label="Holdings" value={`${summary?.totalItems ?? 0} assets`} />
      </div>

      {/* ── Three-panel row: Value Graph + PIE CHART + Allocation bars ───── */}
      <div className="grid grid-cols-3 gap-6">

        {/* Value graph */}
        <div className="bg-[#1e293b] rounded-2xl p-6 border border-slate-700/50">
          <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-widest mb-4">
            Portfolio Value
          </h3>
          <div className="h-48">
            <PortfolioValueGraph data={graphData} />
          </div>
        </div>

        {/* ── PIE CHART (centre) ───────────────────────────────────────── */}
        <div className="bg-[#1e293b] rounded-2xl p-6 border border-slate-700/50 flex flex-col">
          <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-widest mb-4">
            Holdings by Asset Type
          </h3>
          <div className="flex-1 min-h-[192px]">
            <AllocationPie items={items} />
          </div>
        </div>

        {/* Allocation bars */}
        <div className="bg-[#1e293b] rounded-2xl p-6 border border-slate-700/50 flex flex-col gap-3">
          <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-widest">
            Allocation %
          </h3>
          {items.length === 0 ? (
            <p className="text-slate-500 text-sm">No holdings yet.</p>
          ) : (
            Object.entries(
              items.reduce((acc, item) => {
                acc[item.assetType] = (acc[item.assetType] || 0) + Number(item.currentValue || 0)
                return acc
              }, {})
            ).map(([type, val]) => {
              const total = items.reduce((s, i) => s + Number(i.currentValue || 0), 0)
              const pct = total > 0 ? (val / total) * 100 : 0
              return (
                <div key={type}>
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-slate-300">{ASSET_ICONS[type]} {ASSET_LABELS[type] || type}</span>
                    <span className="text-slate-400">{pct.toFixed(1)}%</span>
                  </div>
                  <div className="h-1.5 bg-slate-700 rounded-full overflow-hidden">
                    <div className="h-full rounded-full transition-all"
                      style={{ width: `${pct}%`, background: PIE_COLORS[type] || '#64748b' }} />
                  </div>
                </div>
              )
            })
          )}
        </div>
      </div>


      {/* ── Holdings Table with filter ────────────────────────────────────── */}
      <div className="bg-[#1e293b] rounded-2xl border border-slate-700/50 overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-700/50 flex items-center gap-3 flex-wrap">
          <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-widest mr-2">
            Holdings
          </h3>
          {/* Filter chips */}
          <button
            onClick={() => setFilterType(null)}
            className={`px-3 py-1 rounded-lg border text-xs font-medium transition-colors
              ${!filterType ? 'border-blue-500 bg-blue-500/20 text-blue-300' : 'border-slate-700 text-slate-500 hover:border-slate-500'}`}
          >
            All ({items.length})
          </button>
          {ownedTypes.map(type => (
            <button
              key={type}
              onClick={() => setFilterType(filterType === type ? null : type)}
              className={`px-3 py-1 rounded-lg border text-xs font-medium transition-colors
                ${filterType === type ? 'border-blue-500 bg-blue-500/20 text-blue-300' : 'border-slate-700 text-slate-500 hover:border-slate-500'}`}
            >
              {ASSET_ICONS[type]} {ASSET_LABELS[type]} ({items.filter(i => i.assetType === type).length})
            </button>
          ))}
        </div>

        {filteredItems.length === 0 ? (
          <div className="px-6 py-12 text-center text-slate-500 text-sm">
            No holdings{filterType ? ` for ${ASSET_LABELS[filterType]}` : ''}.
          </div>
        ) : (
          <table className="w-full">
            <thead>
              <tr className="border-b border-slate-700/50">
                {['Asset', 'Type', 'Qty', 'Avg Price', 'Current Price', 'Invested', 'Value', 'P&L', 'Return', ''].map(h => (
                  <th key={h} className="px-4 py-3 text-left text-xs text-slate-500 font-medium uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {filteredItems.map(item => {
                const plc = profitClass(item.profitLoss)
                return (
                  <tr key={item.id} className="border-b border-slate-800/60 hover:bg-slate-700/20 transition-colors">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <span className={`w-8 h-8 rounded-lg flex items-center justify-center text-sm ${ASSET_COLORS[item.assetType] || 'bg-slate-700'}`}>
                          {ASSET_ICONS[item.assetType] || '🔷'}
                        </span>
                        <div>
                          <p className="text-sm font-semibold text-white">{item.symbol}</p>
                          <p className="text-xs text-slate-500 max-w-[120px] truncate">{item.name}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-xs px-2 py-0.5 rounded-full bg-slate-700 text-slate-300">
                        {ASSET_LABELS[item.assetType] || item.assetType}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-300">{fmt.number(item.quantity)}</td>
                    <td className="px-4 py-3 text-sm text-slate-300">{fmt.currency(item.purchasePrice)}</td>
                    <td className="px-4 py-3 text-sm text-white font-medium">{fmt.currency(item.currentPrice)}</td>
                    <td className="px-4 py-3 text-sm text-slate-300">{fmt.currency(item.totalInvestment)}</td>
                    <td className="px-4 py-3 text-sm text-white font-medium">{fmt.currency(item.currentValue)}</td>
                    <td className={`px-4 py-3 text-sm font-medium ${plc}`}>{fmt.currency(item.profitLoss)}</td>
                    <td className={`px-4 py-3 text-sm font-medium ${plc}`}>{fmt.percent(item.profitLossPercentage)}</td>
                    <td className="px-4 py-3">
                      <div className="flex gap-1">
                        <button onClick={() => setSelectedItem(item)}
                          className="px-2 py-1 rounded-lg bg-slate-700 hover:bg-slate-600 text-xs text-slate-300 transition-colors">
                          Details
                        </button>
                        <button onClick={() => setBuySellTarget({ item, mode: 'buy' })}
                          className="px-2 py-1 rounded-lg bg-blue-600/20 hover:bg-blue-600/40 text-xs text-blue-300 transition-colors">
                          Buy
                        </button>
                        <button onClick={() => setBuySellTarget({ item, mode: 'sell' })}
                          className="px-2 py-1 rounded-lg bg-red-600/20 hover:bg-red-600/40 text-xs text-red-300 transition-colors">
                          Sell
                        </button>
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </div>

      {/* ── Modals ───────────────────────────────────────────────────────── */}
      {selectedItem && (
        <AssetDetailModal
          item={selectedItem}
          onClose={() => setSelectedItem(null)}
          onBuy={async (item, qty) => {
            await buyAsset({ symbol: item.symbol, name: item.name, assetType: item.assetType, purchasePrice: item.currentPrice || item.purchasePrice, quantity: qty, notes: item.notes })
            setSelectedItem(null)
          }}
          onSell={async (item, qty) => { await sellAsset(item, qty); setSelectedItem(null) }}
        />
      )}
      {buySellTarget && (
        <BuySellModal
          mode={buySellTarget.mode}
          item={buySellTarget.item}
          onClose={() => setBuySellTarget(null)}
          onConfirm={async (qty) => {
            if (buySellTarget.mode === 'buy') {
              await buyAsset({ symbol: buySellTarget.item.symbol, name: buySellTarget.item.name, assetType: buySellTarget.item.assetType, purchasePrice: buySellTarget.item.currentPrice || buySellTarget.item.purchasePrice, quantity: qty, notes: '' })
            } else {
              await sellAsset(buySellTarget.item, qty)
            }
            setBuySellTarget(null)
          }}
        />
      )}
    </div>
  )
}

function KpiCard({ label, value, sub, highlight }) {
  const color = highlight === 'green' ? 'text-emerald-400' : highlight === 'red' ? 'text-rose-400' : 'text-white'
  return (
    <div className="bg-[#1e293b] rounded-2xl p-5 border border-slate-700/50">
      <p className="text-xs text-slate-500 uppercase tracking-wider mb-2">{label}</p>
      <p className={`text-2xl font-bold ${color}`}>{value}</p>
      {sub && <p className={`text-sm mt-1 ${color}`}>{sub}</p>}
    </div>
  )
}

// re-export PIE_COLORS so Dashboard.jsx can use it
export { PIE_COLORS }
