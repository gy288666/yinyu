import { useEffect, useState } from 'react'
import { apiRecent, apiClearRecent } from '../api'
import { Loading, Empty } from '../components/common'
import SongTable from '../components/SongTable'
import RequireLogin from '../components/RequireLogin'
import { usePlayerStore } from '../store/playerStore'
import { toast } from '../components/toast'

function RecentInner() {
  const [data, setData] = useState(null)
  const [page, setPage] = useState(1)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const { playList } = usePlayerStore.getState()

  const load = () => {
    setLoading(true)
    setError(false)
    apiRecent({ pageNum: page, pageSize: 50 })
      .then((d) => setData(d))
      .catch(() => setError(true))
      .finally(() => setLoading(false))
  }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(load, [page])

  const clear = async () => {
    if (!window.confirm('确定清空最近播放记录吗？')) return
    try {
      await apiClearRecent()
      toast.success('已清空最近播放')
      setPage(1)
      load()
    } catch (e) {
      /* 已 toast */
    }
  }

  const songs = (data?.list || []).map((r) => r.song || r).filter(Boolean)
  return (
    <div className="page">
      <div className="page-head with-action">
        <div>
          <h1 className="page-title">最近播放</h1>
          <p className="muted">最多保留 200 条</p>
        </div>
        <div className="btn-group">
          <button className="gold-btn" disabled={!songs.length} onClick={() => playList(songs, 0)}>▶ 播放全部</button>
          <button className="ghost-btn" disabled={!songs.length} onClick={clear}>清空</button>
        </div>
      </div>
      {loading && <Loading />}
      {!loading && error && <Empty text="加载失败，请稍后重试" />}
      {!loading && !error && (
        <div className="card">
          <SongTable songs={songs} showCover emptyText="还没有播放记录" />
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

export default function Recent() {
  return (
    <RequireLogin tip="登录后查看最近播放">
      <RecentInner />
    </RequireLogin>
  )
}
