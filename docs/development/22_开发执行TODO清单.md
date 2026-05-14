# LIMS4WUXI 开发执行 TODO 清单

本文用于把当前开发文档转成可执行的 goal-driven 开发任务。每个 TODO 必须以一个可演示目标结束，禁止在未通过本项校验前继续扩散分析。

依据文档：

- `00_技术栈与工程约定.md`
- `06_数据模型与状态机.md`
- `07_API与界面契约.md`
- `10_工作中心详细设计.md`
- `13_迭代里程碑与上线切换.md`
- `17_修改生效SLA与可配置项清单.md`
- `specs/openapi.yaml`
- `specs/roles.csv`
- `specs/permission_matrix.csv`
- `specs/dictionaries.csv`
- `specs/submission_ver15_fields.csv`

硬门禁：

- 不新增未定义角色、状态、接口字段。
- 不引入 Flowable、Drools、Groovy、Vue、Pinia、MyBatis。
- 表单字段来自 `form_schema`，初始值由 CSV 转换，前端不得硬编码送检单字段。
- 所有写操作必须走权限校验和 `audit_log`。
- 状态唯一可信源是业务表状态字段，审批历史在 `approval_record`。
- 可调整规则进入 `business_rule` / `system_config` / 字典 / `form_schema`，不要写死。

执行格式：

```text
Goal: 本项完成后用户或测试能看到什么。
Code slice: 本项只允许集中修改哪些文件/类，并给出最小代码形状。
Verify: 必须通过的命令、接口或页面验收。
```

## S0 规格护栏和仓库骨架

### TODO-00 先跑现有规格校验

Goal: 确认文档和机读规格当前可作为开发输入。

Code slice:

```bash
python -m pip install -r scripts/checks/requirements.txt
python scripts/checks/csv-foreign-key.py
python scripts/checks/drift-detector.py
```

Verify: 两个 check 均退出码为 0。若失败，只修规格冲突，不启动应用开发。

### TODO-01 创建模块化单体和前端 workspace 骨架

Goal: 仓库出现 `backend/`、`web-internal/`、`web-portal/`、`web-shared/`，能空跑构建。

Code slice:

```text
backend/
  pom.xml
  src/main/java/com/njxs/lims/LimsApplication.java
  src/main/resources/application.yml
web-internal/
web-portal/
web-shared/
pnpm-workspace.yaml
```

```java
package com.njxs.lims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LimsApplication {
  public static void main(String[] args) {
    SpringApplication.run(LimsApplication.class, args);
  }
}
```

Verify:

```bash
cd backend && mvn test
pnpm -r typecheck
```

### TODO-02 固定 OpenAPI-first 生成入口

Goal: 前端类型和 API client 由 `specs/openapi.yaml` 生成，不手写字段。

Code slice:

```json
{
  "scripts": {
    "gen:api": "openapi-typescript docs/development/specs/openapi.yaml -o web-shared/src/generated/api-types.ts"
  }
}
```

```ts
// web-shared/src/api/http.ts
import axios from "axios";

export const http = axios.create({ baseURL: "/api/v1" });
http.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
```

Verify: 删除生成文件后运行 `pnpm gen:api` 能重新生成；页面代码只 import `web-shared/src/generated/api-types.ts`。

### TODO-03 建立 CSV 到种子 SQL / 枚举的生成脚本

Goal: 角色、权限、字典、状态值从 CSV 进入数据库和前后端类型，避免手工复制。

Code slice:

```text
scripts/dev/generate-seeds.py
backend/src/main/resources/db/migration/V002__seed_roles_permissions_dictionaries.sql
web-shared/src/generated/dictionaries.ts
```

```ts
// web-shared/src/generated/dictionaries.ts
export const PROJECT_STATUS = [
  "DRAFT",
  "ACCEPTED",
  "NONSTANDARD_REVIEW",
  "QUOTABLE",
  "QUOTED",
  "CONTRACTED",
  "WAITING_SAMPLE",
  "SAMPLE_RECEIVED",
  "PLAN_READY",
  "IN_TESTING",
  "REPORTING",
  "PAYMENT_HOLD",
  "RELEASED",
  "ARCHIVED"
] as const;
export type ProjectStatus = (typeof PROJECT_STATUS)[number];
```

Verify: 修改 `dictionaries.csv` 后重新生成，`PROJECT_STATUS` 与 CSV 行数和值完全一致。

## S1 基础框架：登录、角色、权限、字典、审计

