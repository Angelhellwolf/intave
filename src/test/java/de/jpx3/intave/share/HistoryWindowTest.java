package de.jpx3.intave.share;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class HistoryWindowTest {
  @Test
  void testHistoryWindow() {
    HistoryWindow<Integer> historyWindow = new HistoryWindow<>(10);
    for (int i = 0; i <= 40; i++) {
      historyWindow.add(i);
    }
    for (int i = 0; i < 10; i++) {
      assertEquals(40 - i, historyWindow.back(i));
    }
  }
}
