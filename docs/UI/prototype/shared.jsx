/* ====================================================================
   SHARED · Design tokens, sample data, reusable components
   ==================================================================== */

const { useState, useEffect, useRef, useMemo, useCallback, createContext, useContext, Fragment } = React;

/* ---------- Sample data (real, NOT lorem) ---------- */
const ORG = {
  zh: "无锡市南京大学锡山应用生物技术研究所",
  en: "Wuxi Xishan NJU Institute of Applied Biotechnology",
  addr: "江苏省无锡市锡山区东安路 88 号精准医疗产业园 B 栋 5 楼",
  addrEn: "B5, Precision Medicine Park, 88 Dong'an Road, Xishan District, Wuxi, Jiangsu",
  email: "med@sudatest.com",
  tel: "(0512) 65880038",
};

const SAMPLE = {
  zh: "妥能一次性使用水胶体敷贴",
  en: "Disposable hydrocolloid dressing",
  model: "1 片/袋",
  lot: "20250909",
  manufacturer: "无锡某医疗器械有限公司",
  manufacturerEn: "Wuxi Medical Device Co., Ltd.",
};

const PROJECT = {
  id: "P-2026-0001",
  subId: "SUB-2026-0001",
  woId: "WO-2026-00001",
  rrId: "RR-2026-00001",
  reportId: "TR260408-03",
  type: "STD_ACCREDITED",
  health: 92,
  status: "IN_TESTING",
  testItem: "无菌检测",
  testItemCode: "MICRO_STERILITY",
  method: "直接接种法",
  standard: "YY/T 1293.4-2016 接触性创面敷料 第 4 部分：水胶体敷料",
  acceptedAt: "2026-04-08",
  plannedFinish: "2026-05-22",
  contractAmount: 36800,
  paid: 0,
  paymentStatus: "UNPAID",
};

const PEOPLE = {
  sales: { name: "陈业务", role: "业务员", av: "陈", color: "#1A5FB4" },
  sd: { name: "周教授", role: "科技负责人 SD", av: "周", color: "#0E7A7A" },
  qa: { name: "刘老师", role: "QA 质量审核", av: "刘", color: "#7C2D12" },
  qc: { name: "赵工", role: "QC 数据复核", av: "赵", color: "#5B21B6" },
  test1: { name: "张博士", role: "主检测员", av: "张", color: "#1A5FB4" },
  test2: { name: "李工程师", role: "检测员", av: "李", color: "#047857" },
  test3: { name: "王主管", role: "检测主管", av: "王", color: "#92400E" },
  sample: { name: "孙样品", role: "样品管理员", av: "孙", color: "#0E2E5A" },
  finance: { name: "钱财务", role: "财务", av: "钱", color: "#9A3412" },
};

/* 14-node state machine */
const STATE_MACHINE = [
  ["DRAFT", "草稿"],
  ["ACCEPTED", "已受理"],
  ["CONTRACT_PENDING", "合同待签"],
  ["SAMPLE_INCOMING", "样品在途"],
  ["SAMPLE_RECEIVED", "样品到达"],
  ["PLAN_CONFIRMED", "计划确认"],
  ["IN_TESTING", "试验中"],
  ["DATA_REVIEW", "数据复核"],
  ["REPORT_DRAFT", "报告草稿"],
  ["QC_REVIEWING", "QC 复核"],
  ["QA_REVIEWING", "QA 审核"],
  ["SD_APPROVED", "SD 批准"],
  ["PAYMENT_HOLD", "付款门禁"],
  ["RELEASED", "已发放"],
];

/* Customer-facing list of in-flight projects */
const CUSTOMER_PROJECTS = [
  {
    id: "P-2026-0001",
    sampleZh: "妥能一次性使用水胶体敷贴",
    sampleEn: "Disposable hydrocolloid dressing",
    item: "无菌检测",
    stage: 3, // 0..4 simple progress: 受理→到达→试验→报告→发放
    statusText: "试验中",
    statusTone: "blue",
    plannedFinish: "2026-05-22",
    paid: false,
  },
  {
    id: "P-2026-0023",
    sampleZh: "一次性使用聚氨酯泡沫敷料",
    sampleEn: "PU foam dressing",
    item: "生物相容性 (细胞毒性 / 致敏 / 刺激)",
    stage: 2,
    statusText: "样品到达",
    statusTone: "cyan",
    plannedFinish: "2026-06-04",
    paid: false,
  },
  {
    id: "P-2025-0182",
    sampleZh: "壳聚糖创伤敷料",
    sampleEn: "Chitosan wound dressing",
    item: "微生物 · 包装完整性",
    stage: 4,
    statusText: "待付款发放",
    statusTone: "amber",
    plannedFinish: "2026-05-08",
    paid: false,
  },
  {
    id: "P-2025-0156",
    sampleZh: "医用透明质酸钠凝胶",
    sampleEn: "Sodium hyaluronate gel",
    item: "化学 (pH / 重金属) · 物理",
    stage: 4,
    statusText: "已签发",
    statusTone: "green",
    plannedFinish: "2026-04-12",
    paid: true,
  },
];

