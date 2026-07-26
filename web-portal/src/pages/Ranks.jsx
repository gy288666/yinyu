import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { apiRankDetail } from '../api'
import { Loading, Empty } from '../components/common'
import SongTable from '../components/SongTable'
import { usePlayerStore } from '../store/playerStore'

const TABS = [
  { type: 'HOT', name: '热歌榜', desc: '近 7 天播放量' },
  { type: 'NEW', name: '新歌榜', desc: '近 30 天新上架' },
  { type: 'ORIGINAL', name: '原创榜', desc: '原创音乐人作品' },
  { type: 'SOAR', name: '飙升榜', desc: '播放增幅最快' },
]

export default function Ranks() {
  const { type } = useParams()
  const navigate = useNavigate()
  const tab = TABS.some((t) => t.type === type) ? type : 'HOT'
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const { playList } = usePlayerStore.getState()

  useEffect(() => {
    let alive = true
    setLoading(true)
    setError(false)
    apiRankDetail(tab, 50)
      .then((d) => alive && setData(d))
      .catch(() => alive && setError(true))
      .finally(() => alive && setLoading(false))
    return () => {
      alive = false
    }
  }, [tab])

  const songs = (data?.songs || []).map((r) => ({ ...r.song, _trend: r.trend, _rankNo: r.rankNo }))
  const tabInfo = TABS.find((t) => t.type === tab)

  return (
    <div className="page">
      <div className="page-head">
        <h1 className="page-title">排行榜</h1>
        <div className="tabs">
          {TABS.map((t) => (
            <button key={t.type} className={`tab ${tab === t.type ? 'active' : ''}`} onClick={() => navigate(`/ranks/${t.type}`)}>
              {t.name}
            </button>
          ))}
        </div>
      </div>

      <div className="card rank-detail-card">
        <div className="rank-detail-head">
          <div>
            <div className="rank-detail-title">{data?.name || tabInfo.name}</div>
            <div className="rank-detail-sub">
              {tabInfo.desc}
              {data?.updateTime ? ` · 更新于 ${data.updateTime}` : ''}
            </div>
          </div>
          <button className="gold-btn" disabled={!songs.length} onClick={() => playList(songs, 0)}>
            ▶ 播放全部
          </button>
        </div>
        {loading && <Loading />}
        {!loading && (error || songs.length === 0) && <Empty text="榜单暂时没有数据" />}
        {!loading && !error && songs.length > 0 && (
          <SongTable songs={songs} showCover showPlayCount />
        )}
      </div>
    </div>
  )
}
