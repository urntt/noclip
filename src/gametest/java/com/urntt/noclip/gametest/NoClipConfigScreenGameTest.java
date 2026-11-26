package com.urntt.noclip.gametest;

import static com.urntt.noclip.gametest.GameTestSupport.LOGGER;
import static com.urntt.noclip.gametest.GameTestSupport.check;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.urntt.noclip.NoClipClient;
import com.urntt.noclip.compat.OneConfigBridge;
import com.urntt.noclip.config.ConfigMode;
import com.urntt.noclip.config.MultiplayerMode;
import com.urntt.noclip.config.NoClipConfig;
import com.urntt.noclip.config.NoClipConfigScreen;
import com.urntt.noclip.config.NoClipValues;
import com.urntt.noclip.config.SceneRules;
import com.urntt.noclip.config.ServerListScreen;
import com.urntt.noclip.config.SettingKeys;
import com.urntt.noclip.config.Toggle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Checks the configuration modes and the settings screen. The test client runs without OneConfig by default and with
 * it when Gradle is given {@code -PoneConfigRuntime=true}; the checks follow whichever modes are offered. It runs
 * before the tests that join a world, because the run directory starts empty, so no mode has been chosen yet. It
 * leaves custom mode selected.
 */
@SuppressWarnings("UnstableApiUsage")
public final class NoClipConfigScreenGameTest implements FabricClientGameTest {
	/** Mouse wheel steps between the screenshots that show the screen from top to bottom. */
	private static final int PAGE_SCROLL = 6;
	/** Screenshots between the top and the bottom of the screen. */
	private static final int MIDDLE_PAGES = 2;
	/** Four toggles for the defaults and reset rules, the multiplayer mode, and the server list button. */
	private static final int SCENE_RULE_WIDGETS = 4 + 2;

	@Override
	public void runTest(final ClientGameTestContext context) {
		NoClipConfig config = NoClipClient.config();
		boolean oneConfigInstalled = FabricLoader.getInstance().isModLoaded(OneConfigBridge.MOD_ID);
		check(OneConfigBridge.isAvailable() == oneConfigInstalled,
				"OneConfig mode should be available exactly when OneConfig is installed (installed: " + oneConfigInstalled + ")");

		ConfigMode unchosen = oneConfigInstalled ? ConfigMode.ONECONFIG : ConfigMode.CUSTOM;
		check(config.storedMode() == null, "no mode should be chosen yet, was " + config.storedMode());
		check(config.effectiveMode() == unchosen, "without a choice, the mode should be " + unchosen + ", was "
				+ config.effectiveMode());
		NoClipValues applied = context.computeOnClient(client -> NoClipValues.copyOf(config.effective()));
		NoClipValues expectedDefaults = oneConfigInstalled ? NoClipValues.ONECONFIG_DEFAULTS : NoClipValues.DEFAULTS;
		check(applied.equals(expectedDefaults), "without a choice, " + unchosen + " mode's defaults should apply, was "
				+ applied);

		context.setScreen(() -> new NoClipConfigScreen(null));
		parkCursor(context);
		context.takeScreenshot("noclip-config-default");
		checkSettingWidgets(context, unchosen);

		checkModeCycle(context, config, oneConfigInstalled ? List.of(ConfigMode.values())
				: List.of(ConfigMode.DISABLED, ConfigMode.CUSTOM), unchosen);
		context.runOnClient(client -> config.setMode(ConfigMode.CUSTOM));

		context.runOnClient(client -> {
			config.setMultiplayerMode(MultiplayerMode.WHITELIST);
			config.setServers(List.of("mc.example.com", "192.168.1.5"));
		});
		for (String language : List.of("en_us", "zh_cn")) {
			setLanguage(context, language);
			screenshotAllOptions(context, language);
		}
		setLanguage(context, "en_us");
		context.runOnClient(client -> {
			config.setMultiplayerMode(SceneRules.DEFAULTS.multiplayerMode());
			config.setServers(SceneRules.DEFAULTS.servers());
		});
		context.setScreen(() -> null);
	}

