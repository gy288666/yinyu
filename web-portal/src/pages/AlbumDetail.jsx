import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { apiAlbumDetail, apiAlbumSongs } from '../api'
import { Loading, Empty } from '../components/common'
import SongTable from '../components/SongTable'
import { usePlayerStore } from '../store/playerStore'

export default function AlbumDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [album, setAlbum] = useState(null)
  const [songs, setSongs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const { playList } = usePlayerStore.getState()

  useEffect(() => {
    let alive = true
    setLoading(true)
    setError(false)
    Promise.allSettled([apiAlbumDetail(id), apiAlbumSongs(id)]).then(([d, s]) => {
      if (!alive) return
      if (d.status === 'fulfilled') setAlbum(d.value)
      else setError(true)
      setSongs(s.status === 'fulfilled' ? s.value || [] : [])
      setLoading(false)
    })
    return () => {
      alive = false
    }
  }, [id])

  if (loading) return <div className="page"><Loading /></div>
  if (error || !album) return <div className="page"><Empty text="专辑不存在或暂时无法访问" /></div>

  return (
    <div className="page">
      <div className="detail-head">
        <img
          className="detail-cover"
          src={album.cover || '/static/img/singer/li-ronghao.jpg'}
          alt={album.name}
          onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = '/static/img/singer/li-ronghao.jpg' }}
        />
        <div className="detail-info">
          <div className="detail-type">专辑</div>
          <h1 className="detail-title">{album.name}</h1>
          <div className="detail-meta">
            <span className="link gold" onClick={() => album.singerId && navigate(`/singer/${album.singerId}`)}>
              {album.singerName}
            </span>
            <span>{album.publishDate || ''}</span>
            <span>{album.songCount || songs.length} 首</span>
          </div>
          {album.intro && <p className="detail-intro">{album.intro}</p>}
          <div className="detail-actions">
            <button className="gold-btn" disabled={!songs.length} onClick={() => playList(songs, 0)}>▶ 播放全部</button>
          </div>
        </div>
      </div>
      <div className="card">
        <SongTable songs={songs} showAlbum={false} emptyText="专辑内暂无曲目" />
      </div>
    </div>
  )
}
