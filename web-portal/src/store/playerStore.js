import { create } from 'zustand'
import { apiSongUrl, apiLikeSong, apiUnlikeSong, apiFmNext } from '../api'
import { toast } from '../components/toast'
import { getToken } from '../api/client'

const STORAGE_KEY = 'yinyu_player_state'

// 全局唯一 audio 元素（由 App 层 GlobalAudio 组件注册）
let audioEl = null
export function registerAudio(el) {
  audioEl = el
  const st = usePlayerStore.getState()
  if (el) el.volume = st.volume
}
export function getAudio() {
  return audioEl
}

function loadPersisted() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return {}
    const s = JSON.parse(raw)
    return {
      queue: Array.isArray(s.queue) ? s.queue : [],
      index: typeof s.index === 'number' ? s.index : -1,
      mode: ['order', 'random', 'loop'].includes(s.mode) ? s.mode : 'order',
      volume: typeof s.volume === 'number' ? Math.min(1, Math.max(0, s.volume)) : 0.8,
      pendingSeek: typeof s.time === 'number' ? s.time : 0,
    }
  } catch (e) {
    return {}
  }
}

function persist(state) {
  try {
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        queue: state.queue,
        index: state.index,
        mode: state.mode,
        volume: state.volume,
        time: state.currentTime,
      })
    )
  } catch (e) {
    /* 存储失败忽略 */
  }
}

const persisted = loadPersisted()

