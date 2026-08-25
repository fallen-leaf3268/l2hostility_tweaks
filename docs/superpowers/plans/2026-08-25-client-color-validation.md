# 客户端颜色配置严格校验实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 统一校验并安全解析 HUD 颜色配置，阻止超范围 RGB 污染相邻颜色通道。

**Architecture:** 在现有 `ClientL2HConfig` 内新增两个包内纯解析入口，由一个私有结构化整数解析器实现精确字段数、无符号十进制和范围检查。Forge 配置校验器与运行时 getter 共同调用这些入口，使加载期拒绝和消费期防御保持一致。

**Tech Stack:** Java 17、Minecraft Forge 1.20.1、JUnit 5、Gradle 8.5。

## Global Constraints

- 颜色分段格式固定为 `难度,R,G,B`，难度范围为 `0` 到 `2147483647`。
- 默认颜色格式固定为 `R,G,B`，所有 RGB 通道范围为 `0` 到 `255`。
- 字段两侧允许空白，但不接受负号、正号、空字段、浮点数、十六进制或多余字段。
- 不钳制非法 RGB；非法分段跳过，非法默认颜色回退为 `170,170,170`。
- 配置声明与运行时解析必须复用同一套规则。
- 不改变合法分段的升序排序及 HUD 的颜色选择、渐变、宽度和位置逻辑。
- 不修改服务端或通用配置。
- 不把 JAR 部署到 `mods`；验证产物复制到 `C:\Users\Lenovo\Desktop\Ai_Run\output`。

---

### Task 1: 建立严格且可测试的颜色解析器

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/client/config/ClientL2HConfig.java`
- Modify: `src/test/java/com/l2hostility_tweaks/client/config/ClientL2HConfigTest.java`

**Interfaces:**
- Consumes: 任意 `Object` 配置值。
- Produces: 包内方法 `static int[] parseColorSegment(Object)` 与 `static int[] parseRgb(Object)`；合法时返回新数组，非法时返回 `null`。

- [ ] **Step 1: Write failing parser boundary tests**

在 `ClientL2HConfigTest` 增加静态导入 `assertArrayEquals`、`assertNotNull`，并增加：

```java
    @Test
    void parsesValidColorBoundariesAndWhitespace() {
        assertArrayEquals(new int[]{0, 0, 255, 1},
                ClientL2HConfig.parseColorSegment(" 0 , 0 , 255 , 1 "));
        assertArrayEquals(new int[]{Integer.MAX_VALUE, 255, 0, 255},
                ClientL2HConfig.parseColorSegment("2147483647,255,0,255"));
        assertArrayEquals(new int[]{0, 255, 128},
                ClientL2HConfig.parseRgb("0,255,128"));
    }

    @Test
    void rejectsMalformedAndOutOfRangeColors() {
        for (Object value : List.of(
                "100,256,0,0", "100,0,-1,0", "100,+1,2,3",
                "2147483648,1,2,3", "100,1,2", "100,1,2,3,4",
                "100,,2,3", "100,1.5,2,3", "100,0xFF,2,3")) {
            assertNull(ClientL2HConfig.parseColorSegment(value), String.valueOf(value));
        }
        assertNull(ClientL2HConfig.parseColorSegment(100));
        assertNull(ClientL2HConfig.parseRgb("256,0,0"));
        assertNull(ClientL2HConfig.parseRgb("1,2"));
        assertNotNull(ClientL2HConfig.parseRgb("255,255,255"));
    }
