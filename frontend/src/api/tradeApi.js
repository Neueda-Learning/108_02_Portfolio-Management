import api from './axiosInstance'

// POST /api/users/{userId}/portfolios/{portfolioId}/trades/buy
export const buyTrade = (userId, portfolioId, data) =>
  api.post(`/users/${userId}/portfolios/${portfolioId}/trades/buy`, data).then(r => r.data)

// POST /api/users/{userId}/portfolios/{portfolioId}/trades/sell
export const sellTrade = (userId, portfolioId, data) =>
  api.post(`/users/${userId}/portfolios/${portfolioId}/trades/sell`, data).then(r => r.data)

