import api from './axiosInstance'

// GET /api/market-data/price/{ticker} → BigDecimal
export const getCurrentPrice = (ticker) =>
  api.get(`/market-data/price/${ticker.toUpperCase()}`).then(r => r.data)

// GET /api/market-data/stock/{ticker} → StockPriceDTO
// Fields: ticker, currentPrice, currency, timestamp, additionalData
export const getStockInfo = (ticker) =>
  api.get(`/market-data/stock/${ticker.toUpperCase()}`).then(r => r.data)

// GET /api/market-data/check/{ticker} → boolean
export const isTickerSupported = (ticker) =>
  api.get(`/market-data/check/${ticker.toUpperCase()}`).then(r => r.data)

// GET /api/market-data/stocks?assetType=STOCK → string[]
export const getTickersByAssetType = (assetType) =>
  api.get('/market-data/stocks', { params: { assetType } }).then(r => r.data)

