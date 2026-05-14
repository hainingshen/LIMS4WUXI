/* ====================================================================
   PORTAL · P3 Submission New (8 PARTs · core complex form)
   ==================================================================== */

const PARTS = [
  ["P1", "委托方", "Client"],
  ["P2", "报告要求", "Report"],
  ["P3", "样品处置", "Disposal"],
  ["P4", "样品信息", "Sample"],
  ["P5", "生物相容性附加", "Bio Extra"],
  ["P6", "GLP 声明", "GLP"],
  ["P7", "生物相容性矩阵", "Bio Matrix"],
  ["P8", "其他试验", "Others"],
];

function P3_SubmissionNew({ onNavigate }) {
  const toast = useToast();
  const [step, setStep] = useState(0);
  const [drawerOpen, setDrawerOpen] = useState(false);

  // shared form state
  const [form, setForm] = useState({
    clientZh: ORG.zh, clientEn: ORG.en, addrZh: ORG.addr, addrEn: ORG.addrEn,
    mfgZh: SAMPLE.manufacturer, mfgEn: SAMPLE.manufacturerEn,
    contact: "李客户", tel: "138-xxxx-1234", email: "li.purchase@example-medical.cn",
    reportEmail: "report@example-medical.cn", reportAddr: "同委托单位地址",
    glp: false, paper: true, planLang: "CHS", reportLang: "BOTH",
    qty: 12, qtyUnit: "片", disposal: "discard", disposalNote: "",
    sampleZh: SAMPLE.zh, sampleEn: SAMPLE.en, sterile: "EO_STERILE",
    cas: "—", model: SAMPLE.model, spec: "1 片/袋", lot: SAMPLE.lot,
    material: "羧甲基纤维素钠 / 聚氨酯薄膜复合", phase: "solid",
    color: "肉色半透明", density: "—", stability: "常温下稳定 24 个月",
    solubility: "不溶于水", storage: "常温干燥处保存", purpose: "用于浅表创面渗液吸收与保护",
    extra: "—",
    bioPre: "—", nonSterile: "无", cuttable: true, sitesZh: "完整皮肤", sitesEn: "Intact skin",
    workTemp: 25, heat50: true, contactDur: "A", extractBy: "surface", surface: 60, thickness: 0.8,
    bioNote: "—",
    glpDeclared: false, glpName: "", glpDate: "",
    bioMatrix: [true, false, false, false, false, false, false, false, false, false, false],
    bioMethod: ["MEM 浸提法", "", "", "", "", "", "", "", "", "", ""],
    otherMatrix: { "MICRO_STERILITY": true, "MICRO_BIOBURDEN": false, "MICRO_ENDOTOXIN": false, "PHY_DIMENSION": false, "PHY_ABSORPTION": true, "CHEM_PH": false, "CHEM_HEAVY": false },
  });

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const visibleSteps = PARTS.filter((_, i) => i !== 5 || form.glp);

  return (
    <DesktopShell side={<PortalSidebar active="submission-new" />} top={<PortalTopBar />}>
      <div style={{ padding: 24 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", marginBottom: 6 }}>
          <h1 style={{ margin: 0, fontSize: 22, fontWeight: 600 }}>新建送检单 <span style={{ fontSize: 13, color: "var(--ink-400)", fontFamily: "Inter,sans-serif", fontWeight: 400, marginLeft: 8 }}>Submission Form Ver.15</span></h1>
          <div style={{ display: "flex", gap: 12, alignItems: "center", fontSize: 12, color: "var(--ink-600)" }}>
            <span>自动保存已开启 · 上次保存 2 分钟前</span>
            <Tag tone="cyan">草稿 · SUB-2026-DRAFT-082</Tag>
          </div>
        </div>
        <p style={{ fontSize: 13, color: "var(--ink-600)", marginTop: 0, marginBottom: 18 }}>
          请按 8 个 PART 依次填写。带 <span style={{ color: "var(--danger)" }}>*</span> 为必填项；悬停 <span style={{ display: "inline-block", width: 14, height: 14, borderRadius: "50%", border: "1px solid var(--ink-400)", textAlign: "center", lineHeight: "12px", fontSize: 9, color: "var(--ink-400)" }}>i</span> 查看双语字段说明。
        </p>

        {/* Stepper */}
        <div className="card" style={{ padding: "18px 24px", marginBottom: 16 }}>
          <div style={{ display: "flex", alignItems: "center" }}>
            {visibleSteps.map((p, i) => {
              const idx = PARTS.indexOf(p);
              const done = i < step, cur = i === step;
              return (
                <Fragment key={p[0]}>
                  <div style={{ display: "flex", flexDirection: "column", alignItems: "center", flex: 1, cursor: "pointer" }} onClick={() => setStep(i)}>
                    <div style={{
                      width: 32, height: 32, borderRadius: "50%",
                      background: done ? "var(--primary)" : cur ? "var(--accent)" : "#fff",
                      border: done || cur ? "none" : "2px solid var(--line)",
                      color: done || cur ? "#fff" : "var(--ink-400)",
                      display: "flex", alignItems: "center", justifyContent: "center",
                      fontFamily: "Inter,sans-serif", fontSize: 13, fontWeight: 600,
                      boxShadow: cur ? "0 0 0 5px rgba(0,168,168,.18)" : undefined,
                    }}>{done ? "✓" : idx + 1}</div>
                    <div style={{ fontSize: 12, marginTop: 6, fontWeight: cur ? 600 : 500, color: done || cur ? "var(--ink-900)" : "var(--ink-600)" }}>PART {idx + 1} · {p[1]}</div>
                    <div style={{ fontSize: 10, color: "var(--ink-400)", fontFamily: "Inter,sans-serif" }}>{p[2]}</div>
                  </div>
                  {i < visibleSteps.length - 1 && <div style={{ height: 2, flex: 1, maxWidth: 40, background: done ? "var(--primary)" : "var(--line)", marginTop: -28 }} />}
                </Fragment>
              );
            })}
          </div>
        </div>

        {/* Body */}
        <div className="card" style={{ padding: 28, minHeight: 480 }}>
          {step === 0 && <PartClient form={form} set={set} />}
          {step === 1 && <PartReport form={form} set={set} />}
          {step === 2 && <PartDisposal form={form} set={set} />}
          {step === 3 && <PartSample form={form} set={set} />}
          {step === 4 && <PartBioExtra form={form} set={set} />}
          {form.glp && step === 5 && <PartGlp form={form} set={set} />}
          {((!form.glp && step === 5) || (form.glp && step === 6)) && <PartBioMatrix form={form} set={set} />}
          {((!form.glp && step === 6) || (form.glp && step === 7)) && <PartOthers form={form} set={set} />}
        </div>

        {/* Footer */}
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 16, padding: "14px 20px", background: "#fff", border: "1px solid var(--line)", borderRadius: 10, position: "sticky", bottom: 16, boxShadow: "0 -4px 20px rgba(15,23,42,.04)" }}>
          <div style={{ display: "flex", gap: 8 }}>
            <button className="btn btn-default" onClick={() => toast("草稿已保存到云端", "success")}>保存草稿</button>
            <button className="btn btn-ghost" onClick={() => setDrawerOpen(true)}>实时预览</button>
          </div>
          <div style={{ fontSize: 12, color: "var(--ink-600)" }}>
            进度 <b className="mono" style={{ color: "var(--primary)" }}>{step + 1}</b> / {visibleSteps.length} · 字段完成度 <b className="mono" style={{ color: "var(--success)" }}>78%</b>
          </div>
          <div style={{ display: "flex", gap: 8 }}>
            <button className="btn btn-default" disabled={step === 0} onClick={() => setStep(Math.max(0, step - 1))}>← 上一步</button>
            {step < visibleSteps.length - 1 ? (
              <button className="btn btn-primary" onClick={() => setStep(Math.min(visibleSteps.length - 1, step + 1))}>下一步 →</button>
            ) : (
              <button className="btn btn-primary" onClick={() => { toast("送检单已提交，等待业务审核", "success"); onNavigate && onNavigate("P4"); }}>提交送检单</button>
            )}
          </div>
        </div>
      </div>

      <Drawer open={drawerOpen} onClose={() => setDrawerOpen(false)} title="送检单实时预览" width={560}>
        <SubmissionPreview form={form} />
      </Drawer>
    </DesktopShell>
  );
}

