import { useState } from 'react'
import { Upload, message } from 'antd'
import { LoadingOutlined, PlusOutlined } from '@ant-design/icons'
import request from '../utils/request'

/**
 * 图片上传（走 POST /api/admin/upload/image，可通过 action 覆盖）
 * 受控组件：value 为图片 URL 字符串，onChange(url)
 */
export default function UploadImage({ value, onChange, action = '/admin/upload/image', tip = '上传图片' }) {
  const [loading, setLoading] = useState(false)

  const customRequest = async ({ file, onSuccess, onError }) => {
    const formData = new FormData()
    formData.append('file', file)
    setLoading(true)
    try {
      const data = await request.post(action, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      const url = data?.url || data
      onChange?.(typeof url === 'string' ? url : '')
      onSuccess?.(data)
    } catch (e) {
      onError?.(e)
    } finally {
      setLoading(false)
    }
  }

  const beforeUpload = (file) => {
    const okType = ['image/jpeg', 'image/png', 'image/webp'].includes(file.type)
    if (!okType) {
      message.error('仅支持 jpg / png / webp 图片')
      return Upload.LIST_IGNORE
    }
    if (file.size / 1024 / 1024 > 5) {
      message.error('图片大小不能超过 5MB')
      return Upload.LIST_IGNORE
    }
    return true
  }

  return (
    <Upload
      listType="picture-card"
      showUploadList={false}
      customRequest={customRequest}
      beforeUpload={beforeUpload}
      accept="image/*"
    >
      {value ? (
        <img src={value} alt="cover" style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: 6 }} />
      ) : (
        <div style={{ color: '#888' }}>
          {loading ? <LoadingOutlined /> : <PlusOutlined />}
          <div style={{ marginTop: 6, fontSize: 12 }}>{tip}</div>
        </div>
      )}
    </Upload>
  )
}
