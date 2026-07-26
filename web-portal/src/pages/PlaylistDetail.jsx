import { useEffect, useState } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { apiPlaylistDetail, apiCollectPlaylist, apiUncollectPlaylist, apiPlaylistRemoveSong } from '../api'
import { Loading, Empty } from '../components/common'
import SongTable from '../components/SongTable'
import { usePlayerStore } from '../store/playerStore'
import { useAuthStore } from '../store/authStore'
import { toast } from '../components/toast'
import { formatCount } from '../utils'

export default function PlaylistDetail() {
  const { id } = useParams()
  const [params] = useSearchParams()
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const { playList } = usePlayerStore.getState()
  const user = useAuthStore((s) => s.user)
  const logged = useAuthStore((s) => s.logged)

  const load = () => {
    setLoading(true)
    setError(false)
    apiPlaylistDetail(id)
      .then((d) => {
        setData(d)
        if (params.get('autoplay') === '1' && d?.songs?.length) playList(d.songs, 0)
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false))
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(load, [id])

  const toggleCollect = async () => {
    if (!logged) return toast.warn('请先登录')
    try {
      if (data.collected) {
        await apiUncollectPlaylist(id)
        setData({ ...data, collected: false, collectCount: Math.max(0, (data.collectCount || 1) - 1) })
      } else {
        await apiCollectPlaylist(id)
        setData({ ...data, collected: true, collectCount: (data.collectCount || 0) + 1 })
        toast.success('收藏成功')
      }
    } catch (e) {
      /* 已 toast */
    }
  }

  const isMine = logged && data?.creatorId && user?.userId === data.creatorId
  const removeSong = async (song) => {
    try {
      await apiPlaylistRemoveSong(id, song.id)
      setData({ ...data, songs: data.songs.filter((s) => s.id !== song.id) })
      toast.success('已从歌单移除')
    } catch (e) {
      /* 已 toast */
    }
  }

  if (loading) return <div className="page"><Loading /></div>
  if (error || !data) return <div className="page"><Empty text="歌单不存在或暂时无法访问" /></div>

  const songs = data.songs || []
  return (
    <div className="page">
      <div className="detail-head">
        <img
          className="detail-cover"
          src={data.cover || '/static/img/singer/zhang-bichen.jpg'}
          alt={data.title}
          onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = '/static/img/singer/zhang-bichen.jpg' }}
        />
        <div className="detail-info">
          <div className="detail-type">歌单 {data.official && <span className="tag-official">官方</span>}</div>
          <h1 className="detail-title">{data.title}</h1>
          <div className="detail-meta">
            <span>by {data.creatorName || '音域运营'}</span>
            <span>▶ {formatCount(data.playCount)}</span>
            <span>★ {formatCount(data.collectCount)}</span>
          </div>
          {(data.tags || []).length > 0 && (
            <div className="detail-tags">
              {data.tags.map((t) => <span key={t} className="chip static">{t}</span>)}
            </div>
          )}
          {data.intro && <p className="detail-intro">{data.intro}</p>}
          <div className="detail-actions">
            <button className="gold-btn" disabled={!songs.length} onClick={() => playList(songs, 0)}>▶ 播放全部（{songs.length}）</button>
            <button className={`ghost-btn ${data.collected ? 'gold' : ''}`} onClick={toggleCollect}>
              {data.collected ? '★ 已收藏' : '☆ 收藏'}
            </button>
          </div>
        </div>
      </div>

      <div className="card">
        <SongTable songs={songs} emptyText="这个歌单还没有歌曲" onRemove={isMine ? removeSong : undefined} />
      </div>
    </div>
  )
}
