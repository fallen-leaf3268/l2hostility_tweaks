# Player Shulker Friendly Target Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 防止玩家潜影贝词条把同盟实体和玩家拥有的宠物或召唤物选为最终目标。

**Architecture:** 在 `ShulkerTraitMixin` 中提取包内可测试的友方事实合并函数。选靶循环读取双向同盟关系和 `OwnableEntity` 所有者 UUID，在距离与视线评分前跳过友方；其他选靶参数和射击流程保持不变。

**Tech Stack:** Java 17、Forge 1.20.1、Mixin、JUnit 5、Gradle

## Global Constraints

- 排除候选认为玩家是同盟、玩家认为候选是同盟、或候选由玩家拥有的实体。
- 中立生物仍可被玩家主动瞄准。
- 不修改生物原版仇恨目标、子弹伤害、射击间隔或范围。
- 使用 TDD，先观察测试失败，再修改生产代码。
- 输出 JAR 到 `C:\Users\Lenovo\Desktop\Ai_Run\output`，不部署到 `mods`。

---

### Task 1: 增加玩家潜影贝友方过滤

**Files:**
- Create: `src/test/java/com/l2hostility_tweaks/mixin/ShulkerTraitMixinTest.java`
- Modify: `src/main/java/com/l2hostility_tweaks/mixin/ShulkerTraitMixin.java`

**Interfaces:**
- Consumes: 候选到玩家的同盟关系、玩家到候选的同盟关系、候选是否由玩家拥有。
- Produces: `static boolean l2fix$isFriendlyCandidate(boolean candidateAlliedToPlayer, boolean playerAlliedToCandidate, boolean ownedByPlayer)`。

- [ ] **Step 1: 写入失败回归测试**

```java
package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShulkerTraitMixinTest {

    @Test
    void rejectsAlliedAndPlayerOwnedCandidates() {
        assertFalse(ShulkerTraitMixin.l2fix$isFriendlyCandidate(false, false, false));
        assertTrue(ShulkerTraitMixin.l2fix$isFriendlyCandidate(true, false, false));
        assertTrue(ShulkerTraitMixin.l2fix$isFriendlyCandidate(false, true, false));
        assertTrue(ShulkerTraitMixin.l2fix$isFriendlyCandidate(false, false, true));
    }
}
```

- [ ] **Step 2: 运行定向测试并确认 RED**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.mixin.ShulkerTraitMixinTest`

Expected: `compileTestJava` 失败，原因是 `l2fix$isFriendlyCandidate(boolean, boolean, boolean)` 尚不存在。

- [ ] **Step 3: 写入最小实现并接入选靶循环**

增加纯函数：

```java
@Unique
static boolean l2fix$isFriendlyCandidate(boolean candidateAlliedToPlayer,
        boolean playerAlliedToCandidate, boolean ownedByPlayer) {
    return candidateAlliedToPlayer || playerAlliedToCandidate || ownedByPlayer;
}
```

在候选转换为 `LivingEntity` 后读取关系：

```java
boolean ownedByPlayer = entity instanceof OwnableEntity ownable
        && player.getUUID().equals(ownable.getOwnerUUID());
if (l2fix$isFriendlyCandidate(
        entity.isAlliedTo(player), player.isAlliedTo(entity), ownedByPlayer)) {
    continue;
}
```

该过滤必须位于距离、夹角和评分之前。

- [ ] **Step 4: 运行定向测试并确认 GREEN**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.mixin.ShulkerTraitMixinTest`

Expected: 1 test completed，0 failed。

- [ ] **Step 5: 提交修复**

```powershell
git add -- src/main/java/com/l2hostility_tweaks/mixin/ShulkerTraitMixin.java src/test/java/com/l2hostility_tweaks/mixin/ShulkerTraitMixinTest.java
git commit -m "fix: exclude friendly player shulker targets"
```

### Task 2: 完整验证与输出 JAR

**Files:**
- Verify: `build/libs/l2hostility_tweaks-1.0.0.jar`
- Output: `C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar`

**Interfaces:**
- Consumes: Task 1 已通过测试的源码。
- Produces: 含玩家潜影贝友方保护的 Forge 模组 JAR 与 SHA-256。

- [ ] **Step 1: 运行完整干净构建**

Run: `.\gradlew.bat clean build`

Expected: `BUILD SUCCESSFUL`，所有测试 0 failed，资源和 Mixin 注册校验通过。

- [ ] **Step 2: 复制并校验 JAR**

将 `build/libs/l2hostility_tweaks-1.0.0.jar` 覆盖复制到 `C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar`，读取大小与 SHA-256，并用 `jar tf` 确认包含 `com/l2hostility_tweaks/mixin/ShulkerTraitMixin.class`。

- [ ] **Step 3: 清理构建日志并检查工作树**

若测试生成未跟踪的 `logs/`，确认其中仅有本次构建日志后删除。运行 `git status --short`，预期无未提交修改。
