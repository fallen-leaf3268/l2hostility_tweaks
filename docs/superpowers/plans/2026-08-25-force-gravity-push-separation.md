# Force And Gravity Push Separation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让普通推动只受力量免疫控制，防止单独的重力免疫错误获得击退免疫。

**Architecture:** 在 `PushCancelMixin` 中提取一个包内可测试的纯条件函数，显式接收力量和重力免疫状态，但只由力量免疫决定是否取消 `Entity.push(DDD)`。Mixin 调用该函数；现有 `GravityTraitMixin` 保持不变并继续单独处理重力词条。

**Tech Stack:** Java 17、Forge 1.20.1、Mixin、JUnit 5、Gradle

## Global Constraints

- `immune_to_force` 控制普通推动和击退免疫。
- `immune_to_gravity` 只控制重力词条免疫。
- 不修改标签 JSON、不新增配置、不修改 Tooltip。
- 内置恒静腰带和阿布拉哈达布拉仍同时拥有两种免疫。
- 使用 TDD，先观察测试失败，再修改生产代码。
- 输出 JAR 到 `C:\Users\Lenovo\Desktop\Ai_Run\output`，不部署到 `mods`。

---

### Task 1: 分离普通推动的免疫条件

**Files:**
- Create: `src/test/java/com/l2hostility_tweaks/mixin/PushCancelMixinTest.java`
- Modify: `src/main/java/com/l2hostility_tweaks/mixin/PushCancelMixin.java`

**Interfaces:**
- Consumes: `boolean forceImmune`、`boolean gravityImmune`。
- Produces: `static boolean l2fix$shouldCancelPush(boolean forceImmune, boolean gravityImmune)`。

- [ ] **Step 1: 写入失败回归测试**

```java
package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PushCancelMixinTest {

    @Test
    void cancelsPushOnlyForForceImmunity() {
        assertTrue(PushCancelMixin.l2fix$shouldCancelPush(true, false));
        assertTrue(PushCancelMixin.l2fix$shouldCancelPush(true, true));
        assertFalse(PushCancelMixin.l2fix$shouldCancelPush(false, true));
        assertFalse(PushCancelMixin.l2fix$shouldCancelPush(false, false));
    }
}
```

- [ ] **Step 2: 运行定向测试并确认 RED**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.mixin.PushCancelMixinTest`

Expected: `compileTestJava` 失败，原因是 `l2fix$shouldCancelPush(boolean, boolean)` 尚不存在。

- [ ] **Step 3: 写入最小实现并接入 Mixin**

在 `PushCancelMixin` 中增加：

```java
@Unique
static boolean l2fix$shouldCancelPush(boolean forceImmune, boolean gravityImmune) {
    return forceImmune;
}
```

将现有条件改为：

```java
if (l2fix$shouldCancelPush(
        ImmunityHelper.isImmuneToForce(le),
        ImmunityHelper.isImmuneToGravity(le))) {
    ci.cancel();
}
```

- [ ] **Step 4: 运行定向测试并确认 GREEN**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.mixin.PushCancelMixinTest`

Expected: 1 test completed，0 failed。

- [ ] **Step 5: 提交修复**

```powershell
git add -- src/main/java/com/l2hostility_tweaks/mixin/PushCancelMixin.java src/test/java/com/l2hostility_tweaks/mixin/PushCancelMixinTest.java
git commit -m "fix: separate force and gravity push immunity"
```

### Task 2: 完整验证与输出 JAR

**Files:**
- Verify: `build/libs/l2hostility_tweaks-1.0.0.jar`
- Output: `C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar`

**Interfaces:**
- Consumes: Task 1 已通过测试的源码。
- Produces: 含职责分离修复的 Forge 模组 JAR 与 SHA-256。

- [ ] **Step 1: 运行完整干净构建**

Run: `.\gradlew.bat clean build`

Expected: `BUILD SUCCESSFUL`，所有测试 0 failed，资源和 Mixin 注册校验通过。

- [ ] **Step 2: 复制并校验 JAR**

将 `build/libs/l2hostility_tweaks-1.0.0.jar` 覆盖复制到 `C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar`。读取大小与 SHA-256，并用 `jar tf` 确认包含 `com/l2hostility_tweaks/mixin/PushCancelMixin.class`。

- [ ] **Step 3: 检查工作树**

Run: `git status --short`

Expected: 无未提交修改。