const NOTICES = [
  { type: "公告", t: "2026-05-12", title: "五·一假期实验室排班通知", tone: "blue" },
  { type: "维护", t: "2026-05-10", title: "客户门户 V2.3 上线 · 新增报告下载验签", tone: "cyan" },
  { type: "资质", t: "2026-04-28", title: "CNAS 复评审已通过，证书有效期至 2029-04", tone: "green" },
];

const TODOS = [
  { kind: "确认协议", project: "P-2026-0001", text: "非标方法评审补充协议待签署", urgent: true },
  { kind: "草稿预览", project: "P-2025-0182", text: "无菌检测报告草稿已就绪 · 请确认", urgent: false },
  { kind: "付款", project: "P-2025-0182", text: "应付 ¥ 28,500 · 付款后可下载正式报告", urgent: true },
];

/* Bio-compat test matrix (11 items) */
const BIO_TESTS = [
  ["细胞毒性", "Cytotoxicity", "MEM 浸提法 / 直接接触法", true],
  ["皮肤刺激", "Skin irritation", "豚鼠 / 兔皮试验", false],
  ["致敏", "Sensitization", "GPMT / LLNA / 封闭斑贴", false],
  ["急性全身毒性", "Acute systemic toxicity", "小鼠静脉 / 腹腔注射", false],
  ["热原", "Pyrogen", "兔法 / 鲎试剂法", false],
  ["亚急性毒性", "Subacute toxicity", "28d 反复给药", false],
  ["亚慢性毒性", "Subchronic toxicity", "90d 反复给药", false],
  ["慢性毒性", "Chronic toxicity", "≥180d 反复给药", false],
  ["遗传毒性", "Genotoxicity", "Ames / 染色体畸变 / 微核", false],
  ["植入", "Implantation", "肌肉 / 皮下 / 骨植入", false],
  ["血液相容性", "Hemocompatibility", "溶血 / 凝血 / 补体", false],
];

const OTHER_TESTS = [
  ["微生物", [
    ["MICRO_STERILITY", "无菌检测", "直接接种 / 薄膜过滤", true],
    ["MICRO_BIOBURDEN", "微生物限度", "需氧菌 / 霉菌酵母", false],
    ["MICRO_ENDOTOXIN", "细菌内毒素", "凝胶法 / 动态浊度", false],
  ]],
  ["包装", [
    ["PKG_INTEGRITY", "包装完整性", "气泡法 / 真空衰减", false],
    ["PKG_SEAL", "密封强度", "拉伸法", false],
    ["PKG_PEEL", "剥离强度", "180° 剥离", false],
  ]],
  ["物理", [
    ["PHY_DIMENSION", "尺寸测量", "游标 / 三坐标", false],
    ["PHY_ABSORPTION", "吸液量", "饱和吸液", false],
    ["PHY_BREATH", "透气性", "杯式法", false],
  ]],
  ["化学", [
    ["CHEM_PH", "pH 值", "电位法", false],
    ["CHEM_HEAVY", "重金属", "ICP-MS", false],
    ["CHEM_ENDOTOXIN", "EO 残留", "GC 顶空", false],
  ]],
];

/* ---------- Components ---------- */

/* Toast system */
const ToastCtx = createContext(null);
function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const push = useCallback((msg, kind = "info", ms = 2200) => {
    const id = Math.random().toString(36).slice(2);
    setToasts((t) => [...t, { id, msg, kind }]);
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), ms);
  }, []);
  return (
    <ToastCtx.Provider value={push}>
      {children}
      <div className="toast-wrap">
        {toasts.map((t) => (
          <div key={t.id} className={"toast " + t.kind}>
            {t.kind === "success" && "✓ "}
            {t.kind === "danger" && "✕ "}
            {t.kind === "warning" && "⚠ "}
            {t.msg}
          </div>
        ))}
      </div>
    </ToastCtx.Provider>
  );
}
const useToast = () => useContext(ToastCtx) || (() => {});

