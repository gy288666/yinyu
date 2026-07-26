import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Form, Input, message } from 'antd'
import { CustomerServiceOutlined, LockOutlined, SafetyOutlined, UserOutlined } from '@ant-design/icons'
import request from '../utils/request'
import { setAdminInfo, setRefreshToken, setToken } from '../utils/auth'

export default function Login() {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [captcha, setCaptcha] = useState(null) // { captchaKey, imageBase64 }
  const navigate = useNavigate()

  const loadCaptcha = useCallback(async () => {
    try {
      const data = await request.get('/admin/auth/captcha')
      setCaptcha(data || null)
    } catch {
      // 验证码接口不可用时降级为无验证码登录
      setCaptcha(null)
    }
  }, [])

  useEffect(() => {
    loadCaptcha()
  }, [loadCaptcha])

  const onFinish = async (values) => {
    setLoading(true)
    try {
      const payload = {
        username: values.username,
        password: values.password,
      }
      if (captcha?.captchaKey) {
        payload.captchaKey = captcha.captchaKey
        payload.captchaCode = values.captchaCode
      }
      const data = await request.post('/admin/auth/login', payload)
      if (!data?.accessToken) {
        message.error('登录响应异常，请联系管理员')
        return
      }
      setToken(data.accessToken)
      if (data.refreshToken) setRefreshToken(data.refreshToken)
      setAdminInfo({
        adminId: data.adminId,
        name: data.name,
        roles: data.roles || [],
        permissions: data.permissions || [],
        menus: data.menus || [],
      })
      message.success('登录成功')
      navigate('/', { replace: true })
    } catch {
      // 失败提示由拦截器统一处理，刷新验证码
      loadCaptcha()
      form.setFieldValue('captchaCode', '')
    } finally {
      setLoading(false)
    }
  }

  const captchaSrc = captcha?.imageBase64
    ? captcha.imageBase64.startsWith('data:')
      ? captcha.imageBase64
      : `data:image/png;base64,${captcha.imageBase64}`
    : ''

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #2b6de8 0%, #143a8c 60%, #0d2a66 100%)',
      }}
    >
      <div
        style={{
          width: 400,
          background: '#fff',
          borderRadius: 12,
          padding: '40px 36px',
          boxShadow: '0 12px 40px rgba(9, 32, 87, 0.35)',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: 28 }}>
          <div
            style={{
              width: 56,
              height: 56,
              margin: '0 auto 12px',
              borderRadius: 14,
              background: 'linear-gradient(135deg, #2b6de8, #5b8ff0)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <CustomerServiceOutlined style={{ fontSize: 28, color: '#fff' }} />
          </div>
          <div style={{ fontSize: 20, fontWeight: 600, color: '#1a1a1a' }}>音域 YINYU 管理系统</div>
          <div style={{ fontSize: 13, color: '#999', marginTop: 4 }}>纯音乐平台 · 管理后台</div>
        </div>

        <Form form={form} size="large" onFinish={onFinish} autoComplete="off">
          <Form.Item name="username" rules={[{ required: true, message: '请输入管理员账号' }]}>
            <Input prefix={<UserOutlined />} placeholder="管理员账号" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="密码" />
          </Form.Item>
          {captcha && (
            <Form.Item name="captchaCode" rules={[{ required: true, message: '请输入验证码' }]}>
              <div style={{ display: 'flex', gap: 8 }}>
                <Input prefix={<SafetyOutlined />} placeholder="验证码" style={{ flex: 1 }} />
                <img
                  src={captchaSrc}
                  alt="验证码"
                  title="点击刷新"
                  onClick={loadCaptcha}
                  style={{ height: 40, width: 110, borderRadius: 6, cursor: 'pointer', border: '1px solid #eee' }}
                />
              </div>
            </Form.Item>
          )}
          <Form.Item style={{ marginBottom: 8 }}>
            <Button type="primary" htmlType="submit" block loading={loading}>
              登 录
            </Button>
          </Form.Item>
        </Form>
        <div style={{ textAlign: 'center', color: '#bbb', fontSize: 12, marginTop: 16 }}>
          © 2026 音域 YINYU · 仅限授权管理员使用
        </div>
      </div>
    </div>
  )
}
