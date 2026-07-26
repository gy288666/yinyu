import { useCallback, useEffect, useMemo, useState } from 'react'
import { Button, Card, Form, Input, Modal, Popconfirm, Space, Table, message } from 'antd'
import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import request from '../utils/request'

/**
 * 标准 CRUD 页面模板（AntD Table + 搜索 Form + 新增/编辑 Modal）
 *
 * props:
 * - title            页面标题
 * - listUrl          列表接口（GET，分页 pageNum/pageSize）
 * - createUrl        新增接口（POST），缺省为 listUrl
 * - updateUrl        (record) => url，编辑接口（PUT），缺省 `${listUrl}/${id}`
 * - deleteUrl        (record) => url，删除接口（DELETE），缺省 `${listUrl}/${id}`
 * - rowKey           默认 'id'
 * - columns          表格列（不含操作列）
 * - searchItems      搜索区 <Form.Item> 数组（name 即查询参数）
 * - formItems        弹窗表单内容：ReactNode 或 (editingRecord) => ReactNode
 * - toEditValues     (record) => 表单初始值，缺省原样
 * - beforeSubmit     (values, editingRecord) => 提交前转换
 * - pagination       false 时不分页（列表接口直接返回数组）
 * - canCreate/canEdit/canDelete   开关，默认 true
 * - rowActions       (record, reload) => ReactNode 额外行内操作
 * - toolbarExtra     (reload) => ReactNode 工具栏额外按钮
 * - modalWidth       弹窗宽度
 * - transformList    (data) => rows，非标准响应转换
 * - defaultParams    追加到每次查询的固定参数
 */
