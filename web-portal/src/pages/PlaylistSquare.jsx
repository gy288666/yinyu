import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { apiPlaylists, apiCategories, apiSongs, apiPlaylistDetail } from '../api'
import { PlaylistCard, Loading, Empty, SectionHeader } from '../components/common'
import SongTable from '../components/SongTable'
import { usePlayerStore } from '../store/playerStore'

export default function PlaylistSquare() {
  const [params] = useSearchParams()
  const hires = params.get('hires') === '1'
  const [tags, setTags] = useState([])
  const [tag, setTag] = useState('')
  const [sort, setSort] = useState('hot')
  const [page, setPage] = useState(1)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [hiresSongs, setHiresSongs] = useState(null)
  const { playList } = usePlayerStore.getState()

  useEffect(() => {
    apiCategories()
      .then((tree) => {
        const leaves = []
        ;(tree || []).forEach((c) => (c.children || []).forEach((ch) => leaves.push(ch.name)))
        setTags(leaves)
      })
      .catch(() => {})
  }, [])

  useEffect(() => {
    if (hires) {
      setLoading(true)
      apiSongs({ quality: 'lossless', pageNum: page, pageSize: 30 })
        .then((d) => setHiresSongs(d))
        .catch(() => setError(true))
        .finally(() => setLoading(false))
      return
    }
    let alive = true
    setLoading(true)
    setError(false)
    apiPlaylists({ pageNum: page, pageSize: 24, tag: tag || undefined, sort })
      .then((d) => alive && setData(d))
      .catch(() => alive && setError(true))
      .finally(() => alive && setLoading(false))
    return () => {
      alive = false
    }
  }, [tag, sort, page, hires])

  const playPlaylist = async (pl) => {
    try {
      const detail = await apiPlaylistDetail(pl.id)
      if (detail?.songs?.length) playList(detail.songs, 0)
    } catch (e) {
      /* 已 toast */
    }
  }

  if (hires) {
    return (
      <div className="page">
        <div className="page-head">
          <h1 className="page-title">Hi-Res 专区 <span className="tag-sq">无损</span></h1>
          <p className="muted">flac / wav 高解析音质，VIP 专享完整播放</p>
        </div>
        {loading && <Loading />}
        {!loading && (error || !(hiresSongs?.list || []).length) && <Empty text="暂无无损曲目" />}
        {!loading && (hiresSongs?.list || []).length > 0 && (
          <div className="card">
            <SongTable songs={hiresSongs.list} showCover />
          </div>
        )}
      </div>
    )
  }

  const list = data?.list || []
  return (
    <div className="page">
      <div className="page-head">
        <h1 className="page-title">歌单广场</h1>
        <div className="filter-row">
          <div className="tabs small">
            <button className={`tab ${sort === 'hot' ? 'active' : ''}`} onClick={() => { setSort('hot'); setPage(1) }}>最热</button>
            <button className={`tab ${sort === 'latest' ? 'active' : ''}`} onClick={() => { setSort('latest'); setPage(1) }}>最新</button>
          </div>
          <div className="tag-row">
            <button className={`chip ${!tag ? 'active' : ''}`} onClick={() => { setTag(''); setPage(1) }}>全部</button>
            {tags.map((t) => (
              <button key={t} className={`chip ${tag === t ? 'active' : ''}`} onClick={() => { setTag(t); setPage(1) }}>
                {t}
              </button>
            ))}
          </div>
        </div>
      </div>

      {loading && <Loading />}
      {!loading && (error || list.length === 0) && <Empty text="没有找到相关歌单" />}
      <div className="pl-grid wide">
        {list.map((p) => (
          <PlaylistCard key={p.id} playlist={p} onPlay={playPlaylist} />
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
