import { useEffect, useState } from 'react'
import { apiRadios, apiRadioNext } from '../api'
import { Loading, Empty } from '../components/common'
import { usePlayerStore } from '../store/playerStore'
import { toast } from '../components/toast'

export default function Radios() {
  const [radios, setRadios] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [starting, setStarting] = useState(null)
  const { playSong } = usePlayerStore.getState()

  useEffect(() => {
    apiRadios()
      .then((d) => setRadios(d || []))
      .catch(() => setError(true))
      .finally(() => setLoading(false))
  }, [])

  const play = async (radio) => {
    setStarting(radio.id)
    try {
      const song = await apiRadioNext(radio.id)
      if (song) {
        playSong(song)
        toast.success(`开始播放「${radio.name}」`)
      } else {
        toast.warn('这个电台暂时没有节目')
      }
    } catch (e) {
      /* 已 toast */
    } finally {
      setStarting(null)
    }
  }

  return (
    <div className="page">
      <div className="page-head">
        <h1 className="page-title">电台</h1>
        <p className="muted">主题连续播放，遇见随机的美好</p>
      </div>
      {loading && <Loading />}
      {!loading && (error || !(radios || []).length) && <Empty text="暂无电台，敬请期待" />}
      <div className="radio-grid">
        {(radios || []).map((r) => (
          <div key={r.id} className="radio-card card" onClick={() => play(r)}>
            <div className="pl-cover-wrap">
              <img
                className="pl-cover"
                src={r.cover || '/static/img/singer/zhou-jielun.jpg'}
                alt={r.name}
                loading="lazy"
                onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = '/static/img/singer/zhou-jielun.jpg' }}
              />
              <button className="pl-play-btn visible" title="播放">
                {starting === r.id ? '…' : '▶'}
              </button>
            </div>
            <div className="pl-title">📻 {r.name}</div>
            <div className="muted small-text">{r.desc || ''}</div>
          </div>
        ))}
      </div>
    </div>
  )
}