### TODO-10 后端通用模型和错误响应

Goal: 所有实体继承统一审计字段，接口错误结构符合 `07_API与界面契约.md`。

Code slice:

```java
// backend/src/main/java/com/njxs/lims/common/domain/BaseEntity.java
@MappedSuperclass
public abstract class BaseEntity {
  @Id
  private UUID id;
  private String bizNo;
  private Instant createdAt;
  private UUID createdBy;
  private Instant updatedAt;
  private UUID updatedBy;
  private Instant deletedAt;
  @Version
  private Integer version;
  private UUID tenantOrgId;
  private String legacyId;
  private String sourceType;
  private String migrationBatch;
  private Integer dataQualityScore;
}
```

```java
public record ApiError(
    String code,
    String message,
    String traceId,
    Map<String, Object> details
) {}
```

Verify: 一个故意抛出的 `BusinessException("PAYMENT_GATE_LOCKED")` 返回 JSON 字段 `code/message/traceId/details`。

### TODO-11 建立 S1 Flyway 基础表

Goal: 数据库能保存用户、角色、权限、字典、系统配置、业务规则、表单 schema、审批记录、审计日志。

Code slice:

```sql
-- backend/src/main/resources/db/migration/V001__base_tables.sql
create table role (
  role_code varchar(64) primary key,
  label_zh varchar(128) not null,
  label_en varchar(128) not null,
  side varchar(32) not null,
  description text
);

create table permission (
  id uuid primary key,
  action_code varchar(64) not null,
  role_code varchar(64) not null references role(role_code),
  access varchar(32) not null,
  scope varchar(64) not null,
  note text,
  unique(action_code, role_code)
);

create table audit_log (
  id uuid primary key,
  occurred_at timestamptz not null,
  actor_user_id uuid,
  actor_role_code varchar(64),
  client_type varchar(32) not null,
  action_code varchar(64) not null,
  entity_type varchar(64),
  entity_id uuid,
  entity_biz_no varchar(64),
  before_snapshot jsonb,
  after_snapshot jsonb,
  result varchar(16) not null,
  failure_reason text,
  ip varchar(64),
  user_agent varchar(512),
  trace_id varchar(64)
);
```

Verify: `mvn test` 启动 Testcontainers PostgreSQL 并跑完 Flyway；`select count(*) from role` 等于 `roles.csv` 行数。

### TODO-12 实现 JWT 登录、刷新、登出黑名单

Goal: 内部端和客户门户能分别登录，token payload 固化 `clientType`。

Code slice:

```java
public enum ClientType {
  INTERNAL, PORTAL, MOBILE, SYSTEM
}

public record AuthPrincipal(
    UUID userId,
    ClientType clientType,
    List<String> roles,
    UUID orgId
) {}
```

```java
@PostMapping("/auth/internal/login")
TokenPair loginInternal(@RequestBody LoginRequest request) {
  return authService.login(request, ClientType.INTERNAL);
}

@PostMapping("/auth/portal/login")
TokenPair loginPortal(@RequestBody LoginRequest request) {
  return authService.login(request, ClientType.PORTAL);
}
```

Verify:

```bash
curl -X POST /api/v1/auth/internal/login
curl -H "Authorization: Bearer $TOKEN" /api/v1/auth/me
```

`/auth/me` 返回用户、`roles[]`、数据范围；黑名单 token 访问返回 401。

### TODO-13 实现权限矩阵和数据范围校验

Goal: 服务层按 `actionCode + roleCode + scope` 判断权限，不能用中文角色名。

Code slice:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresAction {
  String value();
}
```

```java
@Aspect
class PermissionAspect {
  @Around("@annotation(required)")
  Object check(ProceedingJoinPoint pjp, RequiresAction required) throws Throwable {
    permissionService.assertAllowed(SecurityContext.current(), required.value(), pjp.getArgs());
    return pjp.proceed();
  }
}
```

Verify: `CUSTOMER_VIEWER` 调用 `POST /submissions/{id}/submit` 返回 403；`CUSTOMER_APPLICANT` 对本人草稿返回 200。

### TODO-14 实现写操作审计 AOP

Goal: 每个 POST/PATCH/DELETE 成功或失败都写入 `audit_log`。

Code slice:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
  String actionCode();
  String entityType() default "";
}
```

```java
@Audited(actionCode = "SUBMISSION_ACCEPT", entityType = "submission")
@RequiresAction("SUBMISSION_ACCEPT")
public SubmissionDto accept(UUID id) {
  return submissionService.accept(id);
}
```

