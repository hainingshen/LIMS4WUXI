# 修改生效 SLA 与可配置项清单

## 1. 目的

客户已确认"快速迭代，反馈后修改立即上线"。本文将所有可能的修改分级，明确每一类的生效路径、生效时间、操作角色和落地实现。

本文是 B6 决策的工程版交付物。客户合同附件版本应根据本表精简后提供，不直接交付实现细节。

## 2. 分级原则

修改按"是否动代码、是否动数据库结构"分四类。开发期间所有新需求都必须按此表归类。归到 A / B 类越多，越能兑现"立即上线"承诺。

| 类别 | 是否动代码 | 是否动数据库结构 | 生效路径 |
| --- | --- | --- | --- |
| A 配置型 | 否 | 否 | 后台界面保存 → Redis Pub/Sub → 1 分钟内全网生效 |
| B 模板型 | 否 | 否 | 后台上传 / 编辑 → 下一次渲染即生效 |
| C 前端型 | 仅前端 | 否 | 前端独立 CI/CD → 2-5 分钟切换 |
| D 结构型 | 是 | 是 | Flyway 迁移 + 后端蓝绿 → 5-15 分钟 |

## 3. A 配置型清单

A 类的所有可编辑项必须落到 `system_config`、字典表、`permission_matrix`、`business_rule` 等表，且后台有界面允许指定角色编辑。**严禁写死在代码里**。

