package com.l2hostility_tweaks.compat.jei;

import com.l2hostility_tweaks.client.TraitSpawnClientCache;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@JeiPlugin
public final class L2HTweaksJeiPlugin implements IModPlugin {

    public static final ResourceLocation ID = new ResourceLocation("l2hostility_tweaks", "mob_traits");
    private static final AtomicBoolean LISTENER_ATTACHED = new AtomicBoolean();

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerIngredients(IModIngredientRegistration registration) {
        registration.register(MobIngredient.TYPE, List.of(),
                new MobIngredientHelper(), new MobIngredientRenderer(16));
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new TraitOverviewCategory(registration.getJeiHelpers()));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        JeiRuntimeBridge.INSTANCE.runtimeAvailable(runtime);
        if (LISTENER_ATTACHED.compareAndSet(false, true)) {
            TraitSpawnClientCache.INSTANCE.addListener(JeiRuntimeBridge.INSTANCE::refresh);
        }
        JeiRuntimeBridge.INSTANCE.refresh(TraitSpawnClientCache.INSTANCE.current());
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiRuntimeBridge.INSTANCE.runtimeUnavailable();
    }
}