Verify: 调用一次受理接口后，`audit_log.action_code='SUBMISSION_ACCEPT'` 有一条 `SUCCESS`；故意失败时写 `FAILURE` 和 `failure_reason`。

### TODO-15 前端登录壳、路由守卫和菜单权限

Goal: `web-internal` 与 `web-portal` 可以登录，按 `roleCode` 显示入口，移动端断点切 AntD Mobile。

Code slice:

```ts
type AuthState = {
  accessToken?: string;
  roles: string[];
  clientType?: "INTERNAL" | "PORTAL" | "MOBILE";
  setSession: (session: AuthSession) => void;
};

export const useAuthStore = create<AuthState>((set) => ({
  roles: [],
  setSession: (session) => set(session),
}));
```

```tsx
export function RequireAction({ action, children }: PropsWithChildren<{ action: string }>) {
  const allowed = usePermission(action);
  return allowed ? <>{children}</> : null;
}
```

Verify: 用 `SALES_REP` 登录内部端能看到送检受理入口；用 `CUSTOMER_APPLICANT` 登录客户门户只能看到客户侧入口。

## S2 送检单 Ver.15 和动态表单

### TODO-20 创建客户、制造商、送检单、样品基础表和实体

Goal: `customer_org`、`customer_contact`、`manufacturer`、`submission`、`submission_test_selection`、`sample` 可持久化。

Code slice:

```java
@Entity
@Table(name = "submission")
public class Submission extends BaseEntity {
  private UUID customerOrgId;
  private UUID customerContactId;
  private UUID manufacturerId;
  private UUID applicantUserId;
  private String status;
  private String officialReportEmail;
  private String shippingInfo;
  private boolean requiresGlp;
  private String reportLanguage;
  private String protocolLanguage;
  private String submittedQuantityText;
  private String sampleDisposition;
  private Instant changeLockedAt;
}
```

Verify: Repository 集成测试能保存一张送检单和一条试验选择；数据库字段是 `snake_case`。

### TODO-21 由 `submission_ver15_fields.csv` 生成 `form_schema`

Goal: `SUBMISSION_VER15` v1 从 CSV 生成并发布，字段、必填、字典、`lock_policy` 完整。

Code slice:

```java
public record FormFieldSpec(
    String fieldKey,
    String labelZh,
    String labelEn,
    String type,
    boolean required,
    String dictionary,
    String entity,
    String lockPolicy
) {}
```

```json
{
  "type": "object",
  "properties": {
    "sponsorNameZh": {
      "type": "string",
      "title": "委托单位中文",
      "x-entity": "customer_org",
      "x-lock-policy": "CHANGE_REQUEST"
    }
  },
  "required": ["sponsorNameZh"]
}
```

Verify: `/api/v1/form-schemas?formCode=SUBMISSION_VER15` 返回 `PUBLISHED` 版本；字段数量等于 CSV 数据行数。

### TODO-22 实现送检单保存、提交和后端一致性校验

Goal: 客户能保存草稿和提交送检单，前后端必填校验一致。

Code slice:

```java
@PostMapping("/submissions")
@RequiresAction("SUBMISSION_CREATE")
@Audited(actionCode = "SUBMISSION_CREATE", entityType = "submission")
SubmissionDto createSubmission(@RequestBody SubmissionDraftRequest request) {
  return submissionApplicationService.createDraft(request);
}
```

```java
public void validateAgainstSchema(String formCode, JsonNode payload) {
  FormSchema schema = formSchemaService.current(formCode);
  schemaValidator.validate(schema.schemaJson(), payload);
}
```

Verify: 缺少 `officialReportEmail` 提交返回 400；补齐后 `POST /submissions/{id}/submit` 成功，`submission.status` 从 `DRAFT` 推进到 `SUBMITTED`，并且可被 `SALES_REP` 在待受理列表查询到。

### TODO-23 实现内部受理和拆分项目入口

Goal: `SALES_REP` 能受理送检单并拆分为项目，项目继承客户、GLP、样品信息。

Code slice:

```java
@PostMapping("/submissions/{id}/accept")
@RequiresAction("SUBMISSION_ACCEPT")
@Audited(actionCode = "SUBMISSION_ACCEPT", entityType = "submission")
SubmissionDto accept(@PathVariable UUID id) {
  return submissionWorkflowService.accept(id);
}
```

