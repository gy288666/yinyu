import { useEffect, useState } from 'react'
import { Form, Input, InputNumber, Select, Tag } from 'antd'
import CrudPage from '../../components/CrudPage'
import { silentGet } from '../../utils/request'

/** 权限管理：树形展示菜单/按钮权限，CRUD 走 /api/admin/permissions */
export default function PermissionsPage() {
  const [menuOptions, setMenuOptions] = useState([])

  useEffect(() => {
    silentGet('/admin/permissions', undefined, []).then((res) => {
      const opts = []
      const walk = (nodes, depth = 0) =>
        (nodes || []).forEach((n) => {
          if (n.type === 'MENU') {
            opts.push({ value: n.id, label: `${'　'.repeat(depth)}${n.name}` })
            walk(n.children, depth + 1)
          }
        })
      walk(Array.isArray(res) ? res : [])
      setMenuOptions(opts)
    })
  }, [])

  return (
    <CrudPage
      title="权限"
      listUrl="/admin/permissions"
      pagination={false}
      transformList={(data) => (Array.isArray(data) ? data : [])}
      columns={[
        { title: '名称', dataIndex: 'name' },
        {
          title: '类型',
          dataIndex: 'type',
          width: 90,
          render: (v) => (v === 'MENU' ? <Tag color="blue">菜单</Tag> : <Tag color="purple">按钮</Tag>),
        },
        { title: '权限标识', dataIndex: 'perm', width: 200, render: (v) => (v ? <code>{v}</code> : '-') },
        { title: '路由', dataIndex: 'path', width: 150, render: (v) => v || '-' },
        { title: '排序', dataIndex: 'sort', width: 80, render: (v) => v ?? 0 },
      ]}
      formItems={
        <>
          <Form.Item name="type" label="类型" rules={[{ required: true, message: '请选择类型' }]} initialValue="MENU">
            <Select
              options={[
                { value: 'MENU', label: '菜单' },
                { value: 'BUTTON', label: '按钮' },
              ]}
            />
          </Form.Item>
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input maxLength={20} />
          </Form.Item>
          <Form.Item name="parentId" label="上级菜单（留空为顶级）">
            <Select allowClear placeholder="留空为顶级" options={menuOptions} />
          </Form.Item>
          <Form.Item
            name="perm"
            label="权限标识"
            rules={[{ required: true, message: '请输入权限标识，如 music:audit:pass' }]}
          >
            <Input placeholder="如 music:audit:pass" />
          </Form.Item>
          <Form.Item name="path" label="路由地址（菜单类型）">
            <Input placeholder="如 /music" />
          </Form.Item>
          <Form.Item name="icon" label="图标标识">
            <Input placeholder="如 customer-service" />
          </Form.Item>
          <Form.Item name="sort" label="排序" initialValue={0}>
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
        </>
      }
    />
  )
}
