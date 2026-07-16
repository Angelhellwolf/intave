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

package de.jpx3.intave.cleanup;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public final class GarbageCollector {
  private static final List<Reference<Map<?, ?>>> boundMaps = Lists.newCopyOnWriteArrayList();
  private static final List<Reference<List<?>>> boundLists = Lists.newCopyOnWriteArrayList();
  private static final List<Reference<Set<?>>> boundSets = Lists.newCopyOnWriteArrayList();
  private static final Map<Object, Runnable> removalSubscriptions = Maps.newConcurrentMap();

  private GarbageCollector() {
    throw new UnsupportedOperationException();
  }

  // class loading
  public static void setup() {
    ShutdownTasks.add(GarbageCollector::die);
  }

  public static <K, V> Map<K, V> watch(Map<K, V> initialMap) {
    boundMaps.add(new WeakReference<>(initialMap));
    return initialMap;
  }

  public static <K, V> AtomicReference<Map<K, V>> watch(AtomicReference<Map<K, V>> initialMap) {
    boundMaps.add(new WeakReference<>(initialMap.get()));
    return initialMap;
  }

  public static <T> List<T> watch(List<T> initialList) {
    boundLists.add(new WeakReference<>(initialList));
    return initialList;
  }

  public static <T> Set<T> watch(Set<T> initialSet) {
    boundSets.add(new WeakReference<>(initialSet));
    return initialSet;
  }

  public static void subscribeToRemoval(Object key, Runnable callback) {
    removalSubscriptions.put(key, callback);
  }

  public static <K> void clear(K key) {
    Runnable remove = removalSubscriptions.remove(key);
    if (remove != null) {
      remove.run();
    }
    boundMaps.forEach(reference -> {
      Map<?, ?> map;
      if ((map = reference.get()) != null) {
        map.remove(key);
      }
    });
    boundLists.forEach(reference -> {
      List<?> list;
      if ((list = reference.get()) != null) {
        list.remove(key);
      }
    });
    boundSets.forEach(reference -> {
      Set<?> set;
      if ((set = reference.get()) != null) {
        set.remove(key);
      }
    });
  }

  public static void clearIf(Predicate<Object> check) {
    boundMaps.forEach(reference -> {
      Map<?, ?> map = reference.get();
      if (map != null) {
        map.entrySet().removeIf(entry -> check.test(entry.getKey()));
      }
    });
    boundLists.forEach(reference -> {
      List<?> list = reference.get();
      if (list != null) {
        list.removeIf(check);
      }
    });
    boundSets.forEach(reference -> {
      Set<?> set = reference.get();
      if (set != null) {
        set.removeIf(check);
      }
    });
  }

  public static void die() {
    boundMaps.clear();
    boundLists.clear();
    boundSets.clear();
  }
}
