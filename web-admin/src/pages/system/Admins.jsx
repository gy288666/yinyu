import { useEffect, useState } from 'react'
import { Button, Form, Input, Modal, Popconfirm, Select, Tag, message } from 'antd'
import CrudPage, { keywordItem } from '../../components/CrudPage'
import request, { silentGet } from '../../utils/request'

/** 管理员管理：CRUD + 分配角色 + 重置密码；内置 admin 不可停用/删除 */
export default function AdminsPage() {
  const [roles, setRoles] = useState([])

  useEffect(() => {
    silentGet('/admin/roles', undefined, []).then((res) => {
      const list = Array.isArray(res) ? res : res?.list || []
      setRoles(list)
    })
  }, [])

  const roleOptions = roles.map((r) => ({ value: r.id, label: `${r.name}（${r.code}）` }))

  const resetPassword = async (record) => {
    try {
      const data = await request.put(`/admin/admins/${record.id}/password/reset`)
      Modal.success({
        title: '密码已重置',
        content: `管理员 ${record.username} 的新密码：${data?.password || data || '（见后端返回）'}`,
      })
    } catch {
      // 提示已统一处理
    }
  }

  const toggleStatus = async (record, reload) => {
    const next = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
    try {
      await request.put(`/admin/admins/${record.id}`, { status: next })
      message.success(next === 'ENABLED' ? '已启用' : '已停用')
      reload()
    } catch {
      // 提示已统一处理
    }
  }

  const isBuiltinAdmin = (r) => r.username === 'admin'

  return (
    <CrudPage
      title="管理员"
      listUrl="/admin/admins"
      columns={[
        { title: 'ID', dataIndex: 'id', width: 70 },
        { title: '账号', dataIndex: 'username', width: 130 },
        { title: '姓名', dataIndex: 'name', width: 130 },
        {
          title: '角色',
          dataIndex: 'roles',
          render: (v) =>
            Array.isArray(v) && v.length ? v.map((code) => <Tag key={code} color="blue">{code}</Tag>) : '-',
        },
        {
          title: '状态',
          dataIndex: 'status',
          width: 90,
          render: (v) => (v === 'DISABLED' ? <Tag color="red">已停用</Tag> : <Tag color="green">正常</Tag>),
        },
        { title: '创建时间', dataIndex: 'createTime', width: 170, render: (v) => v || '-' },
      ]}
      searchItems={keywordItem('username', '账号 / 姓名')}
      canDelete={false}
      toEditValues={(r) => ({
        name: r.name,
        status: r.status,
        // 列表返回的是角色 code，编辑时映射回 roleIds
        roleIds: roles.filter((role) => (r.roles || []).includes(role.code)).map((role) => role.id),
      })}
      rowActions={(record, reload) => (
        <>
          <Popconfirm title="确认重置该管理员密码？" onConfirm={() => resetPassword(record)}>
            <Button type="link" size="small">
              重置密码
            </Button>
          </Popconfirm>
          {!isBuiltinAdmin(record) && (
            <Popconfirm
              title={record.status === 'ENABLED' ? '停用后其 token 立即失效，确认？' : '确认启用？'}
              onConfirm={() => toggleStatus(record, reload)}
            >
              <Button type="link" size="small" danger={record.status === 'ENABLED'}>
                {record.status === 'ENABLED' ? '停用' : '启用'}
              </Button>
            </Popconfirm>
          )}
          {!isBuiltinAdmin(record) && (
            <Popconfirm
              title="确认删除该管理员？"
              onConfirm={async () => {
                try {
                  await request.delete(`/admin/admins/${record.id}`)
                  message.success('删除成功')
                  reload()
                } catch {
                  // 提示已统一处理
                }
              }}
            >
              <Button type="link" size="small" danger>
                删除
              </Button>
            </Popconfirm>
          )}
        </>
      )}
      formItems={(editing) => (
        <>
          {!editing && (
            <>
              <Form.Item
                name="username"
                label="登录账号"
                rules={[
                  { required: true, message: '请输入登录账号' },
                  { pattern: /^[a-zA-Z0-9]{4,20}$/, message: '4-20 位字母数字' },
                ]}
              >
                <Input maxLength={20} />
              </Form.Item>
              <Form.Item
                name="password"
                label="初始密码"
                rules={[
                  { required: true, message: '请输入初始密码' },
                  { pattern: /^(?=.*[a-zA-Z])(?=.*\d).{8,32}$/, message: '8-32 位，需含字母与数字' },
                ]}
              >
                <Input.Password maxLength={32} />
              </Form.Item>
            </>
          )}
          <Form.Item name="name" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}>
            <Input maxLength={20} />
          </Form.Item>
          <Form.Item name="roleIds" label="分配角色" rules={[{ required: true, message: '请至少选择一个角色' }]}>
            <Select mode="multiple" placeholder="选择角色" options={roleOptions} optionFilterProp="label" />
          </Form.Item>
          {editing && (
            <Form.Item name="status" label="状态">
              <Select
                options={[
                  { value: 'ENABLED', label: '正常' },
                  { value: 'DISABLED', label: '停用' },
                ]}
              />
            </Form.Item>
          )}
        </>
      )}
    />
  )
}