```java
@PostMapping("/submissions/{id}/split-projects")
@RequiresAction("PROJECT_TYPE_DECIDE")
List<ProjectDto> splitProjects(@PathVariable UUID id, @RequestBody SplitProjectsRequest request) {
  return projectSplitService.splitFromSubmission(id, request);
}
```

Verify: 非 `SALES_REP` 返回 403；拆分后生成的 `project.customer_org_id` 与送检单一致。

### TODO-24 实现客户门户动态送检表单

Goal: 页面运行时加载 `form_schema` 和字典渲染表单，不在 TSX 中写字段列表。

Code slice:

```tsx
export function SubmissionFormPage() {
  const schema = useFormSchema("SUBMISSION_VER15");
  const dictionaries = useDictionaries(schema.dictionaryCodes);
  return (
    <SchemaForm
      schema={schema.schemaJson}
      dictionaries={dictionaries}
      onSubmit={(values) => createSubmission(values)}
    />
  );
}
```

Verify: 修改 `form_schema` 字段顺序后刷新页面，表单顺序变化，无需改前端代码。

## S3 项目、非标评审、业务规则

### TODO-30 创建项目实体和项目状态机服务

Goal: 项目只能按 `06` 文档定义的状态迁移。

Code slice:

```java
public enum ProjectStatus {
  DRAFT, ACCEPTED, NONSTANDARD_REVIEW, QUOTABLE, QUOTED, CONTRACTED,
  WAITING_SAMPLE, SAMPLE_RECEIVED, PLAN_READY, IN_TESTING, REPORTING,
  PAYMENT_HOLD, RELEASED, ARCHIVED
}
```

```java
public void transition(Project project, ProjectStatus target, String reason) {
  if (!allowedTargets(project.getStatus()).contains(target)) {
    throw new BusinessException("ILLEGAL_PROJECT_STATUS_TRANSITION");
  }
  project.setStatus(target.name());
  eventPublisher.publishEvent(new ProjectStatusChangedEvent(project.getId(), target.name(), reason));
}
```

Verify: `ACCEPTED -> QUOTED` 直接迁移必须失败；`ACCEPTED -> QUOTABLE -> QUOTED` 成功。

### TODO-31 实现 `approval_record`

Goal: 非标评审、方案审批、报告审核等审批统一落表。

Code slice:

```sql
create table approval_record (
  id uuid primary key,
  entity_type varchar(64) not null,
  entity_id uuid not null,
  stage varchar(64) not null,
  outcome varchar(32) not null,
  comment text,
  approved_by uuid not null,
  approved_at timestamptz not null
);
```

```java
approvalRecordService.record("project", projectId, stage, outcome, comment, currentUser.id());
```

Verify: 技术和业务两次非标审批后，同一 `project_id` 有 `TECH`、`BUSINESS` 两条审批记录。

### TODO-32 实现 Aviator 业务规则服务和缓存失效

Goal: GLP、动物试验、工作中心阈值等判断从 `business_rule` 或 `system_config` 获取。

Code slice:

```java
public interface RuleEngineService {
  boolean evaluate(String ruleCode, Map<String, Object> context);
}
```

```java
boolean requiresQa = ruleEngineService.evaluate(
    "PLAN_REQUIRES_QA",
    Map.of("requiresGlp", project.isRequiresGlp())
);
```

Verify: 后台把 `PLAN_REQUIRES_QA.condition_expr` 从 `requiresGlp == true` 改成 `false` 后，1 分钟内新方案不再要求 QA。

### TODO-33 实现非标评审接口

Goal: 非标项目技术负责人和业务负责人都通过后才能报价。

Code slice:

```java
@PostMapping("/projects/{id}/nonstandard-review/start")
@RequiresAction("NONSTANDARD_REVIEW_START")
ProjectDto start(@PathVariable UUID id) {
  return nonstandardReviewService.start(id);
}
```

```java
@PostMapping("/projects/{projectId}/nonstandard-review/approve")
ProjectDto approve(@PathVariable UUID projectId, @RequestBody ApprovalRequest request) {
  return nonstandardReviewService.approve(projectId, request.stage(), request.outcome(), request.comment());
}
```

Verify: 只有 TECH 通过时项目仍为 `NONSTANDARD_REVIEW`；TECH 和 BUSINESS 都通过后为 `QUOTABLE`；任一驳回回到 `ACCEPTED`。

### TODO-34 实现报价前 guard

Goal: 非标未评审通过不能生成报价。

Code slice:

