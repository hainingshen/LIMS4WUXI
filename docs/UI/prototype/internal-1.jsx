/* ====================================================================
   INTERNAL · P6 Work Center ⭐
   ==================================================================== */

function P6_WorkCenter({ onNavigate }) {
  const toast = useToast();
  const [hoverHealth, setHoverHealth] = useState(false);

  const pieRef = useChart(() => ({
    tooltip: { trigger: "item" },
    series: [{
      type: "pie", radius: ["62%", "86%"], avoidLabelOverlap: false, label: { show: false },
      itemStyle: { borderColor: "#fff", borderWidth: 2 },
      data: [
        { value: 38, name: "正常", itemStyle: { color: "#10B981" } },
        { value: 11, name: "预警", itemStyle: { color: "#F59E0B" } },
        { value: 5, name: "风险", itemStyle: { color: "#EF4444" } },
      ],
    }],
    graphic: [
      { type: "text", left: "center", top: "42%", style: { text: "54", fontFamily: "Inter", fontSize: 30, fontWeight: 600, fill: "#0F172A" } },
      { type: "text", left: "center", top: "62%", style: { text: "进行中项目", fontSize: 11, fill: "#94A3B8" } },
    ],
  }), []);

  const miniPie1 = useChart(() => ({
    series: [{
      type: "pie", radius: ["60%", "100%"], label: { show: false },
      data: [
        { value: 38, itemStyle: { color: "#10B981" } },
        { value: 11, itemStyle: { color: "#F59E0B" } },
        { value: 5, itemStyle: { color: "#EF4444" } },
      ],
    }],
  }), []);

  const trendRef = useChart(() => ({
    tooltip: { trigger: "axis" },
    grid: { left: 36, right: 16, top: 24, bottom: 28 },
    xAxis: { type: "category", data: ["W14", "W15", "W16", "W17", "W18", "W19"], axisLine: { lineStyle: { color: "#E2E8F0" } }, axisLabel: { color: "#94A3B8", fontFamily: "Inter", fontSize: 11 } },
    yAxis: { type: "value", splitLine: { lineStyle: { color: "#F1F5F9" } }, axisLabel: { color: "#94A3B8", fontFamily: "Inter", fontSize: 11 } },
    series: [
      { name: "计划完成", type: "line", smooth: true, data: [18, 22, 25, 28, 30, 32], itemStyle: { color: "#94A3B8" }, lineStyle: { type: "dashed", width: 2 }, symbol: "circle", symbolSize: 6 },
      { name: "实际完成", type: "line", smooth: true, data: [16, 21, 24, 27, 31, 29], itemStyle: { color: "#1A5FB4" }, lineStyle: { width: 2.5 },
        areaStyle: { color: { type: "linear", x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: "rgba(26,95,180,.18)" }, { offset: 1, color: "rgba(26,95,180,0)" }] } },
        symbol: "circle", symbolSize: 7, markPoint: { data: [{ type: "max" }], itemStyle: { color: "#00A8A8" } } },
    ],
  }), []);

  return (
    <DesktopShell side={<InternalSidebar active="work-center" />} top={<TopBar crumbs={["仪表盘", "工作中心"]} />}>
      <div style={{ padding: 24 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", marginBottom: 18 }}>
          <div>
            <h1 style={{ margin: 0, fontSize: 22, fontWeight: 600 }}>项目工作中心</h1>
            <div style={{ fontSize: 13, color: "var(--ink-600)", marginTop: 4 }}>2026-05-13 · 共 54 个进行中项目 · 数据每 60s 自动刷新</div>
          </div>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
            {["全部项目", "生物相容性", "微生物", "包装", "非标方法", "加急"].map((c, i) => (
              <span key={c} style={{ fontSize: 12, padding: "5px 12px", background: i === 0 ? "var(--primary-50)" : "#fff", border: "1px solid " + (i === 0 ? "#A8C5E8" : "var(--line)"), borderRadius: 999, color: i === 0 ? "var(--primary)" : "var(--ink-600)", cursor: "pointer" }}>{c}</span>
            ))}
          </div>
        </div>

        {/* KPIs */}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(4,1fr)", gap: 14, marginBottom: 18 }}>
          <div className="card" style={{ padding: 18, position: "relative", overflow: "hidden", cursor: "pointer" }}
            onMouseEnter={() => setHoverHealth(true)} onMouseLeave={() => setHoverHealth(false)}>
            <div style={{ position: "absolute", left: 0, top: 0, bottom: 0, width: 3, background: "var(--primary)" }} />
            <div style={{ fontSize: 12, color: "var(--ink-600)" }}>项目健康度概览</div>
            <div style={{ fontFamily: "Inter,sans-serif", fontSize: 30, fontWeight: 600, color: "var(--ink-900)", marginTop: 4 }}>54</div>
            <div style={{ fontSize: 11, color: "var(--ink-600)", marginTop: 6, display: "flex", gap: 10 }}>
              <span><b style={{ color: "#10B981" }}>● 38</b> 正常</span>
              <span><b style={{ color: "#F59E0B" }}>● 11</b> 预警</span>
              <span><b style={{ color: "#EF4444" }}>● 5</b> 风险</span>
            </div>
            <div ref={miniPie1} style={{ position: "absolute", right: 14, top: 12, width: 54, height: 54 }} />
            {hoverHealth && (
              <div style={{ position: "absolute", top: "100%", left: 0, right: 0, marginTop: 8, background: "#0F172A", color: "#fff", padding: 14, borderRadius: 6, fontSize: 12, lineHeight: 1.8, zIndex: 10, boxShadow: "0 8px 24px rgba(0,0,0,.2)" }}>
                <div style={{ fontWeight: 600, marginBottom: 4 }}>健康度评分规则</div>
                <div>初始 100 分，按规则扣分：</div>
                <div>-15 连续 2 天无进展 &nbsp; -10 待审批阻塞 1 项</div>
                <div>-8 设备 / 物料阻塞 &nbsp; -5 客户未确认</div>
                <div>-20 超期 &nbsp; -3 加急未付款</div>
              </div>
            )}
          </div>
          <Stat label="我的待办" value="12" accent="accent" delta="报告审核 7 · 非标评审 3 · 异常处理 2" />
          <Stat label="超期任务" value="5" accent="danger" delta="▼ 较上周 -1 · 最长超期 6 天" deltaTone="down" />
          <Stat label="本周完成" value="29" suffix="项报告" accent="success" delta="▲ 达成率 91% / 目标 32" deltaTone="up" />
        </div>

        {/* 3 cols */}
        <div style={{ display: "grid", gridTemplateColumns: ".95fr 1.3fr .95fr", gap: 14 }}>
          {/* Health panel */}
          <div className="card" style={{ padding: 16, display: "flex", flexDirection: "column" }}>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 8 }}>
              <h3 style={{ margin: 0, fontSize: 14, fontWeight: 600 }}>项目健康度分布</h3>
              <span style={{ fontSize: 12, color: "var(--primary)", cursor: "pointer" }}>明细 →</span>
            </div>
            <div style={{ display: "flex", gap: 14, alignItems: "center" }}>
              <div ref={pieRef} style={{ width: 130, height: 130, flexShrink: 0 }} />
              <div style={{ display: "flex", flexDirection: "column", gap: 8, fontSize: 12, flex: 1 }}>
                {[["#10B981", "正常 · 80-100", "38"], ["#F59E0B", "预警 · 60-79", "11"], ["#EF4444", "风险 · <60", "5"]].map(([c, l, n]) => (
                  <div key={l} style={{ display: "flex", justifyContent: "space-between" }}>
                    <span style={{ color: "var(--ink-600)", display: "flex", gap: 6, alignItems: "center" }}><span style={{ width: 8, height: 8, borderRadius: 2, background: c }}></span>{l}</span>
                    <span className="mono" style={{ fontWeight: 600 }}>{n} 项</span>
                  </div>
                ))}
              </div>
            </div>
            <div style={{ fontSize: 12, fontWeight: 600, color: "var(--ink-600)", marginTop: 16, marginBottom: 8 }}>Top 5 风险项目</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 8, flex: 1 }}>
              {[
                ["P-2026-0007", "某创新医疗 · 一次性导尿管", 42, "r", "-20 阻塞 5 天 · -18 客户未确认"],
                ["P-2026-0012", "华东医械 · 关节假体涂层", 48, "r", "-15 设备冲突 · -12 物料延误"],
                ["P-2026-0019", "康源生物 · 心脏起搏器外壳", 58, "w", "-12 SD 未审 · -10 加急未付款"],
                ["P-2026-0024", "锐康医疗 · 心电监护导联线", 64, "w", "-8 计划超期 1 天"],
                ["P-2026-0031", "晶辰生物 · 一次性透析管路", 68, "w", "-7 浸提条件待客户确认"],
              ].map(([id, nm, sc, t, why]) => (
                <div key={id} style={{ padding: 10, border: "1px solid var(--line)", borderRadius: 6, background: "#FCFDFE", cursor: "pointer" }} onClick={() => onNavigate && onNavigate("P8")}>
                  <div style={{ display: "flex", justifyContent: "space-between" }}>
                    <span className="mono" style={{ fontSize: 11, fontWeight: 600 }}>{id}</span>
                    <span className="mono" style={{ fontSize: 13, fontWeight: 600, color: t === "r" ? "var(--danger)" : "var(--warning)" }}>{sc}</span>
                  </div>
                  <div style={{ fontSize: 12, color: "var(--ink-900)", marginTop: 2 }}>{nm}</div>
                  <div style={{ fontSize: 11, color: "var(--ink-400)", marginTop: 4 }}>{why}</div>
                </div>
              ))}
            </div>
          </div>

          {/* Blocks board */}
          <div className="card" style={{ padding: 16 }}>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 12 }}>
              <h3 style={{ margin: 0, fontSize: 14, fontWeight: 600 }}>当前阻塞看板</h3>
              <span style={{ fontSize: 12, color: "var(--primary)", cursor: "pointer" }}>全部 24 项 →</span>
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
              <BlockCol title="待审批" n={9} items={[
                { id: "P-2026-0001", t: "5h", tone: "r", ds: "非标方法评审 · 妥能水胶体敷贴", owner: "陈业务", av: "陈", role: "待 SD 批准" },
                { id: "P-2026-0007", t: "2d", tone: "r", ds: "报告 v2 SD 终审 · 影响发放", owner: "周教授", av: "周", role: "QA 已通过" },
                { id: "P-2026-0023", t: "1d", tone: "w", ds: "绿色通道申请 · 加急放行", owner: "刘老师", av: "刘", role: "QA 审核中" },
              ]} />
              <BlockCol title="设备 / 物料" n={6} items={[
                { id: "P-2026-0012", t: "3d", tone: "r", ds: "HPLC-001 与外部预约冲突", owner: "李工程师", av: "李", role: "已发覆盖申请" },
                { id: "P-2026-0015", t: "1d", tone: "w", ds: "三蒸水未到货 · 阻塞细胞毒性", owner: "张博士", av: "张", role: "采购已下单" },
                { id: "P-2026-0019", t: "12h", tone: "w", ds: "待客户确认浸提条件变更", owner: "陈业务", av: "陈", role: "已发邮件" },
              ]} />
              <BlockCol title="QC / QA 复核" n={5} items={[
                { id: "P-2026-0028", t: "8h", tone: "w", ds: "原始记录数据点缺失复测", owner: "赵工", av: "赵", role: "已发回主检" },
                { id: "P-2025-0182", t: "1d", tone: "w", ds: "QA 标注 3 处需改", owner: "刘老师", av: "刘", role: "等主检修订" },
              ]} />
              <BlockCol title="客户确认" n={4} items={[
                { id: "P-2026-0033", t: "2d", tone: "r", ds: "需重新签补充协议（批号变更）", owner: "陈业务", av: "陈", role: "客户未回复" },
                { id: "P-2026-0036", t: "6h", tone: "w", ds: "草稿预览邮件未读", owner: "陈业务", av: "陈", role: "已重发" },
              ]} />
            </div>
          </div>

          {/* Heatmap */}
          <div className="card" style={{ padding: 16, display: "flex", flexDirection: "column" }}>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 12 }}>
              <h3 style={{ margin: 0, fontSize: 14, fontWeight: 600 }}>人员负荷热力图</h3>
              <span className="mono" style={{ fontSize: 11, color: "var(--ink-400)" }}>05-13 ~ 05-19</span>
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "62px repeat(7,1fr)", gap: 3, fontSize: 11 }}>
              <div></div>
              {["二", "三", "四", "五", "六", "日", "一"].map((d) => (
                <div key={d} style={{ textAlign: "center", color: "var(--ink-400)", fontSize: 10, fontFamily: "Inter,sans-serif", padding: "2px 0" }}>周{d}</div>
              ))}
              {[
                ["张博士", [3, 4, 4, 3, 2, 0, 2]],
                ["李工程师", [2, 3, 3, 4, 4, 1, 3]],
                ["王主管", [1, 2, 2, 2, 1, 0, 1]],
                ["赵工 QC", [4, 4, 3, 4, 3, 2, 3]],
                ["刘 QA", [2, 3, 3, 2, 2, 0, 2]],
                ["孙样品", [3, 3, 4, 4, 3, 1, 3]],
                ["钱研究员", [1, 2, 3, 3, 2, 0, 1]],
                ["陈业务", [2, 2, 3, 3, 4, 1, 2]],
              ].map(([nm, arr]) => (
                <Fragment key={nm}>
                  <div style={{ fontSize: 11, color: "var(--ink-600)", display: "flex", alignItems: "center" }}>{nm}</div>
                  {arr.map((v, i) => {
                    const colors = ["#F1F5F9", "#CFE0F4", "#7FAEDB", "#1A5FB4", "#0E3A6E"];
                    const labels = ["—", "25", "50", "75", "100"];
                    const color = v > 4 ? "#EF4444" : colors[v];
                    const label = v > 4 ? "120" : labels[v];
                    return <div key={i} style={{ background: color, height: 22, borderRadius: 3, display: "flex", alignItems: "center", justifyContent: "center", color: v === 0 ? "var(--ink-400)" : "#fff", fontSize: 10, fontFamily: "Inter,sans-serif" }} title={label + "%"}>{v === 0 ? "" : label}</div>;
                  })}
                </Fragment>
              ))}
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: 6, fontSize: 11, color: "var(--ink-400)", marginTop: 12, justifyContent: "flex-end" }}>
              负荷
              <div style={{ display: "flex", gap: 2 }}>
                {["#F1F5F9", "#CFE0F4", "#7FAEDB", "#1A5FB4", "#0E3A6E", "#EF4444"].map((c) => <span key={c} style={{ width: 14, height: 10, borderRadius: 2, background: c, display: "block" }}></span>)}
              </div>
              低 → 高 · 红超载
            </div>

            <div style={{ marginTop: 16, padding: 12, background: "var(--bg)", borderRadius: 6 }}>
              <div style={{ fontSize: 11, color: "var(--ink-600)", fontWeight: 600, marginBottom: 6 }}>本周负荷预警</div>
              <div style={{ fontSize: 11, color: "var(--danger)", lineHeight: 1.8 }}>
                ⚠ 赵工 QC 周一负荷 100%，建议调度<br />
                ⚠ 李工程师 周四/周五 ≥ 100%
              </div>
            </div>
          </div>
        </div>

        {/* Trend */}
        <div className="card" style={{ padding: 16, marginTop: 14 }}>
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 8 }}>
            <h3 style={{ margin: 0, fontSize: 14, fontWeight: 600 }}>近 6 周进度趋势</h3>
            <div style={{ display: "flex", gap: 16, fontSize: 11, color: "var(--ink-600)" }}>
              <span style={{ display: "inline-flex", alignItems: "center", gap: 6 }}><span style={{ width: 14, height: 0, borderTop: "2px dashed #94A3B8" }}></span>计划完成</span>
              <span style={{ display: "inline-flex", alignItems: "center", gap: 6 }}><span style={{ width: 14, height: 2, background: "#1A5FB4" }}></span>实际完成</span>
            </div>
          </div>
          <div ref={trendRef} style={{ height: 180 }} />
        </div>
      </div>
    </DesktopShell>
  );
}

