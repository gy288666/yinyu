import { useState } from 'react'
import { Button, Descriptions, Form, Input, Modal, Popconfirm, Select, Tag, message } from 'antd'
import CrudPage from '../../components/CrudPage'
import request from '../../utils/request'

export default function UserListPage() {
  const [detail, setDetail] = useState(null)

  const toggleStatus = async (record, reload) => {
    const next = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
    try {
      await request.put(`/admin/users/${record.id}/status`, { status: next })
      message.success(next === 'ENABLED' ? '已启用' : '已停用')
      reload()
    } catch {
      // 提示已统一处理
    }
  }

  const resetPassword = async (record) => {
    try {
      const data = await request.put(`/admin/users/${record.id}/password/reset`)
      Modal.success({
        title: '密码已重置',
        content: `用户 ${record.username} 的新密码：${data?.password || data || '（见后端返回）'}，用户首次登录需修改密码。`,
      })
    } catch {
      // 提示已统一处理
    }
  }

  const showDetail = async (record) => {
    try {
      const data = await request.get(`/admin/users/${record.id}`)
      setDetail(data || record)
    } catch {
      setDetail(record)
    }
  }

  return (
    <>
      <CrudPage
        title="用户"
        listUrl="/admin/users"
        canCreate={false}
        canEdit={false}
        canDelete={false}
        columns={[
          { title: 'ID', dataIndex: 'id', width: 80 },
          { title: '用户名', dataIndex: 'username', width: 130 },
          { title: '昵称', dataIndex: 'nickname', width: 130, ellipsis: true },
          { title: '等级', dataIndex: 'level', width: 80, render: (v) => <Tag color="blue">Lv{v ?? 1}</Tag> },
          {
            title: 'VIP',
            dataIndex: 'vip',
            width: 90,
            render: (v, r) =>
              v ? <Tag color="gold" title={r.vipExpireAt ? `到期：${r.vipExpireAt}` : ''}>VIP</Tag> : '-',
          },
          { title: '注册渠道', dataIndex: 'channel', width: 100, render: (v) => v || '-' },
          {
            title: '状态',
            dataIndex: 'status',
            width: 90,
            render: (v) => (v === 'DISABLED' ? <Tag color="red">已停用</Tag> : <Tag color="green">正常</Tag>),
          },
          { title: '最近登录', dataIndex: 'lastLoginAt', width: 160, render: (v) => v || '-' },
          { title: '注册时间', dataIndex: 'createTime', width: 160, render: (v) => v || '-' },
        ]}
        searchItems={
          <>
            <Form.Item name="username">
              <Input allowClear placeholder="用户名" style={{ width: 150 }} />
            </Form.Item>
            <Form.Item name="nickname">
              <Input allowClear placeholder="昵称" style={{ width: 150 }} />
            </Form.Item>
            <Form.Item name="status">
              <Select
                allowClear
                placeholder="状态"
                style={{ width: 120 }}
                options={[
                  { value: 'ENABLED', label: '正常' },
                  { value: 'DISABLED', label: '已停用' },
                ]}
              />
            </Form.Item>
            <Form.Item name="vip">
              <Select
                allowClear
                placeholder="是否VIP"
                style={{ width: 120 }}
                options={[
                  { value: true, label: 'VIP' },
                  { value: false, label: '非VIP' },
                ]}
              />
            </Form.Item>
          </>
        }
        rowActions={(record, reload) => (
          <>
            <Button type="link" size="small" onClick={() => showDetail(record)}>
              详情
            </Button>
            <Popconfirm
              title={record.status === 'ENABLED' ? '停用后该用户将无法登录，确认？' : '确认启用该用户？'}
              onConfirm={() => toggleStatus(record, reload)}
            >
              <Button type="link" size="small" danger={record.status === 'ENABLED'}>
                {record.status === 'ENABLED' ? '停用' : '启用'}
              </Button>
            </Popconfirm>
            <Popconfirm title="确认重置该用户密码？" onConfirm={() => resetPassword(record)}>
              <Button type="link" size="small">
                重置密码
              </Button>
            </Popconfirm>
          </>
        )}
      />
      <Modal title="用户详情" open={!!detail} footer={null} onCancel={() => setDetail(null)}>
        {detail && (
          <Descriptions column={1} size="small" bordered style={{ marginTop: 12 }}>
            <Descriptions.Item label="ID">{detail.id ?? detail.userId}</Descriptions.Item>
            <Descriptions.Item label="用户名">{detail.username}</Descriptions.Item>
            <Descriptions.Item label="昵称">{detail.nickname}</Descriptions.Item>
            <Descriptions.Item label="等级">Lv{detail.level ?? 1}</Descriptions.Item>
            <Descriptions.Item label="VIP">
              {detail.vip ? `是（到期：${detail.vipExpireAt || '-'}）` : '否'}
            </Descriptions.Item>
            <Descriptions.Item label="注册渠道">{detail.channel || '-'}</Descriptions.Item>
            <Descriptions.Item label="签名">{detail.signature || '-'}</Descriptions.Item>
            <Descriptions.Item label="最近登录">{detail.lastLoginAt || '-'}</Descriptions.Item>
            <Descriptions.Item label="注册时间">{detail.createTime || '-'}</Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </>
  )
}
