import { NavLink } from 'react-router-dom'
import { useState } from 'react'
import { usePortfolio } from '../context/PortfolioContext'
import { useUser } from '../context/UserContext'
import { useViewMode } from '../context/ViewModeContext'
import { usePreferences, useFmt } from '../context/PreferencesContext'
import ProfileModal from './ProfileModal'
import UserSwitcherModal from './UserSwitcherModal'

const NAV = [
  { to: '/dashboard', label: 'Dashboard',  icon: '⊞' },
  { to: '/assets',    label: 'Recommend',  icon: '◈' },
  { to: '/stats',     label: 'Stats',      icon: '◷' },
  { to: '/settings',  label: 'Settings',   icon: '⚙' },
]

export default function WebLayout({ children }) {
  const { portfolio, summary } = usePortfolio()
  const { activeUser, walletBalance } = useUser()
  const { setAppMode } = useViewMode()
  const { preferences } = usePreferences()
  const fmt = useFmt()
  const [showProfile, setShowProfile] = useState(false)
  const [showUserSwitcher, setShowUserSwitcher] = useState(false)

  return (
    <div className="min-h-screen bg-[#0a0f1a] flex text-white">

      {/* ── Sidebar ──────────────────────────────────────────────────────── */}
      <aside className="fixed top-0 left-0 h-full w-56 bg-[#0f172a] border-r border-slate-800
                        flex flex-col z-40">

        {/* Logo / Portfolio Name */}
        <div className="px-5 py-5 border-b border-slate-800">
          <p className="text-xs text-slate-500 uppercase tracking-widest">Portfolio</p>
          <h1 className="text-base font-bold text-white mt-0.5 truncate">
            {portfolio?.name || 'My Portfolio'}
          </h1>
        </div>

        {/* Nav links */}
        <nav className="flex-1 px-3 py-4 flex flex-col gap-1">
          {NAV.map(tab => (
            <NavLink
              key={tab.to}
              to={tab.to}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-colors
                 ${isActive
                   ? 'bg-blue-600/20 text-blue-300 border border-blue-600/30'
                   : 'text-slate-400 hover:text-white hover:bg-slate-800/60'}`
              }
            >
              <span className="text-lg leading-none">{tab.icon}</span>
              {tab.label}
            </NavLink>
          ))}
        </nav>

        {/* Portfolio quick-stats */}
        {summary && (
          <div className="mx-3 mb-3 p-3 bg-slate-800/60 rounded-xl border border-slate-700/50">
            <p className="text-xs text-slate-500 mb-1">Total Value</p>
            <p className="text-base font-bold text-white">{fmt.currency(summary.currentValue)}</p>
            <p className={`text-xs mt-0.5 ${Number(summary.totalProfitLoss) >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
              {Number(summary.totalProfitLoss) >= 0 ? '+' : ''}{fmt.currency(summary.totalProfitLoss)}
              {' '}({Number(summary.totalProfitLoss) >= 0 ? '+' : ''}{Number(summary.totalProfitLossPercentage).toFixed(2)}%)
            </p>
          </div>
        )}

        {/* Switch to App button */}
        <div className="px-3 pb-4">
          <button
            onClick={setAppMode}
            className="w-full flex items-center justify-center gap-2 px-3 py-2.5
                       rounded-xl border border-slate-700 text-slate-400 text-sm
                       hover:border-blue-500 hover:text-blue-300 transition-colors"
            title="Switch to Mobile App View"
          >
            <span>↩</span>
            <span>Switch to App</span>
          </button>
        </div>
      </aside>

      {/* ── Main Column ──────────────────────────────────────────────────── */}
      <div className={`ml-56 flex-1 flex flex-col min-h-screen ${preferences.compactMode ? 'pm-compact' : ''}`}>

        {/* Top bar */}
        <header className="sticky top-0 z-30 bg-[#0a0f1a]/90 backdrop-blur
                           border-b border-slate-800 px-8 h-14 flex items-center justify-between">
          <p className="text-sm text-slate-500">
            {new Date().toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
          </p>

          <div className="flex items-center gap-3">
            {/* Wallet */}
            <div className="px-3 py-1.5 bg-slate-800 rounded-lg border border-slate-700 text-xs text-slate-300">
              Wallet: {fmt.currency(walletBalance)}
            </div>

            {/* User switcher */}
            <button
              onClick={() => setShowUserSwitcher(true)}
              className="flex items-center gap-2 px-3 py-1.5 rounded-lg
                         bg-slate-800 border border-slate-700 hover:bg-slate-700 transition-colors"
            >
              <span className="w-5 h-5 rounded-full bg-purple-600 flex items-center justify-center
                               text-white text-xs font-bold">
                {activeUser?.username?.[0]?.toUpperCase() || 'U'}
              </span>
              <span className="text-xs text-slate-300 font-medium max-w-[80px] truncate">
                {activeUser?.username || 'Select User'}
              </span>
              <span className="text-slate-500 text-xs">▾</span>
            </button>

            {/* Profile */}
            <button
              onClick={() => setShowProfile(true)}
              className="w-8 h-8 rounded-full bg-blue-600 hover:bg-blue-500
                         flex items-center justify-center text-white font-bold text-sm transition-colors"
            >
              {portfolio?.name?.[0]?.toUpperCase() || 'P'}
            </button>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 px-8 py-6">
          {children}
        </main>
      </div>

      {showProfile      && <ProfileModal      onClose={() => setShowProfile(false)} />}
      {showUserSwitcher && <UserSwitcherModal onClose={() => setShowUserSwitcher(false)} />}
    </div>
  )
}

