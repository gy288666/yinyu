import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  apiBanners,
  apiRecommendPlaylists,
  apiHotPlaylists,
  apiRankDetail,
  apiNewestSongs,
  apiPlaylistDetail,
} from '../api'
import { SectionHeader, PlaylistCard, Loading, Empty, TrendBadge } from '../components/common'
import { usePlayerStore } from '../store/playerStore'
import { formatCount } from '../utils'

const RANK_TABS = [
  { type: 'HOT', name: '热歌榜' },
  { type: 'NEW', name: '新歌榜' },
  { type: 'ORIGINAL', name: '原创榜' },
  { type: 'SOAR', name: '飙升榜' },
]

const ENTRIES = [
  { icon: '📅', title: '每日推荐', desc: '根据口味生成', to: '/recommend/daily' },
  { icon: '🎵', title: '歌单广场', desc: '海量主题歌单', to: '/playlists' },
  { icon: '🏆', title: '排行榜', desc: '热歌新歌飙升', to: '/ranks' },
  { icon: '📻', title: '电台', desc: '主题连续播放', to: '/radios' },
  { icon: '💎', title: 'Hi-Res 专区', desc: 'VIP 无损音质', to: '/playlists?hires=1' },
]

function useFetch(fetcher, deps = []) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  useEffect(() => {
    let alive = true
    setLoading(true)
    setError(false)
    fetcher()
      .then((d) => alive && setData(d))
      .catch(() => alive && setError(true))
      .finally(() => alive && setLoading(false))
    return () => {
      alive = false
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)
  return { data, loading, error }
}

function Banner() {
  const navigate = useNavigate()
  const { data: banners } = useFetch(() => apiBanners())
  const [cur, setCur] = useState(0)
  const list = banners || []

  useEffect(() => {
    if (list.length <= 1) return
    const t = setInterval(() => setCur((c) => (c + 1) % list.length), 5000)
    return () => clearInterval(t)
  }, [list.length])

  const go = (b) => {
    if (!b) return navigate('/playlists')
    if (b.targetType === 'PLAYLIST' && b.targetId) navigate(`/playlist/${b.targetId}`)
    else if (b.targetType === 'SONG' && b.targetId) navigate(`/playlists`)
    else if (b.link) window.open(b.link, '_blank')
    else navigate('/playlists')
  }

  const b = list[cur]
  return (
    <div className="hero" style={b?.image ? { backgroundImage: `url(${b.image})` } : undefined}>
      <div className="hero-mask" />
      <div className="hero-content">
        <div className="hero-eyebrow">YINYU MUSIC</div>
        <h1 className="hero-title">{b?.title || '聆听世界的每一种声音'}</h1>
        <p className="hero-desc">精选歌单 · 高解析音质 · 每日为你推荐</p>
        <button className="gold-btn" onClick={() => go(b)}>开始探索</button>
      </div>
      {list.length > 1 && (
        <div className="hero-dots">
          {list.map((_, i) => (
            <span key={i} className={`dot ${i === cur ? 'active' : ''}`} onClick={() => setCur(i)} />
          ))}
        </div>
      )}
    </div>
  )
}

function RankCard() {
  const navigate = useNavigate()
  const [tab, setTab] = useState('HOT')
  const { data, loading, error } = useFetch(() => apiRankDetail(tab, 10), [tab])
  const { playList, toggleLike } = usePlayerStore.getState()
  const [, force] = useState(0)

  const rows = data?.songs || []
  const songs = rows.map((r) => r.song).filter(Boolean)

  return (
    <section className="section">
      <SectionHeader title="排行榜" more="完整榜单" onMore={() => navigate(`/ranks/${tab}`)} />
      <div className="card rank-card">
        <div className="tabs">
          {RANK_TABS.map((t) => (
            <button key={t.type} className={`tab ${tab === t.type ? 'active' : ''}`} onClick={() => setTab(t.type)}>
              {t.name}
            </button>
          ))}
        </div>
        {loading && <Loading />}
        {!loading && (error || rows.length === 0) && <Empty text="榜单暂时没有数据" />}
        {!loading && !error && rows.length > 0 && (
          <div className="rank-list">
            {rows.map((r, i) => (
              <div key={r.song?.id || i} className="rank-row" onDoubleClick={() => playList(songs, i)}>
                <span className={`rank-no ${i < 3 ? 'top3' : ''}`}>{i + 1}</span>
                <TrendBadge trend={r.trend} delta={r.trendDelta} />
                <button className="row-play icon-btn" onClick={() => playList(songs, i)} title="播放">▶</button>
                <span className="rank-name ellipsis">{r.song?.name}</span>
                <button
                  className={`icon-btn like-btn ${r.song?.liked ? 'liked' : ''}`}
                  onClick={async () => {
                    const liked = await toggleLike(r.song)
                    if (liked !== null) {
                      r.song.liked = liked
                      force((x) => x + 1)
                    }
                  }}
                >
                  {r.song?.liked ? '♥' : '♡'}
                </button>
                <span className="rank-singer ellipsis">{r.song?.singerName}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </section>
  )
}

export default function Discover() {
  const navigate = useNavigate()
  const { playList } = usePlayerStore.getState()

  const recPl = useFetch(() => apiRecommendPlaylists(6))
  const hotPl = useFetch(() => apiHotPlaylists(5))
  const newest = useFetch(() => apiNewestSongs(12))

  const playPlaylist = async (pl) => {
    try {
      const detail = await apiPlaylistDetail(pl.id)
      if (detail?.songs?.length) playList(detail.songs, 0)
    } catch (e) {
      /* 已 toast */
    }
  }

  return (
    <div className="page discover-page">
      <Banner />

      <div className="entry-grid">
        {ENTRIES.map((e) => (
          <div key={e.title} className="entry-card card" onClick={() => navigate(e.to)}>
            <span className="entry-icon">{e.icon}</span>
            <div>
              <div className="entry-title">{e.title}</div>
              <div className="entry-desc">{e.desc}</div>
            </div>
          </div>
        ))}
      </div>

      <section className="section">
        <SectionHeader title="为你推荐" more="更多歌单" onMore={() => navigate('/playlists')} />
        {recPl.loading && <Loading />}
        {!recPl.loading && (recPl.error || !(recPl.data || []).length) && <Empty text="推荐内容准备中，稍后再来看看" />}
        <div className="pl-grid">
          {(recPl.data || []).map((p) => (
            <PlaylistCard key={p.id} playlist={p} onPlay={playPlaylist} />
          ))}
        </div>
      </section>

      <div className="discover-two-col">
        <section className="section">
          <SectionHeader title="热门歌单" more="歌单广场" onMore={() => navigate('/playlists')} />
          <div className="card hot-pl-card">
            {hotPl.loading && <Loading />}
            {!hotPl.loading && (hotPl.error || !(hotPl.data || []).length) && <Empty text="暂无热门歌单" />}
            {(hotPl.data || []).slice(0, 5).map((p, i) => (
              <div key={p.id} className="hot-pl-row" onClick={() => navigate(`/playlist/${p.id}`)}>
                <span className={`hot-no ${i < 3 ? 'top3' : ''}`}>{String(i + 1).padStart(2, '0')}</span>
                <img
                  className="hot-cover"
                  src={p.cover || '/static/img/singer/deng-ziqi.jpg'}
                  alt=""
                  onError={(ev) => { ev.currentTarget.onerror = null; ev.currentTarget.src = '/static/img/singer/deng-ziqi.jpg' }}
                />
                <div className="hot-info">
                  <div className="ellipsis">{p.title}</div>
                  <div className="hot-meta">▶ {formatCount(p.playCount)} · {p.songCount || 0} 首</div>
                </div>
                <button
                  className="row-play icon-btn"
                  title="播放"
                  onClick={(e) => { e.stopPropagation(); playPlaylist(p) }}
                >
                  ▶
                </button>
              </div>
            ))}
          </div>
        </section>
        <RankCard />
      </div>

      <section className="section">
        <SectionHeader title="新歌速递" more="更多新歌" onMore={() => navigate('/ranks/NEW')} />
        {newest.loading && <Loading />}
        {!newest.loading && (newest.error || !(newest.data || []).length) && <Empty text="暂无新歌上架" />}
        <div className="new-song-scroll">
          {(newest.data || []).map((s, i) => (
            <div key={s.id} className="new-song-card card" onClick={() => playList(newest.data, i)}>
              <div className="new-cover-wrap">
                <img
                  className="new-cover"
                  src={s.cover || '/static/img/singer/lin-junjie.jpg'}
                  alt={s.name}
                  loading="lazy"
                  onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = '/static/img/singer/lin-junjie.jpg' }}
                />
                <span className="new-play">▶</span>
              </div>
              <div className="new-name ellipsis" title={s.name}>{s.name}</div>
              <div className="new-singer ellipsis">{s.singerName}</div>
              <div className="new-date">{(s.publishTime || '').slice(0, 10)}</div>
            </div>
          ))}
        </div>
      </section>
    </div>
  )
}
