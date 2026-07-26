import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Card, Col, Empty, List, Popconfirm, Radio, Row, Space, Table, Tabs, Tag, message } from 'antd'
import {
  ArrowDownOutlined,
  ArrowUpOutlined,
  AuditOutlined,
  CrownOutlined,
  CustomerServiceOutlined,
  MoneyCollectOutlined,
  NotificationOutlined,
  PictureOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  SettingOutlined,
  TagsOutlined,
  TeamOutlined,
  UploadOutlined,
  UserOutlined,
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import request, { silentGet } from '../../utils/request'
import RejectModal from '../content/RejectModal'

const PRIMARY = '#2b6de8'

/* ---------- 统计卡片 ---------- */

const CARD_DEFS = [
  { key: 'songTotal', title: '音乐总数', icon: <CustomerServiceOutlined />, color: '#2b6de8', bg: '#e8f0fe' },
  { key: 'userTotal', title: '用户总数', icon: <UserOutlined />, color: '#13c2c2', bg: '#e6fffb' },
  { key: 'todayPlayCount', title: '今日播放量', icon: <PlayCircleOutlined />, color: '#722ed1', bg: '#f9f0ff' },
  { key: 'vipCount', title: '付费会员数', icon: <CrownOutlined />, color: '#fa8c16', bg: '#fff7e6' },
  { key: 'revenue', title: '收益（元）', icon: <MoneyCollectOutlined />, color: '#f5222d', bg: '#fff1f0' },
]

function StatCard({ title, value, ratio, icon, color, bg }) {
  const up = (ratio ?? 0) >= 0
  return (
    <Card variant="borderless" styles={{ body: { padding: '20px 24px' } }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <div style={{ color: '#8c8c8c', fontSize: 13 }}>{title}</div>
          <div style={{ fontSize: 26, fontWeight: 600, margin: '8px 0 6px', color: '#262626' }}>{value}</div>
          <div style={{ fontSize: 12, color: '#8c8c8c' }}>
            较昨日{' '}
            <span style={{ color: up ? '#f5222d' : '#52c41a' }}>
              {up ? <ArrowUpOutlined /> : <ArrowDownOutlined />} {Math.abs((ratio ?? 0) * 100).toFixed(1)}%
            </span>
          </div>
        </div>
        <div
          style={{
            width: 44,
            height: 44,
            borderRadius: 10,
            background: bg,
            color,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: 20,
          }}
        >
          {icon}
        </div>
      </div>
    </Card>
  )
}

/* ---------- 播放量趋势 ---------- */

function PlayTrendCard() {
  const [range, setRange] = useState('7')
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    let alive = true
    setLoading(true)
    const days = range === 'today' ? 1 : Number(range)
    silentGet('/admin/dashboard/play-trend', { days }, []).then((res) => {
      if (!alive) return
      setData(Array.isArray(res) ? res : [])
      setLoading(false)
    })
    return () => {
      alive = false
    }
  }, [range])

  const option = useMemo(
    () => ({
      grid: { left: 48, right: 24, top: 32, bottom: 32 },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: data.map((d) => d.date),
        axisLine: { lineStyle: { color: '#d9d9d9' } },
        axisLabel: { color: '#8c8c8c' },
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { type: 'dashed', color: '#f0f0f0' } },
        axisLabel: { color: '#8c8c8c' },
      },
      series: [
        {
          name: '播放量',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          data: data.map((d) => d.count),
          lineStyle: { color: PRIMARY, width: 3 },
          itemStyle: { color: PRIMARY },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(43,109,232,0.28)' },
                { offset: 1, color: 'rgba(43,109,232,0.02)' },
              ],
            },
          },
        },
      ],
    }),
    [data]
  )

  return (
    <Card
      variant="borderless"
      title="播放量趋势"
      loading={loading}
      extra={
        <Radio.Group size="small" value={range} onChange={(e) => setRange(e.target.value)}>
          <Radio.Button value="today">今日</Radio.Button>
          <Radio.Button value="7">近7日</Radio.Button>
          <Radio.Button value="30">近30日</Radio.Button>
        </Radio.Group>
      }
    >
      {data.length ? (
        <ReactECharts option={option} style={{ height: 300 }} notMerge />
      ) : (
        <Empty description="暂无趋势数据" style={{ padding: '60px 0' }} />
      )}
    </Card>
  )
}

