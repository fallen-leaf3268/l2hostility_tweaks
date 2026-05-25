package com.l2hostility_tweaks.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;

import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "l2hostility_tweaks", bus = Mod.EventBusSubscriber.Bus.MOD)
public class CoPCompat {

	@SubscribeEvent
	public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
		if (!ModList.get().isLoaded("curseofpandora")) return;
		Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(
				new ResourceLocation("curseofpandora", "reality_index"));
		if (attr == null) return;

		for (var entry : ForgeRegistries.ENTITY_TYPES.getEntries()) {
			EntityType<?> type = entry.getValue();
			if (type.getBaseClass() != null
					&& LivingEntity.class.isAssignableFrom(type.getBaseClass())) {
				@SuppressWarnings("unchecked")
				EntityType<? extends LivingEntity> livingType =
						(EntityType<? extends LivingEntity>) type;
				event.add(livingType, attr);
			}
		}
	}
}
