# Safe Item Refunds Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 防止恢复口袋和词条卸载器在背包只能部分接收返还物时丢失剩余物品。

**Architecture:** 在现有 `TraitWandHelper` 中增加共享的 `giveOrDrop(Player, ItemStack)` 入口。该入口忽略 `Player.addItem` 的布尔值，改为检查被原地修改后的 `ItemStack.isEmpty()`；只有仍有余量时才掉落余量。恢复口袋和卸载器都调用这一入口。

**Tech Stack:** Java 17、Minecraft Forge 1.20.1、JUnit 5、Gradle 8.5。

## Global Constraints

- 不改变返还数量、卸载费用、词条等级和恢复计时。
- 正常情况下优先放入玩家背包，只掉落无法容纳的余量。
- 恢复口袋只在完整交付后删除存储记录。
- 不把 JAR 部署到 `mods`。

---

### Task 1: 共享安全交付入口

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/util/TraitWandHelper.java`
- Create: `src/test/java/com/l2hostility_tweaks/util/TraitWandHelperTest.java`

**Interfaces:**
- Consumes: `Player player`、会被原地减少数量的 `ItemStack stack`。
- Produces: `public static boolean giveOrDrop(Player, ItemStack)`；完整进入背包或余量成功掉落时返回 `true`。

- [x] **Step 1: Write the failing test**

新增测试，以真实 `ItemStack` 模拟只插入 3/16 个物品，断言掉落回调收到剩余 13 个，而不是丢弃。

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.util.TraitWandHelperTest`

Expected: FAIL，因为共享交付方法尚不存在。

- [x] **Step 3: Write minimal implementation**

```java
public static boolean giveOrDrop(Player player, ItemStack stack) {
    return deliver(() -> player.addItem(stack), () -> !stack.isEmpty(),
            () -> player.drop(stack, false) != null);
}

static boolean deliver(Runnable insert, BooleanSupplier hasRemainder,
        BooleanSupplier drop) {
    insert.run();
    return !hasRemainder.getAsBoolean() || drop.getAsBoolean();
}
```

- [x] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.util.TraitWandHelperTest`

Expected: PASS。

### Task 2: 接入全部返还路径

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/content/TraitUnloaderWand.java`
- Modify: `src/main/java/com/l2hostility_tweaks/mixin/PocketOfRestorationMixin.java`
- Modify: `src/test/java/com/l2hostility_tweaks/mixin/PocketOfRestorationMixinTest.java`

**Interfaces:**
- Consumes: Task 1 的 `TraitWandHelper.giveOrDrop(Player, ItemStack)`。
- Produces: 单级、整组、全部卸载和恢复口袋统一的完整交付语义。

- [x] **Step 1: Write the failing integration-seam test**

调整恢复口袋测试接口，使回退交付只有在共享入口返回 `true` 时才视为恢复完成，并覆盖失败时保留记录的返回值。

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.mixin.PocketOfRestorationMixinTest`

Expected: FAIL，因为恢复助手仍使用独立的背包/掉落回调。

- [x] **Step 3: Write minimal implementation**

卸载器三处 `player.addItem(symbol)` 全部替换为 `TraitWandHelper.giveOrDrop(player, symbol)`。恢复口袋的回退路径也调用共享入口，并让 `l2fix$restoreStoredItem` 接收单一 `BooleanSupplier`。

- [x] **Step 4: Run focused and complete verification**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.util.TraitWandHelperTest --tests com.l2hostility_tweaks.mixin.PocketOfRestorationMixinTest`

Run: `.\gradlew.bat clean build`

Expected: 两条命令均为 `BUILD SUCCESSFUL`，全部测试零失败。

- [x] **Step 5: Commit**

```powershell
git add docs/superpowers/plans/2026-08-23-safe-item-refunds.md src/main/java/com/l2hostility_tweaks/util/TraitWandHelper.java src/main/java/com/l2hostility_tweaks/content/TraitUnloaderWand.java src/main/java/com/l2hostility_tweaks/mixin/PocketOfRestorationMixin.java src/test/java/com/l2hostility_tweaks/util/TraitWandHelperTest.java src/test/java/com/l2hostility_tweaks/mixin/PocketOfRestorationMixinTest.java
git commit -m "fix: preserve partial item refunds"
```
