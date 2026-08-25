# 词条文本像素折行实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让目标 HUD 与玩家词条面板共享样式感知的像素折行规则，消除超宽词条产生的空行、越界和错误分页。

**Architecture:** 新增纯泛型 `TraitTextLayout` 排版单元，只负责把带所有者的文本按测量器和拆分器排成行；Minecraft 字体拆分由两个界面在调用处注入。输出片段保留所有者、文本、横向偏移和宽度，使玩家面板的绘制、悬停与右键卸载共用同一命中数据。

**Tech Stack:** Java 17、Minecraft Forge 1.20.1、Minecraft `Font.split(FormattedText, int)`、JUnit 5、Gradle 8.5。

## Global Constraints

- 任何输入都不生成空排版行。
- 每个生产环境渲染片段的实际字体宽度不得超过对应界面的可用宽度。
- 普通词条继续以两个空格分隔并尽量同排。
- 不缩放、截断、省略或改写词条文本。
- 保留颜色、删除线及其他样式。
- 玩家面板的每个折行片段都必须支持原词条的悬停提示和右键卸载。
- HUD 与玩家面板必须调用同一个排版实现。
- 不改变 HUD 宽度配置、面板尺寸、每页行数、网络协议或卸载规则。
- 不把 JAR 部署到 `mods`；验证产物复制到 `C:\Users\Lenovo\Desktop\Ai_Run\output`。

---

### Task 1: 建立可测试的共享词条排版器

**Files:**
- Create: `src/main/java/com/l2hostility_tweaks/client/TraitTextLayout.java`
- Create: `src/test/java/com/l2hostility_tweaks/client/TraitTextLayoutTest.java`

**Interfaces:**
- Consumes: `List<Entry<O, S>>`、最大宽度、分隔文本、`ToIntFunction<T>` 测量器和 `BiFunction<S, Integer, List<T>>` 拆分器。
- Produces: `static <O, S, T> List<List<Segment<O, T>>> layout(...)`；`Entry` 保存所有者和源文本，`Segment` 保存所有者、渲染文本、横向偏移和实测宽度。

- [ ] **Step 1: Write failing packing and wrapping tests**

```java
package com.l2hostility_tweaks.client;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TraitTextLayoutTest {

	private static List<String> split(String text, int width) {
		List<String> result = new ArrayList<>();
		for (int start = 0; start < text.length(); start += width)
			result.add(text.substring(start, Math.min(text.length(), start + width)));
		return result;
	}

	@Test
	void packsNormalEntriesThroughExactBoundary() {
		var rows = TraitTextLayout.layout(List.of(
				new TraitTextLayout.Entry<>("first", "aa"),
				new TraitTextLayout.Entry<>("second", "bb")),
				5, " ", String::length, TraitTextLayoutTest::split);

		assertEquals(1, rows.size());
		assertEquals(List.of("aa", " ", "bb"),
				rows.get(0).stream().map(TraitTextLayout.Segment::text).toList());
		assertEquals(List.of(0, 2, 3),
				rows.get(0).stream().map(TraitTextLayout.Segment::xOffset).toList());
	}

	@Test
	void splitsFirstOversizedEntryWithoutLeadingEmptyRow() {
		var rows = TraitTextLayout.layout(List.of(
				new TraitTextLayout.Entry<>("trait", "abcdef")),
				3, " ", String::length, TraitTextLayoutTest::split);

		assertEquals(2, rows.size());
		assertEquals(List.of("abc", "def"),
				rows.stream().map(row -> row.get(0).text()).toList());
		assertTrue(rows.stream().noneMatch(List::isEmpty));
		assertTrue(rows.stream().flatMap(List::stream).allMatch(segment -> segment.width() <= 3));
		assertTrue(rows.stream().flatMap(List::stream).allMatch(segment -> segment.owner().equals("trait")));
	}

	@Test
	void flushesNormalRowBeforeOversizedEntryAndPreservesOrder() {
		var rows = TraitTextLayout.layout(List.of(
				new TraitTextLayout.Entry<>("short", "ab"),
				new TraitTextLayout.Entry<>("long", "1234567")),
				3, " ", String::length, TraitTextLayoutTest::split);

		assertEquals(List.of("ab", "123", "456", "7"),
				rows.stream().map(row -> row.get(0).text()).toList());
		assertEquals(List.of("short", "long", "long", "long"),
				rows.stream().map(row -> row.get(0).owner()).toList());
	}

	@Test
	void ignoresEmptySplitResultsAndClampsInvalidWidth() {
		var rows = TraitTextLayout.layout(List.of(
				new TraitTextLayout.Entry<>("empty", ""),
				new TraitTextLayout.Entry<>("visible", "x")),
				0, " ", String::length, TraitTextLayoutTest::split);

		assertEquals(1, rows.size());
		assertEquals("x", rows.get(0).get(0).text());
	}
}
```

