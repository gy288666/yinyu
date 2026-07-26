import { apiDownloadUrl } from './api'
import { getToken } from './api/client'
import { toast } from './components/toast'

// 歌手头像兜底：使用本地 /static/img/singer/ 下的图片
const SINGER_IMGS = [
  'chen-yixun.jpg',
  'deng-ziqi.jpg',
  'li-ronghao.jpg',
  'lin-junjie.jpg',
  'mao-buyi.jpg',
  'shan-yichun.jpg',
  'wang-lihong.jpg',
  'wang-sulong.jpg',
  'xu-song.jpg',
  'xue-zhiqian.jpg',
  'zhang-bichen.jpg',
  'zhou-jielun.jpg',
]

export function singerFallback(id) {
  const n = Math.abs(Number(id) || 0) % SINGER_IMGS.length
  return `/static/img/singer/${SINGER_IMGS[n]}`
}

export function singerAvatar(singer) {
  return singer?.avatar || singerFallback(singer?.id)
}

// 图片加载失败兜底处理
export function onSingerImgError(e, id) {
  e.currentTarget.onerror = null
  e.currentTarget.src = singerFallback(id)
}

export function formatCount(n) {
  const num = Number(n) || 0
  if (num >= 100000000) return (num / 100000000).toFixed(1) + '亿'
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  return String(num)
}

export function formatDuration(sec) {
  if (!sec) return '--:--'
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

// 下载歌曲：签发附件式预签名 URL 后触发浏览器下载
// 权益不足（30001 VIP / 30002 未购 / 30003 超每日配额）时由 axios 拦截器 toast 接口提示
export async function downloadSong(song) {
  if (!song?.id) return
  if (!getToken()) {
    toast.warn('登录后可下载歌曲')
    return
  }
  try {
    const data = await apiDownloadUrl(song.id)
    if (!data?.url) {
      toast.error('下载地址获取失败')
      return
    }
    const a = document.createElement('a')
    a.href = data.url
    a.download = `${song.name || 'song'}${song.singerName ? ` - ${song.singerName}` : ''}.mp3`
    document.body.appendChild(a)
    a.click()
    a.remove()
    toast.success(data.quotaLeft != null ? `开始下载「${song.name}」，今日剩余 ${data.quotaLeft} 首` : `开始下载「${song.name}」`)
  } catch (e) {
    /* 拦截器已 toast 权益/配额提示 */
  }
}
