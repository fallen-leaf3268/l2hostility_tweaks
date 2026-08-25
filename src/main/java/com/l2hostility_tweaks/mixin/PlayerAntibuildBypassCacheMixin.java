package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.L2HFBypassTags;
import com.l2hostility_tweaks.util.AntibuildBypassCache;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import top.theillusivec4.curios.api.CuriosApi;

@Mixin(Player.class)
public class PlayerAntibuildBypassCacheMixin implements AntibuildBypassCache {

	@Unique
	private static final ResourceLocation l2fix$arenaId =
			new ResourceLocation("l2hostility", "arena");
	@Unique
	private long l2fix$antibuildBypassTime = Long.MIN_VALUE;
	@Unique
	private boolean l2fix$cachedAntibuildBypass;

	@Override
	public boolean l2fix$hasAntibuildBypass() {
		Player player = (Player) (Object) this;
		return l2fix$hasAntibuildBypassAtTime(player.level().getGameTime());
	}

	@Unique
	boolean l2fix$hasAntibuildBypassAtTime(long gameTime) {
		if (l2fix$antibuildBypassTime != gameTime) {
			l2fix$cachedAntibuildBypass = l2fix$scanAntibuildBypass();
			l2fix$antibuildBypassTime = gameTime;
		}
		return l2fix$cachedAntibuildBypass;
	}

	@Unique
	boolean l2fix$scanAntibuildBypass() {
		Player player = (Player) (Object) this;
		return l2fix$hasArenaTrait(player) || l2fix$hasBypassItem(player);
	}

	@Unique
	private static boolean l2fix$hasArenaTrait(Player player) {
		if (!MobTraitCap.HOLDER.isProper(player)) return false;
		var cap = MobTraitCap.HOLDER.get(player);
		for (var entry : cap.traits.entrySet()) {
			if (entry.getValue() > 0 && l2fix$arenaId.equals(entry.getKey().getRegistryName())) {
				return true;
			}
		}
		return false;
	}

	@Unique
	private static boolean l2fix$hasBypassItem(Player player) {
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (player.getItemBySlot(slot).is(L2HFBypassTags.ANTIBUILD_BYPASS)) {
				return true;
			}
		}
		try {
			return CuriosApi.getCuriosInventory(player).resolve().map(handler -> {
				for (var stacksHandler : handler.getCurios().values()) {
					var stacks = stacksHandler.getStacks();
					for (int i = 0; i < stacks.getSlots(); i++) {
						if (stacks.getStackInSlot(i).is(L2HFBypassTags.ANTIBUILD_BYPASS)) {
							return true;
						}
					}
				}
				return false;
			}).orElse(false);
		} catch (Exception ignored) {
			return false;
		}
	}
}
