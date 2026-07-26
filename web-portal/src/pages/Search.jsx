import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { apiSearch, apiHotKeywords } from '../api'
import { Loading, Empty } from '../components/common'
import SongTable from '../components/SongTable'
import { singerAvatar, onSingerImgError } from '../utils'

const TABS = [
  { v: 'song', label: '歌曲' },
  { v: 'singer', label: '歌手' },
  { v: 'album', label: '专辑' },
]

export default function Search() {
  const [params, setParams] = useSearchParams()
  const navigate = useNavigate()
  const keyword = params.get('keyword') || ''
  const type = TABS.some((t) => t.v === params.get('type')) ? params.get('type') : 'song'
  const page = Number(params.get('page')) || 1
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(false)
  const [hotWords, setHotWords] = useState([])

  useEffect(() => {
    apiHotKeywords(10).then((d) => setHotWords(d || [])).catch(() => {})
  }, [])

  useEffect(() => {
    if (!keyword) return
    let alive = true
    setLoading(true)
    setError(false)
    apiSearch({ keyword, type, pageNum: page, pageSize: 30 })
      .then((d) => alive && setData(d))
      .catch(() => alive && setError(true))
      .finally(() => alive && setLoading(false))
    return () => {
      alive = false
    }
  }, [keyword, type, page])

  const switchTab = (t) => setParams({ keyword, type: t })
  const list = data?.list || []

  return (
    <div className="page">
      <div className="page-head">
        <h1 className="page-title">
          {keyword ? <>搜索「<span className="gold">{keyword}</span>」</> : '搜索'}
        </h1>
        {!keyword && hotWords.length > 0 && (
          <div className="tag-row">
            <span className="filter-label">热搜</span>
            {hotWords.map((w) => (
              <button key={w} className="chip" onClick={() => setParams({ keyword: w, type })}>{w}</button>
            ))}
          </div>
        )}
        {keyword && (
          <div className="tabs">
            {TABS.map((t) => (
              <button key={t.v} className={`tab ${type === t.v ? 'active' : ''}`} onClick={() => switchTab(t.v)}>
                {t.label}
              </button>
            ))}
          </div>
        )}
      </div>

      {!keyword && <Empty text="输入关键字，发现好音乐" />}
      {keyword && loading && <Loading />}
      {keyword && !loading && (error || list.length === 0) && <Empty text="没有找到相关结果" />}

      {keyword && !loading && !error && list.length > 0 && (
        <>
          {type === 'song' && (
            <div className="card">
              <SongTable songs={list} showCover />
            </div>
          )}
          {type === 'singer' && (
            <div className="singer-grid">
              {list.map((s) => (
                <div key={s.id} className="singer-card" onClick={() => navigate(`/singer/${s.id}`)}>
                  <img className="singer-avatar" src={singerAvatar(s)} alt={s.name} onError={(e) => onSingerImgError(e, s.id)} />
                  <div className="singer-name">{s.name}</div>
                  <div className="singer-meta muted">{s.songCount || 0} 首歌曲</div>
                </div>
              ))}
            </div>
          )}
          {type === 'album' && (
            <div className="pl-grid wide">
              {list.map((a) => (
                <div key={a.id} className="pl-card" onClick={() => navigate(`/album/${a.id}`)}>
                  <div className="pl-cover-wrap">
                    <img
                      className="pl-cover"
                      src={a.cover || '/static/img/singer/wang-lihong.jpg'}
                      alt={a.name}
                      onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = '/static/img/singer/wang-lihong.jpg' }}
                    />
                  </div>
                  <div className="pl-title">{a.name}</div>
                  <div className="muted small-text">{a.singerName} · {a.publishDate || ''}</div>
                </div>
              ))}
            </div>
          )}
          {data.pages > 1 && (
            <div className="pager">
              <button className="chip" disabled={page <= 1} onClick={() => setParams({ keyword, type, page: page - 1 })}>上一页</button>
              <span className="muted">{page} / {data.pages}</span>
              <button className="chip" disabled={page >= data.pages} onClick={() => setParams({ keyword, type, page: page + 1 })}>下一页</button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
