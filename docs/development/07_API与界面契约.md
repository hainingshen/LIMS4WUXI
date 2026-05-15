# API 与界面契约

## 1. 通用接口规范

接口前缀：`/api/v1`

JSON 字段：统一 `camelCase`

机读接口骨架以 `specs/openapi.yaml` 为准。前后端 DTO、TypeScript 类型和接口 mock 均应优先由 OpenAPI 生成，不再从 Markdown 散文中手工推断字段。

本文件用于描述一期产品和页面契约。当前新增资源已同步到 `specs/openapi.yaml`；后续再新增资源时，仍需先同步机读契约，再进入开发。

分页参数：

```http
GET /api/v1/projects?page=1&pageSize=20&sort=createdAt,desc
```

分页响应：

```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 0
}
```

错误响应：

```json
{
  "code": "PAYMENT_GATE_LOCKED",
  "message": "未到账，禁止下载正式报告",
  "traceId": "018f...",
  "details": {}
}
```

## 2. 主要资源路径

| 资源 | 路径 | 说明 |
| --- | --- | --- |
| 内部登录 | `POST /auth/internal/login` | 内部端登录，token 固化 `clientType=INTERNAL` |
| 客户门户登录 | `POST /auth/portal/login` | 客户门户登录，token 固化 `clientType=PORTAL` |
| 刷新令牌 | `POST /auth/refresh` | 刷新 token |
| 登出 | `POST /auth/logout` | 登出并写入 token 黑名单 |
| 当前用户 | `GET /auth/me` | 用户、角色、数据范围 |
| 客户单位 | `/customer-orgs` | 委托单位 |
| 客户账号 | `/portal-users` | 客户门户用户 |
| 送检单 | `/submissions` | 客户提交和内部受理 |
| 项目 | `/projects` | 项目主对象 |
| 非标评审 | `/projects/{id}/nonstandard-review` | 报价前评审 |
| 报价 | `/quotations` | 报价单 |
| 合同 | `/contracts` | 合同、协议、补充协议 |
| 统一变更申请 | `/change-requests` | 协议、样品、项目、报告等关键字段变更 |
| 样品 | `/samples` | 样品接收、入库、处置 |
| 留样 | `/sample-retentions` | 留样登记、到期预警、归还、销毁 |
| 方案 | `/study-plans` | 方案和审批 |
| 工单 | `/work-orders` | 试验执行 |
| 原始记录 | `/raw-records` | 原始记录和审核 |
| 报告 | `/reports` | 报告生成、审核、发放 |
| 报告模板 | `/report-templates` | 协议、流转单、报告模板及版本 |
| 报告注册中心 | `/report-registry` | 已生成、签发、发放报告索引 |
| 报告发放日志 | `/report-delivery-logs` | 预览、下载、邮件、绿色通道发放留痕 |
| 收款 | `/payment-receipts` | 财务登记到账 |
| 绿色通道 | `/green-channel-requests` | 未到账先发电子报告 |
| 设备 | `/devices` | 设备台账 |
| 设备预约 | `/device-bookings` | 半小时预约 |
| 设备文件 | `/device-files` | 说明书、校准证书、维护记录、SOP 等附件 |
| 库存批次 | `/inventory-batches` | 标准品和耗材 |
| 库存流水 | `/inventory-transactions` | 出入库、领用、报废 |
| 工作中心 | `/work-center` | 看板、健康度、阻塞和趋势 |
| 档案 | `/archive-items` | 项目档案 |
| 字典 | `/dictionaries` | 数据字典 |
| 组织架构 | `/organization-units` | 部门、岗位、人员归属 |
| 内部用户 | `/users` | 内部账号和岗位绑定 |
| 角色 | `/roles` | 角色和菜单授权 |
| 节假日 | `/holidays` | SLA 和设备预约日历排除日 |
| 导入导出任务 | `/import-export-tasks` | 批量导入、迁移导入、列表导出、报告批量下载 |
| 旧状态映射 | `/legacy-status-mappings` | 迁移期旧状态到新状态映射 |

## 3. 关键动作接口

