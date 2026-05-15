package com.l2hostilityfix.mixin;

import com.l2hostilityfix.config.L2HConfig;
import dev.xkmc.l2hostility.content.item.traits.TraitSymbol;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import dev.xkmc.l2hostility.init.data.LangData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;

@Mixin(value = TraitSymbol.class, remap = false)
public class TraitSymbolMixin {

    @Inject(method = "m_7373_", at = @At("TAIL"))
    private void l2fix$addLegendaryTooltip(ItemStack stack, Level level, List<Component> tooltip,
                                            TooltipFlag flag, CallbackInfo ci) {
        MobTrait trait = ((TraitSymbol) (Object) this).get();
        if (trait instanceof LegendaryTrait || trait.isBanned()) return;
        Set<String> extraIds = L2HConfig.getExtraLegendaryIds();
        if (!extraIds.isEmpty() && extraIds.contains(trait.getID())) {
            tooltip.add(2, LangData.TOOLTIP_LEGENDARY.get().withStyle(ChatFormatting.GOLD));
        }
    }
}
