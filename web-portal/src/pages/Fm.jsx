import { useEffect, useRef } from 'react'
import { apiFmNext, apiFmDislike } from '../api'
import RequireLogin from '../components/RequireLogin'
import { usePlayerStore } from '../store/playerStore'
import { toast } from '../components/toast'

function FmInner() {
  const queue = usePlayerStore((s) => s.queue)
  const index = usePlayerStore((s) => s.index)
  const playing = usePlayerStore((s) => s.playing)
  const fmMode = usePlayerStore((s) => s.fmMode)
  const startedRef = useRef(false)
  const { playList, next, togglePlay, toggleLike } = usePlayerStore.getState()

  const song = fmMode && index >= 0 ? queue[index] : null

  useEffect(() => {
    if (startedRef.current || fmMode) return
    startedRef.current = true
    apiFmNext()
      .then((s) => {
        if (s) playList([s], 0, true)
      })
      .catch(() => {})
  }, [fmMode, playList])

  const dislike = async () => {
    if (!song) return
    try {
      await apiFmDislike(song.id)
      toast('已标记不喜欢，7 天内不再推荐')
      next(false)
    } catch (e) {
      /* 已 toast */
    }
  }

  return (
    <div className="page fm-page">
      <h1 className="page-title center">私人FM</h1>
      <p className="muted center">专属电波，一键连续播放</p>
      <div className="fm-stage">
        <div className={`fm-disc ${playing ? 'spin' : ''}`}>
          <img
            src={song?.cover || '/static/img/singer/xue-zhiqian.jpg'}
            alt={song?.name || '私人FM'}
            onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = '/static/img/singer/xue-zhiqian.jpg' }}
          />
        </div>
        <div className="fm-song-name">{song?.name || '正在为你挑选…'}</div>
        <div className="fm-singer muted">{song?.singerName || ''}</div>
        <div className="fm-controls">
          <button className="icon-btn big" title="不喜欢" onClick={dislike} disabled={!song}>🗑</button>
          <button
            className={`icon-btn big like-btn ${song?.liked ? 'liked' : ''}`}
            title="喜欢"
            disabled={!song}
            onClick={() => song && toggleLike(song)}
          >
            {song?.liked ? '♥' : '♡'}
          </button>
          <button className="play-btn large" onClick={togglePlay} disabled={!song}>
            {playing ? '❚❚' : '▶'}
          </button>
          <button className="icon-btn big" title="下一首" onClick={() => next(false)}>⏭</button>
        </div>
        <p className="muted small-text">FM 模式下没有上一首，遇到不喜欢的歌就丢进垃圾桶吧</p>
      </div>
    </div>
  )
}

export default function Fm() {
  return (
    <RequireLogin tip="登录后开启私人FM">
      <FmInner />
    </RequireLogin>
  )
}
