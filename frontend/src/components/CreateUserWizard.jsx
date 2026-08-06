import { useState } from 'react'
import { useUser } from '../context/UserContext'

// ── Config data ───────────────────────────────────────────────────────────────
const RISK_OPTIONS = [
  {
    value: 'CONSERVATIVE',
    icon: '▪',
    label: 'Conservative',
    desc: 'Low risk, stable returns. Suitable for preserving capital.',
    targets: 'Bonds 40% · ETFs 25% · Cash 20% · Stocks 10%',
    color: 'border-emerald-500/60 bg-emerald-500/10 text-emerald-300',
    badge: 'bg-emerald-500/20 text-emerald-300'
  },
  {
    value: 'MODERATE',
    icon: '▪',
    label: 'Moderate',
    desc: 'Balanced risk and reward. A mix of growth and stability.',
    targets: 'Stocks 30% · ETFs 30% · Bonds 20% · Cash 10%',
    color: 'border-blue-500/60 bg-blue-500/10 text-blue-300',
    badge: 'bg-blue-500/20 text-blue-300'
  },
  {
    value: 'AGGRESSIVE',
    icon: '▪',
    label: 'Aggressive',
    desc: 'Higher risk for higher growth potential over the long run.',
    targets: 'Stocks 45% · ETFs 30% · Crypto 10% · Bonds 10%',
    color: 'border-orange-500/60 bg-orange-500/10 text-orange-300',
    badge: 'bg-orange-500/20 text-orange-300'
  },
  {
    value: 'SPECULATIVE',
    icon: '▪',
    label: 'Speculative',
    desc: 'Very high risk, high reward. Suited for experienced investors.',
    targets: 'Stocks 35% · Crypto 35% · ETFs 15% · Cash 10%',
    color: 'border-rose-500/60 bg-rose-500/10 text-rose-300',
    badge: 'bg-rose-500/20 text-rose-300'
  },
]

const GOAL_OPTIONS = [
  { value: 'CAPITAL_PRESERVATION', icon: '○', label: 'Capital Preservation', desc: 'Protect your existing wealth from inflation and loss.' },
  { value: 'INCOME',               icon: '○', label: 'Income',               desc: 'Generate steady returns or dividends from investments.' },
  { value: 'GROWTH',               icon: '○', label: 'Growth',               desc: 'Build long-term wealth with appreciation over time.' },
  { value: 'SPECULATION',          icon: '○', label: 'Speculation',          desc: 'Target high-return opportunities with higher risk tolerance.' },
]

const HORIZON_OPTIONS = [
  { value: 'SHORT_TERM',  icon: '○', label: 'Short Term',  period: 'Under 1 year',  desc: 'Focus on liquidity and stability. Avoid volatile assets.' },
  { value: 'MEDIUM_TERM', icon: '○', label: 'Medium Term', period: '1 – 5 years',   desc: 'Balance growth and stability. Moderate diversification.' },
  { value: 'LONG_TERM',   icon: '○', label: 'Long Term',   period: '5+ years',      desc: 'Maximize growth. Higher risk assets are acceptable.' },
]

const STEPS = [
  { id: 1, title: 'Username',        icon: '1' },
  { id: 2, title: 'Risk Level',      icon: '2' },
  { id: 3, title: 'Investment Goal', icon: '3' },
  { id: 4, title: 'Time Horizon',    icon: '4' },
  { id: 5, title: 'Review',          icon: '5' },
]

