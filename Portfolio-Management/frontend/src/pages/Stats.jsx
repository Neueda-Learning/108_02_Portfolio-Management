import { useState, useEffect } from 'react'
import { getTickersByAssetType, getCurrentPrice } from '../api/marketDataApi'
import AssetStatsPage from './AssetStatsPage'
import BuySellModal from '../components/BuySellModal'
import { usePortfolio } from '../context/PortfolioContext'
import { ASSET_TYPES, ASSET_LABELS, ASSET_ICONS, ASSET_COLORS } from '../utils/helpers'

export default function Stats() {
  const { portfolio, buyAsset, sellAsset } = usePortfolio()

  const [selectedType, setSelectedType]     = useState('STOCK')
  const [tickers, setTickers]               = useState([])
  const [loadingTickers, setLoadingTickers] = useState(false)
  const [selectedTicker, setSelectedTicker] = useState('')
  const [livePrice, setLivePrice]           = useState(null)
  const [modal, setModal]                   = useState(null) // 'buy' | 'sell' | null

  // Load tickers whenever asset type changes
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

  // Fetch live price whenever selected ticker changes
  useEffect(() => {
    if (!selectedTicker) { setLivePrice(null); return }
    let cancelled = false
    getCurrentPrice(selectedTicker)
      .then(p => { if (!cancelled) setLivePrice(p) })
      .catch(() => { if (!cancelled) setLivePrice(null) })
    return () => { cancelled = true }
  }, [selectedTicker])

  // Find how many units the user already holds for the selected ticker
  const heldItem = portfolio?.items?.find(
    i => i.symbol?.toUpperCase() === selectedTicker?.toUpperCase()
  )
  const heldQty = heldItem?.quantity ?? 0

  // item shape expected by BuySellModal
  const modalItem = {
    symbol:       selectedTicker,
    name:         selectedTicker,
    assetType:    selectedType,
    currentPrice: livePrice ?? heldItem?.currentPrice ?? 0,
    purchasePrice: livePrice ?? heldItem?.currentPrice ?? 0,
    quantity:     heldQty
  }

  const handleBuyConfirm = async (qty) => {
    await buyAsset({
      symbol:        modalItem.symbol,
      name:          modalItem.name,
      assetType:     modalItem.assetType,
      purchasePrice: modalItem.currentPrice,
      quantity:      qty,
      notes:         ''
    })
    setModal(null)
  }

  const handleSellConfirm = async (qty) => {
    await sellAsset(modalItem, qty)
    setModal(null)
  }

  return (
    <div className="px-4 py-4 pb-28 flex flex-col gap-4">
      {/* ── Page title ──────────────────────────────────────────────────── */}
      <div>
        <h2 className="text-lg font-bold text-white">Market Stats</h2>
        <p className="text-xs text-slate-400 mt-0.5">
          Choose an asset type and ticker to view chart and price data.
        </p>
      </div>

      {/* ── Asset Type Tabs ──────────────────────────────────────────────── */}
      <div className="flex gap-2 overflow-x-auto pb-1 no-scrollbar">
        {ASSET_TYPES.map(type => (
          <button
            key={type}
            onClick={() => setSelectedType(type)}
            className={`flex-shrink-0 flex items-center gap-1.5 px-3 py-2 rounded-xl border text-xs font-semibold transition-all
              ${selectedType === type
                ? 'border-blue-500 bg-blue-500/20 text-blue-300'
                : 'border-slate-700 bg-slate-800/60 text-slate-400 hover:border-slate-500 hover:text-slate-300'
              }`}
          >
            <span>{ASSET_ICONS[type]}</span>
            <span>{ASSET_LABELS[type]}</span>
          </button>
        ))}
      </div>

      {/* ── Ticker Selector ──────────────────────────────────────────────── */}
      <div className={`rounded-xl border border-slate-700 p-3 ${ASSET_COLORS[selectedType]} bg-opacity-20`}>
        <label className="block text-xs font-semibold text-slate-300 mb-2">
          {ASSET_ICONS[selectedType]}&nbsp;{ASSET_LABELS[selectedType]} — Select Ticker
        </label>

        {loadingTickers ? (
          <div className="flex items-center gap-2 text-slate-400 text-sm py-1">
            <span className="w-4 h-4 border-2 border-blue-400 border-t-transparent rounded-full animate-spin inline-block" />
            Loading tickers...
          </div>
        ) : tickers.length === 0 ? (
          <p className="text-slate-500 text-sm">No tickers available for this type.</p>
        ) : (
          <select
            value={selectedTicker}
            onChange={e => setSelectedTicker(e.target.value)}
            className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2.5
                       text-slate-100 text-sm focus:outline-none focus:border-blue-500 transition-colors"
          >
            {tickers.map(ticker => (
              <option key={ticker} value={ticker}>{ticker}</option>
            ))}
          </select>
        )}
      </div>

      {/* ── Stats / Chart View ───────────────────────────────────────────── */}
      {selectedTicker
        ? <AssetStatsPage tickerOverride={selectedTicker} isEmbedded />
        : (
          <div className="flex flex-col items-center justify-center py-16 text-slate-500 gap-2">
            <span className="text-4xl">{ASSET_ICONS[selectedType]}</span>
            <p className="text-sm">Select a ticker above to view market data.</p>
          </div>
        )
      }

      {/* ── Buy / Sell Modal ─────────────────────────────────────────────── */}
      {modal && (
        <BuySellModal
          mode={modal}
          item={modalItem}
          onClose={() => setModal(null)}
          onConfirm={modal === 'buy' ? handleBuyConfirm : handleSellConfirm}
        />
      )}

      {/* ── Fixed Buy / Sell bar — sits just above the taskbar ──────────── */}
      {selectedTicker && (
        <div className="fixed bottom-16 left-1/2 -translate-x-1/2 w-full max-w-md
                        px-4 py-3 bg-[#0f172a]/95 backdrop-blur border-t border-slate-700/60 z-30
                        flex gap-3">
          <button
            onClick={() => setModal('buy')}
            className="flex-1 py-3 rounded-xl bg-blue-600 hover:bg-blue-500
                       text-white text-sm font-bold transition-colors"
          >
            Buy {selectedTicker}
          </button>
          <button
            onClick={() => setModal('sell')}
            disabled={heldQty <= 0}
            title={heldQty <= 0 ? `You don't hold any ${selectedTicker}` : `Sell ${selectedTicker}`}
            className={`flex-1 py-3 rounded-xl text-sm font-bold transition-colors
              ${heldQty > 0
                ? 'bg-red-600 hover:bg-red-500 text-white'
                : 'bg-slate-700 text-slate-500 cursor-not-allowed'
              }`}
          >
            Sell {selectedTicker}
            {heldQty > 0 && (
              <span className="ml-1 text-xs font-normal opacity-75">({heldQty})</span>
            )}
          </button>
        </div>
      )}
    </div>
  )
}

