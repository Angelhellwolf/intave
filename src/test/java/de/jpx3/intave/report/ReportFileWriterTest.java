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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

final class ReportFileWriterTest {
  @Test
  void createsAUniqueFileForEveryReport(@TempDir Path dataFolder) throws IOException {
    Path reportDirectory = dataFolder.resolve("physicsreports");
    Report report = new Report() {
      @Override
      public User suspect() {
        return null;
      }

      @Override
      public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "physics");
        return json;
      }
    };

    Path first = ReportFileWriter.writeNew(reportDirectory, "intave-physicsreport", report);
    Path second = ReportFileWriter.writeNew(reportDirectory, "intave-physicsreport", report);

    assertEquals(reportDirectory, first.getParent());
    assertEquals(reportDirectory, second.getParent());
    assertNotEquals(first, second);
    assertTrue(first.getFileName().toString().startsWith("intave-physicsreport-"));
    assertTrue(second.getFileName().toString().endsWith(".json"));
    assertEquals("{\"type\":\"physics\"}", new String(Files.readAllBytes(first), UTF_8));
    assertEquals("{\"type\":\"physics\"}", new String(Files.readAllBytes(second), UTF_8));
  }

  @Test
  void removesAnIncompleteFileWhenSerializationFails(@TempDir Path dataFolder) throws IOException {
    Path reportDirectory = dataFolder.resolve("physicsreports");
    Report brokenReport = new Report() {
      @Override
      public User suspect() {
        return null;
      }

      @Override
      public JsonObject toJson() {
        throw new IllegalStateException("serialization failed");
      }
    };

    assertThrows(
      IllegalStateException.class,
      () -> ReportFileWriter.writeNew(reportDirectory, "intave-physicsreport", brokenReport)
    );
    try (Stream<Path> files = Files.list(reportDirectory)) {
      assertEquals(0, files.count());
    }
  }
}
