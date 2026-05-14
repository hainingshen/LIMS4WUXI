/* ====================================================================
   MOBILE · M1 Home · M2 Approvals · M3 Sample Scan · M4 Mobile Work Center
   These are rendered inside a phone frame.
   ==================================================================== */

/* Shared mobile chrome */
function MobileStatusBar({ dark }) {
  return (
    <div style={{
      height: 44, display: "flex", alignItems: "flex-end", justifyContent: "space-between",
      padding: "0 26px 6px", fontFamily: "Inter,sans-serif", fontSize: 15, fontWeight: 600,
      color: dark ? "#fff" : "#0F172A", flexShrink: 0,
    }}>
      <span>9:41</span>
      <span style={{ display: "flex", gap: 5, alignItems: "center", fontSize: 12 }}>
        ●●●●● <span style={{ fontSize: 11 }}>5G</span>
        <svg width="22" height="11" viewBox="0 0 22 11"><rect x="0" y="0" width="18" height="11" rx="2" fill="none" stroke={dark ? "#fff" : "#0F172A"} strokeWidth="1" /><rect x="2" y="2" width="13" height="7" rx="1" fill={dark ? "#fff" : "#0F172A"} /><rect x="19" y="3" width="2" height="5" rx="1" fill={dark ? "#fff" : "#0F172A"} /></svg>
      </span>
    </div>
  );
}

function MobileTabBar({ active, onChange }) {
  const items = [
    ["home", "首页", "⌂"],
    ["tasks", "待办", "✓", 7],
    ["scan", "扫码", "⊞", null, true],
    ["me", "我的", "◔"],
  ];
  return (
    <div style={{
      height: 70, borderTop: "1px solid var(--line)", display: "flex",
      background: "rgba(255,255,255,.95)", backdropFilter: "blur(12px)",
      flexShrink: 0,
    }}>
      {items.map(([k, l, ic, badge, mid]) => {
        const a = k === active;
        return (
          <div key={k} onClick={() => onChange && onChange(k)} style={{
            flex: 1, display: "flex", flexDirection: "column", alignItems: "center",
            justifyContent: "center", color: a ? "var(--primary)" : "var(--ink-400)",
            cursor: "pointer", position: "relative",
          }}>
            {mid ? (
              <div style={{ width: 50, height: 50, borderRadius: 14, background: "linear-gradient(135deg,#1A5FB4,#00A8A8)", color: "#fff", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 22, boxShadow: "0 6px 16px rgba(0,168,168,.35)", marginTop: -16 }}>{ic}</div>
            ) : (
              <span style={{ fontSize: 22, position: "relative" }}>
                {ic}
                {badge && <span style={{ position: "absolute", top: -6, right: -12, background: "var(--danger)", color: "#fff", fontSize: 9, fontWeight: 600, padding: "0 5px", borderRadius: 8, fontFamily: "Inter,sans-serif" }}>{badge}</span>}
              </span>
            )}
            <span style={{ fontSize: 10, marginTop: 3 }}>{l}</span>
          </div>
        );
      })}
    </div>
  );
}

