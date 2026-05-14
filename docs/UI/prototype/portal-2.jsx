/* ====================================================================
   PORTAL · P2 Dashboard, P4 Project Detail, P5 Report Preview
   ==================================================================== */

/* ============ P2: Portal Dashboard ============ */
function P2_Dashboard({ onNavigate }) {
  const toast = useToast();
  return (
    <DesktopShell
      side={<PortalSidebar active="dashboard" />}
      top={<PortalTopBar />}
    >
      <div style={{ padding: 24 }}>
        {/* Welcome hero */}
        <div style={{
          background: "linear-gradient(120deg,#0E3A6E 0%,#1A5FB4 60%,#00A8A8 130%)",
          color: "#fff", borderRadius: 12, padding: "28px 32px", display: "flex",
          justifyContent: "space-between", alignItems: "center", marginBottom: 20,
          boxShadow: "0 8px 24px rgba(20,74,142,.18)",
        }}>
          <div>
            <div style={{ fontSize: 12, opacity: .75, letterSpacing: ".14em", fontFamily: "Inter,sans-serif", textTransform: "uppercase", marginBottom: 6 }}>2026-05-13 · 周三</div>
            <div style={{ fontSize: 24, fontWeight: 600, marginBottom: 4 }}>您好，李客户</div>
            <div style={{ fontSize: 13, opacity: .8 }}>当前服务：{ORG.zh}</div>
          </div>
          <div style={{ display: "flex", gap: 16 }}>
            {[
              ["进行中项目", "4", null],
              ["待付款报告", "2", "warning"],
              ["本月送检", "3", null],
              ["累计报告", "47", null],
            ].map(([l, v, tone]) => (
              <div key={l} style={{ minWidth: 110, padding: "14px 20px", background: "rgba(255,255,255,.10)", borderRadius: 10, border: "1px solid rgba(255,255,255,.18)", backdropFilter: "blur(6px)" }}>
                <div style={{ fontSize: 11, opacity: .75 }}>{l}</div>
                <div style={{ fontFamily: "Inter,sans-serif", fontWeight: 600, fontSize: 28, marginTop: 4, color: tone === "warning" ? "#FFD59E" : "#fff" }}>{v}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Two-column */}
        <div style={{ display: "grid", gridTemplateColumns: "1.5fr 1fr", gap: 18 }}>
          {/* Projects */}
          <div>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
              <h3 style={{ margin: 0, fontSize: 16, fontWeight: 600 }}>我的项目</h3>
              <div style={{ display: "flex", gap: 14, fontSize: 13 }}>
                <span style={{ color: "var(--primary)", fontWeight: 500, borderBottom: "2px solid var(--primary)", paddingBottom: 2 }}>全部 (4)</span>
                <span style={{ color: "var(--ink-600)", cursor: "pointer" }}>进行中 (3)</span>
                <span style={{ color: "var(--ink-600)", cursor: "pointer" }}>已完成 (1)</span>
              </div>
            </div>

            <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
              {CUSTOMER_PROJECTS.map((p) => (
                <div key={p.id} className="card" style={{ padding: 18, cursor: "pointer" }} onClick={() => onNavigate && onNavigate("P4")}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 10 }}>
                    <div>
                      <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 6 }}>
                        <span className="mono" style={{ fontSize: 12, color: "var(--ink-600)", fontWeight: 600 }}>{p.id}</span>
                        <Tag tone={p.statusTone} dot>{p.statusText}</Tag>
                        {!p.paid && p.stage === 4 && <Tag tone="red" dot>待付款</Tag>}
                      </div>
                      <div style={{ fontSize: 15, fontWeight: 600, color: "var(--ink-900)" }}>{p.sampleZh}</div>
                      <div style={{ fontSize: 12, color: "var(--ink-600)", fontFamily: "Inter,sans-serif", marginTop: 2 }}>{p.sampleEn}</div>
                    </div>
                    <span style={{ fontSize: 12, color: "var(--ink-400)" }}>查看详情 →</span>
                  </div>
                  <div style={{ fontSize: 12, color: "var(--ink-600)", marginBottom: 12 }}>
                    检测项目：<span style={{ color: "var(--ink-900)" }}>{p.item}</span>
                  </div>
                  <ProgressSteps stage={p.stage} />
                  <div style={{ display: "flex", justifyContent: "space-between", marginTop: 8, fontSize: 11, color: "var(--ink-600)" }}>
                    {["受理", "样品到达", "试验", "报告", "发放"].map((s, i) => (
                      <span key={s} style={{ color: i === p.stage ? "var(--accent)" : i < p.stage ? "var(--primary)" : "var(--ink-400)", fontWeight: i === p.stage ? 600 : 400 }}>{s}</span>
                    ))}
                  </div>
                  <div style={{ marginTop: 12, paddingTop: 12, borderTop: "1px dashed var(--line)", display: "flex", justifyContent: "space-between", fontSize: 12, color: "var(--ink-600)" }}>
                    <span>计划完成 · <span className="mono" style={{ color: "var(--ink-900)" }}>{p.plannedFinish}</span></span>
                    <span>业务员 · {PEOPLE.sales.name}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Right sidebar */}
          <div style={{ display: "flex", flexDirection: "column", gap: 18 }}>
            <div className="card" style={{ padding: 18 }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14 }}>
                <h3 style={{ margin: 0, fontSize: 15, fontWeight: 600 }}>我的待办</h3>
                <span style={{ fontSize: 12, color: "var(--ink-400)" }}>{TODOS.length} 项</span>
              </div>
              <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                {TODOS.map((t, i) => (
                  <div key={i} style={{ padding: 12, background: t.urgent ? "var(--danger-50)" : "var(--bg)", borderRadius: 8, border: "1px solid " + (t.urgent ? "#FECACA" : "var(--line)") }}>
                    <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
                      <Tag tone={t.urgent ? "red" : "blue"}>{t.kind}</Tag>
                      <span className="mono" style={{ fontSize: 11, color: "var(--ink-600)" }}>{t.project}</span>
                    </div>
                    <div style={{ fontSize: 12, color: "var(--ink-900)", marginTop: 6, lineHeight: 1.5 }}>{t.text}</div>
                    <div style={{ marginTop: 8, display: "flex", gap: 8 }}>
                      <button className="btn btn-primary" style={{ padding: "4px 14px", fontSize: 12 }} onClick={() => toast("已跳转到处理页", "success")}>立即处理</button>
                      <button className="btn btn-ghost" style={{ padding: "4px 10px", fontSize: 12 }}>稍后</button>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="card" style={{ padding: 18 }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14 }}>
                <h3 style={{ margin: 0, fontSize: 15, fontWeight: 600 }}>通知公告</h3>
                <span style={{ fontSize: 12, color: "var(--primary)", cursor: "pointer" }}>全部 →</span>
              </div>
              <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                {NOTICES.map((n, i) => (
                  <div key={i} style={{ display: "flex", gap: 10 }}>
                    <Tag tone={n.tone} style={{ alignSelf: "flex-start", flexShrink: 0 }}>{n.type}</Tag>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 13, color: "var(--ink-900)", lineHeight: 1.5 }}>{n.title}</div>
                      <div className="mono" style={{ fontSize: 11, color: "var(--ink-400)", marginTop: 2 }}>{n.t}</div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="card" style={{ padding: 18, background: "linear-gradient(135deg,#EEF4FB 0%,#E6F6F6 100%)", border: "1px solid #CFE0F4" }}>
              <div style={{ fontSize: 13, fontWeight: 600, color: "var(--primary)", marginBottom: 6 }}>专属服务热线</div>
              <div className="mono" style={{ fontSize: 18, fontWeight: 600, color: "var(--ink-900)" }}>{ORG.tel}</div>
              <div style={{ fontSize: 11, color: "var(--ink-600)", marginTop: 4 }}>工作日 09:00-18:00 · 7×24 应急 187-xxxx-1234</div>
            </div>
          </div>
        </div>
      </div>

      {/* Floating action button */}
      <div
        onClick={() => onNavigate && onNavigate("P3")}
        style={{
          position: "fixed", right: 32, bottom: 32, height: 56, padding: "0 28px", borderRadius: 28,
          background: "var(--primary)", color: "#fff", boxShadow: "0 10px 30px rgba(20,74,142,.35)",
          display: "flex", alignItems: "center", gap: 10, cursor: "pointer", fontWeight: 600,
          fontSize: 14, zIndex: 100,
        }}
      >
        <span style={{ fontSize: 18 }}>＋</span>
        新建送检单
      </div>
    </DesktopShell>
  );
}

/* ============ P4: Portal Project Detail ============ */
function P4_ProjectDetail({ onNavigate }) {
  const toast = useToast();
  const cur = 6; // IN_TESTING
  return (
    <DesktopShell side={<PortalSidebar active="projects" />} top={<PortalTopBar />}>
      <div style={{ padding: 24 }}>
        {/* breadcrumb + header */}
        <div style={{ fontSize: 12, color: "var(--ink-600)", marginBottom: 12 }}>
          我的项目 / <span style={{ color: "var(--ink-900)" }} className="mono">P-2026-0001</span>
        </div>
        <div className="card" style={{ padding: 20, marginBottom: 16 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 14 }}>
            <div>
              <div style={{ display: "flex", gap: 12, alignItems: "center", marginBottom: 8 }}>
                <span className="mono" style={{ fontSize: 14, color: "var(--ink-600)", fontWeight: 600 }}>P-2026-0001</span>
                <Tag tone="blue" dot>试验中</Tag>
                <Tag tone="green">标准方法 · CNAS 认可</Tag>
                <Tag tone="cyan">加急 · 优先级 P1</Tag>
              </div>
              <div style={{ fontSize: 20, fontWeight: 600, color: "var(--ink-900)" }}>{SAMPLE.zh}</div>
              <div style={{ fontSize: 13, color: "var(--ink-600)", fontFamily: "Inter,sans-serif", marginTop: 2 }}>{SAMPLE.en}</div>
            </div>
            <div style={{ display: "flex", gap: 8 }}>
              <button className="btn btn-default" onClick={() => toast("已发送联系业务申请", "success")}>联系业务</button>
              <button className="btn btn-default">下载送检单</button>
              <button className="btn btn-primary">补充协议签署</button>
            </div>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "repeat(4,1fr)", gap: 24, padding: "16px 0", borderTop: "1px solid var(--line)" }}>
            <div>
              <div style={{ fontSize: 11, color: "var(--ink-400)", marginBottom: 4 }}>预计完成</div>
              <div className="mono" style={{ fontSize: 16, fontWeight: 600 }}>2026-05-22</div>
              <div style={{ fontSize: 11, color: "var(--success)", marginTop: 2 }}>计划内 · 提前 1 天</div>
            </div>
            <div>
              <div style={{ fontSize: 11, color: "var(--ink-400)", marginBottom: 4 }}>合同金额</div>
              <div className="mono" style={{ fontSize: 16, fontWeight: 600 }}>{money(36800)}</div>
              <div style={{ fontSize: 11, color: "var(--ink-600)", marginTop: 2 }}>已收 ¥ 0 · 应收 ¥ 36,800</div>
            </div>
            <div>
              <div style={{ fontSize: 11, color: "var(--ink-400)", marginBottom: 4 }}>检测人员</div>
              <div style={{ display: "flex", gap: 4, marginTop: 2 }}>
                <Avatar name="张" color="#1A5FB4" size={28} ring="#fff" />
                <Avatar name="李" color="#047857" size={28} ring="#fff" />
                <Avatar name="赵" color="#5B21B6" size={28} ring="#fff" />
              </div>
              <div style={{ fontSize: 11, color: "var(--ink-600)", marginTop: 4 }}>3 人协作</div>
            </div>
            <div>
              <div style={{ fontSize: 11, color: "var(--ink-400)", marginBottom: 4 }}>当前阶段</div>
              <div style={{ fontSize: 16, fontWeight: 600, color: "var(--accent)" }}>试验执行中</div>
              <div style={{ fontSize: 11, color: "var(--ink-600)", marginTop: 2 }}>预计 3 个工作日完成</div>
            </div>
          </div>
        </div>

        {/* 3 cols */}
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1.4fr 1fr", gap: 16 }}>
          {/* Timeline */}
          <div className="card" style={{ padding: 18 }}>
            <h3 style={{ margin: "0 0 16px", fontSize: 14, fontWeight: 600 }}>项目进度时间线</h3>
            <div style={{ position: "relative", paddingLeft: 28 }}>
              <div style={{ position: "absolute", left: 11, top: 8, bottom: 8, width: 2, background: "var(--line)" }} />
              {STATE_MACHINE.map((s, i) => {
                const done = i < cur, isCur = i === cur;
                return (
                  <div key={s[0]} style={{ position: "relative", paddingBottom: 14 }}>
                    <div style={{
                      position: "absolute", left: -22, top: 2, width: 18, height: 18, borderRadius: "50%",
                      background: done ? "var(--primary)" : isCur ? "var(--accent)" : "#fff",
                      border: done ? "none" : isCur ? "none" : "2px solid var(--line)",
                      display: "flex", alignItems: "center", justifyContent: "center",
                      color: "#fff", fontSize: 9, fontWeight: 600,
                      boxShadow: isCur ? "0 0 0 4px rgba(0,168,168,.18)" : undefined,
                      animation: isCur ? "pulse 2s infinite" : undefined,
                    }}>{done ? "✓" : ""}</div>
                    <div style={{ fontSize: 13, fontWeight: isCur ? 600 : 500, color: done || isCur ? "var(--ink-900)" : "var(--ink-400)" }}>{s[1]}</div>
                    {(done || isCur) && (
                      <div className="mono" style={{ fontSize: 10, color: "var(--ink-400)", marginTop: 2 }}>
                        {i < cur ? `2026-0${4 + Math.floor(i / 3)}-${10 + i * 2}` : "进行中 · 2026-05-13"}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
            <style>{`@keyframes pulse{0%,100%{box-shadow:0 0 0 4px rgba(0,168,168,.18)}50%{box-shadow:0 0 0 7px rgba(0,168,168,.10)}}`}</style>
          </div>

          {/* Sample info */}
          <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
            <div className="card" style={{ padding: 18 }}>
              <h3 style={{ margin: "0 0 14px", fontSize: 14, fontWeight: 600 }}>样品信息</h3>
              <div style={{ display: "flex", gap: 14 }}>
                <div style={{ width: 100, height: 100, borderRadius: 8, background: "linear-gradient(135deg,#F1F5F9,#E2E8F0)", display: "flex", alignItems: "center", justifyContent: "center", color: "var(--ink-400)", fontSize: 11, flexShrink: 0, position: "relative", overflow: "hidden" }}>
                  <div style={{ position: "absolute", inset: 0, background: "repeating-linear-gradient(45deg,#E2E8F0,#E2E8F0 2px,#F1F5F9 2px,#F1F5F9 6px)" }}/>
                  <span style={{ position: "relative", fontFamily: "JetBrains Mono,monospace" }}>SAMPLE PHOTO</span>
                </div>
                <div style={{ flex: 1, fontSize: 12, lineHeight: 1.9 }}>
                  <div><span style={{ color: "var(--ink-600)" }}>样品名：</span><span style={{ fontWeight: 500 }}>{SAMPLE.zh}</span></div>
                  <div className="mono" style={{ color: "var(--ink-600)", fontSize: 11 }}>{SAMPLE.en}</div>
                  <div><span style={{ color: "var(--ink-600)" }}>型号 / 规格：</span><span className="mono">{SAMPLE.model}</span></div>
                  <div><span style={{ color: "var(--ink-600)" }}>批号：</span><span className="mono">{SAMPLE.lot}</span> <Tag tone="grey" style={{ fontSize: 10, padding: "0 4px" }}>🔒 已锁定</Tag></div>
                  <div><span style={{ color: "var(--ink-600)" }}>送检数量：</span>12 片 · 已入库</div>
                </div>
              </div>
            </div>

            <div className="card" style={{ padding: 18 }}>
              <h3 style={{ margin: "0 0 14px", fontSize: 14, fontWeight: 600 }}>检测项目</h3>
              <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                <div style={{ padding: 12, background: "var(--primary-50)", borderRadius: 6, border: "1px solid #CFE0F4" }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <div>
                      <Tag tone="green" style={{ marginRight: 8 }}>CNAS 认可</Tag>
                      <span style={{ fontWeight: 500, fontSize: 13 }}>无菌检测</span>
                      <span className="mono" style={{ marginLeft: 8, fontSize: 11, color: "var(--ink-600)" }}>MICRO_STERILITY</span>
                    </div>
                    <Tag tone="blue" dot>进行中</Tag>
                  </div>
                  <div style={{ fontSize: 12, color: "var(--ink-600)", marginTop: 6 }}>
                    依据：{PROJECT.standard}<br />
                    方法：{PROJECT.method}
                  </div>
                </div>
              </div>
            </div>

            <div className="card" style={{ padding: 18 }}>
              <h3 style={{ margin: "0 0 14px", fontSize: 14, fontWeight: 600 }}>合同与文件</h3>
              <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                {[
                  ["主合同_P-2026-0001.pdf", "2026-04-08 签订", "PDF", 280],
                  ["送检单_SUB-2026-0001.pdf", "2026-04-08 提交", "PDF", 156],
                  ["非标方法评审协议.pdf", "2026-04-10 双方签字", "PDF", 89],
                  ["原始记录摘要_v2.pdf", "草稿 · 仅可预览", "PDF", 32, true],
                ].map(([nm, t, type, kb, locked], i) => (
                  <div key={i} style={{ display: "flex", alignItems: "center", gap: 12, padding: "8px 12px", border: "1px solid var(--line)", borderRadius: 6 }}>
                    <div style={{ width: 32, height: 38, background: "#FEE2E2", color: "#991B1B", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 10, fontWeight: 700, borderRadius: 3, fontFamily: "Inter,sans-serif" }}>{type}</div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 12, color: "var(--ink-900)", fontWeight: 500, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{nm}</div>
                      <div style={{ fontSize: 11, color: "var(--ink-400)" }}>{t} · {kb} KB</div>
                    </div>
                    {locked ? <span style={{ color: "var(--ink-400)", fontSize: 12 }}>🔒 仅预览</span> : <span style={{ color: "var(--primary)", fontSize: 12, cursor: "pointer" }}>下载</span>}
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Right: report area */}
          <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
            <div className="card" style={{ padding: 18 }}>
              <h3 style={{ margin: "0 0 12px", fontSize: 14, fontWeight: 600 }}>报告</h3>
              <div style={{ padding: 14, background: "var(--bg)", borderRadius: 6, marginBottom: 12 }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 6 }}>
                  <span className="mono" style={{ fontSize: 12, fontWeight: 600 }}>TR260408-03</span>
                  <Tag tone="amber" dot>草稿 v1</Tag>
                </div>
                <div style={{ fontSize: 12, color: "var(--ink-600)" }}>报告草稿已就绪，可预览查看检测结果与结论</div>
              </div>
              <button className="btn btn-primary btn-block" onClick={() => onNavigate && onNavigate("P5")}>预览报告草稿</button>
              <div style={{ marginTop: 12, padding: 12, background: "#FEE2E2", border: "1px solid #FECACA", borderRadius: 6, fontSize: 12, color: "#991B1B", lineHeight: 1.6 }}>
                <div style={{ fontWeight: 600, marginBottom: 4 }}>🔒 正式版本已锁定</div>
                付款到账后自动解锁电子版下载、纸质件邮寄、报告验证查询。
                <div style={{ marginTop: 8, paddingTop: 8, borderTop: "1px solid #FECACA" }}>
                  应付：<b className="mono">{money(36800)}</b><br />
                  <span style={{ color: "var(--primary)", cursor: "pointer", fontSize: 11 }}>查看付款方式 →</span>
                </div>
              </div>
            </div>

            <div className="card" style={{ padding: 18 }}>
              <h3 style={{ margin: "0 0 12px", fontSize: 14, fontWeight: 600 }}>责任人</h3>
              <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                {[
                  { name: PEOPLE.sales.name, role: "业务对接", color: "#1A5FB4", tel: "138-xxxx-1234" },
                  { name: PEOPLE.sd.name, role: "科技负责人", color: "#0E7A7A", tel: "—" },
                ].map((p) => (
                  <div key={p.name} style={{ display: "flex", gap: 10, alignItems: "center" }}>
                    <Avatar name={p.name.slice(0, 1)} color={p.color} size={36} />
                    <div style={{ flex: 1 }}>
                      <div style={{ fontSize: 13, fontWeight: 500 }}>{p.name}</div>
                      <div style={{ fontSize: 11, color: "var(--ink-600)" }}>{p.role} · <span className="mono">{p.tel}</span></div>
                    </div>
                    <span style={{ fontSize: 13, color: "var(--primary)", cursor: "pointer" }}>✉</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </DesktopShell>
  );
}

/* ============ P5: Portal Report Preview ============ */
function P5_ReportPreview({ onBack }) {
  const toast = useToast();
  const [page, setPage] = useState(1);
  const totalPages = 6;
  const [zoom, setZoom] = useState(100);

  return (
    <div style={{ background: "#2A3A5A", minHeight: "100vh", display: "flex", flexDirection: "column" }}>
      {/* Toolbar */}
      <div style={{ background: "#0F172A", color: "#fff", padding: "12px 24px", display: "flex", alignItems: "center", gap: 16, flexShrink: 0 }}>
        <span onClick={onBack} style={{ cursor: "pointer", fontSize: 13, color: "#9EBEE8" }}>← 返回项目</span>
        <div style={{ height: 20, width: 1, background: "#1B2942" }} />
        <span className="mono" style={{ fontSize: 13 }}>TR260408-03</span>
        <Tag tone="amber">草稿 v1 · 仅预览</Tag>
        <div style={{ flex: 1 }} />
        <div style={{ display: "flex", alignItems: "center", gap: 8, color: "#9EBEE8" }}>
          <span style={{ cursor: "pointer", padding: "4px 10px" }} onClick={() => setZoom(Math.max(60, zoom - 10))}>−</span>
          <span style={{ fontFamily: "Inter,sans-serif", fontSize: 13, minWidth: 50, textAlign: "center" }}>{zoom}%</span>
          <span style={{ cursor: "pointer", padding: "4px 10px" }} onClick={() => setZoom(Math.min(160, zoom + 10))}>＋</span>
          <div style={{ height: 16, width: 1, background: "#1B2942", margin: "0 8px" }} />
          <span style={{ cursor: "pointer" }} onClick={() => setPage(Math.max(1, page - 1))}>‹</span>
          <span className="mono" style={{ fontSize: 13 }}>{page} / {totalPages}</span>
          <span style={{ cursor: "pointer" }} onClick={() => setPage(Math.min(totalPages, page + 1))}>›</span>
        </div>
        <div style={{ height: 20, width: 1, background: "#1B2942" }} />
        <span title="付款后解锁" style={{ color: "#5D7AA8", cursor: "not-allowed", fontSize: 13 }}>🔒 下载</span>
        <span title="付款后解锁" style={{ color: "#5D7AA8", cursor: "not-allowed", fontSize: 13 }}>🔒 打印</span>
      </div>

      <div style={{ flex: 1, overflow: "auto", padding: "24px 0", display: "flex", justifyContent: "center" }}>
        <div style={{ width: 794 * (zoom / 100), transform: `scale(${zoom / 100})`, transformOrigin: "top center" }}>
          <ReportPage page={page} />
        </div>
      </div>

      {/* Right floating actions */}
      <div style={{ position: "fixed", right: 24, top: "50%", transform: "translateY(-50%)", display: "flex", flexDirection: "column", gap: 12 }}>
        <div style={{ width: 48, height: 48, background: "#fff", borderRadius: 10, boxShadow: "0 8px 24px rgba(0,0,0,.3)", display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer", color: "var(--primary)" }} title="联系业务" onClick={() => toast("已发送联系业务申请", "success")}>✉</div>
        <div style={{ width: 48, height: 48, background: "#fff", borderRadius: 10, boxShadow: "0 8px 24px rgba(0,0,0,.3)", display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer", color: "var(--warning)" }} title="申请加急" onClick={() => toast("已提交加急申请，需 SD 审批", "warning")}>⚡</div>
        <div style={{ width: 48, height: 48, background: "#fff", borderRadius: 10, boxShadow: "0 8px 24px rgba(0,0,0,.3)", display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer", color: "var(--success)" }} title="付款解锁">¥</div>
      </div>
    </div>
  );
}

function ReportPage({ page }) {
  return (
    <div className="doc" style={{ width: 794, minHeight: 1100, background: "#fff", boxShadow: "0 12px 40px rgba(0,0,0,.25)", margin: "0 auto", padding: 60, position: "relative" }}>
      <div className="watermark"><div className="watermark-text">DRAFT 草稿</div></div>
      <div style={{ position: "relative", zIndex: 2 }}>
        {page === 1 && <ReportCover />}
        {page === 2 && <ReportDeclaration />}
        {page === 3 && <ReportInfo />}
        {page === 4 && <ReportMethod />}
        {page === 5 && <ReportResults />}
        {page === 6 && <ReportSignature />}
      </div>
      <div style={{ position: "absolute", bottom: 30, left: 60, right: 60, display: "flex", justifyContent: "space-between", fontSize: 10, color: "var(--ink-400)", borderTop: "1px solid var(--line)", paddingTop: 8 }}>
        <span className="mono">TR260408-03</span>
        <span className="mono">第 {page} 页 / 共 {6} 页</span>
        <span>南京大学锡山应用生物技术研究所</span>
      </div>
    </div>
  );
}

function ReportCover() {
  return (
    <div style={{ textAlign: "center", paddingTop: 80 }}>
      <div style={{ fontSize: 12, color: "var(--ink-600)", letterSpacing: ".3em", marginBottom: 24 }}>WUXI XISHAN NJU INSTITUTE OF APPLIED BIOTECHNOLOGY</div>
      <div style={{ width: 80, height: 80, margin: "0 auto 24px", borderRadius: 16, background: "linear-gradient(135deg,#1A5FB4,#00A8A8)", display: "flex", alignItems: "center", justifyContent: "center", color: "#fff", fontWeight: 700, fontSize: 18, fontFamily: "Inter,sans-serif" }}>NJU</div>
      <div style={{ fontSize: 26, fontWeight: 600, marginBottom: 8 }}>检 测 报 告</div>
      <div style={{ fontSize: 14, color: "var(--ink-600)", marginBottom: 60, fontFamily: "Inter,sans-serif", letterSpacing: ".18em" }}>TEST REPORT</div>
      <div style={{ width: 80, height: 2, background: "var(--primary)", margin: "0 auto 40px" }} />
      <table style={{ margin: "0 auto", borderCollapse: "collapse", fontSize: 14 }}>
        <tbody>
          {[
            ["报告编号 No.", PROJECT.reportId],
            ["项目编号 Project ID", PROJECT.id],
            ["样品名称 Sample", SAMPLE.zh],
            ["检测项目 Test Item", "无菌检查 Sterility Test"],
            ["委托单位 Client", ORG.zh],
            ["签发日期 Issued Date", "待签发 PENDING"],
          ].map(([k, v]) => (
            <tr key={k}>
              <td style={{ padding: "10px 24px", color: "var(--ink-600)", textAlign: "right", border: 0 }}>{k}</td>
              <td style={{ padding: "10px 24px", fontWeight: 500, border: 0, textAlign: "left" }} className={k.includes("ID") || k.includes("No.") ? "mono" : ""}>{v}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <div style={{ marginTop: 80, fontSize: 12, color: "var(--ink-600)" }}>
        <div className="mono">CMA 认可编号 (CMA No.): 2017123456Z</div>
        <div className="mono">CNAS 认可编号 (CNAS No.): L12345</div>
      </div>
    </div>
  );
}
function ReportDeclaration() {
  return (
    <div style={{ paddingTop: 20 }}>
      <h2>声 明 Statement</h2>
      <ol style={{ fontSize: 13, lineHeight: 2.1, color: "var(--ink-900)", paddingLeft: 22 }}>
        <li>本报告无 CMA 章 / CNAS 章 / 检测专用章无效。</li>
        <li>复制本报告未重新加盖 "检测专用章" 或 "检验检测章" 无效。</li>
        <li>报告无主检、复核、签发人员签字无效；涂改无效。</li>
        <li>对本报告若有异议，应于收到报告之日起十五日内向本所提出，逾期不予受理。</li>
        <li>未经本所书面批准，不得复制、节选本报告。</li>
        <li>送样者自行送达的样品，本所只对来样负责。</li>
        <li>本报告检测结果只对受检的来样负责。</li>
      </ol>
      <div style={{ marginTop: 48, padding: 20, background: "var(--bg)", border: "1px dashed var(--line)", borderRadius: 6 }}>
        <div style={{ fontSize: 12, color: "var(--ink-600)", lineHeight: 1.8 }}>
          地址：{ORG.addr}<br />
          Address: {ORG.addrEn}<br />
          电话 Tel: <span className="mono">{ORG.tel}</span> &nbsp; 邮箱 Email: <span className="mono">{ORG.email}</span>
        </div>
      </div>
    </div>
  );
}
function ReportInfo() {
  return (
    <div>
      <h2>样品及检测信息 Sample & Test Information</h2>
      <table>
        <tbody>
          <tr><th style={{ width: "30%" }}>样品名称 Sample Name</th><td>{SAMPLE.zh} <span style={{ color: "var(--ink-600)" }}>/ {SAMPLE.en}</span></td></tr>
          <tr><th>样品型号 Model</th><td className="mono">{SAMPLE.model}</td></tr>
          <tr><th>样品批号 Lot No.</th><td className="mono">{SAMPLE.lot}</td></tr>
          <tr><th>制造商 Manufacturer</th><td>{SAMPLE.manufacturer}</td></tr>
          <tr><th>委托单位 Client</th><td>{ORG.zh}</td></tr>
          <tr><th>委托单位地址 Client Address</th><td>{ORG.addr}</td></tr>
          <tr><th>送样日期 Received Date</th><td className="mono">2026-04-10</td></tr>
          <tr><th>检测日期 Test Date</th><td className="mono">2026-04-12 ~ 2026-05-13</td></tr>
          <tr><th>样品状态 Sample Condition</th><td>包装完好，无破损 · Intact packaging</td></tr>
          <tr><th>样品数量 Quantity</th><td>12 片 / 12 pieces</td></tr>
          <tr><th>保存条件 Storage</th><td>常温干燥处保存 · Room temperature, dry</td></tr>
        </tbody>
      </table>
    </div>
  );
}
function ReportMethod() {
  return (
    <div>
      <h2>检测方法 Test Method</h2>
      <table>
        <tbody>
          <tr><th style={{ width: "30%" }}>检测项目 Test Item</th><td>无菌检查 <span style={{ color: "var(--ink-600)" }}>Sterility Test</span></td></tr>
          <tr><th>检测依据 Standard</th><td>{PROJECT.standard}</td></tr>
          <tr><th>检测方法 Method</th><td>{PROJECT.method} (Direct Inoculation)</td></tr>
          <tr><th>培养基 Medium</th><td>需氧菌·厌氧菌：硫乙醇酸盐流体培养基 (FTM)<br />真菌：胰酪大豆胨液体培养基 (TSB)</td></tr>
          <tr><th>培养温度 Temperature</th><td>FTM: 30 ~ 35 °C &nbsp;&nbsp; TSB: 20 ~ 25 °C</td></tr>
          <tr><th>培养时间 Duration</th><td>不少于 14 天 (≥ 14 days)</td></tr>
          <tr><th>主要仪器设备 Instruments</th><td>生化培养箱 HX-150 · 编号 DEV-2018-021<br />超净工作台 SW-CJ-2D · 编号 DEV-2019-005</td></tr>
        </tbody>
      </table>
    </div>
  );
}
function ReportResults() {
  return (
    <div>
      <h2>检测结果 Test Results</h2>
      <table>
        <thead>
          <tr><th>编号</th><th>培养基</th><th>培养温度</th><th>培养时间</th><th>结果</th></tr>
        </thead>
        <tbody>
          <tr><td className="mono">1#</td><td>FTM 硫乙醇酸盐</td><td className="mono">30~35°C</td><td className="mono">14 d</td><td style={{ color: "var(--success)", fontWeight: 600 }}>无菌生长</td></tr>
          <tr><td className="mono">2#</td><td>FTM 硫乙醇酸盐</td><td className="mono">30~35°C</td><td className="mono">14 d</td><td style={{ color: "var(--success)", fontWeight: 600 }}>无菌生长</td></tr>
          <tr><td className="mono">3#</td><td>TSB 胰酪大豆胨</td><td className="mono">20~25°C</td><td className="mono">14 d</td><td style={{ color: "var(--success)", fontWeight: 600 }}>无菌生长</td></tr>
          <tr><td className="mono">4#</td><td>TSB 胰酪大豆胨</td><td className="mono">20~25°C</td><td className="mono">14 d</td><td style={{ color: "var(--success)", fontWeight: 600 }}>无菌生长</td></tr>
          <tr><td className="mono">阴性对照</td><td colSpan="3">FTM + TSB 同条件培养</td><td style={{ color: "var(--success)", fontWeight: 600 }}>无菌生长</td></tr>
          <tr><td className="mono">阳性对照</td><td colSpan="3">金黄色葡萄球菌 / 白色念珠菌</td><td style={{ color: "var(--ink-600)", fontWeight: 600 }}>菌落生长 (符合)</td></tr>
        </tbody>
      </table>
      <h2 style={{ marginTop: 32 }}>结论 Conclusion</h2>
      <div style={{ padding: 20, background: "#ECFDF5", border: "2px solid #10B981", borderRadius: 6, fontSize: 14, lineHeight: 1.8 }}>
        所送 4 份样品在规定条件下培养 14 天，结果均显示<b style={{ color: "var(--success)" }}>无菌生长</b>，阳性对照菌生长良好，阴性对照无菌生长，试验有效。<br />
        <span className="mono">The submitted samples meet the sterility requirement of YY/T 1293.4-2016.</span>
      </div>
    </div>
  );
}
function ReportSignature() {
  return (
    <div>
      <h2>签 字 栏 Signatures</h2>
      <table>
        <thead>
          <tr><th style={{ width: "20%" }}>角色 Role</th><th>姓名 Name</th><th>签字 Signature</th><th style={{ width: "20%" }}>日期 Date</th></tr>
        </thead>
        <tbody>
          <tr><td>主检 Tester</td><td>{PEOPLE.test1.name}</td><td style={{ color: "var(--ink-400)", textAlign: "center", fontStyle: "italic" }}>(未签发)</td><td className="mono">—</td></tr>
          <tr><td>复核 QC</td><td>{PEOPLE.qc.name}</td><td style={{ color: "var(--ink-400)", textAlign: "center", fontStyle: "italic" }}>(未签发)</td><td className="mono">—</td></tr>
          <tr><td>审核 QA</td><td>{PEOPLE.qa.name}</td><td style={{ color: "var(--ink-400)", textAlign: "center", fontStyle: "italic" }}>(未签发)</td><td className="mono">—</td></tr>
          <tr><td>批准 SD</td><td>{PEOPLE.sd.name}</td><td style={{ color: "var(--ink-400)", textAlign: "center", fontStyle: "italic" }}>(未签发)</td><td className="mono">—</td></tr>
        </tbody>
      </table>
      <div style={{ marginTop: 40, display: "flex", gap: 40, alignItems: "center", justifyContent: "center" }}>
        {["CMA 章", "CNAS 章", "检验检测章"].map((s) => (
          <div key={s} style={{ width: 120, height: 120, border: "2px dashed #FECACA", borderRadius: "50%", display: "flex", alignItems: "center", justifyContent: "center", color: "#FECACA", fontSize: 13, fontWeight: 600 }}>{s}<br />未盖章</div>
        ))}
      </div>
      <div style={{ marginTop: 24, padding: 14, background: "#FEE2E2", border: "1px solid #FECACA", borderRadius: 6, fontSize: 12, color: "#991B1B", textAlign: "center", lineHeight: 1.7 }}>
        本报告为<b>草稿版本</b>，未经四级审核签字、未加盖资质章，<b>不作为正式检测结论使用</b>。<br />
        付款到账后将自动触发签发流程并生成正式版本。
      </div>
    </div>
  );
}

Object.assign(window, { P2_Dashboard, P4_ProjectDetail, P5_ReportPreview });
