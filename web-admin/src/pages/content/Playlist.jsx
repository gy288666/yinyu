import { Form, Image, Input, Select, Switch, Tag } from 'antd'
import CrudPage, { keywordItem } from '../../components/CrudPage'
import UploadImage from '../../components/UploadImage'

const FALLBACK_COVER = '/img/singer/zhang-bichen.jpg'
const TAG_OPTIONS = ['学习', '助眠', '钢琴', '电子', '白噪音', '冥想', '轻音乐', '专注', '放松'].map((v) => ({
  value: v,
  label: v,
}))

export default function PlaylistPage() {
  return (
    <CrudPage
      title="官方歌单"
      listUrl="/admin/playlists"
      columns={[
        { title: 'ID', dataIndex: 'id', width: 70 },
        {
          title: '封面',
          dataIndex: 'cover',
          width: 76,
          render: (v) => (
            <Image
              src={v || FALLBACK_COVER}
              fallback={FALLBACK_COVER}
              width={44}
              height={44}
              style={{ borderRadius: 6, objectFit: 'cover' }}
              preview={!!v}
            />
          ),
        },
        { title: '歌单标题', dataIndex: 'title', ellipsis: true },
        {
          title: '标签',
          dataIndex: 'tags',
          width: 200,
          render: (tags) =>
            Array.isArray(tags) && tags.length ? tags.map((t) => <Tag key={t}>{t}</Tag>) : '-',
        },
        { title: '歌曲数', dataIndex: 'songCount', width: 90, render: (v) => v ?? 0 },
        { title: '播放量', dataIndex: 'playCount', width: 100, render: (v) => v ?? 0 },
        {
          title: '推荐',
          dataIndex: 'recommended',
          width: 80,
          render: (v) => (v ? <Tag color="blue">推荐</Tag> : '-'),
        },
        {
          title: '置顶',
          dataIndex: 'top',
          width: 80,
          render: (v) => (v ? <Tag color="orange">置顶</Tag> : '-'),
        },
      ]}
      searchItems={keywordItem('title', '歌单标题')}
      formItems={
        <>
          <Form.Item name="title" label="歌单标题" rules={[{ required: true, message: '请输入歌单标题' }]}>
            <Input maxLength={40} />
          </Form.Item>
          <Form.Item name="cover" label="封面">
            <UploadImage tip="上传封面" />
          </Form.Item>
          <Form.Item name="tags" label="标签">
            <Select mode="tags" placeholder="选择或输入标签" options={TAG_OPTIONS} />
          </Form.Item>
          <Form.Item name="intro" label="歌单简介">
            <Input.TextArea rows={3} maxLength={300} showCount />
          </Form.Item>
          <Form.Item name="recommended" label="首页推荐" valuePropName="checked" initialValue={false}>
            <Switch />
          </Form.Item>
          <Form.Item name="top" label="置顶" valuePropName="checked" initialValue={false}>
            <Switch />
          </Form.Item>
        </>
      }
    />
  )
}
