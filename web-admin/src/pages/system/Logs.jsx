import { DatePicker, Form, Input, Tag, Typography } from 'antd'
import CrudPage from '../../components/CrudPage'

export default function LogsPage() {
  return (
    <CrudPage
      title="操作日志"
      listUrl="/admin/logs"
      canCreate={false}
      canEdit={false}
      canDelete={false}
      columns={[
        { title: 'ID', dataIndex: 'id', width: 80 },
        { title: '操作人', dataIndex: 'adminName', width: 110 },
        { title: '模块', dataIndex: 'module', width: 110 },
        { title: '操作', dataIndex: 'action', width: 100 },
        {
          title: '请求参数',
          dataIndex: 'params',
          ellipsis: true,
          render: (v) =>
            v ? (
              <Typography.Text code copyable={{ text: v }} style={{ fontSize: 12 }}>
                {v.length > 60 ? `${v.slice(0, 60)}...` : v}
              </Typography.Text>
            ) : (
              '-'
            ),
        },
        { title: 'IP', dataIndex: 'ip', width: 120 },
        { title: '耗时(ms)', dataIndex: 'costMs', width: 90, render: (v) => v ?? '-' },
        {
          title: '结果',
          dataIndex: 'success',
          width: 80,
          render: (v) => (v === false ? <Tag color="red">失败</Tag> : <Tag color="green">成功</Tag>),
        },
        { title: '时间', dataIndex: 'createTime', width: 170 },
      ]}
      searchItems={
        <>
          <Form.Item name="adminName">
            <Input allowClear placeholder="操作人" style={{ width: 140 }} />
          </Form.Item>
          <Form.Item name="module">
            <Input allowClear placeholder="模块" style={{ width: 140 }} />
          </Form.Item>
          <Form.Item name="startTime">
            <DatePicker showTime placeholder="开始时间" />
          </Form.Item>
          <Form.Item name="endTime">
            <DatePicker showTime placeholder="结束时间" />
          </Form.Item>
        </>
      }
      defaultParams={{}}
    />
  )
}
