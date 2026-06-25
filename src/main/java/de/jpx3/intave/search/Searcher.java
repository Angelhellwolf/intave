package de.jpx3.intave.search;

import java.util.*;
import java.util.function.Function;

public final class Searcher<I, T extends SearchConfig> {
	private final List<SearchBrancher<I, T>> branchers;
	private final Function<I, T> initial;
	private final boolean cacheResults;
	private final Map<I, Set<T>> brancherCache = new HashMap<>();

	public Searcher(List<SearchBrancher<I, T>> branchers, Function<I, T> initial) {
		this(branchers, initial, true);
	}

	public Searcher(List<SearchBrancher<I, T>> branchers, Function<I, T> initial, boolean cacheResults) {
		this.branchers = branchers;
		this.initial = initial;
		this.cacheResults = cacheResults;
	}

	public Set<T> searchConfigurationsFor(I input) {
		if (cacheResults) {
			Set<T> cached = brancherCache.get(input);
			if (cached != null) {
				return cached;
			}
		}
		Set<T> result = new LinkedHashSet<>(8);
		result.add(initial.apply(input));
		for (SearchBrancher<I, T> brancher : branchers) {
			Set<T> newResult = new LinkedHashSet<>(8);
			for (T t : result) {
				Set<T> output = brancher.branch(input, t);
				newResult.addAll(output);
			}
			result = newResult;
		}
		if (cacheResults) {
			brancherCache.put(input, result);
		}
		return result;
	}
}
