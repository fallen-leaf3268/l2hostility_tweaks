# L2Hostility Tweaks Stability Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复已证明的构建、资源配置和网络输入稳定性问题，同时保持合法游戏行为不变。

**Architecture:** 构建入口负责可靠选择 Java 并传播真实退出码；Gradle 校验任务在 `check` 阶段统一验证 JSON 与 Mixin 登记；服务端网络处理在访问注册表前拒绝非法资源 ID。每项修改独立验证并单独提交。

**Tech Stack:** Java 17、Minecraft Forge 1.20.1、ForgeGradle 6、Gradle 8.5、Groovy Gradle DSL、Mixin 0.8.5、PowerShell。

## Global Constraints

- 只修复能够由源码或命令输出证明的稳定性问题。
- 不调整游戏数值、现有玩法、配置默认值和依赖版本。
- 不新增运行时依赖，不重构大型类。
- 所有 JSON 与 Mixin 登记必须在构建阶段自动校验。
- 不复制或部署 JAR 到 `mods`。
- 最终构建受外部网络阻塞时，必须报告真实非零状态，不得声称构建通过。

---

### Task 1: Windows Gradle 包装脚本可靠退出

**Files:**
- Modify: `gradlew.bat`
- Modify: `gradle/wrapper/gradle-wrapper.properties`

**Interfaces:**
- Consumes: 可选的 `JAVA_HOME` 环境变量与 PATH 中的 `java.exe`。
- Produces: 能找到 Java 时启动 Gradle；失败时返回真实非零退出码；Wrapper 下载等待最多 60000 毫秒。

- [ ] **Step 1: 运行未设置 `JAVA_HOME` 的失败复现**

Run:

```powershell
cmd /d /c "set JAVA_HOME=& gradlew.bat --version"
echo $LASTEXITCODE
```

Expected: 输出“找不到指定路径”，但退出码错误地为 `0`，证明包装脚本吞掉失败。

- [ ] **Step 2: 修改包装脚本的 Java 选择和退出码传播**

将 `gradlew.bat` 的执行段替换为：

```bat
@rem Select Java executable
if defined JAVA_HOME (
    set "JAVACMD=%JAVA_HOME%\bin\java.exe"
    if not exist "%JAVACMD%" goto fail
) else (
    set "JAVACMD=java.exe"
    where java.exe >NUL 2>&1
    if errorlevel 1 goto fail
)

@rem Execute Gradle
"%JAVACMD%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
set EXIT_CODE=%ERRORLEVEL%
goto end

:fail
echo.
echo ERROR: JAVA_HOME is not set correctly and java.exe is not available on PATH.
echo.
set EXIT_CODE=1

:end
if "%OS%"=="Windows_NT" endlocal & exit /b %EXIT_CODE%
exit /b %EXIT_CODE%
```

- [ ] **Step 3: 提高 Wrapper 下载超时**

在 `gradle/wrapper/gradle-wrapper.properties` 中修改：

```properties
networkTimeout=60000
```

- [ ] **Step 4: 验证 PATH 回退和错误码传播**

Run:

```powershell
cmd /d /c "set JAVA_HOME=& set PATH=D:\JAVA\JAVA_17\bin;%PATH%& gradlew.bat --version"
echo $LASTEXITCODE
```

Expected: Gradle 能启动；若下载成功则退出码为 `0`，若下载失败则退出码为非零且保留 Wrapper 异常。

Run:

```powershell
cmd /d /c "set JAVA_HOME=Z:\missing-java& gradlew.bat --version"
echo $LASTEXITCODE
```

Expected: 显示明确 Java 错误，退出码为 `1`。

- [ ] **Step 5: 提交包装脚本修复**

```powershell
git add gradlew.bat gradle/wrapper/gradle-wrapper.properties
git commit -m "fix: 可靠启动 Gradle 包装脚本"
```

### Task 2: JSON 与 Mixin 一致性校验

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/resources/l2hostility_tweaks.mixins.json`

**Interfaces:**
- Consumes: `src/main/resources/**/*.json`、Mixin JSON 的 `mixins/client/server` 列表、Mixin Java 源文件名。
- Produces: `validateProjectResources` Gradle 任务；`check` 自动依赖该任务。

- [ ] **Step 1: 运行现有资源的失败复现**

Run:

```powershell
$file = 'src/main/resources/l2hostility_tweaks.mixins.json'
Get-Content -Raw -Encoding UTF8 -LiteralPath $file | ConvertFrom-Json | Out-Null
```

Expected: FAIL，指出数组结尾存在多余逗号。

- [ ] **Step 2: 添加最小资源校验任务**

在 `build.gradle` 顶部添加：

```groovy
import groovy.json.JsonSlurper
```

在 `build.gradle` 的 Java 编译配置后添加：

```groovy
tasks.register('validateProjectResources') {
    def resourceFiles = fileTree('src/main/resources') {
        include '**/*.json'
    }
    def mixinSources = fileTree('src/main/java/com/l2hostility_tweaks/mixin') {
        include '*.java'
    }
    inputs.files(resourceFiles, mixinSources)

    doLast {
        resourceFiles.files.sort().each { jsonFile ->
            try {
                new JsonSlurper().parse(jsonFile, 'UTF-8')
            } catch (Exception exception) {
                throw new GradleException("Invalid JSON: ${relativePath(jsonFile)}", exception)
            }
        }

        def config = new JsonSlurper().parse(
                file('src/main/resources/l2hostility_tweaks.mixins.json'), 'UTF-8')
        def registered = ['mixins', 'client', 'server']
                .collectMany { key -> (config[key] ?: []) as List }
                .collect { it as String }
                .toSet()
        def sources = mixinSources.files
                .collect { it.name.substring(0, it.name.length() - '.java'.length()) }
                .toSet()
        def missingSources = registered - sources
        def unregisteredSources = sources - registered

        if (!missingSources.isEmpty() || !unregisteredSources.isEmpty()) {
            throw new GradleException(
                    "Mixin registration mismatch. Missing sources: ${missingSources.sort()}; " +
                            "unregistered sources: ${unregisteredSources.sort()}")
        }
    }
}

