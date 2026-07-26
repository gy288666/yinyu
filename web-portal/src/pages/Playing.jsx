import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiSongLyric } from '../api'
import { usePlayerStore } from '../store/playerStore'
import CommentSection from '../components/CommentSection'
import { downloadSong } from '../utils'

// 解析 LRC：[mm:ss.xx] / [mm:ss]，同一行支持多个时间标签
export function parseLrc(text) {
  if (!text) return []
  const lines = []
  const tagRe = /\[(\d{1,2}):(\d{1,2})(?:[.:](\d{1,3}))?\]/g
  for (const raw of String(text).split(/\r?\n/)) {
    const tags = [...raw.matchAll(tagRe)]
    if (tags.length === 0) continue
    const content = raw.replace(tagRe, '').trim()
    for (const t of tags) {
      const min = Number(t[1])
      const sec = Number(t[2])
      const frac = t[3] ? Number(`0.${t[3]}`) : 0
      if (Number.isNaN(min) || Number.isNaN(sec)) continue
      lines.push({ time: min * 60 + sec + frac, text: content })
    }
  }
  return lines.filter((l) => l.text).sort((a, b) => a.time - b.time)
}

function Lyric({ songId }) {
  const currentTime = usePlayerStore((s) => s.currentTime)
  const [lyricText, setLyricText] = useState(null)
  const [loading, setLoading] = useState(true)
  const boxRef = useRef(null)
  const lineRefs = useRef([])

  useEffect(() => {
    let alive = true
    setLoading(true)
    setLyricText(null)
    if (!songId) {
      setLoading(false)
      return
    }
    apiSongLyric(songId)
      .then((d) => alive && setLyricText(d?.lyric || ''))
      .catch(() => alive && setLyricText(''))
      .finally(() => alive && setLoading(false))
    return () => {
      alive = false
    }
  }, [songId])

  const lines = useMemo(() => parseLrc(lyricText), [lyricText])

  // 当前行：最后一个 time <= currentTime 的行
  let cur = -1
  for (let i = 0; i < lines.length; i++) {
    if (lines[i].time <= currentTime + 0.2) cur = i
    else break
  }

  // 跟随高亮自动滚动到中间
  useEffect(() => {
    const box = boxRef.current
    const el = lineRefs.current[cur]
    if (!box || !el) return
    const target = el.offsetTop - box.clientHeight / 2 + el.clientHeight / 2
    box.scrollTo({ top: Math.max(0, target), behavior: 'smooth' })
  }, [cur])

  if (loading) return <div className="lyric-box lyric-empty muted">歌词加载中…</div>
  if (lines.length === 0) {
    // 无时间轴但有文本：静态展示；完全为空：纯音乐
    const plain = String(lyricText || '')
      .split(/\r?\n/)
      .map((s) => s.replace(/\[[^\]]*\]/g, '').trim())
      .filter(Boolean)
    if (plain.length === 0) {
      return (
        <div className="lyric-box lyric-empty">
          <span className="big-note">♫</span>
          <p className="muted">纯音乐，请欣赏</p>
        </div>
      )
    }
    return (
      <div className="lyric-box" ref={boxRef}>
        <div className="lyric-inner">
          {plain.map((t, i) => (
            <div key={i} className="lyric-line">{t}</div>
          ))}
        </div>
      </div>
    )
  }

  return (
    <div className="lyric-box" ref={boxRef}>
      <div className="lyric-inner">
        {lines.map((l, i) => (
          <div
            key={`${l.time}-${i}`}
            ref={(el) => (lineRefs.current[i] = el)}
            className={`lyric-line ${i === cur ? 'active' : ''}`}
          >
            {l.text}
          </div>
        ))}
      </div>
    </div>
  )
}

export default function Playing() {
  const navigate = useNavigate()
  const queue = usePlayerStore((s) => s.queue)
  const index = usePlayerStore((s) => s.index)
  const playing = usePlayerStore((s) => s.playing)
  const { toggleLike } = usePlayerStore.getState()

  const song = index >= 0 && index < queue.length ? queue[index] : null

  if (!song) {
    return (
      <div className="page">
        <div className="empty-box tall">
          <div className="big-note">♫</div>
          <p>还没有正在播放的歌曲</p>
          <button className="gold-btn" onClick={() => navigate('/')}>去发现音乐</button>
        </div>
      </div>
    )
  }

  return (
    <div className="page playing-page">
      <div className="playing-stage">
        <div className="playing-left">
          <div className="vinyl-wrap">
            <div className={`vinyl-disc ${playing ? '' : 'paused'}`}>
              <img
                src={song.cover || '/static/img/singer/zhou-jielun.jpg'}
                alt={song.name}
                onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = '/static/img/singer/zhou-jielun.jpg' }}
              />
              <span className="vinyl-hole" />
            </div>
          </div>
          <div className="playing-actions">
            <button
              className={`icon-btn big like-btn ${song.liked ? 'liked' : ''}`}
              title={song.liked ? '取消喜欢' : '喜欢'}
              onClick={() => toggleLike(song)}
            >
              {song.liked ? '♥' : '♡'}
            </button>
            <button className="icon-btn big" title="下载" onClick={() => downloadSong(song)}>⤓</button>
          </div>
        </div>

        <div className="playing-right">
          <h1 className="playing-title">
            {song.name}
            {song.vip && <span className="tag-vip">VIP</span>}
            {song.quality === 'lossless' && <span className="tag-sq">SQ</span>}
          </h1>
          <div className="playing-meta muted">
            歌手：
            <span className="link" onClick={() => song.singerId && navigate(`/singer/${song.singerId}`)}>
              {song.singerName || '未知歌手'}
            </span>
            {song.albumName && (
              <>
                　专辑：
                <span className="link" onClick={() => song.albumId && navigate(`/album/${song.albumId}`)}>
                  {song.albumName}
                </span>
              </>
            )}
          </div>
          <Lyric songId={song.id} />
        </div>
      </div>

      <div className="card playing-comments">
        <CommentSection targetType="SONG" targetId={song.id} />
      </div>
    </div>
  )
}
