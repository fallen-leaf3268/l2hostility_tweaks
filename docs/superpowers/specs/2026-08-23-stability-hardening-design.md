# L2Hostility Tweaks 稳定性加固设计

## 目标

只修复能够由源码或命令输出证明的稳定性问题，不调整游戏数值、现有玩法、配置默认值和依赖版本。构建失败必须返回真实非零状态，资源配置必须可被严格解析，异常网络输入不得影响服务端处理线程。

## 已确认问题

1. `l2hostility_tweaks.mixins.json` 的 `mixins` 数组末尾存在尾逗号，严格 JSON 解析失败。
2. `gradlew.bat` 无条件执行 `%JAVA_HOME%\bin\java.exe`。未设置 `JAVA_HOME` 时只输出“找不到指定路径”，最终仍返回 0。
3. Gradle Wrapper 下载超时为 10000 毫秒，当前环境下载 Gradle 8.5 时已触发超时。
4. `UnloadTraitPacket` 直接用客户端字符串构造 `ResourceLocation`，非法字符串可能抛出未处理异常。
5. 项目没有自动检查 JSON 语法和 Mixin 配置与源码的一致性。

## 修改范围

### Windows 构建入口

修改 `gradlew.bat`：

- `JAVA_HOME` 有值时使用其 `bin\java.exe`。
- `JAVA_HOME` 无值时使用 PATH 中的 `java.exe`。
- Java 不存在时输出明确错误并返回非零状态。
- Gradle 结束后保存并返回其真实退出码。

修改 `gradle/wrapper/gradle-wrapper.properties`，将 `networkTimeout` 从 10000 提高到 60000 毫秒。

### 资源与 Mixin 配置

移除 `src/main/resources/l2hostility_tweaks.mixins.json` 中的非法尾逗号，不改变 Mixin 列表和加载顺序。

在 `build.gradle` 中增加轻量级校验任务，并接入 `check`：

- 严格解析 `src/main/resources` 下的全部 JSON。
- 汇总 `mixins`、`client`、`server` 三个列表。
- 对照 `src/main/java/com/l2hostility_tweaks/mixin` 下的 Java 类，双向检查遗漏和无效登记。
- 错误信息包含具体相对路径或类名，并令构建失败。

### 网络输入

修改 `NetworkHandler.UnloadTraitPacket`：

- 用不会抛出格式异常的方式解析词条 ID。
- 非法 ID、未注册词条、未初始化能力或零等级均直接结束。
- 不记录可被重复请求刷屏的警告。
- 合法卸载、返还物品和能力同步流程保持不变。

## 数据流与错误处理

Windows 包装脚本先选择可用 Java，再启动 Gradle，并将 Gradle 退出状态原样传给调用者。资源校验在项目验证阶段运行，任何 JSON 或 Mixin 映射问题都在生成发布产物前失败。

卸载网络包在服务端主线程任务内先验证资源 ID，再查询词条注册表。验证失败时不产生物品、不修改玩家能力数据。验证成功后继续调用现有卸载方法，不引入新的权限条件或玩法变化。

## 验证标准

1. 未设置 `JAVA_HOME` 且 PATH 中存在 Java 17 时，`gradlew.bat` 能启动 Gradle。
2. Java 不可用或 Gradle 任务失败时，`gradlew.bat` 返回非零状态。
3. 全部 JSON 通过严格解析。
4. Mixin 配置与源码目录双向一致。
5. 非法词条 ID 被安全拒绝，合法 ID 仍进入原有卸载流程。
6. 最终执行 `clean build`。若外部下载仍失败，报告网络阻塞及真实退出码，不将其表述为代码通过。
7. 不复制或部署 JAR 到 `mods`。

## 非目标

- 不调整词条、装备、封印或卸载数值。
- 不重构大型类。
- 不新增运行时依赖。
- 不处理缺少证据的推测性问题。
- 不部署构建产物。
