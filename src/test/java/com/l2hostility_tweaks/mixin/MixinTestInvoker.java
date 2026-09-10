package com.l2hostility_tweaks.mixin;

import java.lang.reflect.Method;

final class MixinTestInvoker {

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
