# Player Trait Panel Unload Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 恢复玩家无需持有测试卸载工具即可通过词条面板卸载自身词条的行为。

**Architecture:** 保持客户端面板与现有网络协议不变，只移除服务端处理器中错误新增的主手工具门槛。服务端继续以玩家能力中的真实词条和等级作为唯一卸载与退款依据。

**Tech Stack:** Java 17、Minecraft Forge 1.20.1、JUnit 5、Gradle 8.5、MixinGradle

## Global Constraints

- 面板卸载不要求主手、副手或背包中存在 `TraitUnloaderWand`。
- 卸载工具自身行为不变。
- 不改变协议编号、卸载费用、退款、封印清理或能力同步。
- 不提交 `.gradle-user-home`、`logs` 或构建产物。

---

### Task 1: 恢复面板卸载入口

**Files:**
- Modify: `src/test/java/com/l2hostility_tweaks/config/ConfigCacheReloadHandlerTest.java`
- Modify: `src/main/java/com/l2hostility_tweaks/network/NetworkHandler.java`

**Interfaces:**
- Consumes: `UnloadTraitPacket(String traitId, boolean unloadAll)` 与 `MobTraitCap.traits`
- Produces: 不依赖测试工具、仍由服务端验证和执行的面板卸载流程

- [ ] **Step 1: 写入失败回归测试**

将 `unloadPacketRequiresServerVerifiedMainHandWandBeforeMutation` 替换为 `unloadPacketAllowsPanelUseWithoutTestWandAndKeepsServerValidation`。截取 `UnloadTraitPacket.handle` 方法所在代码段，断言其中不包含 `player.getMainHandItem()` 或 `instanceof TraitUnloaderWand`；并断言 `MobTraitCap.HOLDER.isProper(player)`、`ResourceLocation.tryParse(msg.traitId)`、`cap.traits.get(trait)` 均存在且位于 `TraitUnloaderWand.unload` 之前。

- [ ] **Step 2: 运行测试并确认红灯**

Run: `gradle --offline --no-daemon test --tests com.l2hostility_tweaks.config.ConfigCacheReloadHandlerTest.unloadPacketAllowsPanelUseWithoutTestWandAndKeepsServerValidation`

Expected: FAIL，因为现有处理器仍包含主手 `TraitUnloaderWand` 限制。

- [ ] **Step 3: 实施最小修复**

从 `UnloadTraitPacket.handle` 删除：

```java
if (!(player.getMainHandItem().getItem() instanceof TraitUnloaderWand)) return;
```

不修改同一处理器的其他验证或卸载调用。

- [ ] **Step 4: 运行定向测试并确认绿灯**

Run: `gradle --offline --no-daemon test --tests com.l2hostility_tweaks.config.ConfigCacheReloadHandlerTest.unloadPacketAllowsPanelUseWithoutTestWandAndKeepsServerValidation`

Expected: PASS。

- [ ] **Step 5: 完整验证并输出 JAR**

Run: `gradle --offline --no-daemon test validateProjectResources jar --rerun-tasks`

Expected: 所有测试通过，`validateProjectResources`、`jar`、`reobfJar` 成功。将重混淆 JAR 复制到 `C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar`，并核对 SHA-256。

- [ ] **Step 6: 提交修复**

```bash
git add docs/superpowers/specs/2026-09-04-player-trait-panel-unload-design.md docs/superpowers/plans/2026-09-04-player-trait-panel-unload.md src/test/java/com/l2hostility_tweaks/config/ConfigCacheReloadHandlerTest.java src/main/java/com/l2hostility_tweaks/network/NetworkHandler.java
git commit -m "fix: restore player trait panel unloading"
```
