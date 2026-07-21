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

package de.jpx3.intave.search;

import de.jpx3.intave.IntaveLogger;

import java.util.*;
import java.util.function.Function;

public final class Searcher<I, T> {
	private final List<SearchBrancher<I, T>> branchers;
	private final Function<I, T> initial;

	public Searcher(List<SearchBrancher<I, T>> branchers, Function<I, T> initial) {
		this.branchers = branchers;
		this.initial = initial;
	}

	private final ThreadLocal<List<T>> cachedAlphaLists = ThreadLocal.withInitial(ArrayList::new);
	private final ThreadLocal<List<T>> cachedBetaLists = ThreadLocal.withInitial(ArrayList::new);
	private final ThreadLocal<Set<T>> cachedDeduplicatedSets = ThreadLocal.withInitial(LinkedHashSet::new);

	public Set<T> searchConfigurationsFor(I input) {
		boolean alphaFirst = true;
		List<T> result = cachedAlphaLists.get();
		result.clear();
		result.add(initial.apply(input));
		Set<T> deduplicated = cachedDeduplicatedSets.get();
		for (SearchBrancher<I, T> brancher : branchers) {
			List<T> newResult = alphaFirst ? cachedBetaLists.get() : cachedAlphaLists.get();
			newResult.clear();
			for (T t : result) {
				brancher.branch(input, t, newResult);
			}
			if (newResult.isEmpty()) {
				IntaveLogger.logger().warn("Brancher " + brancher + " produced no results for input " + input + " and result " + result);
			}
			deduplicated.clear();
			deduplicated.addAll(newResult);
			newResult.clear();
			newResult.addAll(deduplicated);
			result = newResult;
			alphaFirst = !alphaFirst;
		}
		deduplicated.clear();
		deduplicated.addAll(result);
		// Result must be dropped before calling the method again
		return Collections.unmodifiableSet(deduplicated);
	}
}
