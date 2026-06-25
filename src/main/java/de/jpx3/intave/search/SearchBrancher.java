package de.jpx3.intave.search;

import java.util.Set;

public abstract class SearchBrancher<I, T extends SearchConfig> {
	public abstract Set<T> branch(I input, T config);
}
