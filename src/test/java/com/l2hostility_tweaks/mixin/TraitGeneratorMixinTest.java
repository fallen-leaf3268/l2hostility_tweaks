package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.content.logic.TraitGenerator;
import com.l2hostility_tweaks.generation.TraitGenerationHelper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitGeneratorMixinTest {

    @Test
    void appliesNbtPresetOnlyWhenItRaisesTheCurrentRank() {
        assertTrue(TraitGeneratorMixin.l2fix$shouldApplyPreset(0, 2));
        assertTrue(TraitGeneratorMixin.l2fix$shouldApplyPreset(1, 3));
        assertFalse(TraitGeneratorMixin.l2fix$shouldApplyPreset(3, 1));
        assertFalse(TraitGeneratorMixin.l2fix$shouldApplyPreset(2, 2));
    }

    @Test
    void locksPreInitializationTraitFilteringBytecodeContract() throws Exception {
        List<String> generatorCalls = methodCalls(TraitGenerator.class, "generate", "()V");
        String entrySet = "java/util/HashMap#entrySet()Ljava/util/Set;";
        String initialize = "dev/xkmc/l2hostility/content/traits/base/MobTrait#initialize(Lnet/minecraft/world/entity/LivingEntity;I)V";
        assertEquals(1, generatorCalls.stream().filter(entrySet::equals).count());
        assertEquals(1, generatorCalls.stream().filter(initialize::equals).count());
        assertTrue(generatorCalls.indexOf(entrySet) < generatorCalls.indexOf(initialize));

        Method preparation = Arrays.stream(TraitGeneratorMixin.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("l2fix$prepareFinalTraits"))
                .findFirst()
                .orElse(null);
        assertNotNull(preparation);
        Inject inject = preparation.getAnnotation(Inject.class);
        assertNotNull(inject);
        At at = inject.at()[0];
        assertEquals("INVOKE", at.value());
        assertEquals("Ljava/util/HashMap;entrySet()Ljava/util/Set;", at.target());
        assertEquals(At.Shift.BEFORE, at.shift());
        assertEquals(1, inject.require());

        assertEquals(List.of(
                "com/l2hostility_tweaks/mixin/TraitGeneratorMixin#l2fix$applyNbtPresets(Lnet/minecraft/world/entity/LivingEntity;Ljava/util/HashMap;)V",
                "com/l2hostility_tweaks/generation/TraitGenerationHelper#applyFinalFilters(Lnet/minecraft/world/entity/LivingEntity;Ljava/util/HashMap;I)V"),
                methodCalls(TraitGeneratorMixin.class, "l2fix$prepareFinalTraits", "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V"));
        assertEquals(0, methodCalls(TraitGeneratorMixin.class, null, null).stream()
                .filter(initialize::equals)
                .count());
    }

    @Test
    void exclusionRollUsesTheEntityRandomSourceWithoutAllocatingJavaRandom() throws IOException {
        String applyDescriptor = "(Ljava/util/HashMap;Lnet/minecraft/util/RandomSource;)V";
        List<String> exclusionCalls = methodCalls(
                TraitGenerationHelper.class, "applyExclusions", applyDescriptor);
        assertEquals(1, exclusionCalls.stream()
                .filter("net/minecraft/util/RandomSource#nextInt(I)I"::equals)
                .count());
        assertEquals(0, exclusionCalls.stream()
                .filter("java/util/Random#<init>()V"::equals)
                .count());

        List<String> finalFilterCalls = methodCalls(
                TraitGenerationHelper.class, "applyFinalFilters",
                "(Lnet/minecraft/world/entity/LivingEntity;Ljava/util/HashMap;I)V");
        String entityRandom = "net/minecraft/world/entity/LivingEntity#getRandom()Lnet/minecraft/util/RandomSource;";
        String applyExclusions = "com/l2hostility_tweaks/generation/TraitGenerationHelper#applyExclusions" +
                applyDescriptor;
        assertTrue(finalFilterCalls.indexOf(entityRandom) >= 0);
        assertTrue(finalFilterCalls.indexOf(entityRandom) < finalFilterCalls.indexOf(applyExclusions));
    }

    @Test
    void traitGenerationStateAccessDoesNotUseReflection() throws IOException {
        assertEquals(0, Arrays.stream(TraitGenerationHelper.class.getDeclaredFields())
                .filter(field -> field.getType() == java.lang.reflect.Field.class)
                .count());
        assertEquals(0, methodCalls(TraitGenerationHelper.class, null, null).stream()
                .filter(call -> call.startsWith("java/lang/reflect/Field#") ||
                        call.startsWith("java/lang/Class#getDeclaredFields"))
                .count());
        assertEquals(0, Arrays.stream(TraitGeneratorMixin.class.getDeclaredFields())
                .filter(field -> field.getType() == java.lang.reflect.Field.class)
                .count());
        assertEquals(0, methodCalls(TraitGeneratorMixin.class, null, null).stream()
                .filter(call -> call.startsWith("java/lang/reflect/Field#") ||
                        call.startsWith("java/lang/Class#getDeclaredFields"))
                .count());
        assertEquals(0, methodCalls(TraitPostRollMixin.class, null, null).stream()
                .filter(call -> call.startsWith("java/lang/reflect/Field#") ||
                        call.startsWith("java/lang/Class#getDeclaredFields"))
                .count());

        List<String> annotations = fieldAnnotations(TraitPostRollMixin.class);
        String shadow = "#Lorg/spongepowered/asm/mixin/Shadow;";
        String immutable = "#Lorg/spongepowered/asm/mixin/Final;";
        assertTrue(annotations.contains("entity:Lnet/minecraft/world/entity/LivingEntity;" + shadow));
        assertTrue(annotations.contains("mobLevel:I" + shadow));
        assertTrue(annotations.contains("ins:Ldev/xkmc/l2hostility/content/logic/MobDifficultyCollector;" + shadow));
        assertTrue(annotations.contains("traits:Ljava/util/HashMap;" + shadow));
        assertTrue(annotations.contains("level:I" + shadow));
        assertTrue(annotations.contains("entity:Lnet/minecraft/world/entity/LivingEntity;" + immutable));
        assertTrue(annotations.contains("mobLevel:I" + immutable));
        assertTrue(annotations.contains("ins:Ldev/xkmc/l2hostility/content/logic/MobDifficultyCollector;" + immutable));
        assertTrue(annotations.contains("traits:Ljava/util/HashMap;" + immutable));
        assertFalse(annotations.contains("level:I" + immutable));
    }

    private static List<String> fieldAnnotations(Class<?> type) throws IOException {
        List<String> annotations = new ArrayList<>();
        new ClassReader(type.getName()).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public FieldVisitor visitField(int access, String name, String descriptor,
                                           String signature, Object value) {
                return new FieldVisitor(Opcodes.ASM9) {
                    @Override
                    public org.objectweb.asm.AnnotationVisitor visitAnnotation(
                            String annotationDescriptor, boolean visible) {
                        annotations.add(name + ":" + descriptor + "#" + annotationDescriptor);
                        return null;
                    }
                };
            }
        }, 0);
        return annotations;
    }

    private static List<String> methodCalls(Class<?> type, String methodName, String descriptor) throws IOException {
        List<String> calls = new ArrayList<>();
        new ClassReader(type.getName()).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String methodDescriptor,
                                             String signature, String[] exceptions) {
                if (methodName != null && (!methodName.equals(name) || !descriptor.equals(methodDescriptor))) {
                    return null;
                }
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name,
                                                String methodDescriptor, boolean isInterface) {
                        calls.add(owner + "#" + name + methodDescriptor);
                    }
                };
            }
        }, 0);
        return calls;
    }
}