```java
public void assertQuotable(Project project) {
  if (!"QUOTABLE".equals(project.getStatus())) {
    throw new BusinessException("PROJECT_NOT_QUOTABLE");
  }
}
```

Verify: `POST /projects/{id}/quotations` 对 `NONSTANDARD_REVIEW` 项目返回 409；对 `QUOTABLE` 项目成功并推进 `QUOTED`。

## S4 合同与样品接收

### TODO-40 实现合同签署确认和字段锁定

Goal: 合同签署后项目进入 `CONTRACTED`，送检单核心字段进入变更申请路径。

Code slice:

```java
@PostMapping("/contracts/{id}/mark-signed")
@RequiresAction("CONTRACT_SIGN_CONFIRM")
@Audited(actionCode = "CONTRACT_SIGN_CONFIRM", entityType = "contract")
ContractDto markSigned(@PathVariable UUID id) {
  return contractService.markSigned(id);
}
```

Verify: 签署后 `submission.change_locked_at` 非空；直接修改 `CHANGE_REQUEST` 字段返回 409 并提示创建变更申请。

### TODO-41 实现样品到达、核验、预入库、正式入库

Goal: 样品状态按 `WAITING_DELIVERY -> ARRIVED_PENDING_CHECK -> PRE_IN_STOCK -> IN_STOCK` 推进，异常进入 `CHECK_EXCEPTION`。

Code slice:

```java
@PostMapping("/samples/{id}/receive")
@RequiresAction("SAMPLE_RECEIVE")
SampleDto receive(@PathVariable UUID id, @RequestBody ReceiveSampleRequest request) {
  return sampleWorkflowService.receive(id, request);
}
```

```java
@PostMapping("/samples/{id}/stock-in")
@RequiresAction("SAMPLE_STOCK_IN")
SampleDto stockIn(@PathVariable UUID id, @RequestBody StockInRequest request) {
  return sampleWorkflowService.stockIn(id, request);
}
```

Verify: 样品核验异常后不能入库；异常处理复核后才允许 `PRE_IN_STOCK -> IN_STOCK`。

### TODO-42 实现文件元数据和样品照片上传

Goal: 样品照片进入 MinIO，元数据进入 `file_metadata`，业务表只存 `file_id`。

Code slice:

```java
public record StoredFile(UUID fileId, String objectKey, String sha256, long sizeBytes) {}

public StoredFile store(MultipartFile file, String folder) {
  String objectKey = folder + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
  return minioStorage.put(objectKey, file);
}
```

Verify: 上传样品照片后 MinIO 有对象，`file_metadata.sha256` 非空，删除样品不删除物理文件。

### TODO-43 实现样品收样台页面

Goal: 内部端能搜索送检单/项目、核验客户填报信息、登记到达、上传照片、入库。

Code slice:

```tsx
export function SampleReceivingDesk() {
  const [keyword, setKeyword] = useState("");
  const samples = useSamples({ keyword });
  return <SampleReceivingTable samples={samples} onReceive={receiveSample} onStockIn={stockInSample} />;
}
```

Verify: 用 `SAMPLE_MANAGER` 能完成接收和入库；用 `INSPECTOR` 页面只读或无入口。

## S4.5 统一变更中心与留样

### TODO-45 实现 `change_request` 和字段 diff

Goal: 协议、样品、项目、报告关键字段变更进入统一申请。

Code slice:

```java
public record ChangeItemRequest(
    String fieldPath,
    String fieldLabel,
    String oldValue,
    String newValue,
    String lockPolicy
) {}
```

```java
ChangeRequest create(UUID targetId, List<ChangeItemRequest> items) {
  lockPolicyService.assertNoDirectHardLockEffect(targetId, items);
  return repository.save(ChangeRequest.draft(targetId, items));
}
```

Verify: `lotBatch` 在报告签发后创建变更申请时状态进入 `NEEDS_REPORT_REVISION`，不能直接覆盖样品表。

### TODO-46 实现变更审批和生效服务

Goal: 审批通过后由领域服务写回目标实体，并写 `approval_record` 和 `audit_log`。

Code slice:

```java
@Transactional
public ChangeRequest effect(UUID id) {
  ChangeRequest request = loadApproved(id);
  targetWriter.apply(request.getTargetType(), request.getTargetId(), request.getItems());
  request.setStatus("EFFECTIVE");
  return request;
}
```

Verify: `CHANGE_REQUEST_EFFECT` 成功后目标字段改变；`audit_log.entity_type` 为目标实体而不是仅记录变更单。

### TODO-47 实现留样登记、延期、处置

