package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.content.traits.common.AdaptingTrait;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.BooleanSupplier;
import java.util.function.IntUnaryOperator;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AdaptingTraitMixinTest {

    private static final String TRAIT_CLASS = "dev/xkmc/l2hostility/content/traits/common/AdaptingTrait.class";

    @Test
    void interceptsActualGameJarDamageImplementationExactlyOnce() throws Exception {
        Path jar = Path.of("libs/l2hostility-adaptive-regression.jar");
        assumeTrue(Files.isRegularFile(jar), "Optional game runtime fixture is not installed");
        try (JarFile file = new JarFile(jar.toFile());
             InputStream input = file.getInputStream(file.getJarEntry(TRAIT_CLASS))) {
            assertSingleDamageHook(readClass(input));
        }
    }

    @Test
    void interceptsBuildDependencyDamageImplementationExactlyOnce() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(TRAIT_CLASS)) {
            ClassNode target = readClass(input);
            String expected = System.getProperty("l2htweaks.adaptive.expectedHook");
            assertNotNull(expected, "Gradle must select the version under test explicitly");
            assertTrue(target.methods.stream().anyMatch(method -> expected.equals(method.name)),
                    "Test runtime must contain the selected upstream implementation: " + expected);
            assertSingleDamageHook(target);
        }
    }

    @Test
    void damageHooksRequireExactlyOneRuntimeMatch() throws Exception {
        List<MethodNode> hooks = damageHooks();
        assertEquals(2, hooks.size());
        for (MethodNode hook : hooks) {
            AnnotationNode group = annotation(hook, "Lorg/spongepowered/asm/mixin/injection/Group;");
            assertNotNull(group, hook.name);
            assertEquals("l2fix$adaptiveDamage", value(group, "name"));
            assertEquals(1, value(group, "min"));
            assertEquals(1, value(group, "max"));
            AnnotationNode inject = annotation(hook, "Lorg/spongepowered/asm/mixin/injection/Inject;");
            assertEquals(0, value(inject, "require"));
            assertEquals(0, value(inject, "expect"), "Optional hooks must also work with mixin.debug.countInjections");
            assertEquals(true, value(inject, "cancellable"));
        }
    }

    @Test
    void bothAdaptersUseOneSharedPolicyAndTheirOwnDamageStage() throws Exception {
        List<MethodNode> hooks = damageHooks();
        assertEquals(2, hooks.size());
        for (MethodNode hook : hooks) {
            List<String> calls = new ArrayList<>();
            for (var instruction : hook.instructions) {
                if (instruction instanceof MethodInsnNode call) calls.add(call.name);
            }
            assertEquals(1, calls.stream().filter("l2fix$getDamageMultiplier"::equals).count());
            assertEquals(1, calls.stream().filter("cancel"::equals).count());
            List<String> selectors = (List<String>) value(annotation(hook,
                    "Lorg/spongepowered/asm/mixin/injection/Inject;"), "method");
            if (selectors.get(0).startsWith("onDamaged(")) {
                assertTrue(calls.contains("addDealtModifier"));
                assertFalse(calls.contains("setAmount"));
            } else {
                assertTrue(selectors.get(0).startsWith("onHurtByOthers("));
                assertTrue(calls.contains("setAmount"));
                assertFalse(calls.contains("addDealtModifier"));
            }
        }
    }

    @Test
    void repeatedHitsStopAtConfiguredCap() {
        AdaptingTrait.Data data = new AdaptingTrait.Data();
        for (int hit = 1; hit <= 200; hit++) {
            double reduction = update(data, 1, "player", 0.1, 0.95);
            assertEquals(Math.min(hit * 0.1, 0.95), reduction, 1e-12);
            assertTrue(60 * (1 - reduction) >= 3 - 1e-10);
        }
        assertEquals(10, data.adaption.get("player"));
    }

    @Test
    void clampsOldExponentialStacksAndReloadedLowerCap() {
        AdaptingTrait.Data data = new AdaptingTrait.Data();
        data.memory.add("player");
        data.adaption.put("player", Integer.MAX_VALUE);
        assertEquals(0.95, update(data, 1, "player", 0.1, 0.95));
        assertEquals(0.35, update(data, 1, "player", 0.1, 0.35));
        assertEquals(4, data.adaption.get("player"));
    }

    @Test
    void zeroSettingsAndInactiveTraitDoNotMutateMemory() {
        AdaptingTrait.Data data = new AdaptingTrait.Data();
        assertEquals(0, update(data, 1, "player", 0, 0.95));
        assertEquals(0, update(data, 1, "player", 0.1, 0));
        assertEquals(0, update(data, 0, "player", 0.1, 0.95));
        assertTrue(data.memory.isEmpty());
        assertTrue(data.adaption.isEmpty());
    }

    @Test
    void damageTypeReplacementRespectsTraitMemoryLimit() {
        AdaptingTrait.Data data = new AdaptingTrait.Data();
        update(data, 1, "player", 0.1, 0.95);
        update(data, 1, "player", 0.1, 0.95);
        assertEquals(0.1, update(data, 1, "arrow", 0.1, 0.95));
        assertEquals(List.of("arrow"), data.memory);
        assertFalse(data.adaption.containsKey("player"));
    }

    @Test
    void ringBypassesBothModesWithoutUpdatingAdaptation() {
        DoubleSupplier unused = () -> { throw new AssertionError("Bypass must not update adaptation"); };
        assertEquals(1f, resolve(true, true, unused));
        assertEquals(1f, resolve(true, false, unused));
        assertNull(resolve(false, false, unused));
        assertEquals(0.05f, resolve(false, true, () -> 0.95), 1e-7f);
    }

    @Test
    void combatMixinsMatchOneDefenseEntryInEachSupportedVersion() throws Exception {
        Path modern = Path.of("libs/l2hostility-adaptive-regression.jar");
        Path legacy = Path.of(System.getProperty("l2htweaks.legacyJar", modern.toString()));
        assertDefenseMatches(modern, "DispellTrait", "l2fix$dispellDefense");
        assertDefenseMatches(modern, "DementorTrait", "l2fix$dementorDefense");
        assertDefenseMatches(legacy, "DispellTrait", "l2fix$dispellDefense");
        assertDefenseMatches(legacy, "DementorTrait", "l2fix$dementorDefense");
    }

    @Test
    void sealedBonusSkipsTraitInvocation() {
        DoubleSupplier unused = () -> { throw new AssertionError("inactive trait must not run"); };
        assertEquals(1D, MixinTestInvoker.call(LHAttackListenerMixin.class,
                "l2fix$resolveBonus", 0, unused));
        assertEquals(1D, MixinTestInvoker.call(LHAttackListenerMixin.class,
                "l2fix$resolveBonus", -2, unused));
        assertEquals(0.75D, MixinTestInvoker.call(LHAttackListenerMixin.class,
                "l2fix$resolveBonus", 2, (DoubleSupplier) () -> 0.75D));
    }

    @Test
    void drainSideEffectsOnlyRunForOrdinaryPositiveHits() {
        BooleanSupplier unused = () -> { throw new AssertionError("skipped hit must not run"); };
        assertFalse(com.l2hostility_tweaks.util.TraitDisableHelper.runPositiveHitSideEffect(false, 3F, unused));
        assertFalse(com.l2hostility_tweaks.util.TraitDisableHelper.runPositiveHitSideEffect(true, 0F, unused));
        int[] calls = {0};
        assertTrue(com.l2hostility_tweaks.util.TraitDisableHelper.runPositiveHitSideEffect(true, 3F,
                (BooleanSupplier) () -> { calls[0]++; return true; }));
        assertEquals(1, calls[0]);
    }

    private static void assertDefenseMatches(Path jar, String trait, String hookName) throws Exception {
        String targetName = "dev/xkmc/l2hostility/content/traits/legendary/" + trait + ".class";
        try (JarFile file = new JarFile(jar.toFile()); InputStream input = file.getInputStream(file.getJarEntry(targetName))) {
            ClassNode target = readClass(input);
            List<String> methods = defenseSelectors(hookName);
            long matches = methods.stream().flatMap(selector -> target.methods.stream()
                    .filter(method -> selector.equals(method.name + method.desc))).count();
            assertEquals(1, matches, trait + " must match exactly one defense callback in " + jar);
        }
    }

    private static List<String> defenseSelectors(String hookName) throws Exception {
        try (InputStream input = AdaptingTraitMixinTest.class.getClassLoader().getResourceAsStream(
                "com/l2hostility_tweaks/mixin/" + (hookName.contains("dispell") ? "Dispell" : "Dementor") + "TraitMixin.class")) {
            List<MethodNode> hooks = readClass(input).methods.stream().filter(method -> {
                AnnotationNode group = annotation(method, "Lorg/spongepowered/asm/mixin/injection/Group;");
                return group != null && String.valueOf(value(group, "name")).contains("Defense");
            }).toList();
            assertEquals(2, hooks.size());
            for (MethodNode hook : hooks) {
                AnnotationNode group = annotation(hook, "Lorg/spongepowered/asm/mixin/injection/Group;");
                assertEquals(1, value(group, "min"));
                assertEquals(1, value(group, "max"));
                List<String> calls = new ArrayList<>();
                for (var instruction : hook.instructions) {
                    if (instruction instanceof MethodInsnNode call) calls.add(call.name);
                }
                assertEquals(1, calls.stream().filter("resolveLivingAttacker"::equals).count());
                assertEquals(1, calls.stream().filter("hasCombatCurioWithTag"::equals).count());
                assertTrue(calls.stream().filter("cancel"::equals).count() >= 1);
            }
            return hooks.stream().flatMap(hook -> ((List<String>) value(annotation(hook,
                    "Lorg/spongepowered/asm/mixin/injection/Inject;"), "method")).stream()).toList();
        }
    }

    private static double update(AdaptingTrait.Data data, int level, String id, double perStack, double cap) {
        return MixinTestInvoker.call(AdaptingTraitMixin.class, "l2fix$updateAdaptation",
                data, level, id, perStack, cap, (IntUnaryOperator) bound -> 0);
    }

    private static Float resolve(boolean bypass, boolean enabled, DoubleSupplier reduction) {
        return MixinTestInvoker.call(AdaptingTraitMixin.class, "l2fix$resolveMultiplier",
                bypass, enabled, reduction);
    }

    private static void assertSingleDamageHook(ClassNode target) throws Exception {
        int matches = 0;
        for (MethodNode hook : damageHooks()) {
            AnnotationNode inject = annotation(hook, "Lorg/spongepowered/asm/mixin/injection/Inject;");
            for (String selector : (List<String>) value(inject, "method")) {
                matches += (int) target.methods.stream()
                        .filter(method -> selector.equals(method.name + method.desc) || selector.equals(method.name))
                        .count();
            }
        }
        assertEquals(1, matches, "Must intercept the damage implementation declared by the actual AdaptingTrait");
    }

    private static List<MethodNode> damageHooks() throws Exception {
        try (InputStream input = AdaptingTraitMixinTest.class.getClassLoader().getResourceAsStream(
                "com/l2hostility_tweaks/mixin/AdaptingTraitMixin.class")) {
            return readClass(input).methods.stream().filter(method -> {
                AnnotationNode inject = annotation(method, "Lorg/spongepowered/asm/mixin/injection/Inject;");
                if (inject == null) return false;
                return ((List<String>) value(inject, "method")).stream()
                        .anyMatch(selector -> selector.startsWith("onDamaged") || selector.startsWith("onHurtByOthers"));
            }).toList();
        }
    }

    private static ClassNode readClass(InputStream input) throws Exception {
        assertNotNull(input);
        ClassNode result = new ClassNode();
        new ClassReader(input).accept(result, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return result;
    }

    private static AnnotationNode annotation(MethodNode method, String descriptor) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (method.visibleAnnotations != null) annotations.addAll(method.visibleAnnotations);
        if (method.invisibleAnnotations != null) annotations.addAll(method.invisibleAnnotations);
        return annotations.stream().filter(node -> descriptor.equals(node.desc)).findFirst().orElse(null);
    }

    private static Object value(AnnotationNode annotation, String name) {
        if (annotation.values == null) return null;
        for (int index = 0; index < annotation.values.size(); index += 2) {
            if (name.equals(annotation.values.get(index))) return annotation.values.get(index + 1);
        }
        return null;
    }
}
