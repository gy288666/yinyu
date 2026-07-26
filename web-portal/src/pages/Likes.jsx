import { useEffect, useState } from 'react'
import { apiLikedSongs } from '../api'
import { Loading, Empty } from '../components/common'
import SongTable from '../components/SongTable'
import RequireLogin from '../components/RequireLogin'
import { usePlayerStore } from '../store/playerStore'
import { useAuthStore } from '../store/authStore'

function LikesInner() {
  const [data, setData] = useState(null)
  const [page, setPage] = useState(1)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const { playList } = usePlayerStore.getState()
  const logged = useAuthStore((s) => s.logged)

  useEffect(() => {
    if (!logged) return
    let alive = true
    setLoading(true)
    setError(false)
    apiLikedSongs({ pageNum: page, pageSize: 50 })
      .then((d) => {
        if (!alive) return
        setData({ ...d, list: (d?.list || []).map((s) => ({ ...s, liked: true })) })
      })
      .catch(() => alive && setError(true))
      .finally(() => alive && setLoading(false))
    return () => {
      alive = false
    }
  }, [page, logged])

  const list = data?.list || []
  return (
    <div className="page">
      <div className="page-head with-action">
        <div>
          <h1 className="page-title"><span className="gold">♥</span> 我喜欢的音乐</h1>
          <p className="muted">共 {data?.total ?? 0} 首</p>
        </div>
        <button className="gold-btn" disabled={!list.length} onClick={() => playList(list, 0)}>▶ 播放全部</button>
      </div>
      {loading && <Loading />}
      {!loading && error && <Empty text="加载失败，请稍后重试" />}
      {!loading && !error && (
        <div className="card">
          <SongTable songs={list} showCover emptyText="还没有喜欢的歌曲，快去发现页逛逛吧" />
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

export default function Likes() {
  return (
    <RequireLogin tip="登录后查看我喜欢的音乐">
      <LikesInner />
    </RequireLogin>
  )
}
