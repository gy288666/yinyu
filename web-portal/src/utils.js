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
