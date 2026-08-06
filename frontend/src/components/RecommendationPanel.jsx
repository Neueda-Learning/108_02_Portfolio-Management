import { useEffect, useState, useCallback } from 'react'
import { getPortfolioRecommendations } from '../api/portfolioApi'
import { useFmt } from '../context/PreferencesContext'

// ── Priority config ──────────────────────────────────────────────────────────
const PRIORITY_CONFIG = {
  HIGH:   { color: 'border-rose-500/50 bg-rose-500/10',   badge: 'bg-rose-500/20 text-rose-300',   dot: 'bg-rose-400',    label: 'High'   },
  MEDIUM: { color: 'border-amber-500/50 bg-amber-500/10', badge: 'bg-amber-500/20 text-amber-300', dot: 'bg-amber-400',   label: 'Medium' },
  LOW:    { color: 'border-emerald-500/50 bg-emerald-500/10', badge: 'bg-emerald-500/20 text-emerald-300', dot: 'bg-emerald-400', label: 'Low' },
}

// ── Category labels (text only) ──────────────────────────────────────────────
const CATEGORY_LABEL = {
  REBALANCE:       'Rebalance',
  RISK:            'Risk',
  TARGET:          'Target',
  DIVERSIFICATION: 'Diversify',
  GOAL_ALIGNMENT:  'Goal',
  HORIZON:         'Horizon',
  GET_STARTED:     'Get Started',
  MAINTENANCE:     'On Track',
}

// ── Allocation Drift Bar ──────────────────────────────────────────────────────
function DriftBar({ assetType, current, target, drift }) {
  const isOver  = drift > 0
  const isUnder = drift < 0
  const absDrift = Math.abs(drift)
  const alerting = absDrift >= 10

  return (
    <div className="space-y-1.5">
      <div className="flex items-center justify-between text-xs">
        <span className="text-slate-300 font-medium">{assetType}</span>
        <span className={`text-[11px] font-semibold ${alerting ? (isOver ? 'text-rose-400' : 'text-amber-400') : 'text-slate-400'}`}>
          {current.toFixed(1)}% <span className="text-slate-500">/ {target.toFixed(1)}% target</span>
        </span>
      </div>
      {/* Target bar */}
      <div className="relative h-2 bg-slate-800 rounded-full overflow-hidden">
        {/* Target marker (dashed) */}
        <div
          className="absolute top-0 bottom-0 w-0.5 bg-slate-500 z-10"
          style={{ left: `${Math.min(target, 100)}%` }}
        />
        {/* Current fill */}
        <div
          className={`h-full rounded-full transition-all ${
            alerting
              ? isOver  ? 'bg-rose-400'   : 'bg-amber-400'
              : 'bg-blue-400'
          }`}
          style={{ width: `${Math.min(current, 100)}%` }}
        />
      </div>
      {alerting && (
        <p className={`text-[10px] ${isOver ? 'text-rose-400' : 'text-amber-400'}`}>
          {isOver ? `▲ ${absDrift.toFixed(1)}% over target` : `▼ ${absDrift.toFixed(1)}% under target`}
        </p>
      )}
    </div>
  )
}

// ── Progress Bar ──────────────────────────────────────────────────────────────
function ProgressBar({ progress }) {
  const fmt = useFmt()
  if (!progress) return null
  const pct = Math.min(Number(progress.progressPercentage || 0), 100)
  const reached = pct >= 100

  return (
    <div className="bg-[#0f172a]/60 rounded-xl p-4 border border-slate-700/50 space-y-2">
      <div className="flex items-center justify-between">
        <p className="text-xs font-semibold text-slate-300">Goal Progress</p>
        <span className={`text-xs font-bold px-2 py-0.5 rounded-full ${reached ? 'bg-emerald-500/20 text-emerald-300' : 'bg-blue-500/20 text-blue-300'}`}>
          {pct.toFixed(1)}%
        </span>
      </div>

      <div className="h-2.5 bg-slate-800 rounded-full overflow-hidden">
        <div
          className={`h-full rounded-full transition-all duration-700 ${reached ? 'bg-emerald-400' : 'bg-blue-400'}`}
          style={{ width: `${pct}%` }}
        />
      </div>

      <div className="flex justify-between text-[11px] text-slate-500">
        <span>{fmt.currency(progress.currentValue)}</span>
        <span>Target: {fmt.currency(progress.targetValue)}</span>
      </div>

      {!reached && Number(progress.remainingToTarget) > 0 && (
        <p className="text-[11px] text-slate-400">
          {fmt.currency(progress.remainingToTarget)} remaining
          {Number(progress.estimatedMonthlyContributionNeeded) > 0 && (
            <span className="text-blue-400 ml-1">
              · ~{fmt.currency(progress.estimatedMonthlyContributionNeeded)}/mo
            </span>
          )}
        </p>
      )}

      {progress.status && (
        <p className="text-[11px] text-slate-500 italic">{progress.status}</p>
      )}
    </div>
  )
}

