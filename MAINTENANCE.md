# L2Hostility Tweaks 维护手册

## 一、封印系统核心机制

### 数据模型
```
traits["tank"] = -3          ← cap.traits 存负数表示封印
PersistentData:
  l2htweaks_sealed_level_tank = 3   ← 封印前原始等级
  l2htweaks_seal_expiry_tank  = -1  ← 到期 tick（-1=永久）
```

### 关键原则
1. **`traits` 中的负数 = 封印**，绝对值 = 原始等级
2. 封印时调用 `initialize(0)` 移除效果，解封时 `initialize(restore)` + `postInit(restore)`
3. 每次封印/解封后调 `cap.syncToClient(target)` 同步到客户端
4. 本模组封印、解封、自用符号和卸载词条时保持**等比例**血量：`newHealth = oldHp/oldMax * newMax`。TraitAdderWand 升级委托本体，保留本体恢复满血的行为。

### 等比例血量位置
```
TraitDisableHelper.setDisabled()       — 封印/解封
TraitSymbolSelfUseMixin                — 玩家自用词条
TraitSymbolMixin                       — 对生物用词条修正
TraitUnloaderWand                      — 卸载词条
```

---

## 二、API 对外表现

### `getTraitLevel(trait)` — 对外返回 0（GetTraitLevelMixin）
### `hasTrait(trait)` — 封印或零等级对外返回 false（GetTraitLevelMixin）

### 玩家词条追加
`PlayerTraitRules` 合并 `traits` 和 `pending`：先按顺序覆盖等级，再移除批次中出现过零等级的词条，与本体 `clearPending()` 的两阶段处理一致。封印词条占用数量和预算；手动自用、给目标玩家使用符号及 Drain 自动追加共用数量、预算、最低等级及互斥检查。

Drain 自动追加不得覆盖封印，被拒绝的候选继续本体最多四次抽样。手动添加成功时只清除目标词条的旧待添加记录，避免后续 `clearPending()` 覆盖升级等级；给目标玩家用符号在原生 `compute()` 前清理，失败及随后 `postInit()` 新产生的队列不受影响。对封印生物使用符号时仍调用本体 `TraitSymbol.allow()` 验证准入。

### ⚠️ 直接读 `traits` 原始值的 Mixin（绕过 API）
这些文件直接读 `cap.traits.get(trait)` 或 `traits.getOrDefault()`，**不走** `getTraitLevel()`：
- `TraitLootConditionMixin` — 需要原始负数判断
- `TraitLootModifierMixin` — 需要原始值算绝对值
- `EnvyLootModifierMixin` — 需要原始值算绝对值
- `TraitAdderWandMixin` — 需要原始值判断封印
- `TraitSymbolMixin` — 需要原始值判断封印
- `TraitSymbolSelfUseMixin` — 需要原始值算绝对值

**新增 Mixin 时**：如果是要读**等级数值**，用 `getTraitLevel()`；如果是要检测**封印状态**，读 `traits.get(trait)` 看是否 < 0。

---

## 三、Mixin 注意事项

### 命名规范
- 注入处理器和 Accessor 方法使用 `l2fix$` 前缀；`@Shadow` 保留目标方法名
- 示例：`private void l2fix$doSomething(...)`

### 类名后缀
- 功能修改：`*Mixin.java`
- Accessor：`*Accessor.java`
- 检测类：`*Detector.java`

### 注册
所有 Mixin 必须在 `l2hostility_tweaks.mixins.json` 中注册。新增或删除 Mixin 后必须同步更新该文件。

---

## 四、常见的坑

### 1. `static` vs 实例方法
目标方法是 `static` → Mixin 处理器也必须是 `static`
目标方法是实例方法 → Mixin 处理器不能是 `static`

### 2. @Redirect 递归
`@Redirect` 替换目标方法调用。如果在 handler 内再次调同一个方法，会触发递归。
解决方式：用 ThreadLocal 标志位防递归，或改用 `@ModifyArg`/`@ModifyVariable`。

### 3. @Mixin 目标类
确保 `@Mixin` 指定的是**包含目标方法的类**，不是相关类。
错误示例：`@Mixin(TraitGenerator.class)` 但 redirect 的 `fill()` 在 `TraitManager` 中。

### 4. 移除配置项时
如果从 `L2HConfig.java` 删除配置字段和 getter，必须同时在 `gradle.properties` 增版本号，否则已有配置文件的用户会读到失效键。

### 5. 同步问题
`sealedLevelKey` 存在 `PersistentData` 中，**不同步到客户端**。
客户端靠 `traits` 里的负值识别封印。`syncToClient()` 同步 `traits` 但不含 `PersistentData`。

---

## 五、封印词条行为过滤（确保所有调用点已被覆盖）