/* ============ M1: Mobile Home (login lite + home) ============ */
function M1_Home({ onSwitch }) {
  const [tab, setTab] = useState("home");
  return (
    <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "#F8FAFC" }}>
      {/* Branded header */}
      <div style={{ background: "linear-gradient(135deg,#0E3A6E 0%,#1A5FB4 60%,#00A8A8 130%)", color: "#fff", padding: "0 22px 24px", position: "relative", overflow: "hidden", flexShrink: 0 }}>
        <div style={{ position: "absolute", inset: 0, background: "radial-gradient(300px 200px at 100% 0%,rgba(0,168,168,.4),transparent)" }} />
        <MobileStatusBar dark />
        <div style={{ position: "relative", display: "flex", alignItems: "center", gap: 12, marginTop: 6 }}>
          <Avatar name="周" color="rgba(255,255,255,.2)" size={48} />
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 17, fontWeight: 600 }}>周教授，上午好</div>
            <div style={{ fontSize: 12, opacity: .75, marginTop: 2 }}>科技负责人 SD · 在岗</div>
          </div>
          <span style={{ fontSize: 20, opacity: .75 }}>🔔</span>
        </div>
        <div style={{ position: "relative", display: "flex", gap: 8, marginTop: 18 }}>
          <div style={{ flex: 1, padding: "10px 12px", background: "rgba(255,255,255,.12)", borderRadius: 10, backdropFilter: "blur(4px)" }}>
            <div style={{ fontSize: 11, opacity: .75 }}>今日待办</div>
            <div style={{ fontFamily: "Inter,sans-serif", fontSize: 22, fontWeight: 600, marginTop: 2 }}>12</div>
          </div>
          <div style={{ flex: 1, padding: "10px 12px", background: "rgba(255,255,255,.12)", borderRadius: 10 }}>
            <div style={{ fontSize: 11, opacity: .75 }}>本周完成</div>
            <div style={{ fontFamily: "Inter,sans-serif", fontSize: 22, fontWeight: 600, marginTop: 2 }}>29</div>
          </div>
          <div style={{ flex: 1, padding: "10px 12px", background: "rgba(239,68,68,.18)", borderRadius: 10 }}>
            <div style={{ fontSize: 11, opacity: .75 }}>超期</div>
            <div style={{ fontFamily: "Inter,sans-serif", fontSize: 22, fontWeight: 600, marginTop: 2, color: "#FFD59E" }}>5</div>
          </div>
        </div>
      </div>

      <div style={{ flex: 1, overflowY: "auto", padding: "16px 14px" }}>
        {/* Urgent banner */}
        <div style={{ background: "linear-gradient(90deg,#FEF3C7,#FEF9E7)", border: "1px solid #FDE68A", borderRadius: 12, padding: 12, display: "flex", alignItems: "center", gap: 10, marginBottom: 14 }}>
          <div style={{ fontSize: 22 }}>⚠</div>
          <div style={{ flex: 1, fontSize: 12, color: "#92400E", lineHeight: 1.6 }}>
            <b>2 项报告</b>需 SD 终审 · <b>1 项</b>非标方法待评审
          </div>
          <span style={{ fontSize: 11, color: "#92400E", fontWeight: 600 }}>处理 →</span>
        </div>

        {/* 5 grid */}
        <div className="card" style={{ padding: 14, marginBottom: 14 }}>
          <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 12 }}>快捷入口</div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(4,1fr)", gap: 14 }}>
            {[
              ["审批", "✓", "#1A5FB4", 7, () => onSwitch && onSwitch("M2")],
              ["工作中心", "▤", "#00A8A8", null, () => onSwitch && onSwitch("M4")],
              ["扫码入库", "⊞", "#10B981", null, () => onSwitch && onSwitch("M3")],
              ["报告预览", "⎙", "#7C3AED", 2],
              ["设备预约", "⏱", "#F59E0B"],
              ["项目搜索", "🔍", "#0E7A7A"],
              ["收款记录", "¥", "#92400E"],
              ["客户消息", "✉", "#5B21B6", 3],
            ].map(([l, ic, c, b, fn], i) => (
              <div key={i} onClick={fn} style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 6, cursor: "pointer" }}>
                <div style={{ width: 48, height: 48, borderRadius: 14, background: c + "1A", color: c, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 22, position: "relative" }}>
                  {ic}
                  {b && <span style={{ position: "absolute", top: -4, right: -4, background: "var(--danger)", color: "#fff", fontSize: 9, fontWeight: 600, padding: "0 5px", borderRadius: 8, fontFamily: "Inter,sans-serif" }}>{b}</span>}
                </div>
                <span style={{ fontSize: 11, color: "var(--ink-700)" }}>{l}</span>
              </div>
            ))}
          </div>
        </div>

        {/* My today */}
        <div className="card" style={{ padding: 14, marginBottom: 14 }}>
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 12 }}>
            <span style={{ fontSize: 13, fontWeight: 600 }}>今日要处理</span>
            <span style={{ fontSize: 11, color: "var(--primary)" }}>查看全部 →</span>
          </div>
          {[
            { id: "P-2026-0001", t: "非标方法评审", desc: "妥能水胶体敷贴 · 待 SD 批准", tone: "blue", time: "5h" },
            { id: "P-2026-0007", t: "报告 v2 终审", desc: "QA 已通过，等您批准发放", tone: "amber", time: "2d" },
            { id: "P-2026-0023", t: "绿色通道申请", desc: "加急放行 · QA 已签", tone: "cyan", time: "1d" },
          ].map((it) => (
            <div key={it.id} style={{ display: "flex", alignItems: "center", gap: 10, padding: "10px 0", borderBottom: "1px dashed var(--line)" }}>
              <div style={{ width: 4, height: 32, background: it.tone === "amber" ? "var(--warning)" : "var(--primary)", borderRadius: 2 }} />
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                  <span className="mono" style={{ fontSize: 10, color: "var(--ink-600)" }}>{it.id}</span>
                  <Tag tone={it.tone} style={{ fontSize: 10, padding: "0 6px" }}>{it.time}</Tag>
                </div>
                <div style={{ fontSize: 13, fontWeight: 500, marginTop: 2 }}>{it.t}</div>
                <div style={{ fontSize: 11, color: "var(--ink-600)" }}>{it.desc}</div>
              </div>
              <span style={{ color: "var(--ink-400)", fontSize: 16 }}>›</span>
            </div>
          ))}
        </div>

        {/* News */}
        <div className="card" style={{ padding: 14 }}>
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 12 }}>
            <span style={{ fontSize: 13, fontWeight: 600 }}>实验室快讯</span>
          </div>
          {[["公告", "5-12", "blue", "五·一假期实验室排班通知"], ["资质", "4-28", "green", "CNAS 复评审已通过"]].map((n, i) => (
            <div key={i} style={{ display: "flex", gap: 10, padding: "8px 0", borderBottom: "1px dashed var(--line)" }}>
              <Tag tone={n[2]} style={{ fontSize: 10, alignSelf: "flex-start" }}>{n[0]}</Tag>
              <div style={{ flex: 1, fontSize: 12 }}>{n[3]}</div>
              <span className="mono" style={{ fontSize: 10, color: "var(--ink-400)" }}>{n[1]}</span>
            </div>
          ))}
        </div>
      </div>
      <MobileTabBar active={tab} onChange={setTab} />
    </div>
  );
}