export const usePlayerStore = create((set, get) => ({
  queue: persisted.queue || [],
  index: persisted.index ?? -1,
  playing: false,
  mode: persisted.mode || 'order', // order 顺序 / random 随机 / loop 单曲循环
  currentTime: 0,
  duration: 0,
  volume: persisted.volume ?? 0.8,
  trialSeconds: 0, // >0 表示试听
  trialNotified: false,
  queueOpen: false,
  fmMode: false,
  loadingUrl: false,
  pendingSeek: persisted.pendingSeek || 0, // 恢复会话时待跳转的进度
  loadedSongId: null, // 当前 audio 已加载 URL 的歌曲

  currentSong() {
    const { queue, index } = get()
    return index >= 0 && index < queue.length ? queue[index] : null
  },

  // ---- 队列操作 ----
  playList(songs, startIndex = 0, fm = false) {
    if (!songs || songs.length === 0) {
      toast.warn('暂无可播放的歌曲')
      return
    }
    set({ queue: [...songs], index: startIndex, fmMode: fm, pendingSeek: 0 })
    get()._loadAndPlay(songs[startIndex])
    persist(get())
  },

  playSong(song) {
    const { queue } = get()
    const idx = queue.findIndex((s) => s.id === song.id)
    if (idx >= 0) {
      set({ index: idx, fmMode: false, pendingSeek: 0 })
      get()._loadAndPlay(queue[idx])
    } else {
      const newQueue = [...queue, song]
      set({ queue: newQueue, index: newQueue.length - 1, fmMode: false, pendingSeek: 0 })
      get()._loadAndPlay(song)
    }
    persist(get())
  },

  addToQueue(song) {
    const { queue } = get()
    if (queue.some((s) => s.id === song.id)) {
      toast('已在播放队列中')
      return
    }
    set({ queue: [...queue, song] })
    toast.success('已添加到播放队列')
    persist(get())
  },

  removeFromQueue(i) {
    const { queue, index } = get()
    const newQueue = queue.filter((_, k) => k !== i)
    let newIndex = index
    if (i < index) newIndex = index - 1
    else if (i === index) newIndex = Math.min(index, newQueue.length - 1)
    set({ queue: newQueue, index: newIndex })
    if (i === index) {
      if (newQueue.length > 0 && newIndex >= 0) get()._loadAndPlay(newQueue[newIndex])
      else get()._stop()
    }
    persist(get())
  },

  clearQueue() {
    get()._stop()
    set({ queue: [], index: -1, queueOpen: false })
    persist(get())
  },

  toggleQueue() {
    set((s) => ({ queueOpen: !s.queueOpen }))
  },

  // ---- 播放控制 ----
  async togglePlay() {
    const st = get()
    const song = st.currentSong()
    if (!song) return
    if (!audioEl) return
    if (st.playing) {
      audioEl.pause()
    } else if (st.loadedSongId === song.id && audioEl.src) {
      audioEl.play().catch(() => {})
    } else {
      // 恢复会话后首次播放：重新取 URL 并 seek 到上次进度
      await st._loadAndPlay(song, st.pendingSeek)
    }
  },

  prev() {
    const { queue, index, mode, fmMode } = get()
    if (fmMode || queue.length === 0) return
    let ni
    if (mode === 'random') ni = Math.floor(Math.random() * queue.length)
    else ni = (index - 1 + queue.length) % queue.length
    set({ index: ni, pendingSeek: 0 })
    get()._loadAndPlay(queue[ni])
    persist(get())
  },

  async next(auto = false) {
    const { queue, index, mode, fmMode } = get()
    if (fmMode) {
      // 私人FM：拉取下一曲追加播放
      try {
        const song = await apiFmNext()
        if (song) {
          const nq = [...queue, song]
          set({ queue: nq, index: nq.length - 1, pendingSeek: 0 })
          get()._loadAndPlay(song)
          persist(get())
        }
      } catch (e) {
        /* 接口已 toast */
      }
      return
    }
    if (queue.length === 0) return
    let ni
    if (mode === 'random' && queue.length > 1) {
      do {
        ni = Math.floor(Math.random() * queue.length)
      } while (ni === index)
    } else if (auto && mode === 'loop') {
      ni = index
    } else {
      ni = (index + 1) % queue.length
    }
    set({ index: ni, pendingSeek: 0 })
    get()._loadAndPlay(queue[ni])
    persist(get())
  },

  onEnded() {
    const { mode, fmMode } = get()
    if (!fmMode && mode === 'loop') {
      if (audioEl) {
        audioEl.currentTime = 0
        audioEl.play().catch(() => {})
      }
      return
    }
    get().next(true)
  },

  cycleMode() {
    const order = ['order', 'random', 'loop']
    const { mode } = get()
    const nm = order[(order.indexOf(mode) + 1) % order.length]
    set({ mode: nm })
    const label = { order: '顺序播放', random: '随机播放', loop: '单曲循环' }[nm]
    toast(label)
    persist(get())
  },

  seek(t) {
    if (audioEl && !Number.isNaN(t)) {
      audioEl.currentTime = t
      set({ currentTime: t })
    }
  },

  setVolume(v) {
    const vol = Math.min(1, Math.max(0, v))
    if (audioEl) audioEl.volume = vol
    set({ volume: vol })
    persist(get())
  },

  // ---- 喜欢 ----
  async toggleLike(song) {
    if (!getToken()) {
      toast.warn('请先登录后再喜欢歌曲')
      return null
    }
    try {
      let liked
      if (song.liked) {
        await apiUnlikeSong(song.id)
        liked = false
      } else {
        await apiLikeSong(song.id)
        liked = true
        toast.success('已添加到我喜欢的音乐')
      }
      set((s) => ({
        queue: s.queue.map((q) => (q.id === song.id ? { ...q, liked } : q)),
      }))
      return liked
    } catch (e) {
      return null
    }
  },

  // ---- 内部 ----
  async _loadAndPlay(song, seekTo = 0) {
    if (!song || !audioEl) return
    set({ loadingUrl: true, trialNotified: false, currentTime: seekTo || 0, duration: song.duration || 0 })
    try {
      const data = await apiSongUrl(song.id)
      // 播放期间用户可能已切歌
      if (get().currentSong()?.id !== song.id) return
      audioEl.src = data.url
      set({ trialSeconds: data.trialSeconds || 0, loadedSongId: song.id })
      if (seekTo > 0) {
        const onMeta = () => {
          audioEl.currentTime = seekTo
          audioEl.removeEventListener('loadedmetadata', onMeta)
        }
        audioEl.addEventListener('loadedmetadata', onMeta)
      }
      await audioEl.play().catch(() => {
        /* 浏览器自动播放策略拦截时静默 */
      })
      if ((data.trialSeconds || 0) > 0) {
        toast.warn(`当前为试听模式（${data.trialSeconds} 秒），登录后可完整播放`)
      }
    } catch (e) {
      // 展示接口返回的权益提示：30001 VIP / 30002 付费 / 20003 下架
      toast.error(e.message || '获取播放地址失败')
      set({ playing: false })
    } finally {
      set({ loadingUrl: false })
    }
  },

  _stop() {
    if (audioEl) {
      audioEl.pause()
      audioEl.removeAttribute('src')
    }
    set({ playing: false, currentTime: 0, duration: 0, loadedSongId: null })
  },

  // GlobalAudio 事件回调
  _onPlay: () => set({ playing: true }),
  _onPause: () => set({ playing: false }),
  _onTimeUpdate() {
    if (!audioEl) return
    const st = get()
    const t = audioEl.currentTime
    set({ currentTime: t, duration: audioEl.duration || st.currentSong()?.duration || 0 })
    // 试听截断
    if (st.trialSeconds > 0 && t >= st.trialSeconds && !st.trialNotified) {
      audioEl.pause()
      set({ trialNotified: true })
      toast.warn('试听结束，登录后可继续畅听完整歌曲')
    }
    // 节流持久化进度（约每 5 秒）
    if (Math.floor(t) % 5 === 0 && Math.floor(t) !== Math.floor(st._lastPersistT || -1)) {
      set({ _lastPersistT: t })
      persist(get())
    }
  },
}))

export function formatTime(sec) {
  if (!sec || Number.isNaN(sec)) return '00:00'
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}
