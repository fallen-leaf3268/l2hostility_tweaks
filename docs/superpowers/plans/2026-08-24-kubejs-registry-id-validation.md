# KubeJS Registry ID Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 KubeJS Builder 配置阶段安全解析药水与属性 ID，避免无效配置在 Tooltip 或战斗阶段抛出异常。

**Architecture:** 新增一个无状态的泛型注册表解析辅助类，通过 `ResourceLocation.tryParse` 和调用方提供的注册表查询函数返回已解析对象或 `null`，并统一记录一次错误。四个 Builder 在接收脚本参数时调用该辅助类，后续只保存有效对象或现有默认回退。

**Tech Stack:** Java 17、Minecraft Forge 1.20.1、KubeJS、JUnit 5、Gradle 8.5

## Global Constraints

- 合法 KubeJS 脚本的效果、数值、颜色和 Tooltip 文案保持不变。
- 无效药水 ID 保留现有默认虚弱效果；无效属性 ID 跳过该属性条目。
- 每个错误输入只在 Builder 配置阶段记录一次，不在 Tooltip 或战斗阶段重复解析。
- 不新增运行时依赖，不部署 JAR 到 `mods`。
- 用户可交付 JAR 复制到 `C:\Users\Lenovo\Desktop\Ai_Run\output`。

---

### Task 1: 可测试的安全注册表解析器

**Files:**
- Create: `src/main/java/com/l2hostility_tweaks/compat/kubejs/KubeJsRegistryResolver.java`
- Create: `src/test/java/com/l2hostility_tweaks/compat/kubejs/KubeJsRegistryResolverTest.java`

**Interfaces:**
- Consumes: `String registryType`、`String id`、`Function<ResourceLocation, T> lookup`
- Produces: `static <T> T resolve(String registryType, String id, Function<ResourceLocation, T> lookup)`，成功时返回注册对象，失败时记录错误并返回 `null`

- [x] **Step 1: 编写失败测试**

```java
package com.l2hostility_tweaks.compat.kubejs;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class KubeJsRegistryResolverTest {

    @Test
    void rejectsMalformedIdWithoutCallingRegistry() {
        AtomicBoolean called = new AtomicBoolean();

        Object result = KubeJsRegistryResolver.resolve("mob effect", "Invalid Effect ID", id -> {
            called.set(true);
            return new Object();
        });

        assertNull(result);
        assertFalse(called.get());
    }

    @Test
    void returnsRegisteredValue() {
        Object expected = new Object();

        Object result = KubeJsRegistryResolver.resolve("mob effect", "minecraft:weakness",
                id -> "minecraft:weakness".equals(id.toString()) ? expected : null);

        assertSame(expected, result);
    }

    @Test
    void rejectsUnregisteredValue() {
        assertNull(KubeJsRegistryResolver.resolve("attribute", "othermod:missing", id -> null));
    }

    @Test
    void rejectsNullIdWithoutCallingRegistry() {
        AtomicBoolean called = new AtomicBoolean();

        Object result = KubeJsRegistryResolver.resolve("attribute", null, id -> {
            called.set(true);
            return new Object();
        });

        assertNull(result);
        assertFalse(called.get());
    }
}
```

- [x] **Step 2: 运行测试并确认 RED**

Run:

```powershell
.\gradlew.bat test --tests "com.l2hostility_tweaks.compat.kubejs.KubeJsRegistryResolverTest"
```

Expected: 编译失败，指出 `KubeJsRegistryResolver` 尚不存在。

- [x] **Step 3: 实现最小解析器**

```java
package com.l2hostility_tweaks.compat.kubejs;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public final class KubeJsRegistryResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger("l2htweaks:kubejs");

    private KubeJsRegistryResolver() {
    }

    public static <T> T resolve(String registryType, String id, Function<ResourceLocation, T> lookup) {
        ResourceLocation location = id == null ? null : ResourceLocation.tryParse(id);
        if (location == null) {
            LOGGER.error("Invalid {} id: {}", registryType, id);
            return null;
        }
        T value = lookup.apply(location);
        if (value == null) {
            LOGGER.error("Unknown {}: {}", registryType, id);
        }
        return value;
    }
}
```

- [x] **Step 4: 运行聚焦测试并确认 GREEN**

Run:

```powershell
.\gradlew.bat test --tests "com.l2hostility_tweaks.compat.kubejs.KubeJsRegistryResolverTest"
```

Expected: `BUILD SUCCESSFUL`，4 项测试全部通过。

