package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.TraitDisableHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetTraitLevelMixinTest {

    @Test
    void mixinsDoNotDeclareNonPrivateStaticMethods() throws Exception {
        Pattern invalid = Pattern.compile(
                "(?m)^\\s*(?!private\\s)(?:public\\s+|protected\\s+)?static\\s+[^=\\n]+\\(");
        List<String> violations = new ArrayList<>();
        try (var files = Files.list(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin"))) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    String source = Files.readString(path);
                    if (source.contains("@Mixin") && invalid.matcher(source).find()) {
                        violations.add(path.getFileName().toString());
                    }
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
        }
        assertEquals(List.of(), violations);
    }

    @Test
    void productionJarIsAlwaysReobfuscated() throws Exception {
        String buildScript = Files.readString(Path.of("build.gradle"));
        assertTrue(buildScript.contains("jar.finalizedBy('reobfJar')"));
    }

    @Test
    void attackListenerRegistersAfterParallelModConstruction() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/L2HostilityFix.java"));
        int setupListener = source.indexOf("addListener(this::onCommonSetup)");
        int setupMethod = source.indexOf("void onCommonSetup(FMLCommonSetupEvent event)");
        int enqueue = source.indexOf("event.enqueueWork(", setupMethod);
        int registration = source.indexOf(
                "AttackEventHandler.register(4500, new RingDamageListener())");

        assertTrue(setupListener >= 0);
        assertTrue(setupMethod >= 0);
        assertTrue(enqueue > setupMethod);
        assertTrue(registration > enqueue);
    }

    @Test
    void onlyPositiveRawLevelsAreActive() {
        assertTrue(MixinTestInvoker.<Boolean>call(GetTraitLevelMixin.class, "l2fix$isActiveLevel", 1));
        assertFalse(MixinTestInvoker.<Boolean>call(GetTraitLevelMixin.class, "l2fix$isActiveLevel", 0));
        assertFalse(MixinTestInvoker.<Boolean>call(GetTraitLevelMixin.class, "l2fix$isActiveLevel", -1));
    }

    @Test
    void traitDescriptionUsesOneFormatterForNormalAndSealedLevels() throws Exception {
        String romanSource = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/TraitRomanMixin.java"));
        String detailSource = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/MobTraitDescMixin.java"));

        long getFullDescInjectors = List.of(romanSource, detailSource).stream()
                .flatMap(source -> source.lines())
                .filter(line -> line.contains("method = \"getFullDesc\""))
                .count();

        assertEquals(1, getFullDescInjectors);
        assertTrue(romanSource.contains("Math.abs(value)"));
        assertTrue(romanSource.contains("ChatFormatting.GRAY, ChatFormatting.STRIKETHROUGH"));
        assertFalse(detailSource.contains("enchantment.level."));
    }

    @Test
    void traitLevelTextUsesAbsoluteValueAndFallsBackAboveRomanRange() throws Exception {
        var method = Arrays.stream(TraitRomanMixin.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals("l2fix$levelText"))
                .findFirst().orElse(null);
        assertNotNull(method);
        method.setAccessible(true);

        assertNull(method.invoke(null, 1, true));
        assertEquals("V", method.invoke(null, -5, true));
        assertEquals("50", method.invoke(null, -50, false));
        assertEquals("4000", method.invoke(null, -4000, true));
    }

    @Test
    void iteratesActiveTraitsWithoutCopyingTheTraitMap() throws Exception {
        var method = Arrays.stream(TraitSealFilterMixin.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals("l2fix$forEachActive"))
                .findFirst().orElse(null);
        assertNotNull(method);
        method.setAccessible(true);

        var traits = new LinkedHashMap<String, Integer>();
        traits.put("active", 2);
        traits.put("sealed", -3);
        traits.put("absent", 0);
        var visited = new ArrayList<String>();
        BiConsumer<String, Integer> consumer = (trait, level) -> visited.add(trait + ":" + level);

        method.invoke(null, traits, consumer);

        assertEquals(List.of("active:2"), visited);
        assertEquals(List.of("active", "sealed", "absent"), new ArrayList<>(traits.keySet()));
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/TraitSealFilterMixin.java"));
        assertFalse(source.contains("new LinkedHashMap<"));
        assertTrue(source.contains("Ljava/util/LinkedHashMap;forEach(Ljava/util/function/BiConsumer;)V"));
    }

    @Test
    void clearsCounterStrikeTargetWhenItBecomesInvalid() {
        var strikeId = UUID.randomUUID();

        assertNull(MixinTestInvoker.call(CounterStrikeTraitMixin.class,
                "l2fix$clearInvalidTarget", strikeId, false));
        assertSame(strikeId, MixinTestInvoker.call(CounterStrikeTraitMixin.class,
                "l2fix$clearInvalidTarget", strikeId, true));
    }

    @Test
    void validatesCounterStrikeTargetBeforeEarlyTickReturns() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/CounterStrikeTraitMixin.java"));
        int validation = source.indexOf("data.strikeId = l2fix$clearInvalidTarget");
        int cooldown = source.indexOf("if (data.cooldown > 0)");
        int onGround = source.indexOf("if (!le.onGround())");

        assertTrue(validation >= 0);
        assertTrue(validation < cooldown);
        assertTrue(validation < onGround);
    }

    @Test
    void drainUsesCallbackLevelWithoutSharedCaptureState() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/DrainTraitMixin.java"));

        assertFalse(source.contains("l2fix$drainLevel"));
        assertFalse(source.contains("l2fix$captureHurt"));
        assertFalse(source.contains("l2fix$capturePostHurt"));
        assertTrue(source.contains("L2HConfig.getDrainDamage(level)"));
    }

    @Test
    void drainCachesIgnoreTagKeyAndBuildsOneCandidateList() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/DrainTraitMixin.java"));
        String compact = source.replaceAll("\\s+", " ");
        assertTrue(compact.contains("@Unique private static final TagKey<MobEffect> l2fix$drainIgnore"));
        assertTrue(source.contains("private static final TagKey<MobEffect> l2fix$drainIgnore"));

        int start = source.indexOf("private void l2fix$drainPostHurt");
        int end = source.indexOf("@Redirect", start);
        String postHurt = source.substring(start, end);
        assertFalse(postHurt.contains("TagKey.create"));
        assertFalse(postHurt.contains(".stream()"));
        assertFalse(postHurt.contains(".toList()"));
        assertTrue(postHurt.contains("new ArrayList<MobEffectInstance>()"));
        assertTrue(postHurt.contains("for (var effect : target.getActiveEffects())"));
        assertTrue(postHurt.contains("BuiltInRegistries.MOB_EFFECT.getTag(l2fix$drainIgnore)"));
        assertTrue(postHurt.contains("getCategory() != MobEffectCategory.BENEFICIAL"));
        assertTrue(postHurt.contains("ignored.contains("));
        assertTrue(postHurt.contains("pos.remove(target.getRandom().nextInt(pos.size()))"));
    }

    @Test
    void arenaRedirectUsesCurrentAttackWithoutSharedBypassState() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/ArenaTraitMixin.java"));
        String compact = source.replaceAll("\\s+", " ");
        assertFalse(source.contains("l2fix$bypass"));
        assertFalse(source.contains("l2fix$captureDamageBypass"));
        assertTrue(compact.contains("l2fix$skipReduction(AttackCache cache, DamageModifier modifier, "
                + "int level, LivingEntity mob, AttackCache originalCache)"));
        assertTrue(source.contains("cache.getAttacker() instanceof Player attacker"));
        assertTrue(source.contains("MobTraitCap.HOLDER.isProper(attacker)"));
        assertTrue(source.contains("getTraitLevel((ArenaTrait) (Object) this) >= level"));
        assertTrue(source.contains("cache.addDealtModifier(modifier)"));
    }

    @Test
    void dispellRestorationKeepsHighestDuplicateEnchantmentLevel() throws Exception {
        ListTag saved = new ListTag();
        CompoundTag sharpnessFive = new CompoundTag();
        sharpnessFive.putString("id", "minecraft:sharpness");
        sharpnessFive.putShort("lvl", (short) 5);
        saved.add(sharpnessFive);

        ListTag current = new ListTag();
        CompoundTag sharpnessOne = new CompoundTag();
        sharpnessOne.putString("id", "minecraft:sharpness");
        sharpnessOne.putShort("lvl", (short) 1);
        current.add(sharpnessOne);
        CompoundTag mending = new CompoundTag();
        mending.putString("id", "minecraft:mending");
        mending.putShort("lvl", (short) 1);
        current.add(mending);

        Method merge = Arrays.stream(TraitDisableHelper.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("mergeEnchantments"))
                .findFirst().orElse(null);
        assertNotNull(merge);
        merge.invoke(null, saved, current);

        assertEquals(2, saved.size());
        assertEquals("minecraft:sharpness", saved.getCompound(0).getString("id"));
        assertEquals(5, saved.getCompound(0).getShort("lvl"));
        assertEquals("minecraft:mending", saved.getCompound(1).getString("id"));
    }

    @Test
    void bothDispellRestorationEntrypointsUseTheSharedMerge() throws Exception {
        String equipment = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/DispealEquipmentTickMixin.java"));
        String upstream = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/EnchantmentDisablerRestoreMixin.java"));
        String mixins = Files.readString(Path.of(
                "src/main/resources/l2hostility_tweaks.mixins.json"));

        assertFalse(equipment.contains("saved.addAll(current)"));
        assertFalse(equipment.contains("static boolean l2fix$mergeEnchantments"));
        assertTrue(equipment.contains("TraitDisableHelper.mergeEnchantments(saved, current)"));
        assertTrue(upstream.contains("EnchantmentDisabler.class"));
        assertTrue(upstream.contains("ListTag;addAll(Ljava/util/Collection;)Z"));
        assertTrue(upstream.contains("TraitDisableHelper.mergeEnchantments"));
        assertTrue(mixins.contains("\"EnchantmentDisablerRestoreMixin\""));
    }
}

final class MixinTestInvoker {

    @SuppressWarnings("unchecked")
    static <T> T call(Class<?> owner, String name, Object... args) {
        for (Method method : owner.getDeclaredMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != args.length) continue;
            try {
                method.setAccessible(true);
                return (T) method.invoke(null, args);
            } catch (IllegalArgumentException ignored) {
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        }
        throw new AssertionError("Missing static helper " + owner.getName() + "." + name);
    }

    private MixinTestInvoker() {
    }
}
