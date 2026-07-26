import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { apiMyPlaylists, apiCreatePlaylist, apiUpdatePlaylist, apiDeletePlaylist } from '../api'
import { Loading, Empty } from '../components/common'
import RequireLogin from '../components/RequireLogin'
import { toast } from '../components/toast'
import { formatCount } from '../utils'

function PlaylistModal({ initial, onClose, onSaved }) {
  const [form, setForm] = useState({
    title: initial?.title || '',
    intro: initial?.intro || '',
    visibility: initial?.visibility || 'PUBLIC',
  })
  const [saving, setSaving] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    if (!form.title.trim()) return toast.warn('请输入歌单名称')
    setSaving(true)
    try {
      if (initial?.id) {
        await apiUpdatePlaylist(initial.id, form)
        toast.success('歌单已更新')
      } else {
        await apiCreatePlaylist(form)
        toast.success('歌单创建成功')
      }
      onSaved()
    } catch (err) {
      /* 已 toast */
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="modal-mask" onClick={onClose}>
      <div className="modal card" onClick={(e) => e.stopPropagation()}>
        <h2 className="modal-title">{initial?.id ? '编辑歌单' : '创建歌单'}</h2>
        <form onSubmit={submit} className="auth-form">
          <label className="field">
            <span>名称</span>
            <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} placeholder="给歌单起个名字" maxLength={40} />
          </label>
          <label className="field">
            <span>简介</span>
            <textarea value={form.intro} onChange={(e) => setForm({ ...form, intro: e.target.value })} rows={3} placeholder="介绍一下这个歌单（可选）" />
          </label>
          <label className="field">
            <span>可见性</span>
            <div className="tabs small">
              <button type="button" className={`tab ${form.visibility === 'PUBLIC' ? 'active' : ''}`} onClick={() => setForm({ ...form, visibility: 'PUBLIC' })}>公开</button>
              <button type="button" className={`tab ${form.visibility === 'PRIVATE' ? 'active' : ''}`} onClick={() => setForm({ ...form, visibility: 'PRIVATE' })}>私密</button>
            </div>
          </label>
          <div className="btn-group right">
            <button type="button" className="ghost-btn" onClick={onClose}>取消</button>
            <button className="gold-btn" type="submit" disabled={saving}>{saving ? '保存中…' : '保存'}</button>
          </div>
        </form>
      </div>
    </div>
  )
}

function MyPlaylistsInner() {
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const [list, setList] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [editing, setEditing] = useState(null) // null 关闭 / {} 新建 / 对象 编辑

  const load = () => {
    setLoading(true)
    setError(false)
    apiMyPlaylists()
      .then((d) => setList(d || []))
      .catch(() => setError(true))
      .finally(() => setLoading(false))
  }
  useEffect(load, [])

  useEffect(() => {
    if (params.get('create') === '1') {
      setEditing({})
      setParams({}, { replace: true })
    }
  }, [params, setParams])

  const del = async (p) => {
    if (!window.confirm(`确定删除歌单「${p.title}」吗？`)) return
    try {
      await apiDeletePlaylist(p.id)
      toast.success('歌单已删除')
      load()
    } catch (e) {
      /* 已 toast */
    }
  }

  return (
    <div className="page">
      <div className="page-head with-action">
        <h1 className="page-title">我的歌单</h1>
        <button className="gold-btn" onClick={() => setEditing({})}>＋ 创建歌单</button>
      </div>
      {loading && <Loading />}
      {!loading && error && <Empty text="加载失败，请稍后重试" />}
      {!loading && !error && (list || []).length === 0 && <Empty text="还没有歌单，点击右上角创建一个吧" />}
      <div className="my-pl-list">
        {(list || []).map((p) => (
          <div key={p.id} className="my-pl-row card" onClick={() => navigate(`/playlist/${p.id}`)}>
            <img
              className="hot-cover"
              src={p.cover || '/static/img/singer/wang-sulong.jpg'}
              alt=""
              onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = '/static/img/singer/wang-sulong.jpg' }}
            />
            <div className="hot-info">
              <div>
                {p.builtin && <span className="gold">♥ </span>}
                {p.title}
                {p.visibility === 'PRIVATE' && <span className="tag-pay">私密</span>}
              </div>
              <div className="hot-meta">{p.songCount || 0} 首 · ▶ {formatCount(p.playCount)}</div>
            </div>
            {!p.builtin && (
              <div className="btn-group" onClick={(e) => e.stopPropagation()}>
                <button className="ghost-btn small" onClick={() => setEditing(p)}>编辑</button>
                <button className="ghost-btn small" onClick={() => del(p)}>删除</button>
              </div>
            )}
          </div>
        ))}
      </div>
      {editing !== null && (
        <PlaylistModal
          initial={editing.id ? editing : null}
          onClose={() => setEditing(null)}
          onSaved={() => { setEditing(null); load() }}
        />
      )}
    </div>
  )
}

export default function MyPlaylists() {
  return (
    <RequireLogin tip="登录后管理我的歌单">
      <MyPlaylistsInner />
    </RequireLogin>
  )
}
