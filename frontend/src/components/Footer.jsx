import { NavLink } from 'react-router-dom'

const tabs = [
  { to: '/dashboard', label: 'Dashboard',   icon: '⊞' },
  { to: '/assets',    label: 'Recommend',   icon: '◈' },
  { to: '/stats',     label: 'Stats',       icon: '◷' },
  { to: '/settings',  label: 'Settings',    icon: '⚙' }
]

export default function Footer() {
  return (
    <nav className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-md z-40
                    bg-[#1e293b] border-t border-slate-700 flex">
      {tabs.map(tab => (
        <NavLink
          key={tab.to}
          to={tab.to}
          className={({ isActive }) =>
            `flex-1 flex flex-col items-center justify-center py-3 gap-1 text-xs font-medium transition-colors
             ${isActive ? 'text-blue-400' : 'text-slate-500 hover:text-slate-300'}`
          }
        >
          <span className="text-xl leading-none">{tab.icon}</span>
          {tab.label}
        </NavLink>
      ))}
    </nav>
  )
}

