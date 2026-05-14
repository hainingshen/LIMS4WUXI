/* ====================================================================
   PORTAL · P1 Login · P2 Dashboard · P3 Submission · P4 Project · P5 Report
   ==================================================================== */

/* ============ P1: Customer Portal Login ============ */
function P1_Login() {
  const toast = useToast();
  const [tab, setTab] = useState("password");
  const [email, setEmail] = useState("li.purchase@example-medical.cn");
  const [pwd, setPwd] = useState("••••••••••");
  const [remember, setRemember] = useState(true);

  return (
    <div style={{ display: "grid", gridTemplateColumns: "1.4fr 1fr", height: "100vh", background: "#fff", minHeight: 720 }}>
      {/* Left brand panel */}
      <div style={{
        background: "linear-gradient(135deg,#0B1A30 0%,#0E3A6E 45%,#1A5FB4 100%)",
        color: "#fff", padding: "60px 80px", display: "flex", flexDirection: "column", justifyContent: "space-between",
        position: "relative", overflow: "hidden",
      }}>
        <div style={{ position: "absolute", inset: 0, background:
          "radial-gradient(800px 400px at 110% 20%,rgba(0,168,168,.30),transparent 60%)," +
          "radial-gradient(600px 320px at -10% 100%,rgba(255,255,255,.08),transparent 60%)",
          pointerEvents: "none",
        }} />
        <div style={{ position: "relative", zIndex: 1 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
            <div style={{ width: 48, height: 48, borderRadius: 12, background: "rgba(255,255,255,.12)", border: "1px solid rgba(255,255,255,.2)", display: "flex", alignItems: "center", justifyContent: "center", fontFamily: "Inter,sans-serif", fontWeight: 700, fontSize: 15, backdropFilter: "blur(6px)" }}>NJU</div>
            <div>
              <div style={{ fontSize: 18, fontWeight: 600 }}>NJU XISHAN LIMS</div>
              <div style={{ fontSize: 12, opacity: .65, fontFamily: "Inter,sans-serif", letterSpacing: ".06em" }}>客户服务平台 · Customer Portal</div>
            </div>
          </div>
        </div>

        <div style={{ position: "relative", zIndex: 1 }}>
          {/* Abstract lab graphic */}
          <div style={{ marginBottom: 40, opacity: .9 }}>
            <svg width="320" height="180" viewBox="0 0 320 180" style={{ filter: "drop-shadow(0 8px 20px rgba(0,168,168,.3))" }}>
              <defs>
                <linearGradient id="lg1" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0" stopColor="#00A8A8" stopOpacity=".7" />
                  <stop offset="1" stopColor="#00A8A8" stopOpacity=".1" />
                </linearGradient>
              </defs>
              {/* Beaker */}
              <path d="M40 40 L40 60 L20 150 Q20 165 35 165 L85 165 Q100 165 100 150 L80 60 L80 40 Z" fill="url(#lg1)" stroke="#00A8A8" strokeWidth="1.5" />
              <line x1="35" y1="40" x2="85" y2="40" stroke="#fff" strokeWidth="2" />
              {/* Test tubes */}
              {[140, 180, 220].map((x, i) => (
                <g key={x}>
                  <rect x={x} y={50} width="22" height="100" rx="2" fill="rgba(255,255,255,.04)" stroke="rgba(255,255,255,.4)" strokeWidth="1" />
                  <rect x={x + 1} y={80 + i * 12} width="20" height={70 - i * 12} fill={["#1A5FB4", "#00A8A8", "#7FE7E7"][i]} opacity=".6" />
                  <rect x={x - 2} y={45} width="26" height="6" rx="1" fill="rgba(255,255,255,.6)" />
                </g>
              ))}
              {/* Petri dish */}
              <ellipse cx="280" cy="120" rx="32" ry="10" fill="rgba(0,168,168,.2)" stroke="#00A8A8" strokeWidth="1.2" />
              <circle cx="270" cy="118" r="2" fill="#fff" opacity=".7" />
              <circle cx="290" cy="122" r="1.5" fill="#fff" opacity=".5" />
              <circle cx="280" cy="115" r="1.2" fill="#fff" opacity=".6" />
              {/* Connecting line */}
              <path d="M105 90 Q120 70 140 80" stroke="rgba(255,255,255,.3)" strokeWidth="1" fill="none" strokeDasharray="3 3" />
            </svg>
          </div>

          <div style={{ fontSize: 32, fontWeight: 700, letterSpacing: ".02em", lineHeight: 1.3, marginBottom: 12 }}>
            专业 · 严谨<br />可信赖的医疗器械检测伙伴
          </div>
          <div style={{ fontSize: 14, opacity: .75, lineHeight: 1.7 }}>
            从送检到归档全流程数字化 · 报告草稿实时预览 · 付款门禁严格管控<br />
            服务超过 1,200 家医疗器械企业 · 累计签发报告 18,000+ 份
          </div>

          <div style={{ display: "flex", gap: 10, marginTop: 32, flexWrap: "wrap" }}>
            {[
              ["CMA", "中国计量认证", "#7FE7E7"],
              ["CNAS", "国家实验室认可", "#9EBEE8"],
              ["GLP", "良好实验室规范", "#FFD59E"],
            ].map(([code, name, color]) => (
              <div key={code} style={{ display: "flex", alignItems: "center", gap: 10, padding: "10px 16px", borderRadius: 8, background: "rgba(255,255,255,.08)", border: "1px solid rgba(255,255,255,.18)", backdropFilter: "blur(6px)" }}>
                <div style={{ width: 28, height: 28, borderRadius: 6, background: "rgba(255,255,255,.1)", display: "flex", alignItems: "center", justifyContent: "center", fontFamily: "Inter,sans-serif", fontWeight: 700, fontSize: 11, color }}>{code}</div>
                <div>
                  <div style={{ fontSize: 12, fontWeight: 600 }}>{code}</div>
                  <div style={{ fontSize: 10, opacity: .65 }}>{name}</div>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div style={{ position: "relative", zIndex: 1, fontSize: 11, opacity: .5, fontFamily: "Inter,sans-serif" }}>
          © 2026 Wuxi Xishan NJU Institute of Applied Biotechnology · 苏 ICP 备 12345678 号
        </div>
      </div>

      {/* Right login form */}
      <div style={{ padding: "60px 60px", display: "flex", flexDirection: "column", justifyContent: "center", maxWidth: 480, margin: "0 auto", width: "100%" }}>
        <div style={{ marginBottom: 32 }}>
          <div style={{ fontSize: 11, color: "var(--accent)", fontFamily: "Inter,sans-serif", letterSpacing: ".18em", fontWeight: 600, marginBottom: 8 }}>WELCOME BACK</div>
          <h1 style={{ fontSize: 28, fontWeight: 600, margin: 0, color: "var(--ink-900)" }}>登录客户服务平台</h1>
          <div style={{ fontSize: 13, color: "var(--ink-600)", marginTop: 6 }}>使用您注册的手机号或邮箱登录</div>
        </div>

        <div style={{ display: "flex", gap: 0, marginBottom: 24, borderBottom: "1px solid var(--line)" }}>
          {[["password", "账号密码登录"], ["qrcode", "扫码登录"]].map(([k, l]) => (
            <div
              key={k}
              onClick={() => setTab(k)}
              style={{
                padding: "10px 0", marginRight: 32, cursor: "pointer", fontSize: 14,
                color: tab === k ? "var(--primary)" : "var(--ink-600)",
                fontWeight: tab === k ? 600 : 400,
                borderBottom: "2px solid " + (tab === k ? "var(--primary)" : "transparent"),
                marginBottom: -1,
              }}
            >{l}</div>
          ))}
        </div>

        {tab === "password" ? (
          <>
            <Field label="手机号 / 邮箱" required>
              <input className="f-input" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="请输入手机号或邮箱" />
            </Field>
            <Field label="密码" required>
              <input className="f-input" type="password" value={pwd} onChange={(e) => setPwd(e.target.value)} placeholder="请输入密码" />
            </Field>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 24 }}>
              <label style={{ fontSize: 13, color: "var(--ink-700)", display: "flex", alignItems: "center", gap: 6, cursor: "pointer" }}>
                <input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} />
                7 天内自动登录
              </label>
              <span style={{ fontSize: 13, color: "var(--primary)", cursor: "pointer" }}>忘记密码？</span>
            </div>
            <button
              className="btn btn-primary btn-lg btn-block"
              onClick={() => toast("登录成功，欢迎回来", "success")}
            >登 录</button>
          </>
        ) : (
          <div style={{ textAlign: "center", padding: "20px 0" }}>
            <div style={{ width: 200, height: 200, margin: "0 auto", border: "1px solid var(--line)", borderRadius: 12, padding: 14, background: "#fff", display: "flex", alignItems: "center", justifyContent: "center", position: "relative" }}>
              <div style={{ width: "100%", height: "100%", background:
                "linear-gradient(45deg, #0F172A 25%, transparent 25%, transparent 75%, #0F172A 75%)," +
                "linear-gradient(-45deg, #0F172A 25%, transparent 25%, transparent 75%, #0F172A 75%)",
                backgroundSize: "8px 8px",
                opacity: .7,
              }}>
                <div style={{ position: "absolute", inset: 14, background: "#fff", display: "flex", alignItems: "center", justifyContent: "center", flexDirection: "column", gap: 6 }}>
                  <div style={{ fontFamily: "Inter,sans-serif", fontSize: 10, color: "var(--ink-600)" }}>请使用 LIMS App</div>
                  <div style={{ fontFamily: "Inter,sans-serif", fontSize: 10, color: "var(--ink-600)" }}>扫一扫登录</div>
                </div>
              </div>
            </div>
            <div style={{ fontSize: 12, color: "var(--ink-600)", marginTop: 16 }}>二维码有效期 02:00 · 刷新</div>
          </div>
        )}

        <div style={{ marginTop: 40, padding: "16px 18px", background: "var(--bg)", border: "1px solid var(--line)", borderRadius: 8, fontSize: 12, color: "var(--ink-600)", lineHeight: 1.7 }}>
          如需开通客户账号，请联系 <b style={{ color: "var(--primary)" }}>{ORG.email}</b> 或致电 <b className="mono" style={{ color: "var(--ink-900)" }}>{ORG.tel}</b>
        </div>
      </div>
    </div>
  );
}

window.P1_Login = P1_Login;
