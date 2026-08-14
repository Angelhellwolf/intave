package de.jpx3.intave.module.feedback;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FeedbackQueueTest {
  @Test
  void drainsOutOfOrderAcknowledgementsInRequestOrder() {
    FeedbackQueue queue = new FeedbackQueue();
    List<String> callbacks = new ArrayList<>();
    FeedbackRequest<String> first = request((short) 1, 1, "A", callbacks);
    FeedbackRequest<String> second = request((short) 2, 2, "B", callbacks);
    queue.add(first);
    queue.add(second);

    second.markAcknowledgedByClient();
    assertTrue(queue.pollAcknowledged().isEmpty());
    assertEquals(2, queue.size());

    first.markAcknowledgedByClient();
    for (FeedbackRequest<?> request : queue.pollAcknowledged()) {
      request.acknowledge(null);
    }

    assertEquals(Arrays.asList("A", "B"), callbacks);
    assertEquals(0, queue.size());
    assertEquals(Collections.emptyList(), queue.pollAcknowledged());
  }

  private FeedbackRequest<String> request(short userKey, long number, String name, List<String> callbacks) {
    return new FeedbackRequest<>((player, target) -> callbacks.add(target), null, name, userKey, number, 0);
  }
}