| 配置项 | 存储 | 默认值 | 可编辑角色 | 影响 |
| --- | --- | --- | --- | --- |
| 字典项（灭菌状态、样品处置、阻塞类型、付款方式等共 25 套） | `dictionary` 表 | `specs/dictionaries.csv` 种子 | `SYSTEM_ADMIN` | 前端下拉、字段渲染 |
| 权限矩阵 | `permission` 表 | `specs/permission_matrix.csv` 种子 | `SYSTEM_ADMIN` | 后端鉴权、前端按钮显示 |
| 角色清单 | `role` 表 | `specs/roles.csv` 种子 | `SYSTEM_ADMIN` | 用户授权候选项 |
| 健康度评分扣分规则 | `system_config`，key 前缀 `WORK_CENTER_` | 见 10 §3 | `QUALITY_MANAGER`、`SYSTEM_ADMIN` | 工作中心健康度计算 |
| 两天无进展告警阈值 | `system_config`，`WORK_CENTER_STALE_DAYS` | 2 | `QUALITY_MANAGER` | 阻塞自动告警触发 |
| 连续无进展扣分 | `system_config`，`WORK_CENTER_STALE_PENALTY` | 20 | `QUALITY_MANAGER` | 项目健康度计算 |
| 正常健康度最低分 | `system_config`，`WORK_CENTER_NORMAL_SCORE` | 85 | `QUALITY_MANAGER` | 项目健康度状态分界 |
| 风险健康度分界 | `system_config`，`WORK_CENTER_RISK_SCORE` | 70 | `QUALITY_MANAGER` | 项目健康度状态分界 |
| 单个超期任务扣分 | `system_config`，`WORK_CENTER_OVERDUE_TASK_PENALTY` | 8 | `QUALITY_MANAGER` | 项目健康度计算 |
| 超期任务最大扣分 | `system_config`，`WORK_CENTER_OVERDUE_TASK_MAX_PENALTY` | 24 | `QUALITY_MANAGER` | 项目健康度计算 |
| 单个未关闭阻塞扣分 | `system_config`，`WORK_CENTER_BLOCKER_PENALTY` | 10 | `QUALITY_MANAGER` | 项目健康度计算 |
| 未关闭阻塞最大扣分 | `system_config`，`WORK_CENTER_BLOCKER_MAX_PENALTY` | 30 | `QUALITY_MANAGER` | 项目健康度计算 |
| 检测员设备预约提前天数 | `system_config`，`DEVICE_BOOKING_INSPECTOR_DAYS` | 7 | `EQUIPMENT_MANAGER`、`SYSTEM_ADMIN` | 预约规则 |
| 外部用户设备预约提前天数 | `system_config`，`DEVICE_BOOKING_PORTAL_DAYS` | 2 | `EQUIPMENT_MANAGER`、`SYSTEM_ADMIN` | 预约规则 |
| 设备预约最小粒度 | `system_config`，`DEVICE_BOOKING_MIN_GRANULARITY_MINUTES` | 30 | `EQUIPMENT_MANAGER` | 预约时间槽 |
| Access Token TTL | `system_config`，`AUTH_ACCESS_TOKEN_TTL_MINUTES` | 15 | `SYSTEM_ADMIN` | JWT 签发参数 |
| Refresh Token TTL | `system_config`，`AUTH_REFRESH_TOKEN_TTL_DAYS` | 7 | `SYSTEM_ADMIN` | JWT 签发参数 |
| 报告归档保存年限 | `system_config`，`ARCHIVE_REPORT_RETENTION_YEARS` | 6 | `QUALITY_MANAGER`、`ARCHIVE_MANAGER` | 归档定期任务 |
| 标准品有效期预警提前天数 | `system_config`，`INVENTORY_EXPIRY_WARNING_DAYS` | 30 | `INVENTORY_MANAGER` | 预警生成 |
| 标准品低库存阈值（按 SKU） | `inventory_batch.low_stock_threshold` 字段 | 由管理员每条配置 | `INVENTORY_MANAGER` | 低库存预警 |
| 业务规则（"GLP 必走 QA"等） | `business_rule` 表 + Aviator 表达式 | 见 04 默认规则 | `QUALITY_MANAGER`、`SD`、`SYSTEM_ADMIN` | 审批岔路判断 |
| 邮件 / 通知文案 | `notification_template` 表 | 初始种子 | `SYSTEM_ADMIN` | 通知内容 |
| 字段标签 / 必填 / `lock_policy` | `form_schema` 表 + `specs/submission_ver15_fields.csv` 种子 | 同 08 文档 | `SYSTEM_ADMIN`、`SD` | 表单渲染 |
| 统一变更字段锁定策略 | `form_schema.lock_policy` + `business_rule` | `FREELY_EDITABLE`、`CHANGE_REQUEST`、`HARD_LOCK` | `QUALITY_MANAGER`、`SD`、`SYSTEM_ADMIN` | 决定字段变更保存即生效、审批后生效或禁止直接覆盖 |

A 类整体生效要求：**保存后 60 秒内全网生效**，通过 Redis Pub/Sub 通知所有 backend 节点失效本地缓存。

统一变更申请的业务生效 SLA：

| 变更类型 | 适用场景 | 生效路径 | 生效时间 |
| --- | --- | --- | --- |
| 软锁字段变更 | 合同签署前、报告签发前的低风险字段 | 申请人保存，领域服务直接写回目标对象 | 保存后 1 分钟内 |
| 审批后生效字段 | 合同签署后、报告签发前的协议、样品、项目关键字段 | 创建 `change_request`，对应审批角色通过后由领域服务写回 | 审批通过后 1 分钟内 |
| 硬锁字段变更 | 已签发报告、已归档项目、已形成法定记录的数据 | 不直接写回目标对象，进入报告重新签发、补充协议或只读更正流程 | 按业务流程完成 |

上述 SLA 只描述系统写回时间，不承诺审批人处理时长。审批人处理时长应另行纳入待办和工作中心超期提醒。

## 4. B 模板型清单

B 类是文件型资产，存在 MinIO 或数据库，按版本管理，后台可上传新版本。

