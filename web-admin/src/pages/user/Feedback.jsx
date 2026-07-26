import { useState } from 'react'
import { Button, Form, Image, Input, Modal, Popconfirm, Select, Space, Tag, message } from 'antd'
import CrudPage from '../../components/CrudPage'
import request from '../../utils/request'

const TYPE_MAP = {
  BUG: { text: '功能异常', color: 'red' },
  SUGGEST: { text: '建议', color: 'blue' },
  COPYRIGHT: { text: '版权投诉', color: 'orange' },
  OTHER: { text: '其他', color: 'default' },
}

const STATUS_MAP = {
  PENDING: { text: '待处理', color: 'processing' },
  REPLIED: { text: '已回复', color: 'success' },
  CLOSED: { text: '已关闭', color: 'default' },
}

export default function FeedbackPage() {
  const [replyTarget, setReplyTarget] = useState(null)
  const [replyForm] = Form.useForm()
  const [reloadFn, setReloadFn] = useState(null)

  const onReply = async () => {
    let values
    try {
      values = await replyForm.validateFields()
    } catch {
      return
    }
    try {
      await request.put(`/admin/feedbacks/${replyTarget.id}`, { reply: values.reply })
      message.success('回复成功')
      setReplyTarget(null)
      replyForm.resetFields()
      reloadFn?.()
    } catch {
      // 提示已统一处理
    }
  }

  const onClose = async (record, reload) => {
    try {
      await request.put(`/admin/feedbacks/${record.id}`, { status: 'CLOSED' })
      message.success('已关闭')
      reload()
    } catch {
      // 提示已统一处理
    }
  }

  return (
    <>
      <CrudPage
        title="用户反馈"
        listUrl="/admin/feedbacks"
        canCreate={false}
        canEdit={false}
        canDelete={false}
        columns={[
          { title: 'ID', dataIndex: 'id', width: 70 },
          { title: '用户', dataIndex: 'nickname', width: 120, render: (v, r) => v || r.username || `#${r.userId ?? '-'}` },
          {
            title: '类型',
            dataIndex: 'type',
            width: 100,
            render: (v) => {
              const t = TYPE_MAP[v] || { text: v || '-', color: 'default' }
              return <Tag color={t.color}>{t.text}</Tag>
            },
          },
          { title: '反馈内容', dataIndex: 'content', ellipsis: true },
          {
            title: '截图',
            dataIndex: 'images',
            width: 120,
            render: (imgs) =>
              Array.isArray(imgs) && imgs.length ? (
                <Space size={4}>
                  {imgs.slice(0, 3).map((url, i) => (
                    <Image key={i} src={url} width={28} height={28} style={{ borderRadius: 4, objectFit: 'cover' }} />
                  ))}
                </Space>
              ) : (
                '-'
              ),
          },
          {
            title: '状态',
            dataIndex: 'status',
            width: 90,
            render: (v) => {
              const s = STATUS_MAP[v] || { text: v || '-', color: 'default' }
              return <Tag color={s.color}>{s.text}</Tag>
            },
          },
          { title: '回复', dataIndex: 'reply', width: 160, ellipsis: true, render: (v) => v || '-' },
          { title: '提交时间', dataIndex: 'createTime', width: 160, render: (v) => v || '-' },
        ]}
        searchItems={
          <>
            <Form.Item name="type">
              <Select
                allowClear
                placeholder="类型"
                style={{ width: 130 }}
                options={Object.entries(TYPE_MAP).map(([value, t]) => ({ value, label: t.text }))}
              />
            </Form.Item>
            <Form.Item name="status">
              <Select
                allowClear
                placeholder="状态"
                style={{ width: 130 }}
                options={Object.entries(STATUS_MAP).map(([value, s]) => ({ value, label: s.text }))}
              />
            </Form.Item>
          </>
        }
        rowActions={(record, reload) => (
          <>
            {record.status !== 'CLOSED' && (
              <Button
                type="link"
                size="small"
                onClick={() => {
                  setReplyTarget(record)
                  setReloadFn(() => reload)
                  replyForm.setFieldValue('reply', record.reply || '')
                }}
              >
                回复
              </Button>
            )}
            {record.status !== 'CLOSED' && (
              <Popconfirm title="确认关闭该反馈？" onConfirm={() => onClose(record, reload)}>
                <Button type="link" size="small">
                  关闭
                </Button>
              </Popconfirm>
            )}
          </>
        )}
      />
      <Modal
        title="回复反馈"
        open={!!replyTarget}
        onOk={onReply}
        onCancel={() => {
          setReplyTarget(null)
          replyForm.resetFields()
        }}
        destroyOnHidden
      >
        {replyTarget && (
          <div style={{ background: '#fafafa', borderRadius: 8, padding: 12, margin: '12px 0', fontSize: 13 }}>
            {replyTarget.content}
          </div>
        )}
        <Form form={replyForm} layout="vertical">
          <Form.Item name="reply" label="回复内容" rules={[{ required: true, message: '请输入回复内容' }]}>
            <Input.TextArea rows={4} maxLength={500} showCount />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
