import api from './axiosInstance'

// GET /api/portfolios → List<PortfolioDTO>
export const getAllPortfolios = () =>
  api.get('/portfolios').then(r => r.data)

// GET /api/portfolios?userId={userId} → List<PortfolioDTO>
export const getPortfoliosByUserId = (userId) =>
  api.get('/portfolios', { params: { userId } }).then(r => r.data)

// GET /api/portfolios/{id} → PortfolioDTO (with embedded items)
export const getPortfolioById = (id) =>
  api.get(`/portfolios/${id}`).then(r => r.data)

// POST /api/portfolios → PortfolioDTO
export const createPortfolio = (data) =>
  api.post('/portfolios', data).then(r => r.data)

// PUT /api/portfolios/{id} → PortfolioDTO
export const updatePortfolio = (id, data) =>
  api.put(`/portfolios/${id}`, data).then(r => r.data)

// DELETE /api/portfolios/{id}
export const deletePortfolio = (id) =>
  api.delete(`/portfolios/${id}`)

// GET /api/portfolios/{id}/summary → PortfolioSummaryDTO
// Fields: portfolioId, portfolioName, totalItems, totalInvestment,
//         currentValue, totalProfitLoss, totalProfitLossPercentage
export const getPortfolioSummary = (id) =>
  api.get(`/portfolios/${id}/summary`).then(r => r.data)

// POST /api/portfolios/{id}/refresh-prices (refreshes live prices for STOCK/ETF items)
export const refreshPortfolioPrices = (id) =>
  api.post(`/portfolios/${id}/refresh-prices`).then(r => r.data)

// GET /api/portfolios/{id}/progress → PortfolioProgressDTO
// Fields: portfolioId, portfolioName, currency, targetValue, currentValue,
//         remainingToTarget, progressPercentage, status,
//         suggestedMonthsToTarget, estimatedMonthlyContributionNeeded
export const getPortfolioProgress = (id) =>
  api.get(`/portfolios/${id}/progress`).then(r => r.data)

// GET /api/portfolios/{id}/recommendations → PortfolioRecommendationDTO
export const getPortfolioRecommendations = (id) =>
  api.get(`/portfolios/${id}/recommendations`).then(r => r.data)

