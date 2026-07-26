import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Empty, InputNumber, Table, Tag, message } from 'antd'
import { ReloadOutlined, SaveOutlined } from '@ant-design/icons'
import request from '../../utils/request'

/** 用户等级规则：Lv1-Lv10 阈值（累计播放分钟数）可编辑保存 */
export default function UserLevelPage() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [loadError, setLoadError] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setLoadError(false)
    try {
      const data = await request.get('/admin/levels')
      const list = Array.isArray(data) ? data : data?.list || []
      setRows(list.map((r, i) => ({ level: r.level ?? i + 1, minPlayMinutes: r.minPlayMinutes ?? 0 })))
    } catch {
      setRows([])
      setLoadError(true)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const onChangeThreshold = (level, value) => {
    setRows((prev) => prev.map((r) => (r.level === level ? { ...r, minPlayMinutes: value ?? 0 } : r)))
  }

  const onSave = async () => {
    setSaving(true)
    try {
      await request.put('/admin/levels', rows)
      message.success('等级规则已保存，次日任务将重算用户等级')
    } catch {
      // 提示已统一处理
    } finally {
      setSaving(false)
    }
  }

  return (
    <Card
      variant="borderless"
      title="用户等级规则"
      extra={
        <>
          <Button icon={<ReloadOutlined />} onClick={load} style={{ marginRight: 8 }}>
            刷新
          </Button>
          <Button type="primary" icon={<SaveOutlined />} loading={saving} onClick={onSave} disabled={!rows.length}>
            保存规则
          </Button>
        </>
      }
    >
      <Table
        rowKey="level"
        size="middle"
        loading={loading}
        dataSource={rows}
        pagination={false}
        locale={
          loadError
            ? { emptyText: '数据加载失败，请检查后端服务后点击"刷新"重试' }
            : { emptyText: <Empty description="暂无等级规则" /> }
        }
        columns={[
          {
            title: '等级',
            dataIndex: 'level',
            width: 120,
            render: (v) => <Tag color="blue">Lv{v}</Tag>,
          },
          {
            title: '升级阈值（累计播放分钟数 ≥）',
            dataIndex: 'minPlayMinutes',
            render: (v, r) => (
              <InputNumber min={0} value={v} style={{ width: 200 }} onChange={(val) => onChangeThreshold(r.level, val)} />
            ),
          },
        ]}
      />
      <div style={{ marginTop: 12, color: '#999', fontSize: 12 }}>
        说明：用户按累计播放时长升级（Lv1-Lv10）。阈值修改保存后，由次日定时任务重算全量用户等级。
      </div>
    </Card>
  )
}