tasks.named('check').configure {
    dependsOn tasks.named('validateProjectResources')
}
```

- [ ] **Step 3: 运行校验并确认它捕获现有缺陷**

Run:

```powershell
$env:JAVA_HOME = 'D:\JAVA\JAVA_17'
.\gradlew.bat validateProjectResources
```

Expected: FAIL，错误包含 `Invalid JSON: src/main/resources/l2hostility_tweaks.mixins.json`。

- [ ] **Step 4: 修复 Mixin JSON**

将数组末尾：

```json
        "PocketOfRestorationMixin",
```

改为：

```json
        "PocketOfRestorationMixin"
```

- [ ] **Step 5: 验证资源和 Mixin 双向一致**

Run:

```powershell
$env:JAVA_HOME = 'D:\JAVA\JAVA_17'
.\gradlew.bat validateProjectResources
```

Expected: PASS，任务退出码为 `0`。

- [ ] **Step 6: 提交资源校验修复**

```powershell
git add build.gradle src/main/resources/l2hostility_tweaks.mixins.json
git commit -m "fix: 构建时校验资源与 Mixin 登记"
```

### Task 3: 非法网络词条 ID 安全拒绝

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/network/NetworkHandler.java`

**Interfaces:**
- Consumes: `UnloadTraitPacket.traitId()` 客户端字符串。
- Produces: 合法字符串对应的 `ResourceLocation`，或对非法字符串直接返回；合法卸载调用不变。

- [ ] **Step 1: 运行危险构造器的失败检查**

Run:

```powershell
$path = 'src/main/java/com/l2hostility_tweaks/network/NetworkHandler.java'
if (Select-String -Quiet -LiteralPath $path -Pattern 'new ResourceLocation\(msg\.traitId\)') {
    throw 'Unsafe packet ResourceLocation constructor is still present'
}
```

Expected: FAIL，证明网络包仍可触发抛异常的构造路径。

- [ ] **Step 2: 实现最小安全解析**

将：

```java
MobTrait trait = LHTraits.TRAITS.get().getValue(new ResourceLocation(msg.traitId));
if (trait == null) return;
```

替换为：

```java
ResourceLocation traitLocation = ResourceLocation.tryParse(msg.traitId);
if (traitLocation == null) return;
MobTrait trait = LHTraits.TRAITS.get().getValue(traitLocation);
if (trait == null) return;
```

- [ ] **Step 3: 运行安全解析静态回归检查**

Run:

```powershell
$path = 'src/main/java/com/l2hostility_tweaks/network/NetworkHandler.java'
if (Select-String -Quiet -LiteralPath $path -Pattern 'new ResourceLocation\(msg\.traitId\)') {
    throw 'Unsafe packet ResourceLocation constructor is still present'
}
if (-not (Select-String -Quiet -LiteralPath $path -Pattern 'ResourceLocation\.tryParse\(msg\.traitId\)')) {
    throw 'Safe packet ResourceLocation parser is missing'
}
```

Expected: PASS，无输出且退出码为 `0`。

- [ ] **Step 4: 编译验证 API 与合法流程**

Run:

```powershell
$env:JAVA_HOME = 'D:\JAVA\JAVA_17'
.\gradlew.bat compileJava
```

Expected: PASS，证明 Forge 1.20.1 映射中存在 `ResourceLocation.tryParse`，并且网络处理代码可编译。

- [ ] **Step 5: 提交网络输入修复**

```powershell
git add src/main/java/com/l2hostility_tweaks/network/NetworkHandler.java
git commit -m "fix: 拒绝非法词条网络标识"
```

### Task 4: 完整验证与交付检查

**Files:**
- Verify: `gradlew.bat`
- Verify: `build.gradle`
- Verify: `gradle/wrapper/gradle-wrapper.properties`
- Verify: `src/main/resources/l2hostility_tweaks.mixins.json`
- Verify: `src/main/java/com/l2hostility_tweaks/network/NetworkHandler.java`

**Interfaces:**
- Consumes: 前三项任务的提交。
- Produces: 可审计的最终构建、差异与仓库状态证据。

- [ ] **Step 1: 执行完整构建**

Run:

```powershell
$env:JAVA_HOME = 'D:\JAVA\JAVA_17'
.\gradlew.bat clean build --stacktrace
```

Expected: `BUILD SUCCESSFUL` 且退出码为 `0`。如外部下载失败，退出码必须非零，并记录网络异常作为未完成阻塞。

- [ ] **Step 2: 检查差异质量**

Run:

```powershell
git diff HEAD~3 --check
git diff HEAD~3 --stat
git status --short --branch
```

Expected: `git diff --check` 无输出；差异只包含计划文件；工作树干净。

- [ ] **Step 3: 核对不部署约束**

Run:

```powershell
git status --short
```

Expected: 没有 `mods` 路径或额外复制产物；本任务不执行任何部署命令。

