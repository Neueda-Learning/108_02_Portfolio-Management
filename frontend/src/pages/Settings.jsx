import { useMemo, useState, useEffect } from 'react'
import { usePreferences } from '../context/PreferencesContext'
import { usePortfolio } from '../context/PortfolioContext'
import { updatePortfolio } from '../api/portfolioApi'

const CURRENCIES = ['USD', 'EUR', 'INR', 'GBP', 'JPY']
const REFRESH_OPTIONS = [15, 30, 60, 120]

const RISK_OPTIONS = [
  { value: 'CONSERVATIVE', label: 'Conservative', desc: 'Low risk, stable returns' },
  { value: 'MODERATE',     label: 'Moderate',     desc: 'Balanced risk/reward' },
  { value: 'AGGRESSIVE',   label: 'Aggressive',   desc: 'Higher risk, higher growth' },
  { value: 'SPECULATIVE',  label: 'Speculative',  desc: 'Very high risk tolerance' },
]
const GOAL_OPTIONS = [
  { value: 'CAPITAL_PRESERVATION', label: 'Capital Preservation', desc: 'Protect what you have' },
  { value: 'INCOME',               label: 'Income',               desc: 'Generate regular returns' },
  { value: 'GROWTH',               label: 'Growth',               desc: 'Long-term wealth building' },
  { value: 'SPECULATION',          label: 'Speculation',          desc: 'High-risk opportunities' },
]
const HORIZON_OPTIONS = [
  { value: 'SHORT_TERM',  label: 'Short Term',  desc: 'Under 1 year' },
  { value: 'MEDIUM_TERM', label: 'Medium Term', desc: '1–5 years' },
  { value: 'LONG_TERM',   label: 'Long Term',   desc: '5+ years' },
]

function ChoiceButton({ active, children, onClick }) {
  return (
    <button
      onClick={onClick}
      className={`px-3 py-2 rounded-lg border text-sm transition-colors ${
        active
          ? 'border-blue-500 bg-blue-500/20 text-blue-300'
          : 'border-slate-700 text-slate-300 hover:border-slate-500'
      }`}
    >
      {children}
    </button>
  )
}

function Section({ title, subtitle, children }) {
  return (
    <section className="bg-[#1e293b] rounded-2xl border border-slate-700/50 p-4 sm:p-5">
      <h2 className="text-sm font-semibold text-white">{title}</h2>
      {subtitle && <p className="text-xs text-slate-400 mt-1">{subtitle}</p>}
      <div className="mt-4">{children}</div>
    </section>
  )
}

function ProfileChoiceButton({ active, label, desc, onClick }) {
  return (
    <button
      onClick={onClick}
      className={`flex items-center justify-between px-3 py-2 rounded-lg border text-xs transition-colors w-full text-left
        ${active
          ? 'border-blue-500 bg-blue-500/15 text-blue-300'
          : 'border-slate-700 text-slate-400 hover:border-slate-600 hover:text-slate-300'}`}
    >
      <span className="font-medium">{label}</span>
      <span className="text-slate-600 text-[10px] ml-2">{desc}</span>
    </button>
  )
}

