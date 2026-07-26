import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Empty, Image, Popconfirm, Space, Table, Tabs, Tag, message } from 'antd'
import { ReloadOutlined, SoundOutlined } from '@ant-design/icons'
import request from '../../utils/request'
import RejectModal from './RejectModal'

const FALLBACK_COVER = '/img/singer/lin-junjie.jpg'

const STATUS_TABS = [
  { key: 'PENDING', label: '待审核' },
  { key: 'PASSED', label: '已通过' },
  { key: 'REJECTED', label: '已驳回' },
]

export default function MusicAuditPage() {
  const [status, setStatus] = useState('PENDING')
  const [rows, setRows] = useState([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState({ pageNum: 1, pageSize: 10 })
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState(false)
  const [rejectTarget, setRejectTarget] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    setLoadError(false)
    try {
      const data = await request.get('/admin/audits', {
        params: { status, pageNum: page.pageNum, pageSize: page.pageSize },
      })
      setRows(data?.list || (Array.isArray(data) ? data : []))
      setTotal(Number(data?.total) || 0)
    } catch {
      setRows([])
      setTotal(0)
      setLoadError(true)
    } finally {
      setLoading(false)
    }
  }, [status, page.pageNum, page.pageSize])

  useEffect(() => {
    load()
  }, [load])

  const onPass = async (record) => {
    try {
      await request.put(`/admin/audits/${record.id}/pass`)
      message.success(`《${record.name}》已通过审核并上架`)
      load()
    } catch {
      // 提示已统一处理
    }
  }

  const onListen = async (record) => {
    try {
      const data = await request.get(`/admin/audits/${record.id}/url`)
      if (data?.url) {
        window.open(data.url, '_blank', 'noopener')
      } else {
        message.warning('未获取到试听地址')
      }
    } catch {
      // 提示已统一处理
    }
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    {
      title: '封面',
      dataIndex: 'cover',
      width: 76,
      render: (v) => (
        <Image
          src={v || FALLBACK_COVER}
          fallback={FALLBACK_COVER}
          width={44}
          height={44}
          style={{ borderRadius: 6, objectFit: 'cover' }}
          preview={!!v}
        />
      ),
    },
    { title: '歌名', dataIndex: 'name', ellipsis: true },
    { title: '歌手', dataIndex: 'singerName', width: 110, ellipsis: true },
    { title: '分类', dataIndex: 'categoryName', width: 90 },
    {
      title: '码率',
      dataIndex: 'bitrate',
      width: 90,
      render: (v) => (v ? `${v} kbps` : '-'),
    },
    {
      title: '大小',
      dataIndex: 'fileSize',
      width: 90,
      render: (v) => (v ? `${(v / 1024 / 1024).toFixed(1)} MB` : '-'),
    },
    { title: '提交人', dataIndex: 'createBy', width: 100, render: (v) => v || '-' },
    { title: '提交时间', dataIndex: 'createTime', width: 160, render: (v) => v || '-' },
  ]

  if (status === 'REJECTED') {
    columns.push({
      title: '驳回理由',
      dataIndex: 'rejectReason',
      width: 180,
      ellipsis: true,
      render: (v) => v || '-',
    })
  }

  columns.push({
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: status === 'PENDING' ? 200 : 90,
    render: (_, record) => (
      <Space size={0}>
        <Button type="link" size="small" icon={<SoundOutlined />} onClick={() => onListen(record)}>
          试听
        </Button>
        {status === 'PENDING' && (
          <>
            <Popconfirm title={`确认通过《${record.name}》？`} onConfirm={() => onPass(record)}>
              <Button type="link" size="small">
                通过
              </Button>
            </Popconfirm>
            <Button type="link" size="small" danger onClick={() => setRejectTarget(record)}>
              驳回
            </Button>
          </>
        )}
      </Space>
    ),
  })

  return (
    <Card
      variant="borderless"
      title="音乐审核"
      extra={
        <Button icon={<ReloadOutlined />} onClick={load}>
          刷新
        </Button>
      }
    >
      <Tabs
        activeKey={status}
        onChange={(k) => {
          setStatus(k)
          setPage({ ...page, pageNum: 1 })
        }}
        items={STATUS_TABS.map((t) => (t.key === 'PENDING' ? { ...t, label: <span>{t.label}</span> } : t))}
      />
      <Table
        rowKey={(r, i) => r.id ?? i}
        size="middle"
        loading={loading}
        columns={columns}
        dataSource={rows}
        scroll={{ x: 'max-content' }}
        locale={
          loadError
            ? { emptyText: '数据加载失败，请检查后端服务后点击"刷新"重试' }
            : { emptyText: <Empty description="暂无数据" /> }
        }
        pagination={{
          current: page.pageNum,
          pageSize: page.pageSize,
          total,
          showSizeChanger: true,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (pageNum, pageSize) => setPage({ pageNum, pageSize }),
        }}
      />
      <RejectModal
        target={rejectTarget}
        onClose={() => setRejectTarget(null)}
        onDone={() => {
          setRejectTarget(null)
          load()
        }}
      />
    </Card>
  )
}