| 动作 | 方法与路径 | 权限 |
| --- | --- | --- |
| 提交送检单 | `POST /submissions/{id}/submit` | 客户申请人 |
| 内部受理 | `POST /submissions/{id}/accept` | 业务员 |
| 拆分项目 | `POST /submissions/{id}/split-projects` | 业务员 |
| 发起非标评审 | `POST /projects/{id}/nonstandard-review/start` | 业务员 |
| 非标审批 | `POST /projects/{projectId}/nonstandard-review/approve` | 技术负责人、业务负责人 |
| 生成报价 | `POST /projects/{id}/quotations` | 业务员 |
| 合同签署确认 | `POST /contracts/{id}/mark-signed` | 业务员、客户 |
| 提交统一变更 | `POST /change-requests/{id}/submit` | 申请人 |
| 审批统一变更 | `POST /change-requests/{id}/approve` | 目标对象对应审批人 |
| 变更生效 | `POST /change-requests/{id}/effect` | 系统或授权管理员 |
| 查询合同协议 | `GET /contracts` | 客户、内部授权人员；门户仅返回授权范围内协议 |
| 样品到达登记 | `POST /samples/{id}/receive` | 样品管理员 |
| 样品入库 | `POST /samples/{id}/stock-in` | 样品管理员 |
| 登记留样 | `POST /sample-retentions/{id}/register` | 样品管理员 |
| 留样处置 | `POST /sample-retentions/{id}/dispose` | 样品管理员、质量管理员 |
| 方案提交审批 | `POST /study-plans/{id}/submit-review` | 技术负责人或 SD |
| 试验下发 | `POST /study-plans/{id}/dispatch` | SD |
| 工单领样 | `POST /work-orders/{id}/pick-sample` | 检测员 |
| 工单开始 | `POST /work-orders/{id}/start` | 检测员 |
| 工单结果录入完成 | `POST /work-orders/{id}/enter-result` | 检测员 |
| 提交原始记录 | `POST /work-orders/{id}/submit-raw-record` | 检测员 |
| 原始记录审核 | `POST /raw-records/{id}/review` | 审核人 |
| 查询报告列表 | `GET /reports` | 客户、内部授权人员；门户仅返回授权范围内报告 |
| 生成报告草稿 | `POST /reports/generate-draft` | 报告编制员 |
| 报告提交 QC | `POST /reports/{id}/submit-qc` | 报告编制员 |
| 报告审批 | `POST /reports/{reportId}/review` | QC、QA、SD |
| 报告签发 | `POST /reports/{id}/issue` | 授权签字人 |
| 下载正式报告 | `GET /reports/{id}/official-pdf` | 客户、内部授权人员 |
| 发布报告模板 | `POST /report-templates/{id}/publish` | 质量管理员、系统管理员 |
| 报告批量下载 | `POST /report-registry/batch-download` | 档案管理员、质量管理员 |
| 登记到账 | `POST /payment-receipts` | 财务人员 |
| 发起绿色通道 | `POST /green-channel-requests` | 业务员 |
| 绿色通道审批 | `POST /green-channel-requests/{id}/approve` | 部门负责人、管理层 |
| 覆盖外部设备预约 | `POST /device-bookings/{id}/override` | 设备管理员、技术负责人 |
| 上传设备文件 | `POST /devices/{id}/files` | 设备管理员 |
| 作废设备文件 | `POST /device-files/{id}/disable` | 设备管理员、质量管理员 |
| 创建导入导出任务 | `POST /import-export-tasks` | 授权内部人员 |
| 导入预校验 | `POST /import-export-tasks/{id}/validate` | 授权内部人员 |
| 确认导入 | `POST /import-export-tasks/{id}/run` | 系统管理员、质量管理员 |

审批接口不得向前端暴露内部任务实现细节。前端只提交业务实体 ID、审批阶段、结论和意见；后端根据业务 ID、stage 和当前用户写入 `approval_record`，并由领域服务推进业务状态。

审批请求体统一结构：

```json
{
  "stage": "QC",
  "outcome": "APPROVED",
  "comment": "同意"
}
```

## 4. 页面契约

### 4.1 客户门户首页

功能：

