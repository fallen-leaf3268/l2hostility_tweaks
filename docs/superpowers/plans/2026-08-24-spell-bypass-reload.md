# Spell Bypass Reload Cleanup Implementation Plan

> **For Codex:** REQUIRED SUB-SKILL: Use executing-plans to implement this plan task-by-task.

**Goal:** 在 KubeJS 脚本重载前清除旧的法术伤害绕过标记，使当前脚本重新声明的配置成为唯一有效状态。

**Architecture:** 使用 `KubeJSPlugin.clearCaches()` 作为脚本卸载前钩子，在 `L2HFKJSPlugin` 中调用父类实现后清空 `SpellDamageFlags`。测试直接实例化插件并调用生命周期方法，因此仅向测试运行时补充 KubeJS、Rhino 和 Architectury 本地 JAR，不改变模组打包内容。

**Tech Stack:** Java 17、Forge 1.20.1、KubeJS、Gradle 8.5、JUnit 5

---

### Task 1: 为插件生命周期测试补齐测试依赖

**Files:**
- Modify: `build.gradle`

- [x] 在 `dependencies` 中为现有三个 `compileOnly` KubeJS 运行库增加对应的 `testImplementation files(...)`。
- [x] 保持依赖为测试专用，不改为 `implementation`，避免这些库进入发布 JAR。

### Task 2: 用失败测试锁定重载前清理行为

**Files:**
- Create: `src/test/java/com/l2hostility_tweaks/compat/kubejs/L2HFKJSPluginTest.java`

- [x] 通过反射向活动标记集合放入哨兵值，避免普通 JUnit 环境触发不完整的 Minecraft/Forge 引导。
- [x] 调用 `new L2HFKJSPlugin().clearCaches()`。
- [x] 断言活动标记集合被清除，并用 `@AfterEach` 隔离静态状态。
- [x] 运行 `./gradlew test --tests com.l2hostility_tweaks.compat.kubejs.L2HFKJSPluginTest`，确认测试因现有继承实现没有清理标记而失败。

### Task 3: 在正确的 KubeJS 生命周期中清理标记

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/compat/kubejs/L2HFKJSPlugin.java`

- [x] 覆盖 `clearCaches()`。
- [x] 先调用 `super.clearCaches()`，再调用 `SpellDamageFlags.clear()`。
- [x] 再次运行聚焦测试，确认通过。

### Task 4: 全量验证、产物和独立提交

**Files:**
- Modify: `docs/superpowers/plans/2026-08-24-spell-bypass-reload.md`
- Output: `C:/Users/Lenovo/Desktop/Ai_Run/output/l2hostility_tweaks-1.0.0.jar`

- [x] 运行 `./gradlew clean build`，确认单元测试、资源校验和 JAR 构建全部通过。
- [x] 检查测试报告的实际测试数量与失败数。
- [x] 将构建 JAR 复制到输出目录，不部署到 `mods`。
- [x] 清理本轮生成且未跟踪的日志文件，确认工作树只含预期修改。
- [x] 将本计划中的检查项更新为完成。
- [x] 检查暂存差异后提交为 `fix: clear spell bypass flags on script reload`。
- [x] 确认提交后工作树干净，并记录提交哈希和产物路径。