| 模板 | 存储 | 版本管理 | 上传角色 | 激活方式 |
| --- | --- | --- | --- | --- |
| 报告 DOCX 模板（按 `template_code`） | MinIO + `report_template_version` 表 | 多版本 + 当前版本指针 | `SD`、`SYSTEM_ADMIN` | 后台点"激活"，下一次渲染采用 |
| 原始记录模板（按 `template_code`） | MinIO + 同上 | 多版本 | `SD`、`SYSTEM_ADMIN` | 同上 |
| 合同模板（按 `template_code`） | MinIO + 同上 | 多版本 | `SALES_REP`（草稿）、`SYSTEM_ADMIN`（发布） | 同上 |
| 报告声明文本（资质 / 无资质 / 非标各一套） | `report_declaration` 表 | 多版本 | `SD` | 同上 |
| 邮件模板 / 站内信模板 | `notification_template` 表 | 多版本 | `SYSTEM_ADMIN` | 即时 |
| 表单 schema（`form_schema`） | DB | 多版本 | `SYSTEM_ADMIN`、`SD` | 后台点"发布"切版本 |

B 类整体生效要求：**上传并激活后立即生效**。已经在渲染中的请求继续使用旧版本，新请求采用新版本。

## 5. C 前端型清单

C 类只动前端代码，后端无变更，单独 CI/CD。

| 修改场景 | 触达 | 生效时间 |
| --- | --- | --- |
| 表单字段顺序、列表筛选项、按钮位置 | `web-internal` 或 `web-portal` | 2-5 分钟 |
| UI 文案、提示信息 | 同上或 i18n 资源文件 | 2-5 分钟 |
| 新增筛选维度、新增列、新增统计图表（不动接口） | 同上 | 2-5 分钟 |

C 类整体生效要求：前端独立流水线推送 Nginx 静态文件软链切换。**用户当前会话不受影响**，下一次进入页面采用新前端。

## 6. D 结构型清单

D 类要动后端代码或数据库结构，是最慢的一档，必须走完整审核 + 蓝绿部署。

| 修改场景 | 步骤 | 生效时间 |
| --- | --- | --- |
| 新增数据库字段 | Flyway 脚本 + 实体类更新 + DTO + 前端表单 + 测试 | 5-15 分钟（含构建） |
| 新增 API 接口 | OpenAPI 更新 → 后端实现 → 前端调用 → 测试 | 5-15 分钟 |
| 改状态机分支 | 06 文档更新 + 服务层 + 测试 | 5-15 分钟 |
| 新增业务模块 | 完整设计评审 → 开发 → 测试 | 3-5 工作日 |
| 紧急 bug 修复 | 改代码 → 单元测试 → 蓝绿切换 | 2 小时内 |

D 类整体生效要求：蓝绿部署 30 秒切换，保留 30 秒回滚窗口。

## 7. 修改归类决策原则

新需求来时按以下顺序判断归类：

1. 能不能用现有 A 类配置实现？能则归 A。
2. 能不能改 B 类模板实现？能则归 B。
3. 是否只动前端？是则归 C。
4. 否则归 D，按 D 流程走。

**Vibe Coding 期间任何把硬编码值写入业务代码的提交都视为缺陷**，必须沉到 A 类配置。00 §15 LLM 代码生成约束已经写入这条规则。

## 8. 客户合同附件简化版本

下表用于客户合同附件，剔除工程实现细节，仅保留承诺：

| 修改类型 | 生效时间 | 谁能操作 |
| --- | --- | --- |
| 字典 / 权限 / 阈值 / 文案 / 审批人 | 保存后 1 分钟内 | 系统管理员后台 |
| 报告 / 邮件 / 通知模板 | 上传后立即生效 | SD / 系统管理员 |
| 业务规则微调 | 保存后 1 分钟内 | 系统管理员后台 |
| 新增表单字段 / 筛选条件 | 当天或次日 | 开发团队 |
| 加新功能模块 / 改流程结构 | 3-5 工作日 | 开发团队 |
| 紧急 bug 修复 | 2 小时内 | 开发团队 |