/* ============ PART 1 Client ============ */
function PartClient({ form, set }) {
  return (
    <div>
      <PartHead n={1} zh="委托方信息" en="Client Information" desc="委托单位 / 制造商 / 联系方式 / 报告寄送" />
      <div className="f-row f-row-2">
        <Field label="委托单位（中文）" en="Client (CHS)" required hint="必须与营业执照完全一致；后续合同将以此命名">
          <input className="f-input" value={form.clientZh} onChange={(e) => set("clientZh", e.target.value)} />
        </Field>
        <Field label="委托单位（英文）" en="Client (ENG)" hint="如未注册英文名，可写拼音；英文报告将使用此名称">
          <input className="f-input" value={form.clientEn} onChange={(e) => set("clientEn", e.target.value)} />
        </Field>
      </div>
      <div className="f-row f-row-2">
        <Field label="委托单位地址（中文）" en="Client Address (CHS)" required>
          <input className="f-input" value={form.addrZh} onChange={(e) => set("addrZh", e.target.value)} />
        </Field>
        <Field label="委托单位地址（英文）" en="Client Address (ENG)">
          <input className="f-input" value={form.addrEn} onChange={(e) => set("addrEn", e.target.value)} />
        </Field>
      </div>
      <div className="f-row f-row-2">
        <Field label="制造商（中文）" en="Manufacturer (CHS)" required>
          <input className="f-input" value={form.mfgZh} onChange={(e) => set("mfgZh", e.target.value)} />
        </Field>
        <Field label="制造商（英文）" en="Manufacturer (ENG)">
          <input className="f-input" value={form.mfgEn} onChange={(e) => set("mfgEn", e.target.value)} />
        </Field>
      </div>
      <div className="f-row f-row-3">
        <Field label="联系人" en="Contact" required><input className="f-input" value={form.contact} onChange={(e) => set("contact", e.target.value)} /></Field>
        <Field label="联系电话" en="Phone" required><input className="f-input" value={form.tel} onChange={(e) => set("tel", e.target.value)} /></Field>
        <Field label="联系邮箱" en="Email" required><input className="f-input" value={form.email} onChange={(e) => set("email", e.target.value)} /></Field>
      </div>
      <div className="f-row f-row-2">
        <Field label="电子报告接收邮箱" en="Report Email" hint="可与联系邮箱不同；多个邮箱用分号分隔">
          <input className="f-input" value={form.reportEmail} onChange={(e) => set("reportEmail", e.target.value)} />
        </Field>
        <Field label="纸质报告 / 发票寄送地址" en="Mailing Address">
          <input className="f-input" value={form.reportAddr} onChange={(e) => set("reportAddr", e.target.value)} />
        </Field>
      </div>
    </div>
  );
}