// ── Step indicator ────────────────────────────────────────────────────────────
function StepIndicator({ current }) {
  return (
    <div className="flex items-center justify-center gap-1 mb-6">
      {STEPS.map((s, i) => (
        <div key={s.id} className="flex items-center gap-1">
          <div className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold border transition-all
            ${s.id < current  ? 'bg-blue-600 border-blue-600 text-white' : ''}
            ${s.id === current ? 'bg-blue-500/20 border-blue-500 text-blue-300' : ''}
            ${s.id > current  ? 'bg-slate-800 border-slate-700 text-slate-600' : ''}`}
          >
            {s.id < current ? '✓' : s.id}
          </div>
          {i < STEPS.length - 1 && (
            <div className={`w-5 h-0.5 transition-all ${s.id < current ? 'bg-blue-600' : 'bg-slate-700'}`} />
          )}
        </div>
      ))}
    </div>
  )
}

// ── Selectable card ───────────────────────────────────────────────────────────
function SelectCard({ active, onClick, icon, label, desc, extra, color }) {
  const base = 'w-full text-left p-4 rounded-xl border transition-all cursor-pointer'
  const activeClass = color || 'border-blue-500/60 bg-blue-500/10'
  const inactiveClass = 'border-slate-700 bg-slate-800/40 hover:border-slate-600 hover:bg-slate-800/70'

  return (
    <button type="button" onClick={onClick}
      className={`${base} ${active ? activeClass : inactiveClass}`}>
      <div className="flex items-start gap-3">
        <span className="text-xl flex-shrink-0 mt-0.5">{icon}</span>
        <div className="flex-1 min-w-0">
          <p className={`text-sm font-semibold ${active ? '' : 'text-slate-200'}`}>{label}</p>
          <p className="text-xs text-slate-400 mt-0.5">{desc}</p>
          {extra && <p className="text-[10px] text-slate-500 mt-1">{extra}</p>}
        </div>
        {active && <span className="text-blue-400 text-lg flex-shrink-0">✓</span>}
      </div>
    </button>
  )
}

// ── Main wizard ───────────────────────────────────────────────────────────────
export default function CreateUserWizard({ onClose, onCreated }) {
  const { users, createNewUser } = useUser()

  const [step,       setStep]       = useState(1)
  const [username,   setUsername]   = useState('')
  const [riskLevel,  setRiskLevel]  = useState('MODERATE')
  const [goal,       setGoal]       = useState('GROWTH')
  const [horizon,    setHorizon]    = useState('MEDIUM_TERM')
  const [busy,       setBusy]       = useState(false)
  const [error,      setError]      = useState('')

  const usernameValid = /^[a-zA-Z0-9_]{3,20}$/.test(username.trim())
  const usernameTaken = users.some(u => (u.username || '').toLowerCase() === username.trim().toLowerCase())

  const selectedRisk    = RISK_OPTIONS.find(o => o.value === riskLevel)
  const selectedGoal    = GOAL_OPTIONS.find(o => o.value === goal)
  const selectedHorizon = HORIZON_OPTIONS.find(o => o.value === horizon)

  const canNext = () => {
    if (step === 1) return usernameValid && !usernameTaken
    return true
  }

  const handleNext = () => {
    if (step < 5) setStep(s => s + 1)
  }

  const handleCreate = async () => {
    setBusy(true); setError('')
    try {
      const created = await createNewUser(username.trim(), { riskLevel, goal, horizon })
      onCreated?.(created)
      onClose()
    } catch (e) {
      setError(e.message)
      setBusy(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/70 backdrop-blur-sm"
      onClick={onClose}>
      <div
        className="bg-[#0f172a] w-full max-w-lg sm:rounded-2xl rounded-t-3xl border border-slate-700
                   max-h-[92vh] overflow-hidden flex flex-col shadow-2xl"
        onClick={e => e.stopPropagation()}
      >
        {/* Header */}
        <div className="px-6 pt-5 pb-4 border-b border-slate-800 flex items-center justify-between">
          <div>
            <p className="text-xs uppercase tracking-widest text-slate-500">New Account</p>
            <h2 className="text-lg font-bold text-white mt-0.5">
              Step {step} — {STEPS.find(s => s.id === step)?.title}
            </h2>
          </div>
          <button onClick={onClose} className="text-slate-500 hover:text-white text-xl transition-colors">✕</button>
        </div>

        {/* Step indicator */}
        <div className="px-6 pt-4">
          <StepIndicator current={step} />
        </div>

        {/* Step content */}
        <div className="flex-1 overflow-y-auto px-6 pb-4 space-y-3">

          {/* ── Step 1: Username ── */}
          {step === 1 && (
            <div className="space-y-4">
              <p className="text-sm text-slate-400">Choose a unique username for this account.</p>
              <input
                autoFocus
                value={username}
                onChange={e => { setUsername(e.target.value); setError('') }}
                onKeyDown={e => { if (e.key === 'Enter' && canNext()) handleNext() }}
                placeholder="e.g. alex_01"
                className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-3
                           text-white text-sm placeholder-slate-600 focus:outline-none
                           focus:border-blue-500 transition-colors"
              />
              <div className="space-y-1">
                <p className={`text-xs flex items-center gap-1.5 ${username.length === 0 ? 'text-slate-600' : usernameValid ? 'text-emerald-400' : 'text-rose-400'}`}>
                  {usernameValid ? '✓' : '○'} 3–20 characters, letters / numbers / underscore
                </p>
                {usernameTaken && (
                  <p className="text-xs text-rose-400">✕ Username already taken</p>
                )}
              </div>
              <button
                type="button"
                onClick={() => {
                  const suggestion = `user_${Math.floor(1000 + Math.random() * 9000)}`
                  setUsername(suggestion)
                }}
                className="text-xs text-blue-400 hover:text-blue-300 transition-colors"
              >
                Suggest a username
              </button>
            </div>
          )}

          {/* ── Step 2: Risk Level ── */}
          {step === 2 && (
            <div className="space-y-3">
              <p className="text-sm text-slate-400">
                How much risk are you comfortable with? This sets your target allocation profile.
              </p>
              {RISK_OPTIONS.map(opt => (
                <SelectCard
                  key={opt.value}
                  active={riskLevel === opt.value}
                  onClick={() => setRiskLevel(opt.value)}
                  icon={opt.icon}
                  label={opt.label}
                  desc={opt.desc}
                  extra={`Target mix: ${opt.targets}`}
                  color={opt.color}
                />
              ))}
            </div>
          )}

          {/* ── Step 3: Investment Goal ── */}
          {step === 3 && (
            <div className="space-y-3">
              <p className="text-sm text-slate-400">
                What are you trying to achieve with this portfolio?
              </p>
              {GOAL_OPTIONS.map(opt => (
                <SelectCard
                  key={opt.value}
                  active={goal === opt.value}
                  onClick={() => setGoal(opt.value)}
                  icon={opt.icon}
                  label={opt.label}
                  desc={opt.desc}
                />
              ))}
            </div>
          )}

          {/* ── Step 4: Time Horizon ── */}
          {step === 4 && (
            <div className="space-y-3">
              <p className="text-sm text-slate-400">
                How long do you plan to keep your money invested?
              </p>
              {HORIZON_OPTIONS.map(opt => (
                <SelectCard
                  key={opt.value}
                  active={horizon === opt.value}
                  onClick={() => setHorizon(opt.value)}
                  icon={opt.icon}
                  label={`${opt.label} · ${opt.period}`}
                  desc={opt.desc}
                />
              ))}
            </div>
          )}

          {/* ── Step 5: Review ── */}
          {step === 5 && (
            <div className="space-y-4">
              <p className="text-sm text-slate-400">
                Review your profile before creating the account.
              </p>

              {/* Summary card */}
              <div className="bg-slate-800/60 rounded-2xl border border-slate-700 p-5 space-y-4">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-full bg-blue-600 flex items-center justify-center text-xl font-bold text-white">
                    {username.trim()[0]?.toUpperCase()}
                  </div>
                  <div>
                    <p className="text-base font-bold text-white">{username.trim()}</p>
                    <p className="text-xs text-slate-400">New portfolio account</p>
                  </div>
                </div>

                <div className="grid grid-cols-3 gap-3">
                  <div className="bg-slate-900/60 rounded-xl p-3 text-center">
                    <p className="text-[11px] font-semibold text-slate-200 mt-1">{selectedRisk?.label}</p>
                    <p className="text-[10px] text-slate-500">Risk Level</p>
                  </div>
                  <div className="bg-slate-900/60 rounded-xl p-3 text-center">
                    <p className="text-[11px] font-semibold text-slate-200 mt-1">{selectedGoal?.label}</p>
                    <p className="text-[10px] text-slate-500">Goal</p>
                  </div>
                  <div className="bg-slate-900/60 rounded-xl p-3 text-center">
                    <p className="text-[11px] font-semibold text-slate-200 mt-1">{selectedHorizon?.label}</p>
                    <p className="text-[10px] text-slate-500">Horizon</p>
                  </div>
                </div>

                <div className="border-t border-slate-700/50 pt-3">
                  <p className="text-xs font-semibold text-slate-400 mb-1">Target Allocation</p>
                  <p className="text-xs text-slate-300">{selectedRisk?.targets}</p>
                </div>
              </div>

              {error && <p className="text-xs text-rose-400 bg-rose-500/10 border border-rose-500/30 rounded-lg px-3 py-2">{error}</p>}
            </div>
          )}
        </div>

        {/* Footer buttons */}
        <div className="px-6 py-4 border-t border-slate-800 flex gap-3">
          {step > 1 && (
            <button
              onClick={() => setStep(s => s - 1)}
              disabled={busy}
              className="px-4 py-2.5 rounded-xl border border-slate-700 text-slate-400
                         hover:text-white hover:border-slate-500 text-sm transition-colors"
            >
              ← Back
            </button>
          )}

          {step < 5 ? (
            <button
              onClick={handleNext}
              disabled={!canNext()}
              className="flex-1 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white
                         font-semibold text-sm transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
            >
              Continue →
            </button>
          ) : (
            <button
              onClick={handleCreate}
              disabled={busy}
              className="flex-1 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white
                         font-semibold text-sm transition-colors disabled:opacity-50"
            >
              {busy ? 'Creating account…' : 'Create Account'}
            </button>
          )}
        </div>
      </div>
    </div>
  )
}

