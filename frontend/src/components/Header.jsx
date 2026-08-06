import { usePortfolio } from '../context/PortfolioContext'
import { useUser } from '../context/UserContext'
import { useViewMode } from '../context/ViewModeContext'

export default function Header({ onProfileClick, onUserClick }) {
  const { portfolio } = usePortfolio()
  const { activeUser } = useUser()
  const { setWebMode } = useViewMode()

  return (
    <header className="fixed top-0 left-1/2 -translate-x-1/2 w-full max-w-md z-40
                        bg-[#0f172a]/90 backdrop-blur border-b border-slate-800
                        flex items-center justify-between px-4 h-16">
      <div>
        <p className="text-xs text-slate-400 font-medium uppercase tracking-widest">Portfolio</p>
        <h1 className="text-base font-bold text-white leading-tight">
          {portfolio?.name || 'My Portfolio'}
        </h1>
      </div>

      <div className="flex items-center gap-2">
        {/* Switch to Web button */}
        <button
          onClick={setWebMode}
          className="flex items-center gap-1 px-2.5 py-1.5 rounded-lg border border-slate-700
                     text-slate-400 hover:text-blue-300 hover:border-blue-500 transition-colors text-xs"
          title="Switch to Web Interface"
        >
          <span>🖥</span>
          <span className="hidden sm:inline">Web</span>
        </button>

        {/* Active user chip — click to switch user */}
        <button
          onClick={onUserClick}
          className="flex items-center gap-2 px-3 py-1.5 rounded-full
                     bg-slate-700/60 border border-slate-600 hover:bg-slate-700 transition-colors"
          title="Switch user"
        >
          <span className="w-5 h-5 rounded-full bg-purple-600 flex items-center justify-center
                           text-white text-xs font-bold">
            {activeUser?.username?.[0]?.toUpperCase() || 'U'}
          </span>
          <span className="text-xs text-slate-300 font-medium max-w-[72px] truncate">
            {activeUser?.username || 'Select'}
          </span>
          <span className="text-slate-500 text-xs">▾</span>
        </button>

        {/* Portfolio / profile button */}
        <button
          onClick={onProfileClick}
          className="w-9 h-9 rounded-full bg-blue-600 flex items-center justify-center
                     text-white font-bold text-sm hover:bg-blue-500 transition-colors"
          title="Profile"
        >
          {portfolio?.name?.[0]?.toUpperCase() || 'P'}
        </button>
      </div>
    </header>
  )
}
