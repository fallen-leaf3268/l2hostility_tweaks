# Killer Aura Targeting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 统一杀戮光环目标选择，永远排除持有者本人，并移除失效的旧目标 Redirect。

**Architecture:** `KillerAuraTraitMixin` 在实际伤害循环中计算实体关系，并调用纯布尔决策方法。决策方法用 JUnit 覆盖全部关系；旧 `KillerAuraSelfMixin` 源文件和注册项一起删除，消除失效 Redirect 与未清理的 `ThreadLocal`。

**Tech Stack:** Java 17、Forge 1.20.1、Sponge Mixin、Gradle 8.5、JUnit 5

## Global Constraints

- 自身排除优先于全部其他目标关系。
- 保留对其他非创造玩家、双方当前目标及玩家最近攻击目标的行为。
- 不改变伤害、间隔、范围、粒子、距离和阿布拉卡达布拉保护。
- 不新增配置或运行时依赖，不把 JAR 部署到 `mods`。

---

### Task 1: 锁定目标选择规则

**Files:**
- Create: `src/test/java/com/l2hostility_tweaks/mixin/KillerAuraTraitMixinTest.java`

**Interfaces:**
- Consumes: 计划新增的 `KillerAuraTraitMixin.l2fix$shouldTarget(boolean, boolean, boolean, boolean, boolean)`
- Produces: 自身否决和四类有效目标关系的回归测试

- [x] **Step 1: 创建完整失败测试**

```java
package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KillerAuraTraitMixinTest {

    @Test
    void alwaysRejectsHolderItself() {
        assertFalse(KillerAuraTraitMixin.l2fix$shouldTarget(true, true, true, true, true));
    }

    @Test
    void targetsOtherNonCreativePlayer() {
        assertTrue(KillerAuraTraitMixin.l2fix$shouldTarget(false, true, false, false, false));
    }

    @Test
    void targetsMobAttackingHolder() {
        assertTrue(KillerAuraTraitMixin.l2fix$shouldTarget(false, false, true, false, false));
    }

    @Test
    void targetsCurrentTargetOfMobHolder() {
        assertTrue(KillerAuraTraitMixin.l2fix$shouldTarget(false, false, false, true, false));
    }

    @Test
    void targetsMobRecentlyHitByPlayerHolder() {
        assertTrue(KillerAuraTraitMixin.l2fix$shouldTarget(false, false, false, false, true));
    }

    @Test
    void rejectsEntityWithoutTargetRelationship() {
        assertFalse(KillerAuraTraitMixin.l2fix$shouldTarget(false, false, false, false, false));
    }
}
```

- [x] **Step 2: 运行聚焦测试并确认准确红灯**

Run: `./gradlew test --tests com.l2hostility_tweaks.mixin.KillerAuraTraitMixinTest --rerun-tasks`

Expected: 测试编译因 `l2fix$shouldTarget` 尚不存在而失败，证明测试要求的是新统一决策入口。

### Task 2: 合并实际伤害循环的目标判断

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/mixin/KillerAuraTraitMixin.java`
- Test: `src/test/java/com/l2hostility_tweaks/mixin/KillerAuraTraitMixinTest.java`

**Interfaces:**
- Consumes: 五个关系布尔值
- Produces: `static boolean l2fix$shouldTarget(boolean self, boolean nonCreativePlayer, boolean candidateTargetsHolder, boolean holderTargetsCandidate, boolean recentlyHitByPlayerHolder)`

- [x] **Step 1: 添加纯目标决策方法**

```java
@Unique
static boolean l2fix$shouldTarget(boolean self, boolean nonCreativePlayer,
                                  boolean candidateTargetsHolder, boolean holderTargetsCandidate,
                                  boolean recentlyHitByPlayerHolder) {
    return !self && (nonCreativePlayer || candidateTargetsHolder ||
            holderTargetsCandidate || recentlyHitByPlayerHolder);
}
```

- [x] **Step 2: 在实际循环中计算并使用全部关系**

将现有复合 `if` 替换为：

```java
boolean nonCreativePlayer = e instanceof Player player && !player.getAbilities().instabuild;
boolean candidateTargetsHolder = e instanceof Mob candidateMob && candidateMob.getTarget() == mob;
boolean holderTargetsCandidate = mob instanceof Mob holderMob && holderMob.getTarget() == e;
boolean recentlyHitByPlayerHolder = mob instanceof Player && e instanceof Mob candidateMob &&
        candidateMob.getLastHurtByMob() == mob;
