import { DatePicker, Form, Image, Input, InputNumber, Select, Switch, Tag } from 'antd'
import dayjs from 'dayjs'
import CrudPage, { keywordItem } from '../../components/CrudPage'
import UploadImage from '../../components/UploadImage'

const TARGET_OPTIONS = [
  { value: 'NONE', label: '无跳转' },
  { value: 'SONG', label: '歌曲' },
  { value: 'PLAYLIST', label: '歌单' },
  { value: 'ALBUM', label: '专辑' },
  { value: 'ACTIVITY', label: '活动' },
  { value: 'URL', label: '外链' },
]

export default function BannerPage() {
  return (
    <CrudPage
      title="轮播图"
      listUrl="/admin/banners"
      modalWidth={620}
      columns={[
        { title: 'ID', dataIndex: 'id', width: 70 },
        {
          title: '图片',
          dataIndex: 'image',
          width: 120,
          render: (v) => (v ? <Image src={v} width={96} height={40} style={{ borderRadius: 6, objectFit: 'cover' }} /> : '-'),
        },
        { title: '标题', dataIndex: 'title', ellipsis: true },
        {
          title: '跳转类型',
          dataIndex: 'targetType',
          width: 100,
          render: (v) => TARGET_OPTIONS.find((o) => o.value === v)?.label || v || '-',
        },
        { title: '排序', dataIndex: 'sort', width: 80, render: (v) => v ?? 0 },
        { title: '开始时间', dataIndex: 'startTime', width: 160, render: (v) => v || '-' },
        { title: '结束时间', dataIndex: 'endTime', width: 160, render: (v) => v || '-' },
        {
          title: '状态',
          dataIndex: 'enabled',
          width: 90,
          render: (v) => (v ? <Tag color="success">启用</Tag> : <Tag>停用</Tag>),
        },
      ]}
      searchItems={keywordItem('title', '标题')}
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
      formItems={
        <>
          <Form.Item name="image" label="轮播图片" rules={[{ required: true, message: '请上传轮播图片' }]}>
            <UploadImage tip="上传图片" />
          </Form.Item>
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input maxLength={40} />
          </Form.Item>
          <Form.Item name="targetType" label="跳转类型" rules={[{ required: true, message: '请选择跳转类型' }]}>
            <Select options={TARGET_OPTIONS} placeholder="选择跳转类型" />
          </Form.Item>
          <Form.Item
            noStyle
            shouldUpdate={(prev, cur) => prev.targetType !== cur.targetType}
          >
            {({ getFieldValue }) =>
              getFieldValue('targetType') === 'LINK' ? (
                <Form.Item name="link" label="外链地址" rules={[{ required: true, message: '请输入外链地址' }]}>
                  <Input placeholder="https://..." />
                </Form.Item>
              ) : (
                <Form.Item name="targetId" label="跳转目标 ID" rules={[{ required: true, message: '请输入目标 ID' }]}>
                  <InputNumber min={1} style={{ width: '100%' }} placeholder="歌单/专辑/活动 ID" />
                </Form.Item>
              )
            }
          </Form.Item>
          <Form.Item name="sort" label="排序（越小越靠前）" initialValue={0}>
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="startTime" label="开始时间">
            <DatePicker showTime style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="endTime" label="结束时间">
            <DatePicker showTime style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked" initialValue={true}>
            <Switch />
          </Form.Item>
        </>
      }
    />
  )
}
