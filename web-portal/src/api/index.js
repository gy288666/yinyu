import client from './client'

// ---- 认证 ----
export const apiLogin = (data) => client.post('/auth/login', data)
export const apiRegister = (data) => client.post('/auth/register', data)
export const apiLogout = () => client.post('/auth/logout')
export const apiMe = (silent = true) => client.get('/auth/me', { silent })

// ---- 歌曲 ----
export const apiSongs = (params) => client.get('/songs', { params })
export const apiSongDetail = (id) => client.get(`/songs/${id}`)
export const apiSongUrl = (id) => client.get(`/songs/${id}/url`, { silent: true })
export const apiNewestSongs = (limit = 12) => client.get('/songs/newest', { params: { limit } })
export const apiSongLyric = (id) => client.get(`/songs/${id}/lyric`, { silent: true })
export const apiDownloadUrl = (id) => client.get(`/songs/${id}/download-url`)

// ---- 歌手 / 专辑 ----
export const apiSingers = (params) => client.get('/singers', { params })
export const apiSingerDetail = (id) => client.get(`/singers/${id}`)
export const apiSingerSongs = (id, params) => client.get(`/singers/${id}/songs`, { params })
export const apiSingerAlbums = (id, params) => client.get(`/singers/${id}/albums`, { params })
export const apiAlbumDetail = (id) => client.get(`/albums/${id}`)
export const apiAlbumSongs = (id) => client.get(`/albums/${id}/songs`)

// ---- 歌单 ----
export const apiPlaylists = (params) => client.get('/playlists', { params })
export const apiHotPlaylists = (limit = 8) => client.get('/playlists/hot', { params: { limit } })
export const apiPlaylistDetail = (id) => client.get(`/playlists/${id}`)
export const apiMyPlaylists = () => client.get('/my/playlists', { silent: true })
export const apiCreatePlaylist = (data) => client.post('/my/playlists', data)
export const apiUpdatePlaylist = (id, data) => client.put(`/my/playlists/${id}`, data)
export const apiDeletePlaylist = (id) => client.delete(`/my/playlists/${id}`)
export const apiPlaylistAddSong = (id, songId) => client.post(`/my/playlists/${id}/songs`, { songId })
export const apiPlaylistRemoveSong = (id, songId) => client.delete(`/my/playlists/${id}/songs/${songId}`)

// ---- 分类 / 搜索 ----
export const apiCategories = () => client.get('/categories', { silent: true })
export const apiSearch = (params) => client.get('/search', { params })
export const apiHotKeywords = (limit = 10) => client.get('/search/hot', { params: { limit }, silent: true })

// ---- 排行榜 ----
export const apiRanks = () => client.get('/ranks')
export const apiRankDetail = (type, limit = 50) => client.get(`/ranks/${type}`, { params: { limit } })

// ---- 推荐 / FM / 电台 ----
export const apiDailyRecommend = () => client.get('/recommend/daily')
export const apiRecommendSongs = (limit = 10) => client.get('/recommend/songs', { params: { limit }, silent: true })
export const apiRecommendPlaylists = (limit = 6) => client.get('/recommend/playlists', { params: { limit }, silent: true })
export const apiFmNext = () => client.get('/fm/next')
export const apiFmDislike = (songId) => client.post('/fm/dislike', { songId })
export const apiRadios = () => client.get('/radios')
export const apiRadioNext = (id) => client.get(`/radios/${id}/next`)

// ---- 喜欢 / 收藏 ----
export const apiLikeSong = (songId) => client.post(`/likes/songs/${songId}`)
export const apiUnlikeSong = (songId) => client.delete(`/likes/songs/${songId}`)
export const apiLikedSongs = (params) => client.get('/likes/songs', { params })
export const apiCollectPlaylist = (id) => client.post(`/collections/playlists/${id}`)
export const apiUncollectPlaylist = (id) => client.delete(`/collections/playlists/${id}`)

// ---- 最近播放 / 下载 ----
export const apiRecent = (params) => client.get('/recent', { params })
export const apiClearRecent = () => client.delete('/recent')
export const apiDownloads = (params) => client.get('/downloads', { params })

// ---- 会员 / 订单 ----
export const apiVipPlans = () => client.get('/vip/plans')
export const apiCreateOrder = (data) => client.post('/orders', data)
export const apiPayOrder = (orderNo, channel = 'MOCK') => client.post(`/orders/${orderNo}/pay`, { channel })
export const apiOrderDetail = (orderNo) => client.get(`/orders/${orderNo}`)
export const apiMyOrders = (params) => client.get('/orders', { params })

// ---- 运营内容 ----
export const apiBanners = () => client.get('/banners', { silent: true })
