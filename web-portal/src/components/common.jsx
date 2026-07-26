import { useNavigate } from 'react-router-dom'
import { formatCount } from '../utils'

export function Loading({ text = '加载中…' }) {
  return <div className="loading-box"><span className="spinner" />{text}</div>
}

export function Empty({ text = '暂无内容' }) {
  return <div className="empty-box">♪ {text}</div>
}

export function SectionHeader({ title, more, onMore, extra }) {
  return (
    <div className="section-header">
      <h2 className="section-title">{title}</h2>
      <div className="section-extra">{extra}</div>
      {more && (
        <span className="section-more link" onClick={onMore}>
          {more} ›
        </span>
      )}
    </div>
  )
}

// 排行榜升降角标：UP 红↑ / DOWN 绿↓ / 其余 灰－
export function TrendBadge({ trend, delta }) {
  const t = String(trend || '').toUpperCase()
  if (t === 'UP') {
    return <span className="trend-badge up" title={`上升 ${delta || 0} 位`}>↑{delta > 0 ? delta : ''}</span>
  }
  if (t === 'DOWN') {
    return <span className="trend-badge down" title={`下降 ${delta || 0} 位`}>↓{delta > 0 ? delta : ''}</span>
  }
  return <span className="trend-badge flat" title="持平">－</span>
}

export function PlaylistCard({ playlist, onPlay }) {
  const navigate = useNavigate()
  return (
    <div className="pl-card" onClick={() => navigate(`/playlist/${playlist.id}`)}>
      <div className="pl-cover-wrap">
        <img
          className="pl-cover"
          src={playlist.cover || '/static/img/singer/shan-yichun.jpg'}
          alt={playlist.title}
          loading="lazy"
          onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = '/static/img/singer/shan-yichun.jpg' }}
        />
        <span className="pl-playcount">▶ {formatCount(playlist.playCount)}</span>
        <button
          className="pl-play-btn"
          title="播放"
          onClick={(e) => {
            e.stopPropagation()
            if (onPlay) onPlay(playlist)
            else navigate(`/playlist/${playlist.id}?autoplay=1`)
          }}
        >
          ▶
        </button>
      </div>
      <div className="pl-title" title={playlist.title}>{playlist.title}</div>
    </div>
  )
}
