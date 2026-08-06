import api from './axiosInstance'

// GET /api/portfolios/{portfolioId}/items → List<PortfolioItemDTO>
export const getPortfolioItems = (portfolioId) =>
  api.get(`/portfolios/${portfolioId}/items`).then(r => r.data)

// GET /api/portfolios/{portfolioId}/items?assetType={type} → List<PortfolioItemDTO>
export const getItemsByAssetType = (portfolioId, assetType) =>
  api.get(`/portfolios/${portfolioId}/items`, { params: { assetType } }).then(r => r.data)

// GET /api/portfolios/{portfolioId}/items/{itemId} → PortfolioItemDTO
export const getItemById = (portfolioId, itemId) =>
  api.get(`/portfolios/${portfolioId}/items/${itemId}`).then(r => r.data)

// POST /api/portfolios/{portfolioId}/items → PortfolioItemDTO
// Body: CreatePortfolioItemRequest
//   assetType (required): STOCK | BOND | CASH | CRYPTO | MUTUAL_FUND | ETF | OTHER
//   symbol    (required): auto-uppercased by backend
//   name      (required)
//   quantity  (required, min 0.0001)
//   purchasePrice (required, min 0.01)
//   purchaseDate  (optional, ISO datetime)
//   notes     (optional)
export const addItemToPortfolio = (portfolioId, data) =>
  api.post(`/portfolios/${portfolioId}/items`, data).then(r => r.data)

// PUT /api/portfolios/{portfolioId}/items/{itemId} → PortfolioItemDTO
// Body: same as CreatePortfolioItemRequest (full replace)
export const updateItem = (portfolioId, itemId, data) =>
  api.put(`/portfolios/${portfolioId}/items/${itemId}`, data).then(r => r.data)

// DELETE /api/portfolios/{portfolioId}/items/{itemId}
export const deleteItem = (portfolioId, itemId) =>
  api.delete(`/portfolios/${portfolioId}/items/${itemId}`)

// POST /api/portfolios/{portfolioId}/items/{itemId}/refresh-price → PortfolioItemDTO
// Refreshes currentPrice from market API (STOCK/ETF only)
export const refreshItemPrice = (portfolioId, itemId) =>
  api.post(`/portfolios/${portfolioId}/items/${itemId}/refresh-price`).then(r => r.data)

