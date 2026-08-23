# Safe NBT Preset Trait ID Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让单个格式非法的 NBT 预设词条 ID 被安全跳过，并继续应用其后的合法预设词条。

**Architecture:** 在 `TraitGeneratorMixin` 内增加一个包可见的纯解析方法，使用 `ResourceLocation.tryParse` 将外部字符串转换为资源位置或 `null`。预设循环在注册表查询前建立单条错误边界，保留现有未注册词条处理、等级比较和最外层异常保护。

**Tech Stack:** Java 17、Minecraft Forge 1.20.1、Mixin 0.8.5、JUnit 5、Gradle 8.5

## Global Constraints

- 合法预设的等级覆盖、初始化和计数行为保持不变。
- 格式非法或空 ID 只跳过当前条目，后续预设继续处理。
- 合法但未注册的 ID 保留现有警告和跳过行为。
- 不修改 NBT 匹配、配置合并或反射字段发现逻辑。
- 不新增运行时依赖，不部署 JAR 到 `mods`。
- 用户可交付 JAR 复制到 `C:\Users\Lenovo\Desktop\Ai_Run\output`。

---

### Task 1: 安全解析与单条隔离

**Files:**
- Modify: `src/test/java/com/l2hostility_tweaks/mixin/TraitGeneratorMixinTest.java`
- Modify: `src/main/java/com/l2hostility_tweaks/mixin/TraitGeneratorMixin.java`

**Interfaces:**
- Consumes: NBT 预设反射得到的 `String traitId`
- Produces: `static ResourceLocation l2fix$parseTraitId(String traitId)`，成功返回资源位置，格式非法或空输入返回 `null`

- [x] **Step 1: 编写失败测试**

在现有 `TraitGeneratorMixinTest` 中增加导入：

```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
```

增加三个测试：

```java
@Test
void parsesValidNbtPresetTraitId() {
    assertEquals("l2hostility:tank",
            TraitGeneratorMixin.l2fix$parseTraitId("l2hostility:tank").toString());
}

@Test
void rejectsMalformedNbtPresetTraitIdWithoutThrowing() {
    assertNull(TraitGeneratorMixin.l2fix$parseTraitId("Invalid Trait ID"));
}

@Test
void rejectsNullNbtPresetTraitId() {
    assertNull(TraitGeneratorMixin.l2fix$parseTraitId(null));
}
```

- [x] **Step 2: 运行聚焦测试并确认 RED**

Run:

```powershell
.\gradlew.bat test --tests "com.l2hostility_tweaks.mixin.TraitGeneratorMixinTest"
```

Expected: 编译失败，指出 `l2fix$parseTraitId(String)` 尚不存在。

- [x] **Step 3: 添加最小安全解析方法**

在 `TraitGeneratorMixin` 中增加：

```java
@Unique
static ResourceLocation l2fix$parseTraitId(String traitId) {
    return traitId == null ? null : ResourceLocation.tryParse(traitId);
}
```

- [x] **Step 4: 在预设循环中建立单条错误边界**

将直接注册表查询：

```java
MobTrait mt = traitReg.get(new ResourceLocation(traitId));
```

替换为：

```java
ResourceLocation traitLocation = l2fix$parseTraitId(traitId);
if (traitLocation == null) {
    LOG.warn("[NbtPresetGen] Trait id '{}' is invalid, skipping", traitId);
    continue;
}

MobTrait mt = traitReg.get(traitLocation);
```

保留其后的未注册判断、等级比较、初始化和外层异常捕获。

- [x] **Step 5: 运行聚焦测试并确认 GREEN**

Run:

```powershell
.\gradlew.bat test --tests "com.l2hostility_tweaks.mixin.TraitGeneratorMixinTest"
```

Expected: `BUILD SUCCESSFUL`，现有等级测试和新增 3 项解析测试全部通过。

- [x] **Step 6: 静态回归检查**

Run:

```powershell
$path = 'src/main/java/com/l2hostility_tweaks/mixin/TraitGeneratorMixin.java'
if (Select-String -Quiet -LiteralPath $path -Pattern 'new ResourceLocation\(traitId\)') {
    throw 'Unsafe NBT preset trait ID constructor remains'
}
if (-not (Select-String -Quiet -LiteralPath $path -Pattern 'ResourceLocation\.tryParse\(traitId\)')) {
    throw 'Safe NBT preset trait ID parser is missing'
}
```

Expected: 无输出且退出码为 `0`。

### Task 2: 全量验证、交付与提交

**Files:**
- Verify: `src/main/java/com/l2hostility_tweaks/mixin/TraitGeneratorMixin.java`
- Verify: `src/test/java/com/l2hostility_tweaks/mixin/TraitGeneratorMixinTest.java`
- Deliver: `C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar`

**Interfaces:**
- Consumes: Task 1 的生产代码和回归测试
- Produces: 通过完整验证的 JAR 与独立 Git 提交

- [x] **Step 1: 执行完整构建**

Run:

```powershell
.\gradlew.bat clean build
```

Expected: `BUILD SUCCESSFUL`，全部测试失败数为 `0`。

- [x] **Step 2: 检查差异质量和需求覆盖**

Run:

```powershell
git diff --check
git diff --stat
git status --short
```

Expected: 差异仅包含实施计划、`TraitGeneratorMixin` 和对应测试，无空白错误。

- [x] **Step 3: 更新输出 JAR**

Run:

```powershell
New-Item -ItemType Directory -Force -Path 'C:\Users\Lenovo\Desktop\Ai_Run\output' | Out-Null
Copy-Item -Force -LiteralPath 'build\libs\l2hostility_tweaks-1.0.0.jar' -Destination 'C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar'
```

Expected: 输出 JAR 存在、长度大于 `0`，且没有写入 `mods`。

- [x] **Step 4: 清理日志并提交**

仅当当前工作树根目录下的 `logs` 存在时，校验其绝对路径位于工作树内后删除。随后运行：

```powershell
git add docs/superpowers/plans/2026-08-24-safe-nbt-preset-trait-id.md `
  src/main/java/com/l2hostility_tweaks/mixin/TraitGeneratorMixin.java `
  src/test/java/com/l2hostility_tweaks/mixin/TraitGeneratorMixinTest.java
git commit -m "fix: safely parse NBT preset trait ids"
```

- [x] **Step 5: 最终核验**

Run:

```powershell
git status --short
git log -2 --oneline
Get-FileHash -Algorithm SHA256 -LiteralPath 'C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar'
```

Expected: 工作树干净，最新两条提交为实现提交和设计提交，输出 JAR 存在。
