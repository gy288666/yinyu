import { useNavigate } from 'react-router-dom'
import { usePlayerStore } from '../store/playerStore'
import { formatDuration, formatCount, downloadSong } from '../utils'
import { TrendBadge } from './common'

// 通用歌曲列表：序号/升降角标/封面/歌名/喜欢/歌手/专辑/时长/下载
export default function SongTable({
  songs = [],
  showCover = false,
  showAlbum = true,
  showPlayCount = false,
  showTrend = false,
  onRemove,
  emptyText = '暂无歌曲',
}) {
  const navigate = useNavigate()
  const queue = usePlayerStore((s) => s.queue)
  const index = usePlayerStore((s) => s.index)
  const playing = usePlayerStore((s) => s.playing)
  const { playList, toggleLike } = usePlayerStore.getState()
  const currentId = index >= 0 && index < queue.length ? queue[index].id : null

  if (!songs || songs.length === 0) {
    return <div className="empty-box">♪ {emptyText}</div>
  }

  return (
    <div className="song-table">
      {songs.map((song, i) => {
        const isCurrent = song.id === currentId
        return (
          <div
            key={`${song.id}-${i}`}
            className={`song-row ${isCurrent ? 'current' : ''}`}
            onDoubleClick={() => playList(songs, i)}
          >
            <span className="col-index">
              {isCurrent ? <span className="gold">{playing ? '♪' : '❚❚'}</span> : String(i + 1).padStart(2, '0')}
            </span>
            {showTrend && <TrendBadge trend={song._trend} delta={song._trendDelta} />}
            <button className="row-play icon-btn" title="播放" onClick={() => playList(songs, i)}>
              ▶
            </button>
            {showCover && (
              <img
                className="row-cover"
                src={song.cover || '/static/img/singer/xu-song.jpg'}
                alt=""
                loading="lazy"
                onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = '/static/img/singer/xu-song.jpg' }}
              />
            )}
            <span className="col-name">
              <span className="song-name" title={song.name}>{song.name}</span>
              {song.vip && <span className="tag-vip">VIP</span>}
              {song.quality === 'lossless' && <span className="tag-sq">SQ</span>}
              {Number(song.price) > 0 && <span className="tag-pay">付费</span>}
            </span>
            <button
              className={`icon-btn like-btn ${song.liked ? 'liked' : ''}`}
              title={song.liked ? '取消喜欢' : '喜欢'}
              onClick={async (e) => {
                e.stopPropagation()
                const liked = await toggleLike(song)
                if (liked !== null) song.liked = liked
              }}
            >
              {song.liked ? '♥' : '♡'}
            </button>
            <span
              className="col-singer link"
              onClick={() => song.singerId && navigate(`/singer/${song.singerId}`)}
            >
              {song.singerName || '-'}
            </span>
            {showAlbum && (
              <span
                className="col-album link"
                onClick={() => song.albumId && navigate(`/album/${song.albumId}`)}
              >
                {song.albumName || '-'}
              </span>
            )}
            {showPlayCount && <span className="col-count">{formatCount(song.playCount)}</span>}
            <button
              className="icon-btn row-dl"
              title="下载"
              onClick={(e) => { e.stopPropagation(); downloadSong(song) }}
            >
              ⤓
            </button>
            <span className="col-duration">{formatDuration(song.duration)}</span>
            {onRemove && (
              <button className="text-btn" title="移除" onClick={() => onRemove(song)}>✕</button>
            )}
          </div>
        )
      })}
    </div>
  )
}
