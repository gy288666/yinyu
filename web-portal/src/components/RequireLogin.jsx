import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'

export default function RequireLogin({ children, tip = '登录后查看你的音乐世界' }) {
  const navigate = useNavigate()
  const logged = useAuthStore((s) => s.logged)
  if (!logged) {
    return (
      <div className="page">
        <div className="empty-box tall">
          <div className="big-note">♫</div>
          <p>{tip}</p>
          <button className="gold-btn" onClick={() => navigate('/login')}>去登录</button>
        </div>
      </div>
    )
  }
  return children
}