/* ---------- 用户来源渠道环形图 ---------- */

const CHANNEL_LABELS = {
  ios: 'iOS',
  android: 'Android',
  web: 'Web',
  mini: '小程序',
  miniprogram: '小程序',
  direct: '直接访问',
  search: '搜索',
  share: '分享',
  activity: '活动',
  other: '其他',
}

function ChannelCard() {
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    silentGet('/admin/dashboard/user-channels', undefined, []).then((res) => {
      setData(Array.isArray(res) ? res : [])
      setLoading(false)
    })
  }, [])

  const option = useMemo(
    () => ({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: 0, icon: 'circle', textStyle: { color: '#595959' } },
      color: ['#2b6de8', '#13c2c2', '#722ed1', '#fa8c16', '#a0d911', '#f5222d'],
      series: [
        {
          type: 'pie',
          radius: ['52%', '74%'],
          center: ['50%', '44%'],
          avoidLabelOverlap: true,
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: { show: false },
          data: data.map((d) => ({
            name: CHANNEL_LABELS[String(d.channel).toLowerCase()] || d.channel || '其他',
            value: d.count ?? 0,
          })),
        },
      ],
    }),
    [data]
  )

  return (
    <Card variant="borderless" title="用户来源渠道" loading={loading}>
      {data.length ? (
        <ReactECharts option={option} style={{ height: 300 }} notMerge />
      ) : (
        <Empty description="暂无渠道数据" style={{ padding: '60px 0' }} />
      )}
    </Card>
  )
}

/* ---------- 实时动态 ---------- */

const EVENT_TAGS = {
  REGISTER: { color: 'blue', text: '注册' },
  ORDER: { color: 'gold', text: '订单' },
  AUDIT: { color: 'green', text: '审核' },
  COPYRIGHT_EXPIRE: { color: 'red', text: '版权' },
  TASK_FAIL: { color: 'volcano', text: '任务' },
}

function EventsCard() {
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    silentGet('/admin/dashboard/events', { limit: 20 }, []).then((res) => {
      setData(Array.isArray(res) ? res : [])
      setLoading(false)
    })
  }, [])

  return (
    <Card variant="borderless" title="实时动态" loading={loading} styles={{ body: { maxHeight: 340, overflow: 'auto' } }}>
      {data.length ? (
        <List
          size="small"
          dataSource={data}
          renderItem={(item, i) => {
            const tag = EVENT_TAGS[item.type] || { color: 'default', text: '动态' }
            return (
              <List.Item key={i} style={{ padding: '8px 0' }}>
                <Space size={8} style={{ width: '100%' }}>
                  <Tag color={tag.color} style={{ marginInlineEnd: 0 }}>
                    {tag.text}
                  </Tag>
                  <span style={{ flex: 1, fontSize: 13 }}>{item.text}</span>
                  <span style={{ color: '#bfbfbf', fontSize: 12 }}>{item.time}</span>
                </Space>
              </List.Item>
            )
          }}
        />
      ) : (
        <Empty description="暂无动态" />
      )}
    </Card>
  )
}

/* ---------- 热门歌曲 TOP5 ---------- */

