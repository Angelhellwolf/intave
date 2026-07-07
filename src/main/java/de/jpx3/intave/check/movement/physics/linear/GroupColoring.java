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

package de.jpx3.intave.check.movement.physics.linear;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class GroupColoring<T> {
	private final Map<T, Integer> colors;

	private GroupColoring(Map<T, Integer> colors) {
		this.colors = colors;
	}

	public void distinctBy(
		Function<T, Object> classifier
	) {
		// no two Ts should have the same color that don't share the same classifier
		for (T t1 : colors.keySet()) {
			for (T t2 : colors.keySet()) {
				if (t1 == t2 || !colors.get(t1).equals(colors.get(t2))) {
					continue;
				}
				if (classifier.apply(t1).equals(classifier.apply(t2))) {
					colors.put(t2, colors.get(t1));
				}
			}
		}
	}

	public static <T> GroupColoring<T> distinct(
		Set<T> elements
	) {
		Map<T, Integer> colors = new HashMap<>();
		for (T element : elements) {
			colors.put(element, colors.size());
		}
		return new GroupColoring<>(colors);
	}

	public static <T> GroupColoring<T> uniform(
		Set<T> elements
	) {
		Map<T, Integer> colors = new HashMap<>();
		for (T element : elements) {
			colors.put(element, 0);
		}
		return new GroupColoring<>(colors);
	}
}
