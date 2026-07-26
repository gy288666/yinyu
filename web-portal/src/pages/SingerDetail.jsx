import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { apiSingerDetail, apiSingerSongs, apiSingerAlbums } from '../api'
import { Loading, Empty } from '../components/common'
import SongTable from '../components/SongTable'
import { usePlayerStore } from '../store/playerStore'
import { singerAvatar, onSingerImgError } from '../utils'

const TYPE_LABEL = { 1: '男歌手', 2: '女歌手', 3: '组合', 4: '厂牌' }

export default function SingerDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [singer, setSinger] = useState(null)
  const [songs, setSongs] = useState([])
  const [albums, setAlbums] = useState([])
  const [tab, setTab] = useState('songs')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const { playList } = usePlayerStore.getState()

  useEffect(() => {
    let alive = true
    setLoading(true)
    setError(false)
    Promise.allSettled([
      apiSingerDetail(id),
      apiSingerSongs(id, { pageNum: 1, pageSize: 50 }),
      apiSingerAlbums(id, { pageNum: 1, pageSize: 30 }),
    ]).then(([d, s, a]) => {
      if (!alive) return
      if (d.status === 'fulfilled') setSinger(d.value)
      else setError(true)
      setSongs(s.status === 'fulfilled' ? s.value?.list || [] : [])
      setAlbums(a.status === 'fulfilled' ? a.value?.list || [] : [])
      setLoading(false)
    })
    return () => {
      alive = false
    }
  }, [id])

  if (loading) return <div className="page"><Loading /></div>
  if (error || !singer) return <div className="page"><Empty text="歌手不存在或暂时无法访问" /></div>

  return (
    <div className="page">
      <div className="detail-head">
        <img
          className="detail-cover round"
          src={singerAvatar(singer)}
          alt={singer.name}
          onError={(e) => onSingerImgError(e, singer.id)}
        />
        <div className="detail-info">
          <div className="detail-type">歌手 · {singer.area || '未知'} · {TYPE_LABEL[singer.type] || '其他'}</div>
          <h1 className="detail-title">{singer.name}</h1>
          <div className="detail-meta">
            <span>单曲 {singer.songCount ?? songs.length}</span>
            <span>专辑 {singer.albumCount ?? albums.length}</span>
          </div>
          {singer.intro && <p className="detail-intro">{singer.intro}</p>}
          <div className="detail-actions">
            <button className="gold-btn" disabled={!songs.length} onClick={() => playList(songs, 0)}>▶ 播放热门</button>
          </div>
        </div>
      </div>

      <div className="tabs">
        <button className={`tab ${tab === 'songs' ? 'active' : ''}`} onClick={() => setTab('songs')}>热门歌曲</button>
        <button className={`tab ${tab === 'albums' ? 'active' : ''}`} onClick={() => setTab('albums')}>专辑</button>
      </div>

      {tab === 'songs' && (
        <div className="card">
          <SongTable songs={songs} emptyText="暂无上架歌曲" />
        </div>
      )}
      {tab === 'albums' && (
        <div className="pl-grid wide">
          {albums.length === 0 && <Empty text="暂无专辑" />}
          {albums.map((a) => (
            <div key={a.id} className="pl-card" onClick={() => navigate(`/album/${a.id}`)}>
              <div className="pl-cover-wrap">
                <img
                  className="pl-cover"
                  src={a.cover || singerAvatar(singer)}
                  alt={a.name}
                  onError={(e) => onSingerImgError(e, singer.id)}
                />
              </div>
              <div className="pl-title">{a.name}</div>
              <div className="muted small-text">{a.publishDate || ''} · {a.songCount || 0} 首</div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
