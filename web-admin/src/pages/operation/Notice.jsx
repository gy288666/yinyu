import { DatePicker, Form, Input } from 'antd'
import dayjs from 'dayjs'
import CrudPage, { keywordItem } from '../../components/CrudPage'

export default function NoticePage() {
  return (
    <CrudPage
      title="公告"
      listUrl="/admin/notices"
      modalWidth={640}
      columns={[
        { title: 'ID', dataIndex: 'id', width: 70 },
        { title: '标题', dataIndex: 'title', ellipsis: true },
        { title: '发布时间', dataIndex: 'publishTime', width: 180, render: (v) => v || '-' },
        { title: '创建时间', dataIndex: 'createTime', width: 180, render: (v) => v || '-' },
      ]}
      searchItems={keywordItem('title', '公告标题')}
      toEditValues={(r) => ({ ...r, publishTime: r.publishTime ? dayjs(r.publishTime) : null })}
      beforeSubmit={(values) => ({
        ...values,
        publishTime: values.publishTime ? values.publishTime.format('YYYY-MM-DD HH:mm:ss') : undefined,
      })}
      formItems={
        <>
          <Form.Item name="title" label="公告标题" rules={[{ required: true, message: '请输入公告标题' }]}>
            <Input maxLength={60} />
          </Form.Item>
          <Form.Item name="content" label="公告内容（支持 HTML 富文本）" rules={[{ required: true, message: '请输入公告内容' }]}>
            <Input.TextArea rows={8} placeholder="支持 HTML 富文本内容" />
          </Form.Item>
          <Form.Item name="publishTime" label="生效时间（留空立即生效）">
            <DatePicker showTime style={{ width: '100%' }} />
          </Form.Item>
        </>
      }
    />
  )
}
