# 复印线性反制计算修复实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复线性复印反制在总点数恰好为 `-1` 时产生额外减伤的问题，并让玩家与非玩家复印共用同一个可测试计算器。

**Architecture:** 新增纯 Java `ReprintDamageCalculator`，用明确的布尔状态区分指数模式的 30 级以上附魔，不再使用 `-1` 哨兵。`ReprintTraitMixin` 只负责收集装备附魔和执行游戏副作用，玩家与非玩家路径都把计算输入交给该计算器。

**Tech Stack:** Java 17、Forge 1.20.1、Mixin 0.8.5、JUnit 5、Gradle

## Global Constraints

- 线性模式的有效附魔点数为 `max(0, 普通附魔总等级 - 复印反制总等级)`。
- 指数模式只有实际遇到 30 级以上普通附魔时才走高等级特殊计算。
- 附魔复制、虚空之触、消失诅咒和复印反制护甲减伤保持现有行为。
- 不修改配置范围、翻译文本或附魔最大等级。
- JAR 只输出到 `C:\Users\Lenovo\Desktop\Ai_Run\output`，不部署到 `mods`。

---

### Task 1: 纯复印伤害计算器

**Files:**
- Create: `src/main/java/com/l2hostility_tweaks/util/ReprintDamageCalculator.java`
- Create: `src/test/java/com/l2hostility_tweaks/util/ReprintDamageCalculatorTest.java`

**Interfaces:**
- Consumes: `boolean linear` 与 `Iterable<ReprintDamageCalculator.Point>`。
- Produces: `ReprintDamageCalculator.calculate(boolean, Iterable<Point>)`，返回包含 `float factor` 和 `int maxLevel` 的 `Result`。

- [ ] **Step 1: 写线性与指数模式失败测试**

创建测试，覆盖线性 `-1`、`-2`、正好抵消、正常正值，以及指数普通计算、反制抵消和真实 30 级高附魔：

```java
package com.l2hostility_tweaks.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReprintDamageCalculatorTest {

    @Test
    void clampsLinearMinusOneToZero() {
        var result = ReprintDamageCalculator.calculate(true, List.of(
                new ReprintDamageCalculator.Point(1, true)));

        assertEquals(0.0f, result.factor());
        assertEquals(0, result.maxLevel());
    }

    @Test
    void clampsLinearMinusTwoToZero() {
        var result = ReprintDamageCalculator.calculate(true, List.of(
                new ReprintDamageCalculator.Point(2, true)));

        assertEquals(0.0f, result.factor());
    }

    @Test
    void clampsExactlyCancelledLinearPointsToZero() {
        var result = ReprintDamageCalculator.calculate(true, List.of(
                new ReprintDamageCalculator.Point(3, false),
                new ReprintDamageCalculator.Point(3, true)));

        assertEquals(0.0f, result.factor());
        assertEquals(3, result.maxLevel());
    }

    @Test
    void sumsPositiveLinearPoints() {
        var result = ReprintDamageCalculator.calculate(true, List.of(
                new ReprintDamageCalculator.Point(4, false),
                new ReprintDamageCalculator.Point(2, false),
                new ReprintDamageCalculator.Point(1, true)));

        assertEquals(5.0f, result.factor());
        assertEquals(4, result.maxLevel());
    }

    @Test
    void preservesOrdinaryExponentialCalculation() {
        var result = ReprintDamageCalculator.calculate(false, List.of(
                new ReprintDamageCalculator.Point(1, false),
                new ReprintDamageCalculator.Point(3, false)));

        assertEquals(5.0f, result.factor());
        assertEquals(3, result.maxLevel());
    }

    @Test
    void clampsOverCancelledExponentialPointsToZero() {
        var result = ReprintDamageCalculator.calculate(false, List.of(
                new ReprintDamageCalculator.Point(1, true)));

        assertEquals(0.0f, result.factor());
        assertEquals(0, result.maxLevel());
    }

    @Test
    void preservesHighLevelExponentialFallback() {
        var result = ReprintDamageCalculator.calculate(false, List.of(
                new ReprintDamageCalculator.Point(30, false),
                new ReprintDamageCalculator.Point(2, true)));

        assertEquals((float) Math.pow(2, 27), result.factor());
        assertEquals(30, result.maxLevel());
    }
}
```

- [ ] **Step 2: 运行测试并确认先失败**

Run: `./gradlew test --tests com.l2hostility_tweaks.util.ReprintDamageCalculatorTest`

Expected: FAIL，编译器报告 `ReprintDamageCalculator` 不存在。

- [ ] **Step 3: 实现最小纯计算器**

