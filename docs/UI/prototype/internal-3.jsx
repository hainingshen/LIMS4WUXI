/* ====================================================================
   INTERNAL · P9 Report Review · P10 Payment · P11 Device Calendar · P12 System Config
   ==================================================================== */

/* ============ P9: Internal Report Review ============ */
function P9_ReportReview() {
  const toast = useToast();
  const [version, setVersion] = useState("v2");
  const [decision, setDecision] = useState(null);
  const [comment, setComment] = useState("");
  const stages = [
    { key: "DRAFT", l: "报告草稿", who: PEOPLE.test1, t: "05-13 11:24", done: true },
    { key: "QC", l: "QC 复核", who: PEOPLE.qc, t: "05-13 14:08", done: true },
    { key: "QA", l: "QA 审核", who: PEOPLE.qa, t: "进行中", done: false, current: true },
    { key: "SD", l: "SD 批准", who: PEOPLE.sd, t: "待启动", done: false },
    { key: "PAY", l: "付款门禁", who: null, t: "待启动", done: false, lock: true },
    { key: "REL", l: "发放", who: null, t: "待启动", done: false },
  ];

  return (
    <DesktopShell side={<InternalSidebar active="report-review" />} top={<TopBar crumbs={["业务", "报告审核", PROJECT.reportId]} user={PEOPLE.qa} />}>
      <div style={{ padding: 16, display: "grid", gridTemplateColumns: "1.55fr 1fr", gap: 16, minHeight: "calc(100vh - 56px)" }}>
        {/* Left: report viewer */}
        <div className="card" style={{ padding: 0, overflow: "hidden", display: "flex", flexDirection: "column" }}>
          {/* Toolbar */}
          <div style={{ padding: "10px 18px", borderBottom: "1px solid var(--line)", display: "flex", alignItems: "center", gap: 12, background: "#FAFBFD" }}>
            <span className="mono" style={{ fontSize: 13, fontWeight: 600 }}>{PROJECT.reportId}</span>
            <div style={{ display: "flex", gap: 0, background: "#fff", border: "1px solid var(--line)", borderRadius: 6, padding: 2 }}>
              {["v1", "v2"].map((v) => (
                <span key={v} onClick={() => setVersion(v)} style={{
                  padding: "3px 12px", fontSize: 12, cursor: "pointer", borderRadius: 4,
                  background: version === v ? "var(--primary)" : "transparent",
                  color: version === v ? "#fff" : "var(--ink-600)",
                  fontFamily: "Inter,sans-serif",
                }}>{v === "v1" ? "v1 旧版" : "v2 当前修订"}</span>
              ))}
            </div>
            <Tag tone="amber">3 处批注</Tag>
            <div style={{ flex: 1 }} />
            <span style={{ fontSize: 12, color: "var(--ink-600)", cursor: "pointer" }}>⇣ 导出 PDF (内部)</span>
          </div>

          <div style={{ flex: 1, overflow: "auto", padding: 30, background: "#E2E8F0" }}>
            <div style={{ background: "#fff", padding: 50, boxShadow: "0 8px 24px rgba(0,0,0,.15)", position: "relative", minHeight: 800, fontFamily: "Noto Sans SC", color: "#000" }}>
              <div className="watermark"><div className="watermark-text">DRAFT v{version[1]}</div></div>
              <div style={{ position: "relative", zIndex: 2 }}>
                <div style={{ textAlign: "center", marginBottom: 24 }}>
                  <div style={{ fontSize: 11, color: "var(--ink-600)", letterSpacing: ".3em", marginBottom: 8 }}>WUXI XISHAN NJU · LIMS</div>
                  <h1 style={{ fontSize: 22, fontWeight: 600, margin: 0 }}>检 测 报 告</h1>
                  <div style={{ fontSize: 11, color: "var(--ink-400)", fontFamily: "Inter", letterSpacing: ".18em", marginTop: 4 }}>TEST REPORT · {PROJECT.reportId}</div>
                </div>

                <h2 style={{ fontSize: 14, fontWeight: 600, borderBottom: "2px solid var(--primary)", paddingBottom: 4, marginTop: 24 }}>1. 样品信息</h2>
                <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 12 }}>
                  <tbody>
                    <tr><td style={{ padding: 6, border: "1px solid #94A3B8", background: "#F1F5F9", width: "25%" }}>样品名称</td><td style={{ padding: 6, border: "1px solid #94A3B8" }}>{SAMPLE.zh}</td></tr>
                    <tr><td style={{ padding: 6, border: "1px solid #94A3B8", background: "#F1F5F9" }}>批号</td>
                      <td style={{ padding: 6, border: "1px solid #94A3B8", position: "relative" }} className="mono">
                        {SAMPLE.lot}
                        <span style={{ display: "inline-block", marginLeft: 8, background: "#FEF3C7", color: "#92400E", padding: "1px 8px", fontSize: 10, borderRadius: 4, fontFamily: "Noto Sans SC" }}>批注 #1 by 刘 QA · 与原始记录核对</span>
                      </td>
                    </tr>
                    <tr><td style={{ padding: 6, border: "1px solid #94A3B8", background: "#F1F5F9" }}>检测项目</td><td style={{ padding: 6, border: "1px solid #94A3B8" }}>无菌检查</td></tr>
                  </tbody>
                </table>

                <h2 style={{ fontSize: 14, fontWeight: 600, borderBottom: "2px solid var(--primary)", paddingBottom: 4, marginTop: 20 }}>2. 检测结果</h2>
                <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 12 }}>
                  <thead><tr style={{ background: "#F1F5F9" }}>
                    <th style={{ padding: 6, border: "1px solid #94A3B8" }}>编号</th>
                    <th style={{ padding: 6, border: "1px solid #94A3B8" }}>培养基</th>
                    <th style={{ padding: 6, border: "1px solid #94A3B8" }}>温度</th>
                    <th style={{ padding: 6, border: "1px solid #94A3B8" }}>时间</th>
                    <th style={{ padding: 6, border: "1px solid #94A3B8" }}>结果</th>
                  </tr></thead>
                  <tbody>
                    <tr><td style={{ padding: 6, border: "1px solid #94A3B8" }} className="mono">1#</td><td style={{ padding: 6, border: "1px solid #94A3B8" }}>FTM</td><td style={{ padding: 6, border: "1px solid #94A3B8" }} className="mono">30~35°C</td><td style={{ padding: 6, border: "1px solid #94A3B8" }} className="mono">14 d</td><td style={{ padding: 6, border: "1px solid #94A3B8", color: "var(--success)", fontWeight: 600 }}>无菌生长</td></tr>
                    <tr><td style={{ padding: 6, border: "1px solid #94A3B8" }} className="mono">2#</td><td style={{ padding: 6, border: "1px solid #94A3B8" }}>FTM</td><td style={{ padding: 6, border: "1px solid #94A3B8" }} className="mono">30~35°C</td><td style={{ padding: 6, border: "1px solid #94A3B8" }} className="mono">14 d</td><td style={{ padding: 6, border: "1px solid #94A3B8", color: "var(--success)", fontWeight: 600 }}>无菌生长</td></tr>
                    <tr style={{ background: "#FEF3C7" }}>
                      <td style={{ padding: 6, border: "1px solid #94A3B8" }} className="mono">3#</td><td style={{ padding: 6, border: "1px solid #94A3B8" }}>TSB</td><td style={{ padding: 6, border: "1px solid #94A3B8" }} className="mono">20~25°C</td><td style={{ padding: 6, border: "1px solid #94A3B8" }} className="mono">14 d</td>
                      <td style={{ padding: 6, border: "1px solid #94A3B8", color: "var(--success)", fontWeight: 600 }}>无菌生长 <span style={{ background: "#FEE2E2", color: "#991B1B", padding: "1px 6px", fontSize: 10, borderRadius: 4, marginLeft: 6 }}>QA #2 · 已确认</span></td>
                    </tr>
                  </tbody>
                </table>

                <h2 style={{ fontSize: 14, fontWeight: 600, borderBottom: "2px solid var(--primary)", paddingBottom: 4, marginTop: 20 }}>3. 结论</h2>
                <div style={{ padding: 14, background: "#ECFDF5", border: "2px solid #10B981", borderRadius: 4, fontSize: 13, lineHeight: 1.7, position: "relative" }}>
                  所送 4 份样品在规定条件下培养 14 天，结果均显示<b style={{ color: "var(--success)" }}>无菌生长</b>，阳性对照菌生长良好，试验有效。
                  <span style={{ position: "absolute", top: -10, right: 10, background: "#FEF3C7", color: "#92400E", padding: "2px 8px", fontSize: 10, borderRadius: 4 }}>QA #3 · 建议补充对照表述</span>
                </div>
                <div style={{ marginTop: 30, padding: 14, background: "#FEE2E2", border: "1px solid #FECACA", borderRadius: 4, fontSize: 11, color: "#991B1B", textAlign: "center" }}>
                  内部审核稿 v{version[1]} · 未盖章 · 仅供 QC/QA/SD 审核使用
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Right: review panel */}
        <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
          {/* Stages */}
          <div className="card" style={{ padding: 16 }}>
            <h3 style={{ margin: "0 0 14px", fontSize: 14, fontWeight: 600 }}>审核流程</h3>
            <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
              {stages.map((s, i) => (
                <div key={s.key} style={{
                  padding: "10px 12px", borderRadius: 6,
                  background: s.current ? "var(--primary-50)" : s.done ? "var(--bg)" : s.lock ? "var(--danger-50)" : "#fff",
                  border: "1px solid " + (s.current ? "var(--primary)" : s.done ? "var(--line)" : s.lock ? "#FECACA" : "var(--line)"),
                }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                    <div style={{
                      width: 22, height: 22, borderRadius: "50%",
                      background: s.done ? "var(--primary)" : s.current ? "var(--accent)" : s.lock ? "var(--danger)" : "#fff",
                      border: !s.done && !s.current && !s.lock ? "2px solid var(--line)" : "none",
                      color: "#fff", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 11, flexShrink: 0,
                    }}>{s.lock ? "🔒" : s.done ? "✓" : s.current ? "●" : i + 1}</div>
                    <div style={{ flex: 1 }}>
                      <div style={{ fontSize: 13, fontWeight: s.current ? 600 : 500 }}>{s.l}</div>
                      <div style={{ fontSize: 11, color: "var(--ink-600)" }}>
                        {s.who ? <>{s.who.name} · {s.who.role}</> : "—"} · <span className="mono">{s.t}</span>
                      </div>
                    </div>
                    {s.current && <Tag tone="blue" dot>当前</Tag>}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Field mapping */}
          <div className="card" style={{ padding: 16 }}>
            <h3 style={{ margin: "0 0 12px", fontSize: 14, fontWeight: 600 }}>原始记录 → 报告字段映射</h3>
            <div style={{ fontSize: 12 }}>
              {[
                ["RR-2026-00001 · 样品批号", "报告 · 1.样品信息.批号", "ok"],
                ["RR-2026-00001 · 培养温度 (3#)", "报告 · 2.结果.3# 温度", "warn"],
                ["RR-2026-00001 · 阳性对照菌株", "报告 · 3.结论 (引用)", "ok"],
                ["RR-2026-00001 · 阴性对照", "报告 · 2.结果.最后一行", "ok"],
              ].map(([s, t, tone]) => (
                <div key={s} style={{ display: "flex", alignItems: "center", gap: 8, padding: "8px 0", borderBottom: "1px dashed var(--line)" }}>
                  <span style={{ flex: 1, color: "var(--ink-600)" }}>{s}</span>
                  <span style={{ color: "var(--ink-400)" }}>→</span>
                  <span style={{ flex: 1, color: "var(--ink-900)" }}>{t}</span>
                  <span style={{ width: 8, height: 8, borderRadius: "50%", background: tone === "ok" ? "var(--success)" : "var(--warning)" }} />
                </div>
              ))}
            </div>
          </div>

          {/* Comments */}
          <div className="card" style={{ padding: 16 }}>
            <h3 style={{ margin: "0 0 12px", fontSize: 14, fontWeight: 600 }}>历史批注 <span style={{ fontSize: 11, fontWeight: 400, color: "var(--ink-400)" }}>QC 2 · QA 3</span></h3>
            <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
              {[
                { who: PEOPLE.qc, t: "05-13 14:08", note: "原始记录 RR-2026-00001 第 3 页第 7 行培养温度记为 30°C，建议改为 30~35°C 区间", tone: "blue", resolved: true },
                { who: PEOPLE.qa, t: "进行中", note: "建议在结论中明确『同时通过阴/阳性对照核对，方法有效』表述", tone: "amber", resolved: false },
                { who: PEOPLE.qa, t: "进行中", note: "样品批号建议与送检单 SUB-2026-0001 PART 4 复核一致性", tone: "amber", resolved: false },
              ].map((c, i) => (
                <div key={i} style={{ padding: 10, background: "var(--bg)", borderRadius: 6, fontSize: 12 }}>
                  <div style={{ display: "flex", gap: 8, alignItems: "center", marginBottom: 4 }}>
                    <Avatar name={c.who.av} color={c.who.color} size={20} />
                    <span style={{ fontWeight: 500, fontSize: 12 }}>{c.who.name}</span>
                    <Tag tone={c.tone} style={{ fontSize: 10 }}>{c.who.role}</Tag>
                    <span className="mono" style={{ fontSize: 10, color: "var(--ink-400)", marginLeft: "auto" }}>{c.t}</span>
                  </div>
                  <div style={{ fontSize: 12, lineHeight: 1.6, color: "var(--ink-700)" }}>{c.note}</div>
                  <div style={{ marginTop: 6 }}>{c.resolved ? <Tag tone="green">✓ 已处理</Tag> : <Tag tone="amber">待处理</Tag>}</div>
                </div>
              ))}
            </div>
          </div>

          {/* Payment gate status */}
          <div className="card" style={{ padding: 16 }}>
            <h3 style={{ margin: "0 0 12px", fontSize: 14, fontWeight: 600 }}>付款门禁</h3>
            <div style={{ padding: 12, background: "var(--danger-50)", border: "1px solid #FECACA", borderRadius: 6, display: "flex", alignItems: "center", gap: 10 }}>
              <div style={{ fontSize: 22 }}>🔒</div>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 13, fontWeight: 600, color: "var(--danger)" }}>付款未到账</div>
                <div style={{ fontSize: 11, color: "var(--ink-600)" }}>应收 <span className="mono">{money(36800)}</span> · 报告通过 SD 批准后将转入此状态</div>
              </div>
            </div>
            <button className="btn btn-default btn-block" style={{ marginTop: 8 }} onClick={() => toast("已发起绿色通道申请，待 QA + 财务双签", "warning")}>申请绿色通道（先发后付）</button>
          </div>

          {/* Decision */}
          <div className="card" style={{ padding: 16 }}>
            <h3 style={{ margin: "0 0 12px", fontSize: 14, fontWeight: 600 }}>QA 审核操作</h3>
            <textarea className="f-textarea" rows="3" placeholder="填写审核意见..." value={comment} onChange={(e) => setComment(e.target.value)} />
            <div style={{ display: "flex", gap: 10, marginTop: 10 }}>
              <button className="btn btn-success" style={{ flex: 1 }} onClick={() => { setDecision("pass"); toast("已通过 · 流转至 SD 批准", "success"); }}>✓ 通过</button>
              <button className="btn btn-danger" style={{ flex: 1 }} onClick={() => { setDecision("reject"); toast("已驳回 · 退回主检", "danger"); }}>✕ 驳回</button>
            </div>
            <div style={{ marginTop: 8, fontSize: 11, color: "var(--ink-400)" }}>下一步：{PEOPLE.sd.name} (SD 终审)</div>
          </div>
        </div>
      </div>
    </DesktopShell>
  );
}

