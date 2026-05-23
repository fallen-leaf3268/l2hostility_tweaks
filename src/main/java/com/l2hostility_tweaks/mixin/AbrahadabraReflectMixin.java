package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.compat.curios.CurioCompat;
import dev.xkmc.l2hostility.content.logic.TraitEffectCache;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

@Mixin(value = TraitEffectCache.class, remap = false)
public class AbrahadabraReflectMixin {

	private static final ResourceLocation ABRAHADABRA_ID = new ResourceLocation("l2hostility", "abrahadabra");

	@Shadow
	@Final
	public LivingEntity target;

	@Inject(method = "reflectTrait", at = @At("RETURN"), cancellable = true, remap = false)
	public void l2fix$minionReflect(MobTrait trait, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue()) return;
		if (!(target instanceof Mob mob)) return;
		LivingEntity owner = getOwner(mob);
		if (owner == null) return;
		Item abrahadabra = ForgeRegistries.ITEMS.getValue(ABRAHADABRA_ID);
		if (abrahadabra != null && CurioCompat.hasItemInCurioChecked(owner, abrahadabra)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "getTargets", at = @At("RETURN"), remap = false)
	public void l2fix$filterReflectedTargets(CallbackInfoReturnable<List<Mob>> cir) {
		List<Mob> targets = cir.getReturnValue();
		Item item = ForgeRegistries.ITEMS.getValue(ABRAHADABRA_ID);
		if (item == null) return;
		targets.removeIf(mob -> isImmune(mob, item));
	}

	private static boolean isImmune(Mob mob, Item item) {
		if (CurioCompat.hasItemInCurioChecked(mob, item)) return true;
		LivingEntity owner = getOwner(mob);
		return owner != null && CurioCompat.hasItemInCurioChecked(owner, item);
	}

	private static LivingEntity getOwner(Mob mob) {
		if (mob instanceof TamableAnimal tamable) {
			LivingEntity owner = tamable.getOwner();
			if (owner != null) return owner;
		}
		UUID uuid = null;
		if (mob instanceof OwnableEntity ownable) {
			uuid = ownable.getOwnerUUID();
		}
		if (uuid == null) {
			uuid = getOwnerFromNbt(mob);
		}
		if (uuid != null && mob.level() instanceof ServerLevel sl) {
			Entity e = sl.getEntity(uuid);
			if (e instanceof LivingEntity le) return le;
		}
		return null;
	}

	private static UUID getOwnerFromNbt(Mob mob) {
		try {
			CompoundTag nbt = mob.getPersistentData();
			if (nbt.contains("Owner")) {
				return nbt.getUUID("Owner");
			}
			if (nbt.contains("OwnerUUID")) {
				return nbt.getUUID("OwnerUUID");
			}
			if (nbt.contains("owner")) {
				return nbt.getUUID("owner");
			}
		} catch (Exception ignored) {
		}
		return null;
	}
}
