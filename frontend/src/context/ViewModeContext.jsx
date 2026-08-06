import { createContext, useContext, useState, useCallback } from 'react'

const ViewModeContext = createContext(null)

export function ViewModeProvider({ children }) {
  const [mode, setMode] = useState(() => localStorage.getItem('viewMode') || 'app')

  const setWebMode = useCallback(() => {
    localStorage.setItem('viewMode', 'web')
    setMode('web')
  }, [])

  const setAppMode = useCallback(() => {
    localStorage.setItem('viewMode', 'app')
    setMode('app')
  }, [])

  return (
    <ViewModeContext.Provider value={{ mode, setWebMode, setAppMode }}>
      {children}
    </ViewModeContext.Provider>
  )
}

export const useViewMode = () => useContext(ViewModeContext)

