# KubeJS 法术绕过参数校验设计

## 目标

让 `L2HFix.enableBypass(...)` 安全处理 `null` 和未知类型：错误脚本不会中断整次 KubeJS 加载，同时日志能明确指出错误值和所有合法选项。

## 已确认根因

`SpellDamageFlags.enableBypass(String type)` 直接对 `type` 执行字符串 `switch`。Java 对 `null` 执行该 `switch` 会抛出空指针异常；未知字符串虽然进入 `default`，但当前静默返回，脚本作者无法从日志判断配置为何没有生效。

## 行为设计

保留现有 `public static void enableBypass(String type)` 方法签名，避免改变 KubeJS 调用方式或 Java 方法描述符。

合法值及行为保持不变：

- `bypass_armor`
- `bypass_magic` 和别名 `bypass_effects`
- `bypass_cooldown`
- `bypass_resistance`
- `bypass_enchantments`

对 `null` 或任何未知字符串：

1. 不抛出异常。
2. 不增加、删除或替换任何活动绕过标记。
3. 使用 `l2htweaks:kubejs` 日志分类记录一条错误日志。
4. 日志包含收到的原始值以及全部合法值，便于直接修正脚本。

输入匹配继续区分大小写，也不自动裁剪空格。比如 `bypass_resistence`、`BYPASS_RESISTANCE` 和 `bypass_resistance ` 都是错误值；明确报错比静默猜测脚本意图更安全。

## 数据流

KubeJS 调用 `L2HFix.enableBypass(type)` 后，方法先校验输入并选择对应 `DamageTypeTags`。合法值加入现有并发集合；无效值只写日志并立即返回。之后伤害判定仍通过 `SpellDamageFlags.isBypassEnabled(tag)` 查询集合，不改变战斗路径。

## 错误处理

错误参数属于脚本配置错误，但不应阻止其他脚本注册。因此本项不抛出 `IllegalArgumentException`，也不启用任何默认绕过能力。每次错误调用记录一次日志；由于该 API 通常在脚本加载或重载时调用，不增加战斗时日志开销。

## 测试

新增 `SpellDamageFlagsTest` 覆盖：

1. `null` 不再抛出异常，且活动标记集合不变。
2. 未知字符串不抛出异常，且活动标记集合不变。
3. 无效输入写入包含原始值和合法值提示的错误日志。
4. 完整 `clean build` 继续通过，确认现有合法路径和资源校验不受影响。

测试通过反射观察私有活动集合，以避免普通 JUnit 进程为无效输入强行启动完整 Minecraft/Forge 注册环境。日志测试只观察公开调用产生的日志事件，不新增测试专用生产接口。

## 非目标

- 不新增绕过类型或配置项。
- 不调整合法绕过类型的伤害计算。
- 不自动修正大小写、空格或拼写。
- 不改变脚本重载清理行为。
- 不把 JAR 部署到 `mods` 目录。
