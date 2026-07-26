import { useCallback, useEffect, useMemo, useState } from 'react'
import { Button, Card, Col, DatePicker, Empty, Radio, Row, Select, Space, message } from 'antd'
import { DownloadOutlined, SearchOutlined } from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import dayjs from 'dayjs'
import { silentGet } from '../../utils/request'

const PRIMARY = '#2b6de8'

const METRIC_OPTIONS = [
  { value: 'play', label: '播放量' },
  { value: 'register', label: '注册量' },
  { value: 'revenue', label: '订单收益' },
]

const GRANULARITY_OPTIONS = [
  { value: 'day', label: '按日' },
  { value: 'week', label: '按周' },
  { value: 'month', label: '按月' },
]

export default function StatsPage() {
  const [metric, setMetric] = useState('play')
  const [granularity, setGranularity] = useState('day')
  const [range, setRange] = useState([dayjs().subtract(29, 'day'), dayjs()])
  const [data, setData] = useState([])
  const [hotSongs, setHotSongs] = useState([])
  const [loading, setLoading] = useState(false)

  const query = useCallback(async () => {
    if (!range?.[0] || !range?.[1]) {
      message.warning('请选择时间范围')
      return
    }
    if (range[1].diff(range[0], 'day') > 366) {
      message.warning('时间范围最大 1 年')
      return
    }
    setLoading(true)
    const res = await silentGet(
      '/admin/stats',
      {
        metric,
        granularity,
        startDate: range[0].format('YYYY-MM-DD'),
        endDate: range[1].format('YYYY-MM-DD'),
      },
      []
    )
    setData(Array.isArray(res) ? res : [])
    setLoading(false)
  }, [metric, granularity, range])

  useEffect(() => {
    query()
    silentGet('/admin/dashboard/hot-songs', undefined, []).then((res) => setHotSongs(Array.isArray(res) ? res : []))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const onExport = () => {
    if (!range?.[0] || !range?.[1]) return
    const params = new URLSearchParams({
      metric,
      granularity,
      startDate: range[0].format('YYYY-MM-DD'),
      endDate: range[1].format('YYYY-MM-DD'),
    })
    // CSV 文件流下载（携带 token 需后端支持；此处直接打开，未登录时由后端 401 拦截）
    window.open(`/api/admin/stats/export?${params.toString()}`, '_blank', 'noopener')
  }

  const metricLabel = METRIC_OPTIONS.find((m) => m.value === metric)?.label || metric

  const trendOption = useMemo(
    () => ({
      grid: { left: 60, right: 24, top: 40, bottom: 40 },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: data.map((d) => d.period),
        axisLabel: { color: '#8c8c8c' },
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { type: 'dashed', color: '#f0f0f0' } },
        axisLabel: { color: '#8c8c8c' },
      },
      series: [
        {
          name: metricLabel,
          type: 'line',
          smooth: true,
          data: data.map((d) => d.value),
          lineStyle: { color: PRIMARY, width: 3 },
          itemStyle: { color: PRIMARY },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(43,109,232,0.25)' },
                { offset: 1, color: 'rgba(43,109,232,0.02)' },
              ],
            },
          },
        },
      ],
    }),
    [data, metricLabel]
  )

  const rankOption = useMemo(
    () => ({
      grid: { left: 110, right: 40, top: 20, bottom: 30 },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      xAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed', color: '#f0f0f0' } } },
      yAxis: {
        type: 'category',
        data: [...hotSongs].reverse().map((s) => s.name),
        axisLabel: { color: '#595959', width: 90, overflow: 'truncate' },
      },
      series: [
        {
          type: 'bar',
          barWidth: 16,
          data: [...hotSongs].reverse().map((s) => s.todayPlayCount ?? 0),
          itemStyle: { color: PRIMARY, borderRadius: [0, 8, 8, 0] },
          label: { show: true, position: 'right', color: '#8c8c8c' },
        },
      ],
    }),
    [hotSongs]
  )

  return (
    <div>
      <Card variant="borderless" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Select value={metric} onChange={setMetric} options={METRIC_OPTIONS} style={{ width: 130 }} />
          <Radio.Group
            value={granularity}
            onChange={(e) => setGranularity(e.target.value)}
            options={GRANULARITY_OPTIONS}
            optionType="button"
          />
          <DatePicker.RangePicker value={range} onChange={setRange} allowClear={false} />
          <Button type="primary" icon={<SearchOutlined />} onClick={query} loading={loading}>
            查询
          </Button>
          <Button icon={<DownloadOutlined />} onClick={onExport}>
            导出 CSV
          </Button>
        </Space>
      </Card>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={14}>
          <Card variant="borderless" title={`${metricLabel}趋势`} loading={loading}>
            {data.length ? (
              <ReactECharts option={trendOption} style={{ height: 360 }} notMerge />
            ) : (
              <Empty description="暂无统计数据（后端接口未实现或该区间无数据）" style={{ padding: '80px 0' }} />
            )}
          </Card>
        </Col>
        <Col xs={24} lg={10}>
          <Card variant="borderless" title="今日热门歌曲榜">
            {hotSongs.length ? (
              <ReactECharts option={rankOption} style={{ height: 360 }} notMerge />
            ) : (
              <Empty description="暂无榜单数据" style={{ padding: '80px 0' }} />
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}
