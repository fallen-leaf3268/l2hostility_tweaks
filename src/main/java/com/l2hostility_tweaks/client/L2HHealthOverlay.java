package com.l2hostility_tweaks.client;

import com.l2hostility_tweaks.client.config.ClientL2HConfig;
import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.util.RomanNumeral;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class L2HHealthOverlay implements IGuiOverlay {

	private static final Logger LOGGER = LoggerFactory.getLogger("l2htweaks:hud_overlay");

	public static boolean hideBossBars;
	public static boolean hudActive;
	public static boolean bossEventsActive;
	public static int trackedEntityId = -1;

	record HudState(boolean hudActive, boolean hideBossBars) {}

	static HudState l2fix$resolveHudState(boolean hasValidTarget,
			boolean bossEventsActive, boolean hideHudWithBossbar) {
		boolean showCustomHud = hasValidTarget && !(hideHudWithBossbar && bossEventsActive);
		return new HudState(showCustomHud, showCustomHud);
	}

	private static final int BAR_H = 3;
	private static final int SCALE = 24;
	private static final long FLASH_MS = 400;
	private static final float BASE_DRAIN = 0.3f;
	private static final float DRAIN_SCALE = 2.5f;
	private static final int SHADOW_DY = 2;
	private static final int ICON_SZ = 9;
	private static final int ICON_GAP = 1;
	private static final int INTERNAL_FULL_H = BAR_H * SCALE;
	private static final double DISPLAY_RADIUS = BAR_H / 2.0;

	private int trackedId = -1;
	private float displayFrac = 1f;
	private float actualFrac = 1f;
	private float accumDamage;
	private long lastDamageTime;

	private int cachedBarW = -1;
	private int[] segH;
	private int[] topOff;

	private int cachedColor = -1;
	private float cachedGrad = -1;
	private int[] gradColors;

	private int cachedEntityId = -1;
	private int cachedTraitHash;
	private int cachedRealityLv;
	private ItemStack cachedRealityIcon;
	private List<ItemStack> cachedLegendIcons;
	private List<List<TraitTextLayout.Segment<MobTrait, FormattedCharSequence>>> cachedTraitLines;

	private final Map<String, ItemStack> iconReuse = new HashMap<>();

	@Override
	public void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
		if (!L2HConfig.COMMON.showHud.get()) {
			hideBossBars = false;
			hudActive = false;
			trackedEntityId = -1;
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		Optional<LivingEntity> target = getMouseOverEntity(mc, partialTick);
		boolean hasValidTarget = target.isPresent() && !(target.get() instanceof Player);
		HudState state = l2fix$resolveHudState(hasValidTarget, bossEventsActive,
				ClientL2HConfig.CLIENT.hideHudWithBossbar.get());
		hudActive = state.hudActive();
		hideBossBars = state.hideBossBars();
		trackedEntityId = state.hudActive() ? target.get().getId() : -1;
		if (state.hudActive()) renderHealthBar(g, target.get());
	}

	public static void precomputeHudState() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) {
			hudActive = false;
			hideBossBars = false;
			trackedEntityId = -1;
			LOGGER.debug("PRECOMPUTE skip: mc.level or mc.player null");
			return;
		}
		if (!L2HConfig.COMMON.showHud.get()) {
			hudActive = false;
			hideBossBars = false;
			trackedEntityId = -1;
			LOGGER.debug("PRECOMPUTE: showHud=false → hudActive=false");
			return;
		}
		var target = getMouseOverEntity(mc, 0f);
		boolean hasValidTarget = target.isPresent() && !(target.get() instanceof Player);
		HudState state = l2fix$resolveHudState(hasValidTarget, bossEventsActive,
				ClientL2HConfig.CLIENT.hideHudWithBossbar.get());
		hudActive = state.hudActive();
		hideBossBars = state.hideBossBars();
		trackedEntityId = state.hudActive() ? target.get().getId() : -1;
		if (!state.hudActive()) {
			LOGGER.debug("PRECOMPUTE: hudActive=false");
			return;
		}
		LOGGER.debug("PRECOMPUTE: target={} → hudActive=true trackedId={}", target.get().getName().getString(), trackedEntityId);
	}

	private static Optional<LivingEntity> getMouseOverEntity(Minecraft mc, float partialTicks) {
		Entity camera = mc.getCameraEntity();
		if (camera == null || mc.level == null) return Optional.empty();

		double range = ClientL2HConfig.CLIENT.hudRange.get();
		Vec3 eyePos = camera.getEyePosition(partialTicks);
		Vec3 viewVec = camera.getViewVector(1.0F);
		Vec3 reachVec = eyePos.add(viewVec.x * range, viewVec.y * range, viewVec.z * range);
		AABB aabb = camera.getBoundingBox().expandTowards(viewVec.scale(range)).inflate(1.0D);

		EntityHitResult hit = ProjectileUtil.getEntityHitResult(camera, eyePos, reachVec, aabb,
				e -> !e.isSpectator() && e.isPickable(), eyePos.distanceToSqr(reachVec));
		if (hit != null && hit.getEntity() instanceof LivingEntity living) {
			if (!MobTraitCap.HOLDER.isProper(living)) return Optional.empty();
			MobTraitCap cap = MobTraitCap.HOLDER.get(living);
			if (cap.isInitialized() && !cap.traits.isEmpty())
				return Optional.of(living);
		}
		return Optional.empty();
	}

	private void renderHealthBar(GuiGraphics g, LivingEntity entity) {
		MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
		if (cap == null) return;

		int barW = ClientL2HConfig.CLIENT.hudBarWidth.get();
		if (barW != cachedBarW) {
			recomputeGeometry(barW);
			cachedColor = -1;
		}

		int color = getDifficultyColor(cap.lv);
		float grad = ClientL2HConfig.CLIENT.gradientStrength.get().floatValue();
		if (color != cachedColor || grad != cachedGrad) {
			recomputeGradient(color, grad, barW);
		}

		int traitHash = traitFingerprint(cap);
		if (entity.getId() != cachedEntityId || traitHash != cachedTraitHash) {
			scanTraits(cap, entity, traitHash);
		}

		Minecraft mc = Minecraft.getInstance();
		int screenW = mc.getWindow().getGuiScaledWidth();
		int xOff = ClientL2HConfig.CLIENT.hudXOffset.get();
		int yOff = ClientL2HConfig.CLIENT.hudYOffset.get();

		float barX = xOff + screenW / 2f - barW / 2f;
		float barY = yOff + 27;
		int bx = (int)(barX * SCALE);
		int by = (int)(barY * SCALE - INTERNAL_FULL_H / 2);

		float curFrac = entity.getHealth() / entity.getMaxHealth();
		if (entity.getId() != trackedId) {
			trackedId = entity.getId();
			displayFrac = curFrac;
			actualFrac = curFrac;
			accumDamage = 0;
		} else {
			if (curFrac < actualFrac - 0.001f) {
				accumDamage += actualFrac - curFrac;
				lastDamageTime = System.currentTimeMillis();
			}
			actualFrac = curFrac;
			long elapsed = System.currentTimeMillis() - lastDamageTime;
			if (elapsed > FLASH_MS) {
				float speed = BASE_DRAIN + accumDamage * DRAIN_SCALE;
				accumDamage = Math.max(0, accumDamage - speed * (elapsed - FLASH_MS) / 1000f);
			}
			displayFrac = Math.min(1f, actualFrac + accumDamage);
		}

		int actualPx = (int)(actualFrac * barW);
		int dispPx = (int)(displayFrac * barW);

		var pose = g.pose();
		pose.pushPose();
		pose.scale(1f / SCALE, 1f / SCALE, 1f);

		if (dispPx > 0) {
			fillRun(g, 0, dispPx, bx, by, 0x40000000, SHADOW_DY * SCALE, 0);
		}

		fillRun(g, 0, barW, bx, by, 0xFF000000, -SCALE, 0);
		fillRun(g, 0, barW, bx, by, 0xFF000000, 0, SCALE);

		fillColorRun(g, 0, Math.min(actualPx, barW), bx, by, 0);
		fillColorRun(g, actualPx, Math.min(dispPx, barW), bx, by, 0xFFFFFFFF);
		fillColorRun(g, Math.max(dispPx, 0), barW, bx, by, 0x80000000);

		int leftTop = by + topOff[0] - SCALE;
		int leftBot = by + topOff[0] + segH[0] + SCALE;
		g.fill(bx - SCALE, leftTop, bx, leftBot, 0xFF000000);
		int rightTop = by + topOff[barW - 1] - SCALE;
		int rightBot = by + topOff[barW - 1] + segH[barW - 1] + SCALE;
		g.fill(bx + barW * SCALE, rightTop, bx + (barW + 1) * SCALE, rightBot, 0xFF000000);

		pose.popPose();

		renderHeader(g, entity, cap, barX, barY, barW, screenW, xOff, color);
		renderTraitLines(g, barX, barY, barW, screenW, xOff);
	}

	private void fillRun(GuiGraphics g, int from, int to, int bx, int by, int color, int yOffDelta, int hOffDelta) {
		int runStart = from;
		while (runStart < to) {
			int runEnd = runStart + 1;
			while (runEnd < to && topOff[runEnd] == topOff[runStart] && segH[runEnd] == segH[runStart]) {
				runEnd++;
			}
			int sx = bx + runStart * SCALE;
			int sy = by + topOff[runStart] + yOffDelta;
			int runW = (runEnd - runStart) * SCALE;
			int runH = segH[runStart] + hOffDelta;
			g.fill(sx, sy, sx + runW, sy + runH, color);
			runStart = runEnd;
		}
	}

	private void fillColorRun(GuiGraphics g, int from, int to, int bx, int by, int solidColor) {
		if (from >= to) return;
		int runStart = from;
		while (runStart < to) {
			int runEnd = runStart + 1;
			int firstColor = solidColor != 0 ? solidColor : gradColors[runStart];
			if (firstColor == 0) {
				runStart = runEnd;
				continue;
			}
			while (runEnd < to && topOff[runEnd] == topOff[runStart] && segH[runEnd] == segH[runStart]) {
				int curColor = solidColor != 0 ? solidColor : gradColors[runEnd];
				if (curColor != firstColor) break;
				runEnd++;
			}
			int sx = bx + runStart * SCALE;
			int sy = by + topOff[runStart];
			int runW = (runEnd - runStart) * SCALE;
			int runH = segH[runStart];
			g.fill(sx, sy, sx + runW, sy + runH, firstColor);
			runStart = runEnd;
		}
	}

	private void renderHeader(GuiGraphics g, LivingEntity entity, MobTraitCap cap,
							  float barX, float barY, int barW, int screenW, int xOff, int color) {
		Minecraft mc = Minecraft.getInstance();
		float padding = BAR_H / 2f + 1;
		float gap = 1f;
		float iconScale = ICON_SZ / 16f;
		int iconStep = ICON_SZ + ICON_GAP;
		float nameGap = 4;
		float barRight = barX + barW;

		float nameY = barY - padding - gap - 9;
		String name = entity.getDisplayName().getString();
		int nameWidth = mc.font.width(name);
		float nameLeft = xOff + (screenW / 2f - nameWidth / 2f);

		float lx = barX;
		if (cachedRealityIcon != null) {
			renderIcon(cachedRealityIcon, lx, nameY, iconScale, g);
			String rText = ClientL2HConfig.CLIENT.romanNumerals.get()
					? RomanNumeral.toRoman(cachedRealityLv) : String.valueOf(cachedRealityLv);
			float textX = lx + ICON_SZ + 1;
			g.drawString(mc.font, rText, textX, nameY, 0xFFFF5555, true);
			lx += ICON_SZ + 1 + mc.font.width(rText) + nameGap;
		}

		int icons = cachedLegendIcons.size();
		int li = 0, ri = icons - 1;
		for (; li <= ri; li++) {
			if (lx + ICON_SZ > nameLeft - nameGap || lx + ICON_SZ > barRight) break;
			renderIcon(cachedLegendIcons.get(li), lx, nameY, iconScale, g);
			lx += iconStep;
		}
		float rx = barRight - ICON_SZ;
		for (; ri >= li; ri--) {
			if (rx < nameLeft + nameWidth + nameGap || rx < barX) break;
			renderIcon(cachedLegendIcons.get(ri), rx, nameY, iconScale, g);
			rx -= iconStep;
		}
		g.drawString(mc.font, name, nameLeft, nameY, color, true);

		String diffText = "Lv." + cap.lv;
		float diffY = nameY - gap - 9;
		int diffWidth = mc.font.width(diffText);
		float diffLeft = xOff + (screenW / 2f - diffWidth / 2f);

		float dlx = barX;
		int dLi = li, dRi = ri;
		for (; dLi <= dRi; dLi++) {
			if (dlx + ICON_SZ > diffLeft - nameGap || dlx + ICON_SZ > barRight) break;
			renderIcon(cachedLegendIcons.get(dLi), dlx, diffY, iconScale, g);
			dlx += iconStep;
		}
		float drx = barRight - ICON_SZ;
		for (; dRi >= dLi; dRi--) {
			if (drx < diffLeft + diffWidth + nameGap || drx < barX) break;
			renderIcon(cachedLegendIcons.get(dRi), drx, diffY, iconScale, g);
			drx -= iconStep;
		}
		g.drawString(mc.font, diffText, diffLeft, diffY, color, true);
	}

	private void renderTraitLines(GuiGraphics g, float barX, float barY,
								  int barW, int screenW, int xOff) {
		if (cachedTraitLines == null || cachedTraitLines.isEmpty()) return;
		Minecraft mc = Minecraft.getInstance();
		float padding = BAR_H / 2f + 1;
		float lineY = barY + padding + 2;
		for (List<TraitTextLayout.Segment<MobTrait, FormattedCharSequence>> line : cachedTraitLines) {
			for (TraitTextLayout.Segment<MobTrait, FormattedCharSequence> segment : line) {
				g.drawString(mc.font, segment.text(), (int) barX + segment.xOffset(),
						(int) lineY, 0xFFFFFF, true);
			}
			lineY += 10;
		}
	}

	private static int traitFingerprint(MobTraitCap cap) {
		int h = 0;
		for (var entry : cap.traits.entrySet())
			h = h * 31 + entry.getKey().getID().hashCode() ^ entry.getValue();
		return h;
	}

	private void scanTraits(MobTraitCap cap, LivingEntity entity, int traitHash) {
		int entityId = entity.getId();
		this.cachedEntityId = entityId;
		this.cachedTraitHash = traitHash;
		this.cachedRealityLv = 0;
		this.cachedRealityIcon = null;
		this.cachedLegendIcons = new ArrayList<>();

		Set<String> extraIds = L2HConfig.getDisplayExtraLegendaryIds();
		for (var entry : cap.traits.entrySet()) {
			String id = entry.getKey().getID();
			if ("curseofpandora:reality".equals(id)) {
				cachedRealityLv = Math.abs(entry.getValue());
				cachedRealityIcon = iconReuse.computeIfAbsent(id,
						k -> new ItemStack(entry.getKey().asItem()));
			}
			if (entry.getKey() instanceof LegendaryTrait || extraIds.contains(id)) {
				cachedLegendIcons.add(iconReuse.computeIfAbsent(id,
						k -> new ItemStack(entry.getKey().asItem())));
			}
		}

		int barW = ClientL2HConfig.CLIENT.hudBarWidth.get();
		int maxW = barW - 6;
		Minecraft mc = Minecraft.getInstance();
		List<TraitTextLayout.Entry<MobTrait, FormattedText>> entries = new ArrayList<>();

		for (var entry : cap.traits.entrySet()) {
			MutableComponent tc = entry.getValue() < 0 ?
					entry.getKey().getFullDesc(-entry.getValue()).copy().withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.STRIKETHROUGH) :
					entry.getKey().getFullDesc(entry.getValue());
			entries.add(new TraitTextLayout.Entry<>(entry.getKey(), tc));
		}
		this.cachedTraitLines = TraitTextLayout.layout(entries, maxW,
				Component.literal("  ").getVisualOrderText(), mc.font::width, mc.font::split);
	}

	private void recomputeGeometry(int barW) {
		segH = new int[barW];
		topOff = new int[barW];
		for (int dpx = 0; dpx < barW; dpx++) {
			int dist = Math.min(dpx, barW - 1 - dpx);
			int sh;
			if (dist >= DISPLAY_RADIUS) {
				sh = INTERNAL_FULL_H;
			} else {
				double dy = Math.sqrt(DISPLAY_RADIUS * DISPLAY_RADIUS
						- (DISPLAY_RADIUS - dist) * (DISPLAY_RADIUS - dist));
				sh = Math.max(SCALE, (int)(2.0 * dy * SCALE));
			}
			segH[dpx] = sh;
			topOff[dpx] = (INTERNAL_FULL_H - sh) / 2;
		}
		this.cachedBarW = barW;
	}

	private void recomputeGradient(int color, float grad, int barW) {
		int r = FastColor.ARGB32.red(color);
		int g = FastColor.ARGB32.green(color);
		int b = FastColor.ARGB32.blue(color);
		if (gradColors == null || gradColors.length != barW)
			gradColors = new int[barW];
		float half = (barW - 1) / 2f;
		float threshold = 0.25f;
		for (int dpx = 0; dpx < barW; dpx++) {
			float t = half == 0 ? 0 : Math.abs(dpx - half) / half;
			float fade;
			if (t <= threshold || threshold >= 1f) {
				fade = 1f;
			} else {
				fade = 1f - grad * (t - threshold) / (1f - threshold);
			}
			gradColors[dpx] = pack(255,
					(int)(r * fade),
					(int)(g * fade),
					(int)(b * fade));
		}
		this.cachedColor = color;
		this.cachedGrad = grad;
	}

	private void renderIcon(ItemStack icon, float screenX, float screenY, float iconScale, GuiGraphics g) {
		int ix = (int)(screenX / iconScale);
		int iy = (int)(screenY / iconScale);
		var p = g.pose();
		p.pushPose();
		p.scale(iconScale, iconScale, 1f);
		g.renderItem(icon, ix, iy);
		p.popPose();
	}

	private static int pack(int a, int r, int g, int b) {
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private int getDifficultyColor(int diff) {
		List<int[]> segs = ClientL2HConfig.getColorSegments();
		for (int i = segs.size() - 1; i >= 0; i--) {
			int[] seg = segs.get(i);
			if (diff >= seg[0])
				return FastColor.ARGB32.color(255, seg[1], seg[2], seg[3]);
		}
		return ClientL2HConfig.getDefaultColor();
	}
}