export default function CrudPage({
  title,
  listUrl,
  createUrl,
  updateUrl,
  deleteUrl,
  rowKey = 'id',
  columns = [],
  searchItems = null,
  formItems = null,
  toEditValues,
  beforeSubmit,
  pagination = true,
  canCreate = true,
  canEdit = true,
  canDelete = true,
  rowActions,
  toolbarExtra,
  modalWidth = 560,
  transformList,
  defaultParams,
}) {
  const [searchForm] = Form.useForm()
  const [modalForm] = Form.useForm()
  const [rows, setRows] = useState([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState(false)
  const [page, setPage] = useState({ pageNum: 1, pageSize: 10 })
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const load = useCallback(
    async (pageOverride) => {
      setLoading(true)
      setLoadError(false)
      const p = pageOverride || page
      try {
        const params = {
          ...(defaultParams || {}),
          ...searchForm.getFieldsValue(),
        }
        Object.keys(params).forEach((k) => {
          const v = params[k]
          if (v === '' || v === undefined || v === null) {
            delete params[k]
          } else if (v && typeof v === 'object' && typeof v.format === 'function') {
            // dayjs 值统一格式化为后端约定时间格式
            params[k] = v.format('YYYY-MM-DD HH:mm:ss')
          }
        })
        if (pagination) {
          params.pageNum = p.pageNum
          params.pageSize = p.pageSize
        }
        const data = await request.get(listUrl, { params })
        if (transformList) {
          const r = transformList(data)
          setRows(Array.isArray(r) ? r : [])
          setTotal(Array.isArray(r) ? r.length : 0)
        } else if (Array.isArray(data)) {
          setRows(data)
          setTotal(data.length)
        } else if (data && Array.isArray(data.list)) {
          setRows(data.list)
          setTotal(Number(data.total) || data.list.length)
        } else {
          setRows([])
          setTotal(0)
        }
      } catch (e) {
        // 错误提示已由拦截器统一弹出，这里保证不白屏；403 单独标记避免误导为服务故障
        setRows([])
        setTotal(0)
        setLoadError(e?.response?.status === 403 ? 'perm' : true)
      } finally {
        setLoading(false)
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [listUrl, page.pageNum, page.pageSize]
  )

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [load])

  const reload = useCallback(() => load(), [load])

  const onSearch = () => {
    const next = { ...page, pageNum: 1 }
    setPage(next)
    load(next)
  }

  const onReset = () => {
    searchForm.resetFields()
    const next = { ...page, pageNum: 1 }
    setPage(next)
    load(next)
  }

  const openCreate = () => {
    setEditing(null)
    modalForm.resetFields()
    setModalOpen(true)
  }

  const openEdit = (record) => {
    setEditing(record)
    modalForm.resetFields()
    modalForm.setFieldsValue(toEditValues ? toEditValues(record) : record)
    setModalOpen(true)
  }

  const onSubmit = async () => {
    let values
    try {
      values = await modalForm.validateFields()
    } catch {
      return
    }
    if (beforeSubmit) values = beforeSubmit(values, editing) || values
    setSubmitting(true)
    try {
      if (editing) {
        const url = updateUrl ? updateUrl(editing) : `${listUrl}/${editing[rowKey]}`
        await request.put(url, values)
        message.success('修改成功')
      } else {
        await request.post(createUrl || listUrl, values)
        message.success('新增成功')
      }
      setModalOpen(false)
      reload()
    } catch {
      // 提示已统一处理
    } finally {
      setSubmitting(false)
    }
  }

  const onDelete = async (record) => {
    try {
      const url = deleteUrl ? deleteUrl(record) : `${listUrl}/${record[rowKey]}`
      await request.delete(url)
      message.success('删除成功')
      reload()
    } catch {
      // 提示已统一处理
    }
  }

  const actionColumn = useMemo(() => {
    if (!canEdit && !canDelete && !rowActions) return null
    return {
      title: '操作',
      key: '__action',
      fixed: 'right',
      width: rowActions ? 220 : 140,
      render: (_, record) => (
        <Space size={4} wrap>
          {canEdit && (
            <Button type="link" size="small" onClick={() => openEdit(record)}>
              编辑
            </Button>
          )}
          {rowActions && rowActions(record, reload)}
          {canDelete && (
            <Popconfirm title="确认删除该记录？" onConfirm={() => onDelete(record)}>
              <Button type="link" size="small" danger>
                删除
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [canEdit, canDelete, rowActions, reload])

  const tableColumns = actionColumn ? [...columns, actionColumn] : columns

  return (
    <div>
      <Card variant="borderless" style={{ marginBottom: 16 }} styles={{ body: { paddingBottom: 0 } }}>
        <Form form={searchForm} layout="inline" onFinish={onSearch} style={{ rowGap: 12, marginBottom: 16 }}>
          {searchItems}
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                查询
              </Button>
              <Button onClick={onReset}>重置</Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card
        variant="borderless"
        title={title}
        extra={
          <Space>
            {toolbarExtra && toolbarExtra(reload)}
            <Button icon={<ReloadOutlined />} onClick={reload}>
              刷新
            </Button>
            {canCreate && (
              <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
                新增
              </Button>
            )}
          </Space>
        }
      >
        <Table
          rowKey={rowKey}
          size="middle"
          loading={loading}
          columns={tableColumns}
          dataSource={rows}
          scroll={{ x: 'max-content' }}
          locale={loadError ? { emptyText: loadError === 'perm' ? '当前账号没有该模块的访问权限' : '数据加载失败，请检查后端服务后点击"刷新"重试' } : undefined}
          pagination={
            pagination
              ? {
                  current: page.pageNum,
                  pageSize: page.pageSize,
                  total,
                  showSizeChanger: true,
                  showTotal: (t) => `共 ${t} 条`,
                  onChange: (pageNum, pageSize) => {
                    const next = { pageNum, pageSize }
                    setPage(next)
                  },
                }
              : false
          }
        />
      </Card>

      <Modal
        title={editing ? `编辑${title}` : `新增${title}`}
        open={modalOpen}
        onOk={onSubmit}
        onCancel={() => setModalOpen(false)}
        confirmLoading={submitting}
        width={modalWidth}
        destroyOnHidden
      >
        <Form form={modalForm} layout="vertical" style={{ marginTop: 16 }}>
          {typeof formItems === 'function' ? formItems(editing) : formItems}
        </Form>
      </Modal>
    </div>
  )
}

/** 常用搜索项：关键字输入 */
export function keywordItem(name, placeholder) {
  return (
    <Form.Item name={name} key={name}>
      <Input allowClear placeholder={placeholder} style={{ width: 200 }} />
    </Form.Item>
  )
}
