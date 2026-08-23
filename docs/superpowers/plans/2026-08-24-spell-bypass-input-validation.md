# Spell Bypass Input Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 `L2HFix.enableBypass(...)` 对 `null` 和未知类型安全返回并记录可操作的错误日志。

**Architecture:** 保留现有公开方法签名和合法值映射，在 `SpellDamageFlags` 内集中处理无效输入。JUnit 直接调用真实公开方法，并通过 Log4j 测试附加器观察 SLF4J 日志、通过反射确认静态集合没有被修改。

**Tech Stack:** Java 17、Forge 1.20.1、KubeJS、SLF4J/Log4j 2、Gradle 8.5、JUnit 5

## Global Constraints

- 保持 `public static void enableBypass(String type)` 签名不变。
- 合法值、别名、大小写和空格处理保持不变。
- 无效输入不得抛出异常或改变活动标记。
- 不新增运行时依赖，不把 JAR 部署到 `mods`。

---

### Task 1: 锁定无效输入行为

**Files:**
- Create: `src/test/java/com/l2hostility_tweaks/compat/kubejs/SpellDamageFlagsTest.java`

**Interfaces:**
- Consumes: `SpellDamageFlags.enableBypass(String)`、私有静态集合 `ACTIVE_TAGS`、日志分类 `l2htweaks:kubejs`
- Produces: 无效输入安全性和错误日志格式的回归测试

- [x] **Step 1: 创建完整失败测试**

```java
package com.l2hostility_tweaks.compat.kubejs;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellDamageFlagsTest {

    private RecordingAppender appender;

    @BeforeEach
    void attachAppender() {
        SpellDamageFlags.clear();
        appender = new RecordingAppender();
        appender.start();
        logger().addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger().removeAppender(appender);
        appender.stop();
        SpellDamageFlags.clear();
    }

    @Test
    void rejectsNullTypeWithoutChangingFlags() {
        assertDoesNotThrow(() -> SpellDamageFlags.enableBypass(null));
        assertTrue(activeTags().isEmpty());
        assertInvalidTypeLogged("null");
    }

    @Test
    void rejectsUnknownTypeWithoutChangingFlags() {
        assertDoesNotThrow(() -> SpellDamageFlags.enableBypass("bypass_resistence"));
        assertTrue(activeTags().isEmpty());
        assertInvalidTypeLogged("bypass_resistence");
    }

    private void assertInvalidTypeLogged(String value) {
        assertTrue(appender.events.stream().anyMatch(event -> {
            String message = event.getMessage().getFormattedMessage();
            return event.getLevel() == Level.ERROR && message.contains(value) &&
                    message.contains("bypass_armor") && message.contains("bypass_resistance");
        }));
    }

    private static Logger logger() {
        return (Logger) LogManager.getLogger("l2htweaks:kubejs");
    }

    @SuppressWarnings("unchecked")
    private static Set<Object> activeTags() {
        try {
            Field field = SpellDamageFlags.class.getDeclaredField("ACTIVE_TAGS");
            field.setAccessible(true);
            return (Set<Object>) field.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static final class RecordingAppender extends AbstractAppender {

        private final List<LogEvent> events = new ArrayList<>();

        private RecordingAppender() {
            super("recording", null, PatternLayout.createDefaultLayout(), false, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            events.add(event.toImmutable());
        }
    }
}
```

- [x] **Step 2: 运行聚焦测试并确认准确红灯**

Run: `./gradlew test --tests com.l2hostility_tweaks.compat.kubejs.SpellDamageFlagsTest --rerun-tasks`

Expected: `null` 测试因空指针失败；未知值测试因没有错误日志失败，而不是编译或测试环境错误。

### Task 2: 实现安全拒绝和日志

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/compat/kubejs/SpellDamageFlags.java`
- Test: `src/test/java/com/l2hostility_tweaks/compat/kubejs/SpellDamageFlagsTest.java`

**Interfaces:**
- Consumes: `String type`
- Produces: 保持不变的 `public static void enableBypass(String type)`

- [x] **Step 1: 添加 KubeJS 日志器和合法值提示常量**

```java
private static final Logger LOGGER = LoggerFactory.getLogger("l2htweaks:kubejs");
private static final String VALID_TYPES = "bypass_armor, bypass_magic, bypass_effects, " +
        "bypass_cooldown, bypass_resistance, bypass_enchantments";
```

- [x] **Step 2: 在 switch 前安全处理 null，并记录所有无效选择**

```java
TagKey<DamageType> tag = type == null ? null : switch (type) {
    case "bypass_armor" -> DamageTypeTags.BYPASSES_ARMOR;
    case "bypass_magic", "bypass_effects" -> DamageTypeTags.BYPASSES_EFFECTS;
    case "bypass_cooldown" -> DamageTypeTags.BYPASSES_COOLDOWN;
    case "bypass_resistance" -> DamageTypeTags.BYPASSES_RESISTANCE;
    case "bypass_enchantments" -> DamageTypeTags.BYPASSES_ENCHANTMENTS;
    default -> null;
};
if (tag == null) {
    LOGGER.error("Invalid spell damage bypass type: {}. Expected one of: {}", type, VALID_TYPES);
    return;
}
ACTIVE_TAGS.add(tag);
```

- [x] **Step 3: 运行聚焦测试并确认转绿**

Run: `./gradlew test --tests com.l2hostility_tweaks.compat.kubejs.SpellDamageFlagsTest --rerun-tasks`

Expected: 两个测试全部通过。

### Task 3: 全量验证、产物和提交

**Files:**
- Modify: `docs/superpowers/plans/2026-08-24-spell-bypass-input-validation.md`
- Output: `C:/Users/Lenovo/Desktop/Ai_Run/output/l2hostility_tweaks-1.0.0.jar`

**Interfaces:**
- Consumes: Gradle `clean build` 产物和 JUnit XML 报告
- Produces: 经验证的 JAR 与独立 Git 提交

- [x] **Step 1: 执行完整构建**

Run: `./gradlew clean build`

Expected: 编译、全部测试、资源校验、重混淆和 JAR 构建成功。

- [x] **Step 2: 核对测试报告和发布内容**

统计 `build/test-results/test/TEST-*.xml` 的测试、失败、错误和跳过数量。读取发布 JAR 的 ZIP 条目，确认不存在 `dev/latvian/` KubeJS 类。

- [x] **Step 3: 输出 JAR 并清理生成日志**

将 `build/libs/l2hostility_tweaks-1.0.0.jar` 复制到 `C:/Users/Lenovo/Desktop/Ai_Run/output/`，不得复制到 `mods`。仅删除已验证位于当前工作树内的未跟踪 `logs` 目录。

- [x] **Step 4: 更新计划并创建独立提交**

```bash
git add build.gradle docs/superpowers/plans/2026-08-24-spell-bypass-input-validation.md \
  src/main/java/com/l2hostility_tweaks/compat/kubejs/SpellDamageFlags.java \
  src/test/java/com/l2hostility_tweaks/compat/kubejs/SpellDamageFlagsTest.java
git diff --cached --check
git commit -m "fix: validate spell bypass script inputs"
```

`build.gradle` 只有在日志测试确实需要补充测试依赖时才暂存；若无需修改则不包含它。

- [x] **Step 5: 最终状态核验**

确认工作树干净，记录提交短哈希、测试统计、JAR 大小与 SHA-256。
