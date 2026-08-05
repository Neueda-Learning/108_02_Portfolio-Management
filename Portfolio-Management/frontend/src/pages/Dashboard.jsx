import { useState, useMemo } from 'react'
import { usePortfolio } from '../context/PortfolioContext'
import { useFmt } from '../context/PreferencesContext'
import PortfolioValueGraph from '../components/PortfolioValueGraph'
import HoldingsSection     from '../components/HoldingsSection'
import AssetDetailModal    from '../components/AssetDetailModal'
import { profitClass, ASSET_ICONS, ASSET_LABELS } from '../utils/helpers'
import { PieChart, Pie, Cell, Tooltip as PieTooltip, ResponsiveContainer, Sector } from 'recharts'

const PIE_COLORS = {
  STOCK: '#3b82f6', ETF: '#a855f7', CRYPTO: '#06b6d4',
  BOND: '#eab308', MUTUAL_FUND: '#f97316', CASH: '#22c55e', OTHER: '#64748b',
}

function AllocationPie({ items }) {
  const fmt = useFmt()
  const [activeIndex, setActiveIndex] = useState(null)

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
    <div className="h-full flex items-center justify-center text-slate-500 text-xs">No holdings yet.</div>
  )

  const totalValue = data.reduce((sum, d) => sum + Number(d.value || 0), 0)

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
          outerRadius={outerRadius + 6}
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
          fontSize={11}
          fontWeight={700}
        >
          {payload.name}
        </text>
        <text
          x={cx}
          y={cy + 8}
          textAnchor="middle"
          fill="#94a3b8"
          fontSize={10}
        >
          {fmt.currency(value)} ({(percent * 100).toFixed(1)}%)
        </text>
      </g>
    )
  }

  const customLabel = ({ cx, cy, midAngle, innerRadius, outerRadius, percent }) => {
    if (percent < 0.06) return null
    const RADIAN = Math.PI / 180
    const r = innerRadius + (outerRadius - innerRadius) * 0.55
    const x = cx + r * Math.cos(-midAngle * RADIAN)
    const y = cy + r * Math.sin(-midAngle * RADIAN)
    return (
      <text x={x} y={y} fill="white" textAnchor="middle" dominantBaseline="central" fontSize={10} fontWeight="700">
        {(percent * 100).toFixed(0)}%
      </text>
    )
  }

  return (
    <div className="h-full flex flex-col gap-2">
      <div className="relative flex-1 min-h-[170px] rounded-xl bg-slate-900/40 border border-slate-700/40 overflow-hidden">
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
              outerRadius="74%"
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
              formatter={(value, name, item) => {
                const pct = totalValue > 0 ? (Number(value) / totalValue) * 100 : 0
                return [`${fmt.currency(value)}  (${pct.toFixed(1)}%)`, name]
              }}
              contentStyle={{
                background: '#0f172a',
                border: '1px solid #334155',
                borderRadius: '10px',
                fontSize: '11px',
                color: '#e2e8f0'
              }}
              itemStyle={{ color: '#e2e8f0' }}
              labelStyle={{ color: '#94a3b8' }}
            />
          </PieChart>
        </ResponsiveContainer>

        <div className="absolute inset-0 pointer-events-none flex flex-col items-center justify-center">
          <p className="text-[10px] uppercase tracking-widest text-slate-500">Total</p>
          <p className="text-sm font-bold text-slate-100">{fmt.currency(totalValue)}</p>
          <p className="text-[10px] text-slate-500">{data.length} types</p>
        </div>
      </div>

      <div className="flex flex-wrap gap-1.5">
        {data.map(entry => (
          <button
            key={entry.type}
            onMouseEnter={() => setActiveIndex(data.findIndex(d => d.type === entry.type))}
            onMouseLeave={() => setActiveIndex(null)}
            className="px-2 py-1 rounded-lg bg-slate-800/60 border border-slate-700/60 text-[10px] text-slate-300 flex items-center gap-1"
          >
            <span className="w-2 h-2 rounded-full" style={{ background: PIE_COLORS[entry.type] || '#64748b' }} />
            <span>{entry.name}</span>
          </button>
        ))}
      </div>
    </div>
  )
}

