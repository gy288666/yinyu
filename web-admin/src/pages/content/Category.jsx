import { useEffect, useState } from 'react'
import { Form, Input, InputNumber, Select, Switch, Tag } from 'antd'
import CrudPage from '../../components/CrudPage'
import { silentGet } from '../../utils/request'

/** 分类管理：树形展示（Table treeData），CRUD 走 /api/admin/categories */
export default function CategoryPage() {
  const [parents, setParents] = useState([])

  useEffect(() => {
    silentGet('/admin/categories', undefined, []).then((res) =>
      setParents((Array.isArray(res) ? res : []).map((n) => ({ value: n.id, label: n.name })))
    )
  }, [])

  return (
    <CrudPage
      title="分类"
      listUrl="/admin/categories"
      pagination={false}
      transformList={(data) => (Array.isArray(data) ? data : [])}
      columns={[
        { title: '分类名称', dataIndex: 'name' },
        { title: 'ID', dataIndex: 'id', width: 90 },
        { title: '排序', dataIndex: 'sort', width: 90, render: (v) => v ?? 0 },
        {
          title: '状态',
          dataIndex: 'enabled',
          width: 90,
          render: (v) => (v === false ? <Tag>停用</Tag> : <Tag color="success">启用</Tag>),
        },
      ]}
      toEditValues={(r) => ({ name: r.name, parentId: r.parentId ?? null, sort: r.sort ?? 0, enabled: r.enabled !== false })}
      formItems={
        <>
          <Form.Item name="name" label="分类名称" rules={[{ required: true, message: '请输入分类名称' }]}>
            <Input maxLength={20} />
          </Form.Item>
          <Form.Item name="parentId" label="上级分类（留空为一级分类）">
            <Select allowClear placeholder="留空为一级分类" options={parents} />
          </Form.Item>
          <Form.Item name="sort" label="排序（越小越靠前）" initialValue={0}>
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked" initialValue={true}>
            <Switch />
          </Form.Item>
        </>
      }
    />
  )
}
