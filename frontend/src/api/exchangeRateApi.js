// Exchange Rate API — uses open.er-api.com (free, no API key required)
// Base currency is always USD since all portfolio values are stored in USD.
const BASE_URL = 'https://open.er-api.com/v6/latest/USD'
const CACHE_KEY = 'pm.exchange-rates-cache'
const CACHE_TTL_MS = 1000 * 60 * 60 // 1 hour

/**
 * Returns { rates: { EUR: 0.91, INR: 83.5, ... }, updatedAt: ISO string }
 * Caches result in localStorage for 1 hour to avoid excessive API calls.
 */
export async function fetchExchangeRates() {
  // Read cache
  try {
    const cached = localStorage.getItem(CACHE_KEY)
    if (cached) {
      const parsed = JSON.parse(cached)
      if (parsed?.rates && Date.now() - parsed.fetchedAt < CACHE_TTL_MS) {
        return { rates: parsed.rates, updatedAt: parsed.updatedAt, fromCache: true }
      }
    }
  } catch (_) {}

  const res = await fetch(BASE_URL)
  if (!res.ok) throw new Error(`Exchange rate API error: ${res.status}`)
  const data = await res.json()

  if (data.result !== 'success') throw new Error('Exchange rate API returned failure')

  const result = {
    rates: data.rates,
    updatedAt: data.time_last_update_utc,
    fetchedAt: Date.now(),
  }

  // Store in cache
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify(result))
  } catch (_) {}

  return { rates: data.rates, updatedAt: data.time_last_update_utc, fromCache: false }
}

/**
 * Get the conversion rate from USD to the given currency.
 * Falls back to 1 (USD) if rate is unavailable.
 */
export function getRateFromCache(targetCurrency) {
  if (!targetCurrency || targetCurrency === 'USD') return 1
  try {
    const cached = localStorage.getItem(CACHE_KEY)
    if (!cached) return 1
    const parsed = JSON.parse(cached)
    return parsed?.rates?.[targetCurrency] ?? 1
  } catch {
    return 1
  }
}

