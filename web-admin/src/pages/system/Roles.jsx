import { useCallback, useEffect, useState } from 'react'
import { Button, Form, Input, Modal, Spin, Tree, message } from 'antd'
import { SafetyCertificateOutlined } from '@ant-design/icons'
import CrudPage, { keywordItem } from '../../components/CrudPage'
import request, { silentGet } from '../../utils/request'

/** 权限树节点转 antd Tree data */
function toTreeData(nodes) {
  return (nodes || []).map((n) => ({
    key: n.id,
    title: `${n.name}${n.perm ? `（${n.perm}）` : ''}`,
    children: n.children?.length ? toTreeData(n.children) : undefined,
  }))
}

export default function RolesPage() {
  const [grantTarget, setGrantTarget] = useState(null)
  const [permTree, setPermTree] = useState([])
  const [checkedKeys, setCheckedKeys] = useState([])
  const [halfKeys, setHalfKeys] = useState([])
  const [treeLoading, setTreeLoading] = useState(false)
  const [saving, setSaving] = useState(false)

  const loadPermTree = useCallback(async () => {
    setTreeLoading(true)
    const res = await silentGet('/admin/permissions', undefined, [])
    setPermTree(Array.isArray(res) ? res : [])
    setTreeLoading(false)
  }, [])

  useEffect(() => {
    loadPermTree()
  }, [loadPermTree])

  const openGrant = async (record) => {
    setGrantTarget(record)
    // 已有权限：优先取记录字段，缺失时置空由管理员重新勾选
    const existed = record.permissionIds || record.permIds || []
    setCheckedKeys(existed)
    setHalfKeys([])
  }

  const onGrant = async () => {
    setSaving(true)
    try {
      await request.put(`/admin/roles/${grantTarget.id}/permissions`, {
        permissionIds: [...checkedKeys, ...halfKeys],
      })
      message.success('权限已保存，该角色下管理员刷新后生效')
      setGrantTarget(null)
    } catch {
      // 提示已统一处理
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <CrudPage
        title="角色"
        listUrl="/admin/roles"
        pagination={false}
        transformList={(data) => (Array.isArray(data) ? data : data?.list || [])}
        columns={[
          { title: 'ID', dataIndex: 'id', width: 70 },
          { title: '角色编码', dataIndex: 'code', width: 150 },
          { title: '角色名称', dataIndex: 'name', width: 150 },
          { title: '备注', dataIndex: 'remark', ellipsis: true, render: (v) => v || '-' },
          { title: '管理员数', dataIndex: 'adminCount', width: 100, render: (v) => v ?? 0 },
        ]}
        searchItems={keywordItem('name', '角色名称')}
        rowActions={(record) => (
          <Button type="link" size="small" icon={<SafetyCertificateOutlined />} onClick={() => openGrant(record)}>
            权限配置
          </Button>
        )}
        formItems={
          <>
            <Form.Item
              name="code"
              label="角色编码"
              rules={[
                { required: true, message: '请输入角色编码' },
                { pattern: /^[A-Z_]{2,30}$/, message: '大写字母与下划线，如 AUDITOR' },
              ]}
            >
              <Input maxLength={30} placeholder="如 AUDITOR" />
            </Form.Item>
            <Form.Item name="name" label="角色名称" rules={[{ required: true, message: '请输入角色名称' }]}>
              <Input maxLength={20} placeholder="如 审核员" />
            </Form.Item>
            <Form.Item name="remark" label="备注">
              <Input.TextArea rows={3} maxLength={200} />
            </Form.Item>
          </>
        }
      />

      <Modal
        title={`权限配置：${grantTarget?.name || ''}`}
        open={!!grantTarget}
        onOk={onGrant}
        onCancel={() => setGrantTarget(null)}
        confirmLoading={saving}
        destroyOnHidden
      >
        <Spin spinning={treeLoading}>
          {permTree.length ? (
            <Tree
              checkable
              defaultExpandAll
              checkedKeys={checkedKeys}
              onCheck={(keys, info) => {
                setCheckedKeys(keys)
                setHalfKeys(info.halfCheckedKeys || [])
              }}
              treeData={toTreeData(permTree)}
              style={{ maxHeight: 420, overflow: 'auto', marginTop: 12 }}
            />
          ) : (
            <div style={{ color: '#999', padding: '24px 0', textAlign: 'center' }}>
              权限树加载失败或为空，请先在"权限管理"中维护权限
            </div>
          )}
        </Spin>
      </Modal>
    </>
  )
}