/* ============ M2: Mobile Approvals ============ */
function M2_Approvals() {
  const toast = useToast();
  const [seg, setSeg] = useState("nonstd");
  const [detailOpen, setDetailOpen] = useState(null);
  const [sheetOpen, setSheetOpen] = useState(false);
  const segs = [
    ["nonstd", "非标评审", 3],
    ["report", "报告审核", 7],
    ["green", "绿色通道", 1],
    ["payment", "收款复核", 2],
  ];
  const lists = {
    nonstd: [
      { id: "P-2026-0001", c: "无锡市南京大学锡山所", amt: 36800, st: "待 SD 批准", who: PEOPLE.sales, at: "5h 前", note: "妥能水胶体敷贴 · YY/T 1293.4-2016 非标补充协议" },
      { id: "P-2026-0033", c: "晶辰生物", amt: 42000, st: "待 SD 批准", who: PEOPLE.sales, at: "1d 前", note: "心电监护导联线 · 浸提条件变更" },
      { id: "P-2026-0019", c: "康源生物", amt: 56300, st: "待评审", who: PEOPLE.sales, at: "2d 前", note: "心脏起搏器外壳 · 新增 ISO 10993-11 测试" },
    ],
    report: [
      { id: "P-2026-0007", c: "某创新医疗", amt: 128500, st: "QA 已过 · 待 SD 终审", who: PEOPLE.qa, at: "2d 前", note: "一次性导尿管 · 报告 v2" },
      { id: "P-2025-0182", c: "锐康医疗", amt: 28500, st: "待 SD 批准", who: PEOPLE.qa, at: "1d 前", note: "壳聚糖创伤敷料 · 无菌报告" },
    ],
    green: [
      { id: "P-2026-0023", c: "聚明医械", amt: 89200, st: "QA + 财务 已签 · 待终审", who: PEOPLE.finance, at: "1d 前", note: "申请先发后付 · 客户 IPO 前急用" },
    ],
    payment: [
      { id: "P-2026-0014", c: "通源医疗", amt: 28500, st: "财务初核已过 · 复核", who: PEOPLE.finance, at: "12h 前", note: "银行转账 · 实收 28,500" },
    ],
  };

  return (
    <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "#F8FAFC" }}>
      <div style={{ background: "#fff", borderBottom: "1px solid var(--line)", flexShrink: 0 }}>
        <MobileStatusBar />
        <div style={{ padding: "8px 18px 14px" }}>
          <h2 style={{ margin: 0, fontSize: 18, fontWeight: 600 }}>待审批 <span style={{ fontSize: 12, color: "var(--ink-400)", fontWeight: 400 }}>13 项</span></h2>
        </div>
        {/* Segment */}
        <div style={{ padding: "0 14px 12px", display: "flex", gap: 6, overflowX: "auto" }}>
          {segs.map(([k, l, n]) => (
            <div key={k} onClick={() => setSeg(k)} style={{
              padding: "6px 14px", borderRadius: 18, fontSize: 12, cursor: "pointer", flexShrink: 0,
              background: seg === k ? "var(--primary)" : "var(--bg)",
              color: seg === k ? "#fff" : "var(--ink-700)",
              fontWeight: seg === k ? 600 : 400,
            }}>{l} · {n}</div>
          ))}
        </div>
      </div>

      <div style={{ flex: 1, overflowY: "auto", padding: "12px 14px" }}>
        {lists[seg].map((it) => (
          <div key={it.id} className="card" style={{ padding: 14, marginBottom: 10 }} onClick={() => setDetailOpen(it)}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 8 }}>
              <div>
                <div style={{ display: "flex", alignItems: "center", gap: 6, marginBottom: 4 }}>
                  <span className="mono" style={{ fontSize: 11, fontWeight: 600 }}>{it.id}</span>
                  <Tag tone="cyan" style={{ fontSize: 10 }}>{seg === "green" ? "绿色通道" : seg === "nonstd" ? "非标" : seg === "payment" ? "收款" : "报告"}</Tag>
                </div>
                <div style={{ fontSize: 13, fontWeight: 500 }}>{it.c}</div>
                <div style={{ fontSize: 11, color: "var(--ink-600)", marginTop: 2, lineHeight: 1.4 }}>{it.note}</div>
              </div>
              <div style={{ textAlign: "right" }}>
                <div className="mono" style={{ fontSize: 14, fontWeight: 600, color: "var(--primary)" }}>{money(it.amt)}</div>
                <Tag tone="amber" style={{ fontSize: 10, marginTop: 4 }}>{it.st}</Tag>
              </div>
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 11, color: "var(--ink-600)", paddingTop: 8, borderTop: "1px dashed var(--line)" }}>
              <Avatar name={it.who.av} color={it.who.color} size={18} />
              <span>{it.who.name} {it.who.role} · {it.at}</span>
            </div>
          </div>
        ))}
      </div>

      {/* Detail panel */}
      {detailOpen && (
        <div style={{ position: "absolute", inset: 0, background: "#F8FAFC", zIndex: 50, display: "flex", flexDirection: "column" }}>
          <div style={{ background: "#fff", borderBottom: "1px solid var(--line)", flexShrink: 0 }}>
            <MobileStatusBar />
            <div style={{ padding: "8px 14px 12px", display: "flex", alignItems: "center", gap: 10 }}>
              <span onClick={() => setDetailOpen(null)} style={{ fontSize: 18 }}>←</span>
              <span style={{ fontSize: 15, fontWeight: 600 }}>审批详情</span>
            </div>
          </div>
          <div style={{ flex: 1, overflow: "auto", padding: "14px" }}>
            <div className="card" style={{ padding: 16, marginBottom: 12 }}>
              <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 10 }}>
                <span className="mono" style={{ fontSize: 12, fontWeight: 600 }}>{detailOpen.id}</span>
                <Tag tone="amber">{detailOpen.st}</Tag>
              </div>
              <div style={{ fontSize: 15, fontWeight: 600 }}>{detailOpen.c}</div>
              <div style={{ fontSize: 12, color: "var(--ink-600)", marginTop: 4, lineHeight: 1.6 }}>{detailOpen.note}</div>
              <div style={{ padding: 12, background: "var(--bg)", borderRadius: 6, marginTop: 12, display: "flex", justifyContent: "space-between" }}>
                <span style={{ fontSize: 12, color: "var(--ink-600)" }}>合同金额</span>
                <span className="mono" style={{ fontSize: 18, fontWeight: 600, color: "var(--primary)" }}>{money(detailOpen.amt)}</span>
              </div>
            </div>
            <div className="card" style={{ padding: 14, marginBottom: 12 }}>
              <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 8 }}>审批历史</div>
              {[
                ["陈业务", "提交申请", "blue"],
                ["刘老师 QA", "✓ 已审核通过", "green"],
                ["周教授 SD", "待审批", "amber"],
              ].map(([n, l, t], i) => (
                <div key={i} style={{ display: "flex", alignItems: "center", gap: 8, padding: "6px 0", fontSize: 12 }}>
                  <Avatar name={n.slice(0, 1)} color="#1A5FB4" size={20} />
                  <span style={{ flex: 1 }}>{n}</span>
                  <Tag tone={t}>{l}</Tag>
                </div>
              ))}
            </div>
          </div>
          <div style={{ padding: 14, background: "#fff", borderTop: "1px solid var(--line)", display: "flex", gap: 8 }}>
            <button className="btn btn-ghost" style={{ flex: 1 }} onClick={() => toast("已转发", "success")}>转发</button>
            <button className="btn btn-danger" style={{ flex: 1 }} onClick={() => { setDetailOpen(null); toast("已驳回", "danger"); }}>驳回</button>
            <button className="btn btn-success" style={{ flex: 1 }} onClick={() => setSheetOpen(true)}>通过</button>
          </div>

          {sheetOpen && (
            <div style={{ position: "absolute", inset: 0, background: "rgba(15,23,42,.4)", zIndex: 60, display: "flex", alignItems: "flex-end" }} onClick={() => setSheetOpen(false)}>
              <div onClick={(e) => e.stopPropagation()} style={{ width: "100%", background: "#fff", borderRadius: "16px 16px 0 0", padding: 18, animation: "slideUp .25s" }}>
                <div style={{ width: 36, height: 4, borderRadius: 2, background: "#CBD5E1", margin: "0 auto 16px" }} />
                <div style={{ fontSize: 16, fontWeight: 600, marginBottom: 12 }}>填写审批意见</div>
                <textarea className="f-textarea" rows="3" placeholder="例：同意通过，附加意见..." />
                <div style={{ marginTop: 12, display: "flex", gap: 8 }}>
                  <button className="btn btn-default" style={{ flex: 1 }} onClick={() => setSheetOpen(false)}>取消</button>
                  <button className="btn btn-success" style={{ flex: 2 }} onClick={() => { setSheetOpen(false); setDetailOpen(null); toast("✓ 已通过", "success"); }}>确认通过</button>
                </div>
                <style>{`@keyframes slideUp{from{transform:translateY(100%)}to{transform:none}}`}</style>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

/* ============ M3: Mobile Sample Scan ============ */
function M3_Scan() {
  const toast = useToast();
  const [scanned, setScanned] = useState(false);
  const [logging, setLogging] = useState(false);

  return (
    <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "#000", color: "#fff", position: "relative", overflow: "hidden" }}>
      {/* Camera viewfinder */}
      <div style={{ position: "absolute", inset: 0, background: "radial-gradient(circle at 50% 40%, #1E2E4A 0%, #000 70%)" }} />

      {/* Top bar */}
      <div style={{ position: "relative", flexShrink: 0 }}>
        <MobileStatusBar dark />
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "4px 16px 12px", color: "#fff" }}>
          <span style={{ fontSize: 22 }}>×</span>
          <span style={{ fontSize: 15, fontWeight: 600 }}>样品扫码入库</span>
          <span style={{ fontSize: 18 }}>⚡</span>
        </div>
      </div>

      {/* Scan area */}
      <div style={{ flex: 1, position: "relative", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", padding: "0 40px" }}>
        <div style={{ fontSize: 12, marginBottom: 32, opacity: .85, textAlign: "center" }}>对准样品标签 QR 码或条形码</div>
        <div style={{ width: 260, height: 260, border: "2px solid rgba(0,168,168,.6)", borderRadius: 16, position: "relative", overflow: "hidden", background: "rgba(0,168,168,.04)" }}>
          {/* Corner brackets */}
          {[[0, 0, "tl"], [0, 1, "tr"], [1, 0, "bl"], [1, 1, "br"]].map(([y, x, k]) => (
            <div key={k} style={{
              position: "absolute", width: 30, height: 30,
              [y ? "bottom" : "top"]: -2, [x ? "right" : "left"]: -2,
              borderTop: !y ? "4px solid #00A8A8" : "none",
              borderBottom: y ? "4px solid #00A8A8" : "none",
              borderLeft: !x ? "4px solid #00A8A8" : "none",
              borderRight: x ? "4px solid #00A8A8" : "none",
              borderRadius: 8,
            }} />
          ))}
          {/* Animated scan line */}
          <div style={{ position: "absolute", left: 0, right: 0, height: 2, background: "linear-gradient(90deg,transparent,#00A8A8,transparent)", boxShadow: "0 0 12px #00A8A8", animation: "scanmove 2.4s linear infinite", top: "50%" }} />
          <style>{`@keyframes scanmove{0%{top:5%}50%{top:95%}100%{top:5%}}`}</style>
          {/* Center "QR" hint */}
          <div style={{ position: "absolute", inset: 80, border: "1px dashed rgba(255,255,255,.2)", borderRadius: 6, display: "flex", alignItems: "center", justifyContent: "center", color: "rgba(255,255,255,.5)", fontFamily: "JetBrains Mono,monospace", fontSize: 10 }}>QR / Code 128</div>
        </div>
        <div style={{ marginTop: 24, fontSize: 12, opacity: .7, textAlign: "center" }}>距离 15 ~ 25cm · 保持稳定</div>

        {!scanned && (
          <button className="btn btn-success btn-lg" style={{ marginTop: 28, padding: "12px 32px" }} onClick={() => { setScanned(true); toast("✓ 扫描成功", "success"); }}>
            模拟扫描成功
          </button>
        )}
      </div>

      {/* History */}
      <div style={{ position: "relative", padding: "14px 16px", background: "rgba(0,0,0,.5)", borderTop: "1px solid rgba(255,255,255,.1)" }}>
        <div style={{ fontSize: 11, color: "rgba(255,255,255,.6)", marginBottom: 6 }}>最近扫描</div>
        <div style={{ display: "flex", gap: 10, overflow: "auto" }}>
          {["SUB-2026-0011", "SUB-2026-0014", "SUB-2026-0017", "SUB-2026-0019"].map((id) => (
            <div key={id} className="mono" style={{ padding: "5px 10px", border: "1px solid rgba(255,255,255,.2)", borderRadius: 6, fontSize: 11, flexShrink: 0 }}>{id}</div>
          ))}
        </div>
      </div>

      {/* Bottom sheet on scan */}
      {scanned && (
        <div style={{ position: "absolute", inset: 0, background: "rgba(0,0,0,.6)", zIndex: 50, display: "flex", alignItems: "flex-end" }} onClick={() => setScanned(false)}>
          <div onClick={(e) => e.stopPropagation()} style={{ width: "100%", background: "#fff", color: "var(--ink-900)", borderRadius: "20px 20px 0 0", maxHeight: "78%", overflowY: "auto", animation: "slideUp .3s" }}>
            <div style={{ padding: "12px 18px 0", display: "flex", justifyContent: "space-between" }}>
              <div style={{ width: 36, height: 4, borderRadius: 2, background: "#CBD5E1", margin: "0 auto" }} />
            </div>
            <div style={{ padding: 18 }}>
              <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 12 }}>
                <Tag tone="green" dot>✓ 扫描成功</Tag>
                <span className="mono" style={{ fontSize: 13, fontWeight: 600 }}>SUB-2026-0001</span>
              </div>
              <div style={{ fontSize: 16, fontWeight: 600 }}>{SAMPLE.zh}</div>
              <div className="mono" style={{ fontSize: 11, color: "var(--ink-600)" }}>{SAMPLE.en}</div>

              <div style={{ marginTop: 14, padding: 12, background: "var(--bg)", borderRadius: 8, fontSize: 12 }}>
                <div style={{ display: "flex", justifyContent: "space-between", padding: "4px 0" }}><span style={{ color: "var(--ink-600)" }}>委托单位</span><span>无锡市南京大学锡山所</span></div>
                <div style={{ display: "flex", justifyContent: "space-between", padding: "4px 0" }}><span style={{ color: "var(--ink-600)" }}>送检数量</span><span>12 片</span></div>
                <div style={{ display: "flex", justifyContent: "space-between", padding: "4px 0" }}><span style={{ color: "var(--ink-600)" }}>批号</span><span className="mono">{SAMPLE.lot}</span></div>
                <div style={{ display: "flex", justifyContent: "space-between", padding: "4px 0" }}><span style={{ color: "var(--ink-600)" }}>储存条件</span><span>常温干燥处</span></div>
              </div>

              <div style={{ marginTop: 12, fontSize: 13, fontWeight: 500, marginBottom: 8 }}>核验</div>
              {["名称匹配", "数量匹配", "包装完好"].map((c) => (
                <label key={c} style={{ display: "flex", alignItems: "center", gap: 10, padding: "8px 12px", border: "1px solid #A7F3D0", background: "var(--success-50)", borderRadius: 6, marginBottom: 6, fontSize: 12 }}>
                  <input type="checkbox" defaultChecked />
                  <span style={{ flex: 1 }}>{c}</span>
                  <Tag tone="green">✓</Tag>
                </label>
              ))}

              <div style={{ display: "flex", gap: 8, marginTop: 14 }}>
                <button className="btn btn-default" style={{ flex: 1 }}>📷 拍照</button>
                <button className="btn btn-danger" style={{ flex: 1 }}>异常</button>
                <button className="btn btn-success" style={{ flex: 2 }} onClick={() => { setScanned(false); setLogging(true); toast("✓ 入库成功 · 项目状态推进", "success"); setTimeout(() => setLogging(false), 1500); }}>正式入库</button>
              </div>
            </div>
            <style>{`@keyframes slideUp{from{transform:translateY(100%)}to{transform:none}}`}</style>
          </div>
        </div>
      )}
    </div>
  );
}