Goal: 留样作为独立生命周期实体，不再只写样品备注。

Code slice:

```java
@PostMapping("/sample-retentions/{id}/register")
@RequiresAction("SAMPLE_RETENTION_REGISTER")
SampleRetentionDto register(@PathVariable UUID id) {
  return retentionService.register(id);
}
```

Verify: 登记后状态 `RETAINED`；延期必须填写新 `retain_until` 和原因；退还/销毁受付款门禁控制。

## S5 方案、工单、原始记录

### TODO-50 实现方案审批和下发 guards

Goal: 资料不全时方案可审批到 `PENDING_DISPATCH`，但不能生成可执行工单。

Code slice:

```java
public DispatchReadiness checkReadiness(StudyPlan plan) {
  return new DispatchReadiness(
      contractSigned(plan.projectId()),
      samplesReady(plan.projectId()),
      requiredApprovalsDone(plan),
      conditioningDone(plan.projectId())
  );
}
```

```java
if (!readiness.ready()) {
  plan.setStatus("PENDING_DISPATCH");
  return;
}
plan.setStatus("READY_TO_DISPATCH");
```

Verify: 样品未到时 `POST /study-plans/{id}/dispatch` 返回 409；样品入库且审批完成后生成工单。

### TODO-51 实现工单执行状态机

Goal: 检测员只能执行分配给自己的工单，状态按文档推进。

Code slice:

```java
public void start(UUID workOrderId, UUID currentUserId) {
  WorkOrder wo = loadAssigned(workOrderId, currentUserId);
  transition(wo, "IN_PROGRESS");
}
```

Verify: 非 owner 启动工单返回 403；`PENDING_EXECUTION -> IN_PROGRESS` 直接跳过 `SAMPLE_PICKED` 返回 409。

### TODO-52 实现原始记录提交和 QC 审核

Goal: 工单提交原始记录后进入审核，通过后工单 `COMPLETED`。

Code slice:

```java
@PostMapping("/work-orders/{id}/submit-raw-record")
@RequiresAction("RAW_RECORD_SUBMIT")
RawRecordDto submitRawRecord(@PathVariable UUID id, @RequestBody RawRecordPayload payload) {
  return rawRecordService.submit(id, payload);
}
```

Verify: QC 驳回后工单回到 `RESULT_ENTERED`；QC 通过后工单 `COMPLETED`，项目所有必需工单完成时项目进入 `REPORTING`。

## S6 无菌报告试点和报告审核

### TODO-60 实现报告模板注册、发布、废止

Goal: 模板按 `template_code + template_version` 版本化，发布新版本不影响历史报告。

Code slice:

```java
public ReportTemplate publish(UUID id) {
  ReportTemplate current = repository.getReferenceById(id);
  repository.deprecatePublished(current.getTemplateCode());
  current.setStatus("PUBLISHED");
  current.setPublishedAt(Instant.now());
  return current;
}
```

Verify: 同一 `template_code` 同时只有一个 `PUBLISHED`；历史 `report_version.template_id` 不变。

### TODO-61 实现无菌原始记录到报告字段映射

Goal: `sterility_report_mapping.csv` 驱动报告数据快照，不从代码临时拼字段。

Code slice:

```java
public Map<String, Object> buildSnapshot(UUID reportId, String templateCode) {
  List<FieldMapping> mappings = mappingRepository.findByTemplateCode(templateCode);
  return mappingEvaluator.evaluate(reportId, mappings);
}
```

Verify: 映射 CSV 中任一模板字段缺失时，生成草稿失败并提示字段名；补齐后成功。

### TODO-62 实现报告草稿生成

Goal: 结构化数据渲染 DOCX 草稿，带水印，保存模板版本、数据快照和渲染日志。

Code slice:

```java
@PostMapping("/reports/generate-draft")
@RequiresAction("REPORT_DRAFT_CREATE")
ReportDto generateDraft(@RequestBody GenerateDraftReportRequest request) {
  return reportGenerationService.generateDraft(request);
}
```

Verify: 工单未全部完成时生成失败；完成后生成 `report.status=DRAFT` 和一条 `report_version`。

### TODO-63 实现 QC/QA/SD 审核和授权签发

Goal: 报告按 `DRAFT -> QC_REVIEWING -> QA_REVIEWING -> SD_REVIEWING -> PENDING_ISSUE -> ISSUED_PAYMENT_LOCKED` 推进。

Code slice:

```java
public Report review(UUID reportId, String stage, String outcome, String comment) {
  approvalRecordService.record("report", reportId, stage, outcome, comment, currentUser.id());
  return reportStateMachine.afterReview(reportId, stage, outcome);
}
```

Verify: QC 驳回回到 `DRAFT`；SD 通过后 `PENDING_ISSUE`；授权签发后未付款进入 `ISSUED_PAYMENT_LOCKED`。

### TODO-64 接入 OnlyOffice 转 PDF 的适配层

Goal: 后端只依赖 `DocumentConversionService` 接口，正式 PDF 生成前必须报告已审核签发。

Code slice:

```java
public interface DocumentConversionService {
  StoredFile convertDocxToPdf(UUID docxFileId);
}
```

Verify: `DRAFT` 报告调用转换返回 409；`PENDING_ISSUE` 签发后才生成正式 PDF。

## S7 付款门禁和绿色通道

### TODO-70 实现收款记录和付款门禁服务

Goal: 未付款禁止客户下载正式 PDF；财务登记到账后解锁。

Code slice:

```java
public PaymentGateStatus gateForReport(UUID reportId) {
  ProjectPayment payment = paymentService.aggregateByReport(reportId);
  if (payment.paid()) return PaymentGateStatus.UNLOCKED;
  if (greenChannelService.unlocked(reportId)) return PaymentGateStatus.GREEN_UNLOCKED;
  return PaymentGateStatus.LOCKED;
}
```

Verify: 未付款 `GET /reports/{id}/official-pdf` 返回 `PAYMENT_GATE_LOCKED`；到账后返回文件。

### TODO-71 实现绿色通道两级审批

Goal: 部门负责人和管理层都通过后，只临时解锁电子正式报告，不改变项目已付款状态。

Code slice:

```java
if (stage.equals("DEPT") && approved) request.setStatus("MANAGEMENT_REVIEWING");
if (stage.equals("MANAGEMENT") && approved) {
  request.setStatus("APPROVED");
  reportGateService.greenUnlock(request.getReportId());
}
```

Verify: 绿色通道通过后 `payment_status` 仍不是 `PAID`；报告门禁为 `GREEN_UNLOCKED`。

### TODO-72 实现正式报告下载和发放日志

Goal: 每次预览、下载、邮件、绿色通道发放都写 `report_delivery_log`。

Code slice:

```java
deliveryLogService.record(new DeliveryLogCommand(
    reportId, versionId, "DOWNLOAD", currentUser.clientType(), gateStatus, fileId
));
```

Verify: 客户下载一次正式 PDF 后，注册中心能查到 `delivery_type=DOWNLOAD` 和当时门禁状态。

## S7.5 报告注册、模板、设备文件、导入导出

### TODO-75 实现报告注册中心查询和批量下载任务

Goal: 已签发报告可按项目、客户、报告编号、模板、签发日期和发放状态检索。

Code slice:

```java
@GetMapping("/report-registry/search")
@RequiresAction("REPORT_REGISTRY_READ")
Page<ReportRegistryRow> search(ReportRegistryQuery query) {
  return reportRegistryService.search(query);
}
```

Verify: 当前版本和历史版本都能显示；批量下载创建 `import_export_task.task_type=REPORT_BATCH_DOWNLOAD`。

### TODO-76 实现设备文件上传、当前有效文件和到期预警

Goal: 设备文件嵌入设备详情，证书到期进入工作中心预警。

Code slice:

```java
@PostMapping("/devices/{id}/files")
@RequiresAction("DEVICE_FILE_UPLOAD")
DeviceFileDto upload(@PathVariable UUID id, @RequestPart MultipartFile file, @RequestPart DeviceFileMeta meta) {
  return deviceFileService.upload(id, file, meta);
}
```

Verify: 普通检测员只能读本人工单相关设备文件状态；设备管理员能上传和作废。

### TODO-77 实现导入导出任务框架

Goal: 大批量导入导出异步执行，有预校验、结果文件、错误摘要。

Code slice:

```java
public void run(UUID taskId) {
  task.markRunning();
  ImportResult result = handlerFor(task.getTaskType()).run(task);
  task.complete(result.resultFileId(), result.errorSummary());
}
```

Verify: 导入预校验失败不会写业务表；成功任务记录操作者、参数、文件哈希和结果摘要。

## S8 工作中心

### TODO-80 实现健康度评分引擎

Goal: 项目健康分和 `NORMAL/WARNING/RISK` 从结构化规则计算，阈值来自 `system_config`。

