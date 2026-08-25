package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.DetectorGlassesCache;
import dev.xkmc.l2hostility.compat.curios.CurioCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Player.class)
public class PlayerDetectorGlassesCacheMixin implements DetectorGlassesCache {

	@Unique
	private static final ResourceLocation l2fix$detectorGlassesId =
			new ResourceLocation("l2hostility", "detector_glasses");
	@Unique
	private static Item l2fix$cachedDetectorGlasses;
	@Unique
	private int l2fix$detectorGlassesTick = Integer.MIN_VALUE;
	@Unique
	private boolean l2fix$cachedHasDetectorGlasses;

	@Override
	public boolean l2fix$hasDetectorGlasses() {
		Player player = (Player) (Object) this;
		if (!l2fix$isDetectorGlassesCacheValid(player.tickCount)) {
			Item glasses = l2fix$getDetectorGlasses();
			boolean has = glasses != null && CurioCompat.hasItemInCurioOrSlot(player, glasses);
			l2fix$storeDetectorGlasses(player.tickCount, has);
		}
		return l2fix$getCachedDetectorGlasses();
	}

	@Unique
	boolean l2fix$isDetectorGlassesCacheValid(int tick) {
		return l2fix$detectorGlassesTick == tick;
	}

	@Unique
	void l2fix$storeDetectorGlasses(int tick, boolean has) {
		l2fix$cachedHasDetectorGlasses = has;
		l2fix$detectorGlassesTick = tick;
	}

	@Unique
	boolean l2fix$getCachedDetectorGlasses() {
		return l2fix$cachedHasDetectorGlasses;
	}

	@Unique
	private static Item l2fix$getDetectorGlasses() {
		if (l2fix$cachedDetectorGlasses == null) {
			l2fix$cachedDetectorGlasses = ForgeRegistries.ITEMS.getValue(l2fix$detectorGlassesId);
		}
		return l2fix$cachedDetectorGlasses;
	}
}
