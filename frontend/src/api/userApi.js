import api from './axiosInstance'

// GET /api/users → List<User>
export const getAllUsers = () =>
  api.get('/users').then(r => r.data)

// GET /api/users/{userId} → User
export const getUserById = (userId) =>
  api.get(`/users/${userId}`).then(r => r.data)

// GET /api/users/username/{username} → User
export const getUserByUsername = (username) =>
  api.get(`/users/username/${username}`).then(r => r.data)

// GET /api/users/{userId}/wallet → WalletBalanceDTO
export const getWalletBalance = (userId) =>
  api.get(`/users/${userId}/wallet`).then(r => r.data)

// POST /api/users/{userId}/wallet/add → WalletBalanceDTO
// Request body: { amount: number }
export const addMoneyToWallet = (userId, amount) =>
  api.post(`/users/${userId}/wallet/add`, { amount }).then(r => r.data)

// POST /api/users/{userId}/wallet/remove → WalletBalanceDTO
// Request body: { amount: number }
export const removeMoneyFromWallet = (userId, amount) =>
  api.post(`/users/${userId}/wallet/remove`, { amount }).then(r => r.data)

// GET /api/users/{userId}/wallet/transactions → List<WalletTransactionDTO>
export const getWalletTransactions = (userId) =>
  api.get(`/users/${userId}/wallet/transactions`).then(r => r.data)

// POST /api/users → User
export const createUser = (userData) =>
  api.post('/users', userData).then(r => r.data)

// DELETE /api/users/{userId}
export const deleteUser = (userId) =>
  api.delete(`/users/${userId}`)

