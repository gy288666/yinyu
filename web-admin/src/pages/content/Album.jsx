import { useEffect, useState } from 'react'
import { DatePicker, Form, Image, Input, Select } from 'antd'
import dayjs from 'dayjs'
import CrudPage, { keywordItem } from '../../components/CrudPage'
import UploadImage from '../../components/UploadImage'
import { silentGet } from '../../utils/request'

const FALLBACK_COVER = '/img/singer/shan-yichun.jpg'

export default function AlbumPage() {
  const [singers, setSingers] = useState([])

  useEffect(() => {
    silentGet('/admin/singers', { pageNum: 1, pageSize: 100 }, null).then((res) =>
      setSingers(res?.list || (Array.isArray(res) ? res : []))
    )
  }, [])

  return (
    <CrudPage
      title="专辑"
      listUrl="/admin/albums"
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
        { title: '专辑名', dataIndex: 'name', ellipsis: true },
        { title: '歌手', dataIndex: 'singerName', width: 130 },
        { title: '发行日期', dataIndex: 'publishDate', width: 120, render: (v) => v || '-' },
        { title: '曲目数', dataIndex: 'songCount', width: 90, render: (v) => v ?? 0 },
      ]}
      searchItems={
        <>
          {keywordItem('name', '专辑名称')}
          <Form.Item name="singerId">
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              placeholder="歌手"
              style={{ width: 160 }}
              options={singers.map((s) => ({ value: s.id, label: s.name }))}
            />
          </Form.Item>
        </>
      }
      toEditValues={(r) => ({ ...r, publishDate: r.publishDate ? dayjs(r.publishDate) : null })}
      beforeSubmit={(values) => ({
        ...values,
        publishDate: values.publishDate ? values.publishDate.format('YYYY-MM-DD') : undefined,
      })}
      formItems={
        <>
          <Form.Item name="name" label="专辑名称" rules={[{ required: true, message: '请输入专辑名称' }]}>
            <Input maxLength={50} />
          </Form.Item>
          <Form.Item name="cover" label="封面">
            <UploadImage tip="上传封面" />
          </Form.Item>
          <Form.Item name="singerId" label="歌手" rules={[{ required: true, message: '请选择歌手' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              placeholder="选择歌手"
              options={singers.map((s) => ({ value: s.id, label: s.name }))}
            />
          </Form.Item>
          <Form.Item name="publishDate" label="发行日期">
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="intro" label="专辑简介">
            <Input.TextArea rows={3} maxLength={500} showCount />
          </Form.Item>
        </>
      }
    />
  )
}
