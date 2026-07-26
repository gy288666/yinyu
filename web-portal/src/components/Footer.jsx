export default function Footer() {
  return (
    <footer className="footer">
      <div className="footer-cols">
        <div className="footer-col">
          <div className="footer-title">关于我们</div>
          <a>公司介绍</a>
          <a>加入我们</a>
          <a>联系方式</a>
        </div>
        <div className="footer-col">
          <div className="footer-title">帮助中心</div>
          <a>常见问题</a>
          <a>意见反馈</a>
          <a>版权申诉</a>
        </div>
        <div className="footer-col">
          <div className="footer-title">服务条款</div>
          <a>用户协议</a>
          <a>隐私政策</a>
          <a>会员服务协议</a>
        </div>
        <div className="footer-col">
          <div className="footer-title">关注我们</div>
          <div className="social-icons">
            <span className="social" title="微博">微</span>
            <span className="social" title="微信">信</span>
            <span className="social" title="哔哩哔哩">B</span>
            <span className="social" title="抖音">抖</span>
          </div>
        </div>
      </div>
      <div className="footer-copy">© 2026 音域 YINYU · 聆听世界的声音 · 仅供学习演示使用</div>
    </footer>
  )
}
