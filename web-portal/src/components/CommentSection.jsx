import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  apiComments,
  apiPostComment,
  apiDeleteComment,
  apiLikeComment,
  apiUnlikeComment,
} from '../api'
import { useAuthStore } from '../store/authStore'
import { Loading, Empty } from './common'
import { toast } from './toast'

const SORTS = [
  { key: 'hot', label: '最热' },
  { key: 'latest', label: '最新' },
]

function Avatar({ nickname, avatar }) {
  if (avatar) return <img className="cmt-avatar" src={avatar} alt={nickname} onError={(e) => { e.currentTarget.style.display = 'none' }} />
  return <span className="cmt-avatar cmt-avatar-text">{(nickname || 'U').slice(0, 1)}</span>
}

// 通用评论区：targetType = SONG / PLAYLIST
export default function CommentSection({ targetType, targetId }) {
  const navigate = useNavigate()
  const logged = useAuthStore((s) => s.logged)
  const user = useAuthStore((s) => s.user)

  const [sort, setSort] = useState('hot')
  const [page, setPage] = useState(1)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const [content, setContent] = useState('')
  const [submitting, setSubmitting] = useState(false)
  // 回复态：{ parentId, toNickname }
  const [replying, setReplying] = useState(null)
  const [replyContent, setReplyContent] = useState('')
  // 已展开回复的一级评论 id 集合
  const [expanded, setExpanded] = useState(() => new Set())

  const load = useCallback(
    (p = page, s = sort) => {
      setLoading(true)
      setError(false)
      apiComments({ targetType, targetId, sort: s, pageNum: p, pageSize: 10 })
        .then((d) => setData(d))
        .catch(() => setError(true))
        .finally(() => setLoading(false))
    },
    [targetType, targetId, page, sort]
  )

  useEffect(() => {
    setPage(1)
    setSort('hot')
    setExpanded(new Set())
    setReplying(null)
  }, [targetType, targetId])

  useEffect(() => {
    load(page, sort)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [targetType, targetId, page, sort])

  const changeSort = (s) => {
    if (s === sort) return
    setSort(s)
    setPage(1)
  }

  const requireLogin = () => {
    if (logged) return true
    toast.warn('请先登录后参与评论')
    return false
  }

  const submit = async () => {
    if (!requireLogin()) return
    const text = content.trim()
    if (!text) return toast.warn('先写点什么吧')
    if (text.length > 500) return toast.warn('评论最多 500 字')
    setSubmitting(true)
    try {
      await apiPostComment({ targetType, targetId, content: text })
      toast.success('评论发表成功')
      setContent('')
      setSort('latest')
      setPage(1)
      load(1, 'latest')
    } catch (e) {
      /* 拦截器已 toast（含 40001 敏感词） */
    } finally {
      setSubmitting(false)
    }
  }

  const submitReply = async (parentId) => {
    if (!requireLogin()) return
    const text = replyContent.trim()
    if (!text) return toast.warn('先写点什么吧')
    if (text.length > 500) return toast.warn('回复最多 500 字')
    setSubmitting(true)
    try {
      await apiPostComment({ targetType, targetId, content: text, parentId })
      toast.success('回复成功')
      setReplying(null)
      setReplyContent('')
      setExpanded((old) => new Set(old).add(parentId))
      load(page, sort)
    } catch (e) {
      /* 拦截器已 toast */
    } finally {
      setSubmitting(false)
    }
  }

  const toggleCmtLike = async (c) => {
    if (!logged) return toast.warn('请先登录后点赞')
    try {
      const res = c.liked ? await apiUnlikeComment(c.id) : await apiLikeComment(c.id)
      const likeCount = res?.likeCount ?? Math.max(0, (c.likeCount || 0) + (c.liked ? -1 : 1))
      setData((d) => ({
        ...d,
        list: (d?.list || []).map((it) => (it.id === c.id ? { ...it, liked: !c.liked, likeCount } : it)),
      }))
    } catch (e) {
      /* 拦截器已 toast */
    }
  }

  const removeComment = async (c) => {
    if (!window.confirm('确定删除这条评论吗？')) return
    try {
      await apiDeleteComment(c.id)
      toast.success('评论已删除')
      load(page, sort)
    } catch (e) {
      /* 拦截器已 toast */
    }
  }

  const toggleExpand = (id) => {
    setExpanded((old) => {
      const next = new Set(old)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const startReply = (parentId, toNickname) => {
    if (!requireLogin()) return
    if (replying?.parentId === parentId && replying?.toNickname === toNickname) {
      setReplying(null)
      return
    }
    setReplying({ parentId, toNickname })
    setReplyContent('')
  }

  const list = data?.list || []
  const total = data?.total || 0
  const pages = data?.pages || 0

  return (
    <section className="comment-section">
      <div className="cmt-head">
        <h2 className="section-title">评论{total > 0 ? `（${total}）` : ''}</h2>
        <div className="tabs small">
          {SORTS.map((s) => (
            <button key={s.key} className={`tab ${sort === s.key ? 'active' : ''}`} onClick={() => changeSort(s.key)}>
              {s.label}
            </button>
          ))}
        </div>
      </div>

      {logged ? (
        <div className="cmt-editor">
          <Avatar nickname={user?.nickname} avatar={user?.avatar} />
          <div className="cmt-editor-main">
            <textarea
              value={content}
              maxLength={500}
              placeholder="说点什么吧，好的评论会被更多人看到…"
              onChange={(e) => setContent(e.target.value)}
            />
            <div className="cmt-editor-foot">
              <span className="muted">{content.length}/500</span>
              <button className="gold-btn small" disabled={submitting || !content.trim()} onClick={submit}>
                发表评论
              </button>
            </div>
          </div>
        </div>
      ) : (
        <div className="cmt-login-tip card">
          <span>登录后即可发表评论，与大家分享你的听后感</span>
          <button className="gold-btn small" onClick={() => navigate('/login')}>去登录</button>
        </div>
      )}

      {loading && <Loading text="评论加载中…" />}
      {!loading && error && <Empty text="评论加载失败，稍后再试" />}
      {!loading && !error && list.length === 0 && <Empty text="还没有评论，来抢沙发吧" />}

      {!loading && !error && list.length > 0 && (
        <div className="cmt-list">
          {list.map((c) => {
            const mine = logged && user?.userId != null && c.userId === user.userId
            const replies = c.replies || []
            const open = expanded.has(c.id)
            return (
              <div key={c.id} className="cmt-item">
                <Avatar nickname={c.nickname} avatar={c.avatar} />
                <div className="cmt-body">
                  <div className="cmt-user">
                    <span className="cmt-nick">{c.nickname}</span>
                    {c.vip && <span className="tag-vip">VIP</span>}
                    {c.level > 0 && <span className="cmt-level">Lv.{c.level}</span>}
                  </div>
                  <div className="cmt-content">{c.content}</div>
                  <div className="cmt-foot">
                    <span className="cmt-time">{c.createTime || ''}</span>
                    <span className={`cmt-op ${c.liked ? 'gold' : ''}`} onClick={() => toggleCmtLike(c)}>
                      {c.liked ? '♥' : '♡'} {c.likeCount > 0 ? c.likeCount : '赞'}
                    </span>
                    <span className="cmt-op" onClick={() => startReply(c.id, c.nickname)}>回复</span>
                    {mine && <span className="cmt-op danger" onClick={() => removeComment(c)}>删除</span>}
                  </div>

                  {replying?.parentId === c.id && (
                    <div className="cmt-reply-editor">
                      <textarea
                        value={replyContent}
                        maxLength={500}
                        placeholder={`回复 @${replying.toNickname}：`}
                        onChange={(e) => setReplyContent(e.target.value)}
                        autoFocus
                      />
                      <div className="cmt-editor-foot">
                        <button className="text-btn" onClick={() => setReplying(null)}>取消</button>
                        <button className="gold-btn small" disabled={submitting || !replyContent.trim()} onClick={() => submitReply(c.id)}>
                          回复
                        </button>
                      </div>
                    </div>
                  )}

                  {replies.length > 0 && (
                    <>
                      <button className="cmt-expand text-btn" onClick={() => toggleExpand(c.id)}>
                        {open ? '收起回复 ▴' : `展开 ${replies.length} 条回复 ▾`}
                      </button>
                      {open && (
                        <div className="cmt-replies">
                          {replies.map((r) => (
                            <div key={r.id} className="cmt-reply">
                              <span className="cmt-nick small">{r.nickname}</span>
                              {r.replyTo && <span className="muted"> 回复 @{r.replyTo}</span>}
                              <span className="muted">：</span>
                              <span className="cmt-reply-text">{r.content}</span>
                              <div className="cmt-foot">
                                <span className="cmt-time">{r.createTime || ''}</span>
                                <span className="cmt-op" onClick={() => startReply(c.id, r.nickname)}>回复</span>
                                {logged && user?.userId != null && r.userId === user.userId && (
                                  <span className="cmt-op danger" onClick={() => removeComment(r)}>删除</span>
                                )}
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}

      {pages > 1 && (
        <div className="pager">
          <button className="chip" disabled={page <= 1} onClick={() => setPage(page - 1)}>上一页</button>
          <span className="muted">{page} / {pages}</span>
          <button className="chip" disabled={page >= pages} onClick={() => setPage(page + 1)}>下一页</button>
        </div>
      )}
    </section>
  )
}