- 新建送检单。
- 查看本人或本单位项目。
- 查看项目进度。
- 查看报告草稿。
- 付款后下载正式报告。
- 确认或签署协议。

筛选字段：

- 项目编号。
- 样品名称。
- 项目状态。
- 报告状态。
- 提交日期。

### 4.2 送检单表单页

表单字段以 `08_送检单Ver15字段映射.md` 和 `specs/submission_ver15_fields.csv` 为准。

实现要求：

- 中英文双字段不得合并。
- 必填项前端校验和后端校验一致。
- 字典项从 `/dictionaries` 获取。
- 合同或方案签署后锁定送检单核心字段。

### 4.3 内部项目工作中心

区域：

- 项目健康度。
- 当前卡点。
- 我的待办。
- 人员负荷。
- 趋势图。
- 超期和预警列表。

字段和算法以 `10_工作中心详细设计.md` 为准。

### 4.4 样品收样台

功能：

- 扫索送检单或项目。
- 查看客户填报信息。
- 登记样品到达。
- 核验名称、型号、批号、数量、包装、储存条件。
- 上传样品照片。
- 记录异常。
- 打印标签。
- 预入库和正式入库。

异常时必须回到受理或异常处理流程，不允许直接进入试验下发。

### 4.5 方案与试验计划页

功能：

- 查看项目、样品、检测项目。
- 配置是否置样、是否 GLP、是否动物试验。
- 生成试验计划。
- 提交 SD、QA、伦理审批。
- 下发工单。

下发按钮后端必须校验样品、合同、审批和置样状态。

### 4.6 工单执行页

功能：

- 领取任务。
- 领样登记。
- 记录开始、暂停、继续、完成。
- 录入结果。
- 上传或填写原始记录。
- 更新进度百分比。
- 记录阻塞问题。

### 4.7 报告审核页

功能：

- 查看草稿报告。
- 查看原始记录和字段映射。
- QC、QA、SD 审批。
- 驳回并填写意见。
- 签发正式报告。
- 查看付款门禁。

客户草稿预览：

- 只允许在线预览。
- 必须带草稿水印。
- 不展示电子签章。
- 不允许下载正式 PDF。

### 4.8 设备预约日历

功能：

- 按设备查看日历。
- 半小时粒度预约。
- 检测员预约必须关联工单号和检测项目。
- 外部用户预约只填用途和时间。
- 内部覆盖外部预约时弹窗填写原因。

默认通知：

- 站内消息。
- 邮件。
- 被覆盖预约状态改为 `NEEDS_RESCHEDULE`。

### 4.9 库存管理页

功能：

- 标准品和耗材台账。
- 基础入库。
- 领用。
- 退回。
- 报废。
- 有效期预警。
- 低库存预警。

一期不出现采购、供应商填报、扫码出入库入口。

### 4.10 统一变更中心

用于承接现有系统中协议变更申请、样品变更申请，以及新系统中项目、报告关键字段变更。旧系统的分散变更菜单不在新系统中照搬为多个孤立模块，而是在统一变更中心中通过 `targetType`、`changeType` 和筛选视图区分。

功能：

- 按协议、样品、项目、报告筛选变更申请。
- 展示字段级变更前后对比。
- 根据目标对象状态识别软锁、审批后生效、硬锁三类策略。
- 对已签发报告、已归档项目等硬锁对象，要求走重新签发或补充协议，不允许直接覆盖历史数据。
- 审批通过后由领域服务生效，写入审计日志和归档记录。

### 4.11 留样管理页

功能：

- 从样品详情或项目详情登记留样。
- 记录留样数量、单位、位置、保存条件、留样人和留样期限。
- 到期前预警，到期后进入待处置。
- 支持退还、销毁、转长期档案。
- 留样处置受付款门禁和项目状态约束。

### 4.12 报告注册中心

对应现有系统“报告数据中心”的业务价值，但不作为一个独立的报告副本库。新系统以 `report`、`report_version`、`report_delivery_log` 和 `archive_item` 组合形成报告注册中心。

功能：