export default function Settings() {
  const { preferences, setPreference, resetPreferences,
          exchangeRate, rateUpdatedAt, rateFetching, rateError, refreshExchangeRate } = usePreferences()
  const { portfolio, portfolioId } = usePortfolio()

  // Local copy of portfolio profile fields
  const [riskLevel,   setRiskLevel]   = useState('')
  const [goal,        setGoal]        = useState('')
  const [horizon,     setHorizon]     = useState('')
  const [targetValue, setTargetValue] = useState('')
  const [saving,      setSaving]      = useState(false)
  const [saveMsg,     setSaveMsg]     = useState('')

  // Sync from portfolio when it loads
  useEffect(() => {
    if (!portfolio) return
    setRiskLevel(portfolio.riskLevel || 'MODERATE')
    setGoal(portfolio.investmentGoal || 'GROWTH')
    setHorizon(portfolio.investmentHorizon || 'MEDIUM_TERM')
    setTargetValue(portfolio.targetValue != null ? String(portfolio.targetValue) : '')
  }, [portfolio])

  const handleSaveProfile = async () => {
    if (!portfolioId || !portfolio) return
    setSaving(true); setSaveMsg('')
    try {
      await updatePortfolio(portfolioId, {
        name: portfolio.name,
        description: portfolio.description,
        currency: portfolio.currency || 'USD',
        riskLevel,
        investmentGoal: goal,
        investmentHorizon: horizon,
        targetValue: targetValue ? parseFloat(targetValue) : null,
      })
      setSaveMsg('✅ Portfolio profile saved!')
    } catch (e) {
      setSaveMsg(`❌ ${e.message}`)
    } finally {
      setSaving(false)
      setTimeout(() => setSaveMsg(''), 4000)
    }
  }

  const refreshLabel = useMemo(() => {
    if (preferences.refreshIntervalSec < 60) return `${preferences.refreshIntervalSec}s`
    return `${preferences.refreshIntervalSec / 60}m`
  }, [preferences.refreshIntervalSec])

  return (
    <div className="px-4 py-4 flex flex-col gap-4 max-w-5xl">
      <div>
        <p className="text-xs uppercase tracking-widest text-slate-500">Preferences</p>
        <h1 className="text-2xl font-bold text-white mt-1">Settings</h1>
        <p className="text-sm text-slate-400 mt-1">Customize how the portfolio app looks and behaves.</p>
      </div>

      {/* ── Portfolio Profile ── */}
      <Section
        title="Portfolio Profile"
        subtitle="Controls target allocation and recommendations. Changes apply to your active portfolio immediately."
      >
        <div className="space-y-4">
          <div className="space-y-1.5">
            <p className="text-xs font-medium text-slate-400">Risk Level</p>
            <div className="flex flex-col gap-1">
              {RISK_OPTIONS.map(opt => (
                <ProfileChoiceButton key={opt.value} active={riskLevel === opt.value} label={opt.label} desc={opt.desc} onClick={() => setRiskLevel(opt.value)} />
              ))}
            </div>
          </div>

          <div className="space-y-1.5">
            <p className="text-xs font-medium text-slate-400">Investment Goal</p>
            <div className="flex flex-col gap-1">
              {GOAL_OPTIONS.map(opt => (
                <ProfileChoiceButton key={opt.value} active={goal === opt.value} label={opt.label} desc={opt.desc} onClick={() => setGoal(opt.value)} />
              ))}
            </div>
          </div>

          <div className="space-y-1.5">
            <p className="text-xs font-medium text-slate-400">Investment Horizon</p>
            <div className="flex flex-col gap-1">
              {HORIZON_OPTIONS.map(opt => (
                <ProfileChoiceButton key={opt.value} active={horizon === opt.value} label={opt.label} desc={opt.desc} onClick={() => setHorizon(opt.value)} />
              ))}
            </div>
          </div>

          <div className="space-y-1.5">
            <p className="text-xs font-medium text-slate-400">Target Portfolio Value (USD)</p>
            <input
              type="number"
              placeholder="e.g. 100000"
              value={targetValue}
              min="0"
              onChange={e => setTargetValue(e.target.value)}
              className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white
                         placeholder-slate-600 focus:outline-none focus:border-blue-500"
            />
            <p className="text-[11px] text-slate-500">Used to track goal progress and estimate monthly contribution needed.</p>
          </div>

          <button
            onClick={handleSaveProfile}
            disabled={saving || !portfolioId}
            className="w-full py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-sm font-semibold transition-colors disabled:opacity-50"
          >
            {saving ? 'Saving…' : 'Save Portfolio Profile'}
          </button>
          {saveMsg && <p className="text-xs text-center mt-1 text-slate-300">{saveMsg}</p>}
          {!portfolioId && <p className="text-xs text-slate-500">No active portfolio loaded yet.</p>}
        </div>
      </Section>

      <Section title="Appearance" subtitle="Pick your preferred theme and layout density.">
        <div className="flex flex-wrap gap-2">
          <ChoiceButton active={preferences.theme === 'dark'} onClick={() => setPreference('theme', 'dark')}>
            Dark mode
          </ChoiceButton>
          <ChoiceButton active={preferences.theme === 'light'} onClick={() => setPreference('theme', 'light')}>
            Light mode
          </ChoiceButton>
        </div>

        <label className="mt-4 flex items-center justify-between gap-3 rounded-xl border border-slate-700 px-3 py-2 text-sm">
          <span className="text-slate-300">Compact layout</span>
          <input type="checkbox" checked={preferences.compactMode}
            onChange={e => setPreference('compactMode', e.target.checked)}
            className="h-4 w-4 accent-blue-500" />
        </label>

        <label className="mt-2 flex items-center justify-between gap-3 rounded-xl border border-slate-700 px-3 py-2 text-sm">
          <span className="text-slate-300">Reduce motion</span>
          <input type="checkbox" checked={preferences.reduceMotion}
            onChange={e => setPreference('reduceMotion', e.target.checked)}
            className="h-4 w-4 accent-blue-500" />
        </label>
      </Section>

      <Section title="Market Data" subtitle="Control automatic price refresh interval.">
        <div className="flex flex-wrap gap-2">
          {REFRESH_OPTIONS.map(sec => (
            <ChoiceButton key={sec} active={preferences.refreshIntervalSec === sec}
              onClick={() => setPreference('refreshIntervalSec', sec)}>
              Every {sec < 60 ? `${sec}s` : `${sec / 60}m`}
            </ChoiceButton>
          ))}
        </div>
        <p className="text-xs text-slate-400 mt-3">Current auto-refresh: {refreshLabel}</p>
      </Section>

      <Section title="Localization" subtitle="Choose how currency values are formatted in the UI. Live exchange rates are applied automatically.">
        <div className="flex flex-wrap gap-2">
          {CURRENCIES.map(code => (
            <ChoiceButton key={code} active={preferences.currency === code}
              onClick={() => setPreference('currency', code)}>
              {code}
            </ChoiceButton>
          ))}
        </div>

        {/* Live rate info panel */}
        <div className="mt-4 rounded-xl border border-slate-700/60 bg-slate-800/40 p-3 space-y-2">
          {rateFetching && (
            <div className="flex items-center gap-2 text-xs text-slate-400">
              <span className="w-3 h-3 border border-blue-500 border-t-transparent rounded-full animate-spin" />
              Fetching live exchange rate…
            </div>
          )}

          {!rateFetching && preferences.currency !== 'USD' && (
            <div className="flex items-center justify-between gap-2">
              <div>
                <p className="text-xs text-slate-300 font-medium">
                  1 USD = <span className="text-blue-300 font-bold">{exchangeRate.toFixed(4)} {preferences.currency}</span>
                </p>
                {rateUpdatedAt && (
                  <p className="text-[11px] text-slate-500 mt-0.5">
                    Rate updated: {new Date(rateUpdatedAt).toLocaleString()}
                  </p>
                )}
              </div>
              <button
                onClick={refreshExchangeRate}
                disabled={rateFetching}
                className="text-xs text-blue-400 hover:text-blue-300 transition-colors border border-slate-700
                           px-2.5 py-1.5 rounded-lg hover:border-blue-500 disabled:opacity-50"
              >
                Refresh rate
              </button>
            </div>
          )}

          {!rateFetching && preferences.currency === 'USD' && (
            <p className="text-xs text-slate-500">USD is the base currency — no conversion applied.</p>
          )}

          {rateError && (
            <p className="text-[11px] text-amber-400">{rateError}</p>
          )}

          <p className="text-[11px] text-slate-600">
            Source: open.er-api.com · Rates cached for 1 hour · All portfolio values are stored in USD.
          </p>
        </div>
      </Section>

      <Section title="Reset" subtitle="Restore all preferences to defaults.">
        <button onClick={resetPreferences}
          className="px-4 py-2 rounded-lg border border-rose-500/40 text-rose-300 hover:bg-rose-500/15 text-sm">
          Reset to defaults
        </button>
      </Section>
    </div>
  )
}