| 词条方法 | 调用路径 | 过滤方式 |
|---|---|---|
| `tick()` | `MobTraitCap.tick()` → `forEach` | `TraitSealFilterMixin` v≤0 |
| `postInit()` | `MobTraitCap.tick()` 第 274 行 | `TraitSealFilterMixin` ordinal=0 |
| `onHurtTarget()` | `LHAttackListener.onHurt` → `traitEvent` | `TraitSealFilterMixin` |
| `onDamaged()` | `LHAttackListener.onDamage` → `traitEvent` | `TraitSealFilterMixin` |
| `onCreateSource()` | `LHAttackListener.onCreateSource` → `traitEvent` | `TraitSealFilterMixin` |
| `onAttackedByOthers()` | `MobEvents` → `traitEvent` | `TraitSealFilterMixin` |
| `onHurtByOthers()` | `MobEvents` → `traitEvent` | `TraitSealFilterMixin` |
| `onDeath()` | `MobEvents` → `traitEvent` | `TraitSealFilterMixin` |
| `modifyBonusDamage()` | `LHAttackListener.onHurt` 直接遍历 | `LHAttackListenerMixin` max(0, level) |

---

## 六、配置文件

### `L2HConfig.java`（COMMON）
| 配置项 | 用途 | 使用位置 |
|---|---|---|
| `reprintLinearEnabled` | 复印线性伤害 | `ReprintTraitMixin` |
| `adaptiveLinearEnabled` | 适应线性减伤 | `AdaptingTraitMixin` |
| `oldDispell` | 旧版破魔免疫 | `DispellTraitMixin` |
| `oldDementor` | 旧版摄魂免疫 | `DementorTraitMixin` |
| `sealDurationMode/Linear/Array` | 封印时长 | `SealTrait` |
| `undyingMaxResurrections/SealDuration` | 不死次数/封印时长 | `UndyingTraitMixin` |
| `showHud` | 自定义血条 HUD | `L2HHealthOverlay` |
| `levelCapEnabled/Thresholds/PerTrait` | 等级上限 | `TraitPostRollMixin` |
| `legendaryEnabled/Thresholds/ExtraIds` | 传奇限制 | `TraitPostRollMixin` |
| `exclusionEnabled/Groups` | 词条互斥 | `TraitPostRollMixin` + `TraitGenerationHelper` |
| `disableNonPresetTraits` | 仅保留预设 | `TraitPostRollMixin` |
| `disableAllTraits` | 禁用词条生成 | `TraitPostRollMixin` |
| `disableMobLevel` | 禁用生物等级 | `TraitManagerMixin` |
| `playerMaxTraits/SelfTraitEnabled/Balance/Cost` | 玩家词条 | `TraitSymbolSelfUseMixin` |
| `playerTraitLimitEnabled/BudgetRatio` | 生物词条预算 | `TraitSymbolBudgetMixin` |

### `ClientL2HConfig.java`（CLIENT）
| 配置项 | 用途 |
|---|---|
| `hudXOffset/hudYOffset/hudRange` | HUD 位置和范围 |
| `hideHudWithBossbar` | 有 BossBar 时隐藏 HUD |
| `romanNumerals` | 词条等级罗马数字 |
| `gradientStrength/hudBarWidth` | 血条外观 |
| `colorSegments/defaultColor` | 血条颜色分阶 |

---

## 七、常见维护场景

### 添加一个新词条
1. `content/traits/` 下新建词条类
2. 如果是 KubeJS 构建器支持的，在 `compat/kubejs/` 加对应 Builder
3. 在 `L2HFKJSPlugin` 注册新类型
4. 不需要手动注册 DeferredRegister——通过词条生成器自动处理

### 添加一个新物品
1. `content/` 下新建物品类
2. `init/L2HFItems.java` 中加 `RegistryObject`
3. 在 `static` 块的物品列表和 `TABS` 中加上
4. `assets/l2hostility_tweaks/models/item/` 加模型 JSON
5. `assets/l2hostility_tweaks/textures/item/` 加纹理 PNG
6. `lang/` 加翻译

### 修改已有 Mixin
1. 先确认 `mixins.json` 中存在该 Mixin
2. 确认方法名带 `l2fix$` 前缀
3. 确认 `static` 修饰符与目标方法匹配
4. 如果修改了 @Redirect，检查是否会引起递归
5. 确认实际调用所在方法；`MobTraitCap.tick()` 的词条 tick 调用位于静态 `lambda$tick$4`。第三方类内的 Minecraft 调用需核对正式 refmap，恢复袋两个调用点使用 `@At(remap=true)`。

### JEI 与 HUD
- JEI 原生轮播、完整候选上下文、装备规则与同步、必定词条提示应与已有后续适配一起保留，不得仅按旧基线构建。
- JEI 的必定生成判定依照本体 `fullChance`：配置最低难度大于 0 且 suppression 为 0 时，生成及词条概率强制为 1。词条自身的动态 `allow()` 仍须单独满足，页面条件成立不能消除该限制。
- 前置预设即使不保证执行，也可能消耗后续预算；按潜在消费上界计算保证剩余预算，并避免同词条重复预扣。最终互斥过滤可能删除的预设不标为必定；展示修正不能退回已消费预算。
- HUD 延迟血条使用单调时钟和按经过时间求解的消退动画，保留 400ms 停留。同一时刻重复渲染不得再次扣减。

### 添加新配置项
1. `config/L2HConfig.java` 加 field + define + getter
2. `gradle.properties` 升级版本号（告诉用户配置变更）


### 修复验证记录
- 2026-10-09：完整 check 主测试 70 类、522 项，旧版适应测试 13 项，0 失败/错误；同一可选游戏 JAR fixture 用例各跳过一次。
- JEI 功能包对照、正式 jar/reobf、69 个 Mixin 注册和 refmap 验证通过；尚未游戏实测。
