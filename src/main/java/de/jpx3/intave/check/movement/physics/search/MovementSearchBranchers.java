package de.jpx3.intave.check.movement.physics.search;

import de.jpx3.intave.search.SearchBrancher;

import java.util.Arrays;
import java.util.List;

public final class MovementSearchBranchers {
  private MovementSearchBranchers() {
  }

  public static List<SearchBrancher<MovementSearchInput, MovementSearchConfig>> normal() {
    return Arrays.asList(
      new SprintingBrancher(),
      new UseItemBrancher(),
      new AttackReduceBrancher(),
      new JumpBrancher(),
      new KeypressBrancher()
    );
  }

  public static List<SearchBrancher<MovementSearchInput, MovementSearchConfig>> flyingPrevRotAnticip() {
    return Arrays.asList(
      new SprintingBrancher(),
      new UseItemBrancher(),
      new AttackReduceBrancher(),
      new JumpBrancher(),
      new KeypressBrancher()
    );
  }
}
