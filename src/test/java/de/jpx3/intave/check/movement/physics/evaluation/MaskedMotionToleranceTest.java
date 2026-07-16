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

package de.jpx3.intave.check.movement.physics.evaluation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MaskedMotionToleranceTest {

	@Test
	public void testStupidity() {
		MaskedMotionTolerance tolerance = new MaskedMotionTolerance();
		tolerance.set(
			0.4707, 0.0
		);
		
		assertTrue(tolerance.isMotionXWithinLimit(
			0.4832
		));
	}

}