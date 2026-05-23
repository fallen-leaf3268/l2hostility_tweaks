package com.l2hostility_tweaks.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.network.NetworkHandler;
import com.l2hostility_tweaks.util.TraitDisableHelper;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.capability.player.PlayerDifficulty;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2library.base.menu.base.MenuLayoutConfig;
import dev.xkmc.l2library.base.menu.base.SpriteManager;
import dev.xkmc.l2tabs.tabs.contents.BaseTextScreen;
import dev.xkmc.l2tabs.tabs.core.TabManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PlayerTraitScreen extends BaseTextScreen {

	public static boolean playerOverheadEnabled = true;

	private static final int WIDTH = 176;
	private static final int HEIGHT = 166;
	private static final int LINES_PER_PAGE = 10;
	private static final int NAV_BTN_W = 9;
	private static final ResourceLocation TEXTURE =
			new ResourceLocation("l2tabs", "textures/gui/empty.png");
	private static final Path OVERHEAD_FILE =
			FMLPaths.CONFIGDIR.get().resolve("l2hostility_tweaks_overhead.json");

	static {
		if (Files.exists(OVERHEAD_FILE)) {
			try {
				String raw = Files.readString(OVERHEAD_FILE);
				playerOverheadEnabled = JsonParser.parseString(raw)
						.getAsJsonObject().get("enabled").getAsBoolean();
			} catch (Exception ignored) {}
		}
	}

	private record TraitEntry(@javax.annotation.Nullable MobTrait trait, MutableComponent text, int xOffset, int width) {}

	private final MenuLayoutConfig config;
	private int currentPage;
	private int totalPages;
	private List<List<TraitEntry>> allLines;
	private int refreshCountdown = -1;
	private Button toggleButton;
	private Button prevButton;
	private Button nextButton;

	protected PlayerTraitScreen() {
		super(Component.translatable("screen.l2hostility_tweaks.player_traits"), TEXTURE);
		this.imageWidth = WIDTH;
		this.imageHeight = HEIGHT;
		this.config = new SpriteManager("l2hostility_tweaks", "player_traits").get();
	}

	public static void open() {
		Minecraft.getInstance().setScreen(new PlayerTraitScreen());
	}

	private static void saveOverheadConfig() {
		try {
			JsonObject obj = new JsonObject();
			obj.addProperty("enabled", playerOverheadEnabled);
			Files.writeString(OVERHEAD_FILE, obj.toString());
		} catch (IOException ignored) {}
	}

	@Override
	protected void init() {
		super.init();
		new TabManager(this).init(this::addRenderableWidget, ClientEventHandler.TAB_PLAYER_TRAITS);

		currentPage = 0;
		rebuildLines();
		if (allLines != null) {
			totalPages = Math.max(1, (allLines.size() + LINES_PER_PAGE - 1) / LINES_PER_PAGE);
		} else {
			totalPages = 1;
		}

		int toggleH = font.lineHeight + 4;
		toggleButton = Button.builder(getToggleText(), b -> {
				playerOverheadEnabled = !playerOverheadEnabled;
				saveOverheadConfig();
			})
			.bounds(leftPos + config.getComp("toggle").x, topPos + config.getComp("toggle").y + 3,
					config.getComp("toggle").w, toggleH)
			.build();
		addRenderableWidget(toggleButton);

		prevButton = addRenderableWidget(Button.builder(
				Component.literal("<"),
				b -> { if (currentPage > 0) { currentPage--; } })
				.bounds(leftPos + config.getComp("prev").x, topPos + config.getComp("prev").y + 3, NAV_BTN_W, toggleH)
				.build());

		nextButton = addRenderableWidget(Button.builder(
				Component.literal(">"),
				b -> { if (currentPage < totalPages - 1) { currentPage++; } })
				.bounds(leftPos + config.getComp("next").x, topPos + config.getComp("next").y + 3, NAV_BTN_W, toggleH)
				.build());
	}

	private void rebuildLines() {
		var player = Minecraft.getInstance().player;
		if (player != null && MobTraitCap.HOLDER.isProper(player)) {
			MobTraitCap cap = MobTraitCap.HOLDER.get(player);
			if (cap.isInitialized() && !cap.traits.isEmpty()) {
				TraitDisableHelper.setDisplayEntity(player);
				TraitDisableHelper.setHideRealityDetail(true);
				try {
					allLines = buildTraitLines(cap);
				} finally {
					TraitDisableHelper.setHideRealityDetail(false);
					TraitDisableHelper.clearDisplayEntity();
				}
				return;
			}
		}
		allLines = null;
	}

	@Override
	public void tick() {
		super.tick();
		boolean needsRefresh = false;
		if (refreshCountdown > 0) {
			refreshCountdown--;
			if (refreshCountdown == 0) {
				needsRefresh = true;
			}
		}
		if (refreshCountdown <= 0 && minecraft.player != null && minecraft.player.tickCount % 20 == 0) {
			needsRefresh = true;
		}
		if (needsRefresh) {
			rebuildLines();
			totalPages = allLines != null ? Math.max(1, (allLines.size() + LINES_PER_PAGE - 1) / LINES_PER_PAGE) : 1;
			if (currentPage >= totalPages) currentPage = totalPages - 1;
		}
	}

	private Component getToggleText() {
		return Component.translatable(playerOverheadEnabled
				? "gui.l2hostility_tweaks.overhead_on"
				: "gui.l2hostility_tweaks.overhead_off");
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		if (toggleButton != null) {
			toggleButton.setMessage(getToggleText());
		}
		super.render(g, mx, my, partial);
		if (toggleButton != null) toggleButton.setFocused(false);
		if (prevButton != null) prevButton.setFocused(false);
		if (nextButton != null) nextButton.setFocused(false);

		int left = leftPos;
		int top = topPos;

		var player = Minecraft.getInstance().player;

		int playerLevel = 0;
		MobTraitCap cap = null;
		if (player != null && MobTraitCap.HOLDER.isProper(player)) {
			cap = MobTraitCap.HOLDER.get(player);
			playerLevel = PlayerDifficulty.HOLDER.isProper(player)
					? PlayerDifficulty.HOLDER.get(player).getLevel().getLevel() : 0;
		}

		Component headerText;
		if (L2HConfig.isPlayerSelfTraitBalanceEnabled()) {
			int budget = (int) (playerLevel * L2HConfig.getPlayerSelfTraitBudgetRatio());
			int usedCost = 0;
			if (cap != null && cap.isInitialized()) {
				for (var entry : cap.traits.entrySet()) {
					var override = L2HConfig.getPlayerTraitOverrides().get(entry.getKey().getID());
					int cost = override != null ? override.cost() : entry.getKey().getConfig().cost;
					usedCost += cost * Math.abs(entry.getValue());
				}
			}
			headerText = Component.literal("(" + usedCost + "/" + budget + ")");
		} else {
			headerText = Component.literal("Lv." + playerLevel);
		}
		g.drawString(font, headerText,
				left + config.getComp("header").x, top + config.getComp("header").y + 3, 0xFFAA00);

		if (player == null || !MobTraitCap.HOLDER.isProper(player)) {
			g.drawCenteredString(font, Component.translatable("gui.l2hostility_tweaks.no_traits"),
					left + WIDTH / 2, top + 60, 0xAAAAAA);
			return;
		}
		if (cap == null || !cap.isInitialized() || cap.traits.isEmpty()) {
			g.drawCenteredString(font, Component.translatable("gui.l2hostility_tweaks.no_traits"),
					left + WIDTH / 2, top + 60, 0xAAAAAA);
			return;
		}

		if (allLines == null || allLines.isEmpty()) {
			g.drawCenteredString(font, Component.translatable("gui.l2hostility_tweaks.no_traits"),
					left + WIDTH / 2, top + 60, 0xAAAAAA);
			return;
		}

		int start = currentPage * LINES_PER_PAGE;
		int end = Math.min(start + LINES_PER_PAGE, allLines.size());

		int traitX = left + config.getComp("traits").x;
		int traitY = top + config.getComp("traits").y;
		int lineHeight = font.lineHeight + 1;

		for (int i = start; i < end; i++) {
			int ly = traitY + (i - start) * lineHeight;
			for (TraitEntry e : allLines.get(i)) {
				g.drawString(font, e.text(), traitX + e.xOffset(), ly, 0x404040, false);
			}
		}

		for (int i = start; i < end; i++) {
			int ly = traitY + (i - start) * lineHeight;
			for (TraitEntry e : allLines.get(i)) {
				if (e.trait() == null) continue;
				int ex = traitX + e.xOffset();
				if (mx >= ex && mx < ex + e.width() && my >= ly && my < ly + lineHeight) {
					ItemStack stack = new ItemStack(e.trait().asItem());
					List<Component> lines = new ArrayList<>(stack.getTooltipLines(
							player, minecraft.options.advancedItemTooltips
									? net.minecraft.world.item.TooltipFlag.Default.ADVANCED
					: net.minecraft.world.item.TooltipFlag.Default.NORMAL));
					Integer lvl = cap.traits.get(e.trait());
					int curLevel = lvl != null ? lvl : 0;
						if (curLevel < 0) {
							String expiryKey = TraitDisableHelper.sealExpiryKey(e.trait().getID());
							long remaining = -1;
							try {
								var server = Minecraft.getInstance().getSingleplayerServer();
								if (server != null) {
									var sp = server.getPlayerList().getPlayer(player.getUUID());
									if (sp != null && sp.getPersistentData().contains(expiryKey)) {
										long expiry = sp.getPersistentData().getLong(expiryKey);
										remaining = Math.max(0, expiry - player.level().getGameTime()) / 20;
									}
								}
							} catch (Exception ignored) {}
							if (remaining >= 0) {
								lines.add(Component.translatable("gui.l2hostility_tweaks.seal_remaining",
										Component.literal(String.valueOf(remaining)).withStyle(ChatFormatting.AQUA))
										.withStyle(ChatFormatting.RED));
							} else {
								lines.add(Component.translatable("gui.l2hostility_tweaks.trait_sealed").withStyle(ChatFormatting.RED));
							}
					} else if (curLevel >= e.trait().getMaxLevel()) {
						lines.add(Component.translatable("gui.l2hostility_tweaks.max_level").withStyle(ChatFormatting.GOLD));
						if (hasShiftDown()) {
							lines.add(Component.translatable("gui.l2hostility_tweaks.unload_all_hint").withStyle(ChatFormatting.GOLD));
						} else {
							lines.add(Component.translatable("gui.l2hostility_tweaks.unload_hint").withStyle(ChatFormatting.GOLD));
						}
					} else {
						int mode = L2HConfig.getPlayerSelfTraitCostMode();
						int upgradeCost;
						if (mode == 2) {
							upgradeCost = curLevel + 1;
						} else if (mode == 3) {
							upgradeCost = 1 << curLevel;
						} else {
							upgradeCost = 1;
						}
						lines.add(Component.translatable("gui.l2hostility_tweaks.upgrade_cost", upgradeCost).withStyle(ChatFormatting.GOLD));
						if (hasShiftDown()) {
							lines.add(Component.translatable("gui.l2hostility_tweaks.unload_all_hint").withStyle(ChatFormatting.GOLD));
						} else {
							lines.add(Component.translatable("gui.l2hostility_tweaks.unload_hint").withStyle(ChatFormatting.GOLD));
						}
					}
					g.renderTooltip(font, lines, stack.getTooltipImage(), mx, my);
					break;
				}
			}
		}
	}

	private List<List<TraitEntry>> buildTraitLines(MobTraitCap cap) {
		List<List<TraitEntry>> result = new ArrayList<>();
		List<TraitEntry> curLine = new ArrayList<>();
		int curWidth = 0;
		int sepWidth = font.width("  ");
		int maxW = config.getComp("traits").w;

		for (var entry : cap.traits.entrySet()) {
			int level = entry.getValue();
			MutableComponent tc;
			if (level < 0) {
				tc = entry.getKey().getFullDesc(-level);
				tc = tc.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.STRIKETHROUGH);
			} else {
				tc = entry.getKey().getFullDesc(level);
			}
			int tw = font.width(tc);
			int needed = curWidth > 0 ? sepWidth + tw : tw;
			if (curWidth + needed > maxW) {
				result.add(curLine);
				curLine = new ArrayList<>();
				curWidth = 0;
			}
			if (curWidth > 0) {
				curLine.add(new TraitEntry(null, Component.literal("  "), curWidth, sepWidth));
				curWidth += sepWidth;
			}
			curLine.add(new TraitEntry(entry.getKey(), tc, curWidth, tw));
			curWidth += tw;
		}
		if (!curLine.isEmpty()) result.add(curLine);
		return result;
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (button == 1 && allLines != null) {
			int traitX = leftPos + config.getComp("traits").x;
			int traitY = topPos + config.getComp("traits").y;
			int lineHeight = font.lineHeight + 1;
			int start = currentPage * LINES_PER_PAGE;
			int end = Math.min(start + LINES_PER_PAGE, allLines.size());
			for (int i = start; i < end; i++) {
				int ly = traitY + (i - start) * lineHeight;
				for (TraitEntry e : allLines.get(i)) {
					if (e.trait() == null) continue;
					int ex = traitX + e.xOffset();
					if (mx >= ex && mx < ex + e.width() && my >= ly && my < ly + lineHeight) {
						boolean unloadAll = hasShiftDown();
						NetworkHandler.sendUnloadToServer(e.trait().getID(), unloadAll);
						refreshCountdown = 3;
						return true;
					}
				}
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
