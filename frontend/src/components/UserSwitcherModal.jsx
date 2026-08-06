import { useState } from 'react'
import { useUser } from '../context/UserContext'
import CreateUserWizard from './CreateUserWizard'

const AVATAR_COLORS = [
  'bg-blue-600', 'bg-purple-600', 'bg-green-600',
  'bg-orange-600', 'bg-pink-600', 'bg-teal-600',
]
function avatarColor(index) { return AVATAR_COLORS[index % AVATAR_COLORS.length] }

export default function UserSwitcherModal({ onClose }) {
  const { users, activeUser, switchUser, deleteCurrentUser } = useUser()
  const [selected,   setSelected]   = useState(activeUser?.userId ?? null)
  const [showWizard, setShowWizard] = useState(false)
  const [busy,       setBusy]       = useState(false)
  const [error,      setError]      = useState('')

  const handleConfirm = () => {
    const user = users.find(u => u.userId === selected)
    if (user) switchUser(user)
    onClose()
  }

  const handleDeleteUser = async () => {
    setBusy(true); setError('')
    try { await deleteCurrentUser() }
    catch (e) { setError(e.message) }
    finally { setBusy(false) }
  }

  return (
    <>
      <div className="modal-backdrop" onClick={onClose}>
        <div
          className="bg-[#1e293b] w-full max-w-md sm:max-w-lg md:max-w-xl
                     rounded-t-3xl sm:rounded-2xl border-t sm:border border-slate-700
                     px-4 sm:px-6 py-4 sm:py-6 pb-6
                     max-h-[90vh] overflow-hidden flex flex-col"
          onClick={e => e.stopPropagation()}
        >
          <div className="w-10 h-1 bg-slate-600 rounded-full mx-auto mb-4 sm:mb-5" />

          <div className="flex items-center justify-between mb-4 sm:mb-6">
            <h2 className="text-lg font-bold text-white">Switch User</h2>
            <button onClick={onClose} className="text-slate-500 hover:text-white text-xl">✕</button>
          </div>

          <p className="text-sm text-slate-400 mb-3 sm:mb-4">
            Select an account to view its portfolios and wallet.
          </p>

          <div className="flex-1 overflow-y-auto pr-1">
            <div className="flex flex-col gap-2 sm:gap-3 mb-4 sm:mb-6">
              {users.map((user, idx) => {
                const isActive = user.userId === selected
                return (
                  <button
                    key={user.userId}
                    onClick={() => setSelected(user.userId)}
                    className={`flex items-center gap-3 sm:gap-4 p-3 sm:p-4 rounded-2xl border transition-all text-left
                      ${isActive
                        ? 'bg-blue-600/20 border-blue-500/60 ring-1 ring-blue-500/40'
                        : 'bg-slate-700/30 border-slate-700 hover:bg-slate-700/60'}`}
                  >
                    <div className={`w-10 h-10 sm:w-11 sm:h-11 rounded-full ${avatarColor(idx)} flex items-center justify-center
                                     text-white font-bold text-base sm:text-lg flex-shrink-0`}>
                      {user.username?.[0]?.toUpperCase()}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-semibold text-white truncate">{user.username}</p>
                      <p className="text-xs text-slate-400">
                        Wallet: <span className="text-green-400 font-medium">
                          ${Number(user.walletBalance ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2 })}
                        </span>
                      </p>
                    </div>
                    {isActive && <span className="text-blue-400 text-lg flex-shrink-0">✓</span>}
                  </button>
                )
              })}
            </div>

            <div className="space-y-2">
              <button
                onClick={handleConfirm}
                className="w-full py-3 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-sm transition-colors"
              >
                Confirm Selection
              </button>

              <button
                onClick={() => setShowWizard(true)}
                className="w-full py-3 rounded-xl border-2 border-dashed border-slate-600
                           text-slate-400 hover:border-blue-500 hover:text-blue-300
                           text-sm font-medium transition-colors flex items-center justify-center gap-2"
              >
                <span className="text-base">＋</span>
                Create New Account
              </button>

              <button
                onClick={handleDeleteUser}
                disabled={busy || users.length <= 1}
                className="w-full py-2.5 rounded-xl bg-red-600/20 border border-red-600/30
                           text-red-400 font-semibold text-sm hover:bg-red-600/30 transition-colors disabled:opacity-50"
              >
                Delete Active User
              </button>

              {error && <p className="text-xs text-red-400">{error}</p>}
            </div>
          </div>
        </div>
      </div>

      {showWizard && (
        <CreateUserWizard
          onClose={() => setShowWizard(false)}
          onCreated={(created) => {
            setSelected(created.userId)
            setShowWizard(false)
          }}
        />
      )}
    </>
  )
}
