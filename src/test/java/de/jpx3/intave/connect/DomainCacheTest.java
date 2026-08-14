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

package de.jpx3.intave.connect;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DomainCacheTest {
  @Test
  void serviceDomainsFallsBackWhenDiscoveryFindsNoDomains() {
    DomainCache cache = DomainCache.of(Collections.emptyMap(), Collections.emptyMap());

    assertEquals(Collections.singletonList("service.intave.de"), cache.serviceDomains());
  }
}
