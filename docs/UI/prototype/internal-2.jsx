/* ====================================================================
   INTERNAL · P7 Sample Desk · P8 Project Detail
   ==================================================================== */

/* ============ P7: Sample Reception Desk ============ */
function P7_SampleDesk() {
  const toast = useToast();
  const [scanned, setScanned] = useState(true);
  const [stage, setStage] = useState("loaded"); // loaded -> prelogged -> logged
  const [checks, setChecks] = useState({ name: true, qty: true, pack: true, storage: true, cold: false });
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [photoCount] = useState(4);

  return (
    <DesktopShell side={<InternalSidebar active="sample-desk" />} top={<TopBar crumbs={["业务", "样品收样台"]} user={PEOPLE.sample} />}>
      <div style={{ padding: 24, display: "grid", gridTemplateColumns: "1.5fr 1fr", gap: 16, minHeight: "calc(100vh - 56px)" }}>
        <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
          {/* Scan box */}
          <div className="card" style={{ padding: 24, background: "linear-gradient(135deg,#0E3A6E 0%,#1A5FB4 100%)", color: "#fff" }}>
            <div style={{ fontSize: 11, opacity: .7, letterSpacing: ".18em", fontFamily: "Inter,sans-serif", textTransform: "uppercase", marginBottom: 8 }}>SCAN / INPUT</div>
            <div style={{ display: "flex", gap: 12, alignItems: "center" }}>
              <div style={{ width: 56, height: 56, borderRadius: 12, background: "rgba(255,255,255,.15)", border: "1px solid rgba(255,255,255,.25)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 26 }}>⊞</div>
              <div style={{ flex: 1 }}>
                <input
                  className="mono"
                  defaultValue="SUB-2026-0001"
                  style={{ width: "100%", background: "rgba(255,255,255,.10)", border: "1px solid rgba(255,255,255,.25)", color: "#fff", fontSize: 18, padding: "10px 14px", borderRadius: 8, outline: "none", fontFamily: "JetBrains Mono,monospace", letterSpacing: ".05em" }}
                  placeholder="扫描或输入送检单号 / 样品标签..."
                />
                <div style={{ fontSize: 11, opacity: .7, marginTop: 8 }}>支持 QR 码 / Code 128 条形码 · 最近扫描 SUB-2026-0001 (12s 前)</div>
              </div>
            </div>
          </div>

          {scanned && (
            <>
              {/* Submission card */}
              <div className="card" style={{ padding: 20 }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 14 }}>
                  <div>
                    <div style={{ display: "flex", gap: 8, alignItems: "center", marginBottom: 4 }}>
                      <span className="mono" style={{ fontSize: 14, fontWeight: 600 }}>SUB-2026-0001</span>
                      <Tag tone="blue" dot>已受理 · 等待收样</Tag>
                      <Tag tone="cyan">加急 P1</Tag>
                    </div>
                    <div style={{ fontSize: 16, fontWeight: 600 }}>{SAMPLE.zh}</div>
                    <div className="mono" style={{ fontSize: 12, color: "var(--ink-600)" }}>{SAMPLE.en}</div>
                  </div>
                  <div style={{ textAlign: "right" }}>
                    <div style={{ fontSize: 11, color: "var(--ink-400)" }}>预计到达</div>
                    <div className="mono" style={{ fontSize: 14, fontWeight: 600 }}>2026-05-13 14:30</div>
                    <div style={{ fontSize: 11, color: "var(--success)" }}>● 已到达 · 准点</div>
                  </div>
                </div>
                <div style={{ display: "grid", gridTemplateColumns: "repeat(4,1fr)", gap: 16, padding: "14px 0", borderTop: "1px solid var(--line)", fontSize: 12 }}>
                  <KV k="委托单位" v={ORG.zh} />
                  <KV k="送检数量" v="12 片" />
                  <KV k="批号" v={<span className="mono">{SAMPLE.lot}</span>} />
                  <KV k="储存条件" v="常温干燥处" />
                </div>
              </div>

              {/* Checklist */}
              <div className="card" style={{ padding: 20 }}>
                <h3 style={{ margin: "0 0 14px", fontSize: 14, fontWeight: 600 }}>核验清单 <span style={{ fontSize: 11, color: "var(--ink-400)", fontWeight: 400, marginLeft: 6 }}>5 项 · 全部勾选后可入库</span></h3>
                <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                  {[
                    ["name", "样品名称与送检单一致", "Disposable hydrocolloid dressing"],
                    ["qty", "送检数量匹配", "送检 12 片 · 实际 12 片"],
                    ["pack", "包装完好无破损", "未拆封、无污渍"],
                    ["storage", "储存条件符合要求", "常温干燥 · OK"],
                    ["cold", "冷链温度记录", "本样品无冷链要求"],
                  ].map(([k, l, sub]) => (
                    <label key={k} style={{ display: "flex", alignItems: "center", gap: 12, padding: 12, border: "1px solid " + (checks[k] ? "#A7F3D0" : "var(--line)"), borderRadius: 8, background: checks[k] ? "var(--success-50)" : "#fff", cursor: "pointer" }}>
                      <input type="checkbox" checked={checks[k]} onChange={(e) => setChecks({ ...checks, [k]: e.target.checked })} />
                      <div style={{ flex: 1 }}>
                        <div style={{ fontSize: 13, fontWeight: 500 }}>{l}</div>
                        <div style={{ fontSize: 11, color: "var(--ink-600)", marginTop: 2 }}>{sub}</div>
                      </div>
                      {checks[k] && <Tag tone="green">✓ 已核验</Tag>}
                    </label>
                  ))}
                </div>
              </div>

              {/* Photo upload */}
              <div className="card" style={{ padding: 20 }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
                  <h3 style={{ margin: 0, fontSize: 14, fontWeight: 600 }}>样品照片 <span style={{ fontSize: 11, color: "var(--ink-400)", fontWeight: 400, marginLeft: 6 }}>已上传 {photoCount} / 6</span></h3>
                  <button className="btn btn-ghost" style={{ fontSize: 12 }}>📷 重拍</button>
                </div>
                <div style={{ display: "grid", gridTemplateColumns: "repeat(6,1fr)", gap: 10 }}>
                  {[1, 2, 3, 4].map((i) => (
                    <div key={i} style={{ aspectRatio: "1", borderRadius: 6, background: "linear-gradient(135deg,#E2E8F0,#CBD5E1)", position: "relative", overflow: "hidden", border: "1px solid var(--line)" }}>
                      <div style={{ position: "absolute", inset: 0, background: "repeating-linear-gradient(45deg,#E2E8F0,#E2E8F0 3px,#CBD5E1 3px,#CBD5E1 7px)", opacity: .8 }}></div>
                      <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center", color: "var(--ink-600)", fontFamily: "JetBrains Mono,monospace", fontSize: 9 }}>IMG_{i}</div>
                      <div style={{ position: "absolute", top: 4, right: 4, width: 16, height: 16, background: "rgba(0,0,0,.5)", color: "#fff", borderRadius: "50%", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 10, cursor: "pointer" }}>×</div>
                    </div>
                  ))}
                  {[5, 6].map((i) => (
                    <div key={i} style={{ aspectRatio: "1", borderRadius: 6, border: "2px dashed var(--line)", display: "flex", alignItems: "center", justifyContent: "center", color: "var(--ink-400)", cursor: "pointer" }}>
                      <span style={{ fontSize: 22 }}>＋</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Actions */}
              <div className="card" style={{ padding: 20, position: "sticky", bottom: 0 }}>
                <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
                  <button className="btn btn-default" onClick={() => toast("已上传异常照片", "warning")}>📷 拍照异常</button>
                  <button className="btn btn-default" onClick={() => toast("已发起联系客户", "success")}>✉ 联系客户</button>
                  <button className="btn btn-danger" onClick={() => setConfirmOpen(true)}>↩ 退回受理</button>
                  <div style={{ flex: 1 }} />
                  {stage === "loaded" && <button className="btn btn-default btn-lg" onClick={() => { setStage("prelogged"); toast("样品已预入库 · 待主检确认", "success"); }}>样品预入库</button>}
                  {stage === "prelogged" && <button className="btn btn-success btn-lg" onClick={() => { setStage("logged"); toast("样品已正式入库 · 项目状态推进至 SAMPLE_RECEIVED", "success"); }}>样品正式入库 ✓</button>}
                  {stage === "logged" && <Tag tone="green" dot>✓ 已正式入库 · 2026-05-13 14:32</Tag>}
                </div>
                <div style={{ marginTop: 10, padding: 10, background: "var(--bg)", borderRadius: 6, fontSize: 11, color: "var(--ink-600)" }}>
                  入库后自动：① 推进项目状态机；② 生成样品标签 LBL-{SAMPLE.lot}；③ 通知 SD/主检；④ 创建试验工单。
                </div>
              </div>
            </>
          )}
        </div>

        {/* Right column */}
        <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
          <div className="card" style={{ padding: 18 }}>
            <h3 style={{ margin: "0 0 14px", fontSize: 14, fontWeight: 600 }}>今日待收 <span style={{ fontSize: 11, color: "var(--ink-400)", fontWeight: 400 }}>5 / 12</span></h3>
            <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
              {[
                ["SUB-2026-0001", "妥能水胶体敷贴", "14:30", "current"],
                ["SUB-2026-0024", "PU 泡沫敷料", "15:00", "wait"],
                ["SUB-2026-0026", "硅胶导尿管 Foley", "15:30", "wait"],
                ["SUB-2026-0028", "壳聚糖创伤敷料", "16:00", "wait"],
                ["SUB-2026-0029", "医用棉签 (无菌)", "16:30", "wait"],
              ].map(([id, nm, t, st]) => (
                <div key={id} style={{ padding: 10, border: "1px solid " + (st === "current" ? "var(--primary)" : "var(--line)"), borderRadius: 6, background: st === "current" ? "var(--primary-50)" : "#fff" }}>
                  <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 2 }}>
                    <span className="mono" style={{ fontSize: 11, fontWeight: 600 }}>{id}</span>
                    <span className="mono" style={{ fontSize: 11, color: st === "current" ? "var(--primary)" : "var(--ink-400)" }}>{t}</span>
                  </div>
                  <div style={{ fontSize: 12 }}>{nm}</div>
                  {st === "current" && <Tag tone="cyan" style={{ marginTop: 4 }}>正在处理</Tag>}
                </div>
              ))}
            </div>
          </div>

          <div className="card" style={{ padding: 18 }}>
            <h3 style={{ margin: "0 0 14px", fontSize: 14, fontWeight: 600 }}>今日已收 <span style={{ fontSize: 11, color: "var(--ink-400)", fontWeight: 400 }}>7 项 · 1 异常</span></h3>
            <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
              {[
                ["SUB-2026-0011", "10:32", "g", "正常入库"],
                ["SUB-2026-0014", "11:05", "r", "数量不符 · 已联系客户"],
                ["SUB-2026-0017", "11:48", "g", "正常入库"],
                ["SUB-2026-0019", "13:20", "g", "正常入库"],
              ].map(([id, t, tone, ds]) => (
                <div key={id} style={{ display: "flex", alignItems: "center", gap: 10, padding: "6px 0", borderBottom: "1px dashed var(--line)", fontSize: 12 }}>
                  <span className="mono" style={{ color: "var(--ink-400)", fontSize: 11, width: 50 }}>{t}</span>
                  <span className="mono" style={{ fontSize: 11, width: 110 }}>{id}</span>
                  <span style={{ flex: 1, color: tone === "r" ? "var(--danger)" : "var(--ink-700)" }}>{ds}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="card" style={{ padding: 18, background: "var(--bg-card)" }}>
            <h3 style={{ margin: "0 0 10px", fontSize: 14, fontWeight: 600 }}>本周统计</h3>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10, fontSize: 12 }}>
              <div><div style={{ color: "var(--ink-400)" }}>总收样</div><div className="mono" style={{ fontSize: 18, fontWeight: 600, color: "var(--ink-900)" }}>48</div></div>
              <div><div style={{ color: "var(--ink-400)" }}>异常率</div><div className="mono" style={{ fontSize: 18, fontWeight: 600, color: "var(--warning)" }}>4.2%</div></div>
              <div><div style={{ color: "var(--ink-400)" }}>平均耗时</div><div className="mono" style={{ fontSize: 18, fontWeight: 600 }}>6.8 <small style={{ fontSize: 11, color: "var(--ink-400)" }}>min</small></div></div>
              <div><div style={{ color: "var(--ink-400)" }}>SLA 达成</div><div className="mono" style={{ fontSize: 18, fontWeight: 600, color: "var(--success)" }}>98%</div></div>
            </div>
          </div>
        </div>
      </div>

      <Modal open={confirmOpen} onClose={() => setConfirmOpen(false)} title="确认退回受理？"
        footer={<>
          <button className="btn btn-default" onClick={() => setConfirmOpen(false)}>取消</button>
          <button className="btn btn-danger" onClick={() => { setConfirmOpen(false); toast("已退回业务并通知客户", "warning"); }}>确认退回</button>
        </>}>
        <div style={{ fontSize: 13, color: "var(--ink-700)", lineHeight: 1.8 }}>
          退回后项目状态回退至 <Tag tone="grey">ACCEPTED</Tag>，并触发：<br />
          · 通知业务员 {PEOPLE.sales.name}（短信 + 系统消息）<br />
          · 通知客户 李客户（邮件）<br />
          · 在审计日志记录退回原因
        </div>
        <Field label="退回原因" required>
          <textarea className="f-textarea" rows="3" placeholder="请填写退回原因（必填），将同步给客户..." />
        </Field>
      </Modal>
    </DesktopShell>
  );
}
function KV({ k, v }) {
  return (
    <div>
      <div style={{ color: "var(--ink-400)", fontSize: 11 }}>{k}</div>
      <div style={{ color: "var(--ink-900)", fontWeight: 500, marginTop: 2 }}>{v}</div>
    </div>
  );
}

/* ============ P8: Internal Project Detail ============ */
function P8_ProjectDetail({ onNavigate }) {
  const [tab, setTab] = useState("basic");
  const tabs = [
    ["basic", "基础信息"], ["sample", "样品"], ["plan", "试验计划"], ["worklist", "工单"],
    ["report", "报告"], ["payment", "收款"], ["archive", "档案"], ["audit", "审计日志"],
  ];
  return (
    <DesktopShell side={<InternalSidebar active="project" />} top={<TopBar crumbs={["项目管理", "P-2026-0001"]} />}>
      <div style={{ padding: 24 }}>
        {/* Header */}
        <div className="card" style={{ padding: 20, marginBottom: 16 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 14 }}>
            <div style={{ flex: 1 }}>
              <div style={{ display: "flex", gap: 10, alignItems: "center", marginBottom: 8, flexWrap: "wrap" }}>
                <span className="mono" style={{ fontSize: 15, fontWeight: 600 }}>P-2026-0001</span>
                <Tag tone="green">STD_ACCREDITED · 标准 + CNAS</Tag>
                <Tag tone="blue" dot>试验中</Tag>
                <Tag tone="cyan">加急 · P1</Tag>
                <Tag tone="amber">⚠ 阻塞 1 项</Tag>
              </div>
              <div style={{ display: "flex", alignItems: "baseline", gap: 12 }}>
                <h2 style={{ margin: 0, fontSize: 20, fontWeight: 600 }}>{SAMPLE.zh}</h2>
                <span className="mono" style={{ fontSize: 13, color: "var(--ink-600)" }}>{SAMPLE.en}</span>
              </div>
              <div style={{ fontSize: 12, color: "var(--ink-600)", marginTop: 6 }}>
                委托 {ORG.zh} · 受理 <span className="mono">2026-04-08</span> · 计划完成 <span className="mono">2026-05-22</span>
              </div>
            </div>
            <div style={{ display: "flex", gap: 8 }}>
              <button className="btn btn-default">作废项目</button>
              <button className="btn btn-default">导出送检单</button>
              <button className="btn btn-primary" onClick={() => onNavigate && onNavigate("P9")}>查看报告审核</button>
            </div>
          </div>
          {/* State machine flow */}
          <StateMachineFlow currentIndex={6} blockedIndex={9} />
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "1fr 320px", gap: 16 }}>
          <div className="card" style={{ padding: 0 }}>
            <div style={{ display: "flex", borderBottom: "1px solid var(--line)", padding: "0 24px" }}>
              {tabs.map(([k, l]) => (
                <div key={k} onClick={() => setTab(k)} style={{
                  padding: "14px 18px", fontSize: 13, cursor: "pointer",
                  color: tab === k ? "var(--primary)" : "var(--ink-700)",
                  fontWeight: tab === k ? 600 : 500,
                  borderBottom: "2px solid " + (tab === k ? "var(--primary)" : "transparent"),
                  marginBottom: -1,
                }}>{l}</div>
              ))}
            </div>
            <div style={{ padding: 24 }}>
              {tab === "basic" && <TabBasic />}
              {tab === "sample" && <TabSample />}
              {tab === "plan" && <TabPlan />}
              {tab === "worklist" && <TabWorklist />}
              {tab === "report" && <TabReport onNavigate={onNavigate} />}
              {tab === "payment" && <TabPayment />}
              {tab === "archive" && <TabArchive />}
              {tab === "audit" && <TabAudit />}
            </div>
          </div>

          {/* Right rail */}
          <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
            <div className="card" style={{ padding: 16 }}>
              <h3 style={{ margin: "0 0 12px", fontSize: 13, fontWeight: 600, color: "var(--ink-600)", letterSpacing: ".05em" }}>责任人</h3>
              <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                {[PEOPLE.sales, PEOPLE.sd, PEOPLE.qa, PEOPLE.test1].map((p) => (
                  <div key={p.name} style={{ display: "flex", alignItems: "center", gap: 10 }}>
                    <Avatar name={p.av} color={p.color} size={32} />
                    <div style={{ flex: 1 }}>
                      <div style={{ fontSize: 13, fontWeight: 500 }}>{p.name}</div>
                      <div style={{ fontSize: 11, color: "var(--ink-600)" }}>{p.role}</div>
                    </div>
                    <span style={{ fontSize: 14, color: "var(--ink-400)", cursor: "pointer" }}>✉</span>
                  </div>
                ))}
              </div>
            </div>
            <div className="card" style={{ padding: 16 }}>
              <h3 style={{ margin: "0 0 12px", fontSize: 13, fontWeight: 600, color: "var(--ink-600)", letterSpacing: ".05em" }}>健康度评分</h3>
              <div style={{ display: "flex", alignItems: "baseline", gap: 8 }}>
                <span className="mono" style={{ fontSize: 36, fontWeight: 600, color: "var(--success)" }}>92</span>
                <span style={{ fontSize: 12, color: "var(--ink-400)" }}>/ 100</span>
                <Tag tone="green" style={{ marginLeft: "auto" }}>正常</Tag>
              </div>
              <div style={{ height: 6, background: "var(--line)", borderRadius: 3, marginTop: 8, overflow: "hidden" }}>
                <div style={{ width: "92%", height: "100%", background: "linear-gradient(90deg,#1A5FB4,#10B981)" }} />
              </div>
              <div style={{ fontSize: 11, color: "var(--ink-600)", marginTop: 8, lineHeight: 1.7 }}>
                ▼ -8 加急未付款<br />
                <span style={{ color: "var(--ink-400)" }}>较上周 +3</span>
              </div>
            </div>
            <div className="card" style={{ padding: 16 }}>
              <h3 style={{ margin: "0 0 12px", fontSize: 13, fontWeight: 600, color: "var(--ink-600)", letterSpacing: ".05em" }}>付款状态</h3>
              <div style={{ display: "flex", alignItems: "center", gap: 10, padding: 12, background: "var(--danger-50)", border: "1px solid #FECACA", borderRadius: 6 }}>
                <div style={{ fontSize: 22 }}>🔒</div>
                <div>
                  <div style={{ fontSize: 13, fontWeight: 600, color: "var(--danger)" }}>未到账 UNPAID</div>
                  <div style={{ fontSize: 11, color: "var(--ink-600)" }}>应收 <span className="mono">{money(36800)}</span></div>
                </div>
              </div>
              <button className="btn btn-default btn-block" style={{ marginTop: 10 }}>申请绿色通道</button>
            </div>
            <div className="card" style={{ padding: 16 }}>
              <h3 style={{ margin: "0 0 10px", fontSize: 13, fontWeight: 600, color: "var(--ink-600)", letterSpacing: ".05em" }}>阻塞清单 · 1 项</h3>
              <div style={{ padding: 10, background: "var(--warning-50)", border: "1px solid #FDE68A", borderRadius: 6, fontSize: 12 }}>
                <div style={{ fontWeight: 600, color: "#92400E" }}>QC 复核待启动</div>
                <div style={{ color: "var(--ink-600)", marginTop: 4 }}>主检 {PEOPLE.test1.name} 待提交原始记录</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </DesktopShell>
  );
}

function TabBasic() {
  return (
    <div>
      <h3 style={{ margin: "0 0 14px", fontSize: 14, fontWeight: 600 }}>基础信息</h3>
      <table style={{ width: "100%", fontSize: 13, borderCollapse: "collapse" }}>
        <tbody>
          {[
            ["项目编号", <span className="mono">P-2026-0001</span>],
            ["送检单号", <span className="mono">SUB-2026-0001</span>],
            ["项目类型", <Tag tone="green">STD_ACCREDITED · 标准 + CNAS</Tag>],
            ["委托单位", `${ORG.zh} (${ORG.en})`],
            ["地址", ORG.addr],
            ["联系人", "李客户 · 138-xxxx-1234 · li.purchase@example-medical.cn"],
            ["签约日期", <span className="mono">2026-04-08</span>],
            ["合同金额", <span className="mono" style={{ color: "var(--primary)", fontWeight: 600 }}>{money(36800)}</span>],
            ["紧急程度", <Tag tone="cyan">加急 P1</Tag>],
            ["GLP", <Tag tone="grey">否</Tag>],
            ["报告语言", "中英双语 BOTH"],
            ["纸质件", "需要 · 寄送地址同委托单位"],
          ].map(([k, v]) => (
            <tr key={k} style={{ borderBottom: "1px solid var(--line-soft)" }}>
              <td style={{ padding: "10px 0", color: "var(--ink-600)", width: 140 }}>{k}</td>
              <td style={{ padding: "10px 0" }}>{v}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
function TabSample() {
  return (
    <div>
      <h3 style={{ margin: "0 0 14px", fontSize: 14, fontWeight: 600 }}>样品信息</h3>
      <div style={{ display: "flex", gap: 16 }}>
        <div style={{ width: 160, height: 160, background: "linear-gradient(135deg,#F1F5F9,#E2E8F0)", borderRadius: 8, position: "relative", overflow: "hidden", flexShrink: 0 }}>
          <div style={{ position: "absolute", inset: 0, background: "repeating-linear-gradient(45deg,#E2E8F0,#E2E8F0 3px,#F1F5F9 3px,#F1F5F9 8px)" }}></div>
          <div style={{ position: "absolute", bottom: 8, left: 8, fontFamily: "JetBrains Mono,monospace", fontSize: 10, color: "var(--ink-600)" }}>SAMPLE PHOTO</div>
        </div>
        <table style={{ flex: 1, borderCollapse: "collapse", fontSize: 13 }}>
          <tbody>
            {[
              ["样品名（中/英）", `${SAMPLE.zh} / ${SAMPLE.en}`],
              ["型号 · 规格", `${SAMPLE.model} · 1 片/袋`],
              ["批号", <span className="mono">{SAMPLE.lot}</span>],
              ["灭菌状态", "环氧乙烷灭菌 EO"],
              ["材料", "羧甲基纤维素钠 / 聚氨酯薄膜复合"],
              ["数量", "12 片 · 已入库"],
              ["保存条件", "常温干燥处"],
              ["入库时间", <span className="mono">2026-04-10 14:32</span>],
            ].map(([k, v]) => (
              <tr key={k} style={{ borderBottom: "1px solid var(--line-soft)" }}>
                <td style={{ padding: "6px 0", color: "var(--ink-600)", width: 120 }}>{k}</td>
                <td style={{ padding: "6px 0" }}>{v}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
function TabPlan() {
  return (
    <div>
      <h3 style={{ margin: "0 0 14px", fontSize: 14, fontWeight: 600 }}>试验计划</h3>
      <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13, border: "1px solid var(--line)" }}>
        <thead>
          <tr style={{ background: "var(--bg)" }}>
            <th style={{ padding: 10, textAlign: "left", borderBottom: "1px solid var(--line)" }}>试验项</th>
            <th style={{ padding: 10, textAlign: "left", borderBottom: "1px solid var(--line)" }}>方法</th>
            <th style={{ padding: 10, textAlign: "left", borderBottom: "1px solid var(--line)" }}>主检</th>
            <th style={{ padding: 10, textAlign: "left", borderBottom: "1px solid var(--line)" }}>设备</th>
            <th style={{ padding: 10, textAlign: "left", borderBottom: "1px solid var(--line)" }}>计划起止</th>
            <th style={{ padding: 10, textAlign: "left", borderBottom: "1px solid var(--line)" }}>状态</th>
          </tr>
        </thead>
        <tbody>
          {[
            ["无菌检查", "直接接种法", PEOPLE.test1.name, "HX-150 培养箱", "04-12 ~ 04-26", "blue", "试验中"],
          ].map((r) => (
            <tr key={r[0]} style={{ borderBottom: "1px solid var(--line-soft)" }}>
              <td style={{ padding: 10 }}>{r[0]} <span className="mono" style={{ fontSize: 10, color: "var(--ink-400)" }}>MICRO_STERILITY</span></td>
              <td style={{ padding: 10 }}>{r[1]}</td>
              <td style={{ padding: 10 }}><span style={{ display: "inline-flex", gap: 6, alignItems: "center" }}><Avatar name={r[2].slice(0, 1)} size={20} color="#1A5FB4" />{r[2]}</span></td>
              <td style={{ padding: 10, fontSize: 12 }}>{r[3]}</td>
              <td style={{ padding: 10 }} className="mono">{r[4]}</td>
              <td style={{ padding: 10 }}><Tag tone={r[5]} dot>{r[6]}</Tag></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
function TabWorklist() {
  return (
    <div>
      <h3 style={{ margin: "0 0 14px", fontSize: 14, fontWeight: 600 }}>试验工单</h3>
      <div className="card" style={{ padding: 16 }}>
        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 10 }}>
          <span className="mono" style={{ fontSize: 13, fontWeight: 600 }}>WO-2026-00001</span>
          <Tag tone="blue" dot>执行中</Tag>
        </div>
        <div style={{ fontSize: 13, marginBottom: 12 }}>无菌检测 · 直接接种法 · 主检 {PEOPLE.test1.name}</div>
        <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 12 }}>
          <tbody>
            <tr><td style={{ padding: 6, color: "var(--ink-600)" }}>开始</td><td className="mono">2026-04-12 09:00</td><td style={{ padding: 6, color: "var(--ink-600)" }}>预计完成</td><td className="mono">2026-04-26 09:00</td></tr>
            <tr><td style={{ padding: 6, color: "var(--ink-600)" }}>原始记录</td><td className="mono">RR-2026-00001 (草稿)</td><td style={{ padding: 6, color: "var(--ink-600)" }}>已耗工时</td><td className="mono">14.5 h</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  );
}
function TabReport({ onNavigate }) {
  return (
    <div>
      <h3 style={{ margin: "0 0 14px", fontSize: 14, fontWeight: 600 }}>报告</h3>
      <div className="card" style={{ padding: 16 }}>
        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 10 }}>
          <span className="mono" style={{ fontSize: 13, fontWeight: 600 }}>TR260408-03 · v1</span>
          <Tag tone="amber">草稿 · 待 QC 复核</Tag>
        </div>
        <div style={{ fontSize: 12, color: "var(--ink-600)", marginBottom: 12 }}>由 {PEOPLE.test1.name} 于 2026-05-13 提交，等待 {PEOPLE.qc.name} QC 复核</div>
        <button className="btn btn-primary" onClick={() => onNavigate && onNavigate("P9")}>进入审核界面</button>
      </div>
    </div>
  );
}
function TabPayment() {
  return (
    <div>
      <h3 style={{ margin: "0 0 14px", fontSize: 14, fontWeight: 600 }}>收款记录</h3>
      <div style={{ padding: 20, textAlign: "center", color: "var(--ink-400)", fontSize: 13 }}>
        当前应收 <span className="mono" style={{ color: "var(--ink-900)", fontWeight: 600, fontSize: 16 }}>{money(36800)}</span><br />
        付款未到账 · 报告处于付款门禁
      </div>
    </div>
  );
}
function TabArchive() {
  return (
    <div>
      <h3 style={{ margin: "0 0 14px", fontSize: 14, fontWeight: 600 }}>档案文件</h3>
      <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
        {[["主合同_P-2026-0001.pdf", "PDF", 280], ["送检单_SUB-2026-0001.pdf", "PDF", 156], ["原始记录_RR-2026-00001.pdf", "PDF", 89], ["样品照片.zip", "ZIP", 1200]].map((f) => (
          <div key={f[0]} style={{ display: "flex", alignItems: "center", gap: 12, padding: 10, border: "1px solid var(--line)", borderRadius: 6, fontSize: 12 }}>
            <div style={{ width: 28, height: 34, background: "#FEE2E2", color: "#991B1B", borderRadius: 3, display: "flex", alignItems: "center", justifyContent: "center", fontWeight: 700, fontSize: 9, fontFamily: "Inter,sans-serif" }}>{f[1]}</div>
            <span style={{ flex: 1 }}>{f[0]}</span>
            <span className="mono" style={{ color: "var(--ink-400)" }}>{f[2]} KB</span>
            <span style={{ color: "var(--primary)", cursor: "pointer" }}>下载</span>
          </div>
        ))}
      </div>
    </div>
  );
}
function TabAudit() {
  const events = [
    ["2026-05-13 11:24", PEOPLE.test1, "提交报告草稿 v1", "REPORT_DRAFT"],
    ["2026-05-13 09:00", PEOPLE.qc, "复核原始记录数据 (3/3 通过)", "DATA_REVIEW"],
    ["2026-05-12 17:15", PEOPLE.test1, "录入培养第 14 天观察数据", "—"],
    ["2026-04-26 09:30", PEOPLE.test1, "试验结束，原始记录草稿生成", "—"],
    ["2026-04-12 09:00", PEOPLE.test1, "试验开始，接种完成", "IN_TESTING"],
    ["2026-04-10 14:32", PEOPLE.sample, "样品正式入库 · 数量核验通过", "SAMPLE_RECEIVED"],
    ["2026-04-08 16:48", PEOPLE.sd, "试验计划已确认", "PLAN_CONFIRMED"],
    ["2026-04-08 11:20", PEOPLE.sales, "合同签订并归档", "CONTRACT_PENDING → ACCEPTED"],
  ];
  return (
    <div>
      <h3 style={{ margin: "0 0 14px", fontSize: 14, fontWeight: 600 }}>审计日志</h3>
      <div style={{ position: "relative", paddingLeft: 18 }}>
        <div style={{ position: "absolute", left: 5, top: 8, bottom: 8, width: 2, background: "var(--line)" }} />
        {events.map(([t, who, ds, st], i) => (
          <div key={i} style={{ position: "relative", paddingBottom: 14 }}>
            <div style={{ position: "absolute", left: -16, top: 5, width: 10, height: 10, borderRadius: "50%", background: i === 0 ? "var(--accent)" : "var(--primary)" }} />
            <div style={{ display: "flex", gap: 10, alignItems: "baseline", marginBottom: 2 }}>
              <span className="mono" style={{ fontSize: 11, color: "var(--ink-400)" }}>{t}</span>
              <Avatar name={who.av} color={who.color} size={18} />
              <span style={{ fontSize: 12, fontWeight: 500 }}>{who.name}</span>
              <Tag tone="grey" style={{ fontSize: 10, padding: "0 6px" }}>{who.role}</Tag>
            </div>
            <div style={{ fontSize: 13, marginLeft: 2 }}>{ds}</div>
            {st !== "—" && <div className="mono" style={{ fontSize: 10, color: "var(--ink-400)", marginTop: 2 }}>状态：{st}</div>}
          </div>
        ))}
      </div>
    </div>
  );
}

Object.assign(window, { P7_SampleDesk, P8_ProjectDetail });
