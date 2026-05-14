# LIMS 开发文档 README

## 30 秒阅读顺序

用于 Vibe Coding 或开发脚手架生成时，先读：

1. `00_技术栈与工程约定.md`
2. `06_数据模型与状态机.md`
3. `07_API与界面契约.md`
4. `specs/openapi.yaml`
5. `specs/roles.csv`
6. `specs/permission_matrix.csv`
7. `specs/dictionaries.csv`
8. `17_修改生效SLA与可配置项清单.md`（配置/模板边界）

用于客户沟通一期范围时，先读：

1. `01_项目基础开发说明.md`
2. `02_业务流程与一期范围.md`
3. `17_修改生效SLA与可配置项清单.md`（快速迭代承诺）
4. `客户需确认的需求冲突与工作流问题.md`

用于字段和模板实现时，先读：

1. `08_送检单Ver15字段映射.md`
2. `09_报告自动生成试点_无菌检查.md`
3. `12_合同协议模板字段矩阵.md`
4. `specs/submission_ver15_fields.csv`
5. `specs/sterility_report_mapping.csv`
6. `specs/test_catalog.csv`

用于项目管理和上线准备时，先读：

1. `10_工作中心详细设计.md`
2. `05_数据迁移与实施边界.md`
3. `11_现有系统复用与重做策略.md`
4. `13_迭代里程碑与上线切换.md`
5. `16_部署与发布手册.md`
6. `21_开工前最终补齐记录.md`
7. `23_本地部署与UI验收计划.md`
8. `24_工作流实测缺口清单_20260515.md`

迁移到新开发机后，先在仓库根目录运行：

```powershell
python -m pip install -r .\scripts\checks\requirements.txt
python .\scripts\checks\csv-foreign-key.py
python .\scripts\checks\drift-detector.py
```

两条命令必须通过后再启动代码生成或 S1 开发。

## 关键技术栈速查

- 后端：Java 17 + Spring Boot 3.3 + JPA + QueryDSL + PostgreSQL 16 + Flyway + Redis + Aviator
- 前端：React 18 + TypeScript + Vite + AntD 5 + AntD Mobile 5 + formily + Zustand + TanStack Query
- 移动端：响应式 PWA（一套代码，AntD 与 AntD Mobile 按 breakpoint 切换）
- 鉴权：JWT Access 15min + Refresh 7d + Redis 黑名单
- 存储：MinIO；文档：OnlyOffice Document Server
- 部署：公有云 ECS 私有部署 + Docker Compose + Nginx 蓝绿 + 等保二级
- 核心实体：客户、送检单、项目、样品、工单、原始记录、报告、统一变更中心、留样、报告注册中心、设备文件、导入导出任务
- 机读契约：`specs/openapi.yaml` 当前覆盖 84 个 API path、83 个 schema，并已声明 JWT `bearerAuth`
- 详见 [00_技术栈与工程约定.md](00_技术栈与工程约定.md) 和 [18_第四轮决策记录.md](18_第四轮决策记录.md)。

## 代码生成硬约束

- 不新增未定义角色，角色以 `specs/roles.csv` 为准。
- 不新增未定义状态，状态以 `06_数据模型与状态机.md` 为准。
- 不新增接口字段，接口以 `specs/openapi.yaml` 和字段 CSV 为准。
- 不引入 Flowable、Drools、Groovy 脚本、Vue、Pinia、MyBatis 等被显式排除的依赖。
- 不在前端实现付款门禁。
- 不用中文角色名写权限判断。
- 不把送检单字段合并成自由文本，必须基于 `form_schema` 渲染。
- 不把字典、阈值、审批人写死在代码，必须读 `system_config` 或 `business_rule`。
- 所有写操作必须经 AOP 写入 `audit_log`。
- 改动 `specs/openapi.yaml`、`specs/*.csv` 或 `07_API与界面契约.md` 后，必须重新运行 `scripts/checks/csv-foreign-key.py` 和 `scripts/checks/drift-detector.py`。
