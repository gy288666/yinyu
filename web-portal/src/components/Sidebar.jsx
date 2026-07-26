import { useEffect, useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { apiMyPlaylists } from '../api'

const ITEMS = [
  { to: '/', label: '发现', icon: '◈', end: true },
  { to: '/recommend/daily', label: '为你推荐', icon: '☀' },
  { to: '/fm', label: '私人FM', icon: '📻' },
  { to: '/my/recent', label: '最近播放', icon: '🕘' },
  { to: '/my/likes', label: '我喜欢的音乐', icon: '♥' },
  { to: '/my/downloads', label: '下载管理', icon: '⬇' },
]

export default function Sidebar() {
  const navigate = useNavigate()
  const logged = useAuthStore((s) => s.logged)
  const [myLists, setMyLists] = useState([])

  useEffect(() => {
    let alive = true
    if (!logged) {
      setMyLists([])
      return
    }
    apiMyPlaylists()
      .then((list) => alive && setMyLists((list || []).filter((p) => !p.builtin)))
      .catch(() => {})
    return () => {
      alive = false
    }
  }, [logged])

  return (
    <aside className="sidebar">
      <div className="side-group">
        {ITEMS.map((it) => (
          <NavLink key={it.to} to={it.to} end={it.end} className={({ isActive }) => `side-item ${isActive ? 'active' : ''}`}>
            <span className="side-icon">{it.icon}</span>
            {it.label}
          </NavLink>
        ))}
      </div>

      <div className="side-group">
        <div className="side-title">
          创建的歌单
          <button className="text-btn" title="创建歌单" onClick={() => navigate('/my/playlists?create=1')}>＋</button>
        </div>
        {logged ? (
          myLists.length > 0 ? (
            myLists.map((p) => (
              <NavLink key={p.id} to={`/playlist/${p.id}`} className={({ isActive }) => `side-item small ${isActive ? 'active' : ''}`}>
                <span className="side-icon">♫</span>
                <span className="ellipsis">{p.title}</span>
              </NavLink>
            ))
          ) : (
            <div className="side-empty">还没有创建歌单</div>
          )
        ) : (
          <div className="side-empty link" onClick={() => navigate('/login')}>登录后查看</div>
        )}
        {logged && (
          <NavLink to="/my/playlists" className="side-item small">
            <span className="side-icon">▤</span>管理我的歌单
          </NavLink>
        )}
      </div>

      <div className="side-group">
        <div className="vip-entry" onClick={() => navigate('/vip')}>
          <div className="vip-entry-title">✦ 会员中心</div>
          <div className="vip-entry-sub">开通 VIP 畅享 Hi-Res 无损</div>
        </div>
      </div>
    </aside>
  )
}
