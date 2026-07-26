import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiSingers } from '../api'
import { Loading, Empty } from '../components/common'
import { singerAvatar, onSingerImgError } from '../utils'

const AREAS = ['全部', '内地', '欧美', '日韩', '其他']
const TYPES = [
  { v: '', label: '全部' },
  { v: 1, label: '男歌手' },
  { v: 2, label: '女歌手' },
  { v: 3, label: '组合' },
  { v: 4, label: '厂牌' },
]
const INITIALS = ['全部', ...'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')]

export default function Singers() {
  const navigate = useNavigate()
  const [area, setArea] = useState('全部')
  const [type, setType] = useState('')
  const [initial, setInitial] = useState('全部')
  const [page, setPage] = useState(1)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  useEffect(() => {
    let alive = true
    setLoading(true)
    setError(false)
    apiSingers({
      pageNum: page,
      pageSize: 30,
      area: area === '全部' ? undefined : area,
      type: type === '' ? undefined : type,
      initial: initial === '全部' ? undefined : initial,
    })
      .then((d) => alive && setData(d))
      .catch(() => alive && setError(true))
      .finally(() => alive && setLoading(false))
    return () => {
      alive = false
    }
  }, [area, type, initial, page])

  const list = data?.list || []
  return (
    <div className="page">
      <div className="page-head">
        <h1 className="page-title">歌手</h1>
        <div className="filter-rows">
          <div className="tag-row">
            <span className="filter-label">地区</span>
            {AREAS.map((a) => (
              <button key={a} className={`chip ${area === a ? 'active' : ''}`} onClick={() => { setArea(a); setPage(1) }}>{a}</button>
            ))}
          </div>
          <div className="tag-row">
            <span className="filter-label">类型</span>
            {TYPES.map((t) => (
              <button key={t.label} className={`chip ${type === t.v ? 'active' : ''}`} onClick={() => { setType(t.v); setPage(1) }}>{t.label}</button>
            ))}
          </div>
          <div className="tag-row">
            <span className="filter-label">字母</span>
            {INITIALS.map((c) => (
              <button key={c} className={`chip mini ${initial === c ? 'active' : ''}`} onClick={() => { setInitial(c); setPage(1) }}>{c}</button>
            ))}
          </div>
        </div>
      </div>

      {loading && <Loading />}
      {!loading && (error || list.length === 0) && <Empty text="没有找到相关歌手" />}
      <div className="singer-grid">
        {list.map((s) => (
          <div key={s.id} className="singer-card" onClick={() => navigate(`/singer/${s.id}`)}>
            <img
              className="singer-avatar"
              src={singerAvatar(s)}
              alt={s.name}
              loading="lazy"
              onError={(e) => onSingerImgError(e, s.id)}
            />
            <div className="singer-name">{s.name}</div>
            <div className="singer-meta muted">{s.songCount || 0} 首歌曲</div>
          </div>
        ))}
      </div>

      {data && data.pages > 1 && (
        <div className="pager">
          <button className="chip" disabled={page <= 1} onClick={() => setPage(page - 1)}>上一页</button>
          <span className="muted">{page} / {data.pages}</span>
          <button className="chip" disabled={page >= data.pages} onClick={() => setPage(page + 1)}>下一页</button>
        </div>
      )}
    </div>
  )
}
