package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.EntityImmunityCache;
import com.l2hostility_tweaks.util.ImmunityHelper;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public class LivingEntityImmunityCacheMixin implements EntityImmunityCache {

	@Unique
	private long l2fix$forceImmunityStamp = Long.MIN_VALUE;
	@Unique
	private boolean l2fix$forceImmunity;
	@Unique
	private long l2fix$gravityImmunityStamp = Long.MIN_VALUE;
	@Unique
	private boolean l2fix$gravityImmunity;

	@Override
	public boolean l2fix$isImmuneToForce(long stamp) {
		if (l2fix$forceImmunityStamp != stamp) {
			l2fix$forceImmunity = l2fix$scanForceImmunity();
			l2fix$forceImmunityStamp = stamp;
		}
		return l2fix$forceImmunity;
	}

	@Override
	public boolean l2fix$isImmuneToGravity(long stamp) {
		if (l2fix$gravityImmunityStamp != stamp) {
			l2fix$gravityImmunity = l2fix$scanGravityImmunity();
			l2fix$gravityImmunityStamp = stamp;
		}
		return l2fix$gravityImmunity;
	}

	@Unique
	boolean l2fix$scanForceImmunity() {
		return ImmunityHelper.computeImmuneToForce((LivingEntity) (Object) this);
	}

	@Unique
	boolean l2fix$scanGravityImmunity() {
		return ImmunityHelper.computeImmuneToGravity((LivingEntity) (Object) this);
	}
}