```

增加 `java.util.List` 导入。

- [ ] **Step 2: Run the focused test to verify RED**

Run: `./gradlew.bat --no-daemon --console=plain test --tests com.l2hostility_tweaks.client.config.ClientL2HConfigTest`

Expected: FAIL during `compileTestJava` because `parseColorSegment` and `parseRgb` do not exist.

- [ ] **Step 3: Implement the minimal strict parsers**

在 `ClientL2HConfig` 中加入：

```java
    static int[] parseColorSegment(Object value) {
        return parseUnsignedFields(value, 4, 1);
    }

    static int[] parseRgb(Object value) {
        return parseUnsignedFields(value, 3, 0);
    }

    private static int[] parseUnsignedFields(Object value, int fieldCount, int rgbStart) {
        if (!(value instanceof String text)) return null;
        String[] parts = text.split(",", -1);
        if (parts.length != fieldCount) return null;
        int[] parsed = new int[fieldCount];
        try {
            for (int i = 0; i < fieldCount; i++) {
                String part = parts[i].trim();
                if (!part.matches("\\d+")) return null;
                parsed[i] = Integer.parseInt(part);
                if (i >= rgbStart && parsed[i] > 255) return null;
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
        return parsed;
    }
```

无符号正则排除正负号和空字段；`Integer.parseInt` 排除整数溢出；`rgbStart` 使分段难度不受 255 限制。

- [ ] **Step 4: Run the focused test to verify GREEN**

Run: `./gradlew.bat --no-daemon --console=plain test --tests com.l2hostility_tweaks.client.config.ClientL2HConfigTest`

Expected: BUILD SUCCESSFUL with 3 tests passing。

### Task 2: 让配置声明与运行时消费复用严格解析

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/client/config/ClientL2HConfig.java`
- Modify: `src/test/java/com/l2hostility_tweaks/client/config/ClientL2HConfigTest.java`

**Interfaces:**
- Consumes: Task 1 的 `parseColorSegment(Object)` 和 `parseRgb(Object)`。
- Produces: 严格的 Forge 配置谓词、仅缓存合法分段的 `getColorSegments()`、对非法默认颜色安全回退的 `getDefaultColor()`。

- [ ] **Step 1: Write failing runtime defense tests**

在测试类增加 `FastColor`、`ArrayList` 与 `List` 所需导入，并增加：

```java
    @Test
    void skipsInvalidRuntimeSegmentsAndKeepsValidOrder() {
        List<? extends String> original = ClientL2HConfig.CLIENT.colorSegments.get();
        try {
            ClientL2HConfig.CLIENT.colorSegments.set(List.of(
                    "300,4,5,6", "200,256,0,0", "100,1,2,3"));
            ClientL2HConfig.invalidateCaches();

            List<int[]> parsed = ClientL2HConfig.getColorSegments();

            assertEquals(2, parsed.size());
            assertArrayEquals(new int[]{100, 1, 2, 3}, parsed.get(0));
            assertArrayEquals(new int[]{300, 4, 5, 6}, parsed.get(1));
        } finally {
            ClientL2HConfig.CLIENT.colorSegments.set(original);
            ClientL2HConfig.invalidateCaches();
        }
    }

    @Test
    void fallsBackWhenRuntimeDefaultColorIsInvalid() {
        String original = ClientL2HConfig.CLIENT.defaultColor.get();
        try {
            ClientL2HConfig.CLIENT.defaultColor.set("256,0,0");
            assertEquals(FastColor.ARGB32.color(255, 170, 170, 170),
                    ClientL2HConfig.getDefaultColor());
        } finally {
            ClientL2HConfig.CLIENT.defaultColor.set(original);
        }
    }
```

增加静态导入 `assertEquals`。

- [ ] **Step 2: Run runtime tests to verify RED**

Run: `./gradlew.bat --no-daemon --console=plain test --tests com.l2hostility_tweaks.client.config.ClientL2HConfigTest`

Expected: FAIL because the current getters accept RGB `256` and either retain the invalid segment or pack the invalid default into a wrong color.

- [ ] **Step 3: Reuse parsers in config predicates and getters**

将颜色配置注释补充为 `RGB 范围: 0–255`。将两个配置谓词替换为：

```java
ClientL2HConfig.parseColorSegment(e) != null
ClientL2HConfig.parseRgb(e) != null
```

将 `getColorSegments()` 的循环体替换为：

```java
            for (String entry : CLIENT.colorSegments.get()) {
                int[] parsed = parseColorSegment(entry);
                if (parsed != null) parsedColorSegments.add(parsed);
            }
```

保留现有按 `a[0]` 升序排序。将 `getDefaultColor()` 替换为：

```java
    public static int getDefaultColor() {
        int[] rgb = parseRgb(CLIENT.defaultColor.get());
        if (rgb != null) return FastColor.ARGB32.color(255, rgb[0], rgb[1], rgb[2]);
        return FastColor.ARGB32.color(255, 170, 170, 170);
    }
```

- [ ] **Step 4: Run focused tests to verify GREEN**

Run: `./gradlew.bat --no-daemon --console=plain test --tests com.l2hostility_tweaks.client.config.ClientL2HConfigTest`

Expected: BUILD SUCCESSFUL with 5 tests passing。

- [ ] **Step 5: Inspect and commit the complete color fix**

Run: `git diff --check`

Expected: no whitespace errors; only `ClientL2HConfig` and its test changed.

```powershell
git add src/main/java/com/l2hostility_tweaks/client/config/ClientL2HConfig.java src/test/java/com/l2hostility_tweaks/client/config/ClientL2HConfigTest.java
git commit -m "fix: validate client color ranges"
```

### Task 3: 完整验证与 JAR 输出

**Files:**
- Verify: `build/test-results/test/TEST-*.xml`
- Verify: `build/libs/l2hostility_tweaks-1.0.0.jar`
- Output: `C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar`

**Interfaces:**
- Consumes: Task 1 和 Task 2 的已提交实现。
- Produces: 完整测试证据、干净工作树和带 SHA-256 的验证 JAR。

- [ ] **Step 1: Run a clean full build**

Run: `./gradlew.bat --no-daemon --console=plain clean build`

Expected: BUILD SUCCESSFUL; all tests have zero failures, errors, and skipped tests.

- [ ] **Step 2: Validate reports and production classes**

```powershell
$reports = Get-ChildItem build/test-results/test -Filter 'TEST-*.xml'
$tests = $failures = $errors = $skipped = 0
foreach ($report in $reports) {
    $xml = [xml](Get-Content -Raw $report.FullName)
    $tests += [int]$xml.testsuite.tests
    $failures += [int]$xml.testsuite.failures
    $errors += [int]$xml.testsuite.errors
    $skipped += [int]$xml.testsuite.skipped
}
[pscustomobject]@{Tests=$tests;Failures=$failures;Errors=$errors;Skipped=$skipped}
& 'D:\JAVA\JAVA_17\bin\jar.exe' tf build/libs/l2hostility_tweaks-1.0.0.jar |
    Select-String 'com/l2hostility_tweaks/client/config/ClientL2HConfig'
```

Expected: failures、errors、skipped 均为 `0`；JAR 包含 `ClientL2HConfig` 及其嵌套类。

- [ ] **Step 3: Copy and hash the verification JAR**

```powershell
$outputDir = 'C:\Users\Lenovo\Desktop\Ai_Run\output'
$target = Join-Path $outputDir 'l2hostility_tweaks-1.0.0.jar'
New-Item -ItemType Directory -Force $outputDir | Out-Null
Copy-Item -Force 'build\libs\l2hostility_tweaks-1.0.0.jar' $target
Get-Item $target | Select-Object FullName,Length,LastWriteTime
Get-FileHash -Algorithm SHA256 $target
```

Expected: JAR 位于 `Ai_Run\output`，不位于任何 `mods` 目录，并输出 SHA-256。

- [ ] **Step 4: Confirm repository state**

Run: `git status --short`

Expected: no output.
