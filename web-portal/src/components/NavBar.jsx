import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'

const NAVS = [
  { to: '/', label: '发现', end: true },
  { to: '/recommend/daily', label: '推荐' },
  { to: '/ranks', label: '排行榜' },
  { to: '/playlists', label: '歌单' },
  { to: '/singers', label: '歌手' },
  { to: '/radios', label: '电台' },
  { to: '/vip', label: '会员' },
]

export default function NavBar() {
  const navigate = useNavigate()
  const [kw, setKw] = useState('')
  const [menuOpen, setMenuOpen] = useState(false)
  const user = useAuthStore((s) => s.user)
  const logged = useAuthStore((s) => s.logged)
  const logout = useAuthStore((s) => s.logout)

  const doSearch = (e) => {
    e.preventDefault()
    const k = kw.trim()
    if (k) navigate(`/search?keyword=${encodeURIComponent(k)}`)
  }

  return (
    <header className="navbar">
      <div className="navbar-inner">
        <div className="logo" onClick={() => navigate('/')}>
          <span className="logo-mark">音域</span>
          <span className="logo-sub">YINYU</span>
        </div>
        <nav className="nav-links">
          {NAVS.map((n) => (
            <NavLink key={n.to} to={n.to} end={n.end} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
              {n.label}
            </NavLink>
          ))}
        </nav>
        <form className="search-box" onSubmit={doSearch}>
          <input
            value={kw}
            onChange={(e) => setKw(e.target.value)}
            placeholder="搜索歌曲 / 歌手 / 专辑"
          />
          <button type="submit" title="搜索">🔍</button>
        </form>
        <div className="nav-user">
          {logged ? (
            <div className="user-menu" onMouseEnter={() => setMenuOpen(true)} onMouseLeave={() => setMenuOpen(false)}>
              <div className="avatar-wrap">
                {user?.avatar ? (
                  <img className="avatar" src={user.avatar} alt={user?.nickname} />
                ) : (
                  <span className="avatar avatar-text">{(user?.nickname || 'U').slice(0, 1)}</span>
                )}
                <span className="nickname">{user?.nickname || '我'}</span>
                {user?.vip && <span className="tag-vip">VIP</span>}
              </div>
              {menuOpen && (
                <div className="user-dropdown card">
                  <div className="drop-item" onClick={() => navigate('/my/likes')}>我的音乐</div>
                  <div className="drop-item" onClick={() => navigate('/vip')}>会员中心</div>
                  <div className="drop-item" onClick={() => logout()}>退出登录</div>
                </div>
              )}
            </div>
          ) : (
            <button className="login-btn" onClick={() => navigate('/login')}>登录</button>
          )}
        </div>
      </div>
    </header>
  )
}
