import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import {
  addMoneyToWallet,
  createUser,
  deleteUser,
  getAllUsers,
  getWalletBalance,
  getWalletTransactions,
  removeMoneyFromWallet,
} from '../api/userApi'
import { useToast } from './ToastContext'

const UserContext = createContext(null)

export function UserProvider({ children }) {
  const { addToast } = useToast()

  const [users, setUsers]         = useState([])
  const [activeUser, setActiveUser] = useState(null)   // full User object
  const [loadingUsers, setLoadingUsers] = useState(true)
  const [walletBalance, setWalletBalance] = useState(0)
  const [walletLoading, setWalletLoading] = useState(false)
  const [walletTransactions, setWalletTransactions] = useState([])
  const [walletTransactionsLoading, setWalletTransactionsLoading] = useState(false)

  const fetchUsers = useCallback(async (preferredUserId = null) => {
    try {
      const data = await getAllUsers()
      setUsers(data)
      // Keep previously selected user when possible; otherwise use preferred id, user #1, then first user.
      const nextActive =
        data.find(u => u.userId === preferredUserId) ??
        data.find(u => u.userId === activeUser?.userId) ??
        data.find(u => u.userId === 1) ??
        data[0]

      if (nextActive) {
        setActiveUser(nextActive)
        setWalletBalance(Number(nextActive.walletBalance ?? 0))
      }
    } catch (err) {
      addToast('Could not load users: ' + err.message)
    } finally {
      setLoadingUsers(false)
    }
  }, [addToast, activeUser?.userId])

  useEffect(() => { fetchUsers() }, [fetchUsers])

  const switchUser = useCallback((user) => {
    setActiveUser(user)
  }, [])

  const refreshWalletBalance = useCallback(async (userId) => {
    const targetUserId = userId ?? activeUser?.userId
    if (!targetUserId) return

    setWalletLoading(true)
    try {
      const wallet = await getWalletBalance(targetUserId)
      const nextBalance = Number(wallet.balance ?? wallet.walletBalance ?? 0)
      setWalletBalance(nextBalance)
      setUsers(prev => prev.map(u => u.userId === targetUserId ? { ...u, walletBalance: nextBalance } : u))
      setActiveUser(prev => prev && prev.userId === targetUserId ? { ...prev, walletBalance: nextBalance } : prev)
    } catch (err) {
      addToast('Could not load wallet balance: ' + err.message)
    } finally {
      setWalletLoading(false)
    }
  }, [activeUser?.userId, addToast])

  const refreshWalletTransactions = useCallback(async (userId) => {
    const targetUserId = userId ?? activeUser?.userId
    if (!targetUserId) return

    setWalletTransactionsLoading(true)
    try {
      const tx = await getWalletTransactions(targetUserId)
      setWalletTransactions(Array.isArray(tx) ? tx : [])
    } catch (err) {
      addToast('Could not load wallet transactions: ' + err.message)
    } finally {
      setWalletTransactionsLoading(false)
    }
  }, [activeUser?.userId, addToast])

  const addWalletMoney = useCallback(async (amount) => {
    if (!activeUser?.userId) throw new Error('No active user selected')
    const res = await addMoneyToWallet(activeUser.userId, amount)
    const nextBalance = Number(res.balance ?? res.walletBalance ?? 0)
    setWalletBalance(nextBalance)
    setUsers(prev => prev.map(u => u.userId === activeUser.userId ? { ...u, walletBalance: nextBalance } : u))
    setActiveUser(prev => prev ? { ...prev, walletBalance: nextBalance } : prev)
    await refreshWalletTransactions(activeUser.userId)
    return res
  }, [activeUser, refreshWalletTransactions])

  const removeWalletMoney = useCallback(async (amount) => {
    if (!activeUser?.userId) throw new Error('No active user selected')
    const res = await removeMoneyFromWallet(activeUser.userId, amount)
    const nextBalance = Number(res.balance ?? res.walletBalance ?? 0)
    setWalletBalance(nextBalance)
    setUsers(prev => prev.map(u => u.userId === activeUser.userId ? { ...u, walletBalance: nextBalance } : u))
    setActiveUser(prev => prev ? { ...prev, walletBalance: nextBalance } : prev)
    await refreshWalletTransactions(activeUser.userId)
    return res
  }, [activeUser, refreshWalletTransactions])


  const createNewUser = useCallback(async (username, portfolioPrefs = {}) => {
    const payload = { username, walletBalance: 0 }
    const created = await createUser(payload)
    // Store portfolio preferences so PortfolioContext can apply them when auto-creating the portfolio
    if (portfolioPrefs && Object.keys(portfolioPrefs).length > 0) {
      localStorage.setItem('pm.pending-portfolio-prefs', JSON.stringify({
        userId: created.userId,
        ...portfolioPrefs
      }))
    }
    await fetchUsers(created.userId)
    addToast(`User "${created.username}" created and selected.`, 'success')
    return created
  }, [fetchUsers, addToast])

  const deleteCurrentUser = useCallback(async () => {
    if (!activeUser?.userId) throw new Error('No active user selected')
    if (users.length <= 1) throw new Error('At least one user must remain')

    const deletingId = activeUser.userId
    await deleteUser(deletingId)

    const remainingUsers = users.filter(u => u.userId !== deletingId)
    setUsers(remainingUsers)
    const nextUser = remainingUsers[0] ?? null
    setActiveUser(nextUser)
    setWalletBalance(Number(nextUser?.walletBalance ?? 0))
    setWalletTransactions([])
  }, [activeUser, users])

  useEffect(() => {
    if (!activeUser?.userId) return
    refreshWalletBalance(activeUser.userId)
    refreshWalletTransactions(activeUser.userId)
  }, [activeUser?.userId, refreshWalletBalance, refreshWalletTransactions])

  return (
    <UserContext.Provider value={{
      users,
      activeUser,
      loadingUsers,
      walletBalance,
      walletLoading,
      walletTransactions,
      walletTransactionsLoading,
      switchUser,
      fetchUsers,
      refreshWalletBalance,
      refreshWalletTransactions,
      addWalletMoney,
      removeWalletMoney,
      createNewUser,
      deleteCurrentUser
    }}>
      {children}
    </UserContext.Provider>
  )
}

export const useUser = () => useContext(UserContext)

