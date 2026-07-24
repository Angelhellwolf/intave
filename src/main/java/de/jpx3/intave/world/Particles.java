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

package de.jpx3.intave.world;

import de.jpx3.intave.share.Position;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.UserLocal;
import org.bukkit.Effect;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class Particles {
	private final static UserLocal<Map<Position, Long>> lastParticleSpawn = UserLocal.withInitial(() -> new HashMap<>());

	public static void spawnVillagerHappyParticleAt(User user, Position position) {
		Map<Position, Long> lastParticleSpawnMap = lastParticleSpawn.get(user);
		long currentTime = System.currentTimeMillis();
		if (lastParticleSpawnMap.containsKey(position)) {
			long lastSpawnTime = lastParticleSpawnMap.get(position);
			if (currentTime - lastSpawnTime < 200) {
				return; // Skip spawning the particle if it was spawned less than 1 second ago
			}
		}
		lastParticleSpawnMap.entrySet().removeIf(entry -> currentTime - entry.getValue() > 3000); // Clean up old entries
		lastParticleSpawnMap.put(position, currentTime);
		Player player = user.player();
		World world = player.getWorld();
		Object villagerHappyParticle = villagerHappyParticle();
		if (villagerHappyParticleCacheFailed) {
			player.playEffect(position.toLocation(world), Effect.HAPPY_VILLAGER, 0);
		} else {
			player.spawnParticle(
				(Particle) villagerHappyParticle,
				position.toLocation(world), 1
			);
		}
	}

	private static Object villagerHappyParticleCache;
	private static Boolean villagerHappyParticleCacheFailed = false;

	private static Object villagerHappyParticle() {
		if (villagerHappyParticleCache == null && !villagerHappyParticleCacheFailed) {
			try {
				try {
					villagerHappyParticleCache = Particle.VILLAGER_HAPPY;
				} catch (NoSuchFieldError e) {
					villagerHappyParticleCache = Particle.class.getField("HAPPY_VILLAGER").get(null);
				} catch (NoClassDefFoundError e) {
					villagerHappyParticleCacheFailed = true;
				}
			} catch (IllegalAccessException | NoSuchFieldException e) {
				villagerHappyParticleCacheFailed = true;
				throw new RuntimeException(e);
			}
		}
		return villagerHappyParticleCache;
	}
}
