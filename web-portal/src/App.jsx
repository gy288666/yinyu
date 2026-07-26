import { useEffect } from 'react'
import { Routes, Route, useLocation, useNavigate } from 'react-router-dom'
import NavBar from './components/NavBar'
import Sidebar from './components/Sidebar'
import Footer from './components/Footer'
import PlayerBar from './components/PlayerBar'
import GlobalAudio from './components/GlobalAudio'
import ToastContainer from './components/ToastContainer'
import { useAuthStore } from './store/authStore'
import { setUnauthorizedHandler } from './api/client'

import Discover from './pages/Discover'
import Ranks from './pages/Ranks'
import PlaylistSquare from './pages/PlaylistSquare'
import PlaylistDetail from './pages/PlaylistDetail'
import Singers from './pages/Singers'
import SingerDetail from './pages/SingerDetail'
import AlbumDetail from './pages/AlbumDetail'
import Search from './pages/Search'
import Login from './pages/Login'
import Likes from './pages/Likes'
import Recent from './pages/Recent'
import MyPlaylists from './pages/MyPlaylists'
import Downloads from './pages/Downloads'
import Vip from './pages/Vip'
import DailyRecommend from './pages/DailyRecommend'
import Fm from './pages/Fm'
import Radios from './pages/Radios'
import Playing from './pages/Playing'

export default function App() {
  const location = useLocation()
  const navigate = useNavigate()
  const init = useAuthStore((s) => s.init)

  useEffect(() => {
    init()
  }, [init])

  useEffect(() => {
    // 401 时引导到登录页
    setUnauthorizedHandler(() => {
      useAuthStore.setState({ user: null, logged: false })
      navigate('/login')
    })
  }, [navigate])

  useEffect(() => {
    document.querySelector('.main-content')?.scrollTo(0, 0)
  }, [location.pathname])

  const isAuthPage = location.pathname === '/login' || location.pathname === '/register'

  return (
    <div className="app-shell">
      <GlobalAudio />
      <ToastContainer />
      {isAuthPage ? (
        <Routes>
          <Route path="/login" element={<Login mode="login" />} />
          <Route path="/register" element={<Login mode="register" />} />
        </Routes>
      ) : (
        <>
          <NavBar />
          <div className="app-body">
            <Sidebar />
            <main className="main-content">
              <Routes>
                <Route path="/" element={<Discover />} />
                <Route path="/ranks" element={<Ranks />} />
                <Route path="/ranks/:type" element={<Ranks />} />
                <Route path="/playlists" element={<PlaylistSquare />} />
                <Route path="/playlist/:id" element={<PlaylistDetail />} />
                <Route path="/singers" element={<Singers />} />
                <Route path="/singer/:id" element={<SingerDetail />} />
                <Route path="/album/:id" element={<AlbumDetail />} />
                <Route path="/search" element={<Search />} />
                <Route path="/my/likes" element={<Likes />} />
                <Route path="/my/recent" element={<Recent />} />
                <Route path="/my/playlists" element={<MyPlaylists />} />
                <Route path="/my/downloads" element={<Downloads />} />
                <Route path="/vip" element={<Vip />} />
                <Route path="/recommend/daily" element={<DailyRecommend />} />
                <Route path="/fm" element={<Fm />} />
                <Route path="/radios" element={<Radios />} />
                <Route path="/playing" element={<Playing />} />
                <Route path="*" element={<Discover />} />
              </Routes>
              <Footer />
            </main>
          </div>
        </>
      )}
      <PlayerBar />
    </div>
  )
}
