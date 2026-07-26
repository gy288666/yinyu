import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { toast } from '../components/toast'

export default function Login({ mode = 'login' }) {
  const navigate = useNavigate()
  const [form, setForm] = useState({ username: '', password: '', nickname: '' })
  const [submitting, setSubmitting] = useState(false)
  const login = useAuthStore((s) => s.login)
  const register = useAuthStore((s) => s.register)
  const isRegister = mode === 'register'

  const setField = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    if (!form.username.trim() || !form.password) return toast.warn('请输入用户名和密码')
    if (isRegister && !form.nickname.trim()) return toast.warn('请输入昵称')
    setSubmitting(true)
    try {
      if (isRegister) {
        await register({ username: form.username.trim(), password: form.password, nickname: form.nickname.trim(), channel: 'direct' })
        toast.success('注册成功，欢迎来到音域')
      } else {
        await login(form.username.trim(), form.password)
        toast.success('登录成功')
      }
      navigate('/')
    } catch (err) {
      /* 拦截器已 toast */
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card card">
        <div className="auth-logo" onClick={() => navigate('/')}>
          <span className="logo-mark">音域</span>
          <span className="logo-sub">YINYU</span>
        </div>
        <h1 className="auth-title">{isRegister ? '创建账号' : '欢迎回来'}</h1>
        <p className="auth-sub">{isRegister ? '注册后畅享完整播放、歌单与私人FM' : '登录音域，继续你的音乐旅程'}</p>
        <form onSubmit={submit} className="auth-form">
          <label className="field">
            <span>用户名</span>
            <input value={form.username} onChange={setField('username')} placeholder="4-20 位字母或数字" autoComplete="username" />
          </label>
          {isRegister && (
            <label className="field">
              <span>昵称</span>
              <input value={form.nickname} onChange={setField('nickname')} placeholder="最多 20 个字" />
            </label>
          )}
          <label className="field">
            <span>密码</span>
            <input
              type="password"
              value={form.password}
              onChange={setField('password')}
              placeholder={isRegister ? '8-32 位，需含字母与数字' : '请输入密码'}
              autoComplete={isRegister ? 'new-password' : 'current-password'}
            />
          </label>
          <button className="gold-btn block" type="submit" disabled={submitting}>
            {submitting ? '请稍候…' : isRegister ? '注 册' : '登 录'}
          </button>
        </form>
        <div className="auth-switch">
          {isRegister ? (
            <>已有账号？<span className="link gold" onClick={() => navigate('/login')}>去登录</span></>
          ) : (
            <>还没有账号？<span className="link gold" onClick={() => navigate('/register')}>立即注册</span></>
          )}
          <span className="link muted" onClick={() => navigate('/')}>先随便逛逛 ›</span>
        </div>
      </div>
    </div>
  )
}