/* Modal */
function Modal({ open, onClose, title, children, footer, width = 520 }) {
  if (!open) return null;
  return (
    <div className="modal-bk" onClick={onClose}>
      <div className="modal" style={{ width }} onClick={(e) => e.stopPropagation()}>
        {title && (
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
            <h3 style={{ margin: 0, fontSize: 16, fontWeight: 600 }}>{title}</h3>
            <span onClick={onClose} style={{ cursor: "pointer", fontSize: 18, color: "#94A3B8" }}>×</span>
          </div>
        )}
        {children}
        {footer && <div style={{ marginTop: 20, display: "flex", justifyContent: "flex-end", gap: 8 }}>{footer}</div>}
      </div>
    </div>
  );
}

/* Drawer (right) */
function Drawer({ open, onClose, title, children, width }) {
  if (!open) return null;
  return (
    <div className="modal-bk right" onClick={onClose}>
      <div className="drawer" style={{ width }} onClick={(e) => e.stopPropagation()}>
        <div style={{ padding: "18px 24px", borderBottom: "1px solid var(--line)", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <h3 style={{ margin: 0, fontSize: 16, fontWeight: 600 }}>{title}</h3>
          <span onClick={onClose} style={{ cursor: "pointer", fontSize: 20, color: "#94A3B8" }}>×</span>
        </div>
        <div style={{ flex: 1, overflow: "auto", padding: 24 }}>{children}</div>
      </div>
    </div>
  );
}

/* Status tag with color mapping */
function Tag({ tone = "grey", dot, children, style }) {
  return (
    <span className={"tag tag-" + tone} style={style}>
      {dot && <span className="d" style={{ background: dotColor(tone) }}></span>}
      {children}
    </span>
  );
}
function dotColor(tone) {
  return ({ blue: "#1A5FB4", cyan: "#00A8A8", green: "#10B981", amber: "#F59E0B", red: "#EF4444", grey: "#94A3B8", deep: "#0E2E5A" })[tone];
}

/* Avatar */
function Avatar({ name, color, size = 28, ring }) {
  const ch = (name || "?").slice(0, 1);
  return (
    <div
      style={{
        width: size, height: size, borderRadius: "50%", background: color || "#1A5FB4", color: "#fff",
        display: "flex", alignItems: "center", justifyContent: "center", fontFamily: "Inter,Noto Sans SC,sans-serif",
        fontWeight: 600, fontSize: size * 0.42, flexShrink: 0,
        boxShadow: ring ? "0 0 0 2px " + ring : undefined,
      }}
    >
      {ch}
    </div>
  );
}

/* Statistic card */
function Stat({ label, value, suffix, delta, deltaTone, accent = "primary", icon, mini }) {
  return (
    <div className="card" style={{ padding: "16px 18px", position: "relative", overflow: "hidden" }}>
      <div style={{ position: "absolute", left: 0, top: 0, bottom: 0, width: 3, background: `var(--${accent})` }} />
      <div style={{ fontSize: 12, color: "var(--ink-600)", display: "flex", justifyContent: "space-between" }}>
        <span>{label}</span>
        {icon && <span style={{ color: "var(--ink-400)" }}>{icon}</span>}
      </div>
      <div style={{ fontFamily: "Inter,sans-serif", fontSize: 28, fontWeight: 600, color: "var(--ink-900)", letterSpacing: "-.01em", marginTop: 4, lineHeight: 1.2 }}>
        {value}
        {suffix && <span style={{ fontSize: 13, color: "var(--ink-400)", fontWeight: 500, marginLeft: 4 }}>{suffix}</span>}
      </div>
      {delta && (
        <div style={{ fontSize: 11, marginTop: 6, color: deltaTone === "up" ? "var(--success)" : deltaTone === "down" ? "var(--danger)" : "var(--ink-600)" }}>
          {delta}
        </div>
      )}
      {mini}
    </div>
  );
}

/* Horizontal state machine */
function StateMachineFlow({ currentIndex = 6, completedIndexes, archived = false, archivedIndex = 14, blockedIndex = -1 }) {
  return (
    <div style={{ overflowX: "auto", padding: "12px 0" }}>
      <div style={{ display: "flex", alignItems: "center", minWidth: 1000 }}>
        {STATE_MACHINE.map((s, i) => {
          const done = completedIndexes ? completedIndexes.includes(i) : i < currentIndex;
          const cur = i === currentIndex;
          const blk = i === blockedIndex;
          return (
            <Fragment key={s[0]}>
              <div style={{ flex: 1, minWidth: 70, display: "flex", flexDirection: "column", alignItems: "center" }}>
                <div
                  style={{
                    width: 28, height: 28, borderRadius: "50%",
                    background: done ? "var(--primary)" : cur ? "var(--accent)" : "#fff",
                    border: done ? "none" : cur ? "none" : "2px solid var(--line)",
                    color: done || cur ? "#fff" : "var(--ink-400)",
                    display: "flex", alignItems: "center", justifyContent: "center",
                    fontFamily: "Inter,sans-serif", fontSize: 11, fontWeight: 600,
                    boxShadow: cur ? "0 0 0 5px rgba(0,168,168,.18)" : undefined,
                    position: "relative",
                  }}
                >
                  {blk ? "🔒" : done ? "✓" : cur ? "●" : i + 1}
                </div>
                <div style={{ fontSize: 11, marginTop: 6, textAlign: "center", color: done || cur ? "var(--ink-900)" : "var(--ink-400)", fontWeight: cur || done ? 500 : 400, lineHeight: 1.3, padding: "0 4px" }}>
                  {s[1]}
                </div>
                <div className="mono" style={{ fontSize: 9, color: "var(--ink-400)", marginTop: 2, letterSpacing: ".05em" }}>{s[0]}</div>
              </div>
              {i < STATE_MACHINE.length - 1 && (
                <div style={{ width: 28, height: 2, background: done ? "var(--primary)" : "var(--line)", marginTop: -34, flexShrink: 0 }} />
              )}
            </Fragment>
          );
        })}
      </div>
    </div>
  );
}

/* Simple 5-step progress bar (for customer portal) */
function ProgressSteps({ stage = 2 }) {
  const labels = ["受理", "样品到达", "试验", "报告", "发放"];
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
      {labels.map((l, i) => (
        <Fragment key={l}>
          <div style={{ flex: 1, height: 6, borderRadius: 3, background: i <= stage ? "linear-gradient(90deg,var(--primary),var(--accent))" : "var(--line)" }} />
        </Fragment>
      ))}
    </div>
  );
}

/* AntD Statistic-like big number with currency */
function money(n) {
  return "¥ " + Number(n).toLocaleString("zh-CN");
}

/* Form field */
function Field({ label, en, required, help, error, hint, children }) {
  return (
    <div style={{ marginBottom: 16 }}>
      <label className="f-label">
        {required && <span className="req">*</span>}
        {label}
        {en && <span style={{ fontSize: 12, color: "var(--ink-400)", fontFamily: "Inter,sans-serif", marginLeft: 8, fontWeight: 400 }}>{en}</span>}
        {hint && <i className="i" title={hint}>i</i>}
      </label>
      {children}
      {help && <div className="f-help">{help}</div>}
      {error && <div className="f-error">{error}</div>}
    </div>
  );
}

/* Card-style radio group */
function CardRadio({ value, onChange, options }) {
  return (
    <div style={{ display: "grid", gridTemplateColumns: `repeat(${options.length},1fr)`, gap: 10 }}>
      {options.map((o) => {
        const sel = o.value === value;
        return (
          <div
            key={o.value}
            onClick={() => onChange && onChange(o.value)}
            style={{
              padding: 14, borderRadius: 8, border: "1px solid " + (sel ? "var(--primary)" : "var(--line)"),
              background: sel ? "var(--primary-50)" : "#fff", cursor: "pointer", textAlign: "center",
              transition: "all .15s",
            }}
          >
            {o.icon && <div style={{ fontSize: 22, marginBottom: 6, color: sel ? "var(--primary)" : "var(--ink-400)" }}>{o.icon}</div>}
            <div style={{ fontSize: 13, fontWeight: sel ? 600 : 500, color: sel ? "var(--primary)" : "var(--ink-900)" }}>{o.label}</div>
            {o.en && <div style={{ fontSize: 11, color: "var(--ink-400)", fontFamily: "Inter,sans-serif", marginTop: 2 }}>{o.en}</div>}
            {o.desc && <div style={{ fontSize: 11, color: "var(--ink-600)", marginTop: 4 }}>{o.desc}</div>}
          </div>
        );
      })}
    </div>
  );
}

/* Toggle */
function Toggle({ value, onChange, labels = ["", ""] }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
      <span
        onClick={() => onChange && onChange(!value)}
        style={{
          width: 40, height: 22, borderRadius: 11, background: value ? "var(--primary)" : "#CBD5E1",
          position: "relative", cursor: "pointer", transition: "background .15s", flexShrink: 0,
        }}
      >
        <span
          style={{
            position: "absolute", top: 2, left: value ? 20 : 2, width: 18, height: 18,
            borderRadius: "50%", background: "#fff", transition: "left .15s", boxShadow: "0 1px 3px rgba(0,0,0,.2)",
          }}
        />
      </span>
      <span style={{ fontSize: 13, color: "var(--ink-700)" }}>{value ? labels[0] : labels[1]}</span>
    </div>
  );
}