/* ============ PART 2 Report ============ */
function PartReport({ form, set }) {
  return (
    <div>
      <PartHead n={2} zh="报告要求" en="Report Requirements" desc="GLP / 纸质件 / 报告语言 · 影响审批路径与签发流程" />
      <div className="f-row f-row-2">
        <Field label="是否要求 GLP" en="GLP Required" required hint="GLP 需触发 FDA 21 CFR 58 表征确认；将影响审批流程">
          <Toggle value={form.glp} onChange={(v) => set("glp", v)} labels={["是 · 触发 GLP 声明流程", "否 · 常规检测"]} />
        </Field>
        <Field label="是否要求纸质盖章件" en="Paper Copy Required" hint="勾选后将寄送一份原件纸质报告">
          <Toggle value={form.paper} onChange={(v) => set("paper", v)} labels={["是 · 寄送纸质件", "否 · 仅电子版"]} />
        </Field>
      </div>
      <Field label="计划书语言" en="Test Plan Language" required>
        <CardRadio value={form.planLang} onChange={(v) => set("planLang", v)} options={[
          { value: "CHS", label: "中文", en: "Chinese", icon: "中" },
          { value: "ENG", label: "英文", en: "English", icon: "EN" },
          { value: "BOTH", label: "中英双语", en: "Bilingual", icon: "中/EN" },
        ]} />
      </Field>
      <Field label="报告语言" en="Report Language" required>
        <CardRadio value={form.reportLang} onChange={(v) => set("reportLang", v)} options={[
          { value: "CHS", label: "中文", en: "Chinese", icon: "中" },
          { value: "ENG", label: "英文", en: "English", icon: "EN" },
          { value: "BOTH", label: "中英双语", en: "Bilingual", icon: "中/EN", desc: "推荐 · 用于注册申报" },
        ]} />
      </Field>
      {form.glp && (
        <div style={{ marginTop: 16, padding: 16, background: "var(--warning-50)", border: "1px solid #FDE68A", borderRadius: 8, fontSize: 13, color: "#92400E" }}>
          <b>⚠ GLP 提示</b> · 您选择了 GLP，将在 <b>PART 6</b> 添加 FDA 21 CFR 58 表征确认环节，审批流程将加入 QA 双签。
        </div>
      )}
    </div>
  );
}