// ── Single Recommendation Card ────────────────────────────────────────────────
function RecommendationCard({ rec }) {
  const [expanded, setExpanded] = useState(false)
  const cfg = PRIORITY_CONFIG[rec.priority] || PRIORITY_CONFIG.LOW

  return (
    <div className={`rounded-xl border p-3 cursor-pointer transition-all ${cfg.color}`}
      onClick={() => setExpanded(e => !e)}
    >
      <div className="flex items-start gap-2">
        <span className={`text-[10px] px-1.5 py-0.5 rounded font-semibold flex-shrink-0 mt-0.5 ${cfg.badge}`}>
          {CATEGORY_LABEL[rec.category] || rec.category}
        </span>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <p className="text-sm font-semibold text-white truncate">{rec.title}</p>
            <span className={`text-[10px] px-1.5 py-0.5 rounded-full font-medium ${cfg.badge}`}>
              {cfg.label}
            </span>
          </div>
          {expanded && (
            <p className="text-xs text-slate-300 mt-1.5 leading-relaxed">{rec.message}</p>
          )}
          {!expanded && (
            <p className="text-[11px] text-slate-500 mt-0.5 truncate">{rec.message}</p>
          )}
        </div>
        <span className="text-slate-500 text-xs flex-shrink-0">{expanded ? '▲' : '▼'}</span>
      </div>
    </div>
  )
}

// ── Main Panel ────────────────────────────────────────────────────────────────
export default function RecommendationPanel({ portfolioId }) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [showDrift, setShowDrift] = useState(false)
  const [collapsed, setCollapsed] = useState(false)

  const load = useCallback(async () => {
    if (!portfolioId) return
    setLoading(true)
    setError(null)
    try {
      const result = await getPortfolioRecommendations(portfolioId)
      setData(result)
    } catch (e) {
      setError('Unable to load recommendations.')
    } finally {
      setLoading(false)
    }
  }, [portfolioId])

  useEffect(() => { load() }, [load])

  if (!portfolioId) return null

  // ── Loading ──
  if (loading) return (
    <div className="bg-[#1e293b] rounded-2xl border border-slate-700/50 p-4 flex items-center gap-3">
      <div className="w-5 h-5 border-2 border-blue-500 border-t-transparent rounded-full animate-spin flex-shrink-0" />
      <p className="text-sm text-slate-400">Analysing portfolio…</p>
    </div>
  )

  // ── Error ──
  if (error) return (
    <div className="bg-[#1e293b] rounded-2xl border border-rose-700/40 p-4">
      <p className="text-sm text-rose-400">{error}</p>
      <button onClick={load} className="text-xs text-blue-400 mt-1 hover:underline">Retry</button>
    </div>
  )

  if (!data) return null

  const recs = data.recommendations || []
  const drifts = data.allocationDrifts || []
  const highCount = recs.filter(r => r.priority === 'HIGH').length
  const medCount  = recs.filter(r => r.priority === 'MEDIUM').length

  return (
    <div className="bg-[#1e293b] rounded-2xl border border-slate-700/50 overflow-hidden">

      {/* ── Header ── */}
      <div
        className="flex items-center justify-between px-4 py-3 border-b border-slate-700/50 cursor-pointer hover:bg-slate-700/20 transition-colors"
        onClick={() => setCollapsed(c => !c)}
      >
        <div className="flex items-center gap-2">
          <div>
            <p className="text-sm font-semibold text-white">Portfolio Recommendations</p>
            <p className="text-[11px] text-slate-400">
              {recs.length} insight{recs.length !== 1 ? 's' : ''}
              {highCount > 0 && <span className="text-rose-400 ml-1">· {highCount} high priority</span>}
              {medCount > 0 && <span className="text-amber-400 ml-1">· {medCount} medium</span>}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={e => { e.stopPropagation(); load() }}
            className="text-slate-500 hover:text-blue-400 text-sm transition-colors"
            title="Refresh recommendations"
          >↻</button>
          <span className="text-slate-500 text-xs">{collapsed ? '▼' : '▲'}</span>
        </div>
      </div>

      {/* ── Body (collapsible) ── */}
      {!collapsed && (
        <div className="p-4 space-y-4">

          {/* Goal Progress */}
          <ProgressBar progress={data.progress} />

          {/* Recommendations */}
          {recs.length > 0 && (
            <div className="space-y-2">
              <p className="text-xs font-semibold text-slate-400 uppercase tracking-widest">
                Insights
              </p>
              {recs.map((rec, i) => (
                <RecommendationCard key={i} rec={rec} />
              ))}
            </div>
          )}

          {/* Allocation Drift toggle */}
          {drifts.length > 0 && (
            <div>
              <button
                onClick={() => setShowDrift(s => !s)}
                className="flex items-center gap-2 text-xs text-slate-400 hover:text-slate-200 transition-colors"
              >
                <span className="font-medium">Allocation vs Target</span>
                <span>{showDrift ? '▲' : '▼'}</span>
              </button>

              {showDrift && (
                <div className="mt-3 space-y-3 pl-1">
                  {drifts.map((d, i) => (
                    <DriftBar
                      key={i}
                      assetType={d.assetType}
                      current={Number(d.currentPercentage || 0)}
                      target={Number(d.targetPercentage || 0)}
                      drift={Number(d.driftPercentage || 0)}
                    />
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Disclaimer */}
          {data.disclaimer && (
            <p className="text-[10px] text-slate-600 leading-relaxed border-t border-slate-700/50 pt-3 mt-2">
              Note: {data.disclaimer}
            </p>
          )}
        </div>
      )}
    </div>
  )
}

