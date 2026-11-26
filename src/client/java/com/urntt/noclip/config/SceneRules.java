package com.urntt.noclip.config;

import java.util.List;

/**
 * Where noclip may be active and what a reset restores there. Only the mod's own settings have these rules; OneConfig
 * mode has none, because OneConfig's profiles take their place.
 *
 * @param singleplayerDefault the state a reset restores in singleplayer worlds
 * @param multiplayerDefault  the state a reset restores on multiplayer servers that the multiplayer mode allows
 * @param resetOnWorldExit    whether every world starts from its default state
 * @param resetOnGameExit     whether the first world after starting the game starts from its default state
 * @param multiplayerMode     on which multiplayer servers noclip may be active
 * @param servers             the server list used by the whitelist and blacklist modes
 */
public record SceneRules(
		boolean singleplayerDefault,
		boolean multiplayerDefault,
		boolean resetOnWorldExit,
		boolean resetOnGameExit,
		MultiplayerMode multiplayerMode,
		List<String> servers) {
	/** The defaults: noclip starts off everywhere, nothing is reset, and every server is ruled out. */
	public static final SceneRules DEFAULTS = new SceneRules(false, false, false, false, MultiplayerMode.DISABLED, List.of());

	public SceneRules {
		servers = List.copyOf(servers);
	}
}
