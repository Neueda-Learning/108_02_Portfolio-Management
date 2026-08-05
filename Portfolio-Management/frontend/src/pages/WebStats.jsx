import { useState, useEffect, useMemo } from 'react'
import { getTickersByAssetType, getCurrentPrice } from '../api/marketDataApi'
import BuySellModal from '../components/BuySellModal'
import { usePortfolio } from '../context/PortfolioContext'
import { ASSET_TYPES, ASSET_LABELS, ASSET_ICONS, ASSET_COLORS } from '../utils/helpers'
import { getAssetStats, getPriceHistory } from '../api/assetStatsApi'
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer
} from 'recharts'

function toChartRows(history) {
  if (!Array.isArray(history)) return []
  return history
    .map(item => ({
      date: item?.date ? new Date(item.date).toLocaleDateString() : '-',
      price: Number(item?.close ?? item?.price ?? 0)
    }))
    .filter(r => Number.isFinite(r.price))
}

function buildChartInsights(chartData) {
  const prices = chartData
    .map(point => Number(point?.price))
    .filter(value => Number.isFinite(value))

  if (prices.length === 0) {
    return {
      points: 0,
      average: 0,
      high: 0,
      low: 0,
      returnPct: 0,
      spreadPct: 0,
      trend: 'No data'
    }
  }

  const first = prices[0]
  const last = prices[prices.length - 1]
  const high = Math.max(...prices)
  const low = Math.min(...prices)
  const average = prices.reduce((sum, value) => sum + value, 0) / prices.length
  const returnPct = first !== 0 ? ((last - first) / first) * 100 : 0
  const spreadPct = average !== 0 ? ((high - low) / average) * 100 : 0

  let trend = 'Sideways'
  if (returnPct > 0.5) trend = 'Bullish'
  else if (returnPct < -0.5) trend = 'Bearish'

  return { points: prices.length, average, high, low, returnPct, spreadPct, trend }
}

