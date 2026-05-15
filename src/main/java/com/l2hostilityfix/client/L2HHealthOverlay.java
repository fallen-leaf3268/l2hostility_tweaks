package com.l2hostilityfix.client;

import com.l2hostilityfix.client.config.ClientL2HConfig;
import com.l2hostilityfix.config.L2HConfig;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class L2HHealthOverlay implements IGuiOverlay {

    public static boolean hideBossBars;

    private static final int BAR_WIDTH = 256;
    private static final int BAR_HEIGHT = 3;
    private static final int MAX_TEXT_WIDTH = 250;
    private static final long FLASH_DELAY_MS = 400;
    private static final float BASE_DRAIN = 0.3f;
    private static final float DRAIN_SCALE = 2.5f;

    private int trackedId = -1;
    private float displayFrac = 1f;
    private float actualFrac = 1f;
    private float accumDamage;
    private long lastDamageTime;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int width, int height) {
        hideBossBars = false;
        if (!L2HConfig.COMMON.showHud.get()) return;

        Minecraft mc = Minecraft.getInstance();
        Optional<LivingEntity> target = getMouseOverEntity(mc, partialTick);

        if (target.isPresent()) {
            hideBossBars = true;
            renderHealthBar(guiGraphics, target.get());
        }
    }

    private Optional<LivingEntity> getMouseOverEntity(Minecraft mc, float partialTicks) {
        Entity camera = mc.getCameraEntity();
        if (camera == null || mc.level == null) return Optional.empty();

        double range = ClientL2HConfig.CLIENT.hudRange.get();
        Vec3 eyePos = camera.getEyePosition(partialTicks);
        Vec3 viewVec = camera.getViewVector(1.0F);
        Vec3 reachVec = eyePos.add(viewVec.x * range, viewVec.y * range, viewVec.z * range);
        AABB aabb = camera.getBoundingBox().expandTowards(viewVec.scale(range)).inflate(1.0D, 1.0D, 1.0D);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(camera, eyePos, reachVec, aabb,
                e -> !e.isSpectator() && e.isPickable(), eyePos.distanceToSqr(reachVec));

        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            MobTraitCap cap = MobTraitCap.HOLDER.get(living);
            if (cap != null && cap.isInitialized() && !cap.traits.isEmpty()) {
                return Optional.of(living);
            }
        }
        return Optional.empty();
    }

    private void renderHealthBar(GuiGraphics guiGraphics, LivingEntity entity) {
        MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
        if (cap == null) return;

        Minecraft mc = Minecraft.getInstance();
        int i = mc.getWindow().getGuiScaledWidth();
        int xOffset = ClientL2HConfig.CLIENT.hudXOffset.get();
        int yOffset = ClientL2HConfig.CLIENT.hudYOffset.get();

        int j = 27;
        int barX = xOffset + i / 2 - BAR_WIDTH / 2;
        int barY = yOffset + j;

        int color = getDifficultyColor(cap.lv);
        int r = FastColor.ARGB32.red(color);
        int g = FastColor.ARGB32.green(color);
        int b = FastColor.ARGB32.blue(color);

        int displayH = BAR_HEIGHT;
        int scale = 24;
        int innerW = BAR_WIDTH * scale;
        int internalFullH = displayH * scale;
        int internalBorder = scale;
        double displayRadius = displayH / 2.0;

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
            if (elapsed > FLASH_DELAY_MS) {
                float speed = BASE_DRAIN + accumDamage * DRAIN_SCALE;
                float drained = speed * (elapsed - FLASH_DELAY_MS) / 1000f;
                accumDamage = Math.max(0, accumDamage - drained);
            }
            displayFrac = Math.min(1f, actualFrac + accumDamage);
        }

        int actualW = (int) (actualFrac * BAR_WIDTH);
        int dispW = (int) (displayFrac * BAR_WIDTH);
        float rStep = r * 0.6f / (BAR_WIDTH - 1);
        float gStep = g * 0.6f / (BAR_WIDTH - 1);
        float bStep = b * 0.6f / (BAR_WIDTH - 1);

        int bx = barX * scale;
        int by = barY * scale - internalFullH / 2;

        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.scale(1.0f / scale, 1.0f / scale, 1.0f);

        for (int dpx = 0; dpx < BAR_WIDTH; dpx++) {
            int sx = bx + dpx * scale;
            int sr = (int) (r - rStep * dpx);
            int sg = (int) (g - gStep * dpx);
            int sb = (int) (b - bStep * dpx);

            int dist = Math.min(dpx, BAR_WIDTH - 1 - dpx);
            int internalSegH;
            if (dist >= displayRadius) {
                internalSegH = internalFullH;
            } else {
                double dy = Math.sqrt(displayRadius * displayRadius - (displayRadius - dist) * (displayRadius - dist));
                internalSegH = Math.max(scale, (int)(2.0 * dy * scale));
            }
            int topOff = (internalFullH - internalSegH) / 2;

            int top = by + topOff;
            int bot = by + topOff + internalSegH;

            guiGraphics.fill(sx, top - internalBorder, sx + scale, top, 0xFF000000);
            guiGraphics.fill(sx, bot, sx + scale, bot + internalBorder, 0xFF000000);

            int col = pack(255, sr, sg, sb);
            if (dpx < actualW) {
                guiGraphics.fill(sx, top, sx + scale, bot, col);
            } else if (dpx < dispW) {
                guiGraphics.fill(sx, top, sx + scale, bot, 0xFFFFFFFF);
            } else {
                guiGraphics.fill(sx, top, sx + scale, bot, 0x80000000);
            }
        }

        pose.popPose();

        float padding = displayH / 2.0f + 1;
        float gap = 1.0f;

        // Collect legendary icons
        List<ItemStack> legendIcons = new ArrayList<>();
        Set<String> extraIds = L2HConfig.getExtraLegendaryIds();
        for (var entry : cap.traits.entrySet()) {
            if (entry.getKey() instanceof LegendaryTrait
                    || extraIds.contains(entry.getKey().getID())) {
                legendIcons.add(new ItemStack(entry.getKey().asItem()));
            }
        }

        int iconSize = 9;
        int iconGap = 1;
        int iconStep = iconSize + iconGap;
        float iconScale = iconSize / 16f;
        float barRight = barX + BAR_WIDTH;

        // Entity name (centered)
        float nameY = barY - padding - gap - 9;
        String name = entity.getDisplayName().getString();
        int nameWidth = mc.font.width(name);
        float nameLeft = xOffset + (i / 2f - nameWidth / 2f);
        float nameRight = nameLeft + nameWidth;

        int nameLi = 0;
        int nameRi = legendIcons.size() - 1;
        float nameGap = 4;
        float lx = barX;
        boolean nameLeftBlocked = false;
        for (; nameLi <= nameRi; nameLi++) {
            if (lx + iconSize > nameLeft - nameGap || lx + iconSize > barRight) break;
            renderIcon(legendIcons.get(nameLi), lx, nameY, iconScale, guiGraphics);
            lx += iconStep;
        }
        float rx = barRight - iconSize;
        boolean nameRightBlocked = false;
        for (; nameRi >= nameLi; nameRi--) {
            if (rx < nameRight + nameGap || rx < barX) break;
            renderIcon(legendIcons.get(nameRi), rx, nameY, iconScale, guiGraphics);
            rx -= iconStep;
        }

        guiGraphics.drawString(mc.font, name, nameLeft, nameY, color, true);

        // Difficulty level + overflow icons
        String diffText = "Lv." + cap.lv;
        float diffY = barY - padding - gap - 9 - gap - 9;
        int diffWidth = mc.font.width(diffText);
        float diffLeft = xOffset + (i / 2f - diffWidth / 2f);
        float diffRight = diffLeft + diffWidth;

        int diffLi = nameLi;
        int diffRi = nameRi;
        float dlx = barX;
        for (; diffLi <= diffRi; diffLi++) {
            if (dlx + iconSize > diffLeft - nameGap || dlx + iconSize > barRight) break;
            renderIcon(legendIcons.get(diffLi), dlx, diffY, iconScale, guiGraphics);
            dlx += iconStep;
        }
        float drx = barRight - iconSize;
        for (; diffRi >= diffLi; diffRi--) {
            if (drx < diffRight + nameGap || drx < barX) break;
            renderIcon(legendIcons.get(diffRi), drx, diffY, iconScale, guiGraphics);
            drx -= iconStep;
        }

        guiGraphics.drawString(mc.font, diffText, diffLeft, diffY, color, true);

        // Trait list — below the name, left-to-right with word-wrap
        List<List<MutableComponent>> lines = new ArrayList<>();
        List<MutableComponent> currentLine = new ArrayList<>();
        int currentWidth = 0;
        MutableComponent sep = Component.literal("  ");
        int sepWidth = mc.font.width(sep);

        for (var entry : cap.traits.entrySet()) {
            MutableComponent traitComp = entry.getKey().getFullDesc(entry.getValue());
            int traitWidth = mc.font.width(traitComp);

            int needed = currentWidth > 0 ? sepWidth + traitWidth : traitWidth;
            if (currentWidth + needed > MAX_TEXT_WIDTH) {
                lines.add(currentLine);
                currentLine = new ArrayList<>();
                currentWidth = 0;
            }
            if (currentWidth > 0) {
                currentLine.add(sep);
                currentWidth += sepWidth;
            }
            currentLine.add(traitComp);
            currentWidth += traitWidth;
        }
        if (!currentLine.isEmpty()) {
            lines.add(currentLine);
        }

        float lineY = barY + padding + 2;
        for (List<MutableComponent> line : lines) {
            int x = barX;
            for (MutableComponent c : line) {
                guiGraphics.drawString(mc.font, c, x, (int) lineY, 0xFFFFFF, true);
                x += mc.font.width(c);
            }
            lineY += 10;
        }
    }

    private void renderIcon(ItemStack icon, float screenX, float screenY, float iconScale, GuiGraphics g) {
        int ix = (int) (screenX / iconScale);
        int iy = (int) (screenY / iconScale);
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
        List<int[]> segments = ClientL2HConfig.getColorSegments();
        for (int i = segments.size() - 1; i >= 0; i--) {
            int[] seg = segments.get(i);
            if (diff >= seg[0]) {
                return FastColor.ARGB32.color(255, seg[1], seg[2], seg[3]);
            }
        }
        return ClientL2HConfig.getDefaultColor();
    }
}
