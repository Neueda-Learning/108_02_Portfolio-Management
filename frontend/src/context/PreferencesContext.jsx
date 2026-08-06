import { createContext, useContext, useEffect, useMemo, useState, useCallback } from 'react'
import { fetchExchangeRates, getRateFromCache } from '../api/exchangeRateApi'

const STORAGE_KEY = 'pm.preferences.v1'

const DEFAULT_PREFERENCES = {
  theme: 'dark',
  currency: 'USD',
  refreshIntervalSec: 30,
  compactMode: false,
  reduceMotion: false
}

const PreferencesContext = createContext(null)

function readStoredPreferences() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return DEFAULT_PREFERENCES
    const parsed = JSON.parse(raw)
    return { ...DEFAULT_PREFERENCES, ...parsed }
  } catch {
    return DEFAULT_PREFERENCES
  }
}

export function PreferencesProvider({ children }) {
  const [preferences, setPreferences] = useState(readStoredPreferences)
  // Live exchange rate for the selected currency (relative to USD)
  const [exchangeRate,    setExchangeRate]    = useState(() => getRateFromCache(readStoredPreferences().currency))
  const [rateUpdatedAt,   setRateUpdatedAt]   = useState(null)
  const [rateFetching,    setRateFetching]    = useState(false)
  const [rateError,       setRateError]       = useState(null)

  // Fetch live rates whenever currency changes
  const loadRates = useCallback(async (currency) => {
    if (!currency || currency === 'USD') {
      setExchangeRate(1)
      setRateError(null)
      return
    }
    setRateFetching(true)
    setRateError(null)
    try {
      const { rates, updatedAt } = await fetchExchangeRates()
      const rate = rates?.[currency] ?? 1
      setExchangeRate(rate)
      setRateUpdatedAt(updatedAt)
    } catch (e) {
      // Fallback to cached rate silently, show error
      const cached = getRateFromCache(currency)
      setExchangeRate(cached)
      setRateError('Could not fetch live rate — using cached rate.')
    } finally {
      setRateFetching(false)
    }
  }, [])

  // On mount — load rate for stored currency
  useEffect(() => {
    loadRates(preferences.currency)
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(preferences))
  }, [preferences])

  useEffect(() => {
    const isLight = preferences.theme === 'light'
    document.body.classList.toggle('theme-light', isLight)
    document.body.style.colorScheme = isLight ? 'light' : 'dark'
  }, [preferences.theme])

  useEffect(() => {
    document.body.classList.toggle('reduce-motion', Boolean(preferences.reduceMotion))
  }, [preferences.reduceMotion])

  const setPreference = useCallback((key, value) => {
    setPreferences(prev => ({ ...prev, [key]: value }))
    if (key === 'currency') {
      loadRates(value)
    }
  }, [loadRates])

  const resetPreferences = useCallback(() => {
    setPreferences(DEFAULT_PREFERENCES)
    loadRates(DEFAULT_PREFERENCES.currency)
  }, [loadRates])

  const value = useMemo(() => ({
    preferences,
    exchangeRate,
    rateUpdatedAt,
    rateFetching,
    rateError,
    setPreference,
    resetPreferences,
    refreshExchangeRate: () => loadRates(preferences.currency),
  }), [preferences, exchangeRate, rateUpdatedAt, rateFetching, rateError, setPreference, resetPreferences, loadRates])

  return (
    <PreferencesContext.Provider value={value}>
      {children}
    </PreferencesContext.Provider>
  )
}

export function usePreferences() {
  const ctx = useContext(PreferencesContext)
  if (!ctx) {
    throw new Error('usePreferences must be used inside PreferencesProvider')
  }
  return ctx
}

/**
 * Returns a reactive `fmt` object whose `currency()` re-evaluates
 * whenever the exchange rate or selected currency changes.
 * Use this instead of the static `fmt` from helpers.js in any component
 * that displays monetary values.
 */
export function useFmt() {
  const { preferences, exchangeRate } = usePreferences()
  return useMemo(() => ({
    currency: (v) => {
      const converted = Number(v) * exchangeRate
      try {
        return converted.toLocaleString('en-US', { style: 'currency', currency: preferences.currency })
      } catch {
        return converted.toLocaleString('en-US', { style: 'currency', currency: 'USD' })
      }
    },
    percent: (v) => `${Number(v) >= 0 ? '+' : ''}${Number(v).toFixed(2)}%`,
    number:  (v) => Number(v).toLocaleString('en-US', { maximumFractionDigits: 4 }),
  }), [exchangeRate, preferences.currency])
}

export function getStoredCurrencyOrDefault() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return DEFAULT_PREFERENCES.currency
    const parsed = JSON.parse(raw)
    return parsed?.currency || DEFAULT_PREFERENCES.currency
  } catch {
    return DEFAULT_PREFERENCES.currency
  }
}