export default function WebStats() {
  const { portfolio, buyAsset, sellAsset } = usePortfolio()
  const today = useMemo(() => new Date().toISOString().split('T')[0], [])

  const [selectedType, setSelectedType] = useState('STOCK')
  const [tickers, setTickers] = useState([])
  const [loadingTickers, setLoadingTickers] = useState(false)
  const [selectedTicker, setSelectedTicker] = useState('')
  const [livePrice, setLivePrice] = useState(null)
  const [stats, setStats] = useState(null)
  const [chartData, setChartData] = useState([])
  const [loadingStats, setLoadingStats] = useState(false)
  const [selectedPeriod, setSelectedPeriod] = useState('1M')
  const [customRange, setCustomRange] = useState(false)
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [modal, setModal] = useState(null)
  const [dateError, setDateError] = useState('')

  // Load tickers when type changes
  useEffect(() => {
    let cancelled = false
    setSelectedTicker('')
    setTickers([])
    setLoadingTickers(true)
    getTickersByAssetType(selectedType)
      .then(data => {
        if (cancelled) return
        const list = Array.isArray(data) ? data : []
        setTickers(list)
        if (list.length > 0) setSelectedTicker(list[0])
      })
      .catch(() => { if (!cancelled) setTickers([]) })
      .finally(() => { if (!cancelled) setLoadingTickers(false) })
    return () => { cancelled = true }
  }, [selectedType])

  // Load stats + live price when ticker/period changes
  useEffect(() => {
    if (!selectedTicker) { setStats(null); setChartData([]); setLivePrice(null); return }
    let cancelled = false
    setLoadingStats(true)
    Promise.all([
      getAssetStats(selectedTicker, selectedPeriod).catch(() => null),
      getCurrentPrice(selectedTicker).catch(() => null)
    ]).then(([statsData, price]) => {
      if (cancelled) return
      setStats(statsData)
      setChartData(toChartRows(statsData?.priceHistory))
      setLivePrice(price)
    }).finally(() => { if (!cancelled) setLoadingStats(false) })
    return () => { cancelled = true }
  }, [selectedTicker, selectedPeriod])

  const loadCustomRange = async () => {
    if (!startDate || !endDate || !selectedTicker) return
    if (startDate > today || endDate > today) {
      setDateError('Future dates are not allowed. Please choose today or an earlier date.');
      return
    }
    if (startDate > endDate) {
      setDateError('Start date cannot be later than the end date.');
      return
    }

    setLoadingStats(true)
    try {
      setDateError('')
      const history = await getPriceHistory(selectedTicker, startDate, endDate)
      setChartData(toChartRows(history))
    } finally {
      setLoadingStats(false)
    }
  }

  const heldItem = portfolio?.items?.find(i => i.symbol?.toUpperCase() === selectedTicker?.toUpperCase())
  const heldQty = heldItem?.quantity ?? 0
  const price = livePrice ?? heldItem?.currentPrice ?? 0

  const modalItem = {
    symbol: selectedTicker, name: selectedTicker,
    assetType: selectedType, currentPrice: price, purchasePrice: price, quantity: heldQty
  }

  const currentPrice = Number(stats?.currentPrice ?? price ?? 0)
  const change = Number(stats?.priceChange ?? 0)
  const changePct = Number(stats?.priceChangePercent ?? 0)
  const isPriceUp = change >= 0
  const chartInsights = buildChartInsights(chartData)

  return (
    <div className="flex flex-col gap-6">

      {/* ── Header ───────────────────────────────────────────────────────── */}
      <div>
        <h2 className="text-2xl font-bold text-white">Market Stats</h2>
        <p className="text-slate-400 text-sm mt-0.5">
          Select an asset type and ticker to explore charts and trade.
        </p>
      </div>

      {/* ── Top controls: type tabs + ticker dropdown + period ───────────── */}
      <div className="bg-[#1e293b] rounded-2xl border border-slate-700/50 p-5 flex flex-col gap-4">

        {/* Asset Type Tabs */}
        <div className="flex gap-2 flex-wrap">
          {ASSET_TYPES.map(type => (
            <button
              key={type}
              onClick={() => setSelectedType(type)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl border text-xs font-semibold transition-all
                ${selectedType === type
                  ? 'border-blue-500 bg-blue-500/20 text-blue-300'
                  : 'border-slate-700 text-slate-400 hover:border-slate-500 hover:text-slate-300'}`}
            >
              <span>{ASSET_ICONS[type]}</span>
              <span>{ASSET_LABELS[type]}</span>
            </button>
          ))}
        </div>

        {/* Ticker + Period controls in one row */}
        <div className="flex items-end gap-4 flex-wrap">
          <div className="flex-1 min-w-[180px]">
            <label className="block text-xs text-slate-400 mb-1">
              {ASSET_ICONS[selectedType]} {ASSET_LABELS[selectedType]} — Select Ticker
            </label>
            {loadingTickers ? (
              <div className="text-slate-500 text-sm py-2">Loading…</div>
            ) : (
              <select
                value={selectedTicker}
                onChange={e => setSelectedTicker(e.target.value)}
                className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2.5
                           text-slate-100 text-sm focus:outline-none focus:border-blue-500 transition-colors"
              >
                {tickers.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            )}
          </div>

          <div>
            <label className="block text-xs text-slate-400 mb-1">Period</label>
            <div className="flex gap-2">
              {['1W', '1M', '1Y'].map(p => (
                <button key={p} onClick={() => { setSelectedPeriod(p); setCustomRange(false) }}
                  className={`px-3 py-2 rounded-xl text-sm transition-colors
                    ${selectedPeriod === p && !customRange ? 'bg-blue-600 text-white' : 'bg-slate-700 text-slate-200 hover:bg-slate-600'}`}>
                  {p}
                </button>
              ))}
              <button onClick={() => setCustomRange(c => !c)}
                className={`px-3 py-2 rounded-xl text-sm transition-colors
                  ${customRange ? 'bg-purple-600 text-white' : 'bg-slate-700 text-slate-200 hover:bg-slate-600'}`}>
                Custom
              </button>
            </div>
          </div>

          {customRange && (
            <>
              <div className="flex items-end gap-2">
                <div>
                  <label className="block text-xs text-slate-400 mb-1">From</label>
                  <input type="date" value={startDate} max={today} onChange={e => { setStartDate(e.target.value); setDateError('') }}
                    className="bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-slate-200 text-sm" />
                </div>
                <div>
                  <label className="block text-xs text-slate-400 mb-1">To</label>
                  <input type="date" value={endDate} max={today} onChange={e => { setEndDate(e.target.value); setDateError('') }}
                    className="bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-slate-200 text-sm" />
                </div>
                <button onClick={loadCustomRange}
                  disabled={!startDate || !endDate || startDate > today || endDate > today || startDate > endDate}
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-sm transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
                  Apply
                </button>
              </div>
              {dateError && (
                <div className="mt-3 rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-rose-200 text-sm shadow-sm">
                  <div className="flex items-start gap-2">
                    <span className="text-rose-300 text-base leading-none">⚠</span>
                    <div>
                      <p className="font-semibold">Custom date range error</p>
                      <p className="text-rose-100/90 mt-0.5">{dateError}</p>
                    </div>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {/* ── Main area: Chart (2/3) + Stats Panel (1/3) ───────────────────── */}
      {selectedTicker && (
        <div className="grid grid-cols-3 gap-6">

          {/* Chart */}
          <div className="col-span-2 bg-[#1e293b] rounded-2xl border border-slate-700/50 p-6">
            <div className="flex items-start justify-between mb-4">
              <div>
                <h3 className="text-xl font-bold text-white">{selectedTicker}</h3>
                <p className="text-slate-400 text-sm">{stats?.name || selectedTicker}</p>
              </div>
              <div className="text-right">
                <p className="text-2xl font-bold text-white">${currentPrice.toFixed(2)}</p>
                <p className={`text-sm ${isPriceUp ? 'text-emerald-400' : 'text-rose-400'}`}>
                  {isPriceUp ? '+' : ''}${Math.abs(change).toFixed(2)} ({isPriceUp ? '+' : ''}{changePct.toFixed(2)}%)
                </p>
              </div>
            </div>

            {loadingStats ? (
              <div className="h-64 flex items-center justify-center text-slate-500">
                <span className="w-6 h-6 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
              </div>
            ) : chartData.length > 0 ? (
              <>
                <div className="h-64">
                  <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={chartData}>
                      <defs>
                        <linearGradient id="webStatsGrad" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.4} />
                          <stop offset="95%" stopColor="#3b82f6" stopOpacity={0.02} />
                        </linearGradient>
                      </defs>
                      <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                      <XAxis dataKey="date" tick={{ fill: '#64748b', fontSize: 11 }} />
                      <YAxis tick={{ fill: '#64748b', fontSize: 11 }} tickFormatter={v => `$${v.toFixed(0)}`} />
                      <Tooltip formatter={v => [`$${Number(v).toFixed(2)}`, 'Price']} />
                        <Area
                          type="monotone"
                          dataKey="price"
                          name="Close Price"
                          stroke="#60a5fa"
                          fill="url(#webStatsGrad)"
                          fillOpacity={1}
                        />
                    </AreaChart>
                  </ResponsiveContainer>
                </div>
                <div className="mt-4">
                  <div className="flex items-center justify-between mb-3">
                    <p className="text-xs font-semibold text-slate-400 uppercase tracking-widest">
                      Quick Insights
                    </p>
                    <p className="text-[11px] text-slate-500">
                      {customRange ? 'Custom Range' : selectedPeriod} · {chartInsights.points} points
                    </p>
                  </div>

                  <div className="grid grid-cols-2 xl:grid-cols-4 gap-3">
                    <InsightCard
                      label="Period Return"
                      value={`${chartInsights.returnPct >= 0 ? '+' : ''}${chartInsights.returnPct.toFixed(2)}%`}
                      accent={chartInsights.returnPct >= 0 ? 'text-emerald-400' : 'text-rose-400'}
                      hint={chartInsights.returnPct >= 0 ? 'Up over visible range' : 'Down over visible range'}
                    />
                    <InsightCard
                      label="Range"
                      value={`$${chartInsights.low.toFixed(2)} - $${chartInsights.high.toFixed(2)}`}
                      hint={`${chartInsights.spreadPct.toFixed(2)}% spread`}
                    />
                    <InsightCard
                      label="Average Close"
                      value={`$${chartInsights.average.toFixed(2)}`}
                      hint="Mean visible close price"
                    />
                    <InsightCard
                      label="Trend"
                      value={chartInsights.trend}
                      accent={chartInsights.trend === 'Bullish' ? 'text-emerald-400' : chartInsights.trend === 'Bearish' ? 'text-rose-400' : 'text-amber-300'}
                      hint="Based on first vs last point"
                    />
                  </div>
                </div>
              </>
            ) : (
              <div className="h-64 flex items-center justify-center text-slate-500 text-sm">
                No chart data available.
              </div>
            )}
          </div>

          {/* Right stats panel */}
          <div className="flex flex-col gap-4">

            {/* Price stats */}
            <div className="bg-[#1e293b] rounded-2xl border border-slate-700/50 p-5">
              <h4 className="text-xs font-semibold text-slate-400 uppercase tracking-widest mb-3">
                Price Stats
              </h4>
              <div className="grid grid-cols-2 gap-3">
                {[
                  { label: 'Day High',   value: stats?.dayHigh },
                  { label: 'Day Low',    value: stats?.dayLow },
                  { label: 'Week High',  value: stats?.weekHigh },
                  { label: 'Week Low',   value: stats?.weekLow },
                  { label: 'Month High', value: stats?.monthHigh },
                  { label: 'Month Low',  value: stats?.monthLow },
                ].map(({ label, value }) => (
                  <div key={label} className="bg-slate-800/60 rounded-xl p-3">
                    <p className="text-xs text-slate-500">{label}</p>
                    <p className="text-sm font-semibold text-slate-100 mt-0.5">
                      ${Number(value ?? 0).toFixed(2)}
                    </p>
                  </div>
                ))}
              </div>
            </div>

            {/* Holding info */}
            <div className="bg-[#1e293b] rounded-2xl border border-slate-700/50 p-5">
              <h4 className="text-xs font-semibold text-slate-400 uppercase tracking-widest mb-3">
                Your Position
              </h4>
              {heldItem ? (
                <div className="flex flex-col gap-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-slate-400">Quantity</span>
                    <span className="text-white font-medium">{heldItem.quantity}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Avg Price</span>
                    <span className="text-white font-medium">${Number(heldItem.purchasePrice).toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Value</span>
                    <span className="text-white font-medium">${Number(heldItem.currentValue).toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">P&L</span>
                    <span className={`font-medium ${Number(heldItem.profitLoss) >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
                      ${Number(heldItem.profitLoss).toFixed(2)}
                    </span>
                  </div>
                </div>
              ) : (
                <p className="text-slate-500 text-sm">You don't hold {selectedTicker}.</p>
              )}
            </div>

            {/* Trade buttons */}
            <div className="flex flex-col gap-2">
              <button onClick={() => setModal('buy')}
                className="w-full py-3 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm transition-colors">
                Buy {selectedTicker}
              </button>
              <button
                onClick={() => setModal('sell')}
                disabled={heldQty <= 0}
                className={`w-full py-3 rounded-xl font-bold text-sm transition-colors
                  ${heldQty > 0 ? 'bg-red-600 hover:bg-red-500 text-white' : 'bg-slate-700 text-slate-500 cursor-not-allowed'}`}>
                Sell {selectedTicker}{heldQty > 0 ? ` (${heldQty})` : ''}
              </button>
            </div>
          </div>
        </div>
      )}

      {!selectedTicker && (
        <div className="flex flex-col items-center justify-center py-24 text-slate-500 gap-3">
          <span className="text-5xl">{ASSET_ICONS[selectedType]}</span>
          <p className="text-base">Select a ticker above to view market data.</p>
        </div>
      )}

      {modal && (
        <BuySellModal
          mode={modal}
          item={modalItem}
          onClose={() => setModal(null)}
          onConfirm={async qty => {
            if (modal === 'buy') await buyAsset({ symbol: modalItem.symbol, name: modalItem.name, assetType: modalItem.assetType, purchasePrice: modalItem.currentPrice, quantity: qty, notes: '' })
            else await sellAsset(modalItem, qty)
            setModal(null)
          }}
        />
      )}
    </div>
  )
}

function InsightCard({ label, value, hint, accent = 'text-slate-100' }) {
  return (
    <div className="rounded-xl border border-slate-700 bg-slate-900/40 p-3">
      <p className="text-[11px] uppercase tracking-wide text-slate-500 mb-1">{label}</p>
      <p className={`text-sm font-bold ${accent}`}>{value}</p>
      {hint && <p className="text-[11px] text-slate-500 mt-1">{hint}</p>}
    </div>
  )
}