/* Brand header (left credentials badge) */
function CredBadges({ size = "sm" }) {
  const fs = size === "sm" ? 10 : 11;
  return (
    <div style={{ display: "flex", gap: 6 }}>
      <span style={{ fontSize: fs, fontWeight: 600, padding: "2px 8px", borderRadius: 4, background: "var(--accent-50)", color: "var(--accent-700)", border: "1px solid #B7E3E3", fontFamily: "Inter,sans-serif" }}>CMA</span>
      <span style={{ fontSize: fs, fontWeight: 600, padding: "2px 8px", borderRadius: 4, background: "var(--primary-50)", color: "var(--primary)", border: "1px solid #CFE0F4", fontFamily: "Inter,sans-serif" }}>CNAS</span>
    </div>
  );
}

/* Desktop sidebar (internal) */
function InternalSidebar({ active = "work-center", onChange, user = PEOPLE.sd }) {
  const groups = [
    { g: "仪表盘", items: [
      ["work-center", "工作中心", "◆"],
      ["my-tasks", "我的任务", "✓"],
    ]},
    { g: "业务", items: [
      ["submission", "送检受理", "✎"],
      ["sample-desk", "样品收样台", "⬚"],
      ["project", "项目管理", "▤"],
      ["worklist", "试验工单", "⚙"],
      ["report-review", "报告审核", "⎙"],
    ]},
    { g: "资源", items: [
      ["device-calendar", "设备预约", "⏱"],
      ["payment", "财务收款", "¥"],
      ["archive", "档案归档", "▣"],
    ]},
    { g: "管理", items: [
      ["audit", "审计日志", "≡"],
      ["system-config", "系统配置", "⚒"],
    ]},
  ];
  return (
    <aside style={{ background: "var(--primary-deep)", color: "#fff", width: 240, display: "flex", flexDirection: "column", flexShrink: 0 }}>
      <div style={{ padding: "16px 18px", borderBottom: "1px solid rgba(255,255,255,.08)", display: "flex", gap: 10, alignItems: "center" }}>
        <div style={{ width: 32, height: 32, borderRadius: 8, background: "linear-gradient(135deg,#1A5FB4,#00A8A8)", display: "flex", alignItems: "center", justifyContent: "center", fontFamily: "Inter,sans-serif", fontWeight: 700, fontSize: 12 }}>NJU</div>
        <div>
          <div style={{ fontSize: 12, fontWeight: 600, lineHeight: 1.3 }}>南京大学锡山</div>
          <div style={{ fontSize: 10, opacity: .55, fontFamily: "Inter,sans-serif", letterSpacing: ".05em" }}>LIMS · v2.3</div>
        </div>
      </div>
      <div style={{ flex: 1, overflowY: "auto", padding: "8px 0" }}>
        {groups.map((g) => (
          <Fragment key={g.g}>
            <div style={{ fontSize: 10, letterSpacing: ".16em", color: "rgba(255,255,255,.4)", padding: "12px 18px 4px", fontFamily: "Inter,sans-serif", textTransform: "uppercase" }}>{g.g}</div>
            {g.items.map((it) => {
              const a = it[0] === active;
              return (
                <div
                  key={it[0]}
                  onClick={() => onChange && onChange(it[0])}
                  style={{
                    display: "flex", alignItems: "center", gap: 10, padding: "8px 18px",
                    color: a ? "#fff" : "rgba(255,255,255,.7)", fontSize: 13, cursor: "pointer",
                    background: a ? "rgba(0,168,168,.18)" : undefined,
                    borderLeft: a ? "2px solid var(--accent)" : "2px solid transparent",
                    paddingLeft: a ? 16 : 18,
                  }}
                >
                  <span style={{ width: 16, textAlign: "center", opacity: .7, fontSize: 14 }}>{it[2]}</span>
                  <span>{it[1]}</span>
                </div>
              );
            })}
          </Fragment>
        ))}
      </div>
      <div style={{ padding: "12px 16px", borderTop: "1px solid rgba(255,255,255,.08)", display: "flex", gap: 10, alignItems: "center" }}>
        <Avatar name={user.av} color={user.color} size={32} />
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 13, fontWeight: 500 }}>{user.name}</div>
          <div style={{ fontSize: 11, opacity: .6 }}>{user.role}</div>
        </div>
        <span style={{ color: "rgba(255,255,255,.5)", cursor: "pointer", fontSize: 14 }}>⏏</span>
      </div>
    </aside>
  );
}

