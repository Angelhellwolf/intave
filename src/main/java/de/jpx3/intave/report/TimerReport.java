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

package de.jpx3.intave.report;

import com.google.gson.JsonObject;
import de.jpx3.intave.user.User;

public final class TimerReport implements Report {


	@Override
	public User suspect() {
		return null;
	}

	@Override
	public JsonObject toJson() {
		return null;
	}
}
