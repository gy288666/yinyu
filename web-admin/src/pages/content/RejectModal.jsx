import { useState } from 'react'
import { Form, Input, Modal, message } from 'antd'
import request from '../../utils/request'

/** 审核驳回弹窗：target 为待驳回歌曲记录，reason 必填（≥5 字） */
export default function RejectModal({ target, onClose, onDone }) {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)

  const onOk = async () => {
    let values
    try {
      values = await form.validateFields()
    } catch {
      return
    }
    setLoading(true)
    try {
      await request.put(`/admin/audits/${target.id}/reject`, { reason: values.reason })
      message.success('已驳回')
      form.resetFields()
      onDone?.()
    } catch {
      // 提示已统一处理
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal
      title={`驳回：${target?.name || ''}`}
      open={!!target}
      onOk={onOk}
      onCancel={() => {
        form.resetFields()
        onClose?.()
      }}
      confirmLoading={loading}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" style={{ marginTop: 12 }}>
        <Form.Item
          name="reason"
          label="驳回理由"
          rules={[
            { required: true, message: '请填写驳回理由' },
            { min: 5, message: '驳回理由不少于 5 个字' },
          ]}
        >
          <Input.TextArea rows={4} maxLength={200} showCount placeholder="请说明驳回原因（不少于 5 个字）" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
