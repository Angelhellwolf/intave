package de.jpx3.intave.check.movement.physics.branch;

import de.jpx3.intave.search.SearchBrancher;

import java.util.Arrays;
import java.util.List;

public final class MovementSearchBranchers {
  private MovementSearchBranchers() {
  }

  public static List<SearchBrancher<MovementSearchInput, MovementSearchConfig>> normal() {
    return Arrays.asList(
      new RotationBrancher(),
      new SprintingBrancher(),
      new UseItemBrancher(),
      new AttackReduceBrancher(),
      new JumpBrancher(),
      new KeypressBrancher()
    );
  }
}
