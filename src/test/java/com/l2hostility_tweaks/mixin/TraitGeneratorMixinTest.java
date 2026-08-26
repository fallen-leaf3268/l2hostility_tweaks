package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.content.logic.TraitGenerator;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
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
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void parsesValidNbtPresetTraitId() {
        assertEquals("l2hostility:tank",
                TraitGeneratorMixin.l2fix$parseTraitId("l2hostility:tank").toString());
    }

    @Test
    void rejectsMalformedNbtPresetTraitIdWithoutThrowing() {
        assertNull(TraitGeneratorMixin.l2fix$parseTraitId("Invalid Trait ID"));
    }

    @Test
    void rejectsNullNbtPresetTraitId() {
        assertNull(TraitGeneratorMixin.l2fix$parseTraitId(null));
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
                "com/l2hostility_tweaks/mixin/TraitGeneratorMixin#l2fix$applyNbtPresets(Ldev/xkmc/l2hostility/content/logic/TraitGenerator;)V",
                "com/l2hostility_tweaks/generation/TraitGenerationHelper#applyFinalFilters(Ldev/xkmc/l2hostility/content/logic/TraitGenerator;)V"),
                methodCalls(TraitGeneratorMixin.class, "l2fix$prepareFinalTraits", "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V"));
        assertEquals(0, methodCalls(TraitGeneratorMixin.class, null, null).stream()
                .filter(initialize::equals)
                .count());
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