/* ============ M4: Mobile Work Center ============ */
function M4_WorkCenter() {
  const pieRef = useChart(() => ({
    series: [{
      type: "pie", radius: ["55%", "85%"], avoidLabelOverlap: false, label: { show: false },
      data: [
        { value: 38, name: "正常", itemStyle: { color: "#10B981" } },
        { value: 11, name: "预警", itemStyle: { color: "#F59E0B" } },
        { value: 5, name: "风险", itemStyle: { color: "#EF4444" } },
      ],
    }],
    graphic: [
      { type: "text", left: "center", top: "40%", style: { text: "92", fontFamily: "Inter", fontSize: 38, fontWeight: 600, fill: "#0F172A" } },
      { type: "text", left: "center", top: "60%", style: { text: "整体健康度", fontSize: 11, fill: "#94A3B8" } },
    ],
  }), []);

  const trendRef = useChart(() => ({
    grid: { left: 28, right: 12, top: 18, bottom: 24 },
    xAxis: { type: "category", data: ["W14", "W15", "W16", "W17", "W18", "W19"], axisLine: { lineStyle: { color: "#E2E8F0" } }, axisLabel: { color: "#94A3B8", fontSize: 9, fontFamily: "Inter" } },
    yAxis: { type: "value", splitLine: { lineStyle: { color: "#F1F5F9" } }, axisLabel: { color: "#94A3B8", fontSize: 9, fontFamily: "Inter" } },
    series: [
      { type: "line", smooth: true, data: [18, 22, 25, 28, 30, 32], itemStyle: { color: "#94A3B8" }, lineStyle: { type: "dashed", width: 1.5 }, symbol: "circle", symbolSize: 4 },
      { type: "line", smooth: true, data: [16, 21, 24, 27, 31, 29], itemStyle: { color: "#1A5FB4" }, lineStyle: { width: 2 }, areaStyle: { color: "rgba(26,95,180,.15)" }, symbol: "circle", symbolSize: 5 },
    ],
  }), []);

  return (
    <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "#F8FAFC" }}>
      <div style={{ background: "#fff", borderBottom: "1px solid var(--line)", flexShrink: 0 }}>
        <MobileStatusBar />
        <div style={{ padding: "8px 18px 14px", display: "flex", alignItems: "center", gap: 10 }}>
          <span style={{ fontSize: 18, color: "var(--ink-600)" }}>←</span>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 16, fontWeight: 600 }}>工作中心</div>
            <div style={{ fontSize: 11, color: "var(--ink-600)" }}>54 项进行中 · 05-13 09:42</div>
          </div>
          <span style={{ fontSize: 20 }}>⋮</span>
        </div>
      </div>

      <div style={{ flex: 1, overflowY: "auto", paddingBottom: 24 }}>
        {/* Stat scroller */}
        <div style={{ padding: "12px 14px", display: "flex", gap: 8, overflowX: "auto" }}>
          {[
            ["进行中", "54", "#1A5FB4"],
            ["我的待办", "12", "#00A8A8"],
            ["超期", "5", "#EF4444"],
            ["本周完成", "29", "#10B981"],
            ["报告审核", "7", "#F59E0B"],
          ].map(([l, v, c]) => (
            <div key={l} style={{ minWidth: 100, padding: "12px 14px", background: "#fff", border: "1px solid var(--line)", borderRadius: 10, flexShrink: 0, position: "relative", overflow: "hidden" }}>
              <div style={{ position: "absolute", left: 0, top: 0, bottom: 0, width: 3, background: c }} />
              <div style={{ fontSize: 11, color: "var(--ink-600)" }}>{l}</div>
              <div className="mono" style={{ fontSize: 22, fontWeight: 600, marginTop: 2 }}>{v}</div>
            </div>
          ))}
        </div>

        {/* Health donut */}
        <div className="card" style={{ margin: "0 14px 12px", padding: 16 }}>
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 8 }}>
            <span style={{ fontSize: 13, fontWeight: 600 }}>项目健康度</span>
            <span style={{ fontSize: 11, color: "var(--ink-400)" }}>54 项 · 实时</span>
          </div>
          <div ref={pieRef} style={{ width: "100%", height: 200 }} />
          <div style={{ display: "flex", justifyContent: "space-around", paddingTop: 8, borderTop: "1px solid var(--line-soft)", fontSize: 11 }}>
            {[["正常", 38, "#10B981"], ["预警", 11, "#F59E0B"], ["风险", 5, "#EF4444"]].map(([l, n, c]) => (
              <div key={l} style={{ textAlign: "center" }}>
                <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 4, color: "var(--ink-600)" }}><span style={{ width: 6, height: 6, borderRadius: 1, background: c }}></span>{l}</div>
                <div className="mono" style={{ fontSize: 16, fontWeight: 600, color: "var(--ink-900)", marginTop: 2 }}>{n}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Blockers */}
        <div className="card" style={{ margin: "0 14px 12px", padding: 16 }}>
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 12 }}>
            <span style={{ fontSize: 13, fontWeight: 600 }}>阻塞清单</span>
            <Tag tone="red">24 项</Tag>
          </div>
          {[
            { id: "P-2026-0001", tone: "amber", t: "5h", title: "非标方法待 SD 批准", who: "陈业务" },
            { id: "P-2026-0007", tone: "red", t: "2d", title: "报告 v2 待 SD 终审", who: "周教授" },
            { id: "P-2026-0012", tone: "red", t: "3d", title: "HPLC 与外部冲突 · 已申请覆盖", who: "李工程师" },
          ].map((b) => (
            <div key={b.id} style={{ display: "flex", gap: 10, padding: "10px 0", borderBottom: "1px dashed var(--line)", alignItems: "center" }}>
              <div style={{ width: 4, alignSelf: "stretch", background: b.tone === "red" ? "var(--danger)" : "var(--warning)", borderRadius: 2 }} />
              <div style={{ flex: 1 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                  <span className="mono" style={{ fontSize: 10, color: "var(--ink-600)" }}>{b.id}</span>
                  <Tag tone={b.tone} style={{ fontSize: 10 }}>{b.t}</Tag>
                </div>
                <div style={{ fontSize: 13, fontWeight: 500, marginTop: 2 }}>{b.title}</div>
                <div style={{ fontSize: 11, color: "var(--ink-600)" }}>负责人 · {b.who}</div>
              </div>
              <span style={{ color: "var(--ink-400)" }}>›</span>
            </div>
          ))}
        </div>

        {/* Trend */}
        <div className="card" style={{ margin: "0 14px 12px", padding: 16 }}>
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 8 }}>
            <span style={{ fontSize: 13, fontWeight: 600 }}>近 6 周进度</span>
            <div style={{ display: "flex", gap: 10, fontSize: 10, color: "var(--ink-600)" }}>
              <span style={{ display: "inline-flex", alignItems: "center", gap: 4 }}><span style={{ width: 10, height: 1, borderTop: "1.5px dashed #94A3B8" }}></span>计划</span>
              <span style={{ display: "inline-flex", alignItems: "center", gap: 4 }}><span style={{ width: 10, height: 2, background: "#1A5FB4" }}></span>实际</span>
            </div>
          </div>
          <div ref={trendRef} style={{ width: "100%", height: 150 }} />
        </div>

        {/* People */}
        <div className="card" style={{ margin: "0 14px 12px", padding: 16 }}>
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 12 }}>
            <span style={{ fontSize: 13, fontWeight: 600 }}>人员负荷预警</span>
          </div>
          {[["赵工 QC", 100, "var(--danger)"], ["李工程师", 95, "var(--warning)"], ["张博士", 78, "var(--primary)"]].map(([nm, p, c]) => (
            <div key={nm} style={{ padding: "8px 0", borderBottom: "1px dashed var(--line)" }}>
              <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12 }}>
                <span>{nm}</span><span className="mono" style={{ color: c, fontWeight: 600 }}>{p}%</span>
              </div>
              <div style={{ height: 4, background: "var(--line)", borderRadius: 2, marginTop: 4 }}>
                <div style={{ height: "100%", width: Math.min(100, p) + "%", background: c, borderRadius: 2 }} />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { M1_Home, M2_Approvals, M3_Scan, M4_WorkCenter });
