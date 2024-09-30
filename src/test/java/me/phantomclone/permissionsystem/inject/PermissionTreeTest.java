package me.phantomclone.permissionsystem.inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PermissionTreeTest {

  static PermissionTree testee;

  @BeforeAll
  static void beforeAll() {
    testee = new PermissionTree();
  }

  @BeforeEach
  void beforeEach() {
    testee.clear();
  }

  @Test
  void testClear() {
    addPerms("*", true);

    assertTrue(testee.getValue(splitPerm("a.b.c.d.a.c")));

    testee.clear();

    assertFalse(testee.getValue(splitPerm("a.b.c.d.a.c")));
  }

  @Test
  void testStar() {
    addPerms("*", true);

    assertTrue(testee.getValue(splitPerm("a.b.c.d.a.c")));
  }

  @Test
  void testStarWithNeg() {
    addPerms("a.b.c.*", true);
    addPerms("a.b.c.d.a", false);

    assertFalse(testee.getValue(splitPerm("a.b.c.d.a")));
  }

  @Test
  void testStarWithNegPackageAndPosiPerm() {
    addPerms("a.b.c.*", true);
    addPerms("a.b.c.d.a", false);
    addPerms("a.b.c.d.a.c", true);

    assertTrue(testee.getValue(splitPerm("a.b.c.d.a.c")));
  }

  @Test
  void testHasPermission() {
    addPerms("a.b.c.d.a", false);
    addPerms("a.b.c.d.a.c", true);

    assertTrue(testee.getValue(splitPerm("a.b.c.d.a.c")));
  }

  @Test
  void testHasNotPermission() {
    addPerms("a.b.c.d.a", true);
    addPerms("a.b.c.d.a.c", false);

    assertFalse(testee.getValue(splitPerm("a.b.c.d.a.c")));
  }

  @Test
  void testHasOnePermission() {
    addPerms("pl.cmd.core.feed", true);

    assertTrue(testee.getValue(splitPerm("pl.cmd.core.feed")));
  }

  @Test
  void testHasOnePermissionWithNegUntil() {
    addPerms("pl.cmd.core.feed", true, LocalDateTime.now().minusMinutes(1));

    assertFalse(testee.getValue(splitPerm("pl.cmd.core.feed")));
  }

  @Test
  void testHasOnePermissionWithPosUntil() {
    addPerms("pl.cmd.core.feed", true, LocalDateTime.now().plusMinutes(1));

    assertTrue(testee.getValue(splitPerm("pl.cmd.core.feed")));
  }

  void addPerms(String permString, boolean value) {
    addPerms(permString, value, null);
  }

  void addPerms(String permString, boolean value, LocalDateTime nullableUntil) {
    testee.add(splitPerm(permString), value, nullableUntil);
  }

  static String[] splitPerm(String permString) {
    return permString.split("\\.");
  }
}