```java
package com.l2hostility_tweaks.util;

public final class ReprintDamageCalculator {

    private ReprintDamageCalculator() {
    }

    public static Result calculate(boolean linear, Iterable<Point> points) {
        long total = 0;
        int maxLevel = 0;
        int counterTotal = 0;
        boolean highLevel = false;

        for (Point point : points) {
            int level = point.level();
            if (point.counter()) {
                counterTotal += level;
                if (linear) {
                    total -= level;
                } else if (!highLevel && total >= 0) {
                    total -= 1L << (level - 1);
                }
                continue;
            }

            maxLevel = Math.max(maxLevel, level);
            if (linear) {
                total += level;
            } else if (level >= 30) {
                highLevel = true;
            } else if (!highLevel && total >= 0) {
                total += 1L << (level - 1);
            }
        }

        if (linear) {
            return new Result((float) Math.max(0, total), maxLevel);
        }
        if (highLevel) {
            int exponent = Math.max(0, maxLevel - 1 - counterTotal);
            return new Result((float) Math.pow(2, exponent), maxLevel);
        }
        return new Result((float) Math.max(0, total), maxLevel);
    }

    public record Point(int level, boolean counter) {
    }

    public record Result(float factor, int maxLevel) {
    }
}
```

- [ ] **Step 4: 运行定向测试并确认通过**

Run: `./gradlew test --tests com.l2hostility_tweaks.util.ReprintDamageCalculatorTest`

Expected: 7 tests completed，0 failed。

- [ ] **Step 5: 提交纯计算器**

```bash
git add src/main/java/com/l2hostility_tweaks/util/ReprintDamageCalculator.java src/test/java/com/l2hostility_tweaks/util/ReprintDamageCalculatorTest.java
git commit -m "fix: normalize reprint counter points"
```

