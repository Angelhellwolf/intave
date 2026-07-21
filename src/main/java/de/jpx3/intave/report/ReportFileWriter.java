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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

public final class ReportFileWriter {
  private ReportFileWriter() {
  }

  /**
   * Writes a report to a uniquely named new file in {@code directory}.
   */
  public static Path writeNew(Path directory, String filePrefix, Report report) throws IOException {
    Files.createDirectories(directory);
    String fileName = filePrefix
      + "-" + System.currentTimeMillis()
      + "-" + UUID.randomUUID()
      + ".json";
    Path reportFile = directory.resolve(fileName);
    boolean created = false;
    try (BufferedWriter writer = Files.newBufferedWriter(reportFile, UTF_8, CREATE_NEW, WRITE)) {
      created = true;
      writer.write(report.toJson().toString());
      return reportFile;
    } catch (IOException | RuntimeException exception) {
      if (created) {
        try {
          Files.deleteIfExists(reportFile);
        } catch (IOException cleanupException) {
          exception.addSuppressed(cleanupException);
        }
      }
      throw exception;
    }
  }
}
