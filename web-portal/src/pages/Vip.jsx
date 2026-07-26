import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiVipPlans, apiCreateOrder, apiPayOrder, apiOrderDetail } from '../api'
import { Loading, Empty } from '../components/common'
import { useAuthStore } from '../store/authStore'
import { toast } from '../components/toast'

const PRIVILEGES = [
  { icon: '💎', title: 'Hi-Res 无损', desc: 'flac / wav 高解析音质' },
  { icon: '🎧', title: 'VIP 曲库', desc: '会员专享歌曲畅听' },
  { icon: '⬇', title: '下载特权', desc: '每日 100 首下载配额' },
  { icon: '✦', title: '尊贵标识', desc: '评论区专属 VIP 标识' },
]

export default function Vip() {
  const navigate = useNavigate()
  const [plans, setPlans] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [buying, setBuying] = useState(null)
  const user = useAuthStore((s) => s.user)
  const logged = useAuthStore((s) => s.logged)
  const refreshMe = useAuthStore((s) => s.refreshMe)

  useEffect(() => {
    apiVipPlans()
      .then((d) => setPlans(d || []))
      .catch(() => setError(true))
      .finally(() => setLoading(false))
  }, [])

  const buy = async (plan) => {
    if (!logged) {
      toast.warn('请先登录后开通会员')
      navigate('/login')
      return
    }
    setBuying(plan.id)
    try {
      const order = await apiCreateOrder({ orderType: 'VIP', planId: plan.id })
      const pay = await apiPayOrder(order.orderNo, 'MOCK')
      if (pay?.status === 'PAID') {
        toast.success(`开通成功：${order.subject || plan.name}`)
        refreshMe()
      } else if (pay?.payUrl) {
        window.open(pay.payUrl, '_blank')
        toast('已跳转支付页，支付完成后自动到账')
        // 轮询订单状态
        let tries = 0
        const timer = setInterval(async () => {
          tries += 1
          try {
            const od = await apiOrderDetail(order.orderNo)
            if (od?.status === 'PAID') {
              clearInterval(timer)
              toast.success('支付成功，VIP 已到账')
              refreshMe()
            }
          } catch (e) {
            /* 忽略 */
          }
          if (tries > 30) clearInterval(timer)
        }, 3000)
      } else {
        // 未知返回：查询一次订单
        const od = await apiOrderDetail(order.orderNo).catch(() => null)
        if (od?.status === 'PAID') {
          toast.success('支付成功，VIP 已到账')
          refreshMe()
        }
      }
    } catch (e) {
      /* 已 toast */
    } finally {
      setBuying(null)
    }
  }

  return (
    <div className="page vip-page">
      <div className="vip-hero card">
        <div>
          <h1 className="vip-hero-title">音域黑金会员</h1>
          <p className="muted">
            {logged
              ? user?.vip
                ? `会员有效期至 ${user.vipExpireAt || '-'}`
                : '开通会员，解锁全部特权'
              : '登录并开通会员，解锁全部特权'}
          </p>
        </div>
        {user?.vip && <span className="tag-vip big">VIP</span>}
      </div>

      <div className="privilege-grid">
        {PRIVILEGES.map((p) => (
          <div key={p.title} className="privilege-card card">
            <span className="entry-icon">{p.icon}</span>
            <div className="entry-title">{p.title}</div>
            <div className="entry-desc">{p.desc}</div>
          </div>
        ))}
      </div>

      <h2 className="section-title">选择套餐</h2>
      {loading && <Loading />}
      {!loading && (error || !(plans || []).length) && <Empty text="套餐信息暂时无法获取" />}
      <div className="plan-grid">
        {(plans || []).map((p, i) => (
          <div key={p.id} className={`plan-card card ${i === (plans.length > 1 ? 1 : 0) ? 'featured' : ''}`}>
            {i === 1 && <span className="plan-badge">最受欢迎</span>}
            <div className="plan-name">{p.name}</div>
            <div className="plan-price">
              <span className="currency">¥</span>
              <span className="amount">{p.price}</span>
            </div>
            {p.originPrice && <div className="plan-origin">原价 ¥{p.originPrice}</div>}
            <div className="plan-days muted">{p.days} 天会员时长</div>
            <button className="gold-btn block" disabled={buying === p.id} onClick={() => buy(p)}>
              {buying === p.id ? '处理中…' : '立即开通'}
            </button>
          </div>
        ))}
      </div>
      <p className="muted small-text center">演示环境使用模拟支付（MOCK），点击开通即完成购买流程</p>
    </div>
  )
}