### Task 2: 接入玩家与非玩家复印路径

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/mixin/ReprintTraitMixin.java`
- Create: `src/test/java/com/l2hostility_tweaks/mixin/ReprintTraitMixinIntegrationTest.java`

**Interfaces:**
- Consumes: Task 1 的 `ReprintDamageCalculator.Point`、`Result` 和 `calculate`。
- Produces: 玩家与非玩家路径各一次统一计算器调用；不再保留 `l2fix$antiReprintTotal`、`l2fix$antiReprintArmor`、`l2fix$linear`、`l2fix$hasCounter` 实例状态。

- [ ] **Step 1: 写源码结构失败测试**

```java
package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ReprintTraitMixinIntegrationTest {

    private static final Path SOURCE = Path.of(
            "src/main/java/com/l2hostility_tweaks/mixin/ReprintTraitMixin.java");

    @Test
    void bothReprintPathsUseSharedCalculator() throws IOException {
        String source = Files.readString(SOURCE);

        assertEquals(2, count(source, "ReprintDamageCalculator.calculate("));
    }

    @Test
    void removesAmbiguousSentinelAndPerCallInstanceState() throws IOException {
        String source = Files.readString(SOURCE);

        assertFalse(source.contains("total != -1"));
        assertFalse(source.contains("private int l2fix$antiReprintTotal"));
        assertFalse(source.contains("private int l2fix$antiReprintArmor"));
        assertFalse(source.contains("private boolean l2fix$linear"));
        assertFalse(source.contains("private boolean l2fix$hasCounter"));
    }

    private static int count(String source, String needle) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
```

- [ ] **Step 2: 运行结构测试并确认先失败**

Run: `./gradlew test --tests com.l2hostility_tweaks.mixin.ReprintTraitMixinIntegrationTest`

Expected: FAIL；统一计算器调用次数为 0，且旧实例字段仍存在。

- [ ] **Step 3: 将入口状态改为局部变量**

在 `ReprintTraitMixin` 中导入 `ReprintDamageCalculator` 和 `ArrayList`，删除四个每次攻击使用的实例字段。将 `l2fix$head` 替换为：

```java
private void l2fix$head(int level, LivingEntity attacker, AttackCache cache, TraitEffectCache traitCache, CallbackInfo ci) {
    int antiReprintArmor = 0;
    boolean hasCounter = false;
    boolean linear = L2HConfig.isReprintLinearEnabled();

    Enchantment antiReprint = L2HFEnchantments.REPRINT_COUNTER.get();
    for (var slot : EquipmentSlot.values()) {
        ItemStack src = cache.getAttackTarget().getItemBySlot(slot);
        for (var e : src.getAllEnchantments().entrySet()) {
            if (e.getKey() == antiReprint) {
                hasCounter = true;
                if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                    antiReprintArmor = Math.max(antiReprintArmor, e.getValue());
                }
            }
        }
    }

    if (attacker instanceof Player) {
        ci.cancel();
        l2fix$playerReprint(attacker, cache, linear, antiReprintArmor);
        return;
    }

    if (linear || hasCounter) {
        ci.cancel();
        l2fix$handleReprint(attacker, cache, linear, antiReprintArmor);
    }
}
```

- [ ] **Step 4: 接入非玩家路径**

将 `l2fix$handleReprint` 改为接收 `linear` 和 `antiReprintArmor`，在现有装备循环中收集输入项。循环结束后调用：

```java
var result = ReprintDamageCalculator.calculate(linear, points);
int maxLv = result.maxLevel();
float factor = result.factor();
```

该方法使用以下输入收集逻辑，且原有 `ReprintHandler.reprint(dst, src)` 保留在同一槽位循环末尾：

```java
var points = new ArrayList<ReprintDamageCalculator.Point>();
for (var slot : EquipmentSlot.values()) {
    ItemStack dst = attacker.getItemBySlot(slot);
    ItemStack src = cache.getAttackTarget().getItemBySlot(slot);
    for (var e : src.getAllEnchantments().entrySet()) {
        points.add(new ReprintDamageCalculator.Point(e.getValue(), e.getKey() == antiReprint));
    }

    if (event != null && event.getSource().getDirectEntity() == attacker) {
        ReprintHandler.reprint(dst, src);
    }
}
```

虚空之触判定继续使用 `maxLv`；护甲减伤改用方法参数 `antiReprintArmor`；最终增伤继续使用 `factor`。

- [ ] **Step 5: 接入玩家路径**

将 `l2fix$playerReprint` 改为接收 `linear` 和 `antiReprintArmor`，只收集输入并调用同一计算器：

```java
private void l2fix$playerReprint(LivingEntity attacker, AttackCache cache,
        boolean linear, int antiReprintArmor) {
    Enchantment antiReprint = L2HFEnchantments.REPRINT_COUNTER.get();
    var points = new ArrayList<ReprintDamageCalculator.Point>();

    for (var slot : EquipmentSlot.values()) {
        ItemStack src = cache.getAttackTarget().getItemBySlot(slot);
        for (var e : src.getAllEnchantments().entrySet()) {
            points.add(new ReprintDamageCalculator.Point(e.getValue(), e.getKey() == antiReprint));
        }
    }

    float factor = ReprintDamageCalculator.calculate(linear, points).factor();
    if (antiReprintArmor > 0) {
        float reduction = antiReprintArmor * (float) L2HConfig.getAntiReprintReduction();
        cache.addHurtModifier(DamageModifier.multTotal(1 - Math.min(reduction, 0.8f)));
    }
    cache.addHurtModifier(DamageModifier.multTotal(1 + (float) (L2HConfig.getReprintDamage() * factor)));
}
```

- [ ] **Step 6: 运行定向测试**

Run: `./gradlew test --tests com.l2hostility_tweaks.util.ReprintDamageCalculatorTest --tests com.l2hostility_tweaks.mixin.ReprintTraitMixinIntegrationTest`

Expected: 9 tests completed，0 failed。

- [ ] **Step 7: 提交 Mixin 接入**

```bash
git add src/main/java/com/l2hostility_tweaks/mixin/ReprintTraitMixin.java src/test/java/com/l2hostility_tweaks/mixin/ReprintTraitMixinIntegrationTest.java
git commit -m "refactor: share reprint damage calculation"
```

### Task 3: 全量验证与 JAR 输出

**Files:**
- Verify: `src/main/resources/l2hostility_tweaks.mixins.json`
- Verify: `build/libs/l2hostility_tweaks-1.0.0.jar`
- Output: `C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar`

**Interfaces:**
- Consumes: Task 1 和 Task 2 的完整实现。
- Produces: 通过全量测试和构建的验证 JAR。

- [ ] **Step 1: 执行干净全量构建**

Run: `./gradlew clean test build`

Expected: `BUILD SUCCESSFUL`，所有测试 0 failures、0 errors、0 skipped。

- [ ] **Step 2: 校验 Mixin 配置和 JAR 内容**

Run: `./gradlew validateMixinConfig validateTooltipTranslations`

Expected: `BUILD SUCCESSFUL`。

Run: `D:\JAVA\JAVA_17\bin\jar.exe tf build/libs/l2hostility_tweaks-1.0.0.jar`

Expected: 同时包含 `com/l2hostility_tweaks/util/ReprintDamageCalculator.class`、`com/l2hostility_tweaks/mixin/ReprintTraitMixin.class` 和 `l2hostility_tweaks.mixins.json`。

- [ ] **Step 3: 输出验证 JAR 并计算摘要**

Run: `Copy-Item -LiteralPath 'build/libs/l2hostility_tweaks-1.0.0.jar' -Destination 'C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar' -Force`

Run: `Get-FileHash -Algorithm SHA256 'C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar'`

Expected: 输出文件存在并返回 SHA-256；不写入任何 `mods` 目录。

- [ ] **Step 4: 检查工作树状态**

Run: `git status --short`

Expected: 无未提交文件。