/* ============ PART 3 Disposal ============ */
function PartDisposal({ form, set }) {
  return (
    <div>
      <PartHead n={3} zh="样品处置" en="Sample Disposal" desc="送检数量 / 处置方式 · 关系样品归还" />
      <div className="f-row f-row-2">
        <Field label="送检样品总数" en="Total Quantity" required hint="包含主样品和备用样品；试验需要的最低数量见检测员建议">
          <div style={{ display: "flex", gap: 8 }}>
            <input className="f-input" type="number" value={form.qty} onChange={(e) => set("qty", Number(e.target.value))} style={{ flex: 1 }} />
            <select className="f-select" value={form.qtyUnit} onChange={(e) => set("qtyUnit", e.target.value)} style={{ width: 100 }}>
              <option>片</option><option>支</option><option>盒</option><option>套</option><option>g</option><option>ml</option>
            </select>
          </div>
        </Field>
      </div>
      <Field label="样品处置方式" en="Sample Disposal Method" required>
        <CardRadio value={form.disposal} onChange={(v) => set("disposal", v)} options={[
          { value: "discard", label: "破坏丢弃", en: "Discard", desc: "试验后按医疗废物处置", icon: "🗑" },
          { value: "return", label: "回寄未使用样品", en: "Return Unused", desc: "运费由客户承担", icon: "↩" },
          { value: "other", label: "其他", en: "Other", desc: "请在下方说明", icon: "···" },
        ]} />
      </Field>
      {form.disposal === "other" && (
        <Field label="其他处置说明" en="Other Disposal Note" required>
          <textarea className="f-textarea" rows="3" value={form.disposalNote} onChange={(e) => set("disposalNote", e.target.value)} placeholder="请描述特殊处置方式..." />
        </Field>
      )}
    </div>
  );
}

