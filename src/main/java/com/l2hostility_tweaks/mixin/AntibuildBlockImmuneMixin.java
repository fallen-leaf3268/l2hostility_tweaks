package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.L2HFBypassTags;
import com.l2hostility_tweaks.util.AntibuildBypassCache;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class AntibuildBlockImmuneMixin extends LivingEntity {

	private static final ResourceLocation ANTIBUILD_ID = new ResourceLocation("l2hostility", "antibuild");

	protected AntibuildBlockImmuneMixin(EntityType<? extends LivingEntity> type, Level level) {
		super(type, level);
	}

	@Inject(method = "blockActionRestricted", at = @At("HEAD"), cancellable = true)
	public void l2fix$antibuildBlockImmune(Level level, BlockPos pos, GameType type, CallbackInfoReturnable<Boolean> cir) {
		if (type != GameType.SURVIVAL) return;
		MobEffect antibuild = ForgeRegistries.MOB_EFFECTS.getValue(ANTIBUILD_ID);
		if (antibuild == null || !hasEffect(antibuild)) return;

		if (((AntibuildBypassCache) (Object) this).l2fix$hasAntibuildBypass()) {
			cir.setReturnValue(false);
			return;
		}

		if (level.getBlockState(pos).is(L2HFBypassTags.ANTIBUILD_IMMUNE)) {
			cir.setReturnValue(false);
		}
	}
}
