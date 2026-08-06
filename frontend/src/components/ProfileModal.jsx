import { useState, useEffect } from 'react'
import { usePortfolio } from '../context/PortfolioContext'
import { useUser } from '../context/UserContext'
import { useFmt } from '../context/PreferencesContext'
import { getPortfolioProgress } from '../api/portfolioApi'

const RISK_COLORS = {
  CONSERVATIVE: 'text-green-400',
  MODERATE:     'text-blue-400',
  AGGRESSIVE:   'text-orange-400',
  SPECULATIVE:  'text-red-400'
}

export default function ProfileModal({ onClose }) {
  const { portfolio, summary } = usePortfolio()
  const fmt = useFmt()
  const {
    activeUser,
    walletBalance,
    walletLoading,
    walletTransactions,
    walletTransactionsLoading,
    refreshWalletTransactions,
    addWalletMoney,
    removeWalletMoney,
  } = useUser()
  const [progress,     setProgress]     = useState(null)
  const [walletMode, setWalletMode]     = useState(null) // 'add' | 'remove' | null
  const [amount,       setAmount]       = useState('')
  const [amountErr,    setAmountErr]    = useState('')
  const [loading,      setLoading]      = useState(false)

  // Fetch progress toward target
  useEffect(() => {
    if (!portfolio?.id) return
    getPortfolioProgress(portfolio.id)
      .then(setProgress)
      .catch(() => {}) // endpoint may not exist yet
  }, [portfolio?.id])

  const handleWalletAction = async () => {
    const n = parseFloat(amount)
    if (!amount || isNaN(n) || n <= 0) { setAmountErr('Enter a valid amount'); return }
    setLoading(true)
    try {
      if (walletMode === 'add') {
        await addWalletMoney(n)
      } else if (walletMode === 'remove') {
        await removeWalletMoney(n)
      }
      setWalletMode(null)
      setAmount('')
    } catch (e) {
      setAmountErr(e.message)
    } finally {
      setLoading(false)
    }
  }

  const txPreview = walletTransactions.slice(0, 5)

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="bg-[#1e293b] rounded-t-3xl w-full max-w-md p-6 pb-8
                   border-t border-slate-700 max-h-[90vh] overflow-y-auto"
        onClick={e => e.stopPropagation()}
      >
        <div className="w-10 h-1 bg-slate-600 rounded-full mx-auto mb-5" />

        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-bold text-white">Profile</h2>
          <button onClick={onClose} className="text-slate-500 hover:text-white">✕</button>
        </div>

        {/* Avatar + name */}
        <div className="flex items-center gap-4 mb-6">
          <div className="w-14 h-14 rounded-full bg-blue-600 flex items-center justify-center
                          text-2xl font-bold text-white">
            {portfolio?.name?.[0]?.toUpperCase() || 'U'}
          </div>
          <div>
            <p className="text-base font-bold text-white">{portfolio?.name || '—'}</p>
            <p className="text-sm text-slate-400">{portfolio?.description || 'No description'}</p>
          </div>
        </div>

        {/* Portfolio stats */}
        <div className="grid grid-cols-2 gap-3 mb-6">
          <InfoCard label="Currency"    value={portfolio?.currency || 'USD'} />
          <InfoCard
            label="Risk Level"
            value={portfolio?.riskLevel || '—'}
            valueClass={RISK_COLORS[portfolio?.riskLevel] || 'text-slate-200'}
          />
          <InfoCard label="Goal"       value={portfolio?.investmentGoal?.replace('_', ' ') || '—'} />
          <InfoCard label="Horizon"    value={portfolio?.investmentHorizon?.replace('_', ' ') || '—'} />
          <InfoCard label="Target"     value={portfolio?.targetValue ? fmt.currency(portfolio.targetValue) : '—'} />
          <InfoCard label="Total Items" value={summary?.totalItems ?? '—'} />
        </div>

        {/* Portfolio value */}
        {summary && (
          <div className="bg-slate-700/40 rounded-2xl p-4 mb-4">
            <p className="text-xs text-slate-400 mb-3">Portfolio Value</p>
            <div className="flex justify-between mb-2">
              <span className="text-sm text-slate-400">Invested</span>
              <span className="text-sm text-slate-200 font-medium">{fmt.currency(summary.totalInvestment)}</span>
            </div>
            <div className="flex justify-between mb-2">
              <span className="text-sm text-slate-400">Current Value</span>
              <span className="text-sm font-bold text-white">{fmt.currency(summary.currentValue)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-sm text-slate-400">P&L</span>
              <span className={`text-sm font-bold ${Number(summary.totalProfitLoss) >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                {fmt.currency(summary.totalProfitLoss)} ({fmt.percent(summary.totalProfitLossPercentage)})
              </span>
            </div>
          </div>
        )}

        {/* Progress toward target */}
        {progress && portfolio?.targetValue && (
          <div className="bg-slate-700/40 rounded-2xl p-4 mb-4">
            <p className="text-xs text-slate-400 mb-2">Progress toward Target</p>
            <div className="flex justify-between text-xs text-slate-400 mb-1">
              <span>{fmt.currency(progress.currentValue)}</span>
              <span>{fmt.currency(portfolio.targetValue)}</span>
            </div>
            {/* Progress bar */}
            <div className="h-2 bg-slate-700 rounded-full overflow-hidden mb-3">
              <div
                className="h-full bg-blue-500 rounded-full transition-all"
                style={{ width: `${Math.min(100, parseFloat(progress.progressPercentage) || 0)}%` }}
              />
            </div>
            <div className="flex justify-between text-xs">
              <span className="text-blue-400 font-semibold">
                {Number(progress.progressPercentage).toFixed(1)}% complete
              </span>
              <span className="text-slate-500">{progress.status}</span>
            </div>
            {progress.suggestedMonthsToTarget && (
              <p className="text-xs text-slate-500 mt-2">
                ~{progress.suggestedMonthsToTarget} months to target ·{' '}
                {fmt.currency(progress.estimatedMonthlyContributionNeeded)}/mo needed
              </p>
            )}
          </div>
        )}

        {/* Wallet */}
        <div className="mb-2">
          <div className="bg-slate-700/40 rounded-2xl p-4 mb-3">
            <p className="text-xs text-slate-400 mb-1">Wallet Balance</p>
            <p className="text-2xl font-bold text-white">
              {walletLoading ? 'Loading…' : fmt.currency(walletBalance ?? 0)}
            </p>
            <p className="text-xs text-slate-500 mt-1">User: {activeUser?.username || '—'}</p>
          </div>

          <div className="bg-slate-700/40 rounded-2xl p-4 mb-3">
            <div className="flex items-center justify-between mb-2">
              <p className="text-xs text-slate-400">Wallet Transactions</p>
              <button
                onClick={() => refreshWalletTransactions()}
                className="text-xs text-blue-400 hover:text-blue-300"
                disabled={walletTransactionsLoading}
              >
                {walletTransactionsLoading ? 'Refreshing…' : 'Refresh'}
              </button>
            </div>

            {txPreview.length === 0 ? (
              <p className="text-xs text-slate-500">No transactions yet.</p>
            ) : (
              <div className="space-y-2">
                {txPreview.map(tx => (
                  <div key={tx.transactionId} className="flex items-center justify-between text-xs">
                    <div>
                      <p className={`${tx.transactionType === 'DEPOSIT' ? 'text-green-400' : 'text-red-400'} font-semibold`}>
                        {tx.transactionType}
                      </p>
                      <p className="text-slate-500">{new Date(tx.createdAt).toLocaleString()}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-slate-200 font-medium">{fmt.currency(tx.amount)}</p>
                      <p className="text-slate-500">Bal: {fmt.currency(tx.balanceAfter)}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {!walletMode ? (
            <div className="grid grid-cols-2 gap-2">
              <button
                onClick={() => setWalletMode('add')}
                className="w-full py-3 rounded-xl bg-green-600/20 text-green-400
                         border border-green-600/30 font-semibold hover:bg-green-600/30 transition-colors"
              >
                + Add Money
              </button>
              <button
                onClick={() => setWalletMode('remove')}
                className="w-full py-3 rounded-xl bg-red-600/20 text-red-400
                         border border-red-600/30 font-semibold hover:bg-red-600/30 transition-colors"
              >
                - Remove Money
              </button>
            </div>
          ) : (
            <div className="bg-slate-700/40 rounded-2xl p-4">
              <p className="text-sm text-slate-300 font-semibold mb-3">
                {walletMode === 'add' ? 'Add Money' : 'Remove Money'}
              </p>
              <input
                type="number"
                placeholder="Amount in USD"
                value={amount}
                min="1"
                onChange={e => { setAmount(e.target.value); setAmountErr('') }}
                className="w-full bg-slate-700/50 border border-slate-600 rounded-xl
                           px-4 py-3 text-white text-sm placeholder-slate-600
                           focus:outline-none focus:border-green-500 mb-2"
                autoFocus
              />
              {amountErr && <p className="text-red-400 text-xs mb-2">{amountErr}</p>}
              <div className="flex gap-2">
                <button
                  onClick={() => { setWalletMode(null); setAmount(''); setAmountErr('') }}
                  className="flex-1 py-2 rounded-xl border border-slate-600
                             text-slate-400 text-sm hover:bg-slate-700 transition-colors"
                >
                  Cancel
                </button>
                <button
                  onClick={handleWalletAction}
                  disabled={loading}
                  className={`flex-1 py-2 rounded-xl ${walletMode === 'add' ? 'bg-green-600 hover:bg-green-500' : 'bg-red-600 hover:bg-red-500'}
                              text-white text-sm font-semibold transition-colors
                              ${loading ? 'opacity-50 cursor-not-allowed' : ''}`}
                >
                  {loading ? 'Processing…' : (walletMode === 'add' ? 'Add' : 'Remove')}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function InfoCard({ label, value, valueClass = 'text-slate-200' }) {
  return (
    <div className="bg-slate-700/40 rounded-xl p-3">
      <p className="text-xs text-slate-500 mb-1">{label}</p>
      <p className={`text-sm font-semibold ${valueClass}`}>{value}</p>
    </div>
  )
}

