import { useEffect, useState } from 'react'
import { Button, Card, Col, Form, Input, InputNumber, Modal, Row, Statistic, Tag, message } from 'antd'
import { CrownOutlined } from '@ant-design/icons'
import CrudPage from '../../components/CrudPage'
import request, { silentGet } from '../../utils/request'

/** 会员管理：VIP 用户列表 + 套餐展示 + 调整时长 */
export default function VipPage() {
  const [plans, setPlans] = useState([])
  const [adjustTarget, setAdjustTarget] = useState(null)
  const [adjustForm] = Form.useForm()
  const [reloadFlag, setReloadFlag] = useState(0)

  useEffect(() => {
    // 套餐展示：优先后台套餐接口，缺失时回落门户公开接口
    silentGet('/admin/vip-plans', undefined, null).then((res) => {
      const list = Array.isArray(res) ? res : res?.list
      if (list?.length) {
        setPlans(list)
      } else {
        silentGet('/vip/plans', undefined, []).then((r) => setPlans(Array.isArray(r) ? r : []))
      }
    })
  }, [])

  const onAdjust = async () => {
    let values
    try {
      values = await adjustForm.validateFields()
    } catch {
      return
    }
    try {
      await request.put(`/admin/vips/${adjustTarget.id ?? adjustTarget.userId}`, { deltaDays: values.deltaDays })
      message.success('VIP 时长已调整')
      setAdjustTarget(null)
      adjustForm.resetFields()
      setReloadFlag((f) => f + 1)
    } catch {
      // 提示已统一处理
    }
  }

  return (
    <div>
      <Card variant="borderless" title="会员套餐" style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          {plans.length ? (
            plans.map((p) => (
              <Col key={p.id} xs={24} sm={8}>
                <Card size="small" style={{ textAlign: 'center', borderColor: '#ffe58f', background: '#fffbe6' }}>
                  <CrownOutlined style={{ fontSize: 22, color: '#faad14' }} />
                  <div style={{ fontWeight: 600, margin: '6px 0' }}>{p.name}</div>
                  <Statistic value={p.price} prefix="¥" valueStyle={{ fontSize: 20, color: '#fa8c16' }} />
                  <div style={{ color: '#999', fontSize: 12, marginTop: 4 }}>
                    {p.days} 天{p.originPrice ? ` · 原价 ¥${p.originPrice}` : ''}
                  </div>
                </Card>
              </Col>
            ))
          ) : (
            <Col span={24} style={{ color: '#999' }}>
              暂无套餐数据（后端接口未实现或无数据）
            </Col>
          )}
        </Row>
      </Card>

      <CrudPage
        key={reloadFlag}
        title="VIP 用户"
        listUrl="/admin/vips"
        canCreate={false}
        canEdit={false}
        canDelete={false}
        columns={[
          { title: '用户ID', dataIndex: 'id', width: 90, render: (v, r) => v ?? r.userId },
          { title: '用户名', dataIndex: 'username', width: 140 },
          { title: '昵称', dataIndex: 'nickname', width: 140, ellipsis: true },
          {
            title: 'VIP 状态',
            dataIndex: 'vip',
            width: 100,
            render: () => <Tag color="gold">VIP</Tag>,
          },
          { title: '到期时间', dataIndex: 'vipExpireAt', width: 180, render: (v) => v || '-' },
          { title: '开通时间', dataIndex: 'createTime', width: 180, render: (v) => v || '-' },
        ]}
        searchItems={
          <Form.Item name="username">
            <Input allowClear placeholder="用户名" style={{ width: 160 }} />
          </Form.Item>
        }
        rowActions={(record) => (
          <Button type="link" size="small" onClick={() => setAdjustTarget(record)}>
            调整时长
          </Button>
        )}
      />

      <Modal
        title={`调整 VIP 时长：${adjustTarget?.username || ''}`}
        open={!!adjustTarget}
        onOk={onAdjust}
        onCancel={() => {
          setAdjustTarget(null)
          adjustForm.resetFields()
        }}
        destroyOnHidden
      >
        <Form form={adjustForm} layout="vertical" style={{ marginTop: 12 }}>
          <Form.Item
            name="deltaDays"
            label="调整天数（正数增加、负数扣减）"
            rules={[{ required: true, message: '请输入调整天数' }]}
          >
            <InputNumber style={{ width: '100%' }} placeholder="如 31 或 -7" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
