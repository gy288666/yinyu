import { useEffect, useRef } from 'react'
import { registerAudio, usePlayerStore } from '../store/playerStore'

// 全局唯一 audio 元素，挂在 App 层，路由切换不卸载
export default function GlobalAudio() {
  const ref = useRef(null)

  useEffect(() => {
    const el = ref.current
    registerAudio(el)
    const st = usePlayerStore.getState
    const onPlay = () => st()._onPlay()
    const onPause = () => st()._onPause()
    const onTime = () => st()._onTimeUpdate()
    const onEnded = () => st().onEnded()
    const onError = () => {
      if (el.src) usePlayerStore.setState({ playing: false })
    }
    el.addEventListener('play', onPlay)
    el.addEventListener('pause', onPause)
    el.addEventListener('timeupdate', onTime)
    el.addEventListener('ended', onEnded)
    el.addEventListener('error', onError)
    return () => {
      el.removeEventListener('play', onPlay)
      el.removeEventListener('pause', onPause)
      el.removeEventListener('timeupdate', onTime)
      el.removeEventListener('ended', onEnded)
      el.removeEventListener('error', onError)
      registerAudio(null)
    }
  }, [])

  return <audio ref={ref} preload="auto" />
}