/* Top bar */
function TopBar({ crumbs = [], user = PEOPLE.sd, search = true }) {
  return (
    <div style={{ height: 56, background: "#fff", borderBottom: "1px solid var(--line)", display: "flex", alignItems: "center", padding: "0 24px", gap: 16, flexShrink: 0 }}>
      <div style={{ fontSize: 13, color: "var(--ink-600)" }}>
        {crumbs.map((c, i) => (
          <Fragment key={i}>
            {i > 0 && <span style={{ margin: "0 8px", color: "var(--ink-300)" }}>/</span>}
            <span style={{ color: i === crumbs.length - 1 ? "var(--ink-900)" : "var(--ink-600)", fontWeight: i === crumbs.length - 1 ? 500 : 400 }}>{c}</span>
          </Fragment>
        ))}
      </div>
      {search && (
        <div style={{ marginLeft: "auto", display: "flex", alignItems: "center", gap: 8, background: "#F1F5F9", borderRadius: 6, padding: "6px 12px", width: 300, fontSize: 13, color: "var(--ink-400)" }}>
          <span>🔍</span>
          <span>搜索项目 / 样品 / 客户...</span>
          <span style={{ marginLeft: "auto", fontFamily: "JetBrains Mono,monospace", fontSize: 11, padding: "1px 6px", background: "#fff", border: "1px solid var(--line)", borderRadius: 4, color: "var(--ink-600)" }}>⌘K</span>
        </div>
      )}
      {!search && <div style={{ flex: 1 }} />}
      <CredBadges />
      <div style={{ width: 1, height: 24, background: "var(--line)" }} />
      <span style={{ fontSize: 16, color: "var(--ink-400)", cursor: "pointer" }}>🔔</span>
      <div style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13 }}>
        <Avatar name={user.av} color={user.color} size={28} />
        <div style={{ lineHeight: 1.2 }}>
          <div style={{ fontWeight: 500 }}>{user.name}</div>
          <div style={{ fontSize: 11, color: "var(--ink-400)" }}>{user.role}</div>
        </div>
      </div>
    </div>
  );
}

