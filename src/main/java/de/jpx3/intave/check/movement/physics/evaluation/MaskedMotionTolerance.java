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

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.jpx3.intave.codec.StreamCodec;

import static de.jpx3.intave.codec.JsonStreamCodecs.doubleField;
import static de.jpx3.intave.codec.JsonStreamCodecs.object;

public final class MaskedMotionTolerance {
	public static final StreamCodec<JsonReader, JsonWriter, MaskedMotionTolerance> JSON_CODEC = object(
		doubleField("motionXTarget", MaskedMotionTolerance::motionXTarget),
		doubleField("motionZTarget", MaskedMotionTolerance::motionZTarget),
		MaskedMotionTolerance::new
	)
	.encodeOnly(doubleField("motionXTolerance", MaskedMotionTolerance::motionXTolerance))
	.encodeOnly(doubleField("motionZTolerance", MaskedMotionTolerance::motionZTolerance));

	private double motionXTarget;
	private double motionZTarget;

	public MaskedMotionTolerance() {
		this(0, 0);
	}

	public MaskedMotionTolerance(double motionXTarget, double motionZTarget) {
		this.motionXTarget = motionXTarget;
		this.motionZTarget = motionZTarget;
	}

	public void set(
		double motionXLimit,
		double motionZLimit
	) {
		this.motionXTarget = motionXLimit;
		this.motionZTarget = motionZLimit;
	}
	
	private final static double MOTION_TOLERANCE = 0.1;

	public boolean isMotionXWithinLimit(double motionX) {
		if (motionXTarget > 0) {
			return -0.03 <= motionX && motionX <= motionXTarget + MOTION_TOLERANCE;
		} else {
			return motionX <= 0.03 && motionX >= motionXTarget - MOTION_TOLERANCE;
		}
	}

	public boolean isMotionZWithinLimit(double motionZ) {
		if (motionZTarget > 0) {
			return -0.03 <= motionZ && motionZ <= motionZTarget + MOTION_TOLERANCE;
		} else {
			return motionZ <= 0.03 && motionZ >= motionZTarget - MOTION_TOLERANCE;
		}
	}

	public double motionXTolerance() {
		return Math.abs(motionXTarget) + MOTION_TOLERANCE;
	}

	public double motionZTolerance() {
		return Math.abs(motionZTarget) + MOTION_TOLERANCE;
	}

	public double motionXTarget() {
		return motionXTarget;
	}

	public double motionZTarget() {
		return motionZTarget;
	}

	public void afterTick(
		double sentOffsetX,
		double sentOffsetZ
	) {
		motionXTarget *= 0.98;
		motionZTarget *= 0.98;

		if (Math.abs(sentOffsetX) > 0.03) {
			motionXTarget = 0;
		}
		if (Math.abs(sentOffsetZ) > 0.03) {
			motionZTarget = 0;
		}
	}
}
