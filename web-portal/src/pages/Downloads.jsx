import { useEffect, useState } from 'react'
import { apiDownloads } from '../api'
import { Loading, Empty } from '../components/common'
import RequireLogin from '../components/RequireLogin'
import { formatDuration } from '../utils'

function fmtSize(bytes) {
  if (!bytes) return '-'
  if (bytes >= 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  return Math.round(bytes / 1024) + ' KB'
}

function DownloadsInner() {
  const [data, setData] = useState(null)
  const [page, setPage] = useState(1)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  useEffect(() => {
    let alive = true
    setLoading(true)
    setError(false)
    apiDownloads({ pageNum: page, pageSize: 30 })
      .then((d) => alive && setData(d))
      .catch(() => alive && setError(true))
      .finally(() => alive && setLoading(false))
    return () => {
      alive = false
    }
  }, [page])

  const list = data?.list || []
  return (
    <div className="page">
      <div className="page-head">
        <h1 className="page-title">下载管理</h1>
        <p className="muted">普通用户每日 10 首，VIP 每日 100 首</p>
      </div>
      {loading && <Loading />}
      {!loading && error && <Empty text="加载失败，请稍后重试" />}
      {!loading && !error && list.length === 0 && <Empty text="还没有下载记录" />}
      {!loading && !error && list.length > 0 && (
        <div className="card song-table">
          {list.map((r, i) => (
            <div key={i} className="song-row">
              <span className="col-index">{String(i + 1).padStart(2, '0')}</span>
              <span className="col-name"><span className="song-name">{r.song?.name}</span></span>
              <span className="col-singer">{r.song?.singerName}</span>
              <span className="col-album">{fmtSize(r.fileSize)}</span>
              <span className="col-count">{r.downloadAt || ''}</span>
              <span className="col-duration">{formatDuration(r.song?.duration)}</span>
            </div>
          ))}
        </div>
      )}
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

export default function Downloads() {
  return (
    <RequireLogin tip="登录后查看下载记录">
      <DownloadsInner />
    </RequireLogin>
  )
}