/* ============ PART 4 Sample ============ */
function PartSample({ form, set }) {
  return (
    <div>
      <PartHead n={4} zh="样品基本信息" en="Sample Basic Information" desc="必须真实完整 · 部分字段提交后硬锁定" />
      <div className="f-row f-row-2">
        <Field label="样品名称（中文）" en="Sample Name (CHS)" required>
          <input className="f-input" value={form.sampleZh} onChange={(e) => set("sampleZh", e.target.value)} />
        </Field>
        <Field label="样品名称（英文）" en="Sample Name (ENG)" required>
          <input className="f-input" value={form.sampleEn} onChange={(e) => set("sampleEn", e.target.value)} />
        </Field>
      </div>
      <div className="f-row f-row-3">
        <Field label="灭菌状态" en="Sterilization" required>
          <select className="f-select" value={form.sterile} onChange={(e) => set("sterile", e.target.value)}>
            <option value="EO_STERILE">环氧乙烷灭菌 EO</option>
            <option value="GAMMA_STERILE">辐照灭菌 Gamma</option>
            <option value="STEAM_STERILE">蒸汽灭菌 Steam</option>
            <option value="NON_STERILE">非灭菌</option>
            <option value="UNKNOWN">未知 / 不适用</option>
          </select>
        </Field>
        <Field label="CAS 编号" en="CAS Number"><input className="f-input" value={form.cas} onChange={(e) => set("cas", e.target.value)} /></Field>
        <Field label="型号" en="Model" required><input className="f-input" value={form.model} onChange={(e) => set("model", e.target.value)} /></Field>
      </div>
      <div className="f-row f-row-3">
        <Field label="规格" en="Specification" required><input className="f-input" value={form.spec} onChange={(e) => set("spec", e.target.value)} /></Field>
        <Field label="批号" en="Lot No." required hint="提交后将硬锁定。如需修改请联系业务签补充协议">
          <input className="f-input mono" value={form.lot} onChange={(e) => set("lot", e.target.value)} />
        </Field>
        <Field label="物理状态" en="Physical State" required>
          <select className="f-select" value={form.phase} onChange={(e) => set("phase", e.target.value)}>
            <option value="solid">固态</option><option value="liquid">液态</option><option value="powder">粉末</option><option value="other">其他</option>
          </select>
        </Field>
      </div>
      <div className="f-row f-row-2">
        <Field label="材料组成" en="Material Composition" required>
          <textarea className="f-textarea" rows="2" value={form.material} onChange={(e) => set("material", e.target.value)} />
        </Field>
        <Field label="预期用途" en="Intended Use" required>
          <textarea className="f-textarea" rows="2" value={form.purpose} onChange={(e) => set("purpose", e.target.value)} />
        </Field>
      </div>
      <div className="f-row f-row-3">
        <Field label="颜色" en="Color"><input className="f-input" value={form.color} onChange={(e) => set("color", e.target.value)} /></Field>
        <Field label="稳定性" en="Stability"><input className="f-input" value={form.stability} onChange={(e) => set("stability", e.target.value)} /></Field>
        <Field label="保存条件" en="Storage" required><input className="f-input" value={form.storage} onChange={(e) => set("storage", e.target.value)} /></Field>
      </div>
    </div>
  );
}

/* ============ PART 5 Bio Extra ============ */
function PartBioExtra({ form, set }) {
  return (
    <div>
      <PartHead n={5} zh="生物相容性附加信息" en="Biocompatibility Additional Info" desc="ISO 10993 系列检测前置参数" />
      <div className="f-row f-row-2">
        <Field label="检测部位（中文）" en="Test Site (CHS)" required hint="如：完整皮肤 / 黏膜 / 血液 / 骨"><input className="f-input" value={form.sitesZh} onChange={(e) => set("sitesZh", e.target.value)} /></Field>
        <Field label="检测部位（英文）" en="Test Site (ENG)"><input className="f-input" value={form.sitesEn} onChange={(e) => set("sitesEn", e.target.value)} /></Field>
      </div>
      <div className="f-row f-row-3">
        <Field label="工作温度 °C" en="Working Temp" hint="实际使用时的环境温度">
          <input className="f-input mono" type="number" value={form.workTemp} onChange={(e) => set("workTemp", Number(e.target.value))} />
        </Field>
        <Field label="是否耐 50°C" en="Heat 50°C" hint="部分浸提试验需在 50°C 条件下进行">
          <Toggle value={form.heat50} onChange={(v) => set("heat50", v)} labels={["是 · 可加热浸提", "否 · 仅常温"]} />
        </Field>
        <Field label="是否可切割" en="Cuttable" hint="不能切割将影响样品制备">
          <Toggle value={form.cuttable} onChange={(v) => set("cuttable", v)} labels={["可切割", "不可切割"]} />
        </Field>
      </div>
      <Field label="累积接触时长" en="Cumulative Contact Duration" required hint="按 ISO 10993-1：A≤24h，B 24h~30d，C >30d">
        <CardRadio value={form.contactDur} onChange={(v) => set("contactDur", v)} options={[
          { value: "A", label: "A · 短期", en: "≤ 24 h", desc: "" },
          { value: "B", label: "B · 长期", en: "24 h ~ 30 d", desc: "" },
          { value: "C", label: "C · 持久", en: "> 30 d", desc: "" },
        ]} />
      </Field>
      <Field label="浸提依据" en="Extraction Basis" required>
        <CardRadio value={form.extractBy} onChange={(v) => set("extractBy", v)} options={[
          { value: "surface", label: "按表面积", en: "Surface Area", desc: "cm² · ISO 10993-12" },
          { value: "weight", label: "按重量", en: "Weight", desc: "g · 适用粉末/液体" },
          { value: "na", label: "N/A", en: "Not Applicable", desc: "经评审豁免" },
        ]} />
      </Field>
      <div className="f-row f-row-2">
        <Field label="表面积 (cm²)" en="Surface Area"><input className="f-input mono" type="number" value={form.surface} onChange={(e) => set("surface", Number(e.target.value))} /></Field>
        <Field label="最薄厚度 (mm)" en="Min Thickness"><input className="f-input mono" type="number" step="0.1" value={form.thickness} onChange={(e) => set("thickness", Number(e.target.value))} /></Field>
      </div>
    </div>
  );
}

