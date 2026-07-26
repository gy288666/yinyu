import { useCallback, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { usePlayerStore, formatTime } from '../store/playerStore'
import { formatDuration } from '../utils'

const MODE_ICON = { order: '⇉', random: '⤨', loop: '↻' }
const MODE_LABEL = { order: '顺序播放', random: '随机播放', loop: '单曲循环' }

function ProgressBar({ value, max, onSeek }) {
  const barRef = useRef(null)
  const [dragRatio, setDragRatio] = useState(null)

  const ratioFromEvent = useCallback((e) => {
    const rect = barRef.current.getBoundingClientRect()
    return Math.min(1, Math.max(0, (e.clientX - rect.left) / rect.width))
  }, [])

  const onMouseDown = (e) => {
    const move = (ev) => setDragRatio(ratioFromEvent(ev))
    const up = (ev) => {
      const r = ratioFromEvent(ev)
      setDragRatio(null)
      if (max > 0) onSeek(r * max)
      window.removeEventListener('mousemove', move)
      window.removeEventListener('mouseup', up)
    }
    setDragRatio(ratioFromEvent(e))
    window.addEventListener('mousemove', move)
    window.addEventListener('mouseup', up)
  }

  const ratio = dragRatio !== null ? dragRatio : max > 0 ? value / max : 0
  return (
    <div className="progress-bar" ref={barRef} onMouseDown={onMouseDown}>
      <div className="progress-track">
        <div className="progress-fill" style={{ width: `${ratio * 100}%` }} />
        <div className="progress-thumb" style={{ left: `${ratio * 100}%` }} />
      </div>
    </div>
  )
}

export default function PlayerBar() {
  const navigate = useNavigate()
  const queue = usePlayerStore((s) => s.queue)
  const index = usePlayerStore((s) => s.index)
  const playing = usePlayerStore((s) => s.playing)
  const mode = usePlayerStore((s) => s.mode)
  const currentTime = usePlayerStore((s) => s.currentTime)
  const duration = usePlayerStore((s) => s.duration)
  const volume = usePlayerStore((s) => s.volume)
  const queueOpen = usePlayerStore((s) => s.queueOpen)
  const fmMode = usePlayerStore((s) => s.fmMode)
  const loadingUrl = usePlayerStore((s) => s.loadingUrl)

  const song = index >= 0 && index < queue.length ? queue[index] : null
  const { togglePlay, prev, next, cycleMode, seek, setVolume, toggleLike, toggleQueue, removeFromQueue, clearQueue, playList } =
    usePlayerStore.getState()

  return (
    <>
      {queueOpen && (
        <div className="queue-panel card">
          <div className="queue-head">
            <span>播放队列（{queue.length}）</span>
            <div>
              <button className="text-btn" onClick={clearQueue}>清空</button>
              <button className="text-btn" onClick={toggleQueue}>关闭</button>
            </div>
          </div>
          <div className="queue-list">
            {queue.length === 0 && <div className="empty-tip">队列空空如也，去发现页找点音乐吧</div>}
            {queue.map((s, i) => (
              <div
                key={`${s.id}-${i}`}
                className={`queue-item ${i === index ? 'active' : ''}`}
                onDoubleClick={() => playList(queue, i)}
              >
                <span className="q-name" onClick={() => playList(queue, i)}>
                  {i === index && <span className="playing-dot">♪ </span>}
                  {s.name}
                </span>
                <span className="q-singer">{s.singerName}</span>
                <span className="q-dur">{formatDuration(s.duration)}</span>
                <button className="text-btn q-del" onClick={() => removeFromQueue(i)} title="移除">✕</button>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="player-bar">
        <div className="player-left">
          {song ? (
            <>
              <img
                className="player-cover"
                src={song.cover || '/static/img/singer/mao-buyi.jpg'}
                alt={song.name}
                onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = '/static/img/singer/mao-buyi.jpg' }}
              />
              <div className="player-song-info">
                <div className="player-song-name" title={song.name}>
                  {song.name}
                  {song.vip && <span className="tag-vip">VIP</span>}
                </div>
                <div
                  className="player-song-singer link"
                  onClick={() => song.singerId && navigate(`/singer/${song.singerId}`)}
                >
                  {song.singerName || '未知歌手'}
                </div>
              </div>
              <button
                className={`icon-btn like-btn ${song.liked ? 'liked' : ''}`}
                title={song.liked ? '取消喜欢' : '喜欢'}
                onClick={() => toggleLike(song)}
              >
                {song.liked ? '♥' : '♡'}
              </button>
            </>
          ) : (
            <div className="player-song-info">
              <div className="player-song-name muted">音域 YINYU</div>
              <div className="player-song-singer">聆听世界的声音</div>
            </div>
          )}
        </div>

        <div className="player-center">
          <div className="player-controls">
            <button className="icon-btn" title={MODE_LABEL[mode]} onClick={cycleMode}>
              {MODE_ICON[mode]}
            </button>
            {!fmMode && (
              <button className="icon-btn ctrl" title="上一首" onClick={prev} disabled={!song}>
                ⏮
              </button>
            )}
            <button className="play-btn" onClick={togglePlay} disabled={!song || loadingUrl} title={playing ? '暂停' : '播放'}>
              {loadingUrl ? '…' : playing ? '❚❚' : '▶'}
            </button>
            <button className="icon-btn ctrl" title="下一首" onClick={() => next(false)} disabled={!song && !fmMode}>
              ⏭
            </button>
            <button className={`icon-btn ${queueOpen ? 'gold' : ''}`} title="播放队列" onClick={toggleQueue}>
              ☰
            </button>
          </div>
          <div className="player-progress">
            <span className="time">{formatTime(currentTime)}</span>
            <ProgressBar value={currentTime} max={duration || song?.duration || 0} onSeek={seek} />
            <span className="time">{formatTime(duration || song?.duration || 0)}</span>
          </div>
        </div>

        <div className="player-right">
          <span className="icon-btn" title="音量">{volume === 0 ? '🔇' : '🔊'}</span>
          <input
            className="volume-slider"
            type="range"
            min="0"
            max="1"
            step="0.01"
            value={volume}
            onChange={(e) => setVolume(Number(e.target.value))}
          />
        </div>
      </div>
    </>
  )
}
