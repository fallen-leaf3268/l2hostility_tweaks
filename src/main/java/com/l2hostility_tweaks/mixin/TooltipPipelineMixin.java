package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.client.tooltip.TooltipPipeline;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public class TooltipPipelineMixin {

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void l2fix$applyTooltipPipeline(@Nullable Player player, TooltipFlag flag,
                                           CallbackInfoReturnable<List<Component>> cir) {
        TooltipPipeline.apply((ItemStack) (Object) this, player, flag, cir.getReturnValue());
    }
}
