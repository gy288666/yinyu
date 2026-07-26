// 轻量级 toast：模块级事件订阅，配合 ToastContainer 展示
let listeners = []
let seq = 0

export function subscribeToast(fn) {
  listeners.push(fn)
  return () => {
    listeners = listeners.filter((l) => l !== fn)
  }
}

export function toast(message, type = 'info', duration = 2600) {
  if (!message) return
  const item = { id: ++seq, message: String(message), type, duration }
  listeners.forEach((fn) => fn(item))
}

toast.success = (msg) => toast(msg, 'success')
toast.error = (msg) => toast(msg, 'error')
toast.warn = (msg) => toast(msg, 'warn')
