import { useMemo, useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Avatar, Badge, Breadcrumb, Button, Dropdown, Layout, Menu, message, theme } from 'antd'
import {
  AppstoreOutlined,
  AuditOutlined,
  BarChartOutlined,
  BellOutlined,
  CommentOutlined,
  CopyrightOutlined,
  CrownOutlined,
  CustomerServiceOutlined,
  DashboardOutlined,
  FileProtectOutlined,
  FileTextOutlined,
  FolderOpenOutlined,
  FundOutlined,
  GiftOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  NotificationOutlined,
  PictureOutlined,
  SafetyCertificateOutlined,
  SettingOutlined,
  SoundOutlined,
  TagsOutlined,
  TeamOutlined,
  TrophyOutlined,
  UserOutlined,
  UserSwitchOutlined,
} from '@ant-design/icons'
import request from '../utils/request'
import { clearAuth, getAdminInfo } from '../utils/auth'

const { Header, Sider, Content } = Layout

const DEFAULT_AVATAR = '/img/singer/mao-buyi.jpg'

/** 菜单配置：label / path / icon，分组对应需求文档 */
const MENUS = [
  { key: '/', icon: <DashboardOutlined />, label: '数据看板' },
  {
    key: 'g-content',
    icon: <FolderOpenOutlined />,
    label: '内容管理',
    children: [
      { key: '/content/music', icon: <CustomerServiceOutlined />, label: '音乐管理' },
      { key: '/content/audit', icon: <AuditOutlined />, label: '音乐审核' },
      { key: '/content/playlist', icon: <AppstoreOutlined />, label: '歌单管理' },
      { key: '/content/album', icon: <SoundOutlined />, label: '专辑管理' },
      { key: '/content/singer', icon: <TeamOutlined />, label: '歌手管理' },
      { key: '/content/category', icon: <TagsOutlined />, label: '分类管理' },
      { key: '/content/copyright', icon: <CopyrightOutlined />, label: '版权管理' },
    ],
  },
  {
    key: 'g-user',
    icon: <UserOutlined />,
    label: '用户管理',
    children: [
      { key: '/user/list', icon: <UserOutlined />, label: '用户列表' },
      { key: '/user/vip', icon: <CrownOutlined />, label: '会员管理' },
      { key: '/user/level', icon: <TrophyOutlined />, label: '用户等级' },
      { key: '/user/feedback', icon: <CommentOutlined />, label: '用户反馈' },
    ],
  },
  {
    key: 'g-operation',
    icon: <FundOutlined />,
    label: '运营中心',
    children: [
      { key: '/operation/banner', icon: <PictureOutlined />, label: '轮播图管理' },
      { key: '/operation/notice', icon: <NotificationOutlined />, label: '公告管理' },
      { key: '/operation/activity', icon: <GiftOutlined />, label: '活动管理' },
      { key: '/operation/stats', icon: <BarChartOutlined />, label: '数据统计' },
    ],
  },
  {
    key: 'g-system',
    icon: <SettingOutlined />,
    label: '系统管理',
    children: [
      { key: '/system/admin', icon: <UserSwitchOutlined />, label: '管理员管理' },
      { key: '/system/role', icon: <SafetyCertificateOutlined />, label: '角色管理' },
      { key: '/system/permission', icon: <FileProtectOutlined />, label: '权限管理' },
      { key: '/system/setting', icon: <SettingOutlined />, label: '系统设置' },
      { key: '/system/log', icon: <FileTextOutlined />, label: '操作日志' },
    ],
  },
]

/** path -> [组名, 页面名] 映射，用于面包屑 */
function buildBreadcrumbMap() {
  const map = { '/': ['首页', '数据看板'] }
  MENUS.forEach((m) => {
    if (m.children) {
      m.children.forEach((c) => {
        map[c.key] = [m.label, c.label]
      })
    }
  })
  return map
}
const BREADCRUMB_MAP = buildBreadcrumbMap()

export default function AdminLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const { token } = theme.useToken()
  const adminInfo = getAdminInfo()

  const selectedKey = location.pathname === '/' ? '/' : location.pathname
  const openKey = useMemo(() => {
    const group = MENUS.find((m) => m.children?.some((c) => c.key === selectedKey))
    return group ? [group.key] : []
  }, [selectedKey])
  const [openKeys, setOpenKeys] = useState(openKey)

  const crumb = BREADCRUMB_MAP[selectedKey] || ['首页', '数据看板']

  const onLogout = async () => {
    try {
      await request.post('/admin/auth/logout')
    } catch {
      // 忽略登出接口异常，本地凭证照常清除
    }
    clearAuth()
    message.success('已退出登录')
    navigate('/login', { replace: true })
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        collapsible
        collapsed={collapsed}
        trigger={null}
        width={220}
        style={{ position: 'sticky', top: 0, height: '100vh', overflow: 'auto' }}
      >
        <div
          style={{
            height: 56,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 8,
            color: '#fff',
            fontWeight: 600,
            fontSize: collapsed ? 14 : 16,
            whiteSpace: 'nowrap',
            overflow: 'hidden',
            background: 'linear-gradient(90deg, #2b6de8 0%, #1d54c0 100%)',
          }}
        >
          <CustomerServiceOutlined style={{ fontSize: 20 }} />
          {!collapsed && <span>音域 YINYU 管理系统</span>}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          openKeys={collapsed ? undefined : openKeys}
          onOpenChange={setOpenKeys}
          items={MENUS}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>

      <Layout>
        <Header
          style={{
            position: 'sticky',
            top: 0,
            zIndex: 10,
            height: 56,
            padding: '0 16px',
            background: token.colorBgContainer,
            display: 'flex',
            alignItems: 'center',
            gap: 16,
            boxShadow: '0 1px 4px rgba(0,21,41,0.08)',
          }}
        >
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
          />
          <Breadcrumb items={[{ title: crumb[0] }, { title: crumb[1] }]} />
          <div style={{ flex: 1 }} />
          <Badge count={5} size="small">
            <Button type="text" icon={<BellOutlined style={{ fontSize: 18 }} />} />
          </Badge>
          <Dropdown
            menu={{
              items: [
                {
                  key: 'logout',
                  icon: <LogoutOutlined />,
                  label: '退出登录',
                  onClick: onLogout,
                },
              ],
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
              <Avatar size={32} src={adminInfo.avatar || DEFAULT_AVATAR} icon={<UserOutlined />} />
              <span style={{ fontSize: 14 }}>{adminInfo.name || '管理员'}</span>
            </div>
          </Dropdown>
        </Header>

        <Content style={{ margin: 16 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
