import { DatePicker, Form, Input, InputNumber, Select, Tag } from 'antd'
import dayjs from 'dayjs'
import CrudPage, { keywordItem } from '../../components/CrudPage'

const LICENSE_OPTIONS = [
  { value: 'BUYOUT', label: '买断' },
  { value: 'EXCLUSIVE', label: '独家授权' },
  { value: 'NON_EXCLUSIVE', label: '非独家授权' },
]

export default function CopyrightPage() {
  return (
    <CrudPage
      title="版权"
      listUrl="/admin/copyrights"
      columns={[
        { title: 'ID', dataIndex: 'id', width: 70 },
        { title: '歌曲ID', dataIndex: 'songId', width: 90 },
        { title: '歌曲名称', dataIndex: 'songName', ellipsis: true },
        { title: '版权方', dataIndex: 'owner', width: 150, ellipsis: true },
        {
          title: '授权类型',
          dataIndex: 'licenseType',
          width: 110,
          render: (v) => LICENSE_OPTIONS.find((o) => o.value === v)?.label || v || '-',
        },
        { title: '生效日期', dataIndex: 'startDate', width: 110 },
        { title: '到期日期', dataIndex: 'endDate', width: 110 },
        {
          title: '状态',
          dataIndex: 'expiringSoon',
          width: 100,
          render: (v, r) => {
            if (r.endDate && dayjs(r.endDate).isBefore(dayjs(), 'day')) return <Tag color="red">已到期</Tag>
            return v ? <Tag color="orange">即将到期</Tag> : <Tag color="green">有效</Tag>
          },
        },
        {
          title: '授权文件',
          dataIndex: 'fileUrl',
          width: 100,
          render: (v) =>
            v ? (
              <a href={v} target="_blank" rel="noreferrer">
                查看
              </a>
            ) : (
              '-'
            ),
        },
      ]}
      searchItems={
        <>
          {keywordItem('songName', '歌曲名称')}
          <Form.Item name="expireBefore">
            <DatePicker placeholder="到期日早于" style={{ width: 160 }} />
          </Form.Item>
        </>
      }
      toEditValues={(r) => ({
        ...r,
        startDate: r.startDate ? dayjs(r.startDate) : null,
        endDate: r.endDate ? dayjs(r.endDate) : null,
      })}
      beforeSubmit={(values) => ({
        ...values,
        startDate: values.startDate ? values.startDate.format('YYYY-MM-DD') : undefined,
        endDate: values.endDate ? values.endDate.format('YYYY-MM-DD') : undefined,
        expireBefore: undefined,
      })}
      formItems={
        <>
          <Form.Item name="songId" label="歌曲 ID" rules={[{ required: true, message: '请输入歌曲 ID' }]}>
            <InputNumber min={1} style={{ width: '100%' }} placeholder="关联的歌曲 ID" />
          </Form.Item>
          <Form.Item name="owner" label="版权方" rules={[{ required: true, message: '请输入版权方' }]}>
            <Input maxLength={50} />
          </Form.Item>
          <Form.Item name="licenseType" label="授权类型" rules={[{ required: true, message: '请选择授权类型' }]}>
            <Select options={LICENSE_OPTIONS} placeholder="选择授权类型" />
          </Form.Item>
          <Form.Item name="startDate" label="生效日期" rules={[{ required: true, message: '请选择生效日期' }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="endDate" label="到期日期" rules={[{ required: true, message: '请选择到期日期' }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="fileUrl" label="授权文件 URL">
            <Input placeholder="授权合同文件地址" />
          </Form.Item>
        </>
      }
    />
  )
}