/* ============ PART 6 GLP ============ */
function PartGlp({ form, set }) {
  return (
    <div>
      <PartHead n={6} zh="GLP 声明" en="GLP Declaration" desc="FDA 21 CFR 58 表征确认" />
      <div style={{ padding: 18, background: "var(--warning-50)", border: "1px solid #FDE68A", borderRadius: 8, marginBottom: 20, fontSize: 13, color: "#92400E", lineHeight: 1.7 }}>
        <b>提示：</b> 您在 PART 2 选择了 GLP 检测。请阅读以下声明并由具备资质的负责人签字确认。该确认将进入归档原始记录。
      </div>
      <div style={{ padding: 24, border: "1px solid var(--line)", borderRadius: 8, background: "#FCFDFE", marginBottom: 20, fontSize: 13, lineHeight: 1.9, color: "var(--ink-700)" }}>
        <div style={{ fontWeight: 600, color: "var(--ink-900)", marginBottom: 10, fontSize: 14 }}>FDA 21 CFR 58 表征确认书</div>
        本人代表 <b>{form.clientZh}</b>，确认下列事项：<br />
        ① 委托的样品 <b>{form.sampleZh}</b> 批号 <b className="mono">{form.lot}</b> 经过完整鉴定/表征，与本送检单所述材料组成一致。<br />
        ② 表征数据完整、可追溯，符合 FDA 21 CFR 58 关于 GLP 试验对受试物表征的要求。<br />
        ③ 本人理解：若表征数据不充分，将导致试验数据不能用于 FDA 申报。<br />
        ④ 同意试验所产生的原始记录、报告进入实验室档案，并可被 FDA / NMPA 审查。
      </div>
      <Field label="确认人签字" en="Declared By" required>
        <input className="f-input" placeholder="请输入完整姓名" value={form.glpName} onChange={(e) => set("glpName", e.target.value)} />
      </Field>
      <div style={{ display: "flex", alignItems: "center", gap: 12, marginTop: 8 }}>
        <input type="checkbox" id="glp-ack" checked={form.glpDeclared} onChange={(e) => set("glpDeclared", e.target.checked)} />
        <label htmlFor="glp-ack" style={{ fontSize: 13, color: "var(--ink-700)", cursor: "pointer" }}>本人已认真阅读上述声明，并同意以上全部内容</label>
      </div>
    </div>
  );
}

