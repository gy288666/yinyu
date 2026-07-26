import { useEffect, useState } from 'react'
import { subscribeToast } from './toast'

export default function ToastContainer() {
  const [items, setItems] = useState([])

  useEffect(() => {
    return subscribeToast((item) => {
      setItems((prev) => [...prev, item])
      setTimeout(() => {
        setItems((prev) => prev.filter((i) => i.id !== item.id))
      }, item.duration)
    })
  }, [])

  return (
    <div className="toast-wrap">
      {items.map((i) => (
        <div key={i.id} className={`toast toast-${i.type}`}>
          {i.message}
        </div>
      ))}
    </div>
  )
}
