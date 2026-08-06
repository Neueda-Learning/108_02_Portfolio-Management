import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { getPortfolioById, getPortfolioSummary, refreshPortfolioPrices, getPortfoliosByUserId, createPortfolio } from '../api/portfolioApi'
import { buyTrade, sellTrade } from '../api/tradeApi'
import { useToast } from './ToastContext'
import { useUser } from './UserContext'
import { usePreferences } from './PreferencesContext'

const PortfolioContext = createContext(null)

export function PortfolioProvider({ children }) {
  const { addToast } = useToast()
  const { activeUser, refreshWalletBalance } = useUser()
  const { preferences } = usePreferences()

  const [portfolioId, setPortfolioId] = useState(null)
  const [portfolio, setPortfolio] = useState(null)   // PortfolioDTO
  const [summary, setSummary]     = useState(null)   // PortfolioSummaryDTO
  const [loading, setLoading]     = useState(true)
  const [graphData, setGraphData] = useState([])     // [{time, value}] for line graph

  // ─── Fetch portfolio details + items ──────────────────────────────────────
  const fetchPortfolio = useCallback(async (id) => {
    const targetId = id ?? portfolioId
    if (!targetId) return

    try {
      const data = await getPortfolioById(targetId)
      setPortfolio(data)
    } catch (err) {
      setPortfolio(null)
      addToast(err.message)
    }
  }, [portfolioId, addToast])

  // ─── Fetch summary (totals + profit/loss) ─────────────────────────────────
  const fetchSummary = useCallback(async (id) => {
    const targetId = id ?? portfolioId
    if (!targetId) return

    try {
      const data = await getPortfolioSummary(targetId)
      setSummary(data)
      // Append a new point to the graph (max 30 points)
      setGraphData(prev => [
        ...prev,
        { time: new Date().toLocaleTimeString(), value: parseFloat(data.currentValue) || 0 }
      ].slice(-30))
    } catch (err) {
      // Silently fail for polling — don't spam toasts
      console.warn('Summary fetch failed:', err.message)
    }
  }, [portfolioId])

  // ─── Refresh live prices then re-fetch summary ─────────────────────────────
  const triggerPriceRefresh = useCallback(async () => {
    if (!portfolioId) return

    try {
      await refreshPortfolioPrices(portfolioId)
      await fetchSummary()
      await fetchPortfolio()
    } catch (err) {
      console.warn('Price refresh failed:', err.message)
    }
  }, [portfolioId, fetchSummary, fetchPortfolio])

  // ─── When active user changes, load their first portfolio ─────────────────
  useEffect(() => {
    if (!activeUser?.userId) return

    let cancelled = false

    const loadUserPortfolio = async () => {
      setLoading(true)
      setPortfolioId(null)
      setPortfolio(null)
      setSummary(null)
      setGraphData([])

      try {
        let all = await getPortfoliosByUserId(activeUser.userId)
        if (cancelled) return

        let target = all?.[0]

        if (!target) {
          // Read pending portfolio prefs set during user creation
          let pendingPrefs = {}
          try {
            const raw = localStorage.getItem('pm.pending-portfolio-prefs')
            if (raw) {
              const parsed = JSON.parse(raw)
              if (parsed.userId === activeUser.userId) {
                pendingPrefs = {
                  riskLevel: parsed.riskLevel,
                  investmentGoal: parsed.goal,
                  investmentHorizon: parsed.horizon,
                }
                localStorage.removeItem('pm.pending-portfolio-prefs')
              }
            }
          } catch (_) {}

          // Auto-provision a starter portfolio so newly created users can trade immediately.
          const baseName = `${activeUser.username || 'User'} Portfolio U${activeUser.userId}`
          const fallbackName = `${baseName} ${Date.now()}`
          try {
            target = await createPortfolio({
              userId: activeUser.userId,
              name: baseName,
              description: 'Auto-created starter portfolio',
              currency: 'USD',
              ...pendingPrefs
            })
            addToast('Created a starter portfolio for this user.', 'success')
          } catch (_) {
            target = await createPortfolio({
              userId: activeUser.userId,
              name: fallbackName,
              description: 'Auto-created starter portfolio',
              currency: 'USD',
              ...pendingPrefs
            })
            addToast('Created a starter portfolio for this user.', 'success')
          }

          all = await getPortfoliosByUserId(activeUser.userId)
          if (cancelled) return
          target = target ?? all?.[0]

          if (!target) {
            setPortfolioId(null)
            addToast('Unable to initialize a portfolio for this user.')
            setLoading(false)
            return
          }
        }

        setPortfolioId(target.id)

        // Fetch full portfolio details + summary in parallel
        const [fullPortfolio, portfolioSummary] = await Promise.allSettled([
          getPortfolioById(target.id),
          getPortfolioSummary(target.id)
        ])

        if (cancelled) return

        if (fullPortfolio.status === 'fulfilled') {
          setPortfolio(fullPortfolio.value)
        }
        if (portfolioSummary.status === 'fulfilled') {
          setSummary(portfolioSummary.value)
          setGraphData([{
            time: new Date().toLocaleTimeString(),
            value: parseFloat(portfolioSummary.value.currentValue) || 0
          }])
        }
      } catch (err) {
        if (!cancelled) addToast('Failed to load portfolios: ' + err.message)
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    loadUserPortfolio()
    return () => { cancelled = true }
  }, [activeUser?.userId]) // eslint-disable-line react-hooks/exhaustive-deps

  // ─── Reload when portfolioId changes (e.g. manual switch) ─────────────────
  useEffect(() => {
    if (!portfolioId) return
    setLoading(true)
    Promise.all([fetchPortfolio(), fetchSummary()])
      .finally(() => setLoading(false))
  }, [portfolioId]) // eslint-disable-line react-hooks/exhaustive-deps

  // ─── Poll based on user-selected refresh interval ─────────────────────────
  useEffect(() => {
    const seconds = Number(preferences.refreshIntervalSec) || 30
    const safeSeconds = Math.min(Math.max(seconds, 10), 600)
    const interval = setInterval(triggerPriceRefresh, safeSeconds * 1000)
    return () => clearInterval(interval)
  }, [triggerPriceRefresh, preferences.refreshIntervalSec])

  // ─── BUY ──────────────────────────────────────────────────────────────────
  const buyAsset = useCallback(async ({ symbol, name, assetType, purchasePrice, quantity, notes }) => {
    if (!activeUser?.userId) {
      throw new Error('No active user selected')
    }
    if (!portfolioId) {
      throw new Error('No portfolio selected for this user yet. Please wait a moment and try again.')
    }

    try {
      await buyTrade(activeUser.userId, portfolioId, {
        assetType,
        symbol: symbol.toUpperCase(),
        name,
        quantity: parseFloat(quantity),
        price: parseFloat(purchasePrice),
        notes: notes || ''
      })

      await fetchPortfolio()
      await fetchSummary()
      await refreshWalletBalance(activeUser.userId)
      addToast('Purchase successful!', 'success')
    } catch (err) {
      addToast(err.message)
      throw err
    }
  }, [activeUser, portfolioId, fetchPortfolio, fetchSummary, refreshWalletBalance, addToast])

  // ─── SELL ─────────────────────────────────────────────────────────────────
  const sellAsset = useCallback(async (item, sellQuantity) => {
    if (!activeUser?.userId) {
      throw new Error('No active user selected')
    }
    if (!portfolioId) {
      throw new Error('No portfolio selected for this user yet. Please wait a moment and try again.')
    }

    try {
      const fallbackPrice = Number(item.currentPrice ?? item.purchasePrice)
      await sellTrade(activeUser.userId, portfolioId, {
        symbol: item.symbol,
        quantity: parseFloat(sellQuantity),
        pricePerUnit: Number.isFinite(fallbackPrice) && fallbackPrice > 0 ? fallbackPrice : undefined
      })

      await fetchPortfolio()
      await fetchSummary()
      await refreshWalletBalance(activeUser.userId)
      addToast('Sale successful!', 'success')
    } catch (err) {
      addToast(err.message)
      throw err
    }
  }, [activeUser, portfolioId, fetchPortfolio, fetchSummary, refreshWalletBalance, addToast])

  // ─── Add Money (CASH item workaround) ─────────────────────────────────────
  const addMoney = useCallback(async (amount) => {
    await buyAsset({
      symbol: 'CASH',
      name: 'Cash Deposit',
      assetType: 'CASH',
      purchasePrice: 1.00,
      quantity: parseFloat(amount),
      notes: `Deposit on ${new Date().toLocaleDateString()}`
    })
  }, [buyAsset])

  return (
    <PortfolioContext.Provider value={{
      portfolioId,
      portfolio,
      summary,
      loading,
      graphData,
      fetchPortfolio,
      fetchSummary,
      triggerPriceRefresh,
      buyAsset,
      sellAsset,
      addMoney
    }}>
      {children}
    </PortfolioContext.Provider>
  )
}

export const usePortfolio = () => useContext(PortfolioContext)

