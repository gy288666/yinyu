import { Button, DatePicker, Form, Image, Input, Popconfirm, Tag, message } from 'antd'
import dayjs from 'dayjs'
import CrudPage, { keywordItem } from '../../components/CrudPage'
import UploadImage from '../../components/UploadImage'
import request from '../../utils/request'

const STATUS_MAP = {
  ONLINE: { text: '进行中', color: 'success' },
  ONGOING: { text: '进行中', color: 'success' },
  OFFLINE: { text: '已下线', color: 'default' },
  ENDED: { text: '已结束', color: 'default' },
}

export default function ActivityPage() {
  const toggleStatus = async (record, reload) => {
    const online = record.status === 'ONLINE' || record.status === 'ONGOING'
    const next = online ? 'OFFLINE' : 'ONLINE'
    try {
      await request.put(`/admin/activities/${record.id}/status`, { status: next })
      message.success(next === 'ONLINE' ? '已上线' : '已下线')
      reload()
    } catch {
      // 提示已统一处理
    }
  }

  return (
    <CrudPage
      title="活动"
      listUrl="/admin/activities"
      modalWidth={640}
      columns={[
        { title: 'ID', dataIndex: 'id', width: 70 },
        {
          title: '封面',
          dataIndex: 'cover',
          width: 110,
          render: (v) => (v ? <Image src={v} width={88} height={40} style={{ borderRadius: 6, objectFit: 'cover' }} /> : '-'),
        },
        { title: '活动标题', dataIndex: 'title', ellipsis: true },
        { title: '开始时间', dataIndex: 'startTime', width: 160, render: (v) => v || '-' },
        { title: '结束时间', dataIndex: 'endTime', width: 160, render: (v) => v || '-' },
        {
          title: '状态',
          dataIndex: 'status',
          width: 100,
          render: (v, r) => {
            if (r.endTime && dayjs(r.endTime).isBefore(dayjs())) {
              return <Tag>已结束</Tag>
            }
            const s = STATUS_MAP[v] || { text: v || '-', color: 'default' }
            return <Tag color={s.color}>{s.text}</Tag>
          },
        },
      ]}
      searchItems={keywordItem('title', '活动标题')}
      toEditValues={(r) => ({
        ...r,
        startTime: r.startTime ? dayjs(r.startTime) : null,
        endTime: r.endTime ? dayjs(r.endTime) : null,
      })}
      beforeSubmit={(values) => ({
        ...values,
        startTime: values.startTime ? values.startTime.format('YYYY-MM-DD HH:mm:ss') : undefined,
        endTime: values.endTime ? values.endTime.format('YYYY-MM-DD HH:mm:ss') : undefined,
      })}
      rowActions={(record, reload) => {
        const online = record.status === 'ONLINE' || record.status === 'ONGOING'
        return (
          <Popconfirm title={online ? '确认下线该活动？' : '确认上线该活动？'} onConfirm={() => toggleStatus(record, reload)}>
            <Button type="link" size="small">
              {online ? '下线' : '上线'}
            </Button>
          </Popconfirm>
        )
      }}
      formItems={
        <>
          <Form.Item name="title" label="活动标题" rules={[{ required: true, message: '请输入活动标题' }]}>
            <Input maxLength={60} />
          </Form.Item>
          <Form.Item name="cover" label="活动封面">
            <UploadImage tip="上传封面" />
          </Form.Item>
          <Form.Item name="content" label="活动详情（支持 HTML 富文本）" rules={[{ required: true, message: '请输入活动详情' }]}>
            <Input.TextArea rows={6} placeholder="支持 HTML 富文本内容" />
          </Form.Item>
          <Form.Item name="startTime" label="开始时间" rules={[{ required: true, message: '请选择开始时间' }]}>
            <DatePicker showTime style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="endTime" label="结束时间" rules={[{ required: true, message: '请选择结束时间' }]}>
            <DatePicker showTime style={{ width: '100%' }} />
          </Form.Item>
        </>
      }
    />
  )
}