/* Customer (portal) sidebar */
function PortalSidebar({ active = "dashboard", onChange }) {
  const items = [
    ["dashboard", "我的首页", "⌂"],
    ["submission-new", "新建送检单", "✎"],
    ["projects", "我的项目", "▤"],
    ["reports", "我的报告", "⎙"],
    ["contracts", "合同协议", "▣"],
    ["messages", "消息中心", "✉"],
    ["settings", "账户设置", "⚙"],
  ];
  return (
    <aside style={{ width: 240, background: "#fff", borderRight: "1px solid var(--line)", flexShrink: 0, display: "flex", flexDirection: "column" }}>
      <div style={{ padding: "18px 20px", borderBottom: "1px solid var(--line)", display: "flex", gap: 12, alignItems: "center" }}>
        <div style={{ width: 36, height: 36, borderRadius: 9, background: "linear-gradient(135deg,#1A5FB4,#00A8A8)", color: "#fff", display: "flex", alignItems: "center", justifyContent: "center", fontWeight: 700, fontSize: 12, fontFamily: "Inter,sans-serif" }}>NJU</div>
        <div>
          <div style={{ fontSize: 13, fontWeight: 600, color: "var(--ink-900)", lineHeight: 1.3 }}>NJU XISHAN LIMS</div>
          <div style={{ fontSize: 11, color: "var(--ink-600)" }}>客户服务平台</div>
        </div>
      </div>
      <div style={{ flex: 1, padding: "10px 8px" }}>
        {items.map((it) => {
          const a = it[0] === active;
          return (
            <div
              key={it[0]}
              onClick={() => onChange && onChange(it[0])}
              style={{
                display: "flex", alignItems: "center", gap: 12, padding: "9px 14px",
                color: a ? "var(--primary)" : "var(--ink-700)", fontSize: 13,
                background: a ? "var(--primary-50)" : undefined, borderRadius: 6, cursor: "pointer", fontWeight: a ? 600 : 400, marginBottom: 2,
              }}
            >
              <span style={{ width: 16, fontSize: 14 }}>{it[2]}</span>
              <span>{it[1]}</span>
            </div>
          );
        })}
      </div>
      <div style={{ padding: "14px 18px", borderTop: "1px solid var(--line)" }}>
        <div style={{ fontSize: 11, color: "var(--ink-400)", marginBottom: 6 }}>客户单位</div>
        <div style={{ fontSize: 12, color: "var(--ink-900)", fontWeight: 500, lineHeight: 1.4 }}>{ORG.zh}</div>
        <div style={{ fontSize: 10, color: "var(--ink-600)", fontFamily: "Inter,sans-serif", marginTop: 2 }}>{ORG.en}</div>
      </div>
    </aside>
  );
}

