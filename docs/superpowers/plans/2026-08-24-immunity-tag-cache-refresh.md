# Immunity Tag Cache Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让词条免疫标签首次查询立即生效，并在 Forge 标签更新后清除所有相关缓存。

**Architecture:** `ImmunityHelper` 用一个“先发现、后取值”的小型通用方法固定标签初始化顺序，并提供统一缓存失效入口。`L2HostilityFix` 监听 Forge `TagsUpdatedEvent`，将数据包重载事件转发给该入口。

**Tech Stack:** Java 17、Forge 1.20.1、Gradle 8.5、JUnit 5

## Global Constraints

- 保留 `traitTagCache`，不改变标签成员或免疫判定规则。
- 标签更新时同时清除词条匹配缓存和实体本 tick 免疫缓存。
- 不改变 Curios 饰品免疫检测、词条 tick 顺序或日志级别。
- 不新增运行时依赖，不把 JAR 部署到 `mods`。

---

### Task 1: 锁定首次初始化和缓存失效行为

**Files:**
- Create: `src/test/java/com/l2hostility_tweaks/util/ImmunityHelperCacheTest.java`
- Test: `src/test/java/com/l2hostility_tweaks/util/ImmunityHelperCacheTest.java`

**Interfaces:**
- Consumes: `ImmunityHelper.resolveAfterDiscovery(Runnable, Supplier<T>)` 和 `ImmunityHelper.invalidateTagCaches()`
- Produces: 首次查询顺序及完整缓存失效的回归测试

- [x] **Step 1: 编写首次查询失败测试**

```java
@Test
void resolvesValueAfterDiscoveryRuns() {
    AtomicReference<String> value = new AtomicReference<>();

    String result = ImmunityHelper.resolveAfterDiscovery(
            () -> value.set("ready"), value::get);

    assertEquals("ready", result);
}
```

- [x] **Step 2: 编写缓存失效失败测试**

通过反射向私有 `traitTagCache` 写入一个旧结果，把 `cacheTickForce` 和 `cacheTickGravity` 设置为当前 tick，再调用 `invalidateTagCaches()`。断言 map 为空、两个 tick 均为 `-1`、两个弱引用均不再持有实体。

- [x] **Step 3: 运行聚焦测试并确认红灯**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.util.ImmunityHelperCacheTest --rerun-tasks`

Expected: 测试编译因 `resolveAfterDiscovery` 和 `invalidateTagCaches` 尚不存在而失败。

### Task 2: 修正初始化顺序并提供统一失效入口

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/util/ImmunityHelper.java`
- Test: `src/test/java/com/l2hostility_tweaks/util/ImmunityHelperCacheTest.java`

**Interfaces:**
- Consumes: `Runnable discovery`、`Supplier<T> refreshedValue`
- Produces: `static <T> T resolveAfterDiscovery(Runnable, Supplier<T>)`、`public static void invalidateTagCaches()`

- [x] **Step 1: 添加先发现后取值方法**

```java
static <T> T resolveAfterDiscovery(Runnable discovery, Supplier<T> refreshedValue) {
    discovery.run();
    return refreshedValue.get();
}
```

- [x] **Step 2: 让标签查询读取初始化后的字段**

力量、重力和玩家自用黑名单判断先通过 `resolveAfterDiscovery` 获取对应 `TagKey<MobTrait>`，再进行注册表 holder 查询。删除 `hasTraitInTag` 和 `isTraitInTag` 内部重复且过晚的 `discoverTraitRegistry()` 调用。

- [x] **Step 3: 添加完整失效入口**

```java
public static void invalidateTagCaches() {
    traitTagCache.clear();
    cachedEntityForceRef = new WeakReference<>(null);
    cacheTickForce = -1;
    cachedImmuneToForce = false;
    cachedEntityGravityRef = new WeakReference<>(null);
    cacheTickGravity = -1;
    cachedImmuneToGravity = false;
}
```

- [x] **Step 4: 运行聚焦测试并确认绿灯**

Run: `.\gradlew.bat test --tests com.l2hostility_tweaks.util.ImmunityHelperCacheTest --rerun-tasks`

Expected: 首次查询和缓存失效测试全部通过。

### Task 3: 接入 Forge 标签更新事件并发布验证包

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/L2HostilityFix.java`
- Modify: `docs/superpowers/plans/2026-08-24-immunity-tag-cache-refresh.md`
- Output: `C:/Users/Lenovo/Desktop/Ai_Run/output/l2hostility_tweaks-1.0.0.jar`

**Interfaces:**
- Consumes: Forge 主事件总线的 `TagsUpdatedEvent`
- Produces: 每次标签重绑定后的即时缓存失效

- [x] **Step 1: 注册标签更新处理器**

```java
@SubscribeEvent
public void onTagsUpdated(TagsUpdatedEvent event) {
    ImmunityHelper.invalidateTagCaches();
}
```

- [x] **Step 2: 执行完整构建**

Run: `.\gradlew.bat clean build`

Expected: 单元测试、Mixin 注解处理、资源校验、重混淆和 JAR 构建全部成功。

- [x] **Step 3: 核对测试与 JAR 内容**

统计 `build/test-results/test/TEST-*.xml`，确认零失败、零错误；读取 JAR 条目，确认没有 `dev/latvian/` 测试桩依赖。

- [x] **Step 4: 输出验证 JAR 并清理生成日志**

把 `build/libs/l2hostility_tweaks-1.0.0.jar` 覆盖复制到 `C:/Users/Lenovo/Desktop/Ai_Run/output/`。只删除经绝对路径验证、位于当前工作树中的未跟踪 `logs` 目录。

- [x] **Step 5: 更新计划并创建独立提交**

```bash
git add docs/superpowers/plans/2026-08-24-immunity-tag-cache-refresh.md \
  src/main/java/com/l2hostility_tweaks/L2HostilityFix.java \
  src/main/java/com/l2hostility_tweaks/util/ImmunityHelper.java \
  src/test/java/com/l2hostility_tweaks/util/ImmunityHelperCacheTest.java
git diff --cached --check
git commit -m "fix: refresh immunity tag caches"
```

- [x] **Step 6: 最终状态核验**

确认工作树干净，记录提交短哈希、测试统计、JAR 大小和 SHA-256。
