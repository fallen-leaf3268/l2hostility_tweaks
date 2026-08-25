# Split Suppressor Target Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让分裂抑制只永久封印当前有效的 `l2hostility:split`，不再回退封印其他词条。

**Architecture:** 在 `RemoveTraitEnchantmentMixin` 中增加一个包内可测试的纯选择函数，按词条 ID 与原始等级筛选有效分裂词条。实际 Mixin 调用该函数；找不到目标时直接返回，让上游保持“移除不存在的分裂词条等于无效果”的行为。

**Tech Stack:** Java 17、Forge 1.20.1、Mixin、JUnit 5、Gradle

## Global Constraints

- 只处理 ID 为 `l2hostility:split` 且原始等级大于 0 的词条。
- 不回退到其他词条，不新增配置，不修改 Tooltip。
- 使用 TDD，先观察回归测试失败，再修改生产代码。
- 输出 JAR 到 `C:\Users\Lenovo\Desktop\Ai_Run\output`，不部署到 `mods`。

---

### Task 1: 修正分裂抑制目标选择

**Files:**
- Modify: `src/test/java/com/l2hostility_tweaks/mixin/RemoveTraitEnchantmentMixinTest.java`
- Modify: `src/main/java/com/l2hostility_tweaks/mixin/RemoveTraitEnchantmentMixin.java`

**Interfaces:**
- Consumes: `Map<T, Integer>` 原始词条等级与 `Function<T, String>` ID 提取器。
- Produces: `static <T> T l2fix$findActiveSplit(Map<T, Integer> traits, Function<T, String> idGetter)`，找不到有效分裂时返回 `null`。

- [ ] **Step 1: 写入失败回归测试**

在现有测试类中加入三个场景：

```java
@Test
void doesNotFallBackToAnotherTraitWhenSplitIsMissing() {
    Map<String, Integer> traits = new LinkedHashMap<>();
    traits.put("l2hostility:speedy", 2);
    assertNull(RemoveTraitEnchantmentMixin.l2fix$findActiveSplit(traits, id -> id));
}

@Test
void selectsOnlyActiveSplitTrait() {
    Map<String, Integer> traits = new LinkedHashMap<>();
    traits.put("l2hostility:speedy", 2);
    traits.put("l2hostility:split", 1);
    assertEquals("l2hostility:split",
            RemoveTraitEnchantmentMixin.l2fix$findActiveSplit(traits, id -> id));
}

@Test
void doesNotFallBackWhenSplitIsAlreadySealed() {
    Map<String, Integer> traits = new LinkedHashMap<>();
    traits.put("l2hostility:split", -1);
    traits.put("l2hostility:speedy", 2);
    assertNull(RemoveTraitEnchantmentMixin.l2fix$findActiveSplit(traits, id -> id));
}
```

- [ ] **Step 2: 运行定向测试并确认 RED**

Run: `./gradlew test --tests com.l2hostility_tweaks.mixin.RemoveTraitEnchantmentMixinTest`

Expected: 编译失败，指出 `l2fix$findActiveSplit` 尚不存在；失败原因必须是待实现接口缺失。

- [ ] **Step 3: 写入最小实现并接入 Mixin**

增加：

```java
@Unique
static <T> T l2fix$findActiveSplit(Map<T, Integer> traits, Function<T, String> idGetter) {
    return traits.entrySet().stream()
            .filter(entry -> "l2hostility:split".equals(idGetter.apply(entry.getKey())))
            .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
}
```

在注入方法中用该函数替换“优先分裂、否则第一个有效词条”的两段选择逻辑；只在返回非空词条时取消上游并执行永久封印。

- [ ] **Step 4: 运行定向测试并确认 GREEN**

Run: `./gradlew test --tests com.l2hostility_tweaks.mixin.RemoveTraitEnchantmentMixinTest`

Expected: 4 tests completed，0 failed。

- [ ] **Step 5: 提交修复**

```powershell
git add -- src/main/java/com/l2hostility_tweaks/mixin/RemoveTraitEnchantmentMixin.java src/test/java/com/l2hostility_tweaks/mixin/RemoveTraitEnchantmentMixinTest.java
git commit -m "fix: restrict split suppressor to active split"
```

### Task 2: 完整验证与输出 JAR

**Files:**
- Verify: `build/libs/l2hostility_tweaks-1.0.0.jar`
- Output: `C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar`

**Interfaces:**
- Consumes: Task 1 已通过测试的源码。
- Produces: 可供用户验证的 Forge 模组 JAR 与 SHA-256。

- [ ] **Step 1: 运行完整干净构建**

Run: `./gradlew clean build`

Expected: `BUILD SUCCESSFUL`，所有测试 0 failed，资源与 Mixin 注册校验通过。

- [ ] **Step 2: 复制并校验 JAR**

将 `build/libs/l2hostility_tweaks-1.0.0.jar` 复制到 `C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar`，然后读取文件大小与 SHA-256，并用 `jar tf` 确认包含 `RemoveTraitEnchantmentMixin.class`。

- [ ] **Step 3: 检查工作树并提交计划执行记录**

Run: `git status --short`

Expected: 除实施计划尚未提交外无未跟踪或未提交修改；提交计划文档后工作树为空。
