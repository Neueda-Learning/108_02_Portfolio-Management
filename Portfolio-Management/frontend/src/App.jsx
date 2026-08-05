import { Routes, Route, Navigate } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { ToastProvider } from './context/ToastContext'
import { UserProvider } from './context/UserContext'
import { PortfolioProvider } from './context/PortfolioContext'
import { ViewModeProvider, useViewMode } from './context/ViewModeContext'
import { PreferencesProvider, usePreferences } from './context/PreferencesContext'

// Mobile App layouts & pages
import Layout from './components/Layout'
import Dashboard from './pages/Dashboard'
import Assets from './pages/Assets'
import Stats from './pages/Stats'
import AssetStatsPage from './pages/AssetStatsPage'
import Settings from './pages/Settings'

// Web layouts & pages
import WebLayout from './components/WebLayout'
import WebDashboard from './pages/WebDashboard'
import WebAssets from './pages/WebAssets'
import WebStats from './pages/WebStats'

const WEB_MIN_WIDTH = 1100

function useViewportWidth() {
  const [width, setWidth] = useState(() => window.innerWidth)

  useEffect(() => {
    const applyViewportHeight = () => {
      // Keeps app layout stable when browser/tab window is resized or minimized.
      const vh = window.innerHeight * 0.01
      document.documentElement.style.setProperty('--app-vh', `${vh}px`)
      setWidth(window.innerWidth)
    }

    applyViewportHeight()
    window.addEventListener('resize', applyViewportHeight)
    window.addEventListener('orientationchange', applyViewportHeight)
    document.addEventListener('visibilitychange', applyViewportHeight)

    return () => {
      window.removeEventListener('resize', applyViewportHeight)
      window.removeEventListener('orientationchange', applyViewportHeight)
      document.removeEventListener('visibilitychange', applyViewportHeight)
    }
  }, [])

  return width
}

function AppRoutes() {
  const { mode } = useViewMode()
  const { preferences } = usePreferences()
  const width = useViewportWidth()
  const forceAppForNarrowViewport = mode === 'web' && width < WEB_MIN_WIDTH
  const effectiveMode = forceAppForNarrowViewport ? 'app' : mode

  if (effectiveMode === 'web') {
    return (
      <div className={preferences.compactMode ? 'pm-compact' : ''} style={{ minHeight: 'calc(var(--app-vh, 1vh) * 100)' }}>
        <WebLayout>
          <Routes>
            <Route path="/"          element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<WebDashboard />} />
            <Route path="/assets"    element={<WebAssets />} />
            <Route path="/stats"     element={<WebStats />} />
            <Route path="/settings"  element={<Settings />} />
            <Route path="/asset/:ticker" element={<AssetStatsPage />} />
          </Routes>
        </WebLayout>
      </div>
    )
  }

  return (
    <div className={preferences.compactMode ? 'pm-compact' : ''} style={{ minHeight: 'calc(var(--app-vh, 1vh) * 100)' }}>
      <Layout>
        <Routes>
          <Route path="/"          element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/assets"    element={<Assets />} />
          <Route path="/stats"     element={<Stats />} />
          <Route path="/settings"  element={<Settings />} />
          <Route path="/asset/:ticker" element={<AssetStatsPage />} />
        </Routes>
      </Layout>
    </div>
  )
}

export default function App() {
  return (
    <ToastProvider>
      <UserProvider>
        <PreferencesProvider>
          <PortfolioProvider>
            <ViewModeProvider>
              <AppRoutes />
            </ViewModeProvider>
          </PortfolioProvider>
        </PreferencesProvider>
      </UserProvider>
    </ToastProvider>
  )
}