/* ============ P10: Payment Receipts ============ */
function P10_Payment({ onNavigate }) {
  const toast = useToast();
  const [selProject, setSelProject] = useState("P-2026-0001");
  const [amount, setAmount] = useState(36800);
  const [method, setMethod] = useState("bank");
  const [confirmOpen, setConfirmOpen] = useState(false);

  return (
    <DesktopShell side={<InternalSidebar active="payment" />} top={<TopBar crumbs={["资源", "财务收款登记"]} user={PEOPLE.finance} />}>
      <div style={{ padding: 24 }}>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(4,1fr)", gap: 14, marginBottom: 18 }}>
          <Stat label="本月已到账" value={money(1286400)} accent="success" delta="共 32 笔 · 较上月 +18%" deltaTone="up" />
          <Stat label="待到账金额" value={money(486200)} accent="warning" delta="14 笔 · 最长账期 32 天" />
          <Stat label="绿色通道笔数" value="3" suffix="笔" accent="accent" delta="QA + 财务双签放行" />
          <Stat label="异常 / 冲销" value="¥ 12,800" accent="danger" delta="本月 1 笔 · 重复收款" deltaTone="down" />
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
          {/* Pending list */}
          <div className="card" style={{ padding: 0 }}>
            <div style={{ padding: "14px 18px", borderBottom: "1px solid var(--line)", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <h3 style={{ margin: 0, fontSize: 14, fontWeight: 600 }}>未结款项目 · 14 项</h3>
              <div style={{ display: "flex", gap: 6 }}>
                <input className="f-input" placeholder="🔍 项目编号 / 客户" style={{ width: 200, padding: "5px 10px", fontSize: 12 }} />
              </div>
            </div>
            <table style={{ width: "100%", fontSize: 12, borderCollapse: "collapse" }}>
              <thead>
                <tr style={{ background: "var(--bg)", color: "var(--ink-600)" }}>
                  <th style={{ padding: 10, textAlign: "left" }}>项目</th>
                  <th style={{ padding: 10, textAlign: "left" }}>客户</th>
                  <th style={{ padding: 10, textAlign: "right" }}>应收</th>
                  <th style={{ padding: 10, textAlign: "left" }}>到期</th>
                  <th style={{ padding: 10, textAlign: "left" }}>状态</th>
                </tr>
              </thead>
              <tbody>
                {[
                  ["P-2026-0001", "无锡市南京大学锡山所", 36800, "2026-05-22", "blue", "正常", true],
                  ["P-2026-0007", "某创新医疗", 128500, "2026-05-15", "amber", "临期", false],
                  ["P-2026-0012", "华东医械", 98000, "2026-05-10", "red", "超期 3d", false],
                  ["P-2025-0182", "锐康医疗", 28500, "2026-05-08", "red", "超期 5d", false],
                  ["P-2026-0019", "康源生物", 56300, "2026-05-25", "blue", "正常", false],
                  ["P-2026-0024", "晶辰生物", 42000, "2026-05-28", "blue", "正常", false],
                  ["P-2026-0031", "维新医疗", 34800, "2026-06-02", "blue", "正常", false],
                ].map(([id, c, amt, due, tone, st, sel]) => (
                  <tr key={id} onClick={() => { setSelProject(id); setAmount(amt); }} style={{ borderBottom: "1px solid var(--line-soft)", cursor: "pointer", background: selProject === id ? "var(--primary-50)" : "#fff" }}>
                    <td style={{ padding: 10 }} className="mono">{id}</td>
                    <td style={{ padding: 10 }}>{c}</td>
                    <td style={{ padding: 10, textAlign: "right" }} className="mono">{money(amt)}</td>
                    <td style={{ padding: 10 }} className="mono">{due}</td>
                    <td style={{ padding: 10 }}><Tag tone={tone}>{st}</Tag></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Form */}
          <div className="card" style={{ padding: 20 }}>
            <h3 style={{ margin: "0 0 16px", fontSize: 14, fontWeight: 600 }}>登记到账</h3>
            <Field label="项目" en="Project" required>
              <select className="f-select mono" value={selProject} onChange={(e) => setSelProject(e.target.value)}>
                <option>P-2026-0001 · 无锡市南京大学锡山所 · {money(36800)}</option>
                <option>P-2026-0007 · 某创新医疗 · {money(128500)}</option>
              </select>
            </Field>
            <Field label="到账金额" en="Amount Received" required>
              <div style={{ display: "flex", alignItems: "center", gap: 12, padding: "14px 16px", background: "var(--primary-50)", border: "1px solid #CFE0F4", borderRadius: 8 }}>
                <span style={{ fontSize: 22, color: "var(--primary)", fontWeight: 600 }}>¥</span>
                <input className="mono" type="number" value={amount} onChange={(e) => setAmount(Number(e.target.value))} style={{ flex: 1, border: "none", background: "transparent", fontSize: 28, fontWeight: 600, outline: "none", color: "var(--ink-900)", fontFamily: "JetBrains Mono,monospace" }} />
                <span style={{ fontSize: 12, color: "var(--ink-600)" }}>大写：叁万陆仟捌佰元整</span>
              </div>
            </Field>
            <Field label="收款方式" en="Payment Method" required>
              <CardRadio value={method} onChange={setMethod} options={[
                { value: "bank", label: "银行转账", en: "Bank Transfer", icon: "▤" },
                { value: "cash", label: "现金", en: "Cash", icon: "¥" },
                { value: "other", label: "其他", en: "Other", icon: "···" },
              ]} />
            </Field>
            <div className="f-row f-row-2">
              <Field label="到账日期" en="Receipt Date" required>
                <input className="f-input mono" defaultValue="2026-05-13" />
              </Field>
              <Field label="付款方账户" en="Payer Account">
                <input className="f-input mono" placeholder="例：6228..." />
              </Field>
            </div>
            <Field label="凭证文件" en="Voucher">
              <div style={{ padding: 14, border: "2px dashed var(--line)", borderRadius: 8, textAlign: "center", color: "var(--ink-400)", fontSize: 12, cursor: "pointer" }}>
                ⤴ 点击上传或拖拽（银行回单 PDF / 转账截图）<br />
                <span style={{ fontSize: 11 }}>支持 PDF / JPG / PNG · 单文件 ≤ 10 MB</span>
              </div>
            </Field>
            <Field label="备注">
              <textarea className="f-textarea" rows="2" placeholder="可填写银行流水号、付款用途等..." />
            </Field>
            <button className="btn btn-primary btn-lg btn-block" style={{ marginTop: 12 }} onClick={() => setConfirmOpen(true)}>
              确认到账 · 解锁报告下载
            </button>
            <div style={{ marginTop: 10, padding: 10, background: "var(--bg)", borderRadius: 6, fontSize: 11, color: "var(--ink-600)", lineHeight: 1.7 }}>
              提交后将触发：① 项目状态机 PAYMENT_HOLD → RELEASED；② 给客户发送电子报告下载链接（带验签）；③ 触发纸质件邮寄打印任务。
            </div>
          </div>
        </div>
      </div>

      <Modal open={confirmOpen} onClose={() => setConfirmOpen(false)} title="确认到账并解锁报告"
        footer={<>
          <button className="btn btn-default" onClick={() => setConfirmOpen(false)}>再核对一下</button>
          <button className="btn btn-success" onClick={() => { setConfirmOpen(false); toast("✓ 到账已登记 · 报告已解锁，客户已收到下载邮件", "success"); }}>确认入账</button>
        </>}>
        <div style={{ fontSize: 13, lineHeight: 1.8 }}>
          <div style={{ padding: 14, background: "var(--success-50)", border: "1px solid #A7F3D0", borderRadius: 6, marginBottom: 14 }}>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
              <span style={{ color: "var(--ink-600)" }}>项目</span><span className="mono" style={{ fontWeight: 600 }}>{selProject}</span>
            </div>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
              <span style={{ color: "var(--ink-600)" }}>到账金额</span><span className="mono" style={{ fontWeight: 600, fontSize: 18, color: "var(--success)" }}>{money(amount)}</span>
            </div>
            <div style={{ display: "flex", justifyContent: "space-between" }}>
              <span style={{ color: "var(--ink-600)" }}>方式</span><span>银行转账</span>
            </div>
          </div>
          <div style={{ color: "var(--ink-600)", marginBottom: 8 }}>本次将解锁以下报告：</div>
          <div style={{ padding: 10, border: "1px solid var(--line)", borderRadius: 6, fontSize: 12 }}>
            <div className="mono" style={{ fontWeight: 600 }}>TR260408-03 · 无菌检测</div>
            <div style={{ color: "var(--ink-600)" }}>{SAMPLE.zh} · 已通过 QC/QA/SD 三级审核</div>
          </div>
        </div>
      </Modal>
    </DesktopShell>
  );
}

/* ============ P11: Device Calendar ============ */
function P11_DeviceCalendar() {
  const toast = useToast();
  const [device, setDevice] = useState("HPLC-001");
  const [view, setView] = useState("week");
  const [bookOpen, setBookOpen] = useState(false);
  const [overrideOpen, setOverrideOpen] = useState(false);
  const devices = ["HPLC-001", "GC-MS-002", "HX-150 培养箱", "SW-CJ-2D 超净台", "ICP-MS-001", "电子天平 BS-224"];
  const days = ["周一", "周二", "周三", "周四", "周五", "周六", "周日"];
  const dates = ["05-13", "05-14", "05-15", "05-16", "05-17", "05-18", "05-19"];
  const hours = Array.from({ length: 10 }, (_, i) => i + 9); // 9:00-18:00

  // status grid: hour x day, value: 'free' | 'mine' | 'ext' | 'maint'
  const grid = useMemo(() => {
    const m = {};
    [
      [0, 9, "mine", "P-2026-0007 浸提液浓度"],
      [0, 9.5, "mine", "P-2026-0007"],
      [0, 14, "ext", "外部客户 XX 制药"],
      [0, 14.5, "ext", "外部客户"],
      [1, 10, "mine", "P-2026-0012"],
      [1, 10.5, "mine", "P-2026-0012"],
      [1, 13, "ext", "外部客户"],
      [2, 9, "maint", "周度维护"],
      [2, 9.5, "maint", "周度维护"],
      [3, 11, "mine", "P-2026-0019"],
      [3, 11.5, "mine", "P-2026-0019"],
      [3, 14, "mine", "P-2026-0024"],
      [3, 14.5, "mine", "P-2026-0024"],
      [4, 9, "ext", "外部"],
      [4, 15, "mine", "P-2026-0028"],
    ].forEach(([d, h, st, info]) => { m[`${d}-${h}`] = { st, info }; });
    return m;
  }, [device]);

  return (
    <DesktopShell side={<InternalSidebar active="device-calendar" />} top={<TopBar crumbs={["资源", "设备预约日历"]} user={PEOPLE.test2} />}>
      <div style={{ padding: 24 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16, flexWrap: "wrap", gap: 12 }}>
          <div>
            <h1 style={{ margin: 0, fontSize: 20, fontWeight: 600 }}>设备预约日历</h1>
            <div style={{ fontSize: 12, color: "var(--ink-600)", marginTop: 4 }}>30 分钟粒度 · 内部 LIMS 预约优先于外部 · 覆盖外部需 QA 审批</div>
          </div>
          <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
            <div style={{ display: "flex", gap: 0, background: "#fff", border: "1px solid var(--line)", borderRadius: 6, padding: 2 }}>
              {["week", "month"].map((v) => (
                <span key={v} onClick={() => setView(v)} style={{ padding: "5px 14px", fontSize: 12, cursor: "pointer", borderRadius: 4, background: view === v ? "var(--primary)" : "transparent", color: view === v ? "#fff" : "var(--ink-600)" }}>{v === "week" ? "周视图" : "月视图"}</span>
              ))}
            </div>
            <button className="btn btn-default">← 上周</button>
            <span style={{ fontSize: 13, color: "var(--ink-900)", fontWeight: 600 }} className="mono">2026-05-13 ~ 05-19</span>
            <button className="btn btn-default">下周 →</button>
          </div>
        </div>

        {/* Device tabs */}
        <div style={{ display: "flex", gap: 4, borderBottom: "1px solid var(--line)", marginBottom: 16 }}>
          {devices.map((d) => (
            <div key={d} onClick={() => setDevice(d)} style={{
              padding: "9px 16px", fontSize: 13, cursor: "pointer",
              color: device === d ? "var(--primary)" : "var(--ink-700)",
              fontWeight: device === d ? 600 : 500,
              borderBottom: "2px solid " + (device === d ? "var(--primary)" : "transparent"),
              marginBottom: -1,
            }}>{d}</div>
          ))}
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "1fr 280px", gap: 16 }}>
          {/* Grid */}
          <div className="card" style={{ padding: 16 }}>
            <div style={{ overflow: "auto" }}>
              <div style={{ display: "grid", gridTemplateColumns: `60px repeat(7, minmax(110px,1fr))`, gap: 1, background: "var(--line)" }}>
                <div style={{ background: "#fff", padding: "8px 4px", fontSize: 11, color: "var(--ink-400)" }}></div>
                {days.map((d, i) => (
                  <div key={d} style={{ background: "#fff", padding: "8px 6px", textAlign: "center" }}>
                    <div style={{ fontSize: 11, color: "var(--ink-400)" }}>{d}</div>
                    <div style={{ fontSize: 13, fontWeight: 600, fontFamily: "Inter,sans-serif" }}>{dates[i]}</div>
                  </div>
                ))}
                {hours.map((h) => (
                  <Fragment key={h}>
                    {[0, 0.5].map((half) => (
                      <Fragment key={half}>
                        {half === 0 ? (
                          <div style={{ background: "#fff", padding: "4px", fontSize: 10, color: "var(--ink-400)", fontFamily: "Inter,sans-serif", display: "flex", alignItems: "flex-start", justifyContent: "flex-end", borderTop: half === 0 ? "1px solid var(--line)" : undefined }}>{h}:00</div>
                        ) : (
                          <div style={{ background: "#fff", padding: "4px" }}></div>
                        )}
                        {days.map((_, di) => {
                          const key = `${di}-${h + half}`;
                          const cell = grid[key];
                          const colors = { mine: ["#EEF4FB", "#1A5FB4"], ext: ["#F1F5F9", "#475569"], maint: ["#FEE2E2", "#991B1B"] };
                          return (
                            <div key={di + "-" + half} style={{ background: cell ? colors[cell.st][0] : "#fff", padding: "4px 6px", fontSize: 10, cursor: "pointer", borderTop: half === 0 ? "1px solid var(--line)" : "1px dashed var(--line)", minHeight: 22, overflow: "hidden" }} onClick={() => cell ? (cell.st === "ext" ? setOverrideOpen(true) : null) : setBookOpen(true)}>
                              {cell && (
                                <div style={{ color: colors[cell.st][1], fontWeight: 500, lineHeight: 1.3 }}>
                                  {cell.st === "maint" ? "🛠 维护" : cell.st === "ext" ? "外部" : cell.info}
                                </div>
                              )}
                            </div>
                          );
                        })}
                      </Fragment>
                    ))}
                  </Fragment>
                ))}
              </div>
            </div>
            <div style={{ display: "flex", gap: 14, marginTop: 14, fontSize: 12, color: "var(--ink-600)" }}>
              <span style={{ display: "inline-flex", alignItems: "center", gap: 6 }}><span style={{ width: 12, height: 12, background: "#EEF4FB", border: "1px solid #1A5FB4" }}></span>已预约（内部）</span>
              <span style={{ display: "inline-flex", alignItems: "center", gap: 6 }}><span style={{ width: 12, height: 12, background: "#F1F5F9", border: "1px solid #475569" }}></span>外部预约</span>
              <span style={{ display: "inline-flex", alignItems: "center", gap: 6 }}><span style={{ width: 12, height: 12, background: "#FEE2E2", border: "1px solid #991B1B" }}></span>维护停机</span>
              <span style={{ display: "inline-flex", alignItems: "center", gap: 6 }}><span style={{ width: 12, height: 12, background: "#fff", border: "1px solid var(--line)" }}></span>空闲（点击预约）</span>
            </div>
          </div>

          {/* My bookings */}
          <div className="card" style={{ padding: 16 }}>
            <h3 style={{ margin: "0 0 14px", fontSize: 14, fontWeight: 600 }}>我的预约 · 本周 6 项</h3>
            <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
              {[
                ["P-2026-0007", "05-13 09:00 ~ 10:00", "浸提液浓度", "blue"],
                ["P-2026-0012", "05-14 10:00 ~ 11:00", "EO 残留 GC", "blue"],
                ["P-2026-0019", "05-16 11:00 ~ 12:00", "重金属 ICP", "blue"],
                ["P-2026-0024", "05-16 14:00 ~ 15:00", "pH 校准", "blue"],
                ["P-2026-0028", "05-17 15:00 ~ 16:00", "HPLC", "amber"],
                ["P-2026-0033", "05-19 09:00 ~ 10:00", "重测", "blue"],
              ].map(([id, t, ds, tone], i) => (
                <div key={i} style={{ padding: 10, border: "1px solid var(--line)", borderRadius: 6, background: "var(--bg-card)" }}>
                  <div style={{ display: "flex", justifyContent: "space-between" }}>
                    <span className="mono" style={{ fontSize: 11, fontWeight: 600 }}>{id}</span>
                    <Tag tone={tone}>{tone === "amber" ? "需复核" : "已确认"}</Tag>
                  </div>
                  <div className="mono" style={{ fontSize: 12, marginTop: 2, color: "var(--ink-700)" }}>{t}</div>
                  <div style={{ fontSize: 11, color: "var(--ink-600)", marginTop: 2 }}>{ds}</div>
                </div>
              ))}
            </div>
            <button className="btn btn-default btn-block" style={{ marginTop: 14 }} onClick={() => setBookOpen(true)}>＋ 新建预约</button>
          </div>
        </div>
      </div>

      <Modal open={bookOpen} onClose={() => setBookOpen(false)} title="新建设备预约"
        footer={<><button className="btn btn-default" onClick={() => setBookOpen(false)}>取消</button>
        <button className="btn btn-primary" onClick={() => { setBookOpen(false); toast("✓ 已预约 HPLC-001 · 05-15 13:00 ~ 14:00", "success"); }}>提交预约</button></>}>
        <Field label="设备" required><input className="f-input" defaultValue={device} readOnly /></Field>
        <div className="f-row f-row-2">
          <Field label="预约日期" required><input className="f-input mono" defaultValue="2026-05-15" /></Field>
          <Field label="时间段" required>
            <div style={{ display: "flex", gap: 8 }}>
              <select className="f-select mono" defaultValue="13:00"><option>13:00</option><option>13:30</option><option>14:00</option></select>
              <span style={{ alignSelf: "center" }}>~</span>
              <select className="f-select mono" defaultValue="14:00"><option>13:30</option><option>14:00</option><option>14:30</option></select>
            </div>
          </Field>
        </div>
        <Field label="预约人"><div style={{ display: "flex", gap: 8, alignItems: "center", padding: 8, background: "var(--bg)", borderRadius: 6 }}><Avatar name="李" color="#047857" size={24} />李工程师（自动带入）</div></Field>
        <Field label="工单号 / 项目" required>
          <select className="f-select mono"><option>WO-2026-00001 · P-2026-0001 · 妥能水胶体敷贴</option></select>
        </Field>
        <Field label="检测项目 / 用途"><input className="f-input" placeholder="例：阳离子检测 · 浸提液浓度" /></Field>
      </Modal>

      <Modal open={overrideOpen} onClose={() => setOverrideOpen(false)} title="申请覆盖外部预约"
        footer={<><button className="btn btn-default" onClick={() => setOverrideOpen(false)}>取消</button>
        <button className="btn btn-danger" onClick={() => { setOverrideOpen(false); toast("⚠ 覆盖申请已提交 QA 审批，外部客户将收到改期通知", "warning"); }}>提交覆盖</button></>}>
        <div style={{ padding: 12, background: "var(--warning-50)", border: "1px solid #FDE68A", borderRadius: 6, fontSize: 13, color: "#92400E", marginBottom: 16, lineHeight: 1.7 }}>
          该时段已被 <b>外部客户 (XX 制药)</b> 预约。覆盖将触发：① QA 审批；② 通知原预约方改期；③ 记录覆盖原因到审计日志。
        </div>
        <Field label="覆盖原因" required hint="必须真实充分，将进入审计日志">
          <textarea className="f-textarea" rows="3" placeholder="例：CNAS 认可项目优先级 P1，必须按计划完成..." />
        </Field>
        <Field label="替代时段建议" hint="将自动通知原预约方">
          <input className="f-input" placeholder="例：05-16 09:00 ~ 10:00" />
        </Field>
      </Modal>
    </DesktopShell>
  );
}

/* ============ P12: System Config ============ */
function P12_SystemConfig() {
  const toast = useToast();
  const [cat, setCat] = useState("threshold");
  const cats = [
    ["dict", "字典管理", "📚"],
    ["permission", "权限矩阵", "🛡"],
    ["role", "角色管理", "👤"],
    ["rule", "业务规则", "⚙"],
    ["threshold", "阈值参数", "📐"],
    ["template", "模板管理", "▤"],
  ];

  return (
    <DesktopShell side={<InternalSidebar active="system-config" />} top={<TopBar crumbs={["管理", "系统配置"]} user={{ name: "管理员", role: "Admin", av: "管", color: "#0E2E5A" }} />}>
      <div style={{ padding: 24, display: "grid", gridTemplateColumns: "200px 1fr", gap: 16 }}>
        <div className="card" style={{ padding: 8, height: "fit-content" }}>
          {cats.map(([k, l, ic]) => (
            <div key={k} onClick={() => setCat(k)} style={{
              padding: "10px 14px", fontSize: 13, cursor: "pointer", borderRadius: 6,
              background: cat === k ? "var(--primary-50)" : undefined,
              color: cat === k ? "var(--primary)" : "var(--ink-700)",
              fontWeight: cat === k ? 600 : 400,
              display: "flex", alignItems: "center", gap: 10, marginBottom: 2,
            }}>
              <span style={{ fontSize: 14 }}>{ic}</span>
              <span>{l}</span>
            </div>
          ))}
        </div>

        <div className="card" style={{ padding: 24 }}>
          {cat === "threshold" && <ThresholdConfig toast={toast} />}
          {cat === "rule" && <RuleConfig />}
          {cat === "dict" && <DictConfig />}
          {cat === "permission" && <PermissionConfig />}
          {cat === "role" && <RoleConfig />}
          {cat === "template" && <TemplateConfig />}
        </div>
      </div>
    </DesktopShell>
  );
}

function ThresholdConfig({ toast }) {
  const [edit, setEdit] = useState(null);
  const [savedAt, setSavedAt] = useState(null);
  const rows = [
    ["health.no_progress_days", "2", "INT", "健康度", "SD / Admin", "连续无进展天数阈值，触发 -15 分"],
    ["health.overdue_penalty", "20", "INT", "健康度", "SD / Admin", "超期扣分"],
    ["sla.report_review_hours", "48", "INT", "SLA", "QA Lead / Admin", "报告审核 SLA（小时）"],
    ["sla.payment_overdue_days", "30", "INT", "SLA", "财务 Lead / Admin", "付款逾期警戒线"],
    ["urgent.green_channel_max", "5", "INT", "绿色通道", "财务 Lead / Admin", "每月绿色通道最大笔数"],
    ["device.book_granularity_min", "30", "INT", "设备", "Admin", "预约最小粒度（分钟）"],
    ["report.draft_watermark", "DRAFT 草稿", "STRING", "报告", "Admin", "草稿水印文字"],
    ["health.refresh_interval_sec", "60", "INT", "实时刷新", "Admin", "工作中心数据刷新间隔"],
  ];
  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
        <div>
          <h2 style={{ margin: 0, fontSize: 18, fontWeight: 600 }}>阈值参数</h2>
          <div style={{ fontSize: 12, color: "var(--ink-600)", marginTop: 4 }}>修改后 <b style={{ color: "var(--primary)" }}>1 分钟内全网生效</b>，无需重启</div>
        </div>
        <div style={{ display: "flex", gap: 8 }}>
          <button className="btn btn-default">⬇ 导出 CSV</button>
          <button className="btn btn-default">⬆ 导入 CSV</button>
          <button className="btn btn-primary">＋ 新建配置项</button>
        </div>
      </div>

      {savedAt && (
        <div style={{ padding: 10, background: "var(--success-50)", border: "1px solid #A7F3D0", borderRadius: 6, fontSize: 12, color: "#047857", marginBottom: 12 }}>
          ✓ 已保存 · {savedAt} · 配置将在 <b>1 分钟内</b>全网生效（审计日志已记录）
        </div>
      )}

      <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13, border: "1px solid var(--line)", borderRadius: 8, overflow: "hidden" }}>
        <thead>
          <tr style={{ background: "var(--bg)" }}>
            <th style={{ padding: 10, textAlign: "left", borderBottom: "1px solid var(--line)" }}>键</th>
            <th style={{ padding: 10, textAlign: "left", borderBottom: "1px solid var(--line)", width: 120 }}>值</th>
            <th style={{ padding: 10, textAlign: "left", borderBottom: "1px solid var(--line)", width: 70 }}>类型</th>
            <th style={{ padding: 10, textAlign: "left", borderBottom: "1px solid var(--line)", width: 100 }}>分类</th>
            <th style={{ padding: 10, textAlign: "left", borderBottom: "1px solid var(--line)", width: 140 }}>可编辑角色</th>
            <th style={{ padding: 10, textAlign: "left", borderBottom: "1px solid var(--line)" }}>描述</th>
            <th style={{ padding: 10, textAlign: "left", borderBottom: "1px solid var(--line)", width: 100 }}>操作</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={r[0]} style={{ borderBottom: "1px solid var(--line-soft)" }}>
              <td style={{ padding: 10 }} className="mono">{r[0]}</td>
              <td style={{ padding: 10 }}>
                {edit === i ? <input className="f-input mono" defaultValue={r[1]} style={{ padding: "3px 8px", fontSize: 12 }} /> : <span className="mono" style={{ fontWeight: 600, color: "var(--primary)" }}>{r[1]}</span>}
              </td>
              <td style={{ padding: 10 }}><Tag tone="grey">{r[2]}</Tag></td>
              <td style={{ padding: 10, fontSize: 12 }}>{r[3]}</td>
              <td style={{ padding: 10, fontSize: 11, color: "var(--ink-600)" }}>{r[4]}</td>
              <td style={{ padding: 10, fontSize: 12, color: "var(--ink-600)" }}>{r[5]}</td>
              <td style={{ padding: 10 }}>
                {edit === i ? (
                  <span>
                    <span style={{ color: "var(--success)", cursor: "pointer", marginRight: 8 }} onClick={() => { setEdit(null); setSavedAt(dayjs().format("YYYY-MM-DD HH:mm:ss")); toast("配置已保存", "success"); }}>✓ 保存</span>
                    <span style={{ color: "var(--ink-400)", cursor: "pointer" }} onClick={() => setEdit(null)}>取消</span>
                  </span>
                ) : (
                  <span style={{ color: "var(--primary)", cursor: "pointer" }} onClick={() => setEdit(i)}>编辑</span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function RuleConfig() {
  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 16 }}>
        <div>
          <h2 style={{ margin: 0, fontSize: 18, fontWeight: 600 }}>业务规则</h2>
          <div style={{ fontSize: 12, color: "var(--ink-600)", marginTop: 4 }}>Aviator 表达式 · 触发器 · 启停</div>
        </div>
        <button className="btn btn-primary">＋ 新建规则</button>
      </div>
      {[
        ["RULE_HEALTH_NO_PROGRESS", "项目无进展扣分", "ON_DAILY_SCAN", `last_progress_days > config.health.no_progress_days && status != "ARCHIVED"`, true],
        ["RULE_PAYMENT_GATE", "付款门禁拦截发放", "ON_STATE_PAYMENT_HOLD", `paid_amount < contract_amount && green_channel != true`, true],
        ["RULE_URGENT_NOPAY", "加急未付款扣分", "ON_HOURLY_SCAN", `urgent_level == "P1" && paid_amount == 0`, true],
        ["RULE_QA_DOUBLE_SIGN_GLP", "GLP 项目 QA 双签", "ON_STATE_QA_REVIEWING", `submission.glp == true`, true],
        ["RULE_OVERRIDE_NOTIFY", "覆盖外部预约通知", "ON_DEVICE_BOOKING_OVERRIDE", `original_party.type == "EXTERNAL"`, false],
      ].map(([code, name, trig, expr, on]) => (
        <div key={code} className="card" style={{ padding: 14, marginBottom: 10 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
            <div>
              <span className="mono" style={{ fontSize: 12, fontWeight: 600, color: "var(--ink-700)" }}>{code}</span>
              <span style={{ marginLeft: 10, fontSize: 13, fontWeight: 500 }}>{name}</span>
            </div>
            <Toggle value={on} labels={["启用", "停用"]} />
          </div>
          <div style={{ fontSize: 11, color: "var(--ink-400)" }}>触发器 · <span className="mono" style={{ color: "var(--ink-600)" }}>{trig}</span></div>
          <div style={{ padding: 10, background: "#0F172A", color: "#7FE7E7", fontFamily: "JetBrains Mono,monospace", fontSize: 12, borderRadius: 4, marginTop: 6, overflow: "auto" }}>
            <span style={{ color: "#94A3B8" }}>// 表达式</span><br />
            {expr}
          </div>
        </div>
      ))}
    </div>
  );
}
function DictConfig() {
  return (
    <div>
      <h2 style={{ margin: "0 0 16px", fontSize: 18, fontWeight: 600 }}>字典管理</h2>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(3,1fr)", gap: 12 }}>
        {[
          ["STERILIZATION", "灭菌状态", 5, ["EO 灭菌", "Gamma 辐照", "蒸汽灭菌", "非灭菌", "未知"]],
          ["TEST_CATEGORY", "检测分类", 5, ["生物相容性", "微生物", "包装", "物理", "化学"]],
          ["DISPOSAL", "样品处置", 3, ["破坏丢弃", "回寄未使用", "其他"]],
          ["CONTACT_DURATION", "接触时长", 3, ["A · ≤24h", "B · ≤30d", "C · >30d"]],
          ["EXTRACT_BASIS", "浸提依据", 3, ["表面积", "重量", "N/A"]],
          ["PROJECT_TYPE", "项目类型", 3, ["STD_ACCREDITED", "STD_NON_ACCREDITED", "NON_STANDARD"]],
        ].map(([code, name, n, items]) => (
          <div key={code} className="card" style={{ padding: 14 }}>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 8 }}>
              <span style={{ fontSize: 13, fontWeight: 600 }}>{name}</span>
              <Tag tone="grey">{n} 项</Tag>
            </div>
            <div className="mono" style={{ fontSize: 10, color: "var(--ink-400)", marginBottom: 8 }}>{code}</div>
            <div style={{ display: "flex", flexWrap: "wrap", gap: 4 }}>
              {items.map((it) => <Tag key={it} tone="blue">{it}</Tag>)}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
function PermissionConfig() {
  const cols = ["业务员", "技术 SD", "QC", "QA", "财务", "Admin"];
  const rows = [
    ["创建送检单", "✓", "—", "—", "—", "—", "✓"],
    ["确认试验计划", "—", "✓", "—", "—", "—", "✓"],
    ["复核原始记录", "—", "—", "✓", "—", "—", "✓"],
    ["报告 QA 审核", "—", "—", "—", "✓", "—", "✓"],
    ["报告 SD 批准", "—", "✓", "—", "—", "—", "✓"],
    ["登记到账", "—", "—", "—", "—", "✓", "✓"],
    ["绿色通道审批", "—", "—", "—", "✓", "✓", "✓"],
    ["编辑配置项", "—", "—", "—", "—", "—", "✓"],
  ];
  return (
    <div>
      <h2 style={{ margin: "0 0 16px", fontSize: 18, fontWeight: 600 }}>权限矩阵</h2>
      <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 12 }}>
        <thead><tr style={{ background: "var(--bg)" }}>
          <th style={{ padding: 10, textAlign: "left", border: "1px solid var(--line)" }}>权限项</th>
          {cols.map((c) => <th key={c} style={{ padding: 10, textAlign: "center", border: "1px solid var(--line)" }}>{c}</th>)}
        </tr></thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r[0]}>
              <td style={{ padding: 8, border: "1px solid var(--line)", fontWeight: 500 }}>{r[0]}</td>
              {r.slice(1).map((v, i) => (
                <td key={i} style={{ padding: 8, textAlign: "center", border: "1px solid var(--line)", color: v === "✓" ? "var(--success)" : "var(--ink-300)", fontWeight: v === "✓" ? 600 : 400, fontSize: 13 }}>{v}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
function RoleConfig() {
  return (
    <div>
      <h2 style={{ margin: "0 0 16px", fontSize: 18, fontWeight: 600 }}>角色管理</h2>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(3,1fr)", gap: 12 }}>
        {Object.values(PEOPLE).map((p) => (
          <div key={p.name} className="card" style={{ padding: 14, display: "flex", alignItems: "center", gap: 12 }}>
            <Avatar name={p.av} color={p.color} size={42} />
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 13, fontWeight: 600 }}>{p.name}</div>
              <div style={{ fontSize: 11, color: "var(--ink-600)" }}>{p.role}</div>
              <div style={{ fontSize: 10, color: "var(--ink-400)", marginTop: 4 }}>最近登录 2026-05-13 09:15</div>
            </div>
            <Tag tone="green">在岗</Tag>
          </div>
        ))}
      </div>
    </div>
  );
}
function TemplateConfig() {
  return (
    <div>
      <h2 style={{ margin: "0 0 16px", fontSize: 18, fontWeight: 600 }}>模板管理</h2>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(2,1fr)", gap: 12 }}>
        {[
          ["报告模板 · 无菌检查", "TPL_REPORT_STERILITY", "v3.2", "2026-04-15"],
          ["报告模板 · 细胞毒性", "TPL_REPORT_CYTOTOX", "v2.1", "2026-03-08"],
          ["送检单模板 Ver.15", "TPL_SUB_V15", "v15.0", "2026-04-01"],
          ["合同模板 · 标准检测", "TPL_CONTRACT_STD", "v4.0", "2026-02-20"],
          ["补充协议 · 非标方法", "TPL_AGREEMENT_NONSTD", "v2.0", "2026-01-15"],
          ["原始记录 · 微生物", "TPL_RR_MICRO", "v3.0", "2026-04-22"],
        ].map(([nm, code, v, t]) => (
          <div key={code} className="card" style={{ padding: 14 }}>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 6 }}>
              <span style={{ fontSize: 13, fontWeight: 600 }}>{nm}</span>
              <Tag tone="cyan">{v}</Tag>
            </div>
            <div className="mono" style={{ fontSize: 10, color: "var(--ink-400)" }}>{code}</div>
            <div style={{ fontSize: 11, color: "var(--ink-600)", marginTop: 6 }}>最近更新 <span className="mono">{t}</span></div>
            <div style={{ marginTop: 10, display: "flex", gap: 8 }}>
              <button className="btn btn-default" style={{ fontSize: 11, padding: "3px 10px" }}>预览</button>
              <button className="btn btn-ghost" style={{ fontSize: 11, padding: "3px 10px" }}>历史版本</button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

Object.assign(window, { P9_ReportReview, P10_Payment, P11_DeviceCalendar, P12_SystemConfig });
