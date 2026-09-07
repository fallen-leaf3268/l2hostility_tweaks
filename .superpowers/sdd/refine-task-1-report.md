# Task 1 报告：JEI 生物预览保守投影缩放

## RED 证据

在未修改生产代码时运行：

```powershell
$env:JAVA_HOME='D:\JAVA\JAVA_17'
$env:GRADLE_USER_HOME='C:\Users\Lenovo\Desktop\Ai_Run\.gradle-cache'
.\gradlew.bat --no-daemon --offline test --tests com.l2hostility_tweaks.compat.jei.MobIngredientRendererTest
```

结果：9 个测试中 3 个失败，且失败符合旧实现行为：

- `previewLayoutRotatesThroughAFullTurnEveryTwelveSeconds`：期望 `0.0`，实际 `145.0`。
- `previewLayoutUsesConservativeProjectionEnvelopeAndFixedFootAnchors`：保守投影包络断言失败。
- `previewLayoutUsesModuloBeforeConvertingLargeAnimationTimestamps`：期望 `0.0`，实际 `161.31827`。

## GREEN 实现

- 将缩放改为水平 `width × sqrt(2) × 1.1` 与垂直 `height + width × sqrt(2) × sin(10°)` 包络的较小限制。
- 48 像素预览使用可用范围 40、上限 20、脚底基线 45；16 像素预览使用 12、6、15。
- 使用 `Math.floorMod(animationMillis, 12_000L)` 后计算 0–360° 旋转，避免大时间戳浮点精度损失。
- 保持 10° 俯视、硬裁剪、fallback 与状态恢复代码不变。

## GREEN 验证

控制器复跑以下指定命令后确认：`MobIngredientRendererTest` 的 9 个测试、0 failures，`BUILD SUCCESSFUL`（36 秒）。

## 修改文件

- `src/main/java/com/l2hostility_tweaks/compat/jei/MobIngredientRenderer.java`
- `src/test/java/com/l2hostility_tweaks/compat/jei/MobIngredientRendererTest.java`
- `.superpowers/sdd/refine-task-1-report.md`

## 风险

无已知风险；布局计算覆盖普通、劫掠兽、远古守卫者、极高和极大实体，以及 48/16 像素框的包络限制。
