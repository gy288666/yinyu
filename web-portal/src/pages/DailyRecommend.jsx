import { useEffect, useState } from 'react'
import { apiDailyRecommend } from '../api'
import { Loading, Empty } from '../components/common'
import SongTable from '../components/SongTable'
import { usePlayerStore } from '../store/playerStore'

export default function DailyRecommend() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const { playList } = usePlayerStore.getState()

  useEffect(() => {
    apiDailyRecommend()
      .then((d) => setData(d))
      .catch(() => setError(true))
      .finally(() => setLoading(false))
  }, [])

  const songs = data?.songs || []
  const today = data?.date || new Date().toISOString().slice(0, 10)
  const [, month, day] = today.split('-')

  return (
    <div className="page">
      <div className="daily-head card">
        <div className="daily-date">
          <span className="daily-day">{day || '--'}</span>
          <span className="daily-month">{month ? `${Number(month)} 月` : ''}</span>
        </div>
        <div className="detail-info">
          <h1 className="detail-title">每日推荐</h1>
          <p className="muted">根据你的口味生成 · 每天 0 点更新{!songs.length ? '' : ` · 共 ${songs.length} 首`}</p>
          <div className="detail-actions">
            <button className="gold-btn" disabled={!songs.length} onClick={() => playList(songs, 0)}>▶ 播放全部</button>
          </div>
        </div>
      </div>
      {loading && <Loading />}
      {!loading && (error || songs.length === 0) && <Empty text="今日推荐生成中，稍后再来看看" />}
      {!loading && songs.length > 0 && (
        <div className="card">
          <SongTable songs={songs} showCover />
        </div>
      )}
    </div>
  )
}