/* ============ PART 7 Bio Matrix ============ */
function PartBioMatrix({ form, set }) {
  const toggle = (i) => {
    const arr = [...form.bioMatrix]; arr[i] = !arr[i]; set("bioMatrix", arr);
  };
  return (
    <div>
      <PartHead n={7} zh="生物相容性试验矩阵" en="Biocompatibility Test Matrix" desc="ISO 10993 系列 · 11 个常用试验项" />
      <div style={{ overflow: "auto", border: "1px solid var(--line)", borderRadius: 8 }}>
        <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13 }}>
          <thead>
            <tr style={{ background: "var(--bg)", color: "var(--ink-600)", fontSize: 12 }}>
              <th style={{ padding: 10, width: 40, textAlign: "center", borderBottom: "1px solid var(--line)" }}>选</th>
              <th style={{ padding: 10, textAlign: "left", borderBottom: "1px solid var(--line)" }}>试验项</th>
              <th style={{ padding: 10, textAlign: "left", borderBottom: "1px solid var(--line)" }}>检测方法</th>
              <th style={{ padding: 10, textAlign: "left", borderBottom: "1px solid var(--line)" }}>浸提介质</th>
              <th style={{ padding: 10, textAlign: "left", borderBottom: "1px solid var(--line)" }}>参照标准</th>
              <th style={{ padding: 10, textAlign: "left", borderBottom: "1px solid var(--line)" }}>备注</th>
            </tr>
          </thead>
          <tbody>
            {BIO_TESTS.map((row, i) => {
              const sel = form.bioMatrix[i];
              return (
                <tr key={row[0]} style={{ background: sel ? "var(--primary-50)" : "#fff", borderBottom: "1px solid var(--line)" }}>
                  <td style={{ padding: 10, textAlign: "center" }}><input type="checkbox" checked={sel} onChange={() => toggle(i)} /></td>
                  <td style={{ padding: 10 }}>
                    <div style={{ fontWeight: 500 }}>{row[0]}</div>
                    <div style={{ fontSize: 11, color: "var(--ink-600)", fontFamily: "Inter,sans-serif" }}>{row[1]}</div>
                  </td>
                  <td style={{ padding: 10 }}>
                    {sel ? (
                      <select className="f-select" style={{ padding: "4px 8px", fontSize: 12 }} defaultValue={row[2].split(" / ")[0]}>
                        {row[2].split(" / ").map((m) => <option key={m}>{m}</option>)}
                      </select>
                    ) : <span style={{ color: "var(--ink-400)", fontSize: 12 }}>—</span>}
                  </td>
                  <td style={{ padding: 10 }}>
                    {sel ? <select className="f-select" style={{ padding: "4px 8px", fontSize: 12 }}><option>极性 (氯化钠注射液)</option><option>非极性 (棉籽油)</option><option>培养基</option></select> : <span style={{ color: "var(--ink-400)", fontSize: 12 }}>—</span>}
                  </td>
                  <td style={{ padding: 10, fontSize: 11, color: "var(--ink-600)" }}>
                    {sel ? <span className="mono">ISO 10993-{i + 3}</span> : <span style={{ color: "var(--ink-400)" }}>—</span>}
                  </td>
                  <td style={{ padding: 10 }}>
                    {sel ? <input className="f-input" style={{ padding: "4px 8px", fontSize: 12 }} placeholder="可选" /> : <span style={{ color: "var(--ink-400)", fontSize: 12 }}>—</span>}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <div style={{ marginTop: 12, padding: 12, background: "var(--accent-50)", borderRadius: 6, fontSize: 12, color: "var(--accent-700)" }}>
        共勾选 <b>{form.bioMatrix.filter(Boolean).length}</b> 项 · 预估检测周期 <b>14 ~ 28 个工作日</b> · 预估金额 <b className="mono">{money(form.bioMatrix.filter(Boolean).length * 8500)}</b>（含税）
      </div>
    </div>
  );
}

/* ============ PART 8 Others ============ */
function PartOthers({ form, set }) {
  const toggleKey = (k) => set("otherMatrix", { ...form.otherMatrix, [k]: !form.otherMatrix[k] });
  return (
    <div>
      <PartHead n={8} zh="其他试验" en="Other Tests" desc="微生物 / 包装 / 物理 / 化学" />
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
        {OTHER_TESTS.map(([groupName, items]) => (
          <div key={groupName} className="card" style={{ padding: 14 }}>
            <h4 style={{ margin: "0 0 10px", fontSize: 13, fontWeight: 600, color: "var(--primary)" }}>{groupName}</h4>
            <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
              {items.map(([code, zh, methods]) => {
                const sel = form.otherMatrix[code];
                return (
                  <div key={code} style={{ padding: 10, border: "1px solid " + (sel ? "var(--primary)" : "var(--line)"), borderRadius: 6, background: sel ? "var(--primary-50)" : "#fff", cursor: "pointer" }} onClick={() => toggleKey(code)}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <div>
                        <input type="checkbox" checked={!!sel} readOnly style={{ marginRight: 8 }} />
                        <span style={{ fontWeight: 500, fontSize: 13 }}>{zh}</span>
                        <span className="mono" style={{ marginLeft: 8, fontSize: 10, color: "var(--ink-400)" }}>{code}</span>
                      </div>
                      {sel && <Tag tone="cyan">已勾选</Tag>}
                    </div>
                    <div style={{ fontSize: 11, color: "var(--ink-600)", marginTop: 4 }}>方法：{methods}</div>
                  </div>
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function PartHead({ n, zh, en, desc }) {
  return (
    <div style={{ marginBottom: 24 }}>
      <div style={{ display: "flex", alignItems: "baseline", gap: 12 }}>
        <span style={{ fontSize: 11, color: "var(--accent)", fontFamily: "Inter,sans-serif", fontWeight: 600, letterSpacing: ".18em" }}>PART {n}</span>
        <h2 style={{ margin: 0, fontSize: 20, fontWeight: 600 }}>{zh}</h2>
        <span style={{ fontSize: 13, color: "var(--ink-400)", fontFamily: "Inter,sans-serif" }}>{en}</span>
      </div>
      <div style={{ fontSize: 12, color: "var(--ink-600)", marginTop: 4 }}>{desc}</div>
      <div style={{ height: 1, background: "var(--line)", marginTop: 16 }} />
    </div>
  );
}

function SubmissionPreview({ form }) {
  const Row = ({ k, v }) => (
    <div style={{ display: "flex", padding: "8px 0", borderBottom: "1px dashed var(--line)", fontSize: 13 }}>
      <span style={{ width: 140, color: "var(--ink-600)", flexShrink: 0 }}>{k}</span>
      <span style={{ flex: 1, color: "var(--ink-900)" }}>{v}</span>
    </div>
  );
  const Sec = ({ t, children }) => (
    <div style={{ marginBottom: 20 }}>
      <div style={{ fontSize: 12, fontWeight: 600, color: "var(--primary)", letterSpacing: ".05em", marginBottom: 8, paddingBottom: 6, borderBottom: "2px solid var(--primary)" }}>{t}</div>
      {children}
    </div>
  );
  const bioItems = BIO_TESTS.filter((_, i) => form.bioMatrix[i]).map((b) => b[0]).join("、");
  const otherItems = Object.entries(form.otherMatrix).filter(([_, v]) => v).map(([k]) => k).join(", ");
  return (
    <div>
      <Sec t="PART 1 委托方">
        <Row k="委托单位" v={form.clientZh} />
        <Row k="联系人 · 电话" v={`${form.contact} · ${form.tel}`} />
        <Row k="邮箱" v={form.email} />
      </Sec>
      <Sec t="PART 2 报告要求">
        <Row k="GLP" v={form.glp ? "✓ 是" : "✗ 否"} />
        <Row k="纸质件" v={form.paper ? "✓ 是" : "✗ 否"} />
        <Row k="计划书 / 报告语言" v={`${form.planLang} / ${form.reportLang}`} />
      </Sec>
      <Sec t="PART 4 样品">
        <Row k="样品名称" v={`${form.sampleZh} (${form.sampleEn})`} />
        <Row k="型号 · 批号" v={`${form.model} · ${form.lot}`} />
        <Row k="灭菌状态" v={form.sterile} />
        <Row k="数量" v={`${form.qty} ${form.qtyUnit}`} />
      </Sec>
      <Sec t="PART 7 生物相容性试验">
        <Row k="勾选项" v={bioItems || "—"} />
      </Sec>
      <Sec t="PART 8 其他试验">
        <Row k="勾选项" v={otherItems || "—"} />
      </Sec>
      <div style={{ padding: 14, background: "var(--accent-50)", borderRadius: 8, fontSize: 13, color: "var(--accent-700)", marginTop: 16 }}>
        预估金额 <b className="mono" style={{ fontSize: 16 }}>{money(form.bioMatrix.filter(Boolean).length * 8500 + Object.values(form.otherMatrix).filter(Boolean).length * 3200)}</b>（含税 · 最终以合同为准）
      </div>
    </div>
  );
}

window.P3_SubmissionNew = P3_SubmissionNew;