function HotSongsCard() {
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    silentGet('/admin/dashboard/hot-songs', undefined, []).then((res) => {
      setData(Array.isArray(res) ? res : [])
      setLoading(false)
    })
  }, [])

  const rankColor = (i) => (i === 0 ? '#f5222d' : i === 1 ? '#fa8c16' : i === 2 ? '#faad14' : '#bfbfbf')

  return (
    <Card variant="borderless" title="热门歌曲 TOP5" loading={loading}>
      <Table
        size="small"
        rowKey={(r, i) => r.songId ?? i}
        pagination={false}
        dataSource={data}
        locale={{ emptyText: <Empty description="暂无数据" /> }}
        columns={[
          {
            title: '排名',
            width: 64,
            render: (_, __, i) => (
              <span style={{ fontWeight: 700, fontStyle: 'italic', color: rankColor(i) }}>{i + 1}</span>
            ),
          },
          { title: '歌曲', dataIndex: 'name', ellipsis: true },
          { title: '歌手', dataIndex: 'singerName', width: 110, ellipsis: true },
          {
            title: '今日播放',
            dataIndex: 'todayPlayCount',
            width: 100,
            align: 'right',
            render: (v) => <span style={{ color: PRIMARY }}>{v ?? 0}</span>,
          },
        ]}
      />
    </Card>
  )
}

/* ---------- 系统公告 ---------- */

function NoticesCard() {
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    silentGet('/admin/notices', { pageNum: 1, pageSize: 5 }, null).then((res) => {
      const list = Array.isArray(res) ? res : res?.list || []
      setData(list.slice(0, 5))
      setLoading(false)
    })
  }, [])

  return (
    <Card
      variant="borderless"
      title="系统公告"
      loading={loading}
      extra={
        <Button type="link" size="small" onClick={() => navigate('/operation/notice')}>
          更多
        </Button>
      }
    >
      {data.length ? (
        <List
          size="small"
          dataSource={data}
          renderItem={(item) => (
            <List.Item style={{ padding: '8px 0' }}>
              <Space style={{ width: '100%' }}>
                <NotificationOutlined style={{ color: PRIMARY }} />
                <span style={{ flex: 1, fontSize: 13 }}>{item.title}</span>
                <span style={{ color: '#bfbfbf', fontSize: 12 }}>{item.publishTime || item.createTime || ''}</span>
              </Space>
            </List.Item>
          )}
        />
      ) : (
        <Empty description="暂无公告" />
      )}
    </Card>
  )
}

/* ---------- 快捷操作 ---------- */

function QuickActionsCard() {
  const navigate = useNavigate()
  const actions = [
    { label: '上传音乐', icon: <UploadOutlined />, path: '/content/music', color: '#2b6de8' },
    { label: '音乐审核', icon: <AuditOutlined />, path: '/content/audit', color: '#52c41a' },
    { label: '发布公告', icon: <NotificationOutlined />, path: '/operation/notice', color: '#fa8c16' },
    { label: '新增轮播', icon: <PictureOutlined />, path: '/operation/banner', color: '#722ed1' },
    { label: '新增歌手', icon: <TeamOutlined />, path: '/content/singer', color: '#13c2c2' },
    { label: '新增分类', icon: <TagsOutlined />, path: '/content/category', color: '#eb2f96' },
    { label: '会员管理', icon: <CrownOutlined />, path: '/user/vip', color: '#faad14' },
    { label: '系统设置', icon: <SettingOutlined />, path: '/system/setting', color: '#595959' },
  ]
  return (
    <Card variant="borderless" title="快捷操作">
      <Row gutter={[8, 16]}>
        {actions.map((a) => (
          <Col span={6} key={a.label}>
            <div
              onClick={() => navigate(a.path)}
              style={{ textAlign: 'center', cursor: 'pointer', padding: '6px 0' }}
            >
              <div
                style={{
                  width: 42,
                  height: 42,
                  margin: '0 auto 6px',
                  borderRadius: 10,
                  background: `${a.color}1a`,
                  color: a.color,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 18,
                }}
              >
                {a.icon}
              </div>
              <div style={{ fontSize: 12, color: '#595959' }}>{a.label}</div>
            </div>
          </Col>
        ))}
      </Row>
    </Card>
  )
}

/* ---------- 音乐审核待办 ---------- */