Code slice:

```java
public ProjectHealth evaluate(ProjectHealthContext ctx) {
  int score = 100;
  score -= stalePenalty(ctx);
  score -= milestonePenalty(ctx);
  score -= blockerPenalty(ctx);
  boolean forcedRisk = hasForcedRisk(ctx);
  return ProjectHealth.of(score, forcedRisk ? "RISK" : statusByScore(score));
}
```

Verify: 将 `WORK_CENTER_STALE_DAYS` 从 2 改为 1 后，1 分钟内无进展项目变为预警或风险。

### TODO-81 实现阻塞记录 CRUD 和关闭校验

Goal: 阻塞必须结构化，关闭必须填写解决说明。

Code slice:

```java
@PostMapping("/work-center/blockers/{id}/close")
BlockerDto close(@PathVariable UUID id, @RequestBody CloseBlockerRequest request) {
  if (request.resolution().isBlank()) throw new BusinessException("BLOCKER_RESOLUTION_REQUIRED");
  return blockerService.close(id, request.resolution());
}
```

Verify: `resolution` 为空关闭返回 400；关闭后项目健康度重算。

### TODO-82 实现工作中心页面

Goal: 内部端能看到健康度、当前卡点、我的待办、人员负荷、趋势。

Code slice:

```tsx
export function WorkCenterPage() {
  const summary = useWorkCenterSummary();
  const projects = useWorkCenterProjects();
  const blockers = useWorkCenterBlockers();
  return <WorkCenterLayout summary={summary} projects={projects} blockers={blockers} />;
}
```

Verify: `QUALITY_MANAGER` 能看全部风险；普通角色只能看权限范围内项目。

## S9 迁移、部署和上线演练

### TODO-90 建立 Docker Compose 私有部署基线

Goal: 本地或测试机一条命令启动 backend、PostgreSQL、Redis、MinIO、OnlyOffice、Nginx。

Code slice:

```yaml
services:
  backend:
    build: ./backend
    environment:
      SPRING_PROFILES_ACTIVE: prod
    depends_on: [postgres, redis, minio]
  postgres:
    image: postgres:16
  redis:
    image: redis:7
  minio:
    image: minio/minio
  onlyoffice:
    image: onlyoffice/documentserver
```

Verify: `docker compose up -d` 后 `/api/v1/auth/me` 未登录返回 401，数据库 Flyway 已执行。

### TODO-91 建立 CI 校验流水线

Goal: 每次变更自动跑文档规格校验、后端测试、前端 typecheck。

Code slice:

```yaml
steps:
  - run: python scripts/checks/csv-foreign-key.py
  - run: python scripts/checks/drift-detector.py
  - run: cd backend && mvn test
  - run: pnpm -r typecheck
```

Verify: 故意改坏 `permission_matrix.csv` 外键时 CI 失败。

### TODO-92 跑通一期最小验收脚本

Goal: 以一条真实业务链路验证一期闭环。

Code slice:

```text
客户提交送检单
-> 内部受理并拆分项目
-> 样品接收和入库
-> 方案审批和工单下发
-> 检测员提交原始记录
-> 生成无菌报告草稿
-> QC/QA/SD/授权签发
-> 未付款禁止下载
-> 财务登记到账
-> 客户下载正式报告
-> 样品处置和归档
-> 工作中心显示健康度和时间线
```

Verify: 使用一组固定测试数据跑完整链路，所有状态与 `06_数据模型与状态机.md` 一致，所有写动作都有 `audit_log`。

Executable: `scripts/acceptance/phase-one-smoke.sh` 运行 `PhaseOneAcceptanceFlowTests`，在 Docker/Testcontainers 可用环境中执行上述固定数据链路。

## 每次提交前检查清单

```bash
python scripts/checks/csv-foreign-key.py
python scripts/checks/drift-detector.py
cd backend && mvn test
pnpm -r typecheck
```

人工检查：

- 新角色是否来自 `roles.csv`。
- 新权限是否来自 `permission_matrix.csv`。
- 新状态是否来自 `dictionaries.csv` 和 `06` 状态机。
- 新接口是否先写进 `specs/openapi.yaml`。
- 写操作是否有 `@RequiresAction` 和 `@Audited`。
- 业务可调值是否进入 `system_config`、`business_rule`、字典或 `form_schema`。
- 送检单字段是否来自 `form_schema`，不是 TSX 硬编码。
- 报告字段是否来自字段映射，不是模板里临时写数据库字段。
