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

package de.jpx3.intave.check.movement.physics.environment;

public enum MoveMetric {
	ALIVE,
	ATTACK_REDUCE,
	BLOCK_PLACEMENT,
	EDGE_SNEAKING,
	EDGE_SNEAKING_TICK_GRANTS(0, 0),
	ELYTRA_FLYING,
	ENTITY_USE,
	EXTERNAL_VELOCITY,
	FIREWORK_ROCKETS,
	FLYING_PACKET_ACCURATE(0, 0),
	FLYING_PACKET_CLIENT(0, 0),
	INVENTORY_OPEN,
	IN_LAVA,
	IN_POWDER_SNOW,
	IN_WATER,
	IN_WEB,
	LONG_TELEPORT,
	NEARBY_COLLISION_INACCURACY(0, 10),
	RECEIVED_VELOCITY_PACKET,
	RIPTIDE_SPIN,
	SLIME_BLOCK,
	SNEAKING,
	SPRINTING,
	SPRINT_CHANGE,
	STEP,
	TELEPORT,
	VEHICLE_ATTACHMENT,
	VEHICLE_DETACHMENT,
	VEHICLE_EXIT,
	VELOCITY,
	WATERFLOW_PUSH;

	private final int activeDefault;
	private final int pastDefault;

	MoveMetric() {
		this(0, 100);
	}

	MoveMetric(int activeDefault, int pastDefault) {
		this.activeDefault = activeDefault;
		this.pastDefault = pastDefault;
	}

	public int activeDefault() {
		return activeDefault;
	}

	public int pastDefault() {
		return pastDefault;
	}
}