function BlockCol({ title, n, items }) {
  return (
    <div style={{ background: "#FAFBFD", borderRadius: 8, padding: 10, display: "flex", flexDirection: "column", gap: 8 }}>
      <div style={{ fontSize: 11, fontWeight: 600, color: "var(--ink-400)", letterSpacing: ".10em", textTransform: "uppercase", fontFamily: "Inter,sans-serif", display: "flex", justifyContent: "space-between" }}>
        <span>{title}</span>
        <span style={{ color: "var(--ink-900)", background: "#fff", padding: "1px 7px", borderRadius: 999, border: "1px solid var(--line)" }}>{n}</span>
      </div>
      {items.map((it) => (
        <div key={it.id} style={{ background: "#fff", border: "1px solid var(--line)", borderRadius: 6, padding: 10, fontSize: 12 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <span className="mono" style={{ fontSize: 11, fontWeight: 600, color: "var(--ink-600)" }}>{it.id}</span>
            <span style={{ fontSize: 11, color: it.tone === "r" ? "var(--danger)" : "var(--warning)", fontWeight: 600 }}>{it.t}</span>
          </div>
          <div style={{ fontSize: 12, color: "var(--ink-900)", margin: "4px 0", lineHeight: 1.4 }}>{it.ds}</div>
          <div style={{ display: "flex", alignItems: "center", gap: 6, fontSize: 11, color: "var(--ink-600)" }}>
            <Avatar name={it.av} size={18} color="#1A5FB4" />
            <span>{it.owner} · {it.role}</span>
          </div>
        </div>
      ))}
    </div>
  );
}

window.P6_WorkCenter = P6_WorkCenter;