- 按项目、客户、报告编号、模板、签发日期、发放状态检索报告。
- `GET /report-registry/search` 的筛选参数必须至少覆盖 `keyword`、`customerOrgId`、`projectId`、`reportBizNo`、`templateType`、`status`、`issuedFrom`、`issuedTo`。
- 查看当前有效版本和历史版本，`ReportRegistryItem.versions` 返回当前报告的版本列表。
- 查看预览、下载、邮件、绿色通道等发放日志。
- 支持授权批量下载，批量包通过导出任务生成。
- 已签发报告的内容变更必须走重新签发流程。

### 4.13 报告模板管理页

功能：

- 管理合同导出模板、流转单导出模板、中文报告、英文报告和专项报告模板。
- 每个模板有版本、状态、自动化等级和适用范围。
- `A` 级模板可结构化自动生成，`B` 级模板半自动生成后人工补录，`C` 级模板仅作为受控附件和人工编辑模板。
- 模板发布后旧版本自动废止，但历史报告继续关联原模板版本。

### 4.14 设备文件页

设备文件不单独做孤立文件柜，而是嵌入设备详情和设备台账。

功能：

- 上传说明书、校准证书、检定证书、维护记录、SOP、培训记录等文件。
- 标记当前有效文件和有效期。
- 证书到期进入工作中心预警。
- 工单选择设备时可查看必要文件状态，但普通检测员不修改设备文件。

### 4.15 主数据管理页

对应现有系统组织架构、角色、用户、系统字典、节假日等功能。一期保留业务运行必需的管理能力，但不把系统管理员做成业务审批人。

功能：

- 维护组织架构、内部用户、角色绑定。
- 维护系统字典和业务配置。
- 维护节假日，用于 SLA、预约日历和工作中心计算。
- 所有主数据变更写审计日志，关键字典发布后不得物理删除。

### 4.16 导入导出任务页

功能：

- 创建迁移导入、主数据导入、列表导出、报告批量下载、审计导出任务。
- 导入先预校验，展示错误清单，不允许部分静默失败。
- 大批量导出异步生成结果文件，设置过期时间。
- 导入导出任务记录操作者、参数、文件哈希和结果摘要。

## 5. 权限边界

客户门户：

- 客户单位管理员可看本单位全部项目。
- 普通申请人默认只看本人提交项目。
- 只读用户只看被授权项目。
- 代理商必须通过代理关系授权，不能默认跨单位访问。

内部端：

- 业务员可看自己客户和项目。
- 业务负责人可看所辖业务项目。
- 技术负责人可看技术评审和技术项目。
- SD、QA、QC 只在待审和授权范围内审批。
- 财务人员只登记和查看收款，不修改报告内容。
- 样品管理员可登记留样和执行受控处置，但不能绕过付款门禁。
- 设备管理员维护设备台账和设备文件；检测员只能查看与工单相关的设备文件状态。
- 报告模板发布应由质量管理员或系统管理员执行，报告内容审批仍由 QC、QA、SD 和授权签字人完成。
- 导入导出必须按任务授权，普通列表导出不得替代迁移导入或审计导出权限。
- 系统管理员不默认拥有业务审批权。

## 6. 前后端字段冻结原则

新增字段流程：

1. 先改 Markdown 或 CSV 字段规约。
2. 再改数据库迁移。
3. 再改后端 DTO 和接口。
4. 最后改前端表单和页面。

不得由前端或后端单方临时新增字段。

## 7. 前端路由清单

内部端和客户门户必须按 `clientType` 分流。`PORTAL` token 不得进入内部端路由，`INTERNAL` token 不得进入客户门户路由。