/* Portal top bar */
function PortalTopBar({ user = "李客户", role = "授权采购员" }) {
  return (
    <div style={{ height: 56, background: "#fff", borderBottom: "1px solid var(--line)", display: "flex", alignItems: "center", padding: "0 24px", gap: 16, flexShrink: 0 }}>
      <div style={{ fontSize: 13, color: "var(--ink-400)" }}>今天是 2026-05-13 · 您还有 <b style={{ color: "var(--warning)" }}>3 项</b> 待办</div>
      <div style={{ flex: 1 }} />
      <span style={{ fontSize: 13, color: "var(--primary)", cursor: "pointer" }}>帮助中心</span>
      <span style={{ fontSize: 16, color: "var(--ink-400)", cursor: "pointer" }}>🔔</span>
      <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
        <Avatar name={user.slice(0, 1)} color="#0E7A7A" size={28} />
        <div style={{ lineHeight: 1.2, fontSize: 13 }}>
          <div style={{ fontWeight: 500 }}>{user}</div>
          <div style={{ fontSize: 11, color: "var(--ink-400)" }}>{role}</div>
        </div>
      </div>
    </div>
  );
}

/* Mock document area for layouts */
function DesktopShell({ side, top, children }) {
  return (
    <div style={{ display: "flex", height: "100vh", background: "var(--bg)" }}>
      {side}
      <div style={{ flex: 1, display: "flex", flexDirection: "column", minWidth: 0 }}>
        {top}
        <div style={{ flex: 1, overflowY: "auto" }}>{children}</div>
      </div>
    </div>
  );
}

/* Echarts hook */
function useChart(buildOption, deps = []) {
  const ref = useRef(null);
  useEffect(() => {
    if (!ref.current || !window.echarts) return;
    const inst = echarts.init(ref.current);
    inst.setOption(buildOption());
    const onResize = () => inst.resize();
    window.addEventListener("resize", onResize);
    const ro = new ResizeObserver(() => inst.resize());
    ro.observe(ref.current);
    return () => {
      window.removeEventListener("resize", onResize);
      ro.disconnect();
      inst.dispose();
    };
  }, deps);
  return ref;
}

/* Export to window */
Object.assign(window, {
  ORG, SAMPLE, PROJECT, PEOPLE, STATE_MACHINE, CUSTOMER_PROJECTS, NOTICES, TODOS, BIO_TESTS, OTHER_TESTS,
  ToastProvider, useToast, Modal, Drawer, Tag, Avatar, Stat, StateMachineFlow, ProgressSteps,
  money, Field, CardRadio, Toggle, CredBadges, InternalSidebar, TopBar, PortalSidebar, PortalTopBar,
  DesktopShell, useChart,
});
