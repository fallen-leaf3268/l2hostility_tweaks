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
4. 所有血量变动用**等比例**：`newHealth = oldHp/oldMax * newMax`

### 等比例血量位置（共 7 处）
```
TraitDisableHelper.setDisabled()       — 封印/解封
TraitSymbolSelfUseMixin                — 玩家自用词条
TraitSymbolMixin                       — 对生物用词条修正
TraitAdderWandMixin                    — 升级封印词条
TraitUnloaderWand                      — 卸载词条
```

---

## 二、API 对外表现

### `getTraitLevel(trait)` — 对外返回 0（GetTraitLevelMixin）
### `hasTrait(trait)` — 对外返回 true（词条存在，但等级为 0）

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
- 所有 Mixin 方法必须以 `l2fix$` 开头
- 示例：`private void l2fix$doSomething(...)`

### 类名后缀
- 功能修改：`*Mixin.java`（58 个）
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

### 添加新配置项
1. `config/L2HConfig.java` 加 field + define + getter
2. `gradle.properties` 升级版本号（告诉用户配置变更）
