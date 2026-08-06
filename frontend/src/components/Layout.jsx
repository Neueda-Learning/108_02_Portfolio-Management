import { useState } from 'react'
import Header from './Header'
import Footer from './Footer'
import ProfileModal from './ProfileModal'
import UserSwitcherModal from './UserSwitcherModal'

export default function Layout({ children }) {
  const [showProfile, setShowProfile] = useState(false)
  const [showUserSwitcher, setShowUserSwitcher] = useState(false)

  return (
    <div className="min-h-screen bg-[#0f172a] flex flex-col max-w-md mx-auto relative">
      <Header
        onProfileClick={() => setShowProfile(true)}
        onUserClick={() => setShowUserSwitcher(true)}
      />
      <main className="flex-1 overflow-y-auto pb-20 pt-16">
        {children}
      </main>
      <Footer />
      {showProfile     && <ProfileModal     onClose={() => setShowProfile(false)} />}
      {showUserSwitcher && <UserSwitcherModal onClose={() => setShowUserSwitcher(false)} />}
    </div>
  )
}
