package com.l2hostility_tweaks.mixin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class MixinTestInvoker {

    public interface Boundary {
        Object invoke(String owner, String name, List<Object> arguments);
    }

    public static ClassNode bytecode(String owner) throws Exception {
        try (var input = MixinTestInvoker.class.getClassLoader().getResourceAsStream(owner + ".class")) {
            if (input == null) throw new AssertionError(owner);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    public static Object replay(ClassNode owner, MethodNode method, Boundary boundary, Object... arguments) {
        Object[] locals = new Object[method.maxLocals];
        System.arraycopy(arguments, 0, locals, 0, arguments.length);
        List<Object> stack = new ArrayList<>();
        int remaining = 1000;
        for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
             instruction = instruction.getNext()) {
            if (--remaining == 0) throw new AssertionError("Replay exceeded bounded callback: " + method.name);
            int op = instruction.getOpcode();
            if (op < 0 || op == CHECKCAST) continue;
            switch (op) {
                case ALOAD, ILOAD, FLOAD -> stack.add(locals[((VarInsnNode) instruction).var]);
                case ASTORE, ISTORE, FSTORE -> locals[((VarInsnNode) instruction).var] = pop(stack);
                case ACONST_NULL -> stack.add(null);
                case ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5 -> stack.add(op - ICONST_0);
                case FCONST_0, FCONST_1, FCONST_2 -> stack.add((float) (op - FCONST_0));
                case BIPUSH, SIPUSH -> stack.add(((IntInsnNode) instruction).operand);
                case LDC -> stack.add(((LdcInsnNode) instruction).cst);
                case POP -> pop(stack);
                case IADD -> { int right = number(pop(stack)); stack.add(number(pop(stack)) + right); }
                case FCMPL, FCMPG -> {
                    float right = ((Number) pop(stack)).floatValue();
                    float left = ((Number) pop(stack)).floatValue();
                    stack.add(Float.isNaN(left) || Float.isNaN(right) ? (op == FCMPL ? -1 : 1)
                            : left > right ? 1 : left == right ? 0 : -1);
                }
                case GETSTATIC -> {
                    FieldInsnNode field = (FieldInsnNode) instruction;
                    stack.add(boundary.invoke(field.owner, field.name, List.of()));
                }
                case INVOKESTATIC, INVOKEVIRTUAL, INVOKEINTERFACE, INVOKESPECIAL -> {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    List<Object> args = new ArrayList<>();
                    for (Type ignored : Type.getArgumentTypes(call.desc)) args.add(0, pop(stack));
                    if (op != INVOKESTATIC) args.add(0, pop(stack));
                    Object result = boundary.invoke(call.owner, call.name, args);
                    if (Type.getReturnType(call.desc).getSort() != Type.VOID) stack.add(result);
                }
                case GOTO -> instruction = ((JumpInsnNode) instruction).label;
                case IFNULL, IFNONNULL, IFEQ, IFNE, IFLT, IFLE, IFGT, IFGE,
                     IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPLE, IF_ICMPGT, IF_ICMPGE -> {
                    Object right = pop(stack);
                    int comparison = op >= IF_ICMPEQ && op <= IF_ICMPLE
                            ? Integer.compare(number(pop(stack)), number(right))
                            : right == null ? 0 : (right instanceof Number || right instanceof Boolean ? number(right) : 1);
                    boolean jump = switch (op) {
                        case IFNULL -> right == null;
                        case IFNONNULL -> right != null;
                        case IFEQ, IF_ICMPEQ -> comparison == 0;
                        case IFNE, IF_ICMPNE -> comparison != 0;
                        case IFLT, IF_ICMPLT -> comparison < 0;
                        case IFLE, IF_ICMPLE -> comparison <= 0;
                        case IFGT, IF_ICMPGT -> comparison > 0;
                        case IFGE, IF_ICMPGE -> comparison >= 0;
                        default -> throw new AssertionError(op);
                    };
                    if (jump) instruction = ((JumpInsnNode) instruction).label;
                }
                case RETURN -> { return null; }
                case IRETURN, ARETURN, FRETURN -> { return pop(stack); }
                default -> throw new AssertionError("Unsupported opcode " + op + " in " + method.name);
            }
        }
        throw new AssertionError("Callback fell through: " + method.name);
    }

    private static Object pop(List<Object> stack) {
        return stack.remove(stack.size() - 1);
    }

    private static int number(Object value) {
        return value instanceof Boolean flag ? (flag ? 1 : 0) : ((Number) value).intValue();
    }

    static <T> T call(Class<?> owner, String name, Object... args) {
        for (Method method : owner.getDeclaredMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != args.length) continue;
            try {
                method.setAccessible(true);
                return (T) method.invoke(null, args);
            } catch (IllegalArgumentException ignored) {
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError(exception);
            }
        }
        throw new AssertionError(owner.getName() + "#" + name);
    }

    private MixinTestInvoker() {
    }
}