### Task 2: Builder 配置阶段一次性解析

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/compat/kubejs/SelfEffectTraitBuilder.java`
- Modify: `src/main/java/com/l2hostility_tweaks/compat/kubejs/LegendarySelfEffectTraitBuilder.java`
- Modify: `src/main/java/com/l2hostility_tweaks/compat/kubejs/LegendaryTargetEffectTraitBuilder.java`
- Modify: `src/main/java/com/l2hostility_tweaks/compat/kubejs/LegendaryAttributeTraitBuilder.java`

**Interfaces:**
- Consumes: Task 1 的 `KubeJsRegistryResolver.resolve(...)`
- Produces: Builder 内只保存已注册对象；无效药水保持虚弱回退，无效属性不进入条目列表

- [x] **Step 1: 修改 SelfEffect Builder**

将两个自我效果 Builder 的 `effect(String id)` 改为：

```java
var eff = KubeJsRegistryResolver.resolve("mob effect", id, ForgeRegistries.MOB_EFFECTS::getValue);
if (eff != null) this.effect = () -> eff;
return this;
```

- [x] **Step 2: 修改 LegendaryTargetEffect Builder**

在 `fixedLevel` 和 `fixedDuration` 调用时立即解析药水。解析失败时将 `func` 设置为现有虚弱回退函数；成功时闭包只捕获解析完成的 `MobEffect`，不再访问注册表。删除该 Builder 自己的 Logger。

`fixedLevel` 的核心实现：

```java
var mobEffect = KubeJsRegistryResolver.resolve("mob effect", effect,
        ForgeRegistries.MOB_EFFECTS::getValue);
if (mobEffect == null) {
    this.func = i -> new MobEffectInstance(MobEffects.WEAKNESS, 100, i - 1);
} else {
    this.func = i -> new MobEffectInstance(mobEffect, duration * i, amplifier);
}
return this;
```

`fixedDuration` 使用相同分支，成功分支为：

```java
this.func = i -> new MobEffectInstance(mobEffect, duration, i - 1);
```

- [x] **Step 3: 修改 LegendaryAttribute Builder**

在 `attribute(...)` 中先解析属性：

```java
var resolved = KubeJsRegistryResolver.resolve("attribute", attribute,
        ForgeRegistries.ATTRIBUTES::getValue);
if (resolved == null) return this;
```

随后把条目 Supplier 从延迟注册表查询改为稳定对象：

```java
name, () -> resolved, () -> factor, op
```

- [x] **Step 4: 静态回归检查**

Run:

```powershell
$files = Get-ChildItem 'src/main/java/com/l2hostility_tweaks/compat/kubejs' -Filter '*TraitBuilder.java'
$unsafe = $files | Select-String -Pattern 'getValue\(new ResourceLocation\((id|effect|attribute)\)\)'
if ($unsafe) { $unsafe; throw 'Unsafe delayed registry ID parsing remains' }
```

Expected: 无匹配且退出码为 `0`。

- [x] **Step 5: 运行聚焦测试**

Run:

```powershell
.\gradlew.bat test --tests "com.l2hostility_tweaks.compat.kubejs.KubeJsRegistryResolverTest"
```

Expected: `BUILD SUCCESSFUL`。

### Task 3: 全量验证、交付与提交

**Files:**
- Verify: `src/main/java/com/l2hostility_tweaks/compat/kubejs/*.java`
- Verify: `src/test/java/com/l2hostility_tweaks/compat/kubejs/KubeJsRegistryResolverTest.java`
- Deliver: `C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar`

**Interfaces:**
- Consumes: Task 1–2 的生产代码和测试
- Produces: 通过全量验证的 JAR 与独立 Git 提交

- [x] **Step 1: 执行完整构建**

Run:

```powershell
.\gradlew.bat clean build
```

Expected: `BUILD SUCCESSFUL`，所有测试失败数为 `0`。

- [x] **Step 2: 检查差异质量与需求覆盖**

Run:

```powershell
git diff --check
git diff --stat
git status --short
```

Expected: 无空白错误，差异只包含计划文件、解析器、四个 Builder 和测试。

- [x] **Step 3: 复制构建产物到输出目录**

Run:

```powershell
New-Item -ItemType Directory -Force -Path 'C:\Users\Lenovo\Desktop\Ai_Run\output' | Out-Null
Copy-Item -Force -LiteralPath 'build\libs\l2hostility_tweaks-1.0.0.jar' -Destination 'C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar'
```

Expected: 输出 JAR 存在且长度大于 `0`，没有写入 `mods`。

- [x] **Step 4: 清理生成日志并提交**

仅当工作树根目录下的 `logs` 存在时，验证其绝对路径位于当前工作树后删除。随后运行：

```powershell
git add docs/superpowers/plans/2026-08-24-kubejs-registry-id-validation.md `
  src/main/java/com/l2hostility_tweaks/compat/kubejs/KubeJsRegistryResolver.java `
  src/main/java/com/l2hostility_tweaks/compat/kubejs/SelfEffectTraitBuilder.java `
  src/main/java/com/l2hostility_tweaks/compat/kubejs/LegendarySelfEffectTraitBuilder.java `
  src/main/java/com/l2hostility_tweaks/compat/kubejs/LegendaryTargetEffectTraitBuilder.java `
  src/main/java/com/l2hostility_tweaks/compat/kubejs/LegendaryAttributeTraitBuilder.java `
  src/test/java/com/l2hostility_tweaks/compat/kubejs/KubeJsRegistryResolverTest.java
git commit -m "fix: safely resolve KubeJS registry ids"
```

- [x] **Step 5: 最终核验**

Run:

```powershell
git status --short
git log -2 --oneline
Get-Item -LiteralPath 'C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar'
```

Expected: 工作树干净，最新两条提交分别是实现提交和设计提交，输出 JAR 存在。
