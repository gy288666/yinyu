import { Avatar, Form, Input, Select, Tag } from 'antd'
import { UserOutlined } from '@ant-design/icons'
import CrudPage, { keywordItem } from '../../components/CrudPage'
import UploadImage from '../../components/UploadImage'

const AREA_OPTIONS = ['内地', '欧美', '日韩', '其他'].map((v) => ({ value: v, label: v }))
const TYPE_MAP = { 1: '男歌手', 2: '女歌手', 3: '组合', 4: '厂牌' }

const FALLBACK_AVATARS = [
  '/img/singer/chen-yixun.jpg',
  '/img/singer/deng-ziqi.jpg',
  '/img/singer/li-ronghao.jpg',
  '/img/singer/wang-lihong.jpg',
  '/img/singer/zhou-jielun.jpg',
]

export default function SingerPage() {
  return (
    <CrudPage
      title="歌手"
      listUrl="/admin/singers"
      columns={[
        { title: 'ID', dataIndex: 'id', width: 70 },
        {
          title: '头像',
          dataIndex: 'avatar',
          width: 70,
          render: (v, r) => (
            <Avatar
              size={40}
              src={v || FALLBACK_AVATARS[(r.id || 0) % FALLBACK_AVATARS.length]}
              icon={<UserOutlined />}
            />
          ),
        },
        { title: '姓名', dataIndex: 'name', width: 140 },
        { title: '地区', dataIndex: 'area', width: 90, render: (v) => v || '-' },
        {
          title: '类型',
          dataIndex: 'type',
          width: 90,
          render: (v) => (TYPE_MAP[v] ? <Tag>{TYPE_MAP[v]}</Tag> : '-'),
        },
        { title: '歌曲数', dataIndex: 'songCount', width: 90, render: (v) => v ?? 0 },
        { title: '简介', dataIndex: 'intro', ellipsis: true, render: (v) => v || '-' },
      ]}
      searchItems={
        <>
          {keywordItem('name', '歌手姓名')}
          <Form.Item name="area">
            <Select allowClear placeholder="地区" style={{ width: 120 }} options={AREA_OPTIONS} />
          </Form.Item>
          <Form.Item name="type">
            <Select
              allowClear
              placeholder="类型"
              style={{ width: 120 }}
              options={Object.entries(TYPE_MAP).map(([value, label]) => ({ value: Number(value), label }))}
            />
          </Form.Item>
        </>
      }
      formItems={
        <>
          <Form.Item name="name" label="歌手姓名" rules={[{ required: true, message: '请输入歌手姓名' }]}>
            <Input maxLength={30} />
          </Form.Item>
          <Form.Item name="avatar" label="头像">
            <UploadImage action="/admin/singers/upload" tip="上传头像" />
          </Form.Item>
          <Form.Item name="area" label="地区" rules={[{ required: true, message: '请选择地区' }]}>
            <Select options={AREA_OPTIONS} placeholder="选择地区" />
          </Form.Item>
          <Form.Item name="type" label="类型" rules={[{ required: true, message: '请选择类型' }]}>
            <Select
              placeholder="选择类型"
              options={Object.entries(TYPE_MAP).map(([value, label]) => ({ value: Number(value), label }))}
            />
          </Form.Item>
          <Form.Item name="intro" label="简介">
            <Input.TextArea rows={3} maxLength={500} showCount />
          </Form.Item>
        </>
      }
    />
  )
}