	/**
	 * Clicks the mode button through every offered mode, in the button's order, and back to {@code start}. OneConfig
	 * mode is only offered while OneConfig is installed.
	 */
	private static void checkModeCycle(final ClientGameTestContext context, final NoClipConfig config,
			final List<ConfigMode> modes, final ConfigMode start) {
		// A custom value that differs from OneConfig's default shows which settings apply in OneConfig mode.
		Toggle marker = Toggle.AXIS_Y;
		boolean markerDefault = NoClipValues.DEFAULTS.get(marker);
		context.runOnClient(client -> config.set(marker, !markerDefault));

		int first = modes.indexOf(start);
		for (int i = 1; i <= modes.size(); i++) {
			ConfigMode expected = modes.get((first + i) % modes.size());
			clickWidget(context, widget -> widget instanceof CycleButton<?> cycle && cycle.getValue() instanceof ConfigMode);
			context.waitForScreen(NoClipConfigScreen.class);
			check(config.storedMode() == expected, "the mode button should switch to " + expected + ", was " + config.storedMode());
			check(NoClipConfig.load(NoClipConfig.defaultPath()).storedMode() == expected, "the " + expected + " mode should be saved");
			checkSettingWidgets(context, expected);
			parkCursor(context);
			context.takeScreenshot("noclip-config-" + expected.name().toLowerCase(Locale.ROOT));

			NoClipValues applied = context.computeOnClient(client -> NoClipValues.copyOf(config.effective()));
			switch (expected) {
				case DISABLED -> check(applied.equals(NoClipValues.DISABLED), "disabled mode should apply the disabled values");
				case ONECONFIG -> {
					check(applied.get(marker) == markerDefault && applied.sceneRules() == null,
							"OneConfig mode should apply OneConfig's settings, which have no scene rules, was " + applied);
					checkOneConfigPage(context);
				}
				case CUSTOM -> check(applied.get(marker) != markerDefault, "custom mode should apply the custom settings");
			}
		}

		context.runOnClient(client -> config.set(marker, markerDefault));
	}

	/** In OneConfig mode, the screen links to the mod's OneConfig page. */
	private static void checkOneConfigPage(final ClientGameTestContext context) {
		clickWidget(context, widget -> widget instanceof Button && hasKey(widget, SettingKeys.OPEN_ONECONFIG));
		context.waitFor(client -> client.gui.screen() != null && !(client.gui.screen() instanceof NoClipConfigScreen));
		context.waitTicks(20);
		context.takeScreenshot("noclip-oneconfig-page");
		// The options are at the bottom of the page.
		double[] center = screenCenter(context);
		context.getInput().setCursorPos(center[0], center[1]);
		context.getInput().scroll(-100);
		context.waitTicks(20);
		context.takeScreenshot("noclip-oneconfig-page-bottom");
		String screen = context.computeOnClient(client -> client.gui.screen().getClass().getName());
		LOGGER.info("Open in OneConfig shows {}", screen);
		context.setScreen(() -> new NoClipConfigScreen(null));
		// The option list positions its widgets while rendering, so let it render before clicking.
		context.waitTicks(2);
	}

	/** Shows the whole screen from top to bottom, then the server list screen. */
	private static void screenshotAllOptions(final ClientGameTestContext context, final String language) {
		context.setScreen(() -> new NoClipConfigScreen(null));
		parkCursor(context);
		context.takeScreenshot("noclip-config-" + language + "-top");
		double[] center = screenCenter(context);
		for (int page = 1; page <= MIDDLE_PAGES; page++) {
			context.getInput().setCursorPos(center[0], center[1]);
			context.getInput().scroll(-PAGE_SCROLL);
			parkCursor(context);
			context.takeScreenshot("noclip-config-" + language + "-middle-" + page);
		}
		context.getInput().setCursorPos(center[0], center[1]);
		context.getInput().scroll(-100);
		parkCursor(context);
		context.takeScreenshot("noclip-config-" + language + "-bottom");

		context.setScreen(() -> new ServerListScreen(new NoClipConfigScreen(null), NoClipClient.config()));
		parkCursor(context);
		context.takeScreenshot("noclip-server-list-" + language);
	}