- [ ] **Step 2: Run the focused test to verify RED**

Run: `./gradlew.bat test --tests com.l2hostility_tweaks.client.TraitTextLayoutTest`

Expected: FAIL during `compileTestJava` because `TraitTextLayout` does not exist.

- [ ] **Step 3: Implement the minimal generic layout unit**

Create a package-private final class with these exact declarations:

```java
final class TraitTextLayout {
	record Entry<O, S>(O owner, S text) {}
	record Segment<O, T>(O owner, T text, int xOffset, int width) {}

	static <O, S, T> List<List<Segment<O, T>>> layout(
			List<Entry<O, S>> entries, int maxWidth, T separator,
			ToIntFunction<T> width, BiFunction<S, Integer, List<T>> split) {
		int limit = Math.max(1, maxWidth);
		int separatorWidth = width.applyAsInt(separator);
		List<List<Segment<O, T>>> rows = new ArrayList<>();
		List<Segment<O, T>> current = new ArrayList<>();
		int currentWidth = 0;

		for (Entry<O, S> entry : entries) {
			List<T> fragments = split.apply(entry.text(), limit).stream()
					.filter(fragment -> width.applyAsInt(fragment) > 0).toList();
			if (fragments.isEmpty()) continue;

			if (fragments.size() > 1) {
				if (!current.isEmpty()) rows.add(List.copyOf(current));
				current = new ArrayList<>();
				currentWidth = 0;
				for (T fragment : fragments) {
					int fragmentWidth = width.applyAsInt(fragment);
					rows.add(List.of(new Segment<>(entry.owner(), fragment, 0, fragmentWidth)));
				}
				continue;
			}

			T fragment = fragments.get(0);
			int fragmentWidth = width.applyAsInt(fragment);
			int needed = current.isEmpty() ? fragmentWidth : separatorWidth + fragmentWidth;
			if (!current.isEmpty() && currentWidth + needed > limit) {
				rows.add(List.copyOf(current));
				current = new ArrayList<>();
				currentWidth = 0;
			}
			if (!current.isEmpty()) {
				current.add(new Segment<>(null, separator, currentWidth, separatorWidth));
				currentWidth += separatorWidth;
			}
			current.add(new Segment<>(entry.owner(), fragment, currentWidth, fragmentWidth));
			currentWidth += fragmentWidth;
		}
		if (!current.isEmpty()) rows.add(List.copyOf(current));
		return List.copyOf(rows);
	}

	private TraitTextLayout() {}
}
```

Add imports for `ArrayList`、`List`、`BiFunction` and `ToIntFunction`.

- [ ] **Step 4: Run the focused test to verify GREEN**

Run: `./gradlew.bat test --tests com.l2hostility_tweaks.client.TraitTextLayoutTest`

Expected: BUILD SUCCESSFUL with 4 tests passing.

- [ ] **Step 5: Commit the shared layout unit**

```powershell
git add src/main/java/com/l2hostility_tweaks/client/TraitTextLayout.java src/test/java/com/l2hostility_tweaks/client/TraitTextLayoutTest.java
git commit -m "feat: add shared trait text layout"
```

### Task 2: 接入目标 HUD 与玩家词条面板

**Files:**
- Modify: `src/main/java/com/l2hostility_tweaks/client/L2HHealthOverlay.java`
- Modify: `src/main/java/com/l2hostility_tweaks/client/PlayerTraitScreen.java`
- Modify: `src/test/java/com/l2hostility_tweaks/client/TraitTextLayoutTest.java`

**Interfaces:**
- Consumes: Task 1 的 `TraitTextLayout.Entry`、`TraitTextLayout.Segment` 和 `TraitTextLayout.layout`。
- Produces: 两个界面均缓存或保存 `FormattedCharSequence` 片段；玩家面板从 `Segment.owner()` 完成悬停和右键命中。

- [ ] **Step 1: Add a failing owner-preservation regression test**

在 `TraitTextLayoutTest` 增加一个带样式标识的值对象，并验证拆分器返回的每个对象原样进入结果：

```java
	record StyledText(String value, String style) {}

	@Test
	void preservesStyledFragmentsAndOwnerForInteraction() {
		StyledText first = new StyledText("abc", "gray-strikethrough");
		StyledText second = new StyledText("def", "gray-strikethrough");
		var rows = TraitTextLayout.layout(List.of(
				new TraitTextLayout.Entry<>("sealed", "source")),
				3, new StyledText(" ", "plain"), text -> text.value().length(),
				(text, width) -> List.of(first, second));

		assertSame(first, rows.get(0).get(0).text());
		assertSame(second, rows.get(1).get(0).text());
		assertEquals("sealed", rows.get(0).get(0).owner());
		assertEquals("sealed", rows.get(1).get(0).owner());
	}
```

