package me.phantomclone.permissionsystem.inject;

import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

class PermissionTree {

  private final Map<String, Entry> head = new HashMap<>();

  void clear() {
    head.clear();
  }

  void add(String[] path, boolean value, LocalDateTime nullableUntil) {
    if (!value && path.length != 0 && path[0].startsWith("-")) {
      path[0] = path[0].substring(1);
    }

    boolean isEnd = path.length == 1;

    head.putIfAbsent(path[0], new Entry(isEnd ? value : null, isEnd ? nullableUntil : null));
    Entry entry = head.get(path[0]);
    if (!isEnd) {
      entry.add(path, 1, value, nullableUntil);
    } else {
      entry.value = value;
    }
  }

  boolean containsPermission(String[] path) {
    if (path.length == 1) {
      return head.containsKey(path[0])
          && Optional.ofNullable(head.get(path[0]).nullableUntil)
              .filter(localDateTime -> LocalDateTime.now().isBefore(localDateTime))
              .isEmpty();
    }

    return head.get(path[0]).containsPermission(path, 1);
  }

  boolean getValue(String[] path) {
    Boolean starResult =
        ofNullable(head.get("*"))
            .filter(entry -> entry.value != null)
            .filter(
                entry ->
                    entry.nullableUntil == null || LocalDateTime.now().isBefore(entry.nullableUntil))
            .map(entry -> entry.value)
            .orElse(null);
    Boolean result = searchTreeForValue(head, path, 0);

    if (starResult == null) {
      return result != null ? result : false;
    }

    return starResult && (result == null || result);
  }

  private static Boolean searchTreeForValue(Map<String, Entry> head, String[] path, int index) {
    Entry entry = head.get(path[index]);
    if (entry == null) {
      return null;
    }

    if (entry.value == null) {
      return entry.getValue(path, index + 1);
    }
    Boolean result = entry.getValue(path, index + 1);

    if (result == null) {
      return entry.value && (entry.nullableUntil == null || LocalDateTime.now().isBefore(entry.nullableUntil));
    }

    return result && (entry.nullableUntil == null || LocalDateTime.now().isBefore(entry.nullableUntil));
  }

  @AllArgsConstructor
  private static class Entry {

    private final Map<String, Entry> head = new HashMap<>();

    private Boolean value;
    private LocalDateTime nullableUntil;

    private void add(String[] path, int index, boolean value, LocalDateTime nullableUntil) {
      boolean isEnd = path.length - 1 == index;

      String key = path[index];
      head.putIfAbsent(key, new Entry(isEnd ? value : null, isEnd ? nullableUntil : null));
      Entry entry = head.get(key);
      if (!isEnd) {
        entry.add(path, index + 1, value, nullableUntil);
      } else {
        entry.value = value;
      }
    }

    private Boolean getValue(String[] path, int index) {
      if (path.length == index) {
        return value;
      }

      Boolean starResult =
          ofNullable(head.get("*"))
              .filter(entry -> entry.value != null)
              .filter(
                  entry ->
                      entry.nullableUntil == null
                          || LocalDateTime.now().isBefore(entry.nullableUntil))
              .map(entry -> entry.value)
              .orElse(null);
      Boolean result = searchTreeForValue(head, path, index);

      if (starResult == null) {
        return result;
      }
      if (starResult && (result == null || result)) {
        return true;
      }

      return result;
    }

    public boolean containsPermission(String[] path, int index) {
      if (path.length - 1 == index) {
        return head.containsKey(path[index])
            && Optional.ofNullable(head.get(path[index]).nullableUntil)
                .filter(localDateTime -> LocalDateTime.now().isBefore(localDateTime))
                .isEmpty();
      }

      return head.get(path[index]).containsPermission(path, index + 1);
    }
  }
}
