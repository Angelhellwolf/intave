package de.jpx3.intave.module.test.record;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

final class MovementRecordingRejectionTuningTests {
	private static final Path REJECTION_TUNING_RECORDINGS = Paths.get(
		"src", "test", "resources", "physics_test_runs", "pending"
	);

	@TestFactory
	Stream<DynamicTest> pendingMovementRecordings() throws IOException {
		List<Path> recordingPaths = MovementRecordingPhysicsTests.findMovementRecordings(REJECTION_TUNING_RECORDINGS);
		if (recordingPaths.isEmpty()) {
			return Stream.empty();
		}
		return recordingPaths.stream()
			.map(recordingPath -> dynamicTest(
				MovementRecordingPhysicsTests.resourcePathOf(recordingPath),
				() -> MovementRecordingPhysicsTests.processRecordingResource(
					MovementRecordingPhysicsTests.resourcePathOf(recordingPath)
				)
			));
	}
}
