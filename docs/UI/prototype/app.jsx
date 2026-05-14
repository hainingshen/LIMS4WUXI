/* ====================================================================
   APP · Page index sidebar + router
   ==================================================================== */

const PAGE_LIST = [
  { group: "客户门户 · Portal", items: [
    ["P1", "客户门户登录", "Login", false],
    ["P2", "客户首页", "Dashboard", false],
    ["P3", "送检单新建", "Submission ⭐", true],
    ["P4", "客户项目详情", "Project Detail", false],
    ["P5", "报告草稿预览", "Report Preview", false],
  ]},
  { group: "内部端 · Internal Desktop", items: [
    ["P6", "工作中心", "Work Center ⭐", true],
    ["P7", "样品收样台", "Sample Desk", false],
    ["P8", "项目详情 + 状态机", "Project Detail", false],
    ["P9", "报告审核", "Report Review ⭐", true],
    ["P10", "财务收款登记", "Payment", false],
    ["P11", "设备预约日历", "Device Calendar", false],
    ["P12", "系统配置", "System Config", false],
  ]},
  { group: "移动端 · Mobile PWA", items: [
    ["M1", "移动首页", "Mobile Home", false],
    ["M2", "移动审批待办", "Approvals", false],
    ["M3", "移动样品扫码", "Sample Scan", false],
    ["M4", "移动工作中心", "Mobile Work Center", false],
  ]},
];

const PAGE_COMP = {
  P1: { c: P1_Login, mobile: false },
  P2: { c: P2_Dashboard, mobile: false },
  P3: { c: P3_SubmissionNew, mobile: false },
  P4: { c: P4_ProjectDetail, mobile: false },
  P5: { c: P5_ReportPreview, mobile: false },
  P6: { c: P6_WorkCenter, mobile: false },
  P7: { c: P7_SampleDesk, mobile: false },
  P8: { c: P8_ProjectDetail, mobile: false },
  P9: { c: P9_ReportReview, mobile: false },
  P10: { c: P10_Payment, mobile: false },
  P11: { c: P11_DeviceCalendar, mobile: false },
  P12: { c: P12_SystemConfig, mobile: false },
  M1: { c: M1_Home, mobile: true },
  M2: { c: M2_Approvals, mobile: true },
  M3: { c: M3_Scan, mobile: true },
  M4: { c: M4_WorkCenter, mobile: true },
};

function App() {
  const [pageId, setPageId] = useState(() => {
    return localStorage.getItem("lims4wuxi.page") || "P6";
  });
  const [collapsed, setCollapsed] = useState(false);
  const [q, setQ] = useState("");

  useEffect(() => { localStorage.setItem("lims4wuxi.page", pageId); }, [pageId]);

  const page = PAGE_COMP[pageId];
  const Comp = page?.c;

  const filtered = useMemo(() => {
    if (!q) return PAGE_LIST;
    const lo = q.toLowerCase();
    return PAGE_LIST.map((g) => ({
      ...g,
      items: g.items.filter((it) => it[0].toLowerCase().includes(lo) || it[1].toLowerCase().includes(lo) || it[2].toLowerCase().includes(lo)),
    })).filter((g) => g.items.length > 0);
  }, [q]);

  const onNavigate = useCallback((id) => setPageId(id), []);

  return (
    <ToastProvider>
      <div className={"host" + (collapsed ? " collapsed" : "")}>
        <aside className="host-side">
          <div className="host-brand" onClick={() => !collapsed && setCollapsed(false)}>
            <div className="logo">NJU</div>
            <div className="t">
              LIMS4WUXI
              <small>16 页高保真原型</small>
            </div>
          </div>
          {!collapsed && (
            <div className="host-search">
              <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="🔍 搜索页面 (P1-M4)..." />
            </div>
          )}
          <div className="host-pages">
            {filtered.map((g) => (
              <Fragment key={g.group}>
                <div className="host-g">{g.group}</div>
                {g.items.map((it) => {
                  const a = it[0] === pageId;
                  return (
                    <div key={it[0]} className={"host-page" + (a ? " active" : "")} onClick={() => setPageId(it[0])}>
                      <span className="code">{it[0]}</span>
                      <span className="nm">{it[1]}</span>
                      {it[3] && <span className="star">★</span>}
                    </div>
                  );
                })}
              </Fragment>
            ))}
          </div>
          <div className="host-foot">
            <span>v0.1.0 · 2026-05-13</span>
            <span className="host-toggle" onClick={() => setCollapsed(!collapsed)} title={collapsed ? "展开" : "收起"}>
              {collapsed ? "›" : "‹"}
            </span>
          </div>
        </aside>

        <main className="host-stage">
          {page?.mobile ? (
            <div className="mobile-stage">
              <div style={{ display: "flex", gap: 48, alignItems: "center" }}>
                <div className="mobile-context">
                  <div className="key">{pageId} · MOBILE PWA</div>
                  <h3>{PAGE_LIST.find((g) => g.items.some((it) => it[0] === pageId))?.items.find((it) => it[0] === pageId)?.[1]}</h3>
                  <p>移动端使用 antd-mobile 风格 PWA，iPhone 14 Pro 视口 (390×844)。所有交互与桌面端共享 Zustand 状态。</p>
                  <div style={{ marginTop: 12, display: "flex", flexDirection: "column", gap: 8 }}>
                    {PAGE_LIST[2].items.map((it) => (
                      <div key={it[0]} onClick={() => setPageId(it[0])} style={{ padding: "8px 14px", borderRadius: 6, background: it[0] === pageId ? "rgba(0,168,168,.18)" : "rgba(255,255,255,.05)", color: it[0] === pageId ? "#7FE7E7" : "rgba(255,255,255,.7)", fontSize: 12, cursor: "pointer", border: it[0] === pageId ? "1px solid rgba(0,168,168,.4)" : "1px solid transparent", display: "flex", gap: 10, alignItems: "center" }}>
                        <span style={{ fontFamily: "Inter,sans-serif", fontSize: 10, fontWeight: 600, color: it[0] === pageId ? "#7FE7E7" : "rgba(255,255,255,.4)" }}>{it[0]}</span>
                        <span>{it[1]}</span>
                      </div>
                    ))}
                  </div>
                </div>
                <div className="mobile-frame">
                  <div className="mobile-screen">
                    {Comp && <Comp onSwitch={onNavigate} />}
                  </div>
                </div>
              </div>
            </div>
          ) : (
            <div style={{ background: "#fff", minHeight: "100vh" }}>
              {Comp && <Comp onNavigate={onNavigate} onBack={() => setPageId("P4")} />}
            </div>
          )}
        </main>
      </div>
    </ToastProvider>
  );
}

const root = ReactDOM.createRoot(document.getElementById("root"));
root.render(<App />);