const AUDIT_STATUS_MAP = {
  PENDING: { text: '待审核', color: 'processing' },
  PASSED: { text: '已通过', color: 'success' },
  REJECTED: { text: '已驳回', color: 'error' },
}

function AuditTodoCard() {
  const [status, setStatus] = useState('PENDING')
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [rejectTarget, setRejectTarget] = useState(null)
  const navigate = useNavigate()

  const load = useCallback(async () => {
    setLoading(true)
    const res = await silentGet('/admin/audits', { status, pageNum: 1, pageSize: 5 }, null)
    setRows(res?.list || (Array.isArray(res) ? res : []))
    setLoading(false)
  }, [status])

  useEffect(() => {
    load()
  }, [load])

  const onPass = async (record) => {
    try {
      await request.put(`/admin/audits/${record.id}/pass`)
      message.success('已通过审核')
      load()
    } catch {
      // 提示已统一处理
    }
  }

  return (
    <Card
      variant="borderless"
      title="音乐审核待办"
      extra={
        <Button type="link" size="small" onClick={() => navigate('/content/audit')}>
          前往审核
        </Button>
      }
    >
      <Tabs
        size="small"
        activeKey={status}
        onChange={setStatus}
        items={[
          { key: 'PENDING', label: '待审核' },
          { key: 'PASSED', label: '已通过' },
          { key: 'REJECTED', label: '已驳回' },
        ]}
      />
      <Table
        size="small"
        rowKey={(r, i) => r.id ?? i}
        loading={loading}
        pagination={false}
        dataSource={rows}
        locale={{ emptyText: <Empty description="暂无数据" /> }}
        columns={[
          { title: '歌曲', dataIndex: 'name', ellipsis: true },
          { title: '歌手', dataIndex: 'singerName', width: 110, ellipsis: true },
          {
            title: '状态',
            dataIndex: 'status',
            width: 90,
            render: (v) => {
              const s = AUDIT_STATUS_MAP[v] || { text: v || '-', color: 'default' }
              return <Tag color={s.color}>{s.text}</Tag>
            },
          },
          {
            title: '操作',
            width: 130,
            render: (_, record) =>
              status === 'PENDING' ? (
                <Space size={0}>
                  <Popconfirm title="确认通过该曲目？" onConfirm={() => onPass(record)}>
                    <Button type="link" size="small">
                      通过
                    </Button>
                  </Popconfirm>
                  <Button type="link" size="small" danger onClick={() => setRejectTarget(record)}>
                    驳回
                  </Button>
                </Space>
              ) : (
                <Button type="link" size="small" onClick={() => navigate('/content/audit')}>
                  详情
                </Button>
              ),
          },
        ]}
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

/* ---------- 页面 ---------- */

export default function Dashboard() {
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    silentGet('/admin/dashboard/summary', undefined, null).then((res) => {
      setSummary(res)
      setLoading(false)
    })
  }, [])

  return (
    <div>
      <Row gutter={[16, 16]}>
        {CARD_DEFS.map((def) => (
          <Col key={def.key} flex="1 1 200px">
            {loading ? (
              <Card variant="borderless" loading />
            ) : (
              <StatCard
                title={def.title}
                value={summary?.[def.key] ?? 0}
                ratio={summary?.compare?.[def.key] ?? 0}
                icon={def.icon}
                color={def.color}
                bg={def.bg}
              />
            )}
          </Col>
        ))}
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={15}>
          <PlayTrendCard />
        </Col>
        <Col xs={24} lg={9}>
          <ChannelCard />
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={9}>
          <EventsCard />
        </Col>
        <Col xs={24} lg={9}>
          <HotSongsCard />
        </Col>
        <Col xs={24} lg={6}>
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <QuickActionsCard />
          </Space>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={15}>
          <AuditTodoCard />
        </Col>
        <Col xs={24} lg={9}>
          <NoticesCard />
        </Col>
      </Row>
    </div>
  )
}