| routeName | path | parentLayout | requiredRoles |
| --- | --- | --- | --- |
| `PortalLogin` | `/portal/login` | `BlankLayout` | none |
| `PortalDashboard` | `/portal` | `PortalLayout` | `CUSTOMER_ORG_ADMIN,CUSTOMER_APPLICANT,CUSTOMER_VIEWER` |
| `PortalSubmissionNew` | `/portal/submissions/new` | `PortalLayout` | `CUSTOMER_ORG_ADMIN,CUSTOMER_APPLICANT` |
| `PortalSubmissionDetail` | `/portal/submissions/:id` | `PortalLayout` | `CUSTOMER_ORG_ADMIN,CUSTOMER_APPLICANT,CUSTOMER_VIEWER` |
| `PortalProjectDetail` | `/portal/projects/:id` | `PortalLayout` | `CUSTOMER_ORG_ADMIN,CUSTOMER_APPLICANT,CUSTOMER_VIEWER` |
| `PortalReportPreview` | `/portal/reports/:id/preview` | `PortalLayout` | `CUSTOMER_ORG_ADMIN,CUSTOMER_APPLICANT,CUSTOMER_VIEWER` |
| `InternalLogin` | `/internal/login` | `BlankLayout` | none |
| `InternalWorkCenter` | `/internal/work-center` | `InternalLayout` | `SALES_REP,BUSINESS_OWNER,TECHNICAL_LEADER,SD,QA,QC,QUALITY_MANAGER,DEPT_MANAGER,GENERAL_MANAGER` |
| `InternalSubmissionList` | `/internal/submissions` | `InternalLayout` | `SALES_REP,SAMPLE_MANAGER,TECHNICAL_LEADER` |
| `InternalProjectDetail` | `/internal/projects/:id` | `InternalLayout` | `SALES_REP,BUSINESS_OWNER,TECHNICAL_LEADER,SD,QA,QC,SAMPLE_MANAGER,REPORT_PREPARER` |
| `InternalSampleDesk` | `/internal/samples/receiving` | `InternalLayout` | `SAMPLE_MANAGER` |
| `InternalStudyPlanDetail` | `/internal/study-plans/:id` | `InternalLayout` | `TECHNICAL_LEADER,SD,QA,ETHICS_COMMITTEE` |
| `InternalWorkOrderDetail` | `/internal/work-orders/:id` | `InternalLayout` | `INSPECTOR,TECHNICAL_LEADER,QC` |
| `InternalReportReview` | `/internal/reports/:id/review` | `InternalLayout` | `REPORT_PREPARER,QC,QA,SD,AUTHORIZED_SIGNATORY` |
| `InternalPaymentReceipts` | `/internal/payments` | `InternalLayout` | `FINANCE_CASHIER,FINANCE_MANAGER` |
| `InternalDeviceCalendar` | `/internal/devices/:id/calendar` | `InternalLayout` | `INSPECTOR,EQUIPMENT_MANAGER,TECHNICAL_LEADER,QUALITY_MANAGER` |
| `InternalInventory` | `/internal/inventory` | `InternalLayout` | `INVENTORY_MANAGER,INSPECTOR,QUALITY_MANAGER` |
| `InternalArchive` | `/internal/archive` | `InternalLayout` | `ARCHIVE_MANAGER,QUALITY_MANAGER` |
| `InternalChangeRequests` | `/internal/change-requests` | `InternalLayout` | `SALES_REP,BUSINESS_OWNER,TECHNICAL_LEADER,SD,QA,QC,SAMPLE_MANAGER,REPORT_PREPARER,QUALITY_MANAGER` |
| `InternalSampleRetention` | `/internal/sample-retentions` | `InternalLayout` | `SAMPLE_MANAGER,QUALITY_MANAGER,ARCHIVE_MANAGER` |
| `InternalReportRegistry` | `/internal/report-registry` | `InternalLayout` | `REPORT_PREPARER,QC,QA,SD,AUTHORIZED_SIGNATORY,ARCHIVE_MANAGER,QUALITY_MANAGER` |
| `InternalReportTemplates` | `/internal/report-templates` | `InternalLayout` | `REPORT_PREPARER,QUALITY_MANAGER,SYSTEM_ADMIN` |
| `InternalDeviceFiles` | `/internal/devices/:id/files` | `InternalLayout` | `EQUIPMENT_MANAGER,INSPECTOR,QUALITY_MANAGER` |
| `InternalMainData` | `/internal/master-data` | `InternalLayout` | `SYSTEM_ADMIN,QUALITY_MANAGER` |
| `InternalImportExportTasks` | `/internal/import-export-tasks` | `InternalLayout` | `SYSTEM_ADMIN,QUALITY_MANAGER,ARCHIVE_MANAGER` |
| `InternalSystemConfig` | `/internal/system` | `InternalLayout` | `SYSTEM_ADMIN` |
