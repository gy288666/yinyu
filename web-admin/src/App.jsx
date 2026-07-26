import { BrowserRouter, Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { App as AntApp, ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import AdminLayout from './layouts/AdminLayout'
import Login from './pages/Login'
import Dashboard from './pages/dashboard'
import MusicPage from './pages/content/Music'
import MusicAuditPage from './pages/content/MusicAudit'
import PlaylistPage from './pages/content/Playlist'
import AlbumPage from './pages/content/Album'
import SingerPage from './pages/content/Singer'
import CategoryPage from './pages/content/Category'
import CopyrightPage from './pages/content/Copyright'
import UserListPage from './pages/user/UserList'
import VipPage from './pages/user/Vip'
import UserLevelPage from './pages/user/UserLevel'
import FeedbackPage from './pages/user/Feedback'
import BannerPage from './pages/operation/Banner'
import NoticePage from './pages/operation/Notice'
import ActivityPage from './pages/operation/Activity'
import StatsPage from './pages/operation/Stats'
import AdminsPage from './pages/system/Admins'
import RolesPage from './pages/system/Roles'
import PermissionsPage from './pages/system/Permissions'
import SettingsPage from './pages/system/Settings'
import LogsPage from './pages/system/Logs'
import { getToken } from './utils/auth'

dayjs.locale('zh-cn')

/** 登录守卫：无 token 跳登录页 */
function RequireAuth() {
  if (!getToken()) {
    return <Navigate to="/login" replace />
  }
  return <Outlet />
}

export default function App() {
  return (
    <ConfigProvider
      locale={zhCN}
      theme={{
        token: {
          colorPrimary: '#2b6de8',
          borderRadius: 6,
        },
        components: {
          Layout: {
            siderBg: '#0f2249',
            triggerBg: '#0a1a38',
          },
          Menu: {
            darkItemBg: '#0f2249',
            darkSubMenuItemBg: '#0a1a38',
            darkItemSelectedBg: '#2b6de8',
          },
        },
      }}
    >
      <AntApp>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route element={<RequireAuth />}>
              <Route element={<AdminLayout />}>
                <Route path="/" element={<Dashboard />} />
                <Route path="/content/music" element={<MusicPage />} />
                <Route path="/content/audit" element={<MusicAuditPage />} />
                <Route path="/content/playlist" element={<PlaylistPage />} />
                <Route path="/content/album" element={<AlbumPage />} />
                <Route path="/content/singer" element={<SingerPage />} />
                <Route path="/content/category" element={<CategoryPage />} />
                <Route path="/content/copyright" element={<CopyrightPage />} />
                <Route path="/user/list" element={<UserListPage />} />
                <Route path="/user/vip" element={<VipPage />} />
                <Route path="/user/level" element={<UserLevelPage />} />
                <Route path="/user/feedback" element={<FeedbackPage />} />
                <Route path="/operation/banner" element={<BannerPage />} />
                <Route path="/operation/notice" element={<NoticePage />} />
                <Route path="/operation/activity" element={<ActivityPage />} />
                <Route path="/operation/stats" element={<StatsPage />} />
                <Route path="/system/admin" element={<AdminsPage />} />
                <Route path="/system/role" element={<RolesPage />} />
                <Route path="/system/permission" element={<PermissionsPage />} />
                <Route path="/system/setting" element={<SettingsPage />} />
                <Route path="/system/log" element={<LogsPage />} />
                <Route path="*" element={<Navigate to="/" replace />} />
              </Route>
            </Route>
          </Routes>
        </BrowserRouter>
      </AntApp>
    </ConfigProvider>
  )
}