export default function Dashboard() {
  const { portfolio, summary, graphData, loading, buyAsset, sellAsset, triggerPriceRefresh } = usePortfolio()
  const fmt = useFmt()
  const [selectedItem, setSelectedItem] = useState(null)
  const [refreshing,   setRefreshing]   = useState(false)
  const [filterType,   setFilterType]   = useState(null)

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
    <div className="px-4 py-4 flex flex-col gap-5">

      {/* ── Value graph card ─────────────────────────────────────────────── */}
      <div className="bg-[#1e293b] rounded-2xl p-4 border border-slate-700/50">
        <div className="flex items-start justify-between mb-1">
          <div>
            <p className="text-xs text-slate-400 uppercase tracking-widest">Total Value</p>
            <p className="text-3xl font-bold text-white mt-1">
              {summary ? fmt.currency(summary.currentValue) : '—'}
            </p>
            {summary && (
              <p className={`text-sm font-medium mt-1 ${plClass}`}>
                {fmt.currency(summary.totalProfitLoss)}{' '}
                ({fmt.percent(summary.totalProfitLossPercentage)})
              </p>
            )}
          </div>
          <button
            onClick={handleRefresh}
            disabled={refreshing}
            className={`text-slate-500 hover:text-blue-400 transition-colors mt-1 text-lg
                        ${refreshing ? 'animate-spin' : ''}`}
            title="Refresh prices"
          >
            ↻
          </button>
        </div>
        <div className="mt-3">
          <PortfolioValueGraph data={graphData} />
        </div>
      </div>

      {/* ── Summary cards ────────────────────────────────────────────────── */}
      {summary && (
        <div className="grid grid-cols-2 gap-3">
          <SummaryCard label="Invested"  value={fmt.currency(summary.totalInvestment)} />
          <SummaryCard label="Holdings"  value={`${summary.totalItems} assets`} />
        </div>
      )}

      {/* ── PIE CHART ────────────────────────────────────────────────────── */}
      <div className="bg-[#1e293b] rounded-2xl p-4 border border-slate-700/50">
        <p className="text-xs font-semibold text-slate-400 uppercase tracking-widest mb-3">
          Holdings by Asset Type
        </p>
        <div className="h-52">
          <AllocationPie items={items} />
        </div>
      </div>


      {/* ── Holdings Section with filter ─────────────────────────────────── */}
      <section>
        <div className="flex items-center justify-between mb-2">
          <h2 className="text-xs font-semibold text-slate-400 uppercase tracking-widest">
            Your Holdings
          </h2>
        </div>

        {/* Filter chips */}
        {items.length > 0 && (
          <div className="flex gap-2 overflow-x-auto pb-2 no-scrollbar mb-3">
            <button
              onClick={() => setFilterType(null)}
              className={`flex-shrink-0 px-3 py-1 rounded-lg border text-xs font-medium transition-colors
                ${!filterType ? 'border-blue-500 bg-blue-500/20 text-blue-300' : 'border-slate-700 text-slate-500 hover:border-slate-600'}`}
            >
              All ({items.length})
            </button>
            {ownedTypes.map(type => (
              <button
                key={type}
                onClick={() => setFilterType(filterType === type ? null : type)}
                className={`flex-shrink-0 px-3 py-1 rounded-lg border text-xs font-medium transition-colors
                  ${filterType === type ? 'border-blue-500 bg-blue-500/20 text-blue-300' : 'border-slate-700 text-slate-500 hover:border-slate-600'}`}
              >
                {ASSET_ICONS[type]} {ASSET_LABELS[type]} ({items.filter(i => i.assetType === type).length})
              </button>
            ))}
          </div>
        )}

        <HoldingsSection
          items={filteredItems}
          onItemClick={setSelectedItem}
        />
      </section>

      {/* ── Asset Detail Modal ────────────────────────────────────────────── */}
      {selectedItem && (
        <AssetDetailModal
          item={selectedItem}
          onClose={() => setSelectedItem(null)}
          onBuy={async (item, qty) => {
            await buyAsset({
              symbol: item.symbol, name: item.name, assetType: item.assetType,
              purchasePrice: item.currentPrice || item.purchasePrice, quantity: qty, notes: item.notes
            })
            setSelectedItem(null)
          }}
          onSell={async (item, qty) => {
            await sellAsset(item, qty)
            setSelectedItem(null)
          }}
        />
      )}
    </div>
  )
}

function SummaryCard({ label, value }) {
  return (
    <div className="bg-[#1e293b] rounded-xl p-4 border border-slate-700/50">
      <p className="text-xs text-slate-500 mb-1">{label}</p>
      <p className="text-base font-bold text-white">{value}</p>
    </div>
  )
}
