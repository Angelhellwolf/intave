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

package de.jpx3.intave.user;

import org.bukkit.entity.Player;

import java.util.function.Function;
import java.util.function.Supplier;

public final class ThreadUserLocal<T> {
	private final ThreadLocal<UserLocal<T>> local;

	private ThreadUserLocal(Function<? super User, ? extends T> initializer) {
		this.local = ThreadLocal.withInitial(() -> UserLocal.withInitialThreadUnsafe(initializer));
	}

	public T get(Player player) {
		return get(UserRepository.userOf(player));
	}

	public T get(User user) {
		if (user == null) {
			throw new IllegalArgumentException("User must not be null");
		}
		return local.get().get(user);
	}

	public static <T> ThreadUserLocal<T> withInitial(Function<? super User, ? extends T> initializer) {
		return new ThreadUserLocal<>(initializer);
	}

	public static <T> ThreadUserLocal<T> withInitial(Supplier<? extends T> initializer) {
		return new ThreadUserLocal<>(user -> initializer.get());
	}
}
