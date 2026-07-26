import { useEffect, useState } from 'react'
import { Button, Form, Image, Input, InputNumber, Popconfirm, Select, Switch, Tag, Upload, message } from 'antd'
import { UploadOutlined } from '@ant-design/icons'
import CrudPage from '../../components/CrudPage'
import UploadImage from '../../components/UploadImage'
import request, { silentGet } from '../../utils/request'

const STATUS_MAP = {
  PENDING: { text: '待审核', color: 'processing' },
  ONLINE: { text: '已上架', color: 'success' },
  REJECTED: { text: '已驳回', color: 'error' },
  OFFLINE: { text: '已下架', color: 'default' },
}

const FALLBACK_COVER = '/img/singer/xu-song.jpg'

/** 音频文件上传（POST /api/admin/songs/upload），受控 value 为 objectKey */
function AudioUpload({ value, onChange, onMeta }) {
  const [uploading, setUploading] = useState(false)
  const [fileName, setFileName] = useState('')

  const customRequest = async ({ file, onSuccess, onError }) => {
    const formData = new FormData()
    formData.append('file', file)
    setUploading(true)
    try {
      const data = await request.post('/admin/songs/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      setFileName(file.name)
      onChange?.(data?.objectKey || '')
      onMeta?.(data || {})
      message.success('音频上传成功')
      onSuccess?.(data)
    } catch (e) {
      onError?.(e)
    } finally {
      setUploading(false)
    }
  }

  return (
    <Upload
      accept=".mp3,.flac,.wav"
      showUploadList={false}
      customRequest={customRequest}
      beforeUpload={(file) => {
        if (file.size / 1024 / 1024 > 200) {
          message.error('音频文件不能超过 200MB')
          return Upload.LIST_IGNORE
        }
        return true
      }}
    >
      <Button icon={<UploadOutlined />} loading={uploading}>
        {value ? `已上传：${fileName || value}` : '选择音频文件（mp3/flac/wav ≤200MB）'}
      </Button>
    </Upload>
  )
}

export default function MusicPage() {
  const [singers, setSingers] = useState([])
  const [albums, setAlbums] = useState([])
  const [categories, setCategories] = useState([])

  useEffect(() => {
    silentGet('/admin/singers', { pageNum: 1, pageSize: 100 }, null).then((res) =>
      setSingers(res?.list || (Array.isArray(res) ? res : []))
    )
    silentGet('/admin/albums', { pageNum: 1, pageSize: 100 }, null).then((res) =>
      setAlbums(res?.list || (Array.isArray(res) ? res : []))
    )
    silentGet('/admin/categories', undefined, []).then((res) => {
      const flat = []
      const walk = (nodes) =>
        (nodes || []).forEach((n) => {
          flat.push({ id: n.id, name: n.name })
          walk(n.children)
        })
      walk(Array.isArray(res) ? res : [])
      setCategories(flat)
    })
  }, [])

  const toggleShelf = async (record, reload) => {
    const next = record.status === 'ONLINE' ? 'OFFLINE' : 'ONLINE'
    try {
      await request.put(`/admin/songs/${record.id}/status`, { status: next })
      message.success(next === 'ONLINE' ? '已上架' : '已下架')
      reload()
    } catch {
      // 提示已统一处理
    }
  }

  return (
    <CrudPage
      title="音乐"
      listUrl="/admin/songs"
      modalWidth={680}
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
        { title: '歌名', dataIndex: 'name', ellipsis: true },
        { title: '歌手', dataIndex: 'singerName', width: 110, ellipsis: true },
        { title: '专辑', dataIndex: 'albumName', width: 120, ellipsis: true },
        { title: '分类', dataIndex: 'categoryName', width: 90 },
        {
          title: '音质',
          dataIndex: 'quality',
          width: 90,
          render: (v) => (v === 'lossless' ? <Tag color="purple">无损</Tag> : <Tag>标准</Tag>),
        },
        {
          title: 'VIP',
          dataIndex: 'vip',
          width: 70,
          render: (v) => (v ? <Tag color="gold">VIP</Tag> : '-'),
        },
        { title: '价格', dataIndex: 'price', width: 80, render: (v) => (v && v !== '0.00' ? `¥${v}` : '免费') },
        { title: '播放量', dataIndex: 'playCount', width: 90, render: (v) => v ?? 0 },
        {
          title: '状态',
          dataIndex: 'status',
          width: 90,
          render: (v, r) => {
            const s = STATUS_MAP[v] || { text: v || '-', color: 'default' }
            return (
              <Tag color={s.color} title={r.rejectReason ? `驳回理由：${r.rejectReason}` : undefined}>
                {s.text}
              </Tag>
            )
          },
        },
      ]}
      searchItems={
        <>
          <Form.Item name="name">
            <Input allowClear placeholder="歌曲名称" style={{ width: 180 }} />
          </Form.Item>
          <Form.Item name="categoryId">
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              placeholder="分类"
              style={{ width: 150 }}
              options={categories.map((c) => ({ value: c.id, label: c.name }))}
            />
          </Form.Item>
          <Form.Item name="status">
            <Select
              allowClear
              placeholder="状态"
              style={{ width: 130 }}
              options={Object.entries(STATUS_MAP).map(([value, s]) => ({ value, label: s.text }))}
            />
          </Form.Item>
        </>
      }
      rowActions={(record, reload) =>
        ['ONLINE', 'OFFLINE'].includes(record.status) && (
          <Popconfirm
            title={record.status === 'ONLINE' ? '确认下架该曲目？' : '确认上架该曲目？'}
            onConfirm={() => toggleShelf(record, reload)}
          >
            <Button type="link" size="small">
              {record.status === 'ONLINE' ? '下架' : '上架'}
            </Button>
          </Popconfirm>
        )
      }
      formItems={(editing) => (
        <>
          {!editing && (
            <Form.Item name="objectKey" label="音频文件" rules={[{ required: true, message: '请上传音频文件' }]}>
              <AudioUpload />
            </Form.Item>
          )}
          <Form.Item name="name" label="歌曲名称" rules={[{ required: true, message: '请输入歌曲名称' }]}>
            <Input maxLength={50} placeholder="歌曲名称" />
          </Form.Item>
          <Form.Item name="singerId" label="歌手" rules={[{ required: true, message: '请选择歌手' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              placeholder="选择歌手"
              options={singers.map((s) => ({ value: s.id, label: s.name }))}
            />
          </Form.Item>
          <Form.Item name="albumId" label="所属专辑">
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              placeholder="选择专辑（可空）"
              options={albums.map((a) => ({ value: a.id, label: a.name }))}
            />
          </Form.Item>
          <Form.Item name="categoryId" label="分类" rules={[{ required: true, message: '请选择分类' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              placeholder="选择分类"
              options={categories.map((c) => ({ value: c.id, label: c.name }))}
            />
          </Form.Item>
          <Form.Item name="cover" label="封面">
            <UploadImage tip="上传封面" />
          </Form.Item>
          <Form.Item name="quality" label="音质" initialValue="standard">
            <Select
              options={[
                { value: 'standard', label: '标准' },
                { value: 'lossless', label: '无损（Hi-Res）' },
              ]}
            />
          </Form.Item>
          <Form.Item name="vip" label="VIP 专享" valuePropName="checked" initialValue={false}>
            <Switch />
          </Form.Item>
          <Form.Item name="original" label="原创" valuePropName="checked" initialValue={false}>
            <Switch />
          </Form.Item>
          <Form.Item name="price" label="单曲价格（元，0 为免费）" initialValue={0}>
            <InputNumber min={0} precision={2} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="lyric" label="歌词（LRC 文本，纯音乐可空）">
            <Input.TextArea rows={4} placeholder="[00:00.00] ..." />
          </Form.Item>
        </>
      )}
      beforeSubmit={(values) => ({
        ...values,
        price: values.price != null ? Number(values.price).toFixed(2) : undefined,
      })}
    />
  )
}