- [ ] **Step 2: Run the focused test before UI integration**

Run: `./gradlew.bat test --tests com.l2hostility_tweaks.client.TraitTextLayoutTest`

Expected: BUILD SUCCESSFUL; this locks the fragment identity and owner semantics required by both UI consumers.

- [ ] **Step 3: Replace HUD-local wrapping with the shared layout**

In `L2HHealthOverlay`:

1. Import `MobTrait`、`FormattedText` and `FormattedCharSequence`.
2. Change `cachedTraitLines` to `List<List<TraitTextLayout.Segment<MobTrait, FormattedCharSequence>>>`.
3. In `scanTraits`, build `List<TraitTextLayout.Entry<MobTrait, FormattedText>>` from the existing styled `getFullDesc` components.
4. Assign the cache with:

```java
this.cachedTraitLines = TraitTextLayout.layout(entries, maxW,
		Component.literal("  ").getVisualOrderText(), mc.font::width, mc.font::split);
```

5. In `renderTraitLines`, draw every `segment.text()` at `(int) barX + segment.xOffset()` using the existing white fallback color and shadow. Remove the old manual `curLine`、`curWidth`、`needed` and separator loop.

- [ ] **Step 4: Replace player-screen wrapping and hit records with the shared layout**

In `PlayerTraitScreen`:

1. Import `FormattedText` and `FormattedCharSequence`.
2. Remove the private `TraitEntry` record.
3. Change `allLines` and `buildTraitLines` to `List<List<TraitTextLayout.Segment<MobTrait, FormattedCharSequence>>>`.
4. Build the same styled source entries and call `TraitTextLayout.layout` with panel width, the visual-order two-space separator, `font::width`, and `font::split`.
5. Render `segment.text()` at `traitX + segment.xOffset()`.
6. In hover and `mouseClicked`, skip `segment.owner() == null`; otherwise use `segment.owner()`、`segment.xOffset()` and `segment.width()` for the existing tooltip and unloading behavior.

- [ ] **Step 5: Run focused tests and compile both client consumers**

Run: `./gradlew.bat test --tests com.l2hostility_tweaks.client.TraitTextLayoutTest compileJava`

Expected: BUILD SUCCESSFUL with 5 layout tests passing and both client classes compiling against the mapped 1.20.1 API.

- [ ] **Step 6: Inspect and commit the UI integration**

Run: `git diff --check`

Expected: no whitespace errors and no remaining manual `curWidth + needed > maxW` wrapping in either client class.

```powershell
git add src/main/java/com/l2hostility_tweaks/client/L2HHealthOverlay.java src/main/java/com/l2hostility_tweaks/client/PlayerTraitScreen.java src/test/java/com/l2hostility_tweaks/client/TraitTextLayoutTest.java
git commit -m "fix: wrap oversized trait text"
```

### Task 3: 完整验证与 JAR 输出

**Files:**
- Verify: `build/test-results/test/TEST-*.xml`
- Verify: `build/libs/l2hostility_tweaks-1.0.0.jar`
- Output: `C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar`

**Interfaces:**
- Consumes: Task 1 和 Task 2 的已提交实现。
- Produces: 完整测试证据、干净工作树和带哈希的验证 JAR。

- [ ] **Step 1: Run a clean full build**

Run: `./gradlew.bat --no-daemon --console=plain clean build`

Expected: BUILD SUCCESSFUL; all existing tests plus 5 new layout tests have zero failures, errors, and skipped tests.

- [ ] **Step 2: Validate test reports and JAR contents**

```powershell
$xml = [xml](Get-Content -Raw build/test-results/test/TEST-com.l2hostility_tweaks.client.TraitTextLayoutTest.xml)
$xml.testsuite | Select-Object tests, failures, errors, skipped
& 'D:\JAVA\JAVA_17\bin\jar.exe' tf build/libs/l2hostility_tweaks-1.0.0.jar | Select-String 'TraitTextLayout|L2HHealthOverlay|PlayerTraitScreen'
```

Expected: layout suite reports `tests="5" failures="0" errors="0" skipped="0"`; JAR contains all three production classes and nested layout record classes.

- [ ] **Step 3: Copy and hash the verification artifact**

```powershell
New-Item -ItemType Directory -Force 'C:\Users\Lenovo\Desktop\Ai_Run\output' | Out-Null
Copy-Item -Force 'build\libs\l2hostility_tweaks-1.0.0.jar' 'C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar'
Get-Item 'C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar' | Select-Object FullName, Length, LastWriteTime
Get-FileHash -Algorithm SHA256 'C:\Users\Lenovo\Desktop\Ai_Run\output\l2hostility_tweaks-1.0.0.jar'
```

Expected: output JAR exists outside every `mods` directory and its SHA-256 is reported for verification.

- [ ] **Step 4: Confirm repository state**

Run: `git status --short`

Expected: no output.
