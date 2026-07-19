/*
 * Copyright 2026 Intave
 *
 * This software is licensed under the PolyForm Perimeter License 1.0.0.
 * You may use this software for any purpose, except for providing to
 * others any product that competes with the software.
 *
 * A copy of the license is available at:
 *   https://polyformproject.org/licenses/perimeter/1.0.0/
 */

package de.jpx3.intave.entity.type;

import java.util.Locale;

public enum RideableEntityType {
	BOAT,
	CHEST_BOAT,
	LLAMA,
	MINECART,
	RAVAGER,
	SKELETON_HORSE,
	SPIDER

	;

	public static RideableEntityType identifyFrom(
		String entityName
	) {
		switch (entityName.toLowerCase(Locale.ROOT)) {
			case "boat":
				return BOAT;
			case "chestboat":
				return CHEST_BOAT;
			case "llama":
				return LLAMA;
			case "minecart":
				return MINECART;
			case "ravager":
				return RAVAGER;
			case "skeletonhorse":
				return SKELETON_HORSE;
			case "spider":
				return SPIDER;
		}
		return null;
	}
}
