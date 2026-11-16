package com.urntt.noclip.config;

/**
 * The kinds of collision noclip can pass through, each switched on its own.
 */
public enum CollisionType {
	/** Block collision shapes, including being pushed out of blocks and forced to crouch or crawl inside them. */
	BLOCKS(true),
	/** Drag, currents, buoyancy, and swimming in water and lava, including bubble columns. */
	FLUIDS(false),
	/** The collision wall of the world border. */
	WORLD_BORDER(true),
	/** Blocks without a collision shape that slow the player down: cobwebs, sweet berry bushes, and powder snow. */
	SLOWING_BLOCKS(false),
	/** Solid entity hitboxes, such as boats, minecarts, and shulkers, and pushing between nearby entities. */
	ENTITIES(true);

	private final boolean shapeBased;

	CollisionType(final boolean shapeBased) {
		this.shapeBased = shapeBased;
	}

	/**
	 * Returns whether this type is made of collision shapes, which noclip ignores only along the enabled axes. The
	 * other types are effects without a direction and are ignored as a whole.
	 */
	public boolean isShapeBased() {
		return this.shapeBased;
	}
}
