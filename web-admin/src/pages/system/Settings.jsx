import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Empty, Form, Input, InputNumber, Switch, message } from 'antd'
import { ReloadOutlined, SaveOutlined } from '@ant-design/icons'
import request from '../../utils/request'

/** 已知设置项的展示配置；接口返回的其他键将以文本框兜底展示 */
const KNOWN_FIELDS = [
  { key: 'site.name', label: '站点名称', type: 'text' },
  { key: 'trial.seconds', label: '游客试听时长（秒）', type: 'number' },
  { key: 'download.quota.normal', label: '普通用户每日下载配额', type: 'number' },
  { key: 'download.quota.vip', label: 'VIP 每日下载配额', type: 'number' },
  { key: 'pay.mock', label: '开启模拟支付', type: 'boolean' },
  { key: 'minio.presign.expire', label: '播放地址有效期（秒）', type: 'number' },
]

export default function SettingsPage() {
  const [form] = Form.useForm()
  const [settings, setSettings] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const data = await request.get('/admin/settings')
      setSettings(data && typeof data === 'object' ? data : {})
      const initial = {}
      Object.entries(data || {}).forEach(([k, v]) => {
        const field = KNOWN_FIELDS.find((f) => f.key === k)
        if (field?.type === 'boolean') initial[k] = String(v) === 'true'
        else if (field?.type === 'number') initial[k] = Number(v)
        else initial[k] = v
      })
      form.setFieldsValue(initial)
    } catch {
      setSettings(null)
    } finally {
      setLoading(false)
    }
  }, [form])

  useEffect(() => {
    load()
  }, [load])

  const onSave = async () => {
    const values = form.getFieldsValue()
    const payload = {}
    Object.entries(values).forEach(([k, v]) => {
      payload[k] = String(v)
    })
    setSaving(true)
    try {
      await request.put('/admin/settings', payload)
      message.success('设置已保存并即时生效')
    } catch {
      // 提示已统一处理
    } finally {
      setSaving(false)
    }
  }

  const keys = settings ? Object.keys(settings) : []
  const extraKeys = keys.filter((k) => !KNOWN_FIELDS.some((f) => f.key === k))

  return (
    <Card
      variant="borderless"
      title="系统设置"
      loading={loading}
      extra={
        <>
          <Button icon={<ReloadOutlined />} onClick={load} style={{ marginRight: 8 }}>
            刷新
          </Button>
          <Button type="primary" icon={<SaveOutlined />} loading={saving} onClick={onSave} disabled={!settings}>
            保存设置
          </Button>
        </>
      }
    >
      {settings === null ? (
        <Empty description="设置加载失败，请检查后端服务后点击刷新重试" style={{ padding: '60px 0' }} />
      ) : (
        <Form form={form} layout="vertical" style={{ maxWidth: 480 }}>
          {KNOWN_FIELDS.filter((f) => f.key in (settings || {})).map((f) => (
            <Form.Item key={f.key} name={f.key} label={f.label} valuePropName={f.type === 'boolean' ? 'checked' : 'value'}>
              {f.type === 'boolean' ? (
                <Switch />
              ) : f.type === 'number' ? (
                <InputNumber min={0} style={{ width: '100%' }} />
              ) : (
                <Input maxLength={100} />
              )}
            </Form.Item>
          ))}
          {extraKeys.map((k) => (
            <Form.Item key={k} name={k} label={k}>
              <Input maxLength={200} />
            </Form.Item>
          ))}
          {!keys.length && <Empty description="暂无设置项" />}
        </Form>
      )}
    </Card>
  )
}