	/** The center of the window, in the cursor coordinates the test input uses. */
	private static double[] screenCenter(final ClientGameTestContext context) {
		return context.computeOnClient(client -> {
			Window window = client.getWindow();
			return new double[] {window.getScreenWidth() / 2.0, window.getScreenHeight() / 2.0};
		});
	}

	private static void setLanguage(final ClientGameTestContext context, final String code) {
		CompletableFuture<Void> reload = context.computeOnClient(client -> {
			client.getLanguageManager().setSelected(code);
			client.options.languageCode = code;
			return client.reloadResourcePacks();
		});
		context.waitFor(client -> reload.isDone() && client.gui.overlay() == null);
	}

	/** Clicks the first widget of the open screen that matches, with the real mouse. */
	private static void clickWidget(final ClientGameTestContext context, final Predicate<AbstractWidget> matches) {
		double[] position = context.computeOnClient(client -> {
			AbstractWidget widget = widgets(client).stream()
					.filter(matches)
					.findFirst()
					.orElseThrow(() -> new AssertionError("widget to click not found"));
			Window window = client.getWindow();
			double scale = window.getScreenWidth() / (double) window.getGuiScaledWidth();
			return new double[] {(widget.getX() + widget.getWidth() / 2.0) * scale, (widget.getY() + widget.getHeight() / 2.0) * scale};
		});
		context.getInput().setCursorPos(position[0], position[1]);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTick();
	}

	/** Moves the cursor into a corner, so that no tooltip covers the screenshot. */
	private static void parkCursor(final ClientGameTestContext context) {
		context.getInput().setCursorPos(0, 0);
		context.waitTicks(2);
	}

	/**
	 * Checks that every widget that edits a setting is there, and that they can only be edited in custom mode.
	 * OneConfig mode has no defaults, reset rules, or multiplayer rules, so their widgets are left out there.
	 */
	private static void checkSettingWidgets(final ClientGameTestContext context, final ConfigMode mode) {
		int expected = Toggle.values().length + (mode == ConfigMode.ONECONFIG ? 0 : SCENE_RULE_WIDGETS);
		boolean editable = mode == ConfigMode.CUSTOM;
		context.runOnClient(client -> {
			List<AbstractWidget> settings = widgets(client).stream()
					.filter(widget -> widget instanceof CycleButton<?> cycle
							&& (cycle.getValue() instanceof Boolean || cycle.getValue() instanceof MultiplayerMode)
							|| widget instanceof Button && hasKey(widget, SettingKeys.EDIT_SERVERS))
					.toList();
			check(settings.size() == expected, "expected " + expected + " setting widgets in " + mode + " mode, found "
					+ settings.size() + ": " + widgets(client).stream()
							.map(widget -> widget.getClass().getSimpleName() + "[" + widget.getMessage().getString() + "]")
							.toList());
			check(settings.stream().allMatch(widget -> widget.active == editable), "settings should "
					+ (editable ? "be editable" : "be greyed out") + " in " + mode + " mode");
		});
	}

	/** Whether the widget's message is the translation of {@code key}. */
	private static boolean hasKey(final AbstractWidget widget, final String key) {
		return widget.getMessage().getContents() instanceof TranslatableContents contents && contents.getKey().equals(key);
	}

	private static List<AbstractWidget> widgets(final Minecraft client) {
		Screen screen = Objects.requireNonNull(client.gui.screen(), "no screen is open");
		List<AbstractWidget> widgets = new ArrayList<>();
		collect(screen.children(), widgets);
		return widgets;
	}

	private static void collect(final List<? extends GuiEventListener> listeners, final List<AbstractWidget> widgets) {
		for (GuiEventListener listener : listeners) {
			if (listener instanceof AbstractWidget widget) {
				widgets.add(widget);
			}
			if (listener instanceof ContainerEventHandler container) {
				collect(container.children(), widgets);
			}
		}
	}
}
