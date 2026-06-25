package de.jpx3.intave.check.movement.physics.search;

import de.jpx3.intave.search.SearchBrancher;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

abstract class MovementSearchBrancher extends SearchBrancher<MovementSearchInput, MovementSearchConfig> {
  final Set<MovementSearchConfig> single(MovementSearchConfig config) {
    return Collections.singleton(config);
  }

  final Set<MovementSearchConfig> ordered() {
    return new LinkedHashSet<>();
  }
}