if (!l2fix$shouldTarget(e == mob, nonCreativePlayer, candidateTargetsHolder,
        holderTargetsCandidate, recentlyHitByPlayerHolder)) {
    continue;
}
if (e.distanceTo(mob) > range) continue;
if (LHItems.ABRAHADABRA.get().isOn(e)) continue;
TraitEffectCache cache = new TraitEffectCache(e);
cap.traitEvent((k, v) -> k.postHurtPlayer(v, mob, cache));
e.hurt(new DamageSource(LHDamageTypes.forKey(mob.level(), LHDamageTypes.KILLER_AURA), null, mob), damage);
```

- [x] **Step 3: 运行聚焦测试并确认转绿**

Run: `./gradlew test --tests com.l2hostility_tweaks.mixin.KillerAuraTraitMixinTest --rerun-tasks`

Expected: 六个目标选择测试全部通过。

### Task 3: 删除失效 Mixin 并完成验证

**Files:**
- Delete: `src/main/java/com/l2hostility_tweaks/mixin/KillerAuraSelfMixin.java`
- Modify: `src/main/resources/l2hostility_tweaks.mixins.json`
- Modify: `docs/superpowers/plans/2026-08-24-killer-aura-targeting.md`
- Output: `C:/Users/Lenovo/Desktop/Ai_Run/output/l2hostility_tweaks-1.0.0.jar`

**Interfaces:**
- Consumes: Task 2 的统一目标判断
- Produces: 无失效 Redirect、无残留 ThreadLocal、注册与源码一致的发布 JAR

- [x] **Step 1: 删除旧源码与注册项**

删除整个 `KillerAuraSelfMixin.java`，并从 `mixins` 数组删除字符串 `KillerAuraSelfMixin`。不得移动其中的 `ThreadLocal` 或 Redirect。

- [x] **Step 2: 执行完整构建**

Run: `./gradlew clean build`

Expected: 单元测试、Mixin 注册资源校验、重混淆和 JAR 构建全部成功。

- [x] **Step 3: 核对测试和发布内容**

统计 `build/test-results/test/TEST-*.xml`。读取 JAR 条目，确认不存在 `KillerAuraSelfMixin.class` 和 `dev/latvian/`。

- [x] **Step 4: 输出 JAR 并清理生成日志**

将 `build/libs/l2hostility_tweaks-1.0.0.jar` 覆盖复制到 `C:/Users/Lenovo/Desktop/Ai_Run/output/`。仅删除经绝对路径验证、位于当前工作树内的未跟踪 `logs` 目录。

- [x] **Step 5: 更新计划并创建独立提交**

```bash
git add docs/superpowers/plans/2026-08-24-killer-aura-targeting.md \
  src/main/java/com/l2hostility_tweaks/mixin/KillerAuraTraitMixin.java \
  src/main/java/com/l2hostility_tweaks/mixin/KillerAuraSelfMixin.java \
  src/main/resources/l2hostility_tweaks.mixins.json \
  src/test/java/com/l2hostility_tweaks/mixin/KillerAuraTraitMixinTest.java
git diff --cached --check
git commit -m "fix: unify killer aura target selection"
```

- [x] **Step 6: 最终状态核验**

确认工作树干净，记录提交短哈希、测试统计、JAR 大小和 SHA-256。
